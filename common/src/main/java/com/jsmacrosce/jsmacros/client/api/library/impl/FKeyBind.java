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
     *
     * @param keyName
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
     *
     * @param bind
     * @param key
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
     *
     * @param keyName
     * @param keyState
     */
    @DocletReplaceParams("keyName: Key, keyState: boolean")
    public void key(String keyName, boolean keyState) {
        key(getKeyCode(keyName), keyState);
    }

    /**
     * Calls {@link #key(String, boolean)} with keyState set to true.
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
     *
     * @param keyBind
     * @param keyState
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
     *
     * @param keyBind
     * @param keyState
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
     *
     * @param keyBind
     * @param keyState
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
