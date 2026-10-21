package com.jsmacrosce.jsmacros.client.api.event.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires before the game handles a scroll input, only when no screen or overlay is open, a player is
 * present, and at least one raw offset is nonzero. The offsets have not yet been scaled by mouse
 * wheel sensitivity or converted by the discrete-scroll setting. Cancelling prevents the game's
 * scroll handling, including its input notification and gameplay effects.
 * <pre>
 * JsMacros.on("MouseScroll", JavaWrapper.methodToJava(function (event) {
 *   if (event.deltaY > 0.0) {
 *     // positive is up, negative is down
 *     Chat.log(`scrolled up by ${event.deltaY}`);
 *     event.cancel();
 *   }
 * }))
 * </pre>
 * @author aMelonRind
 * @since 1.9.0
 */
@DocletCategory("Inputs/Interactions")
@Event(value = "MouseScroll", cancellable = true)
public class EventMouseScroll extends BaseEvent {
    /**
     * Raw horizontal scroll offset from the windowing library, before game sensitivity or discrete-
     * scroll processing.
     */
    public final double deltaX;
    /**
     * Raw vertical scroll offset from the windowing library, before game sensitivity or discrete-
     * scroll processing.
     */
    public final double deltaY;

    /**
     * Initializes the event with the supplied raw scroll offsets; construction does not dispatch it.
     * @param deltaX the horizontal scroll offset, stored unchanged
     * @param deltaY the vertical scroll offset, stored unchanged
     */
    public EventMouseScroll(double deltaX, double deltaY) {
        super(JsMacrosClient.clientCore);
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    /**
     * Returns a diagnostic string containing the event name and both scroll offsets.
     * The result uses the format {@code %s:{"deltaX": %s, "deltaY": %s}}; it is not JSON, and
     * the offsets use {@code Double} string formatting without finite-value validation.
     * @return the event name and horizontal and vertical offsets in diagnostic form
     */
    @Override
    public String toString() {
        return String.format("%s:{\"deltaX\": %s, \"deltaY\": %s}", this.getEventName(), deltaX, deltaY);
    }

}
