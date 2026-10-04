package com.jsmacrosce.jsmacros.client.event;

import com.jsmacrosce.jsmacros.client.api.event.impl.EventKey;
import com.jsmacrosce.jsmacros.client.listeners.KeyListener;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.event.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EventRegistry extends BaseEventRegistry {

    public EventRegistry(Core<?, ?> runner) {
        super(runner);
    }

    @Override
    public synchronized void addScriptTrigger(ScriptTrigger rawMacro) {
        if (oldEvents.containsKey(rawMacro.event)) {
            rawMacro.event = oldEvents.get(rawMacro.event);
        }
        if (rawMacro.triggerType == ScriptTrigger.TriggerType.EVENT) {
            if (rawMacro.event.startsWith("Joined")) {
                rawMacro.event = rawMacro.event.substring(6);
                rawMacro.joined = true;
            }
            addListener(rawMacro.event, new EventListener(rawMacro, runner));
        } else {
            addListener(EventKey.class.getAnnotation(Event.class).value(), new KeyListener(rawMacro, runner));
        }
    }

    @Override
    public synchronized boolean removeScriptTrigger(ScriptTrigger rawMacro) {
        final String event = rawMacro.triggerType == ScriptTrigger.TriggerType.EVENT ? rawMacro.event : EventKey.class.getAnnotation(Event.class).value();
        for (IEventListener macro : getListeners(event)) {
            if (macro instanceof BaseListener && ((BaseListener) macro).getRawTrigger() == rawMacro) {
                removeListener(event, macro);
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized List<ScriptTrigger> getScriptTriggers() {
        final List<ScriptTrigger> rawProf = new ArrayList<>();
        for (Set<IEventListener> eventMacros : listeners.values()) {
            for (IEventListener macro : eventMacros) {
                if (macro instanceof BaseListener) {
                    rawProf.add(((BaseListener) macro).getRawTrigger());
                }
            }
        }
        return rawProf;
    }

}
