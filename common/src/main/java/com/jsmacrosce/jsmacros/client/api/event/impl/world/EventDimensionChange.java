package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the client begins setting a non-null world, such as when joining or changing
 * dimensions. It does not fire when leaving a world; use
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.world.EventDisconnect Disconnect} instead.
 * <pre>
 * JsMacros.on("DimensionChange", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`Dimension changed to ${event.dimension}`);
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("World")
@Event(value = "DimensionChange", oldName = "DIMENSION_CHANGE")
public class EventDimensionChange extends BaseEvent {
    /**
     * The new world's dimension ID in {@code namespace:path} form; script type definitions narrow
     * this value to a {@code Dimension} union.
     */
    @DocletReplaceReturn("Dimension")
    public final String dimension;

    /**
     * Stores the supplied dimension label in this event without validating or normalizing it.
     * @param dimension dimension identifier label, conventionally in {@code namespace:path} form;
     *                  may be {@code null} or any string
     */
    public EventDimensionChange(String dimension) {
        super(JsMacrosClient.clientCore);
        this.dimension = dimension;
    }

    /**
     * Returns a diagnostic representation containing the event name and stored dimension label.
     * The label is quoted but not escaped, so this is not guaranteed to be valid JSON; a null
     * label appears as the quoted text {@code "null"}.
     * @return the event name followed by {@code :{"dimension": "label"}}, with the stored
     *         label substituted verbatim
     */
    @Override
    public String toString() {
        return String.format("%s:{\"dimension\": \"%s\"}", this.getEventName(), dimension);
    }

}
