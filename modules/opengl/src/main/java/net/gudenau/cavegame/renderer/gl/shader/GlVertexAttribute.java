package net.gudenau.cavegame.renderer.gl.shader;

import net.gudenau.cavegame.renderer.shader.AttributeType;
import net.gudenau.cavegame.renderer.shader.AttributeUsage;
import net.gudenau.cavegame.renderer.shader.ShaderMeta;
import net.gudenau.cavegame.renderer.shader.VertexAttribute;
import org.jetbrains.annotations.NotNull;

import static org.lwjgl.opengl.GL33C.*;

public record GlVertexAttribute(
    @NotNull String name,
    @NotNull AttributeType type,
    int count,
    int location,
    @NotNull AttributeUsage usage,
    int offset
) implements VertexAttribute {
    @SuppressWarnings({"DuplicateBranchesInSwitch", "DataFlowIssue"})
    public GlVertexAttribute(@NotNull GlProgram.Attribute input, @NotNull ShaderMeta.Attribute meta, int offset) {
        this(
            input.name(),
            switch(input.type()) {
                case GL_FLOAT_VEC2, GL_FLOAT_VEC3, GL_FLOAT_VEC4, GL_FLOAT_MAT2, GL_FLOAT_MAT3, GL_FLOAT_MAT4 -> AttributeType.FLOAT;
                case GL_INT_VEC2, GL_INT_VEC3, GL_INT_VEC4 -> throw new IllegalArgumentException("ints are not supported");
                case GL_BOOL, GL_BOOL_VEC2, GL_BOOL_VEC3, GL_BOOL_VEC4 -> throw new IllegalArgumentException("bools are not supported");
                case GL_SAMPLER_1D, GL_SAMPLER_2D, GL_SAMPLER_3D, GL_SAMPLER_CUBE, GL_SAMPLER_1D_SHADOW, GL_SAMPLER_2D_SHADOW -> AttributeType.SAMPLER;
                default -> throw new AssertionError("Unknown type");
            },
            switch(input.type()) {
                case GL_FLOAT_VEC2 -> 2;
                case GL_FLOAT_VEC3 -> 3;
                case GL_FLOAT_VEC4 -> 4;
                case GL_INT_VEC2 -> 2;
                case GL_INT_VEC3 -> 3;
                case GL_INT_VEC4 -> 4;
                case GL_BOOL -> 1;
                case GL_BOOL_VEC2 -> 2;
                case GL_BOOL_VEC3 -> 3;
                case GL_BOOL_VEC4 -> 4;
                case GL_FLOAT_MAT2 -> 2 * 2;
                case GL_FLOAT_MAT3 -> 3 * 3;
                case GL_FLOAT_MAT4 -> 4 * 4;
                case GL_SAMPLER_1D -> 1;
                case GL_SAMPLER_2D -> 2;
                case GL_SAMPLER_3D -> 3;
                case GL_SAMPLER_CUBE -> 0;
                case GL_SAMPLER_1D_SHADOW -> 1;
                case GL_SAMPLER_2D_SHADOW -> 2;
                default -> throw new AssertionError("Unknown type");
            },
            input.location(),
            meta.usage(),
            offset
        );
    }

    @Override
    public int stride() {
        return type().size() * count();
    }
}
