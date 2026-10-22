package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;

import net.minecraft.world.entity.Entity;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the local player attacks an entity with the attack button.<br>
 * This covers the attack itself only, not the swing animation and not the damage the target
 * takes. It fires for the hit even if the swing was already mid-swing, and the server still
 * decides what actually happens to the target.<br>
 * This event is not cancellable, the attack has already been sent on to the server by the time
 * listeners see it.
 * example:
 * <pre>
 * JsMacros.on("AttackEntity", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`Attacked ${event.entity.getType()} named ${event.entity.getName().getString()}`);
 * }))
 * </pre>
 */
@DocletCategory("Inputs/Interactions")
@Event("AttackEntity")
public class EventAttackEntity extends BaseEvent {
    /**
     * the entity that was attacked. It is wrapped in the most specific
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper} subclass for its
     * type, so it can be narrowed further with {@code asLiving()}, {@code asPlayer()} and the
     * other {@code as...()} helpers. Its type is available from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper#getType() getType()}
     * and its name from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper#getName() getName()}.
     */
    public final EntityHelper<?> entity;

    public EventAttackEntity(Entity entity) {
        super(JsMacrosClient.clientCore);
        this.entity = EntityHelper.create(entity);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"entity\": %s}", this.getEventName(), entity);
    }

}
