package com.jsmacrosce.jsmacros.core.event.impl;

import com.jsmacrosce.jsmacros.core.config.BaseProfile;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "ProfileLoad", oldName = "PROFILE_LOAD")
public class EventProfileLoad extends BaseEvent {
    public final String profileName;

    public EventProfileLoad(BaseProfile profile, String profileName) {
        super(profile.runner);
        this.profileName = profileName;
    }

    public String toString() {
        return String.format("%s:{\"profileName\": %s}", this.getEventName(), profileName);
    }

}
