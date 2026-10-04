package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when a new world is set on the client, which happens when joining a world and whenever
 * the player travels to another dimension.<br>
 * It does not fire when the client leaves a world entirely, use
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.world.EventDisconnect Disconnect} for that.<br>
 * This event is not cancellable.
 * example:
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
     * the id of the dimension the client just switched to, in {@code namespace:path} form,
     * as reported by the world itself. Script type definitions narrow this to a {@code Dimension}
     * union.
     */
    @DocletReplaceReturn("Dimension")
    public final String dimension;

    public EventDimensionChange(String dimension) {
        super(JsMacrosClient.clientCore);
        this.dimension = dimension;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"dimension\": \"%s\"}", this.getEventName(), dimension);
    }

}
