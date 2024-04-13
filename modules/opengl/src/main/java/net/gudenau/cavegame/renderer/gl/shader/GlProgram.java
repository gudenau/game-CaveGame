package net.gudenau.cavegame.renderer.gl.shader;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.gudenau.cavegame.renderer.BufferBuilder;
import net.gudenau.cavegame.renderer.gl.GlRenderer;
import net.gudenau.cavegame.renderer.gl.GlState;
import net.gudenau.cavegame.renderer.gl.GlTexture;
import net.gudenau.cavegame.renderer.shader.*;
import net.gudenau.cavegame.renderer.texture.Texture;
import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.util.collection.FastCollectors;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.lwjgl.opengl.GL33C.*;

//FIXME Fix this junk
public final class GlProgram implements Shader {
    private final GlRenderer renderer;
    private final int handle;
    private final Map<String, Attribute> attributes;
    private final Map<String, GlUniform> uniforms;
    private final GlVertexFormat format;
    private final Int2ObjectMap<GlTexture> textureBindings;

    public GlProgram(GlRenderer renderer, @NotNull Identifier identifier, @NotNull Map<String, Texture> textures, @NotNull Collection<@NotNull GlShader> shaders, ShaderMeta metadata) {
        this.renderer = renderer;

        handle = glCreateProgram();
        for (var shader : shaders) {
            glAttachShader(handle, shader.handle());
        }
        glLinkProgram(handle);

        if(glGetProgrami(handle, GL_LINK_STATUS) != GL_TRUE) {
            var error = glGetProgramInfoLog(handle);
            throw new RuntimeException("Failed to link program: " + error);
        }

        this.attributes = getAttributes();
        uniforms = getUniforms(metadata);
        format = renderer.executor().get((state) -> new GlVertexFormat(state, this.attributes, metadata.attributes()));

        var samplers = uniforms.entrySet().stream()
            .filter((entry) -> entry.getValue().type() == AttributeType.SAMPLER)
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        var samplerKeys = samplers.keySet();
        if(!samplerKeys.equals(metadata.textures().keySet())) {
            throw new RuntimeException("Shader for " + identifier + " has unmatched image samplers");
        }
        if(!textures.keySet().containsAll(samplerKeys)) {
            throw new RuntimeException("Textures supplied to shader " + identifier + " where missing elements");
        }

        textureBindings = samplers.entrySet().stream().collect(FastCollectors.toInt2ObjectMap(
            (entry) -> entry.getValue().binding(),
            (entry) -> (GlTexture) textures.get(entry.getKey())
        ));
    }

    public void bind() {
        var state = GlState.get();
        state.bindProgram(handle);
        format.bind(state);
        textureBindings.forEach((binding, texture) -> {
            state.activeTexture(GL_TEXTURE0 + binding);
            state.bindTexture(texture.handle());
        });
    }

    public void release() {
        var state = GlState.get();
        format.release(state);
        if(state.boundProgram() == handle) {
            state.bindProgram(0);
        }
    }

    @Override
    public void close() {
        glDeleteProgram(handle);
    }

    @Override
    @NotNull
    public BufferBuilder builder() {
        return BufferBuilder.create(this, (data, type) -> {
            var buffer = renderer.createBuffer(type, data.remaining());
            buffer.shader(this);
            buffer.upload(data);
            return buffer;
        });
    }

    @Override
    @NotNull
    public GlVertexFormat format() {
        return format;
    }

    public int handle() {
        return handle;
    }

    public Map<String, Attribute> attributes() {
        return attributes;
    }

    public Map<String, GlUniform> uniformMap() {
        return uniforms;
    }

    @NotNull
    @Override
    public Collection<? extends Uniform> uniforms() {
        return uniforms.values();
    }

