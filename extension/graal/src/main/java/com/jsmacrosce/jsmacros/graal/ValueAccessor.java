package com.jsmacrosce.jsmacros.graal;

import org.graalvm.polyglot.Value;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Reflectively accesses the private receiver field of GraalVM {@code Value} objects; this is not a general value-conversion API or a public, stable GraalVM interface.
 * Class initialization locates {@code receiver} on {@code Value}'s superclass and makes its field accessible, so it depends on GraalVM's private field layout and reflective access being permitted. Initialization can fail before {@code getReceiver(Value)} is called, including when its argument would be {@code null}.
 * <pre>
 * const Value = Java.type("org.graalvm.polyglot.Value");
 * const Accessor = Java.type("com.jsmacrosce.jsmacros.graal.ValueAccessor");
 * const receiver = Accessor.getReceiver(Value.asValue("sample"));
 * </pre>
 */
public class ValueAccessor {

    private static final MethodHandle GET_RECEIVER;

    /**
     * Returns the receiver stored in the given GraalVM value. Failures invoking the resolved getter are wrapped in {@code RuntimeException}.
     * @param value GraalVM value to inspect, or {@code null}
     * @return the stored receiver, or {@code null} when {@code value} is {@code null}
     */
    public static Object getReceiver(Value value) {
        if (value == null) return null;
        try {
            return GET_RECEIVER.invoke(value);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    static {
        Class<?> abstractValue = Value.class.getSuperclass();
        try {
            Field f = abstractValue.getDeclaredField("receiver");
            f.setAccessible(true);
            GET_RECEIVER = MethodHandles.lookup().unreflectGetter(f);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(String.join(",", Arrays.stream(abstractValue.getDeclaredFields()).map(Field::getName).toArray(String[]::new)), e);
        }
    }
}
