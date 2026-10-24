package com.jsmacrosce;

import com.jsmacrosce.jsmacros.client.util.InputCompat;
import com.mojang.blaze3d.platform.InputConstants;

public final class InputCompatTest {
    public static void main(String[] args) {
        for (int code = 0; code <= 348; code++) InputCompat.legacyKey(code);
        check(InputCompat.mouseButton(InputConstants.MOUSE_BUTTON_LEFT) == 0, "left mouse button");
        check(InputCompat.mouseButton(InputConstants.MOUSE_BUTTON_RIGHT) == 1, "right mouse button");
        check(InputCompat.mouseButton(InputConstants.MOUSE_BUTTON_MIDDLE) == 2, "middle mouse button");
        check(InputCompat.legacyKey(65).getName().equals("key.keyboard.a"), "public legacy A code");
        check(InputCompat.legacyKey(0).getName().equals("key.mouse.left"), "public legacy left button");
        check(InputCompat.legacyKey(1).getName().equals("key.mouse.right"), "public legacy right button");
        check(InputCompat.legacyKey(2).getName().equals("key.mouse.middle"), "public legacy middle button");
        check(InputCompat.legacyKey(7).getName().equals("key.mouse.8"), "public legacy eighth button");
        check(InputCompat.legacyKey(340).getName().equals("key.keyboard.left.shift"), "public legacy modifier key");
        //? if >=26.3 {
        /*check(InputCompat.mouseButton(8) == 7, "extra mouse buttons remain zero-based");
        check(InputCompat.legacyKey(330).getValue() == 99, "legacy keypad decimal maps to SDL period");
        check(InputCompat.legacyKey(314) == InputConstants.UNKNOWN, "GLFW F25 has no SDL equivalent");
        for (int nativeMods : new int[]{1, 2, 3}) check(InputCompat.modifiers(nativeMods) == 1, "both shift sides");
        for (int nativeMods : new int[]{64, 128, 192}) check(InputCompat.modifiers(nativeMods) == 2, "both control sides");
        for (int nativeMods : new int[]{256, 512, 768}) check(InputCompat.modifiers(nativeMods) == 4, "both alt sides");
        check(InputCompat.modifiers(3 | 192 | 768 | 3072 | 8192) == 15, "combined modifiers exclude lock bits");
        check(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_A).getName().equals("key.keyboard.a"), "low SDL key is a keyboard input");
        check(InputConstants.Type.MOUSE.getOrCreate(InputConstants.MOUSE_BUTTON_4).getName().equals("key.mouse.4"), "same numeric value can identify a mouse input");
        *///?} else {
        check(InputCompat.modifiers(15) == 15, "legacy modifiers remain unchanged");
        //?}
        System.out.println("InputCompat: native mouse and modifier checks passed");
    }

    private static void check(boolean success, String message) {
        if (!success) throw new AssertionError(message);
    }
}
