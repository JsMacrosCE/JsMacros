package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.world.damagesource.DamageSource;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author FlareStormGaming
 * @since 1.6.5
 */
@Event("Heal")
public class EventHeal extends BaseEvent {
    @DocletReplaceReturn("HealSource")
    @DocletDeclareType(name = "HealSource", type = "DamageSource")
    public final String source;
    public final float health;
    public final float change;

    public EventHeal(DamageSource source, float health, float change) {
        super(JsMacrosClient.clientCore);
        this.source = source.getMsgId();
        this.health = health;
        this.change = change;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"health\": %f, \"change\": %f}", this.getEventName(), health, change);
    }

}
