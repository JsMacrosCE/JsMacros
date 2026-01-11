package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Event(value = "HealthChange")
public class EventHealthChange extends BaseEvent {

    public final float health;
    public final float change;

    public EventHealthChange(float health, float change) {
        super(JsMacrosClient.clientCore);
        this.health = health;
        this.change = change;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"health\": %f, \"change\": %f}", this.getEventName(), health, change);
    }

}
