package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import net.minecraft.world.entity.Entity;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

@Event("EntityLoad")
public class EventEntityLoad extends BaseEvent {
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
