package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;

import net.minecraft.world.entity.Entity;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires after the local player's entity-attack action. This event is not
 * cancellable.
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
     * The attacked entity, wrapped in the most specific
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper} subclass for its
     * type. Use {@code as...()} helpers to narrow the wrapper; its type and name are available from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper#getType() getType()}
     * and {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper#getName() getName()}.
     */
    public final EntityHelper<?> entity;

    /**
     * Initializes the event with the client core and wraps the supplied entity.
     * The helper refers to the supplied entity; it is not a copy or snapshot.
     * A {@code null} entity causes {@link NullPointerException} during wrapping.
     *
     * @param entity the Minecraft entity to wrap
     */
    public EventAttackEntity(Entity entity) {
        super(JsMacrosClient.clientCore);
        this.entity = EntityHelper.create(entity);
    }

    /**
     * Returns a diagnostic string containing the event name and stored entity helper.
     * The helper's representation is determined by its runtime implementation; the
     * result uses the literal format {@code %s:{"entity": %s}} and is not JSON as a whole.
     *
     * @return the formatted event name and entity-helper representation
     */
    @Override
    public String toString() {
        return String.format("%s:{\"entity\": %s}", this.getEventName(), entity);
    }

}
