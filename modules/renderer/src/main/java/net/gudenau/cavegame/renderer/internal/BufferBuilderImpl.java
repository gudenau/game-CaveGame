package net.gudenau.cavegame.renderer.internal;

import net.gudenau.cavegame.renderer.BufferBuilder;
import net.gudenau.cavegame.renderer.GraphicsBuffer;
import net.gudenau.cavegame.renderer.shader.Shader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

//TODO This is trash, fix it.
public class BufferBuilderImpl implements BufferBuilder {
    private record Data(float[] elements) {
        public int put(int offset, @NotNull ByteBuffer buffer) {
            for(var element : elements) {
                buffer.putFloat(offset, element);
                offset += Float.BYTES;
            }
            return offset;
        }
    }

    private static final class Vertex {
        private final @Nullable Data position;
        private final @Nullable Data color;

        private Vertex(@Nullable Data position, @Nullable Data color) {
            this.position = position;
            this.color = color;
        }

        public void put(int offset, ByteBuffer buffer) {
            if(position != null) {
                offset = position.put(offset, buffer);
            }
            if(color != null) {
                color.put(offset, buffer);
            }
        }
    }

    private final Function<ByteBuffer, GraphicsBuffer> factory;
    private final List<Vertex> vertices = new ArrayList<>();
    private final int stride;

    private final int positionOffset;
    private final int positionSize;
    private Data position;

    private final int colorOffset;
    private final int colorSize;
    private Data color;

    public BufferBuilderImpl(@NotNull Shader shader, @NotNull Function<ByteBuffer, GraphicsBuffer> factory) {
        this.factory = factory;

        var format = shader.format();
        var position = format.position().orElse(null);
        int stride = 0;
        if(position != null) {
            positionOffset = position.offset();
            positionSize = position.count();
            stride += positionSize * Float.BYTES;
        } else {
            positionOffset = -1;
            positionSize = -1;
        }

        var color = format.color().orElse(null);
        if(color != null) {
            colorOffset = color.offset();
            colorSize = color.count();
            stride += colorSize * Float.BYTES;
        } else {
            colorOffset = -1;
            colorSize = -1;
        }

        this.stride = stride;
    }

    @NotNull
    @Override
    public BufferBuilder position(float x, float y, float z) {
        if(positionOffset != -1) {
            position = new Data(Arrays.copyOf(new float[] {x, y, z}, positionSize));
        }

        return this;
    }

    @NotNull
    @Override
    public BufferBuilder color(float r, float g, float b, float a) {
        if(colorOffset != -1) {
            color = new Data(Arrays.copyOf(new float[] {r, g, b, a}, colorSize));
        }

        return this;
    }

    @Override
    public @NotNull BufferBuilder next() {
        if((positionOffset == -1) != (position == null)) {
            throw new IllegalStateException("Missing position data");
        }
        if((colorOffset == -1) != (color == null)) {
            throw new IllegalStateException("Missing color data");
        }

        vertices.add(new Vertex(position, color));
        position = null;
        color = null;

        return this;
    }

    @NotNull
    @Override
    public GraphicsBuffer build() {
        int size = vertices.size() * stride;
        var buffer = MemoryUtil.memAlloc(size);
        try {
            int offset = 0;
            for(Vertex vertex : vertices) {
                vertex.put(offset, buffer);
                offset += stride;
            }
            return factory.apply(buffer);
        } finally {
            MemoryUtil.memFree(buffer);
        }
    }
}
