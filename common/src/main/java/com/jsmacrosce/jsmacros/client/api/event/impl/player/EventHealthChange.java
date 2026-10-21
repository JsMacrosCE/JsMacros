package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires before the local player's health is updated. {@link #change} is positive for an increase
 * and negative for a decrease. Setting health to its current value does not fire this event; it
 * is not cancellable.
 * <pre>
 * JsMacros.on("HealthChange", JavaWrapper.methodToJava(function (event) {
 *   if (event.change > 0) {
 *     Chat.log(`Healed ${event.change} hp, now at ${event.health} hp`);
 *   } else {
 *     Chat.log(`Lost ${-event.change} hp, now at ${event.health} hp`);
 *   }
 * }))
 * </pre>
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Player/Stats")
@Event(value = "HealthChange")
public class EventHealthChange extends BaseEvent {

    /**
     * Player health after the change, in health points (two points per heart).
     */
    public final float health;
    /**
     * Signed health difference: negative for health lost and positive for health gained. Unlike
     * {@link EventDamage#change} and {@link EventHeal#change}, this value retains its sign.
     */
    public final float change;

    /**
     * Initializes the event with the supplied health and signed health difference.
     * @param health supplied health, in health points
     * @param change supplied signed health difference, in health points
     */
    public EventHealthChange(float health, float change) {
        super(JsMacrosClient.clientCore);
        this.health = health;
        this.change = change;
    }

    /**
     * Returns a diagnostic string containing the event name and stored health values, formatted as
     * {@code %s:{"health": %f, "change": %f}}. The {@code %f} conversions use the default
     * format precision and locale; this is not lossless serialization or JSON.
     * @return event name and stored health values in diagnostic form
     */
    @Override
    public String toString() {
        return String.format("%s:{\"health\": %f, \"change\": %f}", this.getEventName(), health, change);
    }

}
