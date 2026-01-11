package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.world.damagesource.DamageSource;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "Damage", oldName = "DAMAGE")
public class EventDamage extends BaseEvent {
    /**
     * @deprecated may not work on servers
     */
    @Deprecated
    public final EntityHelper<?> attacker;
    /**
     * @deprecated may not work on servers
     */
    @DocletReplaceReturn("DamageSource")
    @Deprecated
    public final String source;
    public final float health;
    public final float change;

    public EventDamage(DamageSource source, float health, float change) {
        super(JsMacrosClient.clientCore);
        if (source.getEntity() == null) {
            this.attacker = null;
        } else {
            this.attacker = EntityHelper.create(source.getEntity());
        }
        this.source = source.getMsgId();
        this.health = health;
        this.change = change;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"health\": %f, \"change\": %f}", this.getEventName(), health, change);
    }

}
