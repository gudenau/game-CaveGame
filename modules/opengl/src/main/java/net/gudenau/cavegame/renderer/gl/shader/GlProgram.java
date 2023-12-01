package net.gudenau.cavegame.renderer.gl.shader;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.gudenau.cavegame.renderer.BufferBuilder;
import net.gudenau.cavegame.renderer.gl.GlRenderer;
import net.gudenau.cavegame.renderer.gl.GlState;
import net.gudenau.cavegame.renderer.shader.Shader;
import net.gudenau.cavegame.renderer.shader.ShaderMeta;
import net.gudenau.cavegame.renderer.shader.VertexFormat;
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

public final class GlProgram implements Shader {
    private final GlRenderer renderer;
    private final int handle;
    private final Map<String, Attribute> attributes;
    private final Map<String, Uniform> uniforms;
    private final GlVertexFormat format;
    private final Int2ObjectMap<Texture> textureBindings;

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
        uniforms = getUniforms();
        format = new GlVertexFormat(this.attributes, metadata.attributes());

        var samplers = uniforms.entrySet().stream()
            .filter((entry) -> entry.getValue().type() == GL_SAMPLER_2D)
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
            (entry) -> textures.get(entry.getKey())
        ));
    }

    public void bind() {
        GlState.get().bindProgram(handle);
    }

    public void release() {
        var state = GlState.get();
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
    public VertexFormat format() {
        return format;
    }

    public int handle() {
        return handle;
    }

    public Map<String, Attribute> attributes() {
        return attributes;
    }

    public Map<String, Uniform> uniforms() {
        return uniforms;
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

    private Map<String, Uniform> getUniforms() {
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
            bind();
            try {
                var bindingsByType = new Int2IntOpenHashMap();
                for(int i = 0; i < limit; i++) {
                    int binding = bindingsByType.compute(types.get(i), (key, value) -> value == null ? 0 : value + 1);
                    glUniform1i(i, binding);
                    bindings.add(binding);
                }
            } finally {
                release();
            }

            return IntStream.range(0, limit).mapToObj((index) -> new Uniform(
                    names.get(index),
                    locations.getInt(index),
                    types.get(index),
                    bindings.getInt(index)
                ))
                .collect(Collectors.toUnmodifiableMap(
                    Uniform::name,
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

    public record Uniform(
        String name,
        int location,
        int type,
        int binding
    ) {}
}
