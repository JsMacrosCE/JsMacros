package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires before the local player's food level is assigned, when {@code setFoodLevel} receives a
 * value different from the current one. The event reports the incoming value.
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Player/Stats")
@Event(value = "HungerChange", oldName = "HUNGER_CHANGE")
public class EventHungerChange extends BaseEvent {
    /**
     * The incoming food-level value, conventionally 0 for an empty hunger bar through 20 for a full
     * one. This is the value passed to {@code setFoodLevel}; the method does not clamp it.
     */
    public final int foodLevel;

    /**
     * Initializes this event with the supplied food-level value, stored unchanged in {@code foodLevel}.
     * @param foodLevel supplied food-level value
     */
    public EventHungerChange(int foodLevel) {
        super(JsMacrosClient.clientCore);
        this.foodLevel = foodLevel;
    }

    /**
     * Returns an event-name-prefixed diagnostic string in the form {@code %s:{"foodLevel": %d}}.
     * @return diagnostic string containing the event name and stored food-level value
     */
    @Override
    public String toString() {
        return String.format("%s:{\"foodLevel\": %d}", this.getEventName(), foodLevel);
    }

}
