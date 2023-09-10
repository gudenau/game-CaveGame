package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.renderer.shader.AttributeType;
import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.VK10.*;

public final class VulkanShaderModule implements AutoCloseable {
    @NotNull
    private final VulkanLogicalDevice device;
    @NotNull
    private final Type type;
    private final long handle;

    @NotNull
    private final List<Resource> inputs;
    @NotNull
    private final List<Resource> outputs;
    @NotNull
    private final List<Resource> uniforms;

    private final int inputStride;
    private final int outputStride;

    public record Resource(
        @NotNull String name,
        int location,
        AttributeType type,
        int size,
        int stride
    ) {
        private static Resource of(ShaderReflection.Resource resource) {
            return switch(resource.baseType()) {
                case STRUCT -> {
                    int bytes = 0;
                    for(var member : resource.members()) {
                        bytes += member.size();
                    }

                    yield new Resource(
                        resource.name(),
                        resource.location(),
                        resource.baseType(),
                        -1,
                        bytes
                    );
                }
                default -> new Resource(
                    resource.name(),
                    resource.location(),
                    resource.baseType(),
                    resource.vectorSize(),
                    resource.size()
                );
            };
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
                .map(Resource::of)
                .toList();

            outputs = reflection.outputs().stream()
                .map(Resource::of)
                .toList();

            uniforms = reflection.uniforms().stream()
                .map(Resource::of)
                .toList();
        }

        inputStride = inputs.stream()
            .mapToInt(Resource::stride)
            .sum();

        outputStride = outputs.stream()
            .mapToInt(Resource::stride)
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

    public List<Resource> inputs() {
        return inputs;
    }

    public List<Resource> outputs() {
        return outputs;
    }

    public List<Resource> uniforms() {
        return uniforms;
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
        VERTEX(shaderc_vertex_shader, VK_SHADER_STAGE_VERTEX_BIT, true),
        FRAGMENT(shaderc_fragment_shader, VK_SHADER_STAGE_FRAGMENT_BIT, true),
        ;

        static final Set<Type> REQUIRED = Stream.of(values())
            .filter((value) -> value.required)
            .collect(Collectors.toUnmodifiableSet());

        private final int shadercType;
        final int vulkanType;
        private final boolean required;

        Type(int shadercType, int vulkanType, boolean required) {
            this.shadercType = shadercType;
            this.vulkanType = vulkanType;
            this.required = required;
        }
    }
}
