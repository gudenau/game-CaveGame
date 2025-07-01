package net.gudenau.cavegame.wooting;

import net.gudenau.cavegame.panama.NativeLayout;
import net.gudenau.cavegame.panama.NativeUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.StructLayout;
import java.lang.invoke.VarHandle;
import java.util.Objects;

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

    public DeviceInfoFFI(@NotNull MemorySegment segment) {
        NativeUtils.ensureNonNull(segment, "segment can't be null");

        // TODO Find a better way, this is hacky
        if(segment.byteSize() == 0) {
            this.segment = segment.reinterpret(LAYOUT.byteSize());
        } else {
            this.segment = segment;
        }
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
        return (short) VENDOR_ID.get(segment, 0);
    }

    /**
     * Retrieves the product_id field from the structure.
     *
     * @return The retrieved value
     */
    public short product_id() {
        return (short) PRODUCT_ID.get(segment, 0);
    }

    /**
     * Retrieves the manufacturer_name field from the structure.
     *
     * @return The retrieved value
     */
    @Nullable
    public String manufacturer_name() {
        return NativeUtils.string((MemorySegment) MANUFACTURER_NAME.get(segment, 0));
    }

    /**
     * Retrieves the device_name field from the structure.
     *
     * @return The retrieved value
     */
    @Nullable
    public String device_name() {
        return NativeUtils.string((MemorySegment) DEVICE_NAME.get(segment, 0));
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
}
