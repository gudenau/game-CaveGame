package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.util.ExclusiveLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Stack;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.shaderc.Shaderc.*;

public final class ShaderCompiler implements AutoCloseable {
    private static final ExclusiveLock LOCK = new ExclusiveLock();
    private static final Stack<ShaderCompiler> COMPILERS = new Stack<>();

    private static final long BASE_OPTIONS = shaderc_compile_options_initialize();
    static {
        if(BASE_OPTIONS == NULL) {
            throw new RuntimeException("Failed to create base shaderc options instance");
        }

        shaderc_compile_options_set_source_language(BASE_OPTIONS, shaderc_source_language_glsl);
        shaderc_compile_options_set_optimization_level(BASE_OPTIONS, shaderc_optimization_level_performance);
        shaderc_compile_options_set_target_env(BASE_OPTIONS, shaderc_target_env_vulkan, shaderc_env_version_vulkan_1_0);
        shaderc_compile_options_set_warnings_as_errors(BASE_OPTIONS);
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(ShaderCompiler::cleanup, "ShaderCompiler cleanup"));
    }

    @NotNull
    public static ShaderCompiler acquire() {
        return LOCK.lock(() -> {
            if(COMPILERS.isEmpty()) {
                return new ShaderCompiler();
            } else {
                return COMPILERS.pop();
            }
        });
    }

    private final long handle;
    private final long options;

    private ShaderCompiler() {
        handle = shaderc_compiler_initialize();
        if(handle == NULL) {
            throw new RuntimeException("Failed to create shaderc instance");
        }

        options = shaderc_compile_options_clone(BASE_OPTIONS);
        if(options == NULL) {
            shaderc_compiler_release(handle);
            throw new RuntimeException("Failed to clone shaderc options instance");
        }
    }

    @NotNull
    public ByteBuffer compile(@NotNull ByteBuffer source, int kind, @NotNull String filename) {
        long result = NULL;
        try(var stack = MemoryStack.stackPush()) {
            result = shaderc_compile_into_spv(
                handle,
                source,
                kind,
                stack.UTF8(filename),
                stack.UTF8("main"),
                options
            );

            var status = shaderc_result_get_compilation_status(result);
            if(status != shaderc_compilation_status_success) {
                throw new RuntimeException("Failed to compile shader " + filename + ": " + shaderc_result_get_error_message(result));
            }

            var length = shaderc_result_get_length(result);
            if(length > Integer.MAX_VALUE) {
                throw new RuntimeException("Failed to compile shader " + filename + ": Output binary was too large");
            }

            var output = shaderc_result_get_bytes(result, length);
            if(output == null) {
                throw new RuntimeException("Failed to compile shader " + filename + ": Output was null?");
            }

            var buffer = MemoryUtil.memAlloc(output.limit());
            MemoryUtil.memCopy(output, buffer);
            return buffer;
        } finally {
            if(result != NULL) {
                shaderc_result_release(result);
            }
        }
    }

    @Override
    public void close() {
        LOCK.lock((Runnable)() -> COMPILERS.push(this));
    }

    private static void cleanup() {
        LOCK.lock(() -> COMPILERS.forEach((compiler) -> {
            shaderc_compiler_release(compiler.handle);
            shaderc_compile_options_release(compiler.options);
        }));
        shaderc_compile_options_release(BASE_OPTIONS);
    }
}
