package net.gudenau.cavegame.wooting;

import net.gudenau.cavegame.wooting.internal.NativeLayout;
import net.gudenau.cavegame.wooting.internal.NativeUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.*;
import java.lang.invoke.VarHandle;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Information about a Wooting device.
 *
 * @param segment The native segment
 */
public record DeviceInfoFFI(@NotNull MemorySegment segment) {
    /**
     * The layout of this structure.
     */
    static final StructLayout LAYOUT;

    /**
     * u16 vendor_id
     */
    private static final VarHandle VENDOR_ID;

    /**
     * u16 product_id
     */
    private static final VarHandle PRODUCT_ID;

    /**
     * char* manufacturer_name
     */
    private static final VarHandle MANUFACTURER_NAME;

    /**
     * char* device_name
     */
    private static final VarHandle DEVICE_NAME;

    /**
     * WootingAnalog_DeviceID device_id
     */
    private static final VarHandle DEVICE_ID;

    /**
     * enum WootingAnalog_DeviceType device_type
     */
    private static final VarHandle DEVICE_TYPE;

    static {
        var layout = NativeLayout.builder()
            .u16("vendor_id")
            .u16("product_id")
            .stringPointer("manufacturer_name")
            .stringPointer("device_name")
            .u64("device_id")
            .enumeration("device_type")
            .build();

        LAYOUT = layout.layout();

        VENDOR_ID = layout.handle("vendor_id");
        PRODUCT_ID = layout.handle("product_id");
        MANUFACTURER_NAME = layout.handle("manufacturer_name");
        DEVICE_NAME = layout.handle("device_name");
        DEVICE_ID = layout.handle("device_id");
        DEVICE_TYPE = layout.handle("device_type");
    }

    public DeviceInfoFFI {
        NativeUtils.ensureNonNull(segment, "segment can't be null");
    }

    /**
     * Allocates the native memory for this structure from the provided allocator.
     *
     * @param allocator The allocator to use
     */
    public DeviceInfoFFI(@NotNull SegmentAllocator allocator) {
        this(Objects.requireNonNull(allocator, "allocator can't be null").allocate(LAYOUT));
    }

    /**
     * Retrieves the vendor_id field from the structure.
     *
     * @return The retrieved value
     */
    public short vendor_id() {
        return (short) VENDOR_ID.get(segment);
    }

    /**
     * Retrieves the product_id field from the structure.
     *
     * @return The retrieved value
     */
    public short product_id() {
        return (short) PRODUCT_ID.get(segment);
    }

    /**
     * Retrieves the manufacturer_name field from the structure.
     *
     * @return The retrieved value
     */
    @Nullable
    public String manufacturer_name() {
        return NativeUtils.string((MemorySegment) MANUFACTURER_NAME.get(segment));
    }

    /**
     * Retrieves the device_name field from the structure.
     *
     * @return The retrieved value
     */
    @Nullable
    public String device_name() {
        return NativeUtils.string((MemorySegment) DEVICE_NAME.get(segment));
    }

    /**
     * Retrieves the device_id field from the structure.
     *
     * @return The retrieved value
     */
    public long device_id() {
        return (long) DEVICE_ID.get(segment);
    }

    /**
     * Retrieves the device_type field from the structure.
     *
     * @return The retrieved value
     */
    public int device_type() {
        return (int) DEVICE_TYPE.get(segment);
    }

    /**
     * Retrieves the vendor_id field from the structure.
     *
     * @param value The value to set
     * @return this
     */
    @Contract("_ -> this")
    @NotNull
    public DeviceInfoFFI vendor_id(short value) {
        VENDOR_ID.set(segment, value);
        return this;
    }

    /**
     * Retrieves the product_id field from the structure.
     *
     * @param value The value to set
     * @return this
     */
    @Contract("_ -> this")
    @NotNull
    public DeviceInfoFFI product_id(short value) {
        PRODUCT_ID.set(segment, value);
        return this;
    }

    /**
     * Retrieves the manufacturer_name field from the structure.
     *
     * @param value The value to set
     * @return this
     */
    @Contract("_ -> this")
    @NotNull
    public DeviceInfoFFI manufacturer_name(@NotNull MemorySegment value) {
        MANUFACTURER_NAME.set(segment, Objects.requireNonNull(value, "value can't be null"));
        return this;
    }

