package net.gudenau.cavegame.renderer.shader;

import net.gudenau.cavegame.renderer.BufferBuilder;
import net.gudenau.cavegame.util.OptionalUtil;
import net.gudenau.cavegame.util.collection.StreamUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

public interface Shader extends AutoCloseable {
    @Override void close();

    @NotNull BufferBuilder builder();

    @NotNull VertexFormat format();

    @NotNull Collection<? extends Uniform> uniforms();

    @NotNull
    default Optional<? extends Uniform> uniformByUsage(@NotNull UniformUsage usage) {
        return StreamUtils.findOne(
            uniforms().stream()
                .filter((uniform) -> uniform.usage() == usage)
        );
    }

    record MVP(
        @NotNull Uniform model,
        @NotNull Uniform view,
        @NotNull Uniform projection
    ) {
        public void upload(@NotNull Consumer<ByteBuffer> consumer) {
            model.upload((buffer) -> {});
            view.upload((buffer) -> {});
            projection.upload((buffer) -> {});
        }
    }

    default Optional<MVP> uniformMvp() {
        var model = uniformByUsage(UniformUsage.MODEL);
        var view = uniformByUsage(UniformUsage.VIEW);
        var projection = uniformByUsage(UniformUsage.PROJECTION);
        return OptionalUtil.allOf(model, view, projection, MVP::new);
    }
}
