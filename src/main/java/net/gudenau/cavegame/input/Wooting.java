package net.gudenau.cavegame.input;

import net.gudenau.cavegame.logger.Logger;
import net.gudenau.cavegame.wooting.DeviceInfoFFI;

import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;

import static net.gudenau.cavegame.wooting.WootingAnalog.*;
import static net.gudenau.cavegame.wooting.WootingCommon.*;

public final class Wooting {
    private static final Logger LOGGER = Logger.forName("Wooting");

    private static volatile boolean initialized = false;
    private static int deviceCount = -1;

    private Wooting() {
        throw new AssertionError();
    }

    public static void initialize() {
        synchronized (Wooting.class) {
            if(!initialized) {
                doInitialize();
            }
        }
    }

    private static void doInitialize() {
        int result = wooting_analog_initialise();
        if(result == 0) {
            wooting_analog_uninitialise();
            LOGGER.info("Failed to find a Wooting device");
            return;
        } else if(result <= 0) {
            LOGGER.info("Failed to initialise Wooting SDK: " + errorName(result));
            return;
        }
        deviceCount = result;

        // Just because
        wooting_analog_set_keycode_mode(WootingAnalog_KeycodeType_HID);

        try(var arena = Arena.ofConfined()) {
            var pointers = arena.allocate(ValueLayout.ADDRESS, deviceCount);
            result = wooting_analog_get_connected_devices_info(pointers);
            if(result != deviceCount) {
                throw new RuntimeException("Wooting SDK returned an unexpected amount of device info");
            } else if(result < 0) {
                throw new RuntimeException("Wooting SDK failed to retrieve device info: " + errorName(result));
            }

            LOGGER.info("Found " + deviceCount + " Wooting compatible keyboard" + (deviceCount == 1 ? "" : "s"));
            for(int i = 0; i < deviceCount; i++) {
                var info = new DeviceInfoFFI(pointers.getAtIndex(ValueLayout.ADDRESS, i));
                LOGGER.debug("%s: %s (%02X:%02X)".formatted(
                    info.manufacturer_name(),
                    info.device_name(),
                    info.vendor_id(),
                    info.product_id()
                ));
            }
        }
    }

    private static String errorName(int result) {
        return switch (result) {
            case WootingAnalogResult_Ok -> "Ok";
            case WootingAnalogResult_UnInitialized -> "UnInitialized";
            case WootingAnalogResult_NoDevices -> "NoDevices";
            case WootingAnalogResult_DeviceDisconnected -> "DeviceDisconnected";
            case WootingAnalogResult_Failure -> "Failure";
            case WootingAnalogResult_InvalidArgument -> "InvalidArgument";
            case WootingAnalogResult_NoPlugins -> "NoPlugins";
            case WootingAnalogResult_FunctionNotFound -> "FunctionNotFound";
            case WootingAnalogResult_NoMapping -> "NoMapping";
            case WootingAnalogResult_NotAvailable -> "NotAvailable";
            case WootingAnalogResult_IncompatibleVersion -> "IncompatibleVersion";
            case WootingAnalogResult_DLLNotFound -> "DLLNotFound";
            default -> "Unknown(" + result + ")";
        };
    }
}