    /**
     * Retrieves the device_name field from the structure.
     *
     * @param value The value to set
     * @return this
     */
    @Contract("_ -> this")
    @NotNull
    public DeviceInfoFFI device_name(String value) {
        DEVICE_NAME.set(segment, Objects.requireNonNull(value, "value can't be null"));
        return this;
    }

    /**
     * Retrieves the device_id field from the structure.
     *
     * @param value The value to set
     * @return this
     */
    @Contract("_ -> this")
    @NotNull
    public DeviceInfoFFI device_id(long value) {
        DEVICE_ID.set(segment, value);
        return this;
    }

    /**
     * Retrieves the device_type field from the structure.
     *
     * @param value The value to set
     * @return this
     */
    @Contract("_ -> this")
    @NotNull
    public DeviceInfoFFI device_type(int value) {
        DEVICE_TYPE.set(segment, value);
        return this;
    }

    /**
     * A buffer of multiple consecutive device info structures.
     */
    public static final class Buffer implements Iterable<DeviceInfoFFI> {
        /**
         * An indexable version of {@link DeviceInfoFFI#VENDOR_ID}
         */
        private static final VarHandle VENDOR_ID;

        /**
         * An indexable version of {@link DeviceInfoFFI#PRODUCT_ID}
         */
        private static final VarHandle PRODUCT_ID;

        /**
         * An indexable version of {@link DeviceInfoFFI#MANUFACTURER_NAME}
         */
        private static final VarHandle MANUFACTURER_NAME;

        /**
         * An indexable version of {@link DeviceInfoFFI#DEVICE_NAME}
         */
        private static final VarHandle DEVICE_NAME;

        /**
         * An indexable version of {@link DeviceInfoFFI#DEVICE_ID}
         */
        private static final VarHandle DEVICE_ID;

        /**
         * An indexable version of {@link DeviceInfoFFI#DEVICE_TYPE}
         */
        private static final VarHandle DEVICE_TYPE;

        static {
            //TODO Find a more elegant way of handling this.
            var layout = MemoryLayout.sequenceLayout(LAYOUT);
            VENDOR_ID = layout.varHandle(MemoryLayout.PathElement.sequenceElement(), MemoryLayout.PathElement.groupElement("vendor_id"));
            PRODUCT_ID = layout.varHandle(MemoryLayout.PathElement.sequenceElement(), MemoryLayout.PathElement.groupElement("product_id"));
            MANUFACTURER_NAME = layout.varHandle(MemoryLayout.PathElement.sequenceElement(), MemoryLayout.PathElement.groupElement("manufacturer_name"));
            DEVICE_NAME = layout.varHandle(MemoryLayout.PathElement.sequenceElement(), MemoryLayout.PathElement.groupElement("device_name"));
            DEVICE_ID = layout.varHandle(MemoryLayout.PathElement.sequenceElement(), MemoryLayout.PathElement.groupElement("device_id"));
            DEVICE_TYPE = layout.varHandle(MemoryLayout.PathElement.sequenceElement(), MemoryLayout.PathElement.groupElement("device_type"));
        }

        /**
         * The backing native memory for this structure.
         */
        @NotNull
        private final MemorySegment segment;

        /**
         * The size of this buffer in elements.
         */
        private final int capacity;

        /**
         * Creates a new buffer with the capacity set based on the size of the memory segment.
         *
         * @param segment The backing memory for this structure
         */
        public Buffer(@NotNull MemorySegment segment) {
            this.segment = NativeUtils.ensureNonNull(segment, "segment can't be null");
            this.capacity = (int) Math.min(segment.byteSize() / DeviceInfoFFI.LAYOUT.byteSize(), Integer.MAX_VALUE);
        }

        /**
         * Allocates a new buffer from the provided allocator.
         *
         * @param allocator The allocator to use
         * @param size The element count of the buffer
         */
        public Buffer(@NotNull SegmentAllocator allocator, int size) {
            this(Objects.requireNonNull(allocator, "allocator can't be null").allocateArray(DeviceInfoFFI.LAYOUT, size));
        }

        /**
         * Gets the native memory for this buffer.
         *
         * @return The native memory for this buffer
         */
        @NotNull
        public MemorySegment segment() {
            return segment;
        }

        /**
         * The amount of elements in this buffer
         *
         * @return The capacity of this buffer
         */
        public int capacity() {
            return capacity;
        }

