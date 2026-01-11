package com.jsmacrosce.jsmacros.core.service;

import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.event.Event;

import java.nio.file.Path;
import java.util.Objects;

public class ServiceTrigger {
    public Path file;
    public boolean enabled;

    public ServiceTrigger(Path file, boolean enabled) {
        this.file = file;
        this.enabled = enabled;
    }

    public ScriptTrigger toScriptTrigger() {
        return new ScriptTrigger(ScriptTrigger.TriggerType.EVENT, EventService.class.getAnnotation(Event.class).value(), file, enabled, false);
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(file, enabled);
    }

}
