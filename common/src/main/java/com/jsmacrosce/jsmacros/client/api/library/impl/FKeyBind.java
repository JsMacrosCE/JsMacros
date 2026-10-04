package com.jsmacrosce.jsmacros.client.api.library.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import org.jetbrains.annotations.Nullable;

import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;
import com.jsmacrosce.jsmacros.core.library.Library;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Functions for getting and modifying key pressed states.
 * <p>
 * An instance of this class is passed to scripts as the {@code KeyBind} variable.
 * <br>
 * There are two names in play here and mixing them up is the usual reason a keybind script does
 * nothing. A <em>key</em> is a physical input, named the way the game names them:
 * {@code key.keyboard.w}, {@code key.mouse.0}, {@code key.keyboard.left.shift}. A <em>keybind</em>
 * is what that input is wired up to do, named the way the options screen lists it: {@code
 * key.forward}, {@code key.attack}, {@code key.jump}. So {@link #key(String, boolean)} and
 * {@link #pressKey(String)} take a key name, and {@link #keyBind(String, boolean)},
 * {@link #pressKeyBind(String)} and {@link #setKeyBind(String, String)} take a keybind name.
 * {@link #getKeyBindings()} hands back every keybind name mapped to the key it is currently on,
 * which is the quickest way to find out what the right name is.
 * <br>
 * Pressing is not the same as holding. Setting a state to {@code true} marks the key down, so
 * whatever is bound to it keeps happening on later ticks, and it also fires a single click, which
 * is what most one-shot things such as an attack or a hotbar swap read. Setting it to {@code
 * false} marks it up again and takes it out of {@link #getPressedKeys()}. A script that presses
 * and never releases leaves the key down, and a script that wants one action should press, wait a
 * tick, and release.
 * <br>
 * Two things are worth knowing before relying on this. Every one of the state setters does
 * nothing at all while any screen is open, because the game itself ignores input then, so a call
 * made from a {@code ScreenOpen} event or from inside an open GUI is silently dropped. And a name
 * that is not a real key does not fail, it resolves to the game's unknown key, so a typo presses
 * nothing at all rather than raising anything.
 * example:
 * <pre>
 * // what are the movement keys bound to right now? this maps a keybind name to a key name
 * const binds = KeyBind.getKeyBindings();
 * Chat.log(`forward is on ${binds.get("key.forward")}`);
 *
 * // hold a key for a moment: pressing marks it down and fires one click
 * KeyBind.pressKey("key.keyboard.w");
 * Client.waitTick(2);
 * // and releasing marks it up again
 * KeyBind.releaseKey("key.keyboard.w");
 *
 * // the same thing by keybind name, which is what to use when the binding might be
 * // on a different key than the default
 * KeyBind.pressKeyBind("key.forward");
 * Client.waitTick(2);
 * KeyBind.releaseKeyBind("key.forward");
 *
 * // rebind jump to the numpad 0 key, and put it back afterwards
 * KeyBind.setKeyBind("key.jump", "key.keyboard.numpad.0");
 * KeyBind.setKeyBind("key.jump", null);
 *
 * // what is held down at this moment
 * for (const key of KeyBind.getPressedKeys()) {
 *   Chat.log(`held: ${key}`);
 * }
 * </pre>
 *
 * @author Wagyourtail
 */
@Library("KeyBind")
@SuppressWarnings("unused")
public class FKeyBind extends BaseLibrary {
    private static final Minecraft mc = Minecraft.getInstance();

    public FKeyBind(Core<?, ?> runner) {
        super(runner);
    }

    /**
     * Dont use this one... get the raw minecraft keycode class.
     * <p>
     * A name the game does not know resolves to the game's own unknown key rather than failing,
     * so this cannot be used to check that a name is valid.
     *
     * @param keyName the name of the key, for example {@code key.keyboard.w}
     * @return the raw minecraft keycode class
     */
    @DocletReplaceParams("keyName: Key")
    public Key getKeyCode(String keyName) {
        try {
            return InputConstants.getKey(keyName);
        } catch (Exception e) {
            return InputConstants.UNKNOWN;
        }
    }

    /**
     * every keybind the game has, and the key it is currently on.
     * <p>
     * This is a snapshot taken when it is called, so a rebind made afterwards does not show up in
     * an earlier result. The key on the left of each pair is the keybind name, which is what the
     * {@code keyBind} calls take, and the key on the right is the key name, which is what the
     * {@code key} calls take.
     * example:
     * <pre>
     * // find out what "key.attack" is bound to right now
     * const binds = KeyBind.getKeyBindings();
     * Chat.log(`attack is on ${binds.get("key.attack")}`);
     * </pre>
     *
     * @return A {@link Map Map} of all the minecraft keybinds.
     * @since 1.2.2
     */
    @DocletReplaceReturn("JavaMap<Bind, Key>")
    public Map<String, String> getKeyBindings() {
        Map<String, String> keys = new HashMap<>();
        for (KeyMapping key : ImmutableList.copyOf(mc.options.keyMappings)) {
            keys.put(key.getName(), key.saveString());
        }
        return keys;
    }

    /**
     * Sets a minecraft keybind to the specified key.
     * <p>
     * This changes the option, not the current input state, so it is what the controls screen
     * would write. A keybind set to {@code null} is unbound, which is the same state the controls
     * screen shows for a key with nothing on it. The first keybind whose name matches is changed
     * and the search stops there, so a name that matches nothing is quietly left alone.
     * <p>
     * To hold a key down rather than rebind it, use {@link #key(String, boolean)} or
     * {@link #keyBind(String, boolean)} instead.
     * example:
     * <pre>
     * // put sneak on the left control, and then leave it unbound again
     * KeyBind.setKeyBind("key.sneak", "key.keyboard.left.control");
     * KeyBind.setKeyBind("key.sneak", null);
     * </pre>
     *
     * @param bind the name of the keybind, for example {@code key.forward}
     * @param key the name of the key to put it on, or {@code null} to unbind it
     * @since 1.2.2
     */
    @DocletReplaceParams("bind: Bind, key: Key | null")
    public void setKeyBind(String bind, @Nullable String key) {
        for (KeyMapping keybind : mc.options.keyMappings) {
            if (keybind.getName().equals(bind)) {
                keybind.setKey(key != null ? InputConstants.getKey(key) : InputConstants.UNKNOWN);
                KeyMapping.resetMapping();
                return;
            }
        }
    }

    /**
     * Set a key-state for a key.
     * <p>
     * This takes a <em>key</em> name such as {@code key.keyboard.w}, not a keybind name, and it
     * acts on every keybind that key is currently attached to rather than on one of them, so
     * pressing the physical W key presses whatever W happens to be bound to. Set the state to
     * {@code true} to mark the key down, which also fires a single click, and to {@code false}
     * to mark it up again.<br>
     * Nothing happens at all while a screen is open. A name the game does not know presses
     * nothing rather than raising anything.
     * example:
     * <pre>
     * // hold W down for a moment, then let go
     * KeyBind.key("key.keyboard.w", true);
     * Client.waitTick(2);
     * KeyBind.key("key.keyboard.w", false);
     * </pre>
     *
     * @param keyName the name of the key, for example {@code key.keyboard.w}
     * @param keyState {@code true} to mark the key down, {@code false} to mark it up
     */
    @DocletReplaceParams("keyName: Key, keyState: boolean")
    public void key(String keyName, boolean keyState) {
        key(getKeyCode(keyName), keyState);
    }

    /**
     * Calls {@link #key(String, boolean)} with keyState set to true.
     * <p>
     * That marks the key down and fires a click on every keybind it is attached to, and it does
     * nothing while a screen is open.
     * example:
     * <pre>
     * // one tick of holding W, which is one tick of walking forward
     * KeyBind.pressKey("key.keyboard.w");
     * Client.waitTick(1);
     * KeyBind.releaseKey("key.keyboard.w");
     * </pre>
     *
     * @param keyName the name of the key to press
     * @since 1.8.4
     */
    @DocletReplaceParams("keyName: Key")
    public void pressKey(String keyName) {
        key(keyName, true);
    }

    /**
     * Calls {@link #key(String, boolean)} with keyState set to false.
     * <p>
     * This marks the key up again and takes it out of {@link #getPressedKeys()}. It does nothing
     * while a screen is open, and it does nothing for a key that is not currently down.
     *
     * @param keyName the name of the key to release
     * @since 1.8.4
     */
    @DocletReplaceParams("keyName: Key")
    public void releaseKey(String keyName) {
        key(keyName, false);
    }

    /**
     * Don't use this one... set the key-state using the raw minecraft keycode class.
     * <p>
     * This is what {@link #key(String, boolean)} resolves its name and calls. It acts on every
     * keybind currently attached to the key, and it does nothing at all while a screen is open.
     *
     * @param keyBind the raw minecraft key to set the state of
     * @param keyState {@code true} to mark the key down, {@code false} to mark it up
     */
    protected void key(Key keyBind, boolean keyState) {
        if (Minecraft.getInstance().screen != null) return;
        KeyMapping.set(keyBind, keyState);
        if (keyState) {
            KeyMapping.click(keyBind);
        }

        // add to pressed keys list
        if (keyState) {
            KeyTracker.press(keyBind);
        } else {
            KeyTracker.unpress(keyBind);
        }
    }

    /**
     * Set a key-state using the name of the keybind rather than the name of the key.
     * <p>
     * This is probably the one you should use.
     * <p>
     * Unlike {@link #key(String, boolean)}, which acts on a physical key and therefore on
     * whatever is attached to it, this acts on the one keybind named, so it holds that action
     * down no matter which key it is on. Set the state to {@code true} to mark it down, which
     * also fires a click, and to {@code false} to mark it up again.<br>
     * Nothing happens at all while a screen is open, and a name that matches no keybind is
     * quietly left alone.
     * example:
     * <pre>
     * // sneak for three ticks, by name, whatever key it is currently on
     * KeyBind.keyBind("key.sneak", true);
     * Client.waitTick(3);
     * KeyBind.keyBind("key.sneak", false);
     * </pre>
     *
     * @param keyBind the name of the keybind, for example {@code key.forward}
     * @param keyState {@code true} to mark the keybind down, {@code false} to mark it up
     * @since 1.2.2
     */
    @DocletReplaceParams("keyBind: Bind, keyState: boolean")
    public void keyBind(String keyBind, boolean keyState) {
        if (Minecraft.getInstance().screen != null) return;
        for (KeyMapping key : mc.options.keyMappings) {
            if (key.getName().equals(keyBind)) {
                key.setDown(keyState);
                if (keyState) {
                    KeyMapping.click(InputConstants.getKey(key.saveString()));
                }

                // add to pressed keys list
                if (keyState) {
                    KeyTracker.press(key);
                } else {
                    KeyTracker.unpress(key);
                }
                return;
            }
        }
    }

    /**
     * Calls {@link #keyBind(String, boolean)} with keyState set to true.
     * <p>
     * That marks the keybind down and fires a click, and it does nothing while a screen is open.
     * example:
     * <pre>
     * // tap jump, which is one jump
     * KeyBind.pressKeyBind("key.jump");
     * Client.waitTick(1);
     * KeyBind.releaseKeyBind("key.jump");
     * </pre>
     *
     * @param keyBind the name of the keybinding to press
     * @since 1.8.4
     */
    @DocletReplaceParams("keyBind: Bind")
    public void pressKeyBind(String keyBind) {
        keyBind(keyBind, true);
    }

    /**
     * Calls {@link #keyBind(String, boolean)} with keyState set to false.
     * <p>
     * This marks the keybind up again and takes it out of {@link #getPressedKeys()}. It does
     * nothing while a screen is open, and it does nothing for a keybind that is not down.
     *
     * @param keyBind the name of the keybinding to release
     * @since 1.8.4
     */
    @DocletReplaceParams("keyBind: Bind")
    public void releaseKeyBind(String keyBind) {
        keyBind(keyBind, false);
    }

    /**
     * Don't use this one... set the key-state using the raw minecraft keybind class.
     * <p>
     * This acts on the one keybind it is handed rather than on everything attached to a key.
     * It does nothing at all while a screen is open.
     *
     * @param keyBind the raw minecraft keybind to set the state of
     * @param keyState {@code true} to mark the keybind down, {@code false} to mark it up
     */
    protected void key(KeyMapping keyBind, boolean keyState) {
        if (Minecraft.getInstance().screen != null) return;
        keyBind.setDown(keyState);
        if (keyState) {
            KeyMapping.click(InputConstants.getKey(keyBind.saveString()));
        }

        // add to pressed keys list
        if (keyState) {
            KeyTracker.press(keyBind);
        } else {
            KeyTracker.unpress(keyBind);
        }
    }

    /**
     * every key currently held down, by the name of the key itself.
     * <p>
     * This is a snapshot of the keys JsMacros has been told to hold, which is not quite the same
     * thing as what the game believes: a key the player is physically holding is not in here
     * unless a script put it there, and a key a script pressed and never released stays in here.
     * Both the {@code key} calls and the {@code keyBind} calls add to it, and both take out of
     * it again when released.
     * example:
     * <pre>
     * // hold W down, and watch the set notice
     * KeyBind.pressKey("key.keyboard.w");
     * for (const key of KeyBind.getPressedKeys()) {
     *   Chat.log(`held: ${key}`);
     * }
     * KeyBind.releaseKey("key.keyboard.w");
     * </pre>
     *
     * @return a set of currently pressed keys.
     * @since 1.2.6 (turned into set instead of list in 1.6.5)
     */

    @DocletReplaceReturn("JavaSet<Key>")
    public Set<String> getPressedKeys() {
        return KeyTracker.getPressedKeys();
    }

    public static class KeyTracker {
        private static final Set<String> pressedKeys = new HashSet<>();

        public synchronized static void press(Key key) {
            String translationKey = key.getName();
            if (translationKey != null) {
                pressedKeys.add(translationKey);
            }
        }

        public synchronized static void press(KeyMapping bind) {
            String translationKey = bind.saveString();
            if (translationKey != null) {
                pressedKeys.add(translationKey);
            }
        }

        public synchronized static void unpress(Key key) {
            String translationKey = key.getName();
            if (translationKey != null) {
                pressedKeys.remove(translationKey);
            }
        }

        public synchronized static void unpress(KeyMapping bind) {
            String translationKey = bind.saveString();
            if (translationKey != null) {
                pressedKeys.remove(translationKey);
            }
        }

        public static synchronized Set<String> getPressedKeys() {
            return ImmutableSet.copyOf(pressedKeys);
        }

    }

}
