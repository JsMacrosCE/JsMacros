package com.jsmacrosce.jsmacros.core.event;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.language.EventContainer;

/**
 * Runs a profile macro trigger in response to its registered event. The trigger's enabled and
 * joined settings are read when dispatch occurs.
 */
public class EventListener extends BaseListener {

    /**
     * Creates a listener for a profile trigger; registration is performed separately.
     *
     * @param macro the trigger whose script and dispatch settings are used
     * @param runner the core that executes the trigger's script
     */
    public EventListener(ScriptTrigger macro, Core runner) {
        super(macro, runner);
    }

    /**
     * Runs the trigger for this event when it is enabled.
     *
     * @param event the event passed to the script as the {@code event} global
     * @return the execution container, or {@code null} if the trigger is disabled or script startup
     *         throws before returning a container
     */
    @Override
    public EventContainer<?> trigger(BaseEvent event) {
        return runScript(event);
    }

}
