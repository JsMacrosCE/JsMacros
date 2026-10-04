package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;

import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the local player starts or stops elytra flight, that is when the game toggles the
 * player's fall flying state.<br>
 * The state is polled once per client tick and the event only fires on a change, so it does not
 * repeat every tick while the player is gliding. Elytra flight normally starts and ends on the
 * player's own input, but the server can also force the state.<br>
 * This event is not cancellable, the flight state is applied to the player no matter what a
 * listener does.
 * example:
 * <pre>
 * JsMacros.on("FallFlying", JavaWrapper.methodToJava(function (event) {
 *   if (event.state) {
 *     Chat.log("Elytra flight started");
 *   } else {
 *     Chat.log("Elytra flight ended");
 *   }
 * }))
 * </pre>
 */
@DocletCategory("Player/Stats")
@Event("FallFlying")
public class EventFallFlying extends BaseEvent {
    /**
     * {@code true} if the player just started flying with elytra, {@code false} if the player
     * just stopped.
     */
    public final boolean state;

    public EventFallFlying(boolean state) {
        super(JsMacrosClient.clientCore);
        this.state = state;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"state\": %s}", this.getEventName(), state);
    }

}
