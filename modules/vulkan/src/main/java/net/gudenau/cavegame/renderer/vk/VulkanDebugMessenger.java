package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.logger.LogLevel;
import net.gudenau.cavegame.logger.Logger;
import net.gudenau.cavegame.util.Treachery;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;

import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public final class VulkanDebugMessenger implements AutoCloseable {
    private static final Logger LOGGER = Logger.forName("Vulkan");

    @NotNull
    private final VkDebugUtilsMessengerCallbackEXT callback;
    @NotNull
    private final VulkanInstance instance;
    private final long handle;

    public VulkanDebugMessenger(@NotNull VulkanInstance instance) {
        this.instance = instance;

        LOGGER.debug("Creating debug messenger");

        try(var stack = MemoryStack.stackPush()) {
            var createInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
            createInfo.sType$Default();
            createInfo.messageSeverity(
                VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT |
                    VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT |
                    VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                    VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
            );
            createInfo.messageType(
                VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                    VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                    VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
            );
            callback = VkDebugUtilsMessengerCallbackEXT.create(this::callback);
            createInfo.pfnUserCallback(callback);
            createInfo.pUserData(0);

            var pointer = stack.longs(0);
            var result = vkCreateDebugUtilsMessengerEXT(instance.handle(), createInfo, VulkanAllocator.get(), pointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create debug messenger: " + VulkanUtils.errorString(result));
            }
            handle = pointer.get(0);
        }
    }

    private int callback(int messageSeverity, int messageTypes, long callbackDataPointer, long userData) {
        @SuppressWarnings("resource") // Idea is being a little aggressive here
        var callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(callbackDataPointer);

        LogLevel level;
        if(messageSeverity >= VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) {
            level = LogLevel.ERROR;
        } else if(messageSeverity >= VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) {
            level = LogLevel.WARN;
        } else if(messageSeverity >= VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) {
            level = LogLevel.INFO;
        } else {
            level = LogLevel.DEBUG;
        }

        Throwable exception;
        if(level == LogLevel.ERROR) {
            exception = new Throwable("Stacktrace:");
            var trace = exception.getStackTrace();
            int strip;
            for(strip = 1; strip < trace.length; strip++) {
                if(!trace[strip].getClassName().startsWith("org.lwjgl")) {
                    break;
                }
            }
            Treachery.stripStackFrames(exception, strip);
        } else {
            exception = null;
        }

        LOGGER.log(level, callbackData.pMessageString(), exception);

        return VK_FALSE;
    }

    @Override
    public void close() {
        vkDestroyDebugUtilsMessengerEXT(instance.handle(), handle, VulkanAllocator.get());
        callback.close();
    }
}
