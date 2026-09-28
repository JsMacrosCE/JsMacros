package com.jsmacrosce.jsmacros.client.api.event.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
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
 * Fired before a chat message is added to the HUD.
 * <br>
 * This event is cancellable. Cancelling it prevents the message from being shown in the HUD and
 * from being logged to the console.
 * <br>
 * This event is fired for player chat in addition to other chat-like messages received by the
 * client.
 * <br>
 * Cancelling stops the whole chat HUD {@code addMessage} call rather than just its visible part,
 * so anything else that call would have done is skipped too.
 * example:
 * <pre>
 * JsMacros.on("RecvMessage", JavaWrapper.methodToJava(function (event) {
 *   if (event.messageType === "Chat Error") {
 *     // the red "Chat validation error" line the client shows when it rejects a chat message
 *     event.cancel();
 *     return;
 *   }
 *   if (event.text === null) {
 *     return;
 *   }
 *   Chat.log(`[${event.messageType}] ${event.text.getString()}`);
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "RecvMessage", oldName = "RECV_MESSAGE", cancellable = true)
public class EventRecvMessage extends BaseEvent {
    /**
     * The message content that is about to be added to the HUD.
     * <br>
     * This field is writable. A listener that replaces it changes what the client shows, and the
     * client then marks the line as modified, which shows up as the {@code "Modified"}
     * {@link #messageType} together with an extra line quoting what was originally sent.
     * <br>
     * Note: it is declared nullable here, so script type definitions allow it to be {@code null}
     * and it has to be checked before it is read. The constructor always fills it in, so it is
     * only {@code null} if a listener assigned {@code null} to it.
     */
    @Nullable
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
     * <br>
     * {@code "Modified"} is the one JsMacros puts there itself: it is the tag a line gets when a
     * listener replaced {@link #text}.
     * @since 1.8.2
     */
    @Nullable
    public String messageType;

    public EventRecvMessage(Component message, MessageSignature signature, GuiMessageTag indicator) {
        super(JsMacrosClient.clientCore);
        this.text = TextHelper.wrap(message);

        if (signature == null) {
            this.signature = null;
        } else {
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
