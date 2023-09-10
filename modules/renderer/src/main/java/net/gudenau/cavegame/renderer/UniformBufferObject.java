package net.gudenau.cavegame.renderer;

import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.util.Objects;

public record UniformBufferObject(
    @NotNull Matrix4f model,
    @NotNull Matrix4f view,
    @NotNull Matrix4f proj
) {
    public UniformBufferObject() {
        this(new Matrix4f(), new Matrix4f(), new Matrix4f());
    }

    public UniformBufferObject {
        Objects.requireNonNull(model, "model can't be null");
        Objects.requireNonNull(view, "view can't be null");
        Objects.requireNonNull(proj, "proj can't be null");
    }

    public void write(ByteBuffer buffer) {
        var position = buffer.position();
        model.get(position, buffer);
        view.get(position + Float.BYTES * 16, buffer);
        proj.get(position + Float.BYTES * 16 * 2, buffer);
    }
}
