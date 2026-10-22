package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import net.minecraft.world.entity.Entity;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires before a client-side living entity's {@code setHealth} call when the supplied health is
 * greater than its previous value. {@code health} is the supplied value and {@code damage} is the
 * positive increase; this event does not identify the healing source or cancel the health change.
 * <pre>
 * JsMacros.on("EntityHealed", JavaWrapper.methodToJava(function (event) {
 *   if (event.entity.getType() === "minecraft:villager") {
 *     Chat.log(`Villager regained ${event.damage} health, ${event.health} hp now`);
 *   }
 * }))
 * </pre>
 * @author FlareStormGaming
 * @since 1.6.5
 */

@DocletCategory("World")
@Event("EntityHealed")
public class EventEntityHealed extends BaseEvent {
    /**
     * Client-side living entity whose supplied health value increased.
     */
    public final EntityHelper<?> entity;
    /**
     * Health value supplied to {@code setHealth}, before it is applied.
     */
    public final float health;
    /**
     * Positive difference between the supplied health and the previous health.
     */
    public final float damage;

    /**
     * Wraps the supplied entity and stores the supplied health values; it does not apply a health change.
     * @param e non-null entity to wrap; a null value causes {@link NullPointerException}
     * @param health supplied new-health value, stored unchanged without applying it
     * @param amount supplied health-increase value, stored unchanged as {@code damage}; it is not validated or calculated
     */
    public EventEntityHealed(Entity e, float health, float amount) {
        super(JsMacrosClient.clientCore);
        entity = EntityHelper.create(e);
        this.health = health;
        this.damage = amount;
    }

    /**
     * Formats the event name, entity string, and stored health values as a diagnostic string.
     * @return a string in the form {@code eventName:{"entity": entityString, "health": health, "damage": damage}}
     */
    @Override
    public String toString() {
        return String.format("%s:{\"entity\": %s, \"health\": %f, \"damage\": %f}", this.getEventName(), entity.toString(), health, damage);
    }

}
