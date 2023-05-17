package net.gudenau.cavegame.wooting;

import net.gudenau.cavegame.wooting.internal.Dummy;
import net.gudenau.cavegame.wooting.internal.NativeUtils;
import org.intellij.lang.annotations.MagicConstant;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static net.gudenau.cavegame.wooting.WootingCommon.*;
import static net.gudenau.cavegame.wooting.internal.Functions.*;

/**
 * Mirrors the `wooting-analog-wrapper.h` header file from the Wooting analog SDK.
 */
public sealed interface WootingAnalog permits Dummy {
    /**
     * Initialises the Analog SDK, this needs to be successfully called before any other functions
     * of the SDK can be called
     *
     * # Expected Returns
     * * `ret>=0`: Meaning the SDK initialised successfully and the number indicates the number of devices that were found on plugin initialisation
     * * `NoPlugins`: Meaning that either no plugins were found or some were found but none were successfully initialised
     * * `FunctionNotFound`: The SDK is either not installed or could not be found
     * * `IncompatibleVersion`: The installed SDK is incompatible with this wrapper as they are on different Major versions
     */
    static int wooting_analog_initialise() {
        try {
            return (int) WOOTING_ANALOG_INITIALISE.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to invoke wooting_analog_initialise", e);
        }
    }

    /**
     * Returns a bool indicating if the Analog SDK has been initialised
     */
    static boolean wooting_analog_is_initialised() {
        try {
            return (boolean) WOOTING_ANALOG_IS_INITIALISED.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to invoke wooting_analog_is_initialised", e);
        }
    }


    /**
     * Uninitialises the SDK, returning it to an empty state, similar to how it would be before first initialisation
     * # Expected Returns
     * * `Ok`: Indicates that the SDK was successfully uninitialised
     */
    static int wooting_analog_uninitialise() {
        try {
            return (int) WOOTING_ANALOG_UNINITIALISE.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to invoke wooting_analog_uninitialise", e);
        }
    }

    /**
     * Sets the type of Keycodes the Analog SDK will receive (in `read_analog`) and output (in `read_full_buffer`).
     *
     * By default, the mode is set to HID
     *
     * # Notes
     * * `VirtualKey` and `VirtualKeyTranslate` are only available on Windows
     * * With all modes except `VirtualKeyTranslate`, the key identifier will point to the physical key on the standard layout. i.e. if you ask for the Q key, it will be the key right to tab regardless of the layout you have selected
     * * With `VirtualKeyTranslate`, if you request Q, it will be the key that inputs Q on the current layout, not the key that is Q on the standard layout.
     *
     * # Expected Returns
     * * `Ok`: The Keycode mode was changed successfully
     * * `InvalidArgument`: The given `KeycodeType` is not one supported by the SDK
     * * `NotAvailable`: The given `KeycodeType` is present, but not supported on the current platform
     * * `UnInitialized`: The SDK is not initialised
     */
    static int wooting_analog_set_keycode_mode(@MagicConstant(intValues = {WootingAnalog_KeycodeType_HID, WootingAnalog_KeycodeType_ScanCode1, WootingAnalog_KeycodeType_VirtualKey, WootingAnalog_KeycodeType_VirtualKeyTranslate}) int mode) {
        try {
            return (int) WOOTING_ANALOG_SET_KEYCODE_MODE.invokeExact(
                mode
            );
        } catch (Throwable e) {
            throw new RuntimeException("Failed to invoke wooting_analog_set_keycode_mode", e);
        }
    }

