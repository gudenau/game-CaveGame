package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.renderer.ShaderMeta;
import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.VK10.*;

public final class VulkanShaderModule implements AutoCloseable {
    @NotNull
    private final VulkanLogicalDevice device;
    @NotNull
    private final Type type;
    private final long handle;

    @NotNull
    private final List<Attribute> inputs;
    @NotNull
    private final List<Attribute> outputs;

    private final int inputStride;
    private final int outputStride;

    public record Attribute(
        @NotNull String name,
        int location,
        ShaderReflection.DataType type,
        int size,
        int stride
    ) {
        public int format() {
            return type.format(size);
        }
    }

    public VulkanShaderModule(@NotNull VulkanLogicalDevice device, @NotNull Type type, @NotNull Identifier identifier) {
        this.device = device;
        this.type = type;

        var source = VulkanUtils.readIntoNativeBuffer(identifier);
        ByteBuffer code;
        try(var compiler = ShaderCompiler.acquire()) {
            code = compiler.compile(source, type.shadercType, identifier.toString());
        } finally {
            MemoryUtil.memFree(source);
        }

        try(var reflection = ShaderReflection.acquire(code)) {
            inputs = reflection.inputs().stream()
                .map((attribute) -> new Attribute(
                    attribute.name(),
                    attribute.location(),
                    attribute.baseType(),
                    attribute.vectorSize(),
                    attribute.size()
                ))
                .toList();

            outputs = reflection.outputs().stream()
                .map((attribute) -> new Attribute(
                    attribute.name(),
                    attribute.location(),
                    attribute.baseType(),
                    attribute.vectorSize(),
                    attribute.size()
                ))
                .toList();
        }

        inputStride = inputs.stream()
            .mapToInt(Attribute::stride)
            .sum();

        outputStride = outputs.stream()
            .mapToInt(Attribute::stride)
            .sum();

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

    public List<Attribute> inputs() {
        return inputs;
    }

    public int inputStride() {
        return inputStride;
    }

    public List<Attribute> outputs() {
        return outputs;
    }

    public int outputStride() {
        return outputStride;
    }

    public long handle() {
        return handle;
    }

    @Override
    public void close() {
        vkDestroyShaderModule(device.handle(), handle, VulkanAllocator.get());
    }

    public enum Type {
        VERTEX(shaderc_vertex_shader, VK_SHADER_STAGE_VERTEX_BIT),
        FRAGMENT(shaderc_fragment_shader, VK_SHADER_STAGE_FRAGMENT_BIT),
        ;

        private final int shadercType;
        final int vulkanType;

        Type(int shadercType, int vulkanType) {
            this.shadercType = shadercType;
            this.vulkanType = vulkanType;
        }
    }
}
