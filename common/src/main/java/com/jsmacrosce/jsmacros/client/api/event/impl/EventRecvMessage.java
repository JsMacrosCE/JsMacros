package com.jsmacrosce.jsmacros.client.api.event.impl;

import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fired before a chat message is added to the HUD.
 * <br>
 * This event is cancellable. Cancelling it prevents the message from being shown in the HUD and
 * from being logged to the console.
 * <br>
 * This event is fired for player chat in addition to other chat-like messages received by the
 * client.
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Network/Chat")
@Event(value = "RecvMessage", oldName = "RECV_MESSAGE", cancellable = true)
public class EventRecvMessage extends BaseEvent {
    /**
     * The message content that is about to be added to the HUD.
     */
    @NotNull
    public TextHelper text;

    /**
     * The cryptographic signature of the message, if present.
     * <br>
     * This is {@code null} for unsigned messages and system messages. For signed messages, this
     * contains a 256 byte array containing the raw signature bytes.
     * @since 1.8.2
     */
    @Nullable
    public byte[] signature;

    /**
     * A textual tag describing the message type shown or logged by Minecraft (known as the
     * logTag).
     * <br>
     * This may be {@code null} when no message tag is present.
     * <br>
     * As of 1.21.11, the known values for this include {@code "Modified"}, {@code "System"},
     * {@code "Not Secure"} and {@code "Chat Error"}
     * @since 1.8.2
     */
    @Nullable
    public String messageType;

    public EventRecvMessage(Component message, @Nullable MessageSignature signature, @Nullable GuiMessageTag indicator) {
        super(JsMacrosClient.clientCore);
        this.text = TextHelper.wrap(message);

        if (signature != null) {
            this.signature = signature.bytes();
        }

        if (indicator != null) {
            this.messageType = indicator.logTag();
        }
    }

    public String toString() {
        return String.format("%s:{\"text\": \"%s\", \"signature\": %s, \"messageType\": \"%s\"}", this.getEventName(), text, signature != null && signature.length > 0, messageType);
    }
}
