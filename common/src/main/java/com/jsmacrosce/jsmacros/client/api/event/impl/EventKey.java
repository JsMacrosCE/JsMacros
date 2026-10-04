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
 * Fires for every key press and every mouse button press and release the client receives, at the
 * very start of the game's own input handling, before the game has done anything with the input.
 * Keyboard keys and mouse buttons arrive on the same event, so a click is matched on
 * {@link #key} rather than on a field of its own.<br>
 * This event is cancellable, and cancelling stops the game from doing anything with that input at
 * all. A cancelled key press never reaches the screen that is open, never sets or clicks a key
 * binding, and never triggers the fullscreen, screenshot or debug shortcuts. A cancelled mouse
 * click is not forwarded to the screen either, and does not set or click the attack, use and
 * pick-block bindings. The client keeps running and later input still fires normally.<br>
 * Some input never reaches the event at all, so a listener can see a release without ever having
 * seen the matching press. The key code the game reports when it does not know the key is dropped,
 * and so is auto-repeat, which is why {@link #action} is only ever {@code 1} or {@code 0} in
 * practice. When a screen is open, the event is dropped if any of the following holds: the
 * {@code disableKeyWhenScreenOpen} option in the JsMacros settings is on, which it is by default;
 * the screen is one of JsMacros' own screens; the focused widget is a text field; or the focused
 * widget is the recipe book while it is in search mode. The one exception is the release of a key
 * that was pressed while no screen was open, which is always reported, so that a key held when a
 * screen opens does not stay stuck down in the game after the screen closes.<br>
 * JsMacros' own pressed-key tracker is updated before the event is raised, and is not rolled back
 * when the event is cancelled or dropped, so
 * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FKeyBind#getPressedKeys() getPressedKeys()}
 * reports the key as pressed either way.
 * example:
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
     * what happened to the key, using the windowing library's own numbering: {@code 1} is the press
     * and {@code 0} is the release.<br>
     * Auto-repeat is the third state in that numbering and is filtered out before the event is
     * created, so a listener never sees it. The field is a plain {@code int} rather than a named
     * type, so compare it against {@code 1} and {@code 0} directly instead of expecting names.
     */
    public final int action;

    /**
     * the name the game uses for the key that was pressed or released, which is the same internal
     * name the game's own key binding screen works with rather than a display name.<br>
     * Letters, digits and the function keys come through as {@code key.keyboard.a},
     * {@code key.keyboard.7} and {@code key.keyboard.f5} and so on, and a key code the game has
     * no name for comes through as {@code key.keyboard.} followed by the raw code. The first eight
     * mouse button codes come through as {@code key.mouse.left}, {@code key.mouse.right},
     * {@code key.mouse.middle} and then {@code key.mouse.4} through {@code key.mouse.8}, so the
     * name is the only thing telling a mouse button apart from a key.<br>
     * Script type definitions point this at the {@code Key} type, which is an alias for
     * {@code string}, so comparing it against a name is always allowed.
     */
    @DocletReplaceReturn("globalThis.Key")
    public final String key;
    /**
     * which modifier keys were held down at the moment this input arrived, as the game's own
     * modifier names joined with {@code +}. There is one name per held modifier, always in the
     * order shift, control, alt, and the names are the left hand ones the game uses everywhere,
     * so a right shift is reported as {@code key.keyboard.left.shift} as well.<br>
     * The string is empty when no modifier was held. Note that the declared type shipped for this
     * field lists the seven non-empty combinations but not the empty one, so the field can hold a
     * value the declared type does not allow and comparing it against {@code ""} is not allowed by
     * the type definitions even though it always works at runtime.<br>
     * See {@link #getModInt(String)} for the same set of modifiers in the bitmask form the game
     * uses internally, and {@link #getKeyModifiers(int)} for the conversion back.
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

    public EventKey(int action, String key, String mods) {
        super(JsMacrosClient.clientCore);
        this.action = action;
        this.key = key;
        this.mods = mods;
    }

    /**
     * builds a {@code Key} event out of one raw input report and fires it. This is the entry point
     * the game's keyboard and mouse mixins call, and it is what decides whether the game ever sees
     * the input at all, so it is public so that a script could raise one too.<br>
     * It turns the key code into a name, treating a code of seven or below as a mouse button and
     * anything above as a keyboard key, and turns the modifier bitmask into the string form
     * {@link #getKeyModifiers(int)} builds. It then updates JsMacros' own pressed-key tracker,
     * applies the screen rules described on the class, and only then raises the event.<br>
     * The {@code scancode} parameter is not used for anything here, it is only part of the shape
     * the game's input callback has. The guard against the game's unknown key can never fire: it
     * compares the resolved key for identity with {@code InputConstants.UNKNOWN}, which is the
     * cached keyboard entry for a key code of {@code -1}, and only a key code of {@code -1} can
     * resolve to it, which the {@code key > 7} branch that builds keyboard keys rules out. A key
     * code of {@code -1} instead takes the mouse branch, where the game would name it
     * {@code key.mouse.0}, so an unmapped key code simply produces {@code key.keyboard.} followed
     * by the raw code like any other keyboard key. There is also an attempt to drop the modifier
     * bit belonging to a modifier key that is being pressed right now, but it runs after the
     * modifier string has already been built and never reaches the event, so a shift press is
     * reported with shift held.<br>
     * Note that firing the event from a script is not the same as consuming the input: the game
     * only stops handling it when the mixin that called this method also cancels its own callback,
     * so a script that raises a synthetic event still leaves the real input alone. And cancelling
     * the event raised here only sets the returned value, it does not undo the tracker update that
     * already happened.<br>
     * This is a static entry point for the game's own input handling rather than part of the
     * script-facing shape of the event, and the shipped type definitions leave it off the
     * {@code Key} interface, so a script has no typed way of calling it by name. The table below
     * is a worked reference rather than runnable code, and it shows what it does to a raw input
     * report, with the raw report on the left of each arrow and the event that comes out of it
     * on the right:
     * <pre>
     * // a raw input report, and the Key event that comes out of it:
     * //   key 65, action 1, mods 0 -> action 1, key "key.keyboard.a", mods ""
     * //   key 0,  action 1, mods 0 -> action 1, key "key.mouse.left", mods ""
     * //   key 0,  action 0, mods 2 -> action 0, key "key.mouse.left", mods "key.keyboard.left.control"
     * </pre>
     *
     * @param key the raw key code as the windowing library reports it, where a value of seven or
     *            below is a mouse button and anything above is a keyboard key
     * @param scancode the raw scancode, which this method does not read
     * @param action what happened, using the windowing library's numbering, where {@code 1} is a
     *               press and {@code 0} is a release
     * @param mods the modifier bitmask, where {@code 1} is shift, {@code 2} is control and
     *             {@code 4} is alt
     * @return whether a listener cancelled the event, which is what the calling mixin uses to
     *         decide whether to stop the game from handling the input
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

    @Override
    public String toString() {
        return String.format("%s:{\"key\": \"%s\"}", this.getEventName(), key);
    }

    /**
     * turns the modifier bitmask the windowing library reports into the string form this class uses
     * everywhere else, which is the game's own modifier names joined with {@code +} in the order
     * shift, control, alt. A value of {@code 1} gives {@code key.keyboard.left.shift}, {@code 2}
     * gives {@code key.keyboard.left.control}, {@code 4} gives {@code key.keyboard.left.alt},
     * {@code 3} gives both of the first two joined, and a mask of {@code 0} gives an empty string.
     * Bits it does not know about are ignored rather than rejected, and only the left hand
     * modifier names are ever produced, which is also all {@link #getModInt(String)} needs to read
     * one back.<br>
     * It is the exact inverse of {@link #getModInt(String)} for every value the game produces, and
     * this is what fills in the {@link #mods} field.<br>
     * This is a static helper rather than part of the script-facing shape of the event, and the
     * shipped type definitions leave it off the {@code Key} interface, so a script has no typed way
     * of calling it by name. Every distinct string it can produce, from any mask the game can
     * report. The table below is a reference rather than runnable code:
     * <pre>
     * // 0 -> ""
     * // 1 -> "key.keyboard.left.shift"
     * // 2 -> "key.keyboard.left.control"
     * // 3 -> "key.keyboard.left.shift+key.keyboard.left.control"
     * // 4 -> "key.keyboard.left.alt"
     * // 5 -> "key.keyboard.left.shift+key.keyboard.left.alt"
     * // 6 -> "key.keyboard.left.control+key.keyboard.left.alt"
     * // 7 -> "key.keyboard.left.shift+key.keyboard.left.control+key.keyboard.left.alt"
     * </pre>
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
     * turns a {@code +} joined list of game modifier names back into the modifier bitmask, which is
     * the form {@link #getKeyModifiers(int)} reads. The bits are the same ones the windowing
     * library uses: {@code 1} for shift, {@code 2} for control and {@code 4} for alt, and they are
     * combined, so a set of modifiers can report several at once.<br>
     * Both the left and the right hand name of each modifier is accepted here, so
     * {@code key.keyboard.right.shift} reads the same as {@code key.keyboard.left.shift}, even
     * though {@link #getKeyModifiers(int)} only ever produces the left hand ones. An empty string
     * gives {@code 0}, and any part it does not recognise is skipped rather than rejected, so
     * passing something that is not a modifier string at all gives {@code 0} rather than an error.
     * <br>
     * It is how JsMacros matches a key binding trigger against a key's {@link #mods}, so a script
     * that wants the same treatment for a modifier set of its own wants this conversion. It is
     * also a static helper rather than part of the script-facing shape of the event, and the
     * shipped type definitions leave it off the {@code Key} interface, so a script has no typed
     * way of calling it by name. The table below is a reference rather than runnable code, and it
     * shows what it returns for each string the game can produce:
     * <pre>
     * // ""                                                                       -> 0
     * // "key.keyboard.left.shift"                                                -> 1
     * // "key.keyboard.left.control"                                              -> 2
     * // "key.keyboard.left.shift+key.keyboard.left.control"                      -> 3
     * // "key.keyboard.left.alt"                                                  -> 4
     * // "key.keyboard.left.shift+key.keyboard.left.alt"                          -> 5
     * // "key.keyboard.left.control+key.keyboard.left.alt"                        -> 6
     * // "key.keyboard.left.shift+key.keyboard.left.control+key.keyboard.left.alt" -> 7
     * </pre>
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
