package net.gudenau.cavegame.renderer.internal;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.gudenau.cavegame.renderer.BufferBuilder;
import net.gudenau.cavegame.renderer.BufferType;
import net.gudenau.cavegame.renderer.GraphicsBuffer;
import net.gudenau.cavegame.renderer.shader.Shader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BiFunction;

//TODO This is trash, fix it.
//TODO Don't assume 16 bit indices
public class BufferBuilderImpl implements BufferBuilder {
    private record Data(float[] elements) {
        public int put(int offset, @NotNull ByteBuffer buffer) {
            for(var element : elements) {
                buffer.putFloat(offset, element);
                offset += Float.BYTES;
            }
            return offset;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;
            Data data = (Data) o;
            return Arrays.equals(elements, data.elements);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(elements);
        }

        @Override
        public String toString() {
            return "Data{" +
                "elements=" + Arrays.toString(elements) +
                '}';
        }
    }

    private final class Vertex {
        private final @Nullable Data position;
        private final @Nullable Data color;

        private Vertex(@Nullable Data position, @Nullable Data color) {
            this.position = position;
            this.color = color;
        }

        public void put(int offset, ByteBuffer buffer) {
            if(position != null) {
                position.put(positionOffset + offset, buffer);
            }
            if(color != null) {
                color.put(colorOffset + offset, buffer);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if(obj == this) return true;
            if(obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Vertex) obj;
            return Objects.equals(this.position, that.position) &&
                Objects.equals(this.color, that.color);
        }

        @Override
        public int hashCode() {
            return Objects.hash(position, color);
        }

        @Override
        public String toString() {
            return "Vertex[" +
                "position=" + position + ", " +
                "color=" + color + ']';
        }
    }

    private final BiFunction<ByteBuffer, BufferType, GraphicsBuffer> factory;
    private final Object2IntMap<Vertex> vertices = new Object2IntOpenHashMap<>();
    private final IntList indices = new IntArrayList();
    private final int stride;

    private final int positionOffset;
    private final int positionSize;
    private Data position;

    private final int colorOffset;
    private final int colorSize;
    private Data color;

    public BufferBuilderImpl(@NotNull Shader shader, @NotNull BiFunction<ByteBuffer, BufferType, GraphicsBuffer> factory) {
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

        var vertex = new Vertex(position, color);
        var size = vertices.size();
        var index = vertices.getOrDefault(vertex, size);
        if(size == index) {
            vertices.put(vertex, index);
        }

        indices.add(index);

        position = null;
        color = null;

        return this;
    }

    @Override
    public @NotNull Map<BufferType, GraphicsBuffer> build() {
        var vertexBuffer = MemoryUtil.memAlloc(vertices.size() * stride);
        var indexBuffer = MemoryUtil.memAlloc(indices.size() * Short.BYTES);

        try {
            vertices.object2IntEntrySet().stream()
                .sorted(Comparator.comparingInt(Object2IntMap.Entry::getIntValue))
                .forEachOrdered((entry) -> {
                    var offset = vertexBuffer.position();
                    entry.getKey().put(offset, vertexBuffer);
                    vertexBuffer.position(offset + stride);
                });
            vertexBuffer.position(0);

            for(int i = 0, size = indices.size(); i < size; i++) {
                indexBuffer.putShort(i << 1, (short) indices.getInt(i));
            }

            return Map.of(
                BufferType.VERTEX, factory.apply(vertexBuffer, BufferType.VERTEX),
                BufferType.INDEX, factory.apply(indexBuffer, BufferType.INDEX)
            );
        } finally {
            MemoryUtil.memFree(indexBuffer);
            MemoryUtil.memFree(vertexBuffer);
        }
    }
}
