package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.doclet.DocletCategory;

import net.minecraft.world.entity.Entity;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires before a client-side living entity's health decreases through {@code setHealth}; it does not
 * identify a damage source. {@code health} is the incoming value and
 * {@code damage} is the difference from the previously tracked health.
 * <pre>
 * JsMacros.on("EntityDamaged", JavaWrapper.methodToJava(function (event) {
 *   if (event.entity instanceof EntityHelpers.Creeper) {
 *     Chat.log(`Creeper took ${event.damage} damage; incoming health: ${event.health}`);
 *   }
 * }))
 * </pre>
 */
@DocletCategory("World")
@Event("EntityDamaged")
public class EventEntityDamaged extends BaseEvent {
    /**
     * Entity whose health is decreasing.
     */
    public final EntityHelper<?> entity;
    /**
     * Incoming health value passed to {@code setHealth}.
     *
     * @since 1.6.5
     */
    public final float health;
    /**
     * Decrease from the previously tracked health; positive.
     */
    public final float damage;

    /**
     * Wraps an entity and stores the supplied health and damage values without applying damage.
     * A {@code null} entity causes {@link java.util.Objects#requireNonNull(Object)} to throw
     * {@link NullPointerException} during helper creation.
     *
     * @param e non-null entity to wrap; no living-entity validation is performed
     * @param health supplied incoming health value; it is stored unchanged and not applied
     * @param amount supplied decrease amount, stored unchanged as {@code damage} without calculation or validation
     */
    public EventEntityDamaged(Entity e, float health, float amount) {
        super(JsMacrosClient.clientCore);
        entity = EntityHelper.create(e);
        this.health = health;
        this.damage = amount;
    }

    /**
     * Returns a diagnostic string containing the event name, entity helper string, and stored values.
     * The format is {@code %s:{"entity": %s, "health": %f, "damage": %f}}.
     *
     * @return diagnostic text in the stated format
     */
    @Override
    public String toString() {
        return String.format("%s:{\"entity\": %s, \"health\": %f, \"damage\": %f}", this.getEventName(), entity.toString(), health, damage);
    }

}
