package com.jsmacrosce.jsmacros.client.api.event.impl;

import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.event.filterer.FiltererRecvPacket;
import com.jsmacrosce.jsmacros.client.api.helper.PacketByteBufferHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires for every packet the client receives from the server, before the game looks at it. That is
 * every packet on the hot path of the connection, not only the gameplay ones, so a listener with no
 * filter on it is one of the easiest ways to make the client stutter. Build a filterer with
 * {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#createEventFilterer(java.lang.String) createEventFilterer()}
 * and hand it to the {@code JsMacros.on} overload that takes one, so the callback only runs for the
 * packets you asked for.<br>
 * Only whole packets are reported. A packet the server bundled together with others arrives as one
 * bundle packet, and the packets inside it never get an event of their own, so a filterer on an
 * individual packet name can end up matching nothing.<br>
 * This event is cancellable, and cancelling drops the packet before the game sees it, so the game
 * also never sees whatever was put in {@link #packet} instead. Assigning {@code null} to the packet
 * has the same effect, so a listener can drop a packet either way.<br>
 * Changing the packet is not really possible here yet, unlike on the sending side. Whatever the
 * game is finally handed is whatever is in {@link #packet} once the event has been dispatched, so a
 * replacement does have to be assigned back to the field to have any effect, but there is no
 * working way to build one. {@link #getPacketBuffer()} writes the packet out so its fields can be
 * read, yet writing into that buffer does not reach the game and turning it back into a packet
 * through {@link PacketByteBufferHelper#toPacket() toPacket()} throws a
 * {@link java.lang.NullPointerException NullPointerException} in this build. A packet with no stream
 * codec of its own, a bundle being the usual case, cannot be written out at all either.<br>
 * See {@link com.jsmacrosce.jsmacros.client.api.event.impl.EventSendPacket} for the outgoing
 * direction, which has one extra way of building a replacement.
 * example:
 * <pre>
 * const filterer = JsMacros.createEventFilterer("RecvPacket")
 *   .setType("HealthUpdateS2CPacket");
 * const listener = JsMacros.on("RecvPacket", filterer, JavaWrapper.methodToJava(function (event) {
 *   // the buffer holds the packet's fields in the order the game writes them
 *   const buffer = event.getPacketBuffer();
 *   const health = buffer.readFloat();
 *   Chat.log(`the server says my health is ${health}`);
 *   // reading moves the buffer along, so put it back before reading again
 *   buffer.reset();
 * }));
 * // later
 * JsMacros.off(listener);
 * </pre>
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Network/Chat")
@Event(value = "RecvPacket", cancellable = true, filterer = FiltererRecvPacket.class)
@SuppressWarnings("unused")
public class EventRecvPacket extends BaseEvent {
    /**
     * the packet the client received, which is the game's own object rather than a copy.<br>
     * This field is writable, and assigning to it replaces the packet the game is about to be
     * handed. Assigning {@code null} drops the packet, which is the same as cancelling the event,
     * and is why the declared type here is nullable and has to be checked before it is read.<br>
     * Writing into the buffer from {@link #getPacketBuffer()} on its own changes nothing, since the
     * game is handed this field and not the buffer, and a packet with no stream codec of its own
     * cannot be written out and read back. So the only thing a listener can usefully do with this
     * field right now is drop the packet.
     */
    @Nullable
    public Packet<?> packet;
    /**
     * the name of the packet, as a short readable string such as {@code "HealthUpdateS2CPacket"} or
     * {@code "ChatMessageS2CPacket"}. It is the same name
     * {@link PacketByteBufferHelper#getPacketNames() getPacketNames()} lists and the same one a
     * {@link FiltererRecvPacket} matches on, and it ends in {@code S2CPacket} or {@code C2SPacket}
     * for the direction the packet travels in.<br>
     * It is a hand maintained list rather than a mapped name, it is missing entries for some
     * packets, and a packet the list does not cover falls back to the game's simple class name.
     * Look a name up with {@link PacketByteBufferHelper#getPacketNames() getPacketNames()} rather
     * than hardcoding it, and expect it to change between game versions.
     */
    @DocletReplaceReturn("PacketName")
    public final String type;

    public EventRecvPacket(@NotNull Packet<?> packet) {
        super(JsMacrosClient.clientCore);
        this.packet = packet;
        this.type = PacketByteBufferHelper.getPacketName(packet);
    }

    /**
     * gives you a buffer holding the packet's data written out in the game's own wire format, so
     * its fields can be read and written one at a time. Read them back in the order the packet
     * writes them, and call {@link PacketByteBufferHelper#reset() reset()} to get the untouched
     * bytes again, since every read moves the buffer along.<br>
     * Reading is all this is good for as the code stands. Writing into the buffer does not reach
     * the game, because the game is handed {@link #packet} and not the buffer, and turning the
     * buffer back into a packet through
     * {@link PacketByteBufferHelper#toPacket() toPacket()} throws a
     * {@link java.lang.NullPointerException NullPointerException} in this build, since the lookup
     * table that method reads was never filled in. Treat the buffer as read only for now.<br>
     * A packet with no stream codec of its own, a bundle being the usual case, cannot be written out
     * at all, so the buffer comes back empty and reading from it runs off the end rather than
     * giving the packet's fields.
     * example:
     * <pre>
     * const filterer = JsMacros.createEventFilterer("RecvPacket")
     *   .setType("HealthUpdateS2CPacket");
     * JsMacros.on("RecvPacket", filterer, JavaWrapper.methodToJava(function (event) {
     *   // HealthUpdateS2CPacket writes a float health, then the food level and saturation
     *   const buffer = event.getPacketBuffer();
     *   const health = buffer.readFloat();
     *   Chat.log(`health is ${health}`);
     *   buffer.reset();
     * }));
     * </pre>
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
