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
 * Gets and changes key and keybind states through the script global {@code KeyBind}.
 * <p>
 * Key names identify inputs (for example, {@code key.keyboard.w}); keybind names identify actions
 * (for example, {@code key.forward}). {@code key} methods take key names, while {@code keyBind}
 * methods and {@link #setKeyBind(String, String)} take keybind names. State-setting calls are
 * ignored while a screen is open. Press calls mark the state down and register a click; release
 * calls mark it up. Use the matching release call after a press to avoid leaving a key held.
 * <pre>
 * // Look up the current key assigned to forward.
 * const binds = KeyBind.getKeyBindings();
 * Chat.log(`forward is on ${binds.get("key.forward")}`);
 *
 * // Hold the forward action briefly, regardless of its assigned key.
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

    /**
     * Initializes this library with its owning script runner.
     *
     * @param runner the script runner associated with this library
     */
    public FKeyBind(Core<?, ?> runner) {
        super(runner);
    }

    /**
     * Resolves a serialized key name to Minecraft's key representation. An unrecognized name
     * resolves to the unknown key rather than indicating failure.
     *
     * @param keyName the name of the key, for example {@code key.keyboard.w}
     * @return the resolved key, or Minecraft's unknown key for an unrecognized name
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
     * Returns a snapshot mapping each keybind name to its currently assigned key name.
     *
     * @return a map from keybind names to assigned key names
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
     * Reassigns the first keybind with the given name; {@code null} unbinds it. This changes the
     * binding, not its pressed state. An unmatched name has no effect.
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
     * Sets the state of the named key, affecting every keybind currently mapped to that key.
     * Setting it down also registers a click. State changes are ignored while a screen is open;
     * an unrecognized name resolves to the unknown key.
     *
     * @param keyName the name of the key, for example {@code key.keyboard.w}
     * @param keyState {@code true} to mark the key down, {@code false} to mark it up
     */
    @DocletReplaceParams("keyName: Key, keyState: boolean")
    public void key(String keyName, boolean keyState) {
        key(getKeyCode(keyName), keyState);
    }

    /**
     * Marks the named key down and registers a click on every keybind mapped to it. Has no effect
     * while a screen is open.
     *
     * @param keyName the name of the key to press
     * @since 1.8.4
     */
    @DocletReplaceParams("keyName: Key")
    public void pressKey(String keyName) {
        key(keyName, true);
    }

    /**
     * Marks the named key up and removes it from {@link #getPressedKeys()}. Has no effect while a
     * screen is open.
     *
     * @param keyName the name of the key to release
     * @since 1.8.4
     */
    @DocletReplaceParams("keyName: Key")
    public void releaseKey(String keyName) {
        key(keyName, false);
    }

    /**
     * Sets the state of a raw Minecraft key, affecting every keybind mapped to it. Has no effect
     * while a screen is open.
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
     * Sets the state of the named keybind, independent of which key it is assigned to. Setting it
     * down also registers a click. Has no effect while a screen is open or if no keybind matches.
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
     * Marks the named keybind down and registers a click. Has no effect while a screen is open or
     * if no keybind matches.
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
     * Marks the matching keybind up and removes its name from {@link #getPressedKeys()}. Does
     * nothing while a screen is open or when no keybind has this name.
     *
     * @param keyBind the name of the keybinding to release
     * @since 1.8.4
     */
    @DocletReplaceParams("keyBind: Bind")
    public void releaseKeyBind(String keyBind) {
        keyBind(keyBind, false);
    }

    /**
     * Sets the state of one raw Minecraft keybind. Does nothing while a screen is open.
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
     * Returns a snapshot of key names tracked as pressed by JsMacros. This does not report
     * physical keys held by the player; names are added by key/keybind state calls and removed
     * when those calls release them.
     *
     * @return a snapshot of tracked pressed key names
     * @since 1.2.6 (turned into set instead of list in 1.6.5)
     */

    @DocletReplaceReturn("JavaSet<Key>")
    public Set<String> getPressedKeys() {
        return KeyTracker.getPressedKeys();
    }

    /**
     * Tracks key names marked pressed by JsMacros key and keybind state calls; it does not query
     * physical keyboard state. Direct calls update only this ledger and do not synthesize input.
     * <pre>
     * const tracker = Java.type("com.jsmacrosce.jsmacros.client.api.library.impl.FKeyBind$KeyTracker");
     * Chat.log(tracker.getPressedKeys());
     * </pre>
     */
    public static class KeyTracker {
        private static final Set<String> pressedKeys = new HashSet<>();

        /**
         * Records the raw key's name in the tracked pressed-name set. A null name is ignored.
         *
         * @param key the raw key whose name to record
         */
        public synchronized static void press(Key key) {
            String translationKey = key.getName();
            if (translationKey != null) {
                pressedKeys.add(translationKey);
            }
        }

        /**
         * Records the key mapping's current serialized key name in the tracked pressed-name set. A null name is ignored.
         *
         * @param bind the key mapping whose current assigned key name to record
         */
        public synchronized static void press(KeyMapping bind) {
            String translationKey = bind.saveString();
            if (translationKey != null) {
                pressedKeys.add(translationKey);
            }
        }

        /**
         * Removes the raw key's name from the tracked pressed-name set. A null name is ignored.
         *
         * @param key the raw key whose name to remove
         */
        public synchronized static void unpress(Key key) {
            String translationKey = key.getName();
            if (translationKey != null) {
                pressedKeys.remove(translationKey);
            }
        }

        /**
         * Removes the key mapping's current serialized key name from the tracked pressed-name set. A null name is ignored.
         *
         * @param bind the key mapping whose current assigned key name to remove
         */
        public synchronized static void unpress(KeyMapping bind) {
            String translationKey = bind.saveString();
            if (translationKey != null) {
                pressedKeys.remove(translationKey);
            }
        }

        /**
         * Returns an immutable snapshot of tracked pressed key names.
         *
         * @return immutable snapshot of tracked pressed key names
         */
        public static synchronized Set<String> getPressedKeys() {
            return ImmutableSet.copyOf(pressedKeys);
        }

    }

}
