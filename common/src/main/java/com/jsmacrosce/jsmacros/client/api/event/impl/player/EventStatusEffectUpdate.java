package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.StatusEffectHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when a status effect is applied to or removed from the local player, for example by a
 * potion, a beacon or a command.<br>
 * Applying an effect that the player already has counts as a change, and reports the previous
 * instance in {@link #oldEffect}, so a potion that refreshes an effect shows up as an add rather
 * than as a removal. The server can also lower an effect's strength or duration, which is also
 * reported as an add.<br>
 * This event is not cancellable, the effect has already been applied or removed by the time
 * listeners see it. Only the local player is covered, effects on other entities do not raise
 * this event.
 * example:
 * <pre>
 * JsMacros.on("StatusEffectUpdate", JavaWrapper.methodToJava(function (event) {
 *   if (event.removed) {
 *     if (event.oldEffect !== null) {
 *       Chat.log(`Lost ${event.oldEffect.getId()}`);
 *     }
 *     return;
 *   }
 *   if (event.newEffect !== null) {
 *     const effect = event.newEffect;
 *     Chat.log(`${effect.getId()} ${effect.getStrength() + 1} for ${effect.getTime()} ticks`);
 *   }
 * }))
 * </pre>
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Player/Stats")
@Event(value = "StatusEffectUpdate")
public class EventStatusEffectUpdate extends BaseEvent {

    /**
     * the effect the player had before this change, or {@code null} if the player did not have
     * this effect before, which is the usual case when an effect is first applied.<br>
     * On a removal this is the effect that went away, so check it before reading it. The field
     * is not {@code null} on the removal path, because the mixin wraps whatever the client
     * still has unconditionally and the wrapper itself does no null check. In the rare case
     * where the client turns out not to have the effect after all, the wrapper is still handed
     * over but has no game object behind it, so reading through it fails rather than returning
     * placeholder data.
     */
    public final StatusEffectHelper oldEffect;
    /**
     * the effect the player has after this change, or {@code null} on a removal.<br>
     * Always check this against {@code null} before reading it, because the event fires for
     * removals too.<br>
     * {@link com.jsmacrosce.jsmacros.client.api.helper.StatusEffectHelper#getStrength() getStrength()}
     * reports the 0-based amplifier, so adding one gives the level the game itself counts the
     * effect at, which is why the example below adds one.
     */
    public final StatusEffectHelper newEffect;
    /**
     * {@code true} if the effect was applied or changed, {@code false} if it was removed. This
     * is the same information as {@link #removed} with the sense flipped, and exactly one of the
     * two is ever {@code true}.
     */
    public final boolean added;
    /**
     * {@code true} if the effect was removed, {@code false} if it was applied or changed.
     */
    public final boolean removed;

    public EventStatusEffectUpdate(StatusEffectHelper oldEffect, StatusEffectHelper newEffect, boolean added) {
        super(JsMacrosClient.clientCore);
        this.oldEffect = oldEffect;
        this.newEffect = newEffect;
        this.added = added;
        this.removed = !added;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"effect\": \"%s\", \"strength\": %d, \"time\": %d, \"change\": %s}", this.getEventName(), added ? newEffect.getId() : oldEffect.getId(), added ? newEffect.getStrength() : oldEffect.getStrength(), added ? newEffect.getTime() : oldEffect.getTime(), added ? "added" : "removed");
    }

}
