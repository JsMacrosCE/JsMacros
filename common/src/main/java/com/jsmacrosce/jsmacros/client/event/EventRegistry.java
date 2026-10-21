package com.jsmacrosce.jsmacros.client.event;

import com.jsmacrosce.jsmacros.client.api.event.impl.EventKey;
import com.jsmacrosce.jsmacros.client.listeners.KeyListener;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.event.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Client-side registry for profile macro triggers, exposed through the client's core rather than
 * as a separate global library.
 * <pre>
 * const Client = Java.type('com.jsmacrosce.jsmacros.client.JsMacrosClient');
 * const registry = Client.clientCore.eventRegistry;
 * Chat.log(registry.getScriptTriggers().size());
 * </pre>
 */
public class EventRegistry extends BaseEventRegistry {

    /**
     * Associates this registry with the provided core.
     * @param runner the core associated with this registry
     */
    public EventRegistry(Core<?, ?> runner) {
        super(runner);
    }

    /**
     * Registers a profile trigger as a client event or key listener. Legacy event aliases are
     * remapped first; event triggers with a {@code Joined} prefix have that prefix removed and the
     * supplied trigger's {@code joined} flag set. The trigger is retained by reference.
     * @param rawMacro the profile trigger to register; its event or joined setting may be modified
     */
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

    /**
     * Removes the first listener backed by this exact trigger instance under its current event name,
     * or the key-event name for key triggers. Other event names are not searched.
     * @param rawMacro the trigger instance whose listener should be removed
     * @return {@code true} if one listener was removed; {@code false} if no matching listener was found
     */
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

    /**
     * Returns a new list of triggers held by registered macro listeners. The list contains the
     * original trigger references; changing the list does not change registration.
     * @return a new list of registered macro trigger references
     */
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
