package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.world.damagesource.DamageSource;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when a call decreases the local player's health, before the incoming value is applied.
 * It dispatches before {@link com.jsmacrosce.jsmacros.client.api.event.impl.player.EventHealthChange},
 * which dispatches before {@link com.jsmacrosce.jsmacros.client.api.event.impl.world.EventEntityDamaged}.
 * The injected event supplies a generic damage source, so {@link #source} is its generic damage
 * type ID and {@link #attacker} is {@code null}.
 * <pre>
 * JsMacros.on("Damage", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`Took ${event.change} damage, ${event.health} hp left`);
 *   if (4.0 >= event.health) {
 *     Chat.log("About to die");
 *   }
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Player/Stats")
@Event(value = "Damage", oldName = "DAMAGE")
public class EventDamage extends BaseEvent {
    /**
     * {@code null}; this event is constructed with a generic damage source, which has no entity.
     *
     * @deprecated may not work on servers
     */
    @Deprecated
    @DocletIgnore
    public final EntityHelper<?> attacker;
    /**
     * ID of the generic damage type supplied by this event; the actual damage source is not retained.
     *
     * @deprecated may not work on servers
     */
    @DocletReplaceReturn("DamageSource")
    @Deprecated
    @DocletIgnore
    public final String source;
    /**
     * Incoming player health value, before it is applied.
     */
    public final float health;
    /**
     * Amount of health lost; positive, unlike the matching
     * {@link com.jsmacrosce.jsmacros.client.api.event.impl.player.EventHealthChange#change change}
     * which is negative on the same change.
     */
    public final float change;

    /**
     * Initializes the event with the supplied damage source and health values. The source's message ID
     * is stored as {@code source}; the causing entity is wrapped as {@code attacker}, or {@code null}
     * when absent. A {@code null} source throws {@link NullPointerException}.
     * @param source damage source used to obtain its message ID and causing entity
     * @param health incoming health value
     * @param change amount of health lost
     */
    public EventDamage(DamageSource source, float health, float change) {
        super(JsMacrosClient.clientCore);
        if (source.getEntity() == null) {
            this.attacker = null;
        } else {
            this.attacker = EntityHelper.create(source.getEntity());
        }
        this.source = source.getMsgId();
        this.health = health;
        this.change = change;
    }

    /**
     * Returns a diagnostic string containing this event's name, health, and change values.
     * @return a string in the form {@code eventName:{"health": %f, "change": %f}}
     */
    @Override
    public String toString() {
        return String.format("%s:{\"health\": %f, \"change\": %f}", this.getEventName(), health, change);
    }

}
