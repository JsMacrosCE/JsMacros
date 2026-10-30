package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires before the local player's air supply is set to a value divisible by {@code 20}; it does not
 * report other values.
 * <pre>
 * JsMacros.on("AirChange", JavaWrapper.methodToJava(function (event) {
 *   if (0 >= event.air) {
 *     Chat.log("Out of air");
 *   } else if (60 >= event.air) {
 *     Chat.log(`Only ${event.air} air left, about ${event.air / 20} seconds`);
 *   }
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Player/Stats")
@Event(value = "AirChange", oldName = "AIR_CHANGE")
public class EventAirChange extends BaseEvent {
    /**
     * the air supply value being set on the local player.
     */
    public final int air;

    /**
     * Initializes this event with the client core and the supplied air-supply value, stored unchanged.
     * @param air supplied air-supply value
     */
    public EventAirChange(int air) {
        super(JsMacrosClient.clientCore);
        this.air = air;
    }

    /**
     * Formats the event name and stored air value as a diagnostic string, not JSON.
     * @return the event name followed by the stored air value in decimal form
     */
    @Override
    public String toString() {
        return String.format("%s:{\"air\": %d}", this.getEventName(), air);
    }

}