    private Map<String, Attribute> getAttributes() {
        try(var stack = MemoryStack.stackPush()) {
            var namePointer = stack.malloc(1024);
            var nameLengthPointer = stack.ints(0);
            var sizePointer = stack.ints(0);
            var typePointer = stack.ints(0);
            var limit = glGetProgrami(handle, GL_ACTIVE_ATTRIBUTES);
            return IntStream.range(0, limit)
                .mapToObj((index) -> {
                    MemoryUtil.memSet(namePointer, 0);
                    glGetActiveAttrib(handle, index, nameLengthPointer, sizePointer, typePointer, namePointer);
                    var name = MemoryUtil.memUTF8(namePointer, nameLengthPointer.get(0));
                    var location = glGetAttribLocation(handle, name);
                    return new Attribute(name, location, typePointer.get(0), sizePointer.get(0));
                })
                .collect(Collectors.toUnmodifiableMap(
                    Attribute::name,
                    Function.identity()
                ));
        }
    }

    private Map<String, GlUniform> getUniforms(ShaderMeta metadata) {
        var uniforms = metadata.uniforms();
        var textures = metadata.textures();

        try(var stack = MemoryStack.stackPush()) {
            var limit = glGetProgrami(handle, GL_ACTIVE_UNIFORMS);

            var namePointer = stack.malloc(1024);
            var lengthPointer = stack.ints(0);

            var names = new ArrayList<String>(limit);
            var locations = new IntArrayList(limit);
            for(int i = 0; i < limit; i++) {
                MemoryUtil.memSet(namePointer, 0);
                glGetActiveUniformName(handle, i, lengthPointer, namePointer);
                names.add(MemoryUtil.memUTF8(namePointer, lengthPointer.get(0)));

                locations.add(glGetUniformLocation(handle, namePointer));
            }

            var types = stack.callocInt(limit);
            var indices = stack.callocInt(limit);
            var rawIndices = stack.ints(IntStream.range(0, limit).toArray());
            var bindings = new IntArrayList(limit);
            glGetActiveUniformsiv(handle, rawIndices, GL_UNIFORM_TYPE, types);
            glGetUniformIndices(handle, stack.pointers(names.stream().map(stack::UTF8).toArray(ByteBuffer[]::new)), indices);

            // TODO This is awful, find a better way
            var state = GlState.get();
            state.bindProgram(handle);
            try {
                var bindingsByType = new Int2IntOpenHashMap();
                for(int i = 0; i < limit; i++) {
                    int binding = bindingsByType.compute(types.get(i), (_, value) -> value == null ? 0 : value + 1);
                    glUniform1i(i, binding);
                    bindings.add(binding);
                }
            } finally {
                state.bindProgram(0);
            }

            /*
            @NotNull String name,
            @NotNull AttributeType type,
            int count,
            int stride,
            int location,
            @Nullable UniformUsage usage,
            int binding
             */
            //FIXME Make this somehow work with structs instead of arrays.
            return IntStream.range(0, limit).mapToObj((index) -> {
                    var uniformName = names.get(index);
                    var glType = types.get(index);
                    var type = switch(glType) {
                        case GL_SAMPLER_2D -> AttributeType.SAMPLER;
                        case GL_FLOAT_MAT2, GL_FLOAT_MAT3, GL_FLOAT_MAT4 -> AttributeType.FLOAT;
                        default -> throw new IllegalStateException("Unexpected value: " + glType);
                    };
                    var size = switch(glType) {
                        case GL_SAMPLER_2D -> -1;
                        case GL_FLOAT_MAT2 -> 2 * 2;
                        case GL_FLOAT_MAT3 -> 3 * 3;
                        case GL_FLOAT_MAT4 -> 4 * 4;
                        default -> throw new IllegalStateException("Unexpected value: " + glType);
                    };

                    UniformUsage usage = null;
                    var uniformMeta = uniforms.get(uniformName);
                    if(uniformMeta != null) {
                        usage = uniformMeta.usage();
                    }

                    return new GlUniform(
                        uniformName,
                        type,
                        size,
                        type.size() * size,
                        locations.getInt(index),
                        usage,
                        bindings.getInt(index)
                    );
                })
                .collect(Collectors.toUnmodifiableMap(
                    GlUniform::name,
                    Function.identity()
                ));
        }
    }

    public record Attribute(
        String name,
        int location,
        int type,
        int size
    ) {}
}
