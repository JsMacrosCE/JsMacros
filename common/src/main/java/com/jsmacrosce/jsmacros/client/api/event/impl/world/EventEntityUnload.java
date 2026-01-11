package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import net.minecraft.world.entity.Entity;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

@Event("EntityUnload")
public class EventEntityUnload extends BaseEvent {
    public final EntityHelper<?> entity;
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