        @NotNull
        @Override
        public Iterator<DeviceInfoFFI> iterator() {
            return new Iterator<>() {
                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < capacity;
                }

                @Override
                public DeviceInfoFFI next() {
                    return new DeviceInfoFFI(segment.asSlice(index++ * LAYOUT.byteSize(), LAYOUT.byteSize()));
                }
            };
        }

        /**
         * Creates a {@link Stream} of device info structures.
         *
         * @return The created stream
         */
        @NotNull
        public Stream<DeviceInfoFFI> stream() {
            return StreamSupport.stream(spliterator(), false);
        }

        /**
         * Retrieves the vendor_id field from a structure in this buffer.
         *
         * @param index The index into this buffer
         * @return The retrieved value
         */
        public short vendor_id(int index) {
            return (short) VENDOR_ID.get(segment, (long) index);
        }

        /**
         * Retrieves the product_id field from a structure in this buffer.
         *
         * @param index The index into this buffer
         * @return The retrieved value
         */
        public short product_id(int index) {
            return (short) PRODUCT_ID.get(segment, (long) index);
        }

        /**
         * Retrieves the manufacturer_name field from a structure in this buffer.
         *
         * @param index The index into this buffer
         * @return The retrieved value
         */
        @Nullable
        public String manufacturer_name(int index) {
            return NativeUtils.string((MemorySegment) MANUFACTURER_NAME.get(segment, (long) index));
        }

        /**
         * Retrieves the device_name field from a structure in this buffer.
         *
         * @param index The index into this buffer
         * @return The retrieved value
         */
        @Nullable
        public String device_name(int index) {
            return NativeUtils.string((MemorySegment) DEVICE_NAME.get(segment, (long) index));
        }

        /**
         * Retrieves the device_id field from a structure in this buffer.
         *
         * @param index The index into this buffer
         * @return The retrieved value
         */
        public long device_id(int index) {
            return (long) DEVICE_ID.get(segment, (long) index);
        }

        /**
         * Retrieves the device_type field from a structure in this buffer.
         *
         * @param index The index into this buffer
         * @return The retrieved value
         */
        public int device_type(int index) {
            return (int) DEVICE_TYPE.get(segment, (long) index);
        }

        /**
         * Retrieves the vendor_id field from a structure in this buffer.
         *
         * @param index The index into this buffer
         * @param value The value to set
         * @return this
         */
        @Contract("_, _ -> this")
        @NotNull
        public DeviceInfoFFI.Buffer vendor_id(int index, short value) {
            VENDOR_ID.set(segment, (long) index, value);
            return this;
        }

        /**
         * Retrieves the product_id field from a structure in this buffer.
         *
         * @param index The index into this buffer
         * @param value The value to set
         * @return this
         */
        @Contract("_, _ -> this")
        @NotNull
        public DeviceInfoFFI.Buffer product_id(int index, short value) {
            PRODUCT_ID.set(segment, (long) index, value);
            return this;
        }

        /**
         * Retrieves the manufacturer_name field from a structure in this buffer.
         *
         * @param index The index into this buffer
         * @param value The value to set
         * @return this
         */
        @Contract("_, _ -> this")
        @NotNull
        public DeviceInfoFFI.Buffer manufacturer_name(int index, @NotNull MemorySegment value) {
            MANUFACTURER_NAME.set(segment, (long) index, Objects.requireNonNull(value, "value can't be null"));
            return this;
        }

        /**
         * Retrieves the device_name field from a structure in this buffer.
         *
         * @param index The index into this buffer
         * @param value The value to set
         * @return this
         */
        @Contract("_, _ -> this")
        @NotNull
        public DeviceInfoFFI.Buffer device_name(int index, String value) {
            DEVICE_NAME.set(segment, (long) index, Objects.requireNonNull(value, "value can't be null"));
            return this;
        }

        /**
         * Retrieves the device_id field from a structure in this buffer.
         *
         * @param index The index into this buffer
         * @param value The value to set
         * @return this
         */
        @Contract("_, _ -> this")
        @NotNull
        public DeviceInfoFFI.Buffer device_id(int index, long value) {
            DEVICE_ID.set(segment, (long) index, value);
            return this;
        }

        /**
         * Retrieves the device_type field from a structure in this buffer.
         *
         * @param index The index into this buffer
         * @param value The value to set
         * @return this
         */
        @Contract("_, _ -> this")
        @NotNull
        public DeviceInfoFFI.Buffer device_type(int index, int value) {
            DEVICE_TYPE.set(segment, (long) index, value);
            return this;
        }
    }
}
