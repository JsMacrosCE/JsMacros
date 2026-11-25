package com.jsmacrosce;

import com.jsmacrosce.jsmacros.client.util.ScreenDimensions;

/**
 * Checks the content-rectangle fallback used by the screen width and height getters. The helper
 * works on plain objects, so this runs without a client.
 */
public final class ScreenDimensionsTest {
    private static int checks;

    public static void main(String[] args) {
        // A screen that publishes its content rectangle is measured from it.
        check(ScreenDimensions.widthOf(new RectScreen(), 1920) == 640, "content width");
        check(ScreenDimensions.heightOf(new RectScreen(), 1080) == 360, "content height");
        check(ScreenDimensions.widthOf(new InheritedRectScreen(), 1920) == 640, "inherited content rectangle");

        // A screen without a rectangle keeps the size of its fields.
        check(ScreenDimensions.widthOf(new PlainScreen(), 1920) == 1920, "plain width");
        check(ScreenDimensions.heightOf(new PlainScreen(), 1080) == 1080, "plain height");

        // The shape that hid Sodium's buttons: a concrete method on the superclass wins over an
        // interface default, so the screen's own rectangle must still be what callers see.
        check(new InterfaceDefaultScreen().getWidth() == 640, "content width wins over interface default");
        check(new InterfaceDefaultScreen().getHeight() == 360, "content height wins over interface default");

        // Unusable dimension sources fall back instead of failing the caller.
        check(ScreenDimensions.widthOf(new StaticSource(), 1920) == 1920, "static getter ignored");
        check(ScreenDimensions.widthOf(new WrongShape(), 1920) == 1920, "return type without accessors");
        check(ScreenDimensions.widthOf(new NullRect(), 1920) == 1920, "null rectangle");
        check(ScreenDimensions.widthOf(new ThrowingRect(), 1920) == 1920, "throwing accessor");

        System.out.println("ScreenDimensions: " + checks + " checks passed");
    }

    private static void check(boolean condition, String what) {
        checks++;
        if (!condition) {
            throw new AssertionError("ScreenDimensions: " + what);
        }
    }

    public static final class PlainScreen {
    }

    /**
     * Simulates a screen base class that adds width/height getters, and a screen that publishes a
     * smaller content rectangle while also inheriting an interface default for the same getter.
     */
    public static class ScreenBase {
        public int width = 1920;
        public int height = 1080;

        public int getWidth() {
            return ScreenDimensions.widthOf(this, width);
        }

        public int getHeight() {
            return ScreenDimensions.heightOf(this, height);
        }
    }

    public interface Dimensioned {
        default int getWidth() {
            return 640;
        }

        default int getHeight() {
            return 360;
        }
    }

    public static final class InterfaceDefaultScreen extends ScreenBase implements Dimensioned {
        public Rect getDimensions() {
            return new Rect(640, 360);
        }
    }

    public static class RectScreen {
        public Rect getDimensions() {
            return new Rect(640, 360);
        }
    }

    public static final class InheritedRectScreen extends RectScreen {
    }

    public static final class StaticSource {
        public static Rect getDimensions() {
            return new Rect(640, 360);
        }
    }

    public static final class WrongShape {
        public Object getDimensions() {
            return new Object();
        }
    }

    public static final class NullRect {
        public Rect getDimensions() {
            return null;
        }
    }

    public static final class ThrowingRect {
        public BadRect getDimensions() {
            return new BadRect();
        }
    }

    public static final class Rect {
        private final int width;
        private final int height;

        public Rect(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }
    }

    public static final class BadRect {
        public int width() {
            throw new IllegalStateException("unusable rectangle");
        }

        public int height() {
            return 0;
        }
    }
}
