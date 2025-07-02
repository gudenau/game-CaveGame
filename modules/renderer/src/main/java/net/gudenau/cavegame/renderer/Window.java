package net.gudenau.cavegame.renderer;

import net.gudenau.cavegame.renderer.screen.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.IntConsumer;

public interface Window extends AutoCloseable {
    void visible(boolean visible);
    boolean visible();

    boolean closeRequested();

    void bind();
    void release();

    void flip();

    @Override void close();

    void position(int x, int y);

    @NotNull Size size();

    void resizeCallback(@Nullable ResizeCallback callback);

    void pushScreen(@NotNull Screen screen);

    @NotNull Screen popScreen(boolean close);

    @NotNull
    default Screen popScreen() {
        return popScreen(true);
    }

    @NotNull Optional<Screen> currentScreen();

    record Size(int width, int height) {}

    @FunctionalInterface
    interface ResizeCallback {
        void invoke(Window window, int width, int height);
    }
}
