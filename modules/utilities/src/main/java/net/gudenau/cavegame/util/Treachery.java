package net.gudenau.cavegame.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import sun.misc.Unsafe;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * A bunch of hacks, beware the dragons. (This is undocumented on purpose)
 */
public final class Treachery {
    private Treachery() {
        throw new AssertionError();
    }

    private static final Unsafe UNSAFE = findUnsafe();
    private static final MethodHandles.Lookup LOOKUP = createLookup();

    private static Unsafe findUnsafe() {
        final int modifiers = Modifier.STATIC | Modifier.FINAL;

        Set<Throwable> exceptions = new HashSet<>();
        for(var field : Unsafe.class.getDeclaredFields()) {
            if(field.getType() != Unsafe.class || (field.getModifiers() & modifiers) != modifiers) {
                continue;
            }

            try {
                field.setAccessible(true);
                if(field.get(null) instanceof Unsafe unsafe) {
                    return unsafe;
                }
            } catch (Throwable e) {
                exceptions.add(e);
            }
        }

        var exception = new RuntimeException("Failed to find Unsafe");
        exceptions.forEach(exception::addSuppressed);
        throw exception;
    }

    private static MethodHandles.Lookup createLookup() {
        long override = -1;
        {
            var object = allocateInstance(AccessibleObject.class);
            for (long offset = 4; offset < 64; offset++) {
                object.setAccessible(false);
                if(UNSAFE.getBoolean(object, offset)) {
                    continue;
                }
                object.setAccessible(true);
                if(UNSAFE.getBoolean(object, offset)) {
                    override = offset;
                    break;
                }
            }
            if(override == -1) {
                throw new RuntimeException("Failed to find AccessibleObject.override offset");
            }
        }

        try {
            var constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, Class.class, int.class);
            UNSAFE.putBoolean(constructor, override, true);
            return constructor.newInstance(Object.class, null, -1);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate MethodHandles.Lookup", e);
        }
    }

    @NotNull
    @Contract("_ -> param1")
    public static <T> Class<T> ensureInitialized(@NotNull Class<T> type) {
        try {
            LOOKUP.in(type).ensureInitialized(type);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to ensure " + MiscUtils.longClassName(type) + " was initialized", e);
        }
        return type;
    }

    @NotNull
    @Contract("_ -> param1")
    public static Class<?>[] ensureInitialized(@NotNull Class<?> @NotNull ... types) {
        for (var type : types) {
            ensureInitialized(type);
        }
        return types;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static <T> T allocateInstance(@NotNull Class<T> type) {
        try {
            return (T) UNSAFE.allocateInstance(type);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to allocate instance of " + MiscUtils.longClassName(type), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T field(Object owner, String name, Class<T> type) {
        try {
            return (T) LOOKUP.findGetter(owner.getClass(), name, type).invoke(owner);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to get field " + name + " from " + MiscUtils.longClassName(owner.getClass()), e);
        }
    }

    public static void tryInvoke(Object owner, String name, MethodType type, Object... arguments) {
        try {
            LOOKUP.bind(owner, name, type).invokeWithArguments(arguments);
        } catch (Throwable e) {
            new RuntimeException("Failed to invoke " + MiscUtils.longClassName(owner.getClass()) + "." + name + type, e).printStackTrace();
        }
    }

    public static MethodHandle constructor(Class<?> owner, MethodType type) throws NoSuchMethodException {
        try {
            return LOOKUP.findConstructor(owner, type);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Trusted lookup could not access constructor " + MiscUtils.longClassName(owner) + type, e);
        }
    }

    public static MethodHandle unreflect(Method method) {
        try {
            return LOOKUP.unreflect(method);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Trusted lookup could not unreflect " + method, e);
        }
    }

    public static <A> MethodHandle handle(Supplier<A> getter) {
        try {
            return LOOKUP.bind(getter, "get", MethodType.methodType(Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Trusted lookup could not ", e);
        }
    }

    public static MethodHandle findVirtualMethod(Class<?> owner, String name, MethodType type) throws NoSuchMethodException {
        try {
            return LOOKUP.findVirtual(owner, name, type);
        } catch(IllegalAccessException e) {
            throw new RuntimeException("Trusted lookup could not find " + MiscUtils.longClassName(owner) + "::" + name, e);
        }
    }

    private static final MethodHandle ClassLoader$findResource;
    static {
        try {
            ClassLoader$findResource = findVirtualMethod(ClassLoader.class, "findResource", MethodType.methodType(URL.class, String.class, String.class));
        } catch(NoSuchMethodException e) {
            throw new RuntimeException("Failed to find ClassLoader.findResource", e);
        }
    }

    @NotNull
    public static InputStream getResourceAsStream(@NotNull Module module, @NotNull String path) throws IOException {
        if(path.startsWith("/")) {
            path = path.substring(1);
        }

        URL resource;
        try {
            var loader = module.getClassLoader();
            resource = (URL) ClassLoader$findResource.invokeExact(loader, module.getName(), path);
        } catch(Throwable e) {
            throw new RuntimeException("Failed to get resource " + path + " from " + module.getName(), e);
        }

        if(resource == null) {
            throw new FileNotFoundException(module.getName() + "!" + path);
        }

        return resource.openStream();
    }

    public static MethodHandle findVirtualMethodUnchecked(Class<?> owner, String name, MethodType type) {
        try {
            return LOOKUP.findVirtual(owner, name, type);
        } catch(NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Failed to find method " + MiscUtils.longClassName(owner) + "::" + name, e);
        }
    }

    public static MethodHandle findVirtualSetterUnchecked(Class<?> owner, String name, Class<?> type) {
        try {
            return LOOKUP.findSetter(owner, name, type);
        } catch(NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to find setter for " + MiscUtils.longClassName(owner) + "::" + name, e);
        }
    }

    private static final VarHandle Throwable$stackTrace;
    static {
        try {
            Throwable$stackTrace = LOOKUP.findVarHandle(Throwable.class, "stackTrace", StackTraceElement[].class);
        } catch(NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to get handle to Throwable::stackTrace", e);
        }
    }

    public static void stripStackFrames(@NotNull Throwable exception, int strip) {
        var trace = exception.getStackTrace();
        var replacement = new StackTraceElement[trace.length - strip];
        System.arraycopy(trace, strip, replacement, 0, replacement.length);
        Throwable$stackTrace.set(exception, replacement);
    }
}
