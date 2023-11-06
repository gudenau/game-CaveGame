package net.gudenau.cavegame.wooting.internal;

import net.gudenau.cavegame.panama.NativeBinder;
import net.gudenau.cavegame.panama.NativeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * Handles to the native functions from the Wooting SDK.
 */
public final class Functions {
    private static final NativeBinder BINDER = NativeUtils.load("wooting_analog_wrapper");

    @NotNull
    public static final MethodHandle NEW_DEVICE_INFO = BINDER.bind("new_device_info", ADDRESS, JAVA_SHORT, JAVA_SHORT, ADDRESS, ADDRESS, JAVA_LONG, JAVA_INT);
    @NotNull
    public static final MethodHandle DROP_DEVICE_INFO = BINDER.bind("drop_device_info", null, ADDRESS);
    @NotNull
    public static final MethodHandle WOOTING_ANALOG_INITIALISE = BINDER.bind("wooting_analog_initialise", JAVA_INT);
    @NotNull
    public static final MethodHandle WOOTING_ANALOG_IS_INITIALISED = BINDER.bind("wooting_analog_is_initialised", JAVA_BOOLEAN);
    @NotNull
    public static final MethodHandle WOOTING_ANALOG_UNINITIALISE = BINDER.bind("wooting_analog_uninitialise", JAVA_INT);
    @NotNull
    public static final MethodHandle WOOTING_ANALOG_SET_KEYCODE_MODE = BINDER.bind("wooting_analog_set_keycode_mode", JAVA_INT, JAVA_INT);
    @NotNull
    public static final MethodHandle WOOTING_ANALOG_READ_ANALOG = BINDER.bind("wooting_analog_read_analog", JAVA_FLOAT, JAVA_SHORT);
    @NotNull
    public static final MethodHandle WOOTING_ANALOG_READ_ANALOG_DEVICE = BINDER.bind("wooting_analog_read_analog_device", JAVA_FLOAT, JAVA_SHORT, JAVA_LONG);
    @NotNull
    public static final MethodHandle WOOTING_ANALOG_SET_DEVICE_EVENT_CB = BINDER.bind("wooting_analog_get_connected_devices_info", JAVA_INT, ADDRESS);
    @NotNull
    public static final MethodHandle WOOTING_ANALOG_GET_CONNECTED_DEVICES_INFO = BINDER.bind("wooting_analog_get_connected_devices_info", JAVA_INT, ADDRESS, JAVA_INT);
    @NotNull
    public static final MethodHandle WOOTING_ANALOG_READ_FULL_BUFFER = BINDER.bind("wooting_analog_read_full_buffer", JAVA_INT, ADDRESS, ADDRESS, JAVA_INT);
    @NotNull
    public static final MethodHandle WOOTING_ANALOG_READ_FULL_BUFFER_DEVICE = BINDER.bind("wooting_analog_read_full_buffer_device", JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_LONG);

    private Functions() {
        throw new AssertionError();
    }
}
