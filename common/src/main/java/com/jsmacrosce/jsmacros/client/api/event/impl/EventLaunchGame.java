package com.jsmacrosce.jsmacros.client.api.event.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires at the head of the client's {@code run} method and provides the client session's name.
 * This event is not cancellable.
 * <pre>
 * JsMacros.on("LaunchGame", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`client starting as ${event.playerName}`);
 * }))
 * </pre>
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("System/Lifecycle")
@Event(value = "LaunchGame")
public class EventLaunchGame extends BaseEvent {

    /**
     * The name from the client session, captured when the client's {@code run} method starts.
     */
    public final String playerName;

    /**
     * Sets this event's runner to the client core and stores the supplied name without validation.
     * Construction does not trigger or dispatch the event.
     *
     * @param playerName the supplied player name; no session lookup is performed
     */
    public EventLaunchGame(String playerName) {
        super(JsMacrosClient.clientCore);
        this.playerName = playerName;
    }

    /**
     * Formats a diagnostic string as {@code %s:{"name": "%s"}}, using the event name and supplied
     * player name. The name is not escaped, so this output is not necessarily valid JSON.
     *
     * @return the diagnostic string containing the event name and supplied player name
     */
    @Override
    public String toString() {
        return String.format("%s:{\"name\": \"%s\"}", this.getEventName(), playerName);
    }

}
