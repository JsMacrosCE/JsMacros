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
 * Fires for an incoming packet before it is handled by the game. The event is cancellable; assigning
 * {@code null} to {@link #packet} also drops it. Bundled packets are reported as a single packet,
 * not as separate events for their contents. Use a filterer to restrict which packets invoke a
 * listener. Packet-buffer edits affect handling only after the decoded packet is assigned to
 * {@link #packet}; check {@link #canGetPacketBuffer()} because not every packet has a standalone
 * codec.
 * <p>
 * <pre>
 * const filterer = JsMacros.createEventFilterer("RecvPacket").setType("HealthUpdateS2CPacket");
 * const listener = JsMacros.on("RecvPacket", filterer, JavaWrapper.methodToJava(event => {
 *   if (!event.canGetPacketBuffer()) return;
 *   const buffer = event.getPacketBuffer();
 *   // This packet's first field is the health value.
 *   Chat.log(`the server says my health is ${buffer.readFloat()}`);
 *   // Reading advances the buffer; reset before reading or decoding it again.
 *   buffer.reset();
 * }));
 * // Later, remove the listener:
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
     * The incoming packet object. Replacing it changes the packet handled by the game; assigning
     * {@code null} drops it. Buffer edits do not affect this field until the decoded packet is
     * assigned here.
     */
    @Nullable
    public Packet<?> packet;
    /**
     * The packet's convenience name, matching names in
     * {@link PacketByteBufferHelper#getPacketNames()} and accepted by {@link FiltererRecvPacket}.
     * Unlisted packet classes fall back to their simple class name; names can vary by game version.
     */
    @DocletReplaceReturn("PacketName")
    public final String type;

    /**
     * Initializes the event with the client core and captures the supplied packet and its type.
     * The packet is stored by reference; it is not cloned, and this constructor does not dispatch,
     * handle, or send it. The captured type uses a registered shortcut name when available, otherwise
     * the packet's simple class name, and remains unchanged if {@link #packet} is later replaced.
     *
     * @param packet the non-null incoming or outgoing packet to store and name
     */
    public EventRecvPacket(@NotNull Packet<?> packet) {
        super(JsMacrosClient.clientCore);
        this.packet = packet;
        this.type = PacketByteBufferHelper.getPacketName(packet);
    }

    /**
     * Checks whether the current packet has a registered phase and standalone codec; this does not
     * validate packet contents. Bundles are unsupported.
     *
     * @return whether a standalone packet buffer is supported
     * @since 2.0.0
     */
    public boolean canGetPacketBuffer() {
        return PacketByteBufferHelper.canSerialize(packet);
    }

    /**
     * Returns a helper containing the packet encoded in its wire format. Reads and writes advance
     * its independent buffer; edits affect handling only after decoding and assigning the result to
     * {@link #packet}. Call {@link PacketByteBufferHelper#reset()} to restore its original contents
     * and reader position. Check {@link #canGetPacketBuffer()} first; bundles and other unsupported
     * packets have no standalone codec.
     * <p>
     * <pre class="language-typescript">
     * const filterer = (JsMacros.createEventFilterer("RecvPacket") as FiltererRecvPacket)
     *   .setType("HealthUpdateS2CPacket");
     * JsMacros.on("RecvPacket", filterer, JavaWrapper.methodToJava(function (event: EventRecvPacket) {
     *   if (!event.canGetPacketBuffer()) return;
     *   // HealthUpdateS2CPacket writes a float health, then the food level and saturation
     *   const buffer = event.getPacketBuffer();
     *   const health = buffer.readFloat();
     *   Chat.log(`health is ${health}`);
     *   buffer.reset();
     * }));
     * </pre>
     *
     * @return a helper for reading or editing the packet's encoded data
     * @since 1.8.4
     */
    public PacketByteBufferHelper getPacketBuffer() {
        return new PacketByteBufferHelper(packet);
    }

    /**
     * Returns a diagnostic string containing the event name and the type captured at construction.
     * Its {@code eventName:{"type": "type"}} shape is not JSON, and the captured type is not escaped.
     *
     * @return a diagnostic string with the event name and captured packet type
     */
    @Override
    public String toString() {
        return String.format("%s:{\"type\": \"%s\"}", this.getEventName(), type);
    }

}
