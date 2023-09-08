package net.gudenau.cavegame.renderer.vk;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.gudenau.cavegame.logger.Logger;
import net.gudenau.cavegame.renderer.shader.AttributeType;
import net.gudenau.cavegame.util.BufferStreams;
import net.gudenau.cavegame.util.collection.FastCollectors;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.spvc.SpvcErrorCallback;
import org.lwjgl.util.spvc.SpvcReflectedResource;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.spvc.Spv.SpvDecorationLocation;
import static org.lwjgl.util.spvc.Spvc.*;
import static org.lwjgl.vulkan.VK10.*;

public final class ShaderReflection implements AutoCloseable {
    public final class Resource {
        private final SpvcReflectedResource resource;
        private final long base;

        private Resource(SpvcReflectedResource resource) {
            this.resource = resource;
            base = spvc_compiler_get_type_handle(compiler, resource.type_id());
        }

        public String name() {
            return resource.nameString();
        }

        public int location() {
            return spvc_compiler_get_decoration(compiler, resource.id(), SpvDecorationLocation);
        }

        @NotNull
        public AttributeType baseType() {
            return switch(spvc_type_get_basetype(base)) {
                case SPVC_BASETYPE_FP32 -> AttributeType.FLOAT;
                default -> throw new RuntimeException("Don't know how to handle a base type of " + spvc_type_get_basetype(base));
            };
        }

        public int vectorSize() {
            return spvc_type_get_vector_size(base);
        }

        public int size() {
            return (spvc_type_get_bit_width(base) >> 3) * vectorSize();
        }
    }

    private static final Logger LOGGER = Logger.forName("SPVC");

    private final Int2ObjectMap<List<Resource>> resourceCache = new Int2ObjectOpenHashMap<>();
    private static SpvcErrorCallback errorCallback;
    private final long context;
    final long compiler;
    private final long resources;

    private ShaderReflection(long context, long compiler, long resources) {
        this.context = context;
        this.compiler = compiler;
        this.resources = resources;
    }

    @NotNull
    public static ShaderReflection acquire(@NotNull ByteBuffer code) {
        long context = NULL;
        try(var stack = MemoryStack.stackPush()) {
            var contextPointer = stack.pointers(0);
            var result = spvc_context_create(contextPointer);
            if(result != SPVC_SUCCESS) {
                throw new RuntimeException("Failed to create SPVC context: " + result);
            }
            context = contextPointer.get(0);

            errorCallback = SpvcErrorCallback.create((user, errorPointer) ->
                LOGGER.error(MemoryUtil.memUTF8(errorPointer))
            );
            spvc_context_set_error_callback(context, errorCallback, NULL);

            var irPointer = stack.pointers(0);
            result = spvc_context_parse_spirv(context, code.asIntBuffer(), code.remaining() >> 2, irPointer);
            if(result != SPVC_SUCCESS) {
                throw new RuntimeException("Failed to parse SPIRV: " + result);
            }
            var ir = irPointer.get(0);

            var compilerPointer = stack.pointers(0);
            result = spvc_context_create_compiler(context, SPVC_BACKEND_NONE, ir, SPVC_CAPTURE_MODE_TAKE_OWNERSHIP, compilerPointer);
            if(result != SPVC_SUCCESS) {
                throw new RuntimeException("Failed to create compiler: " + result);
            }
            var compiler = compilerPointer.get(0);

            var resourcesPointer = stack.pointers(0);
            result = spvc_compiler_create_shader_resources(compiler, resourcesPointer);
            if(result != SPVC_SUCCESS) {
                throw new RuntimeException("Failed to get resources: " + result);
            }
            var resources = resourcesPointer.get(0);

            return new ShaderReflection(context, compiler, resources);
        } catch (Throwable e) {
            if(context != NULL) {
                spvc_context_destroy(context);
            }
            throw e;
        }
    }

    private List<Resource> getResources(int resource) {
        var cached = resourceCache.get(resource);
        if(cached != null) {
            return cached;
        }

        try(var stack = MemoryStack.stackPush()) {
            var list = stack.pointers(0);
            var countPointer = stack.pointers(0);
            var result = spvc_resources_get_resource_list_for_type(resources, resource, list, countPointer);
            if(result != SPVC_SUCCESS) {
                throw new RuntimeException("Failed to get resource: " + result);
            }

            var buffer = SpvcReflectedResource.create(list.get(0), (int) countPointer.get(0));
            var resources = BufferStreams.structureStream(buffer)
                .map(Resource::new)
                .toList();
            resourceCache.put(resource, resources);
            return resources;
        }
    }

    public List<Resource> uniforms() {
        return getResources(SPVC_RESOURCE_TYPE_UNIFORM_BUFFER);
    }

    public List<Resource> inputs() {
        return getResources(SPVC_RESOURCE_TYPE_STAGE_INPUT);
    }

    public List<Resource> outputs() {
        return getResources(SPVC_RESOURCE_TYPE_STAGE_OUTPUT);
    }

    @Override
    public void close() {
        spvc_context_destroy(context);
        errorCallback.close();
    }
}
