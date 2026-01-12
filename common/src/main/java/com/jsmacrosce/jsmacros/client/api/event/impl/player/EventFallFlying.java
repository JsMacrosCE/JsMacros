package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

@DocletCategory("Player/Stats")
@Event("FallFlying")
public class EventFallFlying extends BaseEvent {
    public final boolean state;

    public EventFallFlying(boolean state) {
        super(JsMacrosClient.clientCore);
        this.state = state;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"state\": %s}", this.getEventName(), state);
    }

}
