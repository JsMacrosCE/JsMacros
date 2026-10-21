package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.world.damagesource.DamageSource;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires before the local player's health is updated to a higher value. It reports the observed
 * change rather than its cause; {@link #source} is always the generic damage type. This event is
 * not cancellable.
 * <pre>
 * JsMacros.on("Heal", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`Healed ${event.change} hp, now at ${event.health} hp`);
 * }))
 * </pre>
 * @author FlareStormGaming
 * @since 1.6.5
 */
@DocletCategory("Player/Stats")
@Event("Heal")
public class EventHeal extends BaseEvent {
    /**
     * Message id of the generic damage type; the cause of the health increase is not reported.
     */
    @DocletReplaceReturn("HealSource")
    @DocletDeclareType(name = "HealSource", type = "DamageSource")
    public final String source;
    /**
     * Health value being written, in health points (two points per heart).
     */
    public final float health;
    /**
     * Positive difference between the previous health value and the value being written, in health
     * points.
     */
    public final float change;

    /**
     * Initializes the event with the message id from the supplied damage source and the supplied
     * health values; it does not update health or dispatch the event.
     * @param source damage source whose message id is captured; {@code null} causes {@link NullPointerException}.
     * @param health supplied health value
     * @param change supplied health-increase amount
     */
    public EventHeal(DamageSource source, float health, float change) {
        super(JsMacrosClient.clientCore);
        this.source = source.getMsgId();
        this.health = health;
        this.change = change;
    }

    /**
     * Returns a diagnostic string containing the event name, stored health, and stored change.
     * The source is omitted; the result is not a JSON serialization.
     * @return diagnostic text in the form {@code %s:{"health": %f, "change": %f}}
     */
    @Override
    public String toString() {
        return String.format("%s:{\"health\": %f, \"change\": %f}", this.getEventName(), health, change);
    }

}
