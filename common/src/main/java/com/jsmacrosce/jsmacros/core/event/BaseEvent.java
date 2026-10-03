package com.jsmacrosce.jsmacros.core.event;

import com.jsmacrosce.jsmacros.core.Core;

public class BaseEvent {
    public final Core<?, ?> runner;
    protected boolean cancelled;

    public BaseEvent(Core<?, ?> runner) {
        this.runner = runner;
    }

    public boolean cancellable() {
        Event annotation = this.getClass().getAnnotation(Event.class);
        return annotation != null && annotation.cancellable();
    }

    public boolean joinable() {
        Event annotation = this.getClass().getAnnotation(Event.class);
        return cancellable() || annotation != null && annotation.joinable();
    }

    /**
     * Cancel the event
     */
    public final void cancel() {
        if (cancellable()) {
            cancelled = true;
        } else {
            throw new UnsupportedOperationException("Event is not cancellable");
        }
    }

    public final boolean isCanceled() {
        return cancelled;
    }

    public String getEventName() {
        Event annotation = this.getClass().getAnnotation(Event.class);
        return annotation == null ? getClass().getSimpleName() : annotation.value();
    }

    public void trigger() {
        runner.profile.triggerEvent(this);
    }

}
