package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.world.entity.Entity;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @since 1.5.0
 */
@Event("Riding")
public class EventRiding extends BaseEvent {
    public final boolean state;
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
