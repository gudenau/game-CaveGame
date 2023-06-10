package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.renderer.GlfwWindow;
import org.jetbrains.annotations.NotNull;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class VkWindow extends GlfwWindow {
    public VkWindow(@NotNull String title, int width, int height) {
        super(title, width, height, null);
    }

    @Override
    protected long createWindow(String title, int width, int height, Object user) {
        glfwDefaultWindowHints();

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);

        //TODO Remove VK string
        return glfwCreateWindow(width, height, title + " (VK)", NULL, NULL);
    }

    @Override
    public void bind() {}

    @Override
    public void release() {}

    @Override
    public void flip() {}
}
