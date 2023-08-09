package net.gudenau.cavegame.wooting;

import net.gudenau.cavegame.wooting.internal.Dummy;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static net.gudenau.cavegame.wooting.internal.Functions.DROP_DEVICE_INFO;
import static net.gudenau.cavegame.wooting.internal.Functions.NEW_DEVICE_INFO;

/**
 * Mirrors the `wooting-analog-common.h` header file from the Wooting analog SDK.
 */
public sealed interface WootingCommon permits Dummy {

    /**
     * Device has been connected
     */
    int WootingAnalog_DeviceEventType_Connected = 1;
    /**
     * Device has been disconnected
     */
    int WootingAnalog_DeviceEventType_Disconnected = 2;
    /**
     * Device is of type Keyboard
     */
    int WootingAnalog_DeviceType_Keyboard = 1;
    /**
     * Device is of type Keypad
     */
    int WootingAnalog_DeviceType_Keypad = 2;
    /**
     * Device
     */
    int WootingAnalog_DeviceType_Other = 3;
    /**
     * USB HID Keycodes https://www.usb.org/document-library/hid-usage-tables-112 pg53
     */
    int WootingAnalog_KeycodeType_HID = 0;
    /**
     * Scan code set 1
     */
    int WootingAnalog_KeycodeType_ScanCode1 = 1;
    /**
     * Windows Virtual Keys
     */
    int WootingAnalog_KeycodeType_VirtualKey = 2;
    /**
     * Windows Virtual Keys which are translated to the current keyboard locale
     */
    int WootingAnalog_KeycodeType_VirtualKeyTranslate = 3;
    int WootingAnalogResult_Ok = 1;
    /**
     * Item hasn't been initialized
     */
    int WootingAnalogResult_UnInitialized = -2000;
    /**
     * No Devices are connected
     */
    int WootingAnalogResult_NoDevices = -1999;
    /**
     * Device has been disconnected
     */
    int WootingAnalogResult_DeviceDisconnected = -1998;
    /**
     * Generic Failure
     */
    int WootingAnalogResult_Failure = -1997;
    /**
     * A given parameter was invalid
     */
    int WootingAnalogResult_InvalidArgument = -1996;
    /**
     * No Plugins were found
     */
    int WootingAnalogResult_NoPlugins = -1995;
    /**
     * The specified function was not found in the library
     */
    int WootingAnalogResult_FunctionNotFound = -1994;
    /**
     * No Keycode mapping to HID was found for the given Keycode
     */
    int WootingAnalogResult_NoMapping = -1993;
    /**
     * Indicates that it isn't available on this platform
     */
    int WootingAnalogResult_NotAvailable = -1992;
    /**
     * Indicates that the operation that is trying to be used is for an older version
     */
    int WootingAnalogResult_IncompatibleVersion = -1991;
    /**
     * Indicates that the Analog SDK could not be found on the system
     */
    int WootingAnalogResult_DLLNotFound = -1990;

    @Nullable
    static DeviceInfo new_device_info(short vendor_id, short product_id, @NotNull String manufacturer_name, @NotNull String device_name, long device_id, @MagicConstant(intValues = {WootingAnalog_DeviceType_Keyboard, WootingAnalog_DeviceType_Keypad, WootingAnalog_DeviceType_Other}) int device_type) {
        try(var arena = Arena.ofConfined()) {
            var result = (MemorySegment) NEW_DEVICE_INFO.invokeExact(
                vendor_id,
                product_id,
                arena.allocateUtf8String(manufacturer_name),
                arena.allocateUtf8String(device_name),
                device_id,
                device_type
            );
            return result.equals(MemorySegment.NULL) ? null : new DeviceInfo(result);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to invoke new_device_info");
        }
    }

    static void drop_device_info(@NotNull DeviceInfo device) {
        try {
            DROP_DEVICE_INFO.invokeExact(
                device.value()
            );
        } catch (Throwable e) {
            throw new RuntimeException("Failed to invoke drop_device_info");
        }
    }

    static int extract_error_code(float result) {
        if(result < 0) {
            return Float.floatToRawIntBits(result);
        } else {
            return WootingAnalogResult_Ok;
        }
    }
}
