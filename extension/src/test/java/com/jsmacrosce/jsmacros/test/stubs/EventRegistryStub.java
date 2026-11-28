package com.jsmacrosce.jsmacros.test.stubs;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.event.BaseEventRegistry;
import com.jsmacrosce.jsmacros.core.event.BaseListener;
import com.jsmacrosce.jsmacros.core.event.IEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EventRegistryStub extends BaseEventRegistry {
    public EventRegistryStub(Core runner) {
        super(runner);
    }

    @Override
    public void addScriptTrigger(ScriptTrigger rawmacro) {
        // no-op
    }

    @Override
    public boolean removeScriptTrigger(ScriptTrigger rawmacro) {
        throw new AssertionError("not implemented");
    }

    @Override
    public List<ScriptTrigger> getScriptTriggers() {
        List<ScriptTrigger> triggers = new ArrayList<>();
        for (Set<IEventListener> listeners : getListenersSnapshot().values()) {
            for (IEventListener listener : listeners) {
                if (listener instanceof BaseListener macro) {
                    triggers.add(macro.getRawTrigger());
                }
            }
        }
        return triggers;
    }

}
