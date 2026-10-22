package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires before the client writes the local player's experience values. The current fields contain
 * the incoming values and the {@code prev} fields contain the prior values. It fires on every set,
 * including unchanged values, and is not cancellable.
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
     * Experience progress value being applied.
     */
    public final float progress;
    /**
     * Total experience value being applied.
     */
    public final int total;
    /**
     * Experience level value being applied.
     */
    public final int level;
    /**
     * The prior experience progress value.
     *
     * @since 1.6.5
     */
    public final float prevProgress;
    /**
     * The prior total experience value.
     *
     * @since 1.6.5
     */
    public final int prevTotal;
    /**
     * The prior experience level value.
     *
     * @since 1.6.5
     */
    public final int prevLevel;

    /**
     * Stores the supplied incoming experience values and their supplied prior values.
     *
     * @param progress incoming experience progress value
     * @param total incoming total experience value
     * @param level incoming experience level value
     * @param prevProgress supplied prior experience progress value
     * @param prevTotal supplied prior total experience value
     * @param prevLevel supplied prior experience level value
     */
    public EventEXPChange(float progress, int total, int level, float prevProgress, int prevTotal, int prevLevel) {
        super(JsMacrosClient.clientCore);
        this.progress = progress;
        this.total = total;
        this.level = level;

        this.prevProgress = prevProgress;
        this.prevTotal = prevTotal;
        this.prevLevel = prevLevel;
    }

    /**
     * Returns an event-name-prefixed diagnostic containing only the stored total experience value.
     * Its exact format is {@code %s:{"total": %d}}; this is not a complete event payload or serialization.
     *
     * @return event name and stored total experience in the diagnostic format
     */
    @Override
    public String toString() {
        return String.format("%s:{\"total\": %d}", this.getEventName(), total);
    }

}
