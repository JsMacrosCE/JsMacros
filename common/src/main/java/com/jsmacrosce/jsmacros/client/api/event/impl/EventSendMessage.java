package com.jsmacrosce.jsmacros.client.api.event.impl;

import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "SendMessage", oldName = "SEND_MESSAGE", cancellable = true)
public class EventSendMessage extends BaseEvent {
    @Nullable
    public String message;

    @SuppressWarnings("NullableProblems")
    public EventSendMessage(String message) {
        super(JsMacrosClient.clientCore);
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"message\": \"%s\"}", this.getEventName(), message);
    }

}
