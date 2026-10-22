package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the local player's hunger bar value changes.<br>
 * Note: on the current build this event does not actually reach listeners. The mixin that
 * detects the change builds the event object but never triggers it, so the event is constructed
 * and immediately discarded. A listener registered for {@code "HungerChange"} will simply never
 * be called until that mixin is fixed. The payload below describes what the event would report
 * once it is.
 * example:
 * <pre>
 * JsMacros.on("HungerChange", JavaWrapper.methodToJava(function (event) {
 *   if (event.foodLevel >= 20) {
 *     Chat.log("Not hungry");
 *   } else if (6 >= event.foodLevel) {
 *     Chat.log(`Starving, ${event.foodLevel} food points left`);
 *   }
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Player/Stats")
@Event(value = "HungerChange", oldName = "HUNGER_CHANGE")
public class EventHungerChange extends BaseEvent {
    /**
     * the player's new hunger value as the hunger bar counts it, from 0 for an empty bar up to 20
     * for a full one. It can go past 20 while the saturation effect is active and below 0 once
     * the player is starving.
     */
    public final int foodLevel;

    public EventHungerChange(int foodLevel) {
        super(JsMacrosClient.clientCore);
        this.foodLevel = foodLevel;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"foodLevel\": %d}", this.getEventName(), foodLevel);
    }

}
