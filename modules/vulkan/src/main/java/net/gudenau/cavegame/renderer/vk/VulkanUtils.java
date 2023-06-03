package net.gudenau.cavegame.renderer.vk;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.gudenau.cavegame.config.Config;
import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.resource.ResourceLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;

public final class VulkanUtils {
    // Thanks Apple
    public static final boolean IS_OSX = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac");

    public static final boolean ENABLE_DEBUG = Config.DEBUG.get();
    public static final Set<String> INSTANCE_VALIDATION_LAYERS = Set.of(
        "VK_LAYER_KHRONOS_validation"
    );
    public static final Set<String> INSTANCE_VALIDATION_EXTENSIONS = Set.of(
        VK_EXT_DEBUG_UTILS_EXTENSION_NAME
    );
    public static final Set<String> DEVICE_EXTENSIONS = Set.of(
        VK_KHR_SWAPCHAIN_EXTENSION_NAME
    );

    @Nullable
    public static PointerBuffer mergeBuffers(@NotNull MemoryStack stack, @Nullable PointerBuffer @NotNull ... buffers) {
        int size = 0;
        for(var buffer : buffers) {
            size += buffer == null ? 0 : buffer.remaining();
        }
        if(size == 0) {
            return null;
        }

        var result = stack.callocPointer(size);
        for(var buffer : buffers) {
            if(buffer == null) {
                continue;
            }
            var position = buffer.position();
            result.put(buffer);
            buffer.position(position);
        }
        return result.position(0);
    }

    @Nullable
    public static PointerBuffer osxInstanceExtensions(@NotNull MemoryStack stack) {
        if(!IS_OSX) {
            return null;
        }

        return stack.pointers(
            stack.UTF8(VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME)
        );
    }

    @NotNull
    public static Set<@NotNull String> extractSet(@NotNull PointerBuffer buffer) {
        var length = buffer.capacity();
        var result = new HashSet<String>(length);
        for(int i = buffer.position(); i < length; i++) {
            result.add(MemoryUtil.memUTF8(buffer.get(i)));
        }
        return result;
    }

    @Nullable
    public static PointerBuffer enabledInstanceLayers(MemoryStack stack) {
        if(!ENABLE_DEBUG) {
            return null;
        }

        var pointers = stack.mallocPointer(INSTANCE_VALIDATION_LAYERS.size());
        INSTANCE_VALIDATION_LAYERS.forEach((layer) -> pointers.put(stack.UTF8(layer)));
        return pointers.position(0);
    }

    @Nullable
    public static PointerBuffer requiredInstanceExtensions(MemoryStack stack) {
        if(!ENABLE_DEBUG) {
            return null;
        }

        var pointers = stack.mallocPointer(INSTANCE_VALIDATION_EXTENSIONS.size());
        INSTANCE_VALIDATION_EXTENSIONS.forEach((extension) -> pointers.put(stack.UTF8(extension)));
        return pointers.position(0);
    }

    public static String errorString(int result) {
        return switch (result) {
            case VK_NOT_READY -> "VK_NOT_READY";
            case VK_TIMEOUT -> "VK_TIMEOUT";
            case VK_EVENT_SET -> "VK_EVENT_SET";
            case VK_EVENT_RESET -> "VK_EVENT_RESET";
            case VK_INCOMPLETE -> "VK_INCOMPLETE";
            case VK_ERROR_OUT_OF_HOST_MEMORY -> "VK_ERROR_OUT_OF_HOST_MEMORY";
            case VK_ERROR_OUT_OF_DEVICE_MEMORY -> "VK_ERROR_OUT_OF_DEVICE_MEMORY";
            case VK_ERROR_INITIALIZATION_FAILED -> "VK_ERROR_INITIALIZATION_FAILED";
            case VK_ERROR_DEVICE_LOST -> "VK_ERROR_DEVICE_LOST";
            case VK_ERROR_MEMORY_MAP_FAILED -> "VK_ERROR_MEMORY_MAP_FAILED";
            case VK_ERROR_LAYER_NOT_PRESENT -> "VK_ERROR_LAYER_NOT_PRESENT";
            case VK_ERROR_EXTENSION_NOT_PRESENT -> "VK_ERROR_EXTENSION_NOT_PRESENT";
            case VK_ERROR_FEATURE_NOT_PRESENT -> "VK_ERROR_FEATURE_NOT_PRESENT";
            case VK_ERROR_INCOMPATIBLE_DRIVER -> "VK_ERROR_INCOMPATIBLE_DRIVER";
            case VK_ERROR_TOO_MANY_OBJECTS -> "VK_ERROR_TOO_MANY_OBJECTS";
            case VK_ERROR_FORMAT_NOT_SUPPORTED -> "VK_ERROR_FORMAT_NOT_SUPPORTED";
            case VK_ERROR_FRAGMENTED_POOL -> "VK_ERROR_FRAGMENTED_POOL";
            case VK_ERROR_UNKNOWN -> "VK_ERROR_UNKNOWN";
            default -> "Unknown error (" + result + ")";
        };
    }

    public static PointerBuffer packStrings(MemoryStack stack, Collection<String> strings) {
        var buffer = stack.callocPointer(strings.size());
        strings.forEach((string) -> buffer.put(stack.UTF8(string)));
        return buffer.position(0);
    }

    @NotNull
    public static IntList extractList(@NotNull IntBuffer buffer) {
        var result = new IntArrayList(buffer.remaining());
        int length = buffer.capacity();
        for(int i = buffer.position(); i < length; i++) {
            result.add(buffer.get(i));
        }
        return result;
    }

    @NotNull
    public static LongList extractList(@NotNull LongBuffer buffer) {
        var result = new LongArrayList(buffer.remaining());
        int length = buffer.capacity();
        for(int i = buffer.position(); i < length; i++) {
            result.add(buffer.get(i));
        }
        return result;
    }

    private VulkanUtils() {
        throw new AssertionError();
    }

    @NotNull
    public static ByteBuffer readIntoNativeBuffer(@NotNull Identifier identifier) {
        try {
            return ResourceLoader.buffer(identifier);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to read resource " + identifier + " into a native buffer", e);
        }
    }
}
