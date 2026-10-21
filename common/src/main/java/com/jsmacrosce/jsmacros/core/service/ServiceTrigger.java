package com.jsmacrosce.jsmacros.core.service;

import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.event.Event;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Mutable configuration for a service trigger. Obtain a registered trigger with
 * {@link ServiceManager#getTrigger(String)} after checking its name, for example:
 * <pre>
 * const manager = JsMacros.getServiceManager();
 * if (manager.getServices().contains("example")) {
 *   const trigger = manager.getTrigger("example");
 *   Chat.log(`${trigger.file}: enabled=${trigger.enabled}`);
 * }
 * </pre>
 */
public class ServiceTrigger {
    /** Script path as stored in the trigger configuration. */
    public Path file;
    /** Whether the service is configured as enabled; this does not indicate that it is running. */
    public boolean enabled;

    /**
     * Stores the supplied path and enabled configuration flag without validating or normalizing the path.
     * @param file path to store; may be {@code null}
     * @param enabled whether the service is configured as enabled
     */
    public ServiceTrigger(Path file, boolean enabled) {
        this.file = file;
        this.enabled = enabled;
    }

    /**
     * Converts this configuration to an event trigger for the {@code Service} event.
     * The new trigger uses {@code file.toString()}, this instance's enabled flag,
     * and {@code joined == false}; conversion does not register or start a service.
     * A null file causes {@link NullPointerException}.
     * @return a new service event trigger
     */
    public ScriptTrigger toScriptTrigger() {
        return new ScriptTrigger(ScriptTrigger.TriggerType.EVENT, EventService.class.getAnnotation(Event.class).value(), file, enabled, false);
    }

    /**
     * Compares service triggers by their enabled flags and paths; null paths compare equal.
     * @param o object to compare
     * @return {@code true} for this instance or an equal {@code ServiceTrigger}; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceTrigger)) {
            return false;
        }
        ServiceTrigger that = (ServiceTrigger) o;
        return enabled == that.enabled && Objects.equals(file, that.file);
    }

    /**
     * Computes a hash from this trigger's path and enabled flag. The result may change
     * when either mutable field changes, so avoid using a trigger as a hash key while
     * those fields are being modified.
     * @return hash code derived from the current field values
     */
    @Override
    public int hashCode() {
        return Objects.hash(file, enabled);
    }

}
