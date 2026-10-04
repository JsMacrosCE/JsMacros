package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.doclet.DocletCategory;

import net.minecraft.world.entity.Entity;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when an entity is added to the client world.<br>
 * This covers every entity the client is told about, including the local player on join and any
 * entity that enters the loaded area, not just mobs.<br>
 * This event is not cancellable, the entity is already part of the client world when it fires.
 * example:
 * <pre>
 * JsMacros.on("EntityLoad", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`${event.entity.getType()} spawned at ${event.entity.getPos()}`);
 * }))
 * </pre>
 */
@DocletCategory("World")
@Event("EntityLoad")
public class EventEntityLoad extends BaseEvent {
    /**
     * the entity that was added. It is wrapped in the most specific
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper} subclass for its
     * type, so it can be narrowed further with {@code asLiving()}, {@code asPlayer()} and the
     * other {@code as...()} helpers. Its type is available from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper#getType() getType()}
     * and its position from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper#getPos() getPos()}.
     */
    public final EntityHelper<?> entity;

    public EventEntityLoad(Entity e) {
        super(JsMacrosClient.clientCore);
        entity = EntityHelper.create(e);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"entity\": %s}", this.getEventName(), entity.toString());
    }

}
