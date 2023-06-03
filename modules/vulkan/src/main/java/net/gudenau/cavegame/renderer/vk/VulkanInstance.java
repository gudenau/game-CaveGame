package net.gudenau.cavegame.renderer.vk;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.vulkan.KHRPortabilityEnumeration.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;

public final class VulkanInstance implements AutoCloseable {
    private final VkInstance handle;

    public VulkanInstance() {
        try(var stack = MemoryStack.stackPush()) {
            var appInfo = VkApplicationInfo.calloc(stack);
            appInfo.sType$Default();
            //TODO Make these fields reflect actual game information
            appInfo.pApplicationName(stack.UTF8("CaveGame"));
            appInfo.applicationVersion(VK_MAKE_VERSION(0, 0, 0));
            appInfo.pEngineName(stack.UTF8("CaveGameVk"));
            appInfo.engineVersion(VK_MAKE_VERSION(0, 0, 0));
            appInfo.apiVersion(VK_API_VERSION_1_0);

            var createInfo = VkInstanceCreateInfo.calloc(stack);
            createInfo.sType$Default();
            createInfo.pApplicationInfo(appInfo);

            if(VulkanUtils.IS_OSX) {
                createInfo.flags(createInfo.flags() | VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR);
            }

            var requiredExtensions = VulkanUtils.mergeBuffers(
                stack,
                glfwGetRequiredInstanceExtensions(),
                VulkanUtils.osxInstanceExtensions(stack),
                VulkanUtils.requiredInstanceExtensions(stack)
            );
            validateExtensionSupport(requiredExtensions);
            createInfo.ppEnabledExtensionNames(requiredExtensions);

            var requiredLayers = VulkanUtils.enabledInstanceLayers(stack);
            validateLayerSupport(requiredLayers);
            createInfo.ppEnabledLayerNames(requiredLayers);

            var pointer = stack.pointers(0);
            var result = vkCreateInstance(createInfo, VulkanAllocator.get(), pointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("vkCreateInstance failed: " + VulkanUtils.errorString(result));
            }
            handle = new VkInstance(pointer.get(0), createInfo);
        }
    }

    private void validateLayerSupport(@Nullable PointerBuffer requiredLayers) {
        if(requiredLayers == null) {
            return;
        }

        try(var stack = MemoryStack.stackPush()) {
            var layerCountPointer = stack.ints(0);
            vkEnumerateInstanceLayerProperties(layerCountPointer, null);

            var layerCount = layerCountPointer.get(0);
            var layers = VkLayerProperties.calloc(layerCount, stack);
            vkEnumerateInstanceLayerProperties(layerCountPointer, layers);

            var missingLayers = VulkanUtils.extractSet(requiredLayers);
            for (var layer : layers) {
                missingLayers.remove(layer.layerNameString());
            }

            if(!missingLayers.isEmpty()) {
                throw new RuntimeException("Required Vulkan validation layers are missing: " + String.join(", ", missingLayers));
            }
        }
    }

    private void validateExtensionSupport(@Nullable PointerBuffer requiredExtensions) {
        if(requiredExtensions == null) {
            return;
        }

        try(var stack = MemoryStack.stackPush()) {
            var extensionCountPointer = stack.ints(0);
            vkEnumerateInstanceExtensionProperties((ByteBuffer) null, extensionCountPointer, null);

            var extensionCount = extensionCountPointer.get(0);
            var extensionProps = VkExtensionProperties.calloc(extensionCount, stack);
            vkEnumerateInstanceExtensionProperties((ByteBuffer) null, extensionCountPointer, extensionProps);

            var missingExtensions = VulkanUtils.extractSet(requiredExtensions);
            for (var extensionProp : extensionProps) {
                missingExtensions.remove(extensionProp.extensionNameString());
            }

            if(!missingExtensions.isEmpty()) {
                throw new RuntimeException("Required Vulkan instance extensions are missing: " + String.join(", ", missingExtensions));
            }
        }
    }

    @NotNull
    public VkInstance handle() {
        return handle;
    }

    @Override
    public void close() {
        vkDestroyInstance(handle, VulkanAllocator.get());
    }
}
