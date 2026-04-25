package net.gudenau.cavegame.renderer;

import net.gudenau.cavegame.renderer.screen.Screen;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

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

    @Contract("_ -> this")
    @NotNull ResizeCallback addResizeCallback(@NotNull ResizeCallback callback);
    void removeResizeCallback(@NotNull ResizeCallback callback);

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
