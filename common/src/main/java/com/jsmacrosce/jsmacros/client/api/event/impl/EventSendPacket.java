package com.jsmacrosce.jsmacros.client.api.event.impl;

import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.event.filterer.FiltererSendPacket;
import com.jsmacrosce.jsmacros.client.api.helper.PacketByteBufferHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;
import com.jsmacrosce.jsmacros.core.library.impl.FReflection;

/**
 * Fires synchronously at the client's packet-send path, including for non-gameplay packets.
 * Listeners can cancel sending or replace {@link #packet}; assigning {@code null} drops the packet.
 * A buffer from {@link #getPacketBuffer()} is independent, so apply edits by decoding it and
 * assigning the result to {@code packet}. Use a filterer to limit which packets invoke a listener.
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Network/Chat")
@Event(value = "SendPacket", cancellable = true, filterer = FiltererSendPacket.class)
@SuppressWarnings("unused")
public class EventSendPacket extends BaseEvent {

    /**
     * The outgoing packet object. Assigning a replacement changes what is sent; assigning
     * {@code null} drops it. Buffer edits do not alter this field unless the decoded packet is
     * assigned here.
     */
    @Nullable
    public Packet<?> packet;
    /**
     * The convenience name used by {@link PacketByteBufferHelper#getPacketNames()} and
     * {@link FiltererSendPacket}. Unlisted packet classes use their simple class name; names may
     * vary by game version.
     */
    @DocletReplaceReturn("PacketName")
    public final String type;

    /**
     * Initializes the event with the client core and the supplied outgoing packet, retaining the
     * packet reference without sending or dispatching it. The packet must be non-null. {@link #type}
     * captures its shortcut name, or its simple class name when no shortcut is registered; this
     * value does not change if {@link #packet} is later replaced.
     *
     * @param packet the non-null outgoing packet to store
     */
    public EventSendPacket(@NotNull Packet<?> packet) {
        super(JsMacrosClient.clientCore);
        this.packet = packet;
        this.type = PacketByteBufferHelper.getPacketName(packet);
    }

    /**
     * Constructs a packet of the current packet's class and assigns it to {@link #packet}. It first
     * tries an exact runtime-argument-type match, then constructors with the same argument count,
     * coercing numeric arguments to parameter types.
     *
     * @param args the arguments to pass to the packet's constructor
     * @throws NullPointerException if this.packet or any argument is null
     * @throws RuntimeException if no matching constructor can be invoked
     * @since 1.8.4
     */
    public void replacePacket(Object... args) {
        //noinspection DataFlowIssue
        packet = FReflection.newInstance0(packet.getClass(), args);
    }

    /**
     * Checks whether the current packet has a selectable registered phase and standalone codec.
     * This does not guarantee that encoding its contents will succeed; bundles are unsupported.
     *
     * @return whether a standalone packet buffer is supported
     * @since 2.0.0
     */
    public boolean canGetPacketBuffer() {
        return PacketByteBufferHelper.canSerialize(packet);
    }

    /**
     * Encodes the current packet into an independent buffer helper. Read and write operations change
     * only that buffer; to change what is sent, decode the edits and assign the packet to
     * {@link #packet}. Unsupported packets, including bundles, cause an
     * {@link IllegalArgumentException}; {@link #canGetPacketBuffer()} checks codec availability but
     * does not guarantee encoding succeeds.
     *
     * @return a helper for reading or editing the packet's encoded data.
     * @since 1.8.4
     */
    public PacketByteBufferHelper getPacketBuffer() {
        return new PacketByteBufferHelper(packet);
    }

    /**
     * Returns a diagnostic string containing the event name and the type captured at construction.
     * The format is {@code %s:{"type": "%s"}}; it is not JSON, and the type is not escaped.
     *
     * @return a diagnostic string with the event name and captured packet type
     */
    @Override
    public String toString() {
        return String.format("%s:{\"type\": \"%s\"}", this.getEventName(), type);
    }

}
