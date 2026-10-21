package com.jsmacrosce.jsmacros.client.api.event.impl;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the client receives a title, subtitle, or action-bar message; {@link #type} identifies
 * the HUD line. Action-bar messages include client-side system messages. Listeners can replace
 * {@link #message}, assign {@code null}, or cancel the event to suppress the message.
 * <pre>
 * JsMacros.on("Title", JavaWrapper.methodToJava(function (event) {
 *   if (event.message === null) {
 *     return;
 *   }
 *   if (event.type === "ACTIONBAR") {
 *     if (event.message.getString().includes("moved too quickly")) {
 *       // hide the lag message, the game shows nothing in its place
 *       event.cancel();
 *       return;
 *     }
 *   }
 *   if (event.type === "TITLE") {
 *     // keep the line but put something else in it
 *     event.message = Chat.createTextHelperFromString("hello");
 *   }
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Render/UI")
@Event(value = "Title", oldName = "TITLE", cancellable = true)
public class EventTitle extends BaseEvent {
    /**
     * Identifies the message's HUD line: {@code 'TITLE'}, {@code 'SUBTITLE'}, or {@code 'ACTIONBAR'}.
     * Client-side system messages use {@code 'ACTIONBAR'}.
     */
    @DocletReplaceReturn("TitleType")
    @DocletDeclareType(name = "TitleType", type = "'TITLE' | 'SUBTITLE' | 'ACTIONBAR'")
    public final String type;
    /**
     * Message to display, wrapped in a {@link TextHelper}; assigning a replacement changes the
     * displayed text. {@code null} or cancelling passes no component to the game. For action-bar
     * messages, the usual display timer is still reset when suppressed.
     */
    @Nullable
    public TextHelper message;

    /**
     * Initializes this event with a HUD line identifier and its supplied component; this does not dispatch or display the event.
     * The identifier is stored verbatim and is not checked against {@code TITLE}, {@code SUBTITLE}, or {@code ACTIONBAR}.
     * The component is wrapped by {@link TextHelper#wrap(Component)} without cloning it; a {@code null} component produces a {@code null} helper.
     * @param type supplied HUD line identifier, conventionally {@code TITLE}, {@code SUBTITLE}, or {@code ACTIONBAR}
     * @param message supplied component, or {@code null}
     */
    public EventTitle(String type, Component message) {
        super(JsMacrosClient.clientCore);
        this.type = type;
        this.message = TextHelper.wrap(message);
    }

    /**
     * Returns a diagnostic string containing the event name, type, and current message helper's string representation.
     * The format is {@code %s:{"type": "%s", "message": "%s"}}; it is not JSON serialization, and values are not escaped.
     * A {@code null} current message is represented as the literal {@code null} inside the quoted message field.
     * @return diagnostic text with the event name, type, and current message helper representation
     */
    @Override
    public String toString() {
        return String.format("%s:{\"type\": \"%s\", \"message\": \"%s\"}", this.getEventName(), type, message);
    }

}
