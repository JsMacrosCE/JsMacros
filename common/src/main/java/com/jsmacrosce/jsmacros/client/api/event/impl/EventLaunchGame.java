package com.jsmacrosce.jsmacros.client.api.event.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("System/Lifecycle")
@Event(value = "LaunchGame")
public class EventLaunchGame extends BaseEvent {

    public final String playerName;

    public EventLaunchGame(String playerName) {
        super(JsMacrosClient.clientCore);
        this.playerName = playerName;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"name\": \"%s\"}", this.getEventName(), playerName);
    }

}
