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
 * A joined listener can cancel sending or replace the packet with {@code setPacket(...)};
 * assigning {@code null} drops it. A joined listener runs on the network thread, so keep it short;
 * pass {@code false} to observe asynchronously without changing or cancelling the packet. A buffer
 * from {@code getPacketBuffer()} is independent, so apply edits by decoding it and assigning the
 * result to {@code setPacket(...)}. Use a filterer to limit which packets invoke a listener.
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
     * @deprecated Removal in 3.0.0: Use {@link #getPacket()} and {@link #setPacket(Packet)} instead.
     */
    @Nullable
    @Deprecated
    public Packet<?> packet;

    /**
     * Returns the outgoing packet object, or {@code null} when sending is suppressed.
     *
     * @return the current packet, or {@code null}
     * @since 2.2.0
     */
    @Nullable
    public Packet<?> getPacket() {
        return packet;
    }

    /**
     * Replaces the outgoing packet. {@code null} drops it. Buffer edits do not change the packet
     * unless the decoded result is assigned here.
     *
     * @param packet replacement packet, or {@code null} to drop it
     * @throws IllegalStateException if this delivery is not joined or its event lock was released
     * @since 2.2.0
     */
    public void setPacket(@Nullable Packet<?> packet) {
        assertCanModify("setPacket");
        this.packet = packet;
    }
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
     * value does not change if {@code setPacket(...)} is later called.
     *
     * @param packet the non-null outgoing packet to store
     */
    public EventSendPacket(@NotNull Packet<?> packet) {
        super(JsMacrosClient.clientCore);
        this.packet = packet;
        this.type = PacketByteBufferHelper.getPacketName(packet);
    }

    /**
     * Constructs a packet of the current packet's class and assigns it with
     * {@code setPacket(...)}. It first
     * tries an exact runtime-argument-type match, then constructors with the same argument count,
     * coercing numeric arguments to parameter types.
     *
     * @param args the arguments to pass to the packet's constructor
     * @throws NullPointerException if the current packet or any argument is null
     * @throws RuntimeException if no matching constructor can be invoked
     * @since 1.8.4
     */
    public void replacePacket(Object... args) {
        assertCanModify("replacePacket");
        //noinspection DataFlowIssue
        setPacket(FReflection.newInstance0(getPacket().getClass(), args));
    }

    /**
     * Checks whether the current packet has a selectable registered phase and standalone codec.
     * Returns {@code false} when the current packet is {@code null}. This does not guarantee that
     * encoding its contents will succeed; bundles are unsupported.
     *
     * @return whether a standalone packet buffer is supported
     * @since 2.1.0
     */
    public boolean canGetPacketBuffer() {
        Packet<?> packet = getPacket();
        return packet != null && PacketByteBufferHelper.canSerialize(packet);
    }

    /**
     * Encodes the current packet into an independent buffer helper. Read and write operations change
     * only that buffer; to change what is sent, decode the edits and assign the packet with
     * {@code setPacket(...)}. Unsupported packets, including bundles, cause an
     * {@link IllegalArgumentException}; {@link #canGetPacketBuffer()} checks codec availability but
     * does not guarantee encoding succeeds.
     *
     * @return a helper for reading or editing the packet's encoded data.
     * @throws IllegalStateException if the current packet is {@code null}
     * @since 1.8.4
     */
    public PacketByteBufferHelper getPacketBuffer() {
        Packet<?> packet = getPacket();
        if (packet == null) {
            throw new IllegalStateException("The event has no packet; a listener dropped it or none was supplied.");
        }
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
