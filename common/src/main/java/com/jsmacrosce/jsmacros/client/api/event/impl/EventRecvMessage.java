package com.jsmacrosce.jsmacros.client.api.event.impl;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

//? if >=26.1 {
/*import net.minecraft.client.multiplayer.chat.GuiMessageTag;
*///? } else {
import net.minecraft.client.GuiMessageTag;
//? }

/**
 * Fired before a message is added to the chat HUD. Cancelling skips the HUD's entire
 * {@code addMessage} call, including its chat-log output and queue updates.
 * <pre>
 * JsMacros.on("RecvMessage", JavaWrapper.methodToJava(function (event) {
 *   if (event.messageType === "Chat Error") {
 *     // Hide the chat-validation error message.
 *     event.cancel();
 *     return;
 *   }
 *   Chat.log(`[${event.messageType}] ${event.text.getString()}`);
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Network/Chat")
@Event(value = "RecvMessage", oldName = "RECV_MESSAGE", cancellable = true)
public class EventRecvMessage extends BaseEvent {
    /**
     * Message content to be added to the HUD. Replacing it changes the displayed and logged
     * content and marks the line with a {@code "Modified"} {@link #messageType}; assigning
     * {@code null} instead causes the HUD message to be skipped.
     */
    @NotNull
    public TextHelper text;

    /**
     * Raw message-signature bytes, or {@code null} when no signature was supplied. Present
     * signatures contain 256 bytes.
     * @since 1.8.2
     */
    @Nullable
    public byte[] signature;

    /**
     * Minecraft chat-log tag for the message, or {@code null} when no tag was supplied. JsMacros
     * sets this to {@code "Modified"} when a listener changes {@link #text}.
     * @since 1.8.2
     */
    @Nullable
    public String messageType;

    /**
     * Initializes the event with the supplied message, signature bytes, and chat-log tag; this
     * constructor does not add or dispatch a HUD message. The message is wrapped with
     * {@link TextHelper#wrap(Component)}, which returns {@code null} for a null component despite
     * the non-null annotation on {@link #text}. The signature byte array is retained, not copied.
     * @param message raw message content, or {@code null}
     * @param signature raw signature, or {@code null}; its byte array is retained
     * @param indicator HUD tag, or {@code null}; its log tag is stored as {@link #messageType}
     */
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

    /**
     * Formats a diagnostic string from the event name and the current values of {@link #text},
     * {@link #signature}, and {@link #messageType}. Its shape is
     * {@code eventName:{"text": "textValue", "signature": boolean, "messageType": "tagValue"}};
     * the text and tag are inserted using ordinary string formatting, not JSON escaping. The
     * signature value is {@code true} only when the current signature array is non-null and
     * non-empty; it does not verify the signature.
     * @return diagnostic string containing the current field values
     */
    public String toString() {
        return String.format("%s:{\"text\": \"%s\", \"signature\": %s, \"messageType\": \"%s\"}", this.getEventName(), text, signature != null && signature.length > 0, messageType);
    }
}
