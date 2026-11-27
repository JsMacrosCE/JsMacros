package com.jsmacrosce.jsmacros.client.util;

import com.jsmacrosce.doclet.DocletIgnore;
import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Reads the size of the area a screen lays its content out in, when that area is smaller than the
 * full window.
 * <p>
 * The scripting API exposes {@code getWidth()} and {@code getHeight()} on every screen, so
 * {@code MixinScreen} adds those methods to the screen superclass. Reading the {@code width} and
 * {@code height} fields is correct for ordinary screens, but some screens like Sodium's video
 * settings deliberately reserve a border and publish a smaller content rectangle through their
 * own {@code getDimensions()} method instead.
 * <p>
 * Asking the screen for its rectangle first keeps both kinds of screen correct. Screens that only
 * set the fields fall back to them unchanged, so no mod-specific dependency is needed. The accessors
 * are resolved once per screen class and cached. A screen whose {@code getDimensions()} is missing,
 * static, does not return an object with public {@code int width()} and {@code int height()}
 * accessors, returns {@code null}, or fails while being read keeps the field value.
 */
@ApiStatus.Internal
@DocletIgnore
public final class ScreenDimensions {
    private ScreenDimensions() {
    }

    private record RectAccess(MethodHandle dimensions, MethodHandle width, MethodHandle height) {
        private static final RectAccess NONE = new RectAccess(null, null, null);

        private boolean present() {
            return dimensions != null;
        }
    }

    private static final ClassValue<RectAccess> RECT_ACCESS = new ClassValue<>() {
        @Override
        protected RectAccess computeValue(Class<?> screenClass) {
            Method getDimensions;
            try {
                getDimensions = screenClass.getMethod("getDimensions");
            } catch (NoSuchMethodException | SecurityException e) {
                return RectAccess.NONE;
            }
            int modifiers = getDimensions.getModifiers();
            if (getDimensions.getParameterCount() != 0
                    || Modifier.isStatic(modifiers)
                    || Modifier.isAbstract(modifiers)) {
                return RectAccess.NONE;
            }
            Method width = intAccessor(getDimensions.getReturnType(), "width");
            Method height = intAccessor(getDimensions.getReturnType(), "height");
            if (width == null || height == null) {
                return RectAccess.NONE;
            }
            try {
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                return new RectAccess(
                        lookup.unreflect(getDimensions).asType(MethodType.methodType(Object.class, Object.class)),
                        lookup.unreflect(width).asType(MethodType.methodType(int.class, Object.class)),
                        lookup.unreflect(height).asType(MethodType.methodType(int.class, Object.class)));
            } catch (IllegalAccessException e) {
                return RectAccess.NONE;
            }
        }
    };

    /**
     * Returns the content width {@code screen} publishes through {@code getDimensions()}, or
     * {@code fallback} when it publishes none.
     */
    public static int widthOf(Object screen, int fallback) {
        return read(screen, fallback, true);
    }

    /**
     * Returns the content height {@code screen} publishes through {@code getDimensions()}, or
     * {@code fallback} when it publishes none.
     */
    public static int heightOf(Object screen, int fallback) {
        return read(screen, fallback, false);
    }

    private static int read(Object screen, int fallback, boolean width) {
        RectAccess access = RECT_ACCESS.get(screen.getClass());
        if (!access.present()) {
            return fallback;
        }
        try {
            Object dimensions = access.dimensions().invokeExact(screen);
            if (dimensions == null) {
                return fallback;
            }
            MethodHandle accessor = width ? access.width() : access.height();
            return (int) accessor.invokeExact(dimensions);
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable ignored) {
            // A screen that publishes an unusable rectangle is no worse off than before.
            return fallback;
        }
    }

    private static Method intAccessor(Class<?> type, String name) {
        try {
            Method method = type.getMethod(name);
            if (method.getParameterCount() == 0
                    && method.getReturnType() == int.class
                    && !Modifier.isStatic(method.getModifiers())) {
                return method;
            }
        } catch (NoSuchMethodException | SecurityException ignored) {
        }
        return null;
    }
}
