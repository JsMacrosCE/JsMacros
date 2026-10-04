package com.jsmacrosce.jsmacros.client.api.event.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires once when the Java virtual machine is on its way down, from a shutdown hook that JsMacros
 * registers while the mod initialises. There is no payload at all: the event exists only to say
 * that the client is exiting.<br>
 * Two things follow from where it is raised. It fires on a normal exit and on a crash alike, since
 * a shutdown hook runs either way, so it is not a reliable signal that the game shut down cleanly.
 * And listeners are handed off to JsMacros' script thread pool, which is not waited on, so a
 * listener body is not guaranteed to finish before the process is gone. Keep the work in a
 * listener short and do not rely on it having run.<br>
 * It is also not one of the events a {@code Joined} trigger can be attached to, since those only
 * work on cancellable or explicitly joinable events, and it is not cancellable, so calling
 * {@code cancel()} on it throws.<br>
 * It fires nothing when the game returns to a world and keeps running, so do not use it to detect
 * a disconnect: {@link com.jsmacrosce.jsmacros.client.api.event.impl.world.EventDisconnect} is
 * the event for leaving a server.
 * example:
 * <pre>
 * JsMacros.on("QuitGame", JavaWrapper.methodToJava(function () {
 *   // print writes straight to stdout; Chat.log would need the game thread,
 *   // which is the very thing that is going away here
 *   print("the client is exiting, do any last cleanup here");
 * }))
 * </pre>
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("System/Lifecycle")
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
