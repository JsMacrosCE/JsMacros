package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the local player's experience values are set, which the client does whenever the
 * server sends new experience values, for example after the player levels up or loses experience
 * on death.<br>
 * The event fires before the new values are written to the player, so {@link #progress},
 * {@link #total} and {@link #level} are the values being applied and the three {@code prev}
 * fields are what the player had before. Nothing is checked for having actually changed, so the
 * same values can be reported more than once, including once when the player first joins.<br>
 * This event is not cancellable, the new experience values are applied to the player no matter
 * what a listener does.
 * example:
 * <pre>
 * JsMacros.on("EXPChange", JavaWrapper.methodToJava(function (event) {
 *   if (event.level !== event.prevLevel) {
 *     if (event.level > event.prevLevel) {
 *       Chat.log(`Level up, now level ${event.level} with ${event.total} total xp`);
 *     } else {
 *       Chat.log(`Lost levels, back to level ${event.level}`);
 *     }
 *   } else {
 *     Chat.log(`Progress towards level ${event.level + 1} is ${event.progress}`);
 *   }
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Player/Stats")
@Event(value = "EXPChange", oldName = "EXP_CHANGE")
public class EventEXPChange extends BaseEvent {
    /**
     * the progress towards the next level that is being applied, from 0 up to 1. Compare it with
     * {@link #prevProgress} to see how far the bar moved.
     */
    public final float progress;
    /**
     * the player's total accumulated experience that is being applied. This is the number shown
     * as green XP on the stats screen, not the level.
     */
    public final int total;
    /**
     * the experience level that is being applied, so level 0 means no levels yet.
     */
    public final int level;
    /**
     * the player's progress towards the next level from before this change.
     *
     * @since 1.6.5
     */
    public final float prevProgress;
    /**
     * the player's total accumulated experience from before this change.
     *
     * @since 1.6.5
     */
    public final int prevTotal;
    /**
     * the player's experience level from before this change.
     *
     * @since 1.6.5
     */
    public final int prevLevel;

    public EventEXPChange(float progress, int total, int level, float prevProgress, int prevTotal, int prevLevel) {
        super(JsMacrosClient.clientCore);
        this.progress = progress;
        this.total = total;
        this.level = level;

        this.prevProgress = prevProgress;
        this.prevTotal = prevTotal;
        this.prevLevel = prevLevel;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"total\": %d}", this.getEventName(), total);
    }

}
