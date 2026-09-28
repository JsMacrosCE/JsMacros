package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.world.damagesource.DamageSource;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the local player's health goes up.<br>
 * This is the local-player companion to
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.world.EventEntityHealed}, which is
 * world-wide and fires for every living entity the client tracks. For the local player both fire
 * on the same health change, this one and
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.player.EventHealthChange} first and
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.world.EventEntityHealed} after. In a
 * singleplayer world the integrated server's copy of the player is skipped, so this still fires
 * exactly once.<br>
 * The event is derived from health going up, not from a cause, so it also fires for health a mod,
 * a command or an effect restored. It is not cancellable, and it fires just before the new value
 * is written, not after. The mixin injects at the head of {@code setHealth}, so {@code health}
 * already carries the incoming value while reading the player's health back from the world still
 * gives the old one.<br>
 * Note: the client is only told the new health value over the network, never what caused it, so
 * {@link #source} is always the generic damage source, which is the game's placeholder for "no
 * particular cause".
 * example:
 * <pre>
 * JsMacros.on("Heal", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`Healed ${event.change} hp, now at ${event.health} hp`);
 * }))
 * </pre>
 * @author FlareStormGaming
 * @since 1.6.5
 */
@DocletCategory("Player/Stats")
@Event("Heal")
public class EventHeal extends BaseEvent {
    /**
     * the id of the damage type that was used as the placeholder cause of the healing. The client
     * never receives the real cause, so this is the generic damage source on the current build.
     * Script type definitions refer to it through the {@code HealSource} alias.
     */
    @DocletReplaceReturn("HealSource")
    @DocletDeclareType(name = "HealSource", type = "DamageSource")
    public final String source;
    /**
     * the player's health after the gain, in half hearts as the vanilla health bar counts them.
     */
    public final float health;
    /**
     * how much health the player gained, the difference between their health before and after
     * the change. Always positive.
     */
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
