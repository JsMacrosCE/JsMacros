package com.jsmacrosce.jsmacros.client.listeners;

import com.mojang.blaze3d.platform.InputConstants;
import com.jsmacrosce.jsmacros.client.api.event.impl.EventKey;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.BaseListener;
import com.jsmacrosce.jsmacros.core.language.EventContainer;

public class KeyListener extends BaseListener {
    private int mods;
    private String key;

    public KeyListener(ScriptTrigger macro, Core runner) {
        super(macro, runner);
        StringBuilder mods = new StringBuilder();
        this.mods = 0;
        try {
            String[] comb = macro.event.split("\\+");
            int i = 0;
            for (String key : comb) {
                if (++i == comb.length) {
                    this.key = key;
                } else {
                    if (i > 1) {
                        mods.append("+");
                    }
                    mods.append(key);
                }
            }
            this.mods = EventKey.getModInt(mods.toString());
        } catch (Throwable e) {
            key = InputConstants.UNKNOWN.getName();
        }
    }

    @Override
    public EventContainer<?> trigger(BaseEvent event) {
        if (check((EventKey) event)) {
            return runScript(event);
        }
        return null;
    }

    private boolean check(EventKey event) {
        boolean keyState = event.action == 1;
        if (event.key.equals(key) && (EventKey.getModInt(event.mods) & mods) == mods) {
            switch (getRawTrigger().triggerType) {
                case KEY_FALLING:
                    return !keyState;
                case KEY_RISING:
                    return keyState;
                case EVENT:
                case KEY_BOTH:
                default:
                    return true;
            }
        }
        return false;
    }

}
