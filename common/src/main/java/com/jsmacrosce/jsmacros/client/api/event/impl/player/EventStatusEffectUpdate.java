package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.StatusEffectHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires while the client processes a status-effect update or removal packet for the local
 * player. It is not cancellable, and listeners run before the packet's effect change is applied.
 * Updates (including refreshes or downgrades) set {@link #added}; removals set {@link #removed}.
 * Effects on other entities do not raise this event.
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
     * A wrapper for the effect stored on the player before the packet is applied, or
     * {@code null} if none is stored. On removal, the wrapper is created even if the effect is
     * absent; in that case {@link StatusEffectHelper#getRaw()} returns {@code null}, and effect
     * accessors cannot be used.
     */
    public final StatusEffectHelper oldEffect;
    /**
     * The effect specified by an update packet, or {@code null} on a removal. Its strength is
     * the zero-based amplifier returned by
     * {@link com.jsmacrosce.jsmacros.client.api.helper.StatusEffectHelper#getStrength()}.
     */
    public final StatusEffectHelper newEffect;
    /**
     * {@code true} for an update packet (including a refresh); {@code false} for a removal.
     */
    public final boolean added;
    /**
     * {@code true} for a removal packet; {@code false} for an update packet.
     */
    public final boolean removed;

    /**
     * Initializes the event with the supplied effect helpers and update flag; the helpers are
     * stored as supplied, including {@code null}, and {@code removed} is the complement of
     * {@code added}.
     * @param oldEffect supplied helper for the prior effect, or {@code null}
     * @param newEffect supplied helper for the incoming effect, or {@code null}
     * @param added {@code true} for an update or {@code false} for a removal; this records the
     *     event's change type and does not guarantee an effect is present or an update succeeds
     */
    public EventStatusEffectUpdate(StatusEffectHelper oldEffect, StatusEffectHelper newEffect, boolean added) {
        super(JsMacrosClient.clientCore);
        this.oldEffect = oldEffect;
        this.newEffect = newEffect;
        this.added = added;
        this.removed = !added;
    }

    /**
     * Formats a diagnostic string using the selected effect helper: {@code newEffect} for an
     * update and {@code oldEffect} for a removal. The selected helper's accessors are evaluated
     * when called; a {@code null} selected helper or one whose raw effect is {@code null} causes
     * a {@link NullPointerException}. The change label is unquoted, so the result is not JSON.
     * @return a formatted diagnostic containing the selected effect's ID, strength, duration, and
     *     added/removed label
     */
    @Override
    public String toString() {
        return String.format("%s:{\"effect\": \"%s\", \"strength\": %d, \"time\": %d, \"change\": %s}", this.getEventName(), added ? newEffect.getId() : oldEffect.getId(), added ? newEffect.getStrength() : oldEffect.getStrength(), added ? newEffect.getTime() : oldEffect.getTime(), added ? "added" : "removed");
    }

}