    /**
     *  Reads the Analog value of the key with identifier `code` from any connected device. The set of key identifiers that is used
     *  depends on the Keycode mode set using `wooting_analog_set_mode`.
     *
     *  # Examples
     *  ```ignore
     *  wooting_analog_set_mode(KeycodeType::ScanCode1);
     *  wooting_analog_read_analog(0x10); //This will get you the value for the key which is Q in the standard US layout (The key just right to tab)
     *
     *  wooting_analog_set_mode(KeycodeType::VirtualKey); //This will only work on Windows
     *  wooting_analog_read_analog(0x51); //This will get you the value for the key that is Q on the standard layout
     *
     *  wooting_analog_set_mode(KeycodeType::VirtualKeyTranslate);
     *  wooting_analog_read_analog(0x51); //This will get you the value for the key that inputs Q on the current layout
     *  ```
     *
     *  # Expected Returns
     *  The float return value can be either a 0->1 analog value, or (if <0) is part of the WootingAnalogResult enum, which is how errors are given back on this call.
     *  So if the value is below 0, you should cast it as WootingAnalogResult to see what the error is.
     *  * `0.0f - 1.0f`: The Analog value of the key with the given id `code`
     *  * `WootingAnalogResult::NoMapping`: No keycode mapping was found from the selected mode (set by wooting_analog_set_mode) and HID.
     *  * `WootingAnalogResult::UnInitialized`: The SDK is not initialised
     *  * `WootingAnalogResult::NoDevices`: There are no connected devices
     */
    static float wooting_analog_read_analog(short code) {
        try {
            return (float) WOOTING_ANALOG_READ_ANALOG.invokeExact(code);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to invoke wooting_analog_read_analog", e);
        }
    }

    /**
     * Reads the Analog value of the key with identifier `code` from the device with id `device_id`. The set of key identifiers that is used
     * depends on the Keycode mode set using `wooting_analog_set_mode`.
     *
     * The `device_id` can be found through calling `wooting_analog_device_info` and getting the DeviceID from one of the DeviceInfo structs
     *
     * # Expected Returns
     * The float return value can be either a 0->1 analog value, or (if <0) is part of the WootingAnalogResult enum, which is how errors are given back on this call.
     * So if the value is below 0, you should cast it as WootingAnalogResult to see what the error is.
     * * `0.0f - 1.0f`: The Analog value of the key with the given id `code` from device with id `device_id`
     * * `WootingAnalogResult::NoMapping`: No keycode mapping was found from the selected mode (set by wooting_analog_set_mode) and HID.
     * * `WootingAnalogResult::UnInitialized`: The SDK is not initialised
     * * `WootingAnalogResult::NoDevices`: There are no connected devices with id `device_id`
     */
    static float wooting_analog_read_analog_device(short code, long device_id) {
        try {
            return (float) WOOTING_ANALOG_READ_ANALOG_DEVICE.invokeExact(code, device_id);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to invoke wooting_analog_read_analog_device", e);
        }
    }

    //TODO Not implemented in the SDK yet so why bother implementing them?
    /* *
     * Set the callback which is called when there is a DeviceEvent. Currently these events can either be Disconnected or Connected(Currently not properly implemented).
     * The callback gets given the type of event `DeviceEventType` and a pointer to the DeviceInfo struct that the event applies to
     *
     * # Notes
     * * You must copy the DeviceInfo struct or its data if you wish to use it after the callback has completed, as the memory will be freed straight after
     * * The execution of the callback is performed in a separate thread so it is fine to put time consuming code and further SDK calls inside your callback
     *
     * # Expected Returns
     * * `Ok`: The callback was set successfully
     * * `UnInitialized`: The SDK is not initialised
     */
    /*
    public static int wooting_analog_set_device_event_cb(@NotNull DeviceEventCallback cb) {

    }
     */

    /**
     * Clears the device event callback that has been set
     *
     * # Expected Returns
     * * `Ok`: The callback was cleared successfully
     * * `UnInitialized`: The SDK is not initialised
WootingAnalogResult wooting_analog_clear_device_event_cb(void);
    /**
     * Fills up the given `buffer`(that has length `len`) with pointers to the DeviceInfo structs for all connected devices (as many that can fit in the buffer)
     *
     * # Notes
     * * The memory of the returned structs will only be kept until the next call of `get_connected_devices_info`, so if you wish to use any data from them, please copy it or ensure you don't reuse references to old memory after calling `get_connected_devices_info` again.
     *
     * # Expected Returns
     * Similar to wooting_analog_read_analog, the errors and returns are encoded into one type. Values >=0 indicate the number of items filled into the buffer, with `<0` being of type WootingAnalogResult
     * * `ret>=0`: The number of connected devices that have been filled into the buffer
     * * `WootingAnalogResult::UnInitialized`: Indicates that the AnalogSDK hasn't been initialised
     */
    static int wooting_analog_get_connected_devices_info(DeviceInfoFFI.Buffer buffer) {
        try {
            return (int) WOOTING_ANALOG_GET_CONNECTED_DEVICES_INFO.invokeExact(
                buffer.segment(),
                buffer.capacity()
            );
        } catch (Throwable e) {
            throw new RuntimeException("Failed to invoke wooting_analog_get_connected_devices_info", e);
        }
    }

