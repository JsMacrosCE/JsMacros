package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.doclet.DocletCategory;

import net.minecraft.world.entity.Entity;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires after an entity is added to the client world. This event is not cancellable.
 */
@DocletCategory("World")
@Event("EntityLoad")
public class EventEntityLoad extends BaseEvent {
    /**
     * Helper for the entity added to the client world.
     */
    public final EntityHelper<?> entity;

    /**
     * Wraps the supplied entity for this event. A {@code null} entity causes
     * construction to fail with {@link NullPointerException}.
     *
     * @param e the raw entity to wrap; cannot be {@code null}
     */
    public EventEntityLoad(Entity e) {
        super(JsMacrosClient.clientCore);
        entity = EntityHelper.create(e);
    }

    /**
     * Returns a diagnostic string containing the event name and the current
     * value of {@link #entity}'s {@code toString()}.
     *
     * @return the string in the form {@code %s:{"entity": %s}}, with the event
     *         name and entity-helper string substituted for the placeholders
     */
    @Override
    public String toString() {
        return String.format("%s:{\"entity\": %s}", this.getEventName(), entity.toString());
    }

}
