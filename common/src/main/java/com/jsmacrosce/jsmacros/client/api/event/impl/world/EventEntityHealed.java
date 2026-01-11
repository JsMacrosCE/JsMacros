package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import net.minecraft.world.entity.Entity;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author FlareStormGaming
 * @since 1.6.5
 */

@Event("EntityHealed")
public class EventEntityHealed extends BaseEvent {
    public final EntityHelper<?> entity;
    public final float health;
    public final float damage;

    public EventEntityHealed(Entity e, float health, float amount) {
        super(JsMacrosClient.clientCore);
        entity = EntityHelper.create(e);
        this.health = health;
        this.damage = amount;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"entity\": %s, \"health\": %f, \"damage\": %f}", this.getEventName(), entity.toString(), health, damage);
    }

}
