package com.jsmacrosce.jsmacros.core.event.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.config.BaseProfile;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires once a JsMacros profile has finished loading, both at startup with the default profile
 * and every time the profile is switched afterwards.<br>
 * This is the event the main script hangs off. The stock {@code default} profile ships with a
 * {@code ProfileLoad} trigger pointing at {@code index.js}, so that file is what runs when a
 * profile becomes ready, and it is why this is the first event a fresh install sees.<br>
 * It is raised at the end of the profile load, after the profile's macro triggers have been torn
 * down and put back, so a trigger that is part of the profile being loaded is already in place
 * and fires as part of this. Listeners a running script added with {@code JsMacros.on} are not
 * torn down by a profile switch, since only the macro triggers configured in the profile are, so
 * a {@code ProfileLoad} listener registered from a script survives into the next profile and runs
 * again on the next switch.<br>
 * This event is not raised when the config holds the profile's name with a {@code null} trigger
 * list under it, the case {@code loadProfile} calls broken or null in its warning and gives up
 * on. A name the config has no entry for at all is not that case: {@code loadOrCreateProfile}
 * stores an empty list under the name, saves the config, and this event does fire, just with
 * none of that profile's macro triggers to restore. This event is not cancellable, the profile
 * is already loaded by the time listeners see it.
 * example:
 * <pre>
 * // this is the kind of thing index.js does, since the default profile
 * // registers it as a ProfileLoad trigger
 * JsMacros.on("ProfileLoad", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`"${event.profileName}" is ready, setting the rest of the profile up`);
 * }));
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Core")
@Event(value = "ProfileLoad", oldName = "PROFILE_LOAD")
public class EventProfileLoad extends BaseEvent {
    /**
     * the name of the profile that just finished loading. It is also the key the profile's
     * trigger list is stored under in the config and the name the profile is listed under in the
     * settings screen, so the same string is what identifies the profile everywhere.
     */
    public final String profileName;

    public EventProfileLoad(BaseProfile profile, String profileName) {
        super(profile.runner);
        this.profileName = profileName;
    }

    public String toString() {
        return String.format("%s:{\"profileName\": %s}", this.getEventName(), profileName);
    }

}
