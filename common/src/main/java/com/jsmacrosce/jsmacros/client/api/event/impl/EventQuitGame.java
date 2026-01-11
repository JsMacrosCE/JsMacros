package com.jsmacrosce.jsmacros.client.api.event.impl;

import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Event(value = "QuitGame")
public class EventQuitGame extends BaseEvent {

    public EventQuitGame() {
        super(JsMacrosClient.clientCore);
    }

    @Override
    public String toString() {
        return String.format("%s:{}", this.getEventName());
    }

}
