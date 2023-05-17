package net.gudenau.cavegame.wooting.internal;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * Handles to the native functions from the Wooting SDK.
 */
public interface Functions {
    @NotNull MethodHandle NEW_DEVICE_INFO = NativeUtils.function("new_device_info", ADDRESS, JAVA_SHORT, JAVA_SHORT, ADDRESS, ADDRESS, JAVA_LONG, JAVA_INT);
    @NotNull MethodHandle DROP_DEVICE_INFO = NativeUtils.function("drop_device_info", null, ADDRESS);
    @NotNull MethodHandle WOOTING_ANALOG_INITIALISE = NativeUtils.function("wooting_analog_initialise", JAVA_INT);
    @NotNull MethodHandle WOOTING_ANALOG_IS_INITIALISED = NativeUtils.function("wooting_analog_is_initialised", JAVA_BOOLEAN);
    @NotNull MethodHandle WOOTING_ANALOG_UNINITIALISE = NativeUtils.function("wooting_analog_uninitialise", JAVA_INT);
    @NotNull MethodHandle WOOTING_ANALOG_SET_KEYCODE_MODE = NativeUtils.function("wooting_analog_set_keycode_mode", JAVA_INT, JAVA_INT);
    @NotNull MethodHandle WOOTING_ANALOG_READ_ANALOG = NativeUtils.function("wooting_analog_read_analog", JAVA_FLOAT, JAVA_SHORT);
    @NotNull MethodHandle WOOTING_ANALOG_READ_ANALOG_DEVICE = NativeUtils.function("wooting_analog_read_analog_device", JAVA_FLOAT, JAVA_SHORT, JAVA_LONG);
    @NotNull MethodHandle WOOTING_ANALOG_SET_DEVICE_EVENT_CB = NativeUtils.function("wooting_analog_get_connected_devices_info", JAVA_INT, ADDRESS);
    @NotNull MethodHandle WOOTING_ANALOG_GET_CONNECTED_DEVICES_INFO = NativeUtils.function("wooting_analog_get_connected_devices_info", JAVA_INT, ADDRESS, JAVA_INT);
    @NotNull MethodHandle WOOTING_ANALOG_READ_FULL_BUFFER = NativeUtils.function("wooting_analog_read_full_buffer", JAVA_INT, ADDRESS, ADDRESS, JAVA_INT);
    @NotNull MethodHandle WOOTING_ANALOG_READ_FULL_BUFFER_DEVICE = NativeUtils.function("wooting_analog_read_full_buffer_device", JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_LONG);
}
