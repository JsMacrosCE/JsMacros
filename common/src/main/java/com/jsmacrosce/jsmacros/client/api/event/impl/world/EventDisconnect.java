package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import net.minecraft.network.chat.Component;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the client disconnects, including when leaving without a disconnection screen. The
 * disconnection-screen reason is available as {@link #message} when present; this event is not
 * cancellable.
 * <pre>
 * JsMacros.on("Disconnect", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`Left the world${event.message === null ? "" : ": " + event.message.getString()}`);
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Network/Chat")
@Event(value = "Disconnect", oldName = "DISCONNECT")
public class EventDisconnect extends BaseEvent {
    /**
     * The disconnection-screen reason, or {@code null} when disconnecting without a disconnection
     * screen.
     * @since 1.6.4
     */
    public final TextHelper message;

    /**
     * Wraps the disconnect reason when present and initializes the event with the client core.
     * This does not itself perform a disconnect, kick, world leave, GUI change, or event dispatch.
     *
     * @param message raw reason component to wrap; {@code null} produces a {@code null} message field
     */
    public EventDisconnect(Component message) {
        super(JsMacrosClient.clientCore);
        this.message = TextHelper.wrap(message);
    }

    /**
     * Returns this event's name followed by the fixed empty-brace payload marker; it does not
     * include or inspect the disconnect reason, so it remains safe when {@code message} is null.
     *
     * @return the event name followed by {@code :{}}; this diagnostic string is not the full reason
     * or serialized component JSON
     */
    @Override
    public String toString() {
        return String.format("%s:{}", this.getEventName());
    }

}
