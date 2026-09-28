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
 * Fires for every line of text the server puts on the HUD above the hotbar, which is the big title
 * that fades in and out, the smaller line under it, and the action bar line over the hotbar. The
 * {@link #type} says which of the three it is, and they are three separate places on screen, so
 * replacing one does not disturb the others.<br>
 * The action bar is also what a client side system message is drawn on, so a system message shows
 * up here as an {@code 'ACTIONBAR'} rather than as a chat line, and a listener that hides every
 * action bar line hides those too.<br>
 * This event is cancellable, and cancelling hides the line rather than merely ignoring it. The
 * game is handed no text at all, so the slot it would have used is left empty and nothing is
 * drawn. For the action bar the showing time is still restarted even though there is nothing to
 * show, so the slot stays reserved for as long as it would have been.<br>
 * A listener can also rewrite {@link #message} to change what the line says, which is the only way
 * to keep it on screen but with different words. Read the text back with
 * {@link TextHelper#getString() getString()}, and build a replacement with
 * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FChat#createTextHelperFromString(String) Chat.createTextHelperFromString(String)}
 * or {@@code Chat.createTextHelperFromTranslationKey(...)}.
 * Remember that assigning {@code null} to the message hides the line in exactly the same way that
 * cancelling does.
 * example:
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
     * which of the three HUD lines this is: {@code 'TITLE'} for the big line that fades in and out
     * over the screen, {@code 'SUBTITLE'} for the smaller line under it, and {@code 'ACTIONBAR'} for
     * the line just above the hotbar. The action bar is also what client side system messages are
     * drawn on, so those arrive here as {@code 'ACTIONBAR'}.<br>
     * Script type definitions narrow this to a {@code TitleType} union, so comparing it against one
     * of those three strings is always allowed.
     */
    @DocletReplaceReturn("TitleType")
    @DocletDeclareType(name = "TitleType", type = "'TITLE' | 'SUBTITLE' | 'ACTIONBAR'")
    public final String type;
    /**
     * the text the line is about to show, wrapped in a {@link TextHelper}, which keeps the colours
     * and the click and hover events the server sent with it. Read the plain text from
     * {@link TextHelper#getString() getString()}.<br>
     * This field is writable, and assigning to it replaces the text the line shows. Assigning
     * {@code null} hides the line, which is what cancelling the event does: the client then hands
     * the game no text rather than an empty one, so the slot is left blank for the whole duration
     * of the line instead of being cleared straight away.
     */
    @Nullable
    public TextHelper message;

    public EventTitle(String type, Component message) {
        super(JsMacrosClient.clientCore);
        this.type = type;
        this.message = TextHelper.wrap(message);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"type\": \"%s\", \"message\": \"%s\"}", this.getEventName(), type, message);
    }

}
