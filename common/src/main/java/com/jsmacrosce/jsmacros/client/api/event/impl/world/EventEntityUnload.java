package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.doclet.DocletCategory;

import net.minecraft.world.entity.Entity;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when an entity is removed from the client world, together with the reason the client
 * removed it.<br>
 * This fires for the entity dying, and for the client dropping it because it left the loaded
 * area. It is not a "death" event, use
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.player.EventDeath} for the local player
 * dying.<br>
 * This event is not cancellable, the entity is already leaving the client world when it fires.
 * example:
 * <pre>
 * JsMacros.on("EntityUnload", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`${event.entity.getType()} unloaded (${event.reason})`);
 * }))
 * </pre>
 */
@DocletCategory("World")
@Event("EntityUnload")
public class EventEntityUnload extends BaseEvent {
    /**
     * the entity that is being removed. It is wrapped in the most specific
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper} subclass for its
     * type, so it can be narrowed further with {@code asLiving()}, {@code asPlayer()} and the
     * other {@code as...()} helpers. Its type is available from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper#getType() getType()}
     * and its uuid from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper#getUUID() getUUID()}.
     */
    public final EntityHelper<?> entity;
    /**
     * why the entity was removed, the name of the vanilla removal reason. The exact set of
     * reasons depends on the Minecraft version. Script type definitions narrow this to an
     * {@code EntityUnloadReason} union.
     */
    @DocletReplaceReturn("EntityUnloadReason")
    public final String reason;

    public EventEntityUnload(Entity e, Entity.RemovalReason reason) {
        super(JsMacrosClient.clientCore);
        this.entity = EntityHelper.create(e);
        this.reason = reason.toString();
    }

    @Override
    public String toString() {
        return String.format("%s:{\"entity\": %s, \"reason\": \"%s\"}", this.getEventName(), entity.toString(), reason);
    }

}
