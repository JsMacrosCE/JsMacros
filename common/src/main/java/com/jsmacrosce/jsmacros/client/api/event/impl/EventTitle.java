package com.jsmacrosce.jsmacros.client.api.event.impl;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "Title", oldName = "TITLE", cancellable = true)
public class EventTitle extends BaseEvent {
    @DocletReplaceReturn("TitleType")
    @DocletDeclareType(name = "TitleType", type = "'TITLE' | 'SUBTITLE' | 'ACTIONBAR'")
    public final String type;
    @Nullable
    public TextHelper message;

    public EventTitle(String type, Component message) {
        super(JsMacrosClient.clientCore);
        this.type = type;
        this.message = TextHelper.wrap(message);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"type\": \"%s\", \"message\": \"%s\"}", this.getEventName(), type, message);
    }

}
