package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.renderer.shader.VertexAttribute;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.Collection;
import java.util.List;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.*;

public final class VulkanGraphicsPipeline implements AutoCloseable {
    @NotNull
    private final VulkanLogicalDevice device;
    private final long pipelineLayout;
    private final long handle;

    public VulkanGraphicsPipeline(
        @NotNull VulkanLogicalDevice device,
        @NotNull VkExtent2D viewportExtent,
        @NotNull VulkanRenderPass renderPass,
        @NotNull Collection<VulkanShaderModule> modules,
        @NotNull VkVertexFormat format
    ) {
        this.device = device;

        try (var stack = MemoryStack.stackPush()) {
            var dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack);
            dynamicState.sType$Default();
            dynamicState.pDynamicStates(stack.ints(
                VK_DYNAMIC_STATE_VIEWPORT,
                VK_DYNAMIC_STATE_SCISSOR
            ));

            var inputs = format.attributes().values();
            var bindingDescriptions = VkVertexInputBindingDescription.calloc(1, stack);
            var bindingDescription = bindingDescriptions.get(0);
            bindingDescription.binding(0);
            bindingDescription.stride(format.stride());
            bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

            var attributeDescriptions = VkVertexInputAttributeDescription.calloc(inputs.size(), stack);
            int offset = 0;
            for(var attribute : inputs) {
                //noinspection resource
                attributeDescriptions.get().set(
                    attribute.location(),
                    0,
                    format(attribute),
                    offset
                );
                offset += attribute.stride();
            }
            attributeDescriptions.flip();

            var vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInputInfo.sType$Default();
            vertexInputInfo.pVertexBindingDescriptions(bindingDescriptions);
            vertexInputInfo.pVertexAttributeDescriptions(attributeDescriptions);

            var inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            inputAssembly.sType$Default();
            inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
            inputAssembly.primitiveRestartEnable(false);

            var viewport = VkViewport.calloc(stack);
            viewport.set(
                0,
                0,
                viewportExtent.width(),
                viewportExtent.height(),
                0,
                1
            );

            var scissor = VkRect2D.calloc(stack);
            scissor.offset().set(0, 0);
            scissor.extent(viewportExtent);

            var viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            viewportState.sType$Default();
            viewportState.viewportCount(1);
            viewportState.scissorCount(1);

            var rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
            rasterizer.sType$Default();
            rasterizer.depthClampEnable(false);
            rasterizer.rasterizerDiscardEnable(false);
            rasterizer.polygonMode(VK_POLYGON_MODE_FILL);
            rasterizer.lineWidth(1);
            rasterizer.cullMode(VK_CULL_MODE_BACK_BIT);
            rasterizer.frontFace(VK_FRONT_FACE_CLOCKWISE);
            rasterizer.depthBiasEnable(false);

            var multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
            multisampling.sType$Default();
            multisampling.sampleShadingEnable(false);
            multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            var colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack);
            colorBlendAttachment.colorWriteMask(
                VK_COLOR_COMPONENT_R_BIT |
                    VK_COLOR_COMPONENT_G_BIT |
                    VK_COLOR_COMPONENT_B_BIT |
                    VK_COLOR_COMPONENT_A_BIT
            );
            colorBlendAttachment.blendEnable(false);

            var colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            colorBlending.sType$Default();
            colorBlending.logicOpEnable(false);
            colorBlending.pAttachments(colorBlendAttachment);

            var pilelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            pilelineLayoutInfo.sType$Default();
            pilelineLayoutInfo.pSetLayouts(null);
            pilelineLayoutInfo.pPushConstantRanges(null);

            var pointer = stack.longs(0);
            var result = vkCreatePipelineLayout(device.handle(), pilelineLayoutInfo, VulkanAllocator.get(), pointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan pipeline layout: " + VulkanUtils.errorString(result));
            }
            pipelineLayout = pointer.get(0);

            var shaderInfo = VkPipelineShaderStageCreateInfo.calloc(modules.size(), stack);
            var shaderMain = stack.UTF8("main");
            for(var shader : modules) {
                //noinspection resource
                shaderInfo.get().set(
                    VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO,
                    NULL,
                    0,
                    shader.type().vulkanType,
                    shader.handle(),
                    shaderMain,
                    null
                );
            }
            shaderInfo.clear();

            var pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType$Default();
            pipelineInfo.stageCount(2);
            pipelineInfo.pStages(shaderInfo);
            pipelineInfo.pVertexInputState(vertexInputInfo);
            pipelineInfo.pInputAssemblyState(inputAssembly);
            pipelineInfo.pViewportState(viewportState);
            pipelineInfo.pRasterizationState(rasterizer);
            pipelineInfo.pMultisampleState(multisampling);
            pipelineInfo.pColorBlendState(colorBlending);
            pipelineInfo.pDynamicState(dynamicState);
            pipelineInfo.layout(pipelineLayout);
            pipelineInfo.renderPass(renderPass.handle());
            pipelineInfo.subpass(0);
            pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);

            result = vkCreateGraphicsPipelines(device.handle(), VK_NULL_HANDLE, pipelineInfo, VulkanAllocator.get(), pointer);
            if(result != VK_SUCCESS) {
                vkDestroyPipelineLayout(device.handle(), pipelineLayout, VulkanAllocator.get());
                throw new RuntimeException("Failed to create Vulkan graphics pipeline: " + VulkanUtils.errorString(result));
            }
            handle = pointer.get(0);
        }
    }

    private int format(@NotNull VertexAttribute attribute) {
        return switch(attribute.type()) {
            case FLOAT -> switch(attribute.count()) {
                case 1 -> VK_FORMAT_R32_SFLOAT;
                case 2 -> VK_FORMAT_R32G32_SFLOAT;
                case 3 -> VK_FORMAT_R32G32B32_SFLOAT;
                case 4 -> VK_FORMAT_R32G32B32A32_SFLOAT;
                default -> throw new RuntimeException("Unknown format for " + attribute.type().name().toLowerCase() + attribute.count());
            };
        };
    }

    public long handle() {
        return handle;
    }

    @Override
    public void close() {
        vkDestroyPipeline(device.handle(), handle, VulkanAllocator.get());
        vkDestroyPipelineLayout(device.handle(), pipelineLayout, VulkanAllocator.get());
    }
}
