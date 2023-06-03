package net.gudenau.cavegame.renderer.vk;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.VK10.*;

public final class VulkanSurface implements AutoCloseable {
    @NotNull
    private final VulkanInstance instance;
    private final long handle;

    public VulkanSurface(@NotNull VkWindow window, @NotNull VulkanInstance instance) {
        this.instance = instance;

        try(var stack = MemoryStack.stackPush()) {
            var pointer = stack.longs(0);
            var result = glfwCreateWindowSurface(instance.handle(), window.handle(), VulkanAllocator.get(), pointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan surface: " + VulkanUtils.errorString(result));
            }
            handle = pointer.get(0);
        }
    }

    public long handle() {
        return handle;
    }

    @Override
    public void close() {
        vkDestroySurfaceKHR(instance.handle(), handle, VulkanAllocator.get());
    }
}
