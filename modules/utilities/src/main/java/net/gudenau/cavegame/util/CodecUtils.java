package net.gudenau.cavegame.util;

import net.gudenau.codec.Codec;
import net.gudenau.codec.util.Result;
import net.gudenau.codec.ops.json.JsonOps;
import net.gudenau.json.Json;
import org.jetbrains.annotations.NotNull;

public class CodecUtils {
    public static <T> Result<T> error(@NotNull String message, @NotNull Throwable exception) {
        return Result.error(() -> message + '\n' + MiscUtils.stringifyException(exception));
    }

    public static <T> Result<T> decode(@NotNull Codec<T> codec, @NotNull Json json) {
        return codec.decode(JsonOps.instance(false), json);
    }
}
