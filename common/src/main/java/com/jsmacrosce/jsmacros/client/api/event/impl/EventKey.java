package com.jsmacrosce.jsmacros.client.api.event.impl;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import com.jsmacrosce.doclet.DocletCategory;
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
 * Fires for keyboard and mouse-button input before the game's input handling. The event is
 * cancellable; cancellation causes the game's calling input callback to be cancelled. Unknown keys and
 * repeats are dropped, and with a screen open input may be suppressed by the JsMacros setting or
 * focused-screen/widget rules. Releases for keys pressed before a screen opened are still reported.
 * JsMacros updates its pressed-key tracker before filtering or firing the event, so cancellation
 * does not undo that update. Use {@link #key} to distinguish keyboard keys from mouse buttons.
 * <pre>
 * JsMacros.on("Key", JavaWrapper.methodToJava(function (event) {
 *   if (event.action === 0) {
 *     // 0 is the release, 1 is the press
 *     return;
 *   }
 *   if (event.key === "key.keyboard.j") {
 *     if (event.mods === "key.keyboard.left.control") {
 *       Chat.log("ctrl+J swallowed, the game will not see it");
 *       event.cancel();
 *     }
 *   }
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Inputs/Interactions")
@Event(value = "Key", oldName = "KEY", cancellable = true)
public class EventKey extends BaseEvent {
    static final Minecraft mc = Minecraft.getInstance();
    /**
     * Input action: {@code 1} for a press and {@code 0} for a release. Repeats are filtered before
     * an event is created.
     */
    public final int action;

    /**
     * The game's internal name for the input, rather than its display name. Keyboard and mouse
     * button names share this field (for example, {@code key.keyboard.a} and {@code key.mouse.left}).
     */
    @DocletReplaceReturn("globalThis.Key")
    public final String key;
    /**
     * Held modifiers as left-side game key names joined by {@code +} in shift, control, alt order;
     * empty when none are held. See {@link #getModInt(String)} and {@link #getKeyModifiers(int)}
     * for conversions to and from the bitmask form.
     */
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

    /**
     * Initializes an event with the supplied action and input names without validating or dispatching them.
     * The action is stored unchanged; {@code 1} conventionally means press and {@code 0} release. The
     * strings are also stored unchanged, including arbitrary or {@code null} values. This constructor
     * does not update the pressed-key tracker or apply input filters.
     *
     * @param action action value to store
     * @param key input name to store, conventionally an internal keyboard or mouse-button name
     * @param mods modifier names to store, conventionally joined by {@code +}, or an empty string
     */
    public EventKey(int action, String key, String mods) {
        super(JsMacrosClient.clientCore);
        this.action = action;
        this.key = key;
        this.mods = mods;
    }

    /**
     * Converts a raw keyboard or mouse-button report into a {@code Key} event. It updates
     * JsMacros' pressed-key tracker, applies the screen filters, and then fires the event. The
     * {@code scancode} is unused. The modifier string is formed before the attempted modifier-key
     * adjustment, so that adjustment does not affect the event. Calling this method directly fires
     * a synthetic event; only the game's input callback uses its cancellation result to stop game
     * handling.
     *
     * @param key raw code; values through {@code 7} select mouse buttons, larger values select keyboard keys
     * @param scancode the raw scancode, which this method does not read
     * @param action what happened, using the windowing library's numbering, where {@code 1} is a
     *               press and {@code 0} is a release
     * @param mods the modifier bitmask, where {@code 1} is shift, {@code 2} is control and
     *             {@code 4} is alt
     * @return whether a listener cancelled the event
     */
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

    /**
     * Returns an event-name-prefixed diagnostic string containing the key field only. The key is
     * inserted without escaping, so the result is not guaranteed to be valid JSON.
     *
     * @return a string in the form {@code %s:{"key": "%s"}}, with the event name and raw key substituted
     */
    @Override
    public String toString() {
        return String.format("%s:{\"key\": \"%s\"}", this.getEventName(), key);
    }

    /**
     * Converts modifier bits into the game's left-side modifier names, joined by {@code +} in
     * shift, control, alt order. Other bits are ignored; no set bits produces an empty string.
     *
     * @param mods the modifier bitmask, where {@code 1} is shift, {@code 2} is control and
     *             {@code 4} is alt
     * @return the held modifiers as {@code +} joined game modifier names, or an empty string when
     *         no modifier is set
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
     * Converts {@code +}-separated modifier names to bits: {@code 1} for shift, {@code 2} for
     * control, and {@code 4} for alt. Both left- and right-side names are accepted; unrecognized
     * parts are ignored, and an empty or wholly unrecognized string yields {@code 0}.
     *
     * @param mods the held modifiers as {@code +} joined game modifier names, such as the
     *              {@link #mods} field carries, or an empty string for none
     * @return the combined bitmask, where {@code 1} is shift, {@code 2} is control and {@code 4}
     *         is alt
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
