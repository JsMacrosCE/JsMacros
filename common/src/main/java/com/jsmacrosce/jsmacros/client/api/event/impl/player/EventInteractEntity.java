package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;

import net.minecraft.world.entity.Entity;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires after the local player's entity-interact action returns, except when it returns
 * {@link net.minecraft.world.InteractionResult#FAIL}. {@link #result} is true when the result
 * consumes the action and false when it does not; false is not an event rejection. This event
 * reports the interact action, not attacks, which use
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.player.EventAttackEntity}.
 * <pre>
 * JsMacros.on("InteractEntity", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`${event.result ? "Consumed" : "Passed"}: ${event.entity.getType()}`);
 * }))
 * </pre>
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
     * {@code true} if the interaction result consumes the action, {@code false} if it does not.
     * The event is not fired when the result is {@code FAIL}.
     */
    public final boolean result;
    /**
     * The clicked entity, wrapped in the most specific
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper} subclass for its
     * type, so it can be narrowed with {@code instanceof EntityHelpers.Player} and the other
     * namespace types; {@code asLiving()}, {@code asPlayer()} and the other {@code as...()}
     * helpers are casts. Its type is available from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper#getType() getType()}
     * and its name from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper#getName() getName()}.
     */
    public final EntityHelper<?> entity;

    /**
     * Stores the interaction hand, consumption flag, and wrapped entity; this constructor does
     * not perform or dispatch an interaction.
     *
     * @param offhand whether the supplied hand flag indicates the off hand; {@code false}
     *                 indicates the main hand
     * @param accepted the supplied action-consumption flag, stored as {@code result}
     * @param entity the Minecraft entity to wrap; wrapping fails with {@link NullPointerException}
     *               if it is {@code null}
     */
    public EventInteractEntity(boolean offhand, boolean accepted, Entity entity) {
        super(JsMacrosClient.clientCore);
        this.offhand = offhand;
        this.result = accepted;
        this.entity = EntityHelper.create(entity);
    }

    /**
     * Returns this event's diagnostic string, including its event name, entity representation,
     * and stored result flag.
     *
     * @return a string formatted as {@code %s:{"entity": %s, "result": "%s"}}; the entity
     *         field uses the helper's runtime representation, and the offhand flag is omitted
     */
    @Override
    public String toString() {
        return String.format("%s:{\"entity\": %s, \"result\": \"%s\"}", this.getEventName(), entity, result);
    }

}
