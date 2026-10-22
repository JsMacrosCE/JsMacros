package com.jsmacrosce.jsmacros.client.api.event.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires for one notch of the scroll wheel, or one step of a touchpad scroll, at the very start of
 * the game's own handling of it, before the mouse wheel sensitivity or the discrete scroll setting
 * has been applied and before the game knows what to do with it.<br>
 * It only fires in game, with no screen and no loading overlay open, and only when at least one of
 * the two deltas is not zero. Scrolling in a screen is handled by that screen and never reaches
 * this event, so a menu or an inventory wheel does not show up here, and neither does a scroll that
 * the game would ignore because both deltas are zero.<br>
 * This event is cancellable, and cancelling stops the game from doing anything with that scroll at
 * all. Cancelling is not just a hotbar change that does not happen: the scroll sensitivity and
 * the discrete scroll setting are not applied, a spectator's flight speed does not move, and the
 * framerate limit tracker is not even told that input arrived, which is the first thing the game
 * does with a scroll. Nothing is left half done by a cancel, and later scrolls still fire
 * normally.
 * example:
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
     * how far the wheel moved sideways, in the scroll steps the windowing library reports. Most
     * wheels have no sideways movement at all and report exactly {@code 0.0} here, while a
     * touchpad or a wheel that can be pushed sideways is where a non-zero value comes from.
     * <br>
     * This is the raw step count, not a distance in pixels and not scaled by any sensitivity, and
     * the sign is the direction: positive is one way and negative the other.
     */
    public final double deltaX;
    /**
     * how far the wheel moved up and down, in the scroll steps the windowing library reports, with
     * a positive value for one direction and a negative value for the other. The game's own
     * sensitivity setting and its discrete scroll option have not been applied yet, so a notch
     * arrives as the same {@code 1.0} or {@code -1.0} whatever the player's settings say.
     * <br>
     * The sign follows the windowing library rather than the screen, so a positive value means
     * scrolling away from the player, which is the same as the hotbar selection moving towards
     * slot zero.
     */
    public final double deltaY;

    public EventMouseScroll(double deltaX, double deltaY) {
        super(JsMacrosClient.clientCore);
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"deltaX\": %s, \"deltaY\": %s}", this.getEventName(), deltaX, deltaY);
    }

}
