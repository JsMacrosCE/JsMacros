package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.world.entity.Entity;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

@Event("AttackEntity")
public class EventAttackEntity extends BaseEvent {
    public final EntityHelper<?> entity;

    public EventAttackEntity(Entity entity) {
        super(JsMacrosClient.clientCore);
        this.entity = EntityHelper.create(entity);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"entity\": %s}", this.getEventName(), entity);
    }

}
