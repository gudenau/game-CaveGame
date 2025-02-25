package net.gudenau.cavegame.renderer.vk;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import net.gudenau.cavegame.logger.Logger;
import net.gudenau.cavegame.renderer.*;
import net.gudenau.cavegame.renderer.shader.Shader;
import net.gudenau.cavegame.renderer.shader.ShaderMeta;
import net.gudenau.cavegame.renderer.texture.Texture;
import net.gudenau.cavegame.renderer.texture.TextureManager;
import net.gudenau.cavegame.renderer.vk.texture.VulkanTexture;
import net.gudenau.cavegame.renderer.vk.texture.VulkanTextureManager;
import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.util.collection.FastCollectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFormatProperties;
import org.lwjgl.vulkan.VkPresentInfoKHR;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public final class VkRenderer implements Renderer {
    public static final Logger LOGGER = Logger.forName("Vulkan");

    private static final int MAX_FRAMES_IN_FLIGHT = 2;
    private int currentFrame = 0;
    private int realCurrentFrame = 0;
    private boolean framebufferResized = false;

    @NotNull
    private final VulkanInstance instance;
    @Nullable
    private final VulkanDebugMessenger debugMessenger;
    @NotNull
    private final VulkanSurface surface;
    @NotNull
    private final VulkanPhysicalDevice physicalDevice;
    @NotNull
    private final VulkanLogicalDevice logicalDevice;
    @NotNull
    private VulkanSwapchain swapchain;
    @NotNull
    private List<@NotNull VulkanImageView> imageViews;
    @NotNull
    private final VulkanRenderPass renderPass;
    @NotNull
    private List<@NotNull VulkanFramebuffer> swapchainFramebuffers;
    @NotNull
    private final VulkanCommandPool commandPool;
    @NotNull
    private VulkanImageBuffer colorBuffer;
    @NotNull
    private VulkanImageBuffer depthBuffer;
    private final List<FrameState> frameState;
    @Nullable
    private FrameState currentFrameState;
    private int currentImageIndex;
    private final VulkanDescriptorPool descriptorPool;
    private final VulkanTextureManager textureManager;

    public VulkanPhysicalDevice physicalDevice() {
        return physicalDevice;
    }

    private record FrameState(
        int index,
        @NotNull VulkanCommandBuffer commandBuffer,
        @NotNull VulkanSemaphore imageAvailableSemaphore,
        @NotNull VulkanSemaphore renderFinishedSemaphore,
        @NotNull VulkanFence inFlightFence,
        @NotNull GraphicsBuffer uniformBuffer
    ) implements AutoCloseable {
        @Override
        public void close() {
            uniformBuffer.close();
            commandBuffer.close();
            imageAvailableSemaphore.close();
            renderFinishedSemaphore.close();
            inFlightFence.close();
        }
    }

    private FrameState createFrameState(
        int index,
        @NotNull VulkanLogicalDevice device,
        @NotNull VulkanCommandPool commandPool
    ) {
        return new FrameState(
            index,
            new VulkanCommandBuffer(device, commandPool),
            new VulkanSemaphore(device),
            new VulkanSemaphore(device),
            new VulkanFence(device),
            createBuffer(BufferType.UNIFORM, Float.BYTES * 4 * 4 * 3)
        );
    }

    public VkRenderer(@NotNull VkWindow window) {
        try(var stack = MemoryStack.stackPush()) {
            window.resizeCallback((_, _, _) -> framebufferResized = true);

            instance = new VulkanInstance();
            debugMessenger = VulkanUtils.ENABLE_DEBUG ? new VulkanDebugMessenger(instance) : null;
            surface = new VulkanSurface(window, instance);
            physicalDevice = VulkanPhysicalDevice.pick(instance, surface, window);
            var extent = physicalDevice.surfaceExtent(stack);
            logicalDevice = new VulkanLogicalDevice(physicalDevice);
            swapchain = new VulkanSwapchain(physicalDevice, surface, logicalDevice, extent);
            imageViews = swapchain.stream()
                .map((image) -> new VulkanImageView(logicalDevice, image))
                .toList();

            commandPool = new VulkanCommandPool(physicalDevice, logicalDevice);

            colorBuffer = new VulkanImageBuffer(logicalDevice, swapchain, swapchain.imageFormat(), VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
            depthBuffer = new VulkanImageBuffer(logicalDevice, swapchain, findDepthFormat(physicalDevice), VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);

            renderPass = new VulkanRenderPass(logicalDevice, swapchain, depthBuffer);

            swapchainFramebuffers = imageViews.stream()
                .map((view) -> new VulkanFramebuffer(logicalDevice, swapchain, renderPass, view, depthBuffer, colorBuffer))
                .toList();

            descriptorPool = new VulkanDescriptorPool(
                logicalDevice,
                new VulkanDescriptorPool.Info(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, MAX_FRAMES_IN_FLIGHT),
                new VulkanDescriptorPool.Info(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, MAX_FRAMES_IN_FLIGHT)
            );

            frameState = IntStream.range(0, MAX_FRAMES_IN_FLIGHT)
                .mapToObj((index) -> createFrameState(index, logicalDevice, commandPool))
                .toList();

            textureManager = new VulkanTextureManager(this);
        }
    }

    private int findDepthFormat(VulkanPhysicalDevice device) {
        try(var stack = MemoryStack.stackPush()) {
            var props = VkFormatProperties.calloc(stack);
            var formats = IntList.of(VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT, VK_FORMAT_D24_UNORM_S8_UINT);
            for(var format : formats) {
                vkGetPhysicalDeviceFormatProperties(device.device(), format, props);

                if((props.optimalTilingFeatures() & VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) != 0) {
                    return format;
                }
            }
        }

        throw new RuntimeException("Failed to find depth buffer format");
    }

    private void destroySwapChain() {
        colorBuffer.close();
        depthBuffer.close();
        swapchainFramebuffers.forEach(VulkanFramebuffer::close);
        imageViews.forEach(VulkanImageView::close);
        swapchain.close();
    }

    private void recreateSwapChain() {
        try(var stack = MemoryStack.stackPush()) {
            logicalDevice.waitForIdle();

            destroySwapChain();

            var extent = physicalDevice.surfaceExtent(stack);
            swapchain = new VulkanSwapchain(physicalDevice, surface, logicalDevice, extent);
            imageViews = swapchain.stream()
                .map((image) -> new VulkanImageView(logicalDevice, image))
                .toList();
            colorBuffer = new VulkanImageBuffer(logicalDevice, swapchain, swapchain.imageFormat(), VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
            depthBuffer = new VulkanImageBuffer(logicalDevice, swapchain, findDepthFormat(physicalDevice), VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);
            swapchainFramebuffers = imageViews.stream()
                .map((view) -> new VulkanFramebuffer(logicalDevice, swapchain, renderPass, view, depthBuffer, colorBuffer))
                .toList();
        }
    }

    @Override
    public void close() {
        logicalDevice.waitForIdle();

        textureManager.close();

        destroySwapChain();

        frameState.forEach(FrameState::close);
        descriptorPool.close();
        renderPass.close();
        commandPool.close();
        logicalDevice.close();
        physicalDevice.close();
        surface.close();
        if(debugMessenger != null) {
            debugMessenger.close();
        }
        instance.close();
    }



    @Override
    public void begin() {
        currentFrameState = this.frameState.get(currentFrame);
        realCurrentFrame = currentFrame;
        currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;

        var commandBuffer = currentFrameState.commandBuffer();
        var imageAvailableSemaphore = currentFrameState.imageAvailableSemaphore();
        var inFlightFence = currentFrameState.inFlightFence();

        inFlightFence.yield();

        int imageIndex = swapchain.acquireNextImage(imageAvailableSemaphore);
        //TODO Fix this
        if(imageIndex == -1) {
            recreateSwapChain();
        }
        currentImageIndex = imageIndex;

        updateUniforms();

        commandBuffer.reset();
        commandBuffer.begin();
    }

    private long startTime = System.nanoTime();
    private void updateUniforms() {
        var currentTime = System.nanoTime();
        var delta = (float)((currentTime - startTime) / 10000000000D);

        var ubo = new UniformBufferObject();
        ubo.model().rotate(
            (float) (delta * Math.toRadians(90)),
            new Vector3f(0, 0, 1)
        );
        ubo.view().lookAt(
            new Vector3f(2, 2, 2),
            new Vector3f(0, 0, 0),
            new Vector3f(0, 0, 1)
        );
        var projection = ubo.proj();
        projection.perspective(
            (float) Math.toRadians(45),
            swapchain.extent().width() / (float) swapchain.extent().height(),
            0.1F,
            10.0F,
            true
        );
        projection.m11(-projection.m11());

        try(var stack = MemoryStack.stackPush()) {
            var buffer = stack.calloc(Float.BYTES * 4 * 4 * 3);
            ubo.write(buffer);
            currentFrameState.uniformBuffer.upload(buffer);
        }
    }

    @Override
    public void waitForIdle() {
        logicalDevice.waitForIdle();
    }

    @NotNull
    @Override
    public TextureManager textureManager() {
        return textureManager;
    }

    @Override
    public void drawBuffer(int vertexCount, @NotNull GraphicsBuffer vertexBuffer, @Nullable GraphicsBuffer indexBuffer) {
        var commandBuffer = currentFrameState.commandBuffer;
        commandBuffer.beginRenderPass(swapchain.extent(), renderPass, swapchainFramebuffers.get(currentImageIndex));
        var vulkanShader = (VkShader) vertexBuffer.shader();
        commandBuffer.bindPipeline(vulkanShader.pipeline());
        commandBuffer.setViewport(swapchain.extent().width(), swapchain.extent().height());
        commandBuffer.setScissor(0, 0, swapchain.extent().width(), swapchain.extent().height());
        commandBuffer.bindVertexBuffer((VkGraphicsBuffer) vertexBuffer);
        var pipeline = vulkanShader.pipeline();
        commandBuffer.bindDescriptorSets(pipeline.layout(), pipeline.descriptorSet(realCurrentFrame));
        if(indexBuffer != null) {
            commandBuffer.bindIndexBuffer((VkGraphicsBuffer) indexBuffer);
            commandBuffer.drawIndexed(vertexCount, 1, 0, 0, 0);
        } else {
            commandBuffer.draw(vertexCount, 1, 0, 0);
        }
    }

    @Override
    public void draw() {
        var commandBuffer = currentFrameState.commandBuffer();
        var renderFinishedSemaphore = currentFrameState.renderFinishedSemaphore();
        var imageAvailableSemaphore = currentFrameState.imageAvailableSemaphore();
        var inFlightFence = currentFrameState.inFlightFence();

        commandBuffer.endRenderPass();
        commandBuffer.submit(logicalDevice.graphicsQueue(), imageAvailableSemaphore, renderFinishedSemaphore, inFlightFence);

        try(var stack = MemoryStack.stackPush()) {
            var presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType$Default();
            presentInfo.pWaitSemaphores(stack.longs(renderFinishedSemaphore.handle()));
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(swapchain.handle()));
            presentInfo.pImageIndices(stack.ints(currentImageIndex));
            var result = vkQueuePresentKHR(logicalDevice.presentQueue(), presentInfo);
            switch (result) {
                case VK_SUCCESS -> {}
                case VK_ERROR_OUT_OF_DATE_KHR, VK_SUBOPTIMAL_KHR -> recreateSwapChain();
                default -> throw new RuntimeException("Failed to present Vulkan queue: " + VulkanUtils.errorString(result));
            }
            if(framebufferResized) {
                framebufferResized = false;
                recreateSwapChain();
            }
        }

        currentFrameState = null;
        currentImageIndex = -1;
    }

    @Override
    public Shader loadShader(@NotNull Identifier identifier, @NotNull Map<String, Texture> textures) {
        ShaderMeta metadata;
        {
            var result = ShaderMeta.load(identifier);
            var partial = result.partial();
            if (partial.isPresent()) {
                throw new RuntimeException("Failed to load shader metadata for " + identifier + '\n' + partial.get().error());
            }
            metadata = result.getResult();
        }

        var modules = new HashMap<VulkanShaderModule.Type, VulkanShaderModule>();
        var required = new HashSet<>(VulkanShaderModule.Type.REQUIRED);
        metadata.shaders().forEach((type, info) -> {
            var vkType = switch(type) {
                case VERTEX -> VulkanShaderModule.Type.VERTEX;
                case FRAGMENT -> VulkanShaderModule.Type.FRAGMENT;
            };
            required.remove(vkType);
            modules.put(vkType, new VulkanShaderModule(
                logicalDevice,
                vkType,
                info.files().get("vulkan").normalize("shader", '.' + type.extension())
            ));
        });
        if(!required.isEmpty()) {
            throw new RuntimeException(
                "Failed to load shader, required shaders where missing: " +
                    required.stream()
                        .map((missing) -> missing.name().toLowerCase())
                        .collect(Collectors.joining(", "))
            );
        }

        Int2ObjectMap<VulkanTexture> textureBindings;
        {
            var fragmentShader = modules.get(VulkanShaderModule.Type.FRAGMENT);
            var fragmentSamplers = fragmentShader.samplers().stream()
                .collect(Collectors.toUnmodifiableMap(
                    VulkanShaderModule.Resource::name,
                    Function.identity()
                ));
            var keys = fragmentSamplers.keySet();
            if(!keys.equals(metadata.textures().keySet())) {
                throw new RuntimeException("Fragment shader for " + identifier + " has unmatched image samplers");
            }
            if(!textures.keySet().containsAll(keys)) {
                throw new RuntimeException("Textures supplied to shader " + identifier + " where missing elements");
            }

            textureBindings = fragmentSamplers.entrySet().stream().collect(FastCollectors.toInt2ObjectMap(
                (entry) -> entry.getValue().binding(),
                (entry) -> (VulkanTexture) textures.get(entry.getKey())
            ));
        }

        var vertex = modules.get(VulkanShaderModule.Type.VERTEX);
        var uniforms = new VkUniformLayout(vertex, metadata.uniforms());
        var vertexFormat = new VkVertexFormat(vertex, metadata.attributes());

        try(var stack = MemoryStack.stackPush()) {
            var pipeline = new VulkanGraphicsPipeline(
                logicalDevice,
                physicalDevice.surfaceExtent(stack),
                renderPass,
                modules.values(),
                vertexFormat,
                uniforms,
                descriptorPool,
                frameState.stream()
                    .map(FrameState::uniformBuffer)
                    .toList(),
                textureBindings
            );

            return new VkShader(this, pipeline, vertexFormat);
        } finally {
            modules.values().forEach(VulkanShaderModule::close);
        }
    }

    @NotNull
    @Override
    public VkGraphicsBuffer createBuffer(@NotNull BufferType type, int size) {
        return new VkGraphicsBuffer(logicalDevice, commandPool, type, size);
    }

    @NotNull
    public VulkanLogicalDevice logicalDevice() {
        return logicalDevice;
    }

    @NotNull
    public VulkanCommandPool commandPool() {
        return commandPool;
    }
}