    /**
     * Reads all the analog values for pressed keys for all devices and combines their values, filling up `code_buffer` with the
     * keycode identifying the pressed key and fills up `analog_buffer` with the corresponding float analog values. i.e. The analog
     * value for they key at index 0 of code_buffer, is at index 0 of analog_buffer.
     *
     * # Notes
     * * `len` is the length of code_buffer & analog_buffer, if the buffers are of unequal length, then pass the lower of the two, as it is the max amount of
     * key & analog value pairs that can be filled in.
     * * The codes that are filled into the `code_buffer` are of the KeycodeType set with wooting_analog_set_mode
     * * If two devices have the same key pressed, the greater value will be given
     * * When a key is released it will be returned with an analog value of 0.0f in the first read_full_buffer call after the key has been released
     *
     * # Expected Returns
     * Similar to other functions like `wooting_analog_device_info`, the return value encodes both errors and the return value we want.
     * Where >=0 is the actual return, and <0 should be cast as WootingAnalogResult to find the error.
     * * `>=0` means the value indicates how many keys & analog values have been read into the buffers
     * * `WootingAnalogResult::UnInitialized`: Indicates that the AnalogSDK hasn't been initialised
     * * `WootingAnalogResult::NoDevices`: Indicates no devices are connected
     */
    static int wooting_analog_read_full_buffer(ShortBuffer code_buffer, FloatBuffer analog_buffer) {
        try {
            return (int) WOOTING_ANALOG_READ_FULL_BUFFER.invokeExact(
                NativeUtils.segment(code_buffer),
                NativeUtils.segment(analog_buffer),
                Math.min(code_buffer.remaining(), analog_buffer.remaining())
            );
        } catch (Throwable e) {
            throw new RuntimeException("Failed to invoke wooting_analog_read_full_buffer", e);
        }
    }

    /**
     * Reads all the analog values for pressed keys for the device with id `device_id`, filling up `code_buffer` with the
     * keycode identifying the pressed key and fills up `analog_buffer` with the corresponding float analog values. i.e. The analog
     * value for they key at index 0 of code_buffer, is at index 0 of analog_buffer.
     *
     * # Notes
     * * `len` is the length of code_buffer & analog_buffer, if the buffers are of unequal length, then pass the lower of the two, as it is the max amount of
     * key & analog value pairs that can be filled in.
     * * The codes that are filled into the `code_buffer` are of the KeycodeType set with wooting_analog_set_mode
     * * When a key is released it will be returned with an analog value of 0.0f in the first read_full_buffer call after the key has been released
     *
     * # Expected Returns
     * Similar to other functions like `wooting_analog_device_info`, the return value encodes both errors and the return value we want.
     * Where >=0 is the actual return, and <0 should be cast as WootingAnalogResult to find the error.
     * * `>=0` means the value indicates how many keys & analog values have been read into the buffers
     * * `WootingAnalogResult::UnInitialized`: Indicates that the AnalogSDK hasn't been initialised
     * * `WootingAnalogResult::NoDevices`: Indicates the device with id `device_id` is not connected
     */
    static int wooting_analog_read_full_buffer_device(ShortBuffer code_buffer, FloatBuffer analog_buffer, int len, long device_id) {
        try {
            return (int) WOOTING_ANALOG_READ_FULL_BUFFER_DEVICE.invokeExact(
                NativeUtils.segment(code_buffer),
                NativeUtils.segment(analog_buffer),
                Math.min(code_buffer.remaining(), analog_buffer.remaining()),
                device_id
            );
        } catch (Throwable e) {
            throw new RuntimeException("Failed to invoke wooting_analog_read_full_buffer_device", e);
        }
    }
}
