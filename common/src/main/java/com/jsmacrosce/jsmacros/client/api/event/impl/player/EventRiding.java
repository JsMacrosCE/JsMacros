package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.world.entity.Entity;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires from a {@code startRiding} return path when the local player attempts to mount, or when
 * the player begins removing an existing vehicle. The mount event does not establish that riding
 * succeeded. {@link #state} is {@code true} for mounting and {@code false} for dismounting;
 * {@link #entity} is the targeted entity or former vehicle, respectively. The dismount event also
 * fires for externally caused dismounts.
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
     * {@code true} when the player started riding, {@code false} when the player dismounted.
     */
    public final boolean state;
    /**
     * The mounted entity when {@link #state} is {@code true}, or the former vehicle when it is
     * {@code false}. It is wrapped in the corresponding {@link EntityHelper} subclass.
     */
    public final EntityHelper<?> entity;

    /**
     * Initializes the event with the supplied state and a wrapped entity; this does not perform a
     * mount or dismount.
     * @param state {@code true} for mounting or {@code false} for dismounting; this flag does not
     *              establish that riding succeeded
     * @param entity raw Minecraft entity to wrap: target when mounting or former vehicle when dismounting; {@code null} fails with {@link NullPointerException} during wrapping
     */
    public EventRiding(boolean state, Entity entity) {
        super(JsMacrosClient.clientCore);
        this.state = state;
        this.entity = EntityHelper.create(entity);
    }

    /**
     * Returns a diagnostic string containing the event name and the state and entity values.
     * @return the event name followed by {@code {"state": boolean, "entity": entity}}, with
     *         the boolean unquoted and the entity represented by its {@code toString()} result
     */
    @Override
    public String toString() {
        return String.format("%s:{\"state\": %b, \"entity\": %s}", this.getEventName(), state, entity.toString());
    }

}
