package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import net.minecraft.world.entity.Entity;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires for every living entity in the client world whose health goes up, including the local
 * player and every mob or animal the client is tracking.<br>
 * The event is derived from health changes, not from damage sources, so it also fires for health
 * that is restored by a mod or by a command, and it does not fire in singleplayer worlds for the
 * integrated server's copy of the player.<br>
 * For the local player only, {@link com.jsmacrosce.jsmacros.client.api.event.impl.player.EventHeal}
 * and {@link com.jsmacrosce.jsmacros.client.api.event.impl.player.EventHealthChange} fire
 * alongside this event.<br>
 * This event is not cancellable, and it fires just before the new value is written, not after.
 * The mixin injects at the head of {@code setHealth}, so {@code health} already carries the
 * incoming value while reading the entity's health back from the world still gives the old one.
 * example:
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
     * the entity that regained health. It is wrapped in the most specific
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper} subclass for its
     * type, so it can be narrowed further with {@code asLiving()}, {@code asPlayer()} and the
     * other {@code as...()} helpers. Its type is available from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper#getType() getType()}
     * and its name from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper#getName() getName()}.
     */
    public final EntityHelper<?> entity;
    /**
     * the entity's health after the gain, in half hearts as the vanilla health bar counts them.
     */
    public final float health;
    /**
     * how much health the entity gained, the difference between its health before and after the
     * change. Always positive.
     */
    public final float damage;

    public EventEntityHealed(Entity e, float health, float amount) {
        super(JsMacrosClient.clientCore);
        entity = EntityHelper.create(e);
        this.health = health;
        this.damage = amount;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"entity\": %s, \"health\": %f, \"damage\": %f}", this.getEventName(), entity.toString(), health, damage);
    }

}
