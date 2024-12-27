package net.gudenau.cavegame.renderer.vk;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.gudenau.cavegame.config.Config;
import net.gudenau.cavegame.logger.Logger;
import net.gudenau.cavegame.util.SharedLock;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkAllocationCallbacks;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.NULL;

public final class VulkanAllocator {
    private static final boolean ENABLED = Config.DEBUG_VK_ALLOCATOR.get();

    private static final VkAllocationCallbacks CALLBACKS = VkAllocationCallbacks.calloc()
        .pfnAllocation(VulkanAllocator::allocate)
        .pfnReallocation(VulkanAllocator::reallocate)
        .pfnFree(VulkanAllocator::free);

    @NotNull
    public static VkAllocationCallbacks get() {
        return CALLBACKS;
    }

    private record Allocation(
        long address,
        long size,
        long alignment
    ) {
        public ByteBuffer buffer() {
            return MemoryUtil.memByteBuffer(address, (int) size);
        }
    }

    private static final Logger LOGGER = Logger.forName("Vulkan");
    private static final SharedLock LOCK = new SharedLock();
    private static final Long2ObjectMap<Allocation> ALLOCATIONS = new Long2ObjectOpenHashMap<>();
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOCK.read(() -> {
                if(ALLOCATIONS.isEmpty()) {
                    return;
                }

                var builder = new StringBuilder("Leaked memory:\n");
                ALLOCATIONS.values().forEach((allocation) -> {
                    var address = allocation.address();
                    var size = allocation.size();
                    builder.append("    0x%016X 0x%016X\n".formatted(address, size));
                });
                LOGGER.warn(builder.toString());
            });
        }, "Vulkan leak nag"));
    }

    private static long allocate(long user, long size, long alignment, int scope) {
        ByteBuffer buffer;
        try {
            buffer = MemoryUtil.memAlignedAlloc((int) alignment, (int) size);
        } catch(OutOfMemoryError _) {
            buffer = null;
        }
        if(buffer == null) {
            return NULL;
        }

        var address = MemoryUtil.memAddress(buffer);
        var allocation = new Allocation(address, size, alignment);
        var original = LOCK.write(() -> ALLOCATIONS.put(address, allocation));
        if(original != null) {
            throw new AssertionError("Somehow a duplicate address was allocated: 0x%016X".formatted(address));
        }

        return address;
    }

    private static long reallocate(long user, long original, long size, long alignment, int scope) {
        if(original == NULL) {
            return allocate(user, size, alignment, scope);
        }
        if(size == 0) {
            free(user, original);
            return NULL;
        }

        var existing = LOCK.read(() -> ALLOCATIONS.get(original));
        if(existing == null) {
            throw new AssertionError("reallocate called on an unknown address: 0x%016X".formatted(original));
        }
        if(existing.alignment() != alignment) {
            return NULL;
        }

        var replacement = allocate(user, size, alignment, scope);
        if(replacement == NULL) {
            return NULL;
        }

        MemoryUtil.memCopy(
            original,
            replacement,
            Math.min(existing.size(), size)
        );

        free(user, original);

        return replacement;
    }

    private static void free(long user, long memory) {
        var allocation = LOCK.write(() -> ALLOCATIONS.remove(memory));
        if(allocation == null) {
            throw new AssertionError("Attempt to free non-tracked pointer: 0x%016X".formatted(memory));
        }
        MemoryUtil.memAlignedFree(allocation.buffer());
    }
}
