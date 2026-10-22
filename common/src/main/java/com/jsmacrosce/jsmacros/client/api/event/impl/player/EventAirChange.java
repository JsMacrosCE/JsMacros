package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the local player's remaining air supply is set to a new value.<br>
 * The game applies the server's air value on every air tick, so this event is filtered down to
 * values that are a multiple of 20. Air counts down from the player's maximum while the player
 * cannot breathe, and the maximum is 300 by default, so the event fires roughly once per second
 * at 300, 280, 260 and so on down to 0, rather than on every single tick.<br>
 * This event is not cancellable, the new air value is applied to the player no matter what a
 * listener does.
 * example:
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
     * the player's new air supply, in the same unit the game counts it in. A completely full bar
     * of air is 300 by default and an empty one is 0, and the value can also exceed the maximum
     * if a mod or the server sets it higher.
     */
    public final int air;

    public EventAirChange(int air) {
        super(JsMacrosClient.clientCore);
        this.air = air;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"air\": %d}", this.getEventName(), air);
    }

}
