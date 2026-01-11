package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "HungerChange", oldName = "HUNGER_CHANGE")
public class EventHungerChange extends BaseEvent {
    public final int foodLevel;

    public EventHungerChange(int foodLevel) {
        super(JsMacrosClient.clientCore);
        this.foodLevel = foodLevel;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"foodLevel\": %d}", this.getEventName(), foodLevel);
    }

}
