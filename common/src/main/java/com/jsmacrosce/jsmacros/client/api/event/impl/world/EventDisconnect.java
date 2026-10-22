package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import net.minecraft.network.chat.Component;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the client leaves a world and returns to the title screen.<br>
 * This covers both being kicked or disconnected by the server and quitting to the title screen
 * on purpose, so {@link #message} can be {@code null} when there is no disconnection reason to
 * report.<br>
 * This event is not cancellable, the client is already leaving the world when it fires.
 * example:
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
     * the reason the client was disconnected, as the disconnection screen would show it.<br>
     * Note: this is {@code null} when the client left the world without going through a
     * disconnection screen, for example by quitting to the title screen, so always check it
     * before reading from it.
     * @since 1.6.4
     */
    public final TextHelper message;

    public EventDisconnect(Component message) {
        super(JsMacrosClient.clientCore);
        this.message = TextHelper.wrap(message);
    }

    @Override
    public String toString() {
        return String.format("%s:{}", this.getEventName());
    }

}
