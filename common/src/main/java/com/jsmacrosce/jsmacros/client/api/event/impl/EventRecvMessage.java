package com.jsmacrosce.jsmacros.client.api.event.impl;

import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "RecvMessage", oldName = "RECV_MESSAGE", cancellable = true)
public class EventRecvMessage extends BaseEvent {
    @Nullable
    public TextHelper text;

    /**
     * @since 1.8.2
     */
    @Nullable
    public byte[] signature;

    /**
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
