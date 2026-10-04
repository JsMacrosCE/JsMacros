package com.jsmacrosce.jsmacros.client.api.event.impl;

import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the player presses enter in the chat screen to send what they typed, before the client
 * looks at it. Only that one path goes through here: a message the server sends, a message another
 * mod injects, and a command run from anywhere but the chat box all bypass this event.<br>
 * The event is raised before the game normalises what was typed, so {@link #message} is the raw
 * contents of the chat box, with whatever leading and trailing space is in it. The game trims and
 * normalises that later, so the event can see text that the game would not have sent unchanged.<br>
 * This event is cancellable, and cancelling stops the message being sent at all. It is not just
 * that the server never sees it: the text is not added to the chat history either. The chat screen
 * still closes either way, since that happens in the game's own key handling around the call this
 * event is raised from.<br>
 * A listener can also rewrite the message instead of cancelling. Assigning something different
 * makes the client send that instead, and it is sent without the trimming the game would have
 * applied and without being added to the history, so check for an empty string yourself if that
 * matters. A leading {@code /} still means a command and is stripped before the command is run.
 * example:
 * <pre>
 * JsMacros.on("SendMessage", JavaWrapper.methodToJava(function (event) {
 *   if (event.message === null) {
 *     return;
 *   }
 *   if (event.message === "/hello") {
 *     // swallow it, nothing is sent and nothing is added to the history
 *     event.cancel();
 *     return;
 *   }
 *   // rewrite it, the client sends this instead of what was typed
 *   event.message = `say ${event.message}`;
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Network/Chat")
@Event(value = "SendMessage", oldName = "SEND_MESSAGE", cancellable = true)
public class EventSendMessage extends BaseEvent {
    /**
     * what the player typed, as it was in the chat box before the game trimmed and normalised it.<br>
     * This field is writable, and assigning to it is what replaces the message. A command is still
     * recognised by its leading {@code /}, which is stripped before the command is run, while a
     * message without one is sent as chat.<br>
     * Note: it is declared nullable here, so script type definitions allow it to be {@code null} and
     * it has to be checked before it is read. Assigning {@code null} to it means nothing is sent,
     * which is the same as cancelling the event.
     */
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
