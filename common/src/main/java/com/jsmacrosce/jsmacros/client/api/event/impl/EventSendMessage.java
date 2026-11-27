package com.jsmacrosce.jsmacros.client.api.event.impl;

import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires for text submitted through the chat screen, before the game normalizes it. Cancelling
 * prevents sending and chat-history insertion. A changed, nonempty value is sent directly without
 * normalization or history insertion; {@code null} or an empty replacement is not sent. A leading
 * {@code /} selects command submission, with the slash removed.
 * <pre>
 * JsMacros.on("SendMessage", true, JavaWrapper.methodToJava(function (event) {
 *   if (event.getMessage() === null) {
 *     return;
 *   }
 *   if (event.getMessage() === "/hello") {
 *     // swallow it, nothing is sent and nothing is added to the history
 *     event.cancel();
 *     return;
 *   }
 *   // rewrite it, the client sends this instead of what was typed
 *   event.setMessage(`say ${event.getMessage()}`);
 * }))
 * </pre>
 * @since 1.2.7
 */
@DocletCategory("Network/Chat")
@Event(value = "SendMessage", oldName = "SEND_MESSAGE", cancellable = true)
public class EventSendMessage extends BaseEvent {
    /**
     * The raw text submitted in the chat screen. A changed, nonempty value is sent without
     * normalization or history insertion; a leading {@code /} selects command submission, with the
     * slash removed. {@code null}, an empty string, or cancelling the event prevents sending.
     * @deprecated Removal in 3.0.0: Use {@link #getMessage()} and {@link #setMessage(String)} instead.
     */
    @Nullable
    @Deprecated
    public String message;

    /**
     * Returns the raw text submitted in the chat screen. {@code null} means no text will be sent.
     *
     * @return the current message text, or {@code null}
     * @since 2.2.0
     */
    @Nullable
    public String getMessage() {
        return message;
    }

    /**
     * Replaces the text that will be sent. A changed, nonempty value is sent without normalization
     * or history insertion; a leading {@code /} selects command submission, with the slash
     * removed. {@code null}, an empty string, or cancelling the event prevents sending.
     *
     * @param message replacement text, or {@code null} to prevent sending
     * @throws IllegalStateException if this delivery is not joined or its event lock was released
     * @since 2.2.0
     */
    public void setMessage(@Nullable String message) {
        modify("setMessage", () -> this.message = message);
    }

    /**
     * Initializes the event with the supplied message; construction does not dispatch the event.
     * @param message initial message text, stored verbatim
     */
    @SuppressWarnings("NullableProblems")
    public EventSendMessage(String message) {
        super(JsMacrosClient.clientCore);
        this.message = message;
    }

    /**
     * Returns a diagnostic string containing the event name and current message text. The message
     * is not escaped, so this representation is not JSON.
     * @return event name and current message text in the form {@code %s:{"message": "%s"}}
     */
    @Override
    public String toString() {
        return String.format("%s:{\"message\": \"%s\"}", this.getEventName(), getMessage());
    }

}
