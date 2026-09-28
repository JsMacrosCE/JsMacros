package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.world.entity.Entity;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the local player starts or stops riding an entity.<br>
 * {@link #state} says which side of the change this is, and {@link #entity} is the entity
 * involved: the one being mounted when {@code state} is {@code true} and the one being left
 * behind when it is {@code false}.<br>
 * The two sides are not symmetric. The dismount side is raised from the game's
 * {@code removeVehicle} call and only when the player actually had a vehicle, so it cannot fire
 * twice for one dismount, and it also fires for dismounts the player did not ask for, such as
 * being thrown off or the vehicle being destroyed.<br>
 * Note: the mount side is raised from the second return of the game's {@code startRiding}
 * method. On the versions this branch builds against that return is the early exit taken when
 * the target entity reports that it cannot accept a passenger, so whether the mount side fires
 * on a successful mount is not verified. Treat a listener for this event as best effort.<br>
 * This event is not cancellable, the mount state is applied to the player no matter what a
 * listener does.
 * example:
 * <pre>
 * JsMacros.on("Riding", JavaWrapper.methodToJava(function (event) {
 *   if (event.state) {
 *     Chat.log(`Mounting ${event.entity.getType()}`);
 *   } else {
 *     Chat.log(`Dismounted from ${event.entity.getType()}`);
 *   }
 * }))
 * </pre>
 * @since 1.5.0
 */
@DocletCategory("Player/Stats")
@Event("Riding")
public class EventRiding extends BaseEvent {
    /**
     * {@code true} if the player just started riding, {@code false} if the player just stopped.
     */
    public final boolean state;
    /**
     * the entity being mounted when {@link #state} is {@code true}, and the entity being left
     * behind when it is {@code false}. It is wrapped in the most specific
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper} subclass for its
     * type, so it can be narrowed further with {@code asLiving()}, {@code asPlayer()} and the
     * other {@code as...()} helpers. Its type is available from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper#getType() getType()}
     * and its name from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper#getName() getName()}.
     */
    public final EntityHelper<?> entity;

    public EventRiding(boolean state, Entity entity) {
        super(JsMacrosClient.clientCore);
        this.state = state;
        this.entity = EntityHelper.create(entity);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"state\": %b, \"entity\": %s}", this.getEventName(), state, entity.toString());
    }

}
