package com.jsmacrosce.jsmacros.client.api.event.impl;

import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.event.filterer.FiltererSendPacket;
import com.jsmacrosce.jsmacros.client.api.helper.PacketByteBufferHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;
import com.jsmacrosce.jsmacros.core.library.impl.FReflection;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Event(value = "SendPacket", cancellable = true, filterer = FiltererSendPacket.class)
@SuppressWarnings("unused")
public class EventSendPacket extends BaseEvent {

    @Nullable
    public Packet<?> packet;
    @DocletReplaceReturn("PacketName")
    public final String type;

    public EventSendPacket(@NotNull Packet<?> packet) {
        super(JsMacrosClient.clientCore);
        this.packet = packet;
        this.type = PacketByteBufferHelper.getPacketName(packet);
    }

    /**
     * Replaces the packet of this event with a new one of the same type, created from the given
     * arguments. It's recommended to use {@link #getPacketBuffer()} to modify the packet instead.
     *
     * @param args the arguments to pass to the packet's constructor
     * @throws NullPointerException if this.packet is null
     * @since 1.8.4
     */
    public void replacePacket(Object... args) {
        //noinspection DataFlowIssue
        packet = FReflection.newInstance0(packet.getClass(), args);
    }

    /**
     * After modifying the buffer, use {@link PacketByteBufferHelper#toPacket()} to get the modified
     * packet and replace this packet with the modified one.
     *
     * @return a helper for accessing and modifying the packet's data.
     * @since 1.8.4
     */
    public PacketByteBufferHelper getPacketBuffer() {
        return new PacketByteBufferHelper(packet);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"type\": \"%s\"}", this.getEventName(), type);
    }

}
