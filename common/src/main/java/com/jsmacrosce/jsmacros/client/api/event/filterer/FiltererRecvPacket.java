package com.jsmacrosce.jsmacros.client.api.event.filterer;

import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.event.impl.EventRecvPacket;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.EventFilterer;

/**
 * @author aMelonRind
 * @since 1.9.1
 */
@SuppressWarnings("unused")
public class FiltererRecvPacket implements EventFilterer {
    @Nullable
    @DocletReplaceReturn("PacketName | null")
    public String type;

    @Override
    public boolean canFilter(String event) {
        return "RecvPacket".equals(event);
    }

    @Override
    public boolean test(BaseEvent event) {
        return (event instanceof EventRecvPacket e) && (type == null || e.type.equals(type));
    }

    @DocletReplaceParams("type: PacketName | null")
    public FiltererRecvPacket setType(@Nullable String type) {
        this.type = type;
        return this;
    }

}
