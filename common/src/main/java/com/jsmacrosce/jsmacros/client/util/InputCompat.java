package com.jsmacrosce.jsmacros.client.util;

import com.jsmacrosce.doclet.DocletIgnore;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
//? if >1.21.8 {
/*import net.minecraft.client.input.KeyEvent;
*///?}

@DocletIgnore
public final class InputCompat {
    private InputCompat() {
    }

    // EventKey.parse(int, ...) predates SDL and retains its GLFW-numbered public contract.
    public static InputConstants.Key legacyKey(int code) {
        //? if >=26.3 {
        /*if (code <= 7) {
            int button = switch (code) {
                case 0 -> InputConstants.MOUSE_BUTTON_LEFT;
                case 1 -> InputConstants.MOUSE_BUTTON_RIGHT;
                case 2 -> InputConstants.MOUSE_BUTTON_MIDDLE;
                default -> code >= 3 ? code + 1 : code;
            };
            return InputConstants.Type.MOUSE.getOrCreate(button);
        }
        String name;
        if (code >= 65 && code <= 90) name = String.valueOf((char) ('a' + code - 65));
        else if (code >= 48 && code <= 57) name = String.valueOf((char) code);
        else if (code >= 290 && code <= 313) name = "f" + (code - 289);
        else if (code >= 320 && code <= 329) name = "keypad." + (code - 320);
        else name = switch (code) {
            case 32 -> "space";
            case 39 -> "apostrophe";
            case 44 -> "comma";
            case 45 -> "minus";
            case 46 -> "period";
            case 47 -> "slash";
            case 59 -> "semicolon";
            case 61 -> "equal";
            case 91 -> "left.bracket";
            case 92 -> "backslash";
            case 93 -> "right.bracket";
            case 96 -> "grave.accent";
            case 161 -> "world.1";
            case 162 -> "world.2";
            case 256 -> "escape";
            case 257 -> "enter";
            case 258 -> "tab";
            case 259 -> "backspace";
            case 260 -> "insert";
            case 261 -> "delete";
            case 262 -> "right";
            case 263 -> "left";
            case 264 -> "down";
            case 265 -> "up";
            case 266 -> "page.up";
            case 267 -> "page.down";
            case 268 -> "home";
            case 269 -> "end";
            case 280 -> "caps.lock";
            case 281 -> "scroll.lock";
            case 282 -> "num.lock";
            case 283 -> "print.screen";
            case 284 -> "pause";
            case 330 -> "keypad.period";
            case 331 -> "keypad.divide";
            case 332 -> "keypad.multiply";
            case 333 -> "keypad.subtract";
            case 334 -> "keypad.add";
            case 335 -> "keypad.enter";
            case 336 -> "keypad.equal";
            case 340 -> "left.shift";
            case 341 -> "left.control";
            case 342 -> "left.alt";
            case 343 -> "left.win";
            case 344 -> "right.shift";
            case 345 -> "right.control";
            case 346 -> "right.alt";
            case 347 -> "right.win";
            case 348 -> "menu";
            default -> null;
        };
        return name == null ? InputConstants.UNKNOWN : InputConstants.getKey("key.keyboard." + name);
        *///?} else {
        return (code <= 7 ? InputConstants.Type.MOUSE : InputConstants.Type.KEYSYM).getOrCreate(code);
        //?}
    }

    // Keep the script modifier bitmask independent of GLFW/SDL's native masks.
    public static int modifiers(int nativeModifiers) {
        //? if >=26.3 {
        /*return ((nativeModifiers & InputConstants.MOD_SHIFT) != 0 ? 1 : 0)
                | ((nativeModifiers & InputConstants.MOD_CONTROL) != 0 ? 2 : 0)
                | ((nativeModifiers & InputConstants.MOD_ALT) != 0 ? 4 : 0)
                | ((nativeModifiers & InputConstants.MOD_SUPER) != 0 ? 8 : 0);
        *///?} else {
        return nativeModifiers;
        //?}
    }

    // Script mouse callbacks retain left=0, right=1, middle=2.
    public static int mouseButton(int nativeButton) {
        //? if >=26.3 {
        /*return switch (nativeButton) {
            case InputConstants.MOUSE_BUTTON_LEFT -> 0;
            case InputConstants.MOUSE_BUTTON_RIGHT -> 1;
            case InputConstants.MOUSE_BUTTON_MIDDLE -> 2;
            default -> nativeButton - 1;
        };
        *///?} else {
        return nativeButton;
        //?}
    }

    //? if >1.21.8 {
    /*public static int scanCode(KeyEvent event) {
        //? if >=26.3 {
        /^return event.key();
        ^///?} else {
        return event.scancode();
        //?}
    }
    *///?}

    public static boolean superDown(Minecraft client) {
        //? if >=26.3 {
        /*return InputConstants.isKeyDown(InputConstants.KEY_LGUI) || InputConstants.isKeyDown(InputConstants.KEY_RGUI);
        *///?} else if >1.21.8 {
        /*return InputConstants.isKeyDown(client.getWindow(), InputConstants.KEY_LSUPER)
                || InputConstants.isKeyDown(client.getWindow(), InputConstants.KEY_RSUPER);
        *///?} else {
        return InputConstants.isKeyDown(client.getWindow().getWindow(), InputConstants.KEY_LWIN)
                || InputConstants.isKeyDown(client.getWindow().getWindow(), InputConstants.KEY_RWIN);
        //?}
    }
}
