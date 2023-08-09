package net.gudenau.cavegame.wooting.internal;

import net.gudenau.cavegame.wooting.HidKeyCodes;
import net.gudenau.cavegame.wooting.WootingCommon;

import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.Modifier;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.gudenau.cavegame.wooting.WootingAnalog.*;
import static net.gudenau.cavegame.wooting.WootingCommon.*;

/**
 * Basic test code.
 */
public class Test {
    private static float check(float result) {
        check(WootingCommon.extract_error_code(result));
        return result;
    }

    private static int check(int result) {
        if(result >= 0) {
            return result;
        }

        throw new RuntimeException("Wooting API call failed: " + result);
    }

    public static void main(String[] args) {
        Map<Short, String> keyNames = Stream.of(HidKeyCodes.class.getDeclaredFields())
            .filter((field) -> field.getType() == short.class && Modifier.isStatic(field.getModifiers()))
            .peek((field) -> field.setAccessible(true))
            .collect(Collectors.toUnmodifiableMap(
                (field) -> {
                    try {
                        return (short) field.get(null);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                },
                (field) -> field.getName().substring(4).toLowerCase().replaceAll("_", " ")
            ));

        try(var arena = Arena.ofConfined()) {
            check(wooting_analog_initialise());

            var keyCodes = arena.allocateArray(ValueLayout.JAVA_SHORT, 4).asByteBuffer().order(ByteOrder.nativeOrder()).asShortBuffer();
            var values = arena.allocateArray(ValueLayout.JAVA_FLOAT, 4).asByteBuffer().order(ByteOrder.nativeOrder()).asFloatBuffer();

            for(;;) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    break;
                }

                var count = check(wooting_analog_read_full_buffer(keyCodes, values));
                if(count == 0) {
                    continue;
                }

                for(int i = 0; i < count; i++) {
                    System.out.print(keyNames.getOrDefault(keyCodes.get(i), String.valueOf(keyCodes.get(i))) + ": " + values.get(i) + "\t");
                }
                System.out.println();
            }
        } finally {
            check(wooting_analog_uninitialise());
        }
    }
}
