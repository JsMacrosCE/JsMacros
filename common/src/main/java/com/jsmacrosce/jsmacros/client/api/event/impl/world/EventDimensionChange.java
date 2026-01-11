package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "DimensionChange", oldName = "DIMENSION_CHANGE")
public class EventDimensionChange extends BaseEvent {
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
