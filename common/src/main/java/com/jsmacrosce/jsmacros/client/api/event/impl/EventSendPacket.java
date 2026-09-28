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
 * Fires for every packet the client is about to send to the server, before the game sends it. That
 * is every packet on the hot path of the connection, not only the gameplay ones, so a listener
 * with no filter on it is one of the easiest ways to make the client stutter. Build a filterer
 * with {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#createEventFilterer(java.lang.String) createEventFilterer()}
 * and hand it to the {@code JsMacros.on} overload that takes one, so the callback only runs for the
 * packets you asked for.<br>
 * This event is cancellable, and cancelling drops the packet before it goes out, so the server
 * never sees it and never sees whatever was put in {@link #packet} instead. Assigning {@code null}
 * to the packet has the same effect, so a listener can drop a packet either way.<br>
 * There are two ways to change what is sent, and only one of them works. {@link #replacePacket(Object...)}
 * builds a fresh packet of the same class from the arguments given, going through the packet's own
 * constructor with numbers coerced to the parameter types, and that is the way to use.
 * {@link #getPacketBuffer()} writes the packet out so its fields can be read, but writing into
 * that buffer does not reach the game and turning it back into a packet through
 * {@link PacketByteBufferHelper#toPacket() toPacket()} throws a
 * {@link java.lang.NullPointerException NullPointerException} in this build, so treat the buffer
 * as read only.<br>
 * The packets the client sends by itself are here too, such as the ones keeping the connection
 * alive, so filtering is worth doing even for a listener that only cares about the occasional chat
 * or movement packet.
 * example:
 * <pre>
 * // refuse to let any chat text out, and say so on stdout rather than in chat,
 * // since chat is exactly what is being blocked
 * const filterer = JsMacros.createEventFilterer("SendPacket")
 *   .setType("ChatMessageC2SPacket");
 * const listener = JsMacros.on("SendPacket", filterer, JavaWrapper.methodToJava(function (event) {
 *   const buffer = event.getPacketBuffer();
 *   print(`blocked a chat message: ${buffer.readString()}`);
 *   event.cancel();
 * }));
 * // later
 * JsMacros.off(listener);
 * </pre>
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Network/Chat")
@Event(value = "SendPacket", cancellable = true, filterer = FiltererSendPacket.class)
@SuppressWarnings("unused")
public class EventSendPacket extends BaseEvent {

    /**
     * the packet the client is about to send, which is the game's own object rather than a copy.<br>
     * This field is writable, and assigning to it replaces the packet that goes out. Assigning
     * {@code null} drops the packet, which is the same as cancelling the event, and is why the
     * declared type here is nullable and has to be checked before it is read.<br>
     * For a replacement built by hand, {@link #replacePacket(Object...)} is the way that works
     * today. Writing into the buffer from {@link #getPacketBuffer()} does not reach the game, since
     * the game is handed this field and not the buffer.
     */
    @Nullable
    public Packet<?> packet;
    /**
     * the name of the packet, as a short readable string such as
     * {@code "ChatMessageC2SPacket"} or {@code "ChatCommandSignedC2SPacket"}. It is the same name
     * {@link PacketByteBufferHelper#getPacketNames() getPacketNames()} lists and the same one a
     * {@link FiltererSendPacket} matches on, and it ends in {@code S2CPacket} or {@code C2SPacket}
     * for the direction the packet travels in. A packet class that is one of several variants of
     * the same packet is listed separately for each, with a {@code $} and the variant's name
     * after it.<br>
     * It is a hand maintained list rather than a mapped name, it is missing entries for some
     * packets, and a packet the list does not cover falls back to the game's simple class name.
     * Look a name up with {@link PacketByteBufferHelper#getPacketNames() getPacketNames()} rather
     * than hardcoding it, and expect it to change between game versions.
     */
    @DocletReplaceReturn("PacketName")
    public final String type;

    public EventSendPacket(@NotNull Packet<?> packet) {
        super(JsMacrosClient.clientCore);
        this.packet = packet;
        this.type = PacketByteBufferHelper.getPacketName(packet);
    }

    /**
     * builds a new packet of the same class as the one this event carries, from the arguments
     * given, and puts it in its place. The arguments are matched against the packet class's
     * constructors by how many there are, and plain numbers are coerced to whatever the parameter
     * type is, so a whole number can stand in for a float and the other way round. The first
     * constructor whose parameters fit is the one used, and there is no way to ask which that will
     * be, so check the game's own packet class first.<br>
     * A class with no constructor taking that many arguments, or whose parameters are not the right
     * types once the numbers are coerced, has nothing that fits and this throws. The new packet has
     * no type of its own, so {@link #type} keeps reporting the original name, which is the same
     * class, and a filterer keeps matching.<br>
     * Note that {@link #getPacketBuffer()} is not an alternative to this, since the buffer cannot
     * be turned back into a packet in this build.
     * example:
     * <pre>
     * // the full movement packet takes the position, the rotation and two flags,
     * // and rebuilding it pins the player to the middle of the world
     * const filterer = JsMacros.createEventFilterer("SendPacket")
     *   .setType("PlayerMoveC2SPacket$Full");
     * JsMacros.on("SendPacket", filterer, JavaWrapper.methodToJava(function (event) {
     *   event.replacePacket(0, -64, 0, 0, 0, true, false);
     * }));
     * </pre>
     *
     * @param args the arguments to pass to the packet's constructor
     * @throws NullPointerException if this.packet is null
     * @throws RuntimeException if no constructor of the packet class takes that many
     *         arguments, or the arguments cannot be coerced to the parameter types
     * @since 1.8.4
     */
    public void replacePacket(Object... args) {
        //noinspection DataFlowIssue
        packet = FReflection.newInstance0(packet.getClass(), args);
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
     * table that method reads was never filled in. Use {@link #replacePacket(Object...)} to change
     * what is sent instead.<br>
     * A packet with no stream codec of its own cannot be written out at all, so the buffer comes
     * back empty and reading from it runs off the end rather than giving the packet's fields.
     * example:
     * <pre>
     * const filterer = JsMacros.createEventFilterer("SendPacket")
     *   .setType("ChatMessageC2SPacket");
     * JsMacros.on("SendPacket", filterer, JavaWrapper.methodToJava(function (event) {
     *   // the first field of this packet is the text being sent
     *   const buffer = event.getPacketBuffer();
     *   print(`about to send ${buffer.readString()}`);
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
