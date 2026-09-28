package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires whenever the local player's health changes, upwards or downwards, and reports both
 * directions through this one event.<br>
 * This is the local-player companion to the world-wide
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.world.EventEntityDamaged} and
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.world.EventEntityHealed}. For the local
 * player the direction-specific event fires first, this one second and the world-wide event
 * last, so a loss goes
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.player.EventDamage} then this event then
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.world.EventEntityDamaged}, and a gain
 * goes {@link com.jsmacrosce.jsmacros.client.api.event.impl.player.EventHeal} then this event
 * then {@link com.jsmacrosce.jsmacros.client.api.event.impl.world.EventEntityHealed}. In a
 * singleplayer world the integrated server's copy of the player is skipped, so this still fires
 * exactly once per change.<br>
 * The event is derived from the health value, not from damage sources, so it also fires for
 * health a mod, a command or a status effect changed. It is not cancellable, and it fires
 * just before the new value is written, not after. The mixin injects at the head of
 * {@code setHealth}, so {@code health} already carries the incoming value while reading the
 * player's health back from the world still gives the old one.<br>
 * The new value is compared against the last value this event saw, so writing the same health
 * value again produces no event at all.
 * example:
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
     * the player's health after the change, in half hearts as the vanilla health bar counts them.
     */
    public final float health;
    /**
     * how much the health moved by, signed. It is negative when the player lost health and
     * positive when the player gained health, so {@code health - change} is the health from
     * before the change either way.<br>
     * This differs from the matching
     * {@link com.jsmacrosce.jsmacros.client.api.event.impl.player.EventDamage#change} and
     * {@link com.jsmacrosce.jsmacros.client.api.event.impl.player.EventHeal#change}, which are
     * always positive amounts.
     */
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
