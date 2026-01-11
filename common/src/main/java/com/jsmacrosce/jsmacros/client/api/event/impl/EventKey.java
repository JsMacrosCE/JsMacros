package com.jsmacrosce.jsmacros.client.api.event.impl;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.access.IRecipeBookWidget;
import com.jsmacrosce.jsmacros.client.api.library.impl.FKeyBind;
import com.jsmacrosce.jsmacros.client.config.ClientConfigV2;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;
import com.jsmacrosce.wagyourgui.BaseScreen;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "Key", oldName = "KEY", cancellable = true)
public class EventKey extends BaseEvent {
    static final Minecraft mc = Minecraft.getInstance();
    public final int action;

    @DocletReplaceReturn("globalThis.Key")
    public final String key;
    @DocletReplaceReturn("KeyMods")
    @DocletDeclareType(name = "KeyMods", type =
            """
            KeyMod.shift | KeyMod.ctrl | KeyMod.alt
            | `${KeyMod.shift}+${KeyMod.ctrl | KeyMod.alt}`
            | `${KeyMod.ctrl}+${KeyMod.alt}`
            | `${KeyMod.shift}+${KeyMod.ctrl}+${KeyMod.alt}`
            declare namespace KeyMod {
                type shift = 'key.keyboard.left.shift';
                type ctrl = 'key.keyboard.left.control';
                type alt = 'key.keyboard.left.alt';
            }
            """
    )
    public final String mods;

    private static final Set<Integer> wasNullOnDown = new HashSet<>();

    public EventKey(int action, String key, String mods) {
        super(JsMacrosClient.clientCore);
        this.action = action;
        this.key = key;
        this.mods = mods;
    }

    public static boolean parse(int key, int scancode, int action, int mods) {
        InputConstants.Key keycode;
        if (key <= 7) {
            keycode = InputConstants.Type.MOUSE.getOrCreate(key);
        } else {
            keycode = InputConstants.Type.KEYSYM.getOrCreate(key);
        }

        String keyStr = keycode.getName();
        String modsStr = getKeyModifiers(mods);

        if (keycode == InputConstants.UNKNOWN) {
            return false;
        }

        if (action == 1) {
            FKeyBind.KeyTracker.press(keycode);
        } else {
            FKeyBind.KeyTracker.unpress(keycode);
        }

        if (mc.screen != null) {
            if (action != 0 || !wasNullOnDown.contains(key)) {
                ClientConfigV2 config = JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class);
                if (config.disableKeyWhenScreenOpen) {
                    return false;
                }
                if (mc.screen instanceof BaseScreen) {
                    return false;
                }
                GuiEventListener focused = mc.screen.getFocused();
                if (focused instanceof EditBox) {
                    return false;
                }
                if (focused instanceof RecipeBookComponent && ((IRecipeBookWidget) focused).jsmacros_isSearching()) {
                    return false;
                }
            }
        } else if (action == 1) {
            wasNullOnDown.add(key);
        }

        if (action == 0) {
            wasNullOnDown.remove(key);
        }

        // fix mods if it was a mod key
        if (action == 1) {
            if (key == 340 || key == 344) {
                mods -= 1;
            } else if (key == 341 || key == 345) {
                mods -= 2;
            } else if (key == 342 || key == 346) {
                mods -= 4;
            }
        }

        EventKey ev = new EventKey(action, keyStr, modsStr);
        ev.trigger();
        return ev.isCanceled();
    }

    @Override
    public String toString() {
        return String.format("%s:{\"key\": \"%s\"}", this.getEventName(), key);
    }

    /**
     * turn an {@link Integer Integer} for key modifiers into a Translation Key.
     *
     * @param mods
     * @return
     */
    public static String getKeyModifiers(int mods) {
        String s = "";
        if ((mods & 1) == 1) {
            s += "key.keyboard.left.shift";
        }
        if ((mods & 2) == 2) {
            if (s.length() > 0) {
                s += "+";
            }
            s += "key.keyboard.left.control";
        }
        if ((mods & 4) == 4) {
            if (s.length() > 0) {
                s += "+";
            }
            s += "key.keyboard.left.alt";
        }
        return s;
    }

    /**
     * turn a Translation Key for modifiers into an {@link Integer Integer}.
     *
     * @param mods
     * @return
     */
    public static int getModInt(String mods) {
        int i = 0;
        String[] modArr = mods.split("\\+");
        for (String mod : modArr) {
            switch (mod) {
                case "key.keyboard.left.shift":
                case "key.keyboard.right.shift":
                    i |= 1;
                    break;
                case "key.keyboard.left.control":
                case "key.keyboard.right.control":
                    i |= 2;
                    break;
                case "key.keyboard.left.alt":
                case "key.keyboard.right.alt":
                    i |= 4;
                    break;
                default:
            }
        }
        return i;

    }
}
