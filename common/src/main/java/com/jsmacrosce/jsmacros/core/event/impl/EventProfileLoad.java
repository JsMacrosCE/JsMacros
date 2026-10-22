package com.jsmacrosce.jsmacros.core.event.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.config.BaseProfile;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires after a profile's script triggers have been registered, including at startup and on profile
 * switches. Script event listeners remain registered when triggers are replaced, so listeners can
 * observe subsequent loads. No event is fired when the profile has no trigger list; a missing profile
 * is created with an empty list and does fire this event.
 * <pre>
 * // The default profile's index.js can register a listener for later profile switches.
 * JsMacros.on("ProfileLoad", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`"${event.profileName}" is ready, setting the rest of the profile up`);
 * }));
 * </pre>
 * @since 1.2.7
 */
@DocletCategory("Core")
@Event(value = "ProfileLoad", oldName = "PROFILE_LOAD")
public class EventProfileLoad extends BaseEvent {
    /**
     * The name of the profile that was loaded.
     */
    public final String profileName;

    /**
     * Initializes this event with the source profile's runner and stores the supplied profile name.
     *
     * @param profile source of the event core
     * @param profileName name stored in the event payload
     */
    public EventProfileLoad(BaseProfile profile, String profileName) {
        super(profile.runner);
        this.profileName = profileName;
    }

    /**
     * Returns diagnostic text naming this event and its profile, formatted as
     * {@code eventName:{"profileName": profileName}}. This is not a JSON serialization contract.
     *
     * @return diagnostic text naming the event and profile
     */
    public String toString() {
        return String.format("%s:{\"profileName\": %s}", this.getEventName(), profileName);
    }

}
