package com.jsmacrosce.jsmacros.client.api.event.filterer;

import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.event.impl.EventRecvPacket;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.EventFilterer;

/**
 * Filters callbacks for the {@code RecvPacket} event. Create it with
 * {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#createEventFilterer(java.lang.String) createEventFilterer()}
 * and pass it to the filterer-taking {@code JsMacros.on} overload. It matches packet names
 * exactly; {@code null} accepts every received packet. Filtering only controls whether the
 * listener runs. Packet changes or cancellation must be done by the listener. This filterer is
 * valid only for {@code RecvPacket}.
 * <pre>
 * // only look at the chunk data packet
 * const filterer = JsMacros.createEventFilterer("RecvPacket")
 *   .setType("ChunkDataS2CPacket");
 * const listener = JsMacros.on("RecvPacket", filterer, JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`packet ${event.type} arrived`);
 * }));
 * // later
 * JsMacros.off(listener);
 * </pre>
 * @author aMelonRind
 * @since 1.9.1
 */
@DocletCategory("Event Filterers")
@SuppressWarnings("unused")
public class FiltererRecvPacket implements EventFilterer {
    /**
     * Packet name to accept, or {@code null} to accept every packet. Names are those
     * {@link com.jsmacrosce.jsmacros.client.api.helper.PacketByteBufferHelper#getPacketNames() getPacketNames()}
     * lists, not necessarily the game's class names, and may change between game versions. Matching
     * is exact. Names outside that list can still match a packet's simple class name when the
     * helper falls back to it.
     */
    @Nullable
    @DocletReplaceReturn("PacketName | null")
    public String type;

    /**
     * Whether this filterer supports the named event; the filterer-taking {@code JsMacros.on}
     * overload rejects unsupported event names.
     *
     * @param event the name of the event being listened to
     * @return {@code true} only for {@code "RecvPacket"}
     */
    @Override
    public boolean canFilter(String event) {
        return "RecvPacket".equals(event);
    }

    /**
     * Tests whether the event is a received-packet event matching {@link #type}, or accepts every
     * received packet when {@code type} is {@code null}. Other event types are rejected. This only
     * controls listener invocation and does not modify or cancel the packet.
     *
     * @param event the event being filtered, which is an
     * {@link com.jsmacrosce.jsmacros.client.api.event.impl.EventRecvPacket} here
     * @return {@code true} if this filterer accepts the event
     */
    @Override
    public boolean test(BaseEvent event) {
        return (event instanceof EventRecvPacket e) && (type == null || e.type.equals(type));
    }

    /**
     * Sets the exact packet name to accept; {@code null} disables type filtering. Returns this
     * filterer for chaining.
     *
     * @param type the packet name to match, or {@code null} to accept every packet
     * @return this filterer, for chaining
     */
    @DocletReplaceParams("type: PacketName | null")
    public FiltererRecvPacket setType(@Nullable String type) {
        this.type = type;
        return this;
    }

}
