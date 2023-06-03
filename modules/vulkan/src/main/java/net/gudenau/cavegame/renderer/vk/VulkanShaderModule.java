package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;

import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.VK10.*;

public final class VulkanShaderModule implements AutoCloseable {
    @NotNull
    private final VulkanLogicalDevice device;
    @NotNull
    private final Type type;
    private final long handle;

    public VulkanShaderModule(@NotNull VulkanLogicalDevice device, @NotNull Type type, @NotNull Identifier identifier) {
        this.device = device;
        this.type = type;

        //TODO Name this and shove it into Identifier.
        var filename = identifier.filename();
        var directory = identifier.directory();
        var namespace = identifier.namespace();
        Identifier resource;
        if(directory == null) {
            resource = new Identifier(namespace, "shader/" + filename + "/vulkan." + type.extension);
        } else {
            resource = new Identifier(namespace, "shader/" + directory + "/" + filename + "/vulkan." + type.extension);
        }

        var source = VulkanUtils.readIntoNativeBuffer(resource);
        ByteBuffer code;
        try(var compiler = ShaderCompiler.acquire()) {
            code = compiler.compile(source, type.shadercType, identifier.toString());
        } finally {
            MemoryUtil.memFree(source);
        }

        try(var stack = MemoryStack.stackPush()) {
            var createInfo = VkShaderModuleCreateInfo.calloc(stack);
            createInfo.sType$Default();
            createInfo.pCode(code);

            var pointer = stack.longs(0);
            var result = vkCreateShaderModule(device.handle(), createInfo, VulkanAllocator.get(), pointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan shader module: " + VulkanUtils.errorString(result));
            }
            handle = pointer.get(0);
        } finally {
            MemoryUtil.memFree(code);
        }
    }

    @NotNull
    public Type type() {
        return type;
    }

    public long handle() {
        return handle;
    }

    @Override
    public void close() {
        vkDestroyShaderModule(device.handle(), handle, VulkanAllocator.get());
    }

    public enum Type {
        VERTEX("vert", shaderc_vertex_shader, VK_SHADER_STAGE_VERTEX_BIT),
        FRAGMENT("frag", shaderc_fragment_shader, VK_SHADER_STAGE_FRAGMENT_BIT),
        ;

        @NotNull
        private final String extension;
        private final int shadercType;
        final int vulkanType;

        Type(@NotNull String extension, int shadercType, int vulkanType) {
            this.extension = extension;
            this.shadercType = shadercType;
            this.vulkanType = vulkanType;
        }
    }
}
