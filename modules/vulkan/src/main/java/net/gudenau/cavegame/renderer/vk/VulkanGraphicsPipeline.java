package net.gudenau.cavegame.renderer.vk;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.gudenau.cavegame.renderer.GraphicsBuffer;
import net.gudenau.cavegame.renderer.shader.VertexAttribute;
import net.gudenau.cavegame.renderer.vk.texture.VulkanTexture;
import net.gudenau.cavegame.util.BufferUtil;
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
    private final long descriptorSetLayout;
    private final VulkanDescriptorSets descriptorSets;

    //TODO Cache stuff
    //FIXME Break this up
    public VulkanGraphicsPipeline(
        @NotNull VulkanLogicalDevice device,
        @NotNull VkExtent2D viewportExtent,
        @NotNull VulkanRenderPass renderPass,
        @NotNull Collection<VulkanShaderModule> modules,
        @NotNull VkVertexFormat format,
        @NotNull VkUniformLayout uniforms,
        @NotNull VulkanDescriptorPool descriptorPool,
        @NotNull List<GraphicsBuffer> buffers,
        @NotNull Int2ObjectMap<VulkanTexture> textures
    ) {
        this.device = device;

        try (var stack = MemoryStack.stackPush()) {
            var dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack);
            dynamicState.sType$Default();
            dynamicState.pDynamicStates(stack.ints(
                VK_DYNAMIC_STATE_VIEWPORT,
                VK_DYNAMIC_STATE_SCISSOR
            ));

            var inputs = format.attributes();
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
            rasterizer.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE);
            rasterizer.depthBiasEnable(false);

            var multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
            multisampling.sType$Default();
            multisampling.sampleShadingEnable(false);
            multisampling.rasterizationSamples(device.device().maxSampleCount());

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

            var layoutBindings = VkDescriptorSetLayoutBinding.calloc(uniforms.uniforms().size() + 1, stack);
            for(int i = 0, limit = uniforms.uniforms().size(); i < limit; i++) {
                var uniform = uniforms.uniforms().get(i);
                var binding = layoutBindings.get(i);
                binding.binding(uniform.location());
                binding.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                binding.descriptorCount(1);
                binding.stageFlags(switch(uniform.shader()) {
                    case FRAGMENT -> VK_SHADER_STAGE_FRAGMENT_BIT;
                    case VERTEX -> VK_SHADER_STAGE_VERTEX_BIT;
                });
            }

            {
                var binding = layoutBindings.get(uniforms.uniforms().size());
                binding.binding(1);
                binding.descriptorCount(1);
                binding.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                binding.pImmutableSamplers(null);
                binding.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
            }

            var layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
            layoutInfo.sType$Default();
            layoutInfo.pBindings(layoutBindings);
            var descriptorSetLayoutPointer = stack.longs(0);
            var result = vkCreateDescriptorSetLayout(device.handle(), layoutInfo, VulkanAllocator.get(), descriptorSetLayoutPointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan descriptor set layout: " + VulkanUtils.errorString(result));
            }
            descriptorSetLayout = descriptorSetLayoutPointer.get(0);

            var pilelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            pilelineLayoutInfo.sType$Default();
            pilelineLayoutInfo.pSetLayouts(descriptorSetLayoutPointer);
            pilelineLayoutInfo.pPushConstantRanges(null);

            var pointer = stack.longs(0);
            result = vkCreatePipelineLayout(device.handle(), pilelineLayoutInfo, VulkanAllocator.get(), pointer);
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

            var depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack);
            depthStencil.sType$Default();
            depthStencil.depthTestEnable(true);
            depthStencil.depthWriteEnable(true);
            depthStencil.depthCompareOp(VK_COMPARE_OP_LESS);
            depthStencil.depthBoundsTestEnable(false);
            depthStencil.stencilTestEnable(false);

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
            pipelineInfo.pDepthStencilState(depthStencil);

            result = vkCreateGraphicsPipelines(device.handle(), VK_NULL_HANDLE, pipelineInfo, VulkanAllocator.get(), pointer);
            if(result != VK_SUCCESS) {
                vkDestroyPipelineLayout(device.handle(), pipelineLayout, VulkanAllocator.get());
                throw new RuntimeException("Failed to create Vulkan graphics pipeline: " + VulkanUtils.errorString(result));
            }
            handle = pointer.get(0);

            descriptorSets = new VulkanDescriptorSets(device, descriptorPool, BufferUtil.ofFilled(stack, buffers.size(), descriptorSetLayout));

            var descriptorWrites = VkWriteDescriptorSet.calloc(buffers.size() + buffers.size() * textures.size(), stack);
            for(int i = 0; i < buffers.size(); i++) {
                var bufferInfos = VkDescriptorBufferInfo.calloc(1, stack);
                var bufferInfo = bufferInfos.get(0);
                bufferInfo.buffer(((VkGraphicsBuffer) buffers.get(i)).handle());
                bufferInfo.offset(0);
                bufferInfo.range(VK_WHOLE_SIZE);

                var descriptorWrite = descriptorWrites.get();
                descriptorWrite.sType$Default();
                descriptorWrite.dstSet(descriptorSets.get(i));
                descriptorWrite.dstBinding(0);
                descriptorWrite.dstArrayElement(0);
                descriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                descriptorWrite.descriptorCount(1);
                descriptorWrite.pBufferInfo(bufferInfos);
                descriptorWrite.pImageInfo(null);
                descriptorWrite.pTexelBufferView(null);
            }

            if(!textures.isEmpty()) {
                for(int i = 0; i < buffers.size(); i++) {
                    for(var entry : textures.int2ObjectEntrySet()) {
                        var binding = entry.getIntKey();
                        var texture = entry.getValue();

                        //TODO This seems wrong.
                        var imageInfos = VkDescriptorImageInfo.calloc(1, stack);
                        var imageInfo = imageInfos.get(0);
                        imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                        imageInfo.imageView(texture.imageView().handle());
                        imageInfo.sampler(texture.sampler().handle());

                        var descriptorWrite = descriptorWrites.get();
                        descriptorWrite.sType$Default();
                        descriptorWrite.dstSet(descriptorSets.get(i));
                        descriptorWrite.dstBinding(binding);
                        descriptorWrite.dstArrayElement(0);
                        descriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                        descriptorWrite.descriptorCount(1);
                        descriptorWrite.pBufferInfo(null);
                        descriptorWrite.pImageInfo(imageInfos);
                        descriptorWrite.pTexelBufferView(null);
                    }
                }
            }

            descriptorWrites.flip();
            vkUpdateDescriptorSets(device.handle(), descriptorWrites, null);
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
            case STRUCT -> throw new RuntimeException("Struct is not yet supported");
            case SAMPLER -> throw new RuntimeException("Sampler is not yet supported");
        };
    }

    public long handle() {
        return handle;
    }

    public long layout() {
        return pipelineLayout;
    }

    @Override
    public void close() {
        descriptorSets.close();
        vkDestroyPipeline(device.handle(), handle, VulkanAllocator.get());
        vkDestroyDescriptorSetLayout(device.handle(), descriptorSetLayout, VulkanAllocator.get());
        vkDestroyPipelineLayout(device.handle(), pipelineLayout, VulkanAllocator.get());
    }

    public long descriptorSet(int index) {
        return descriptorSets.get(index);
    }
}
