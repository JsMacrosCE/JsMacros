package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;

import net.minecraft.world.entity.Entity;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the local player right clicks an entity, for example to trade with a villager, name
 * a mob, shear a sheep or interact with a boat or minecart.<br>
 * The event fires after the game has processed the click, and only if the click was not rejected
 * outright, so right clicking an entity that cannot be interacted with produces no event.<br>
 * {@link #result} tells the two useful cases apart. It is {@code true} when the entity actually
 * did something, and {@code false} when the click was only passed along the chain, which is
 * what happens when sneaking past the entity to use the item in hand instead.<br>
 * Note: this is the interact click only. Hitting a mob with the attack button raises
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.player.EventAttackEntity} instead.<br>
 * This event is not cancellable, the interaction has already been sent on to the server by the
 * time listeners see it.
 * example:
 * <pre>
 * JsMacros.on("InteractEntity", JavaWrapper.methodToJava(function (event) {
 *   if (event.result) {
 *     Chat.log(`${event.offhand ? "Off hand" : "Main hand"} used on ${event.entity.getType()}`);
 *   }
 * }))
 * </pre>
 */
@DocletCategory("Inputs/Interactions")
@Event("InteractEntity")
public class EventInteractEntity extends BaseEvent {
    /**
     * {@code true} if the click was made with the off hand, {@code false} if it was made with the
     * main hand.
     */
    public final boolean offhand;
    /**
     * {@code true} if the entity actually reacted to the click, {@code false} if the interaction
     * was only passed along the chain without the entity doing anything. The event does not fire
     * at all when the click was rejected outright, so {@code false} means "passed on", not
     * "failed".
     */
    public final boolean result;
    /**
     * the entity that was clicked. It is wrapped in the most specific
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper} subclass for its
     * type, so it can be narrowed further with {@code asLiving()}, {@code asPlayer()} and the
     * other {@code as...()} helpers. Its type is available from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper#getType() getType()}
     * and its name from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper#getName() getName()}.
     */
    public final EntityHelper<?> entity;

    public EventInteractEntity(boolean offhand, boolean accepted, Entity entity) {
        super(JsMacrosClient.clientCore);
        this.offhand = offhand;
        this.result = accepted;
        this.entity = EntityHelper.create(entity);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"entity\": %s, \"result\": \"%s\"}", this.getEventName(), entity, result);
    }

}
