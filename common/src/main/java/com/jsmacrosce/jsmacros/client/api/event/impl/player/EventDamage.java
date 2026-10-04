package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.world.damagesource.DamageSource;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the local player's health goes down.<br>
 * This is the local-player companion to
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.world.EventEntityDamaged}, which is
 * world-wide and fires for every living entity the client tracks. For the local player both fire
 * on the same health change, this one and
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.player.EventHealthChange} first and
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.world.EventEntityDamaged} after. In a
 * singleplayer world the integrated server's copy of the player is skipped, so this still fires
 * exactly once.<br>
 * The event is derived from health going down, not from a damage source, so it also fires for
 * health that a mod or a command takes away. It is not cancellable, and it fires just before
 * the new value is written, not after. The mixin injects at the head of {@code setHealth}, so
 * {@code health} already carries the incoming value while reading the player's health back
 * from the world still gives the old one.<br>
 * Note: the client is only told the new health value over the network, never what caused it, so
 * {@link #source} is always the generic damage source and {@link #attacker} is always
 * {@code null}. Both fields are deprecated for that reason.
 * example:
 * <pre>
 * JsMacros.on("Damage", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`Took ${event.change} damage, ${event.health} hp left`);
 *   if (4.0 >= event.health) {
 *     Chat.log("About to die");
 *   }
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Player/Stats")
@Event(value = "Damage", oldName = "DAMAGE")
public class EventDamage extends BaseEvent {
    /**
     * the entity that dealt the damage, or {@code null} if the damage had no attacker.
     *
     * @deprecated may not work on servers
     */
    @Deprecated
    public final EntityHelper<?> attacker;
    /**
     * the id of the damage type that dealt the damage. The client never receives the real damage
     * source, so this is the generic damage source on the current build.
     *
     * @deprecated may not work on servers
     */
    @DocletReplaceReturn("DamageSource")
    @Deprecated
    public final String source;
    /**
     * the player's health after the loss, in half hearts as the vanilla health bar counts them.
     */
    public final float health;
    /**
     * how much health the player lost, the difference between their health before and after the
     * change. Always positive, unlike the matching
     * {@link com.jsmacrosce.jsmacros.client.api.event.impl.player.EventHealthChange#change change}
     * which is negative on the same change.
     */
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
