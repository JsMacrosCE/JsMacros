package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.world.entity.Entity;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

@Event("InteractEntity")
public class EventInteractEntity extends BaseEvent {
    public final boolean offhand;
    public final boolean result;
    public final EntityHelper<?> entity;

    public EventInteractEntity(boolean offhand, boolean accepted, Entity entity) {
        super(JsMacrosClient.clientCore);
        this.offhand = offhand;
        this.result = accepted;
        this.entity = EntityHelper.create(entity);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"entity\": %s, \"result\": \"%s\"}", this.getEventName(), entity, result);
    }

}
