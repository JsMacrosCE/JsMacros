package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.doclet.DocletCategory;

import net.minecraft.world.entity.Entity;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when an entity is removed from the client world. The reason is the removal reason's name;
 * the available names depend on the Minecraft version.
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
     * The entity being removed from the client world.
     */
    public final EntityHelper<?> entity;
    /**
     * The removal reason's name; available names depend on the Minecraft version.
     */
    @DocletReplaceReturn("EntityUnloadReason")
    public final String reason;

    /** Wraps the entity and captures the removal reason as a string; it does not perform the removal.
     * Construction throws {@link NullPointerException} if either argument is {@code null}.
     * @param e the Minecraft entity to wrap; must not be {@code null}
     * @param reason the removal reason to convert to a string; must not be {@code null}
     */
    public EventEntityUnload(Entity e, Entity.RemovalReason reason) {
        super(JsMacrosClient.clientCore);
        this.entity = EntityHelper.create(e);
        this.reason = reason.toString();
    }

    /** Returns a diagnostic string containing the event name, entity helper string, and captured reason.
     * The format is {@code %s:{"entity": %s, "reason": "%s"}}; the reason is the captured string,
     * quoted in the output, not a newly computed enum value or serialized entity data.
     * @return the diagnostic string in the format described above
     */
    @Override
    public String toString() {
        return String.format("%s:{\"entity\": %s, \"reason\": \"%s\"}", this.getEventName(), entity.toString(), reason);
    }

}
