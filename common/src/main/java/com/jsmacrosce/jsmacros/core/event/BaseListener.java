package com.jsmacrosce.jsmacros.core.event;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.language.EventContainer;
import org.jetbrains.annotations.Nullable;

/**
 * Base implementation for listeners backed by a {@link ScriptTrigger}.
 */
public abstract class BaseListener implements IEventListener {
    protected final ScriptTrigger trigger;
    protected final Core runner;

    /**
     * Stores the trigger and script runner used by this listener.
     * @param trigger trigger configuration retained by reference
     * @param runner core used to execute the trigger
     */
    public BaseListener(ScriptTrigger trigger, Core runner) {
        this.trigger = trigger;
        this.runner = runner;
    }

    /**
     * Returns the listener's live trigger configuration; changes to it affect this listener.
     * @return the trigger stored by this listener
     */
    public ScriptTrigger getRawTrigger() {
        return trigger;
    }

    /**
     * Executes this trigger for an event when enabled. Execution failures are printed and return
     * {@code null}; a disabled trigger also returns {@code null}.
     * @param event event passed to the script runner
     * @return the execution container, or {@code null} when disabled or execution fails
     */
    @Nullable
    public EventContainer<?> runScript(BaseEvent event) {
        if (trigger.enabled) {
            try {
                return runner.exec(trigger, event);
            } catch (Throwable e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /**
     * Reports the trigger's current join preference.
     * @return the current value of the trigger's {@code joined} setting
     */
    @Override
    public boolean joined() {
        return trigger.joined;
    }

    /**
     * Compares another listener's trigger using {@link ScriptTrigger#equals(ScriptTrigger)};
     * non-listener objects use identity comparison.
     * @param o object to compare
     * @return whether the triggers compare equal, or whether this is the same non-listener object
     */
    public boolean equals(Object o) {
        if (o instanceof BaseListener) {
            return trigger.equals(((BaseListener) o).trigger);
        }
        return super.equals(o);
    }

    /**
     * Disables this listener by setting its trigger's enabled flag to {@code false}; it does not
     * unregister the listener or stop an execution already underway.
     */
    @Override
    public void off() {
        trigger.enabled = false;
    }

    /**
     * Returns diagnostic text derived from the trigger description with its first three characters
     * removed; the resulting format is not a serialization contract.
     * @return trigger diagnostic text without its initial three characters
     */
    public String toString() {
        return trigger.toString().substring(3);
    }

}
