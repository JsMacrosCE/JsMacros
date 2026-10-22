package com.jsmacrosce.jsmacros.client.api.event.filterer;

import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.event.impl.EventSendPacket;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.EventFilterer;

/**
 * A filter for the {@code SendPacket} event, which fires for every packet the client sends to the
 * server, so it is the busiest event there is. A filterer is built with
 * {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#createEventFilterer(java.lang.String) createEventFilterer()}
 * and handed to the {@code JsMacros.on} overload that takes a filterer, which then only calls back
 * for the packets this filterer accepts.<br>
 * The single {@link #type} is the only thing that can be filtered on, and it has to match the
 * packet's name exactly. It starts out {@code null}, which means no filtering, so an untouched
 * filterer accepts every sent packet.<br>
 * A filterer only decides whether the listener runs, it never touches the packet. To actually
 * change or block a packet, cancel the event inside the listener instead. This filterer can only
 * be used with {@code SendPacket}, and handing it to {@code JsMacros.on} for any other event
 * throws an error.
 * example:
 * <pre>
 * // only look at chat packets
 * const filterer = JsMacros.createEventFilterer("SendPacket")
 *   .setType("ChatCommandSignedC2SPacket");
 * const listener = JsMacros.on("SendPacket", filterer, JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`sending ${event.type}`);
 * }));
 * // later
 * JsMacros.off(listener);
 * </pre>
 * @author aMelonRind
 * @since 1.9.1
 */
@DocletCategory("Event Filterers")
@SuppressWarnings("unused")
public class FiltererSendPacket implements EventFilterer {
    /**
     * the name of the packet to accept, such as {@code "ChatCommandSignedC2SPacket"} for a chat
     * command, or {@code null} to accept every packet. The names are the ones
     * {@link com.jsmacrosce.jsmacros.client.api.helper.PacketByteBufferHelper#getPacketNames() getPacketNames()}
     * lists, not the game's class names, and they change between game versions, so look one up
     * rather than hardcoding it.<br>
     * The name has to match exactly, so a name the game does not use lets nothing through rather
     * than everything.<br>
     * This field is writable, {@link FiltererSendPacket#setType(String) setType()} only does this
     * assignment.
     */
    @Nullable
    @DocletReplaceReturn("PacketName | null")
    public String type;

    /**
     * whether this filterer can be used for the given event name. It only answers {@code true}
     * for {@code "SendPacket"}, which is the event it is registered for, and the
     * {@code JsMacros.on} overload that takes a filterer refuses a filterer that cannot filter
     * the event it was given.
     *
     * @param event the name of the event being listened to
     * @return {@code true} only for {@code "SendPacket"}
     */
    @Override
    public boolean canFilter(String event) {
        return "SendPacket".equals(event);
    }

    /**
     * the predicate this filterer evaluates. It is called for every packet the client sends, so it
     * is on the hot path. Anything that is not a sent-packet event is rejected outright, and for a
     * sent-packet event it passes every packet while {@link #type} is {@code null}, otherwise only
     * the packets whose name is exactly that. It never changes or blocks the packet.
     *
     * @param event the event being filtered, which is an
     * {@link com.jsmacrosce.jsmacros.client.api.event.impl.EventSendPacket} here
     * @return {@code true} if this filterer accepts the event
     */
    @Override
    public boolean test(BaseEvent event) {
        return (event instanceof EventSendPacket e) && (type == null || e.type.equals(type));
    }

    /**
     * sets the name of the packet to accept, which turns filtering off entirely when passed
     * {@code null}. The name has to match the packet's name exactly. Returns the same filterer so
     * calls can be chained.
     *
     * @param type the packet name to match, or {@code null} to accept every packet
     * @return this filterer, for chaining
     */
    @DocletReplaceParams("type: PacketName | null")
    public FiltererSendPacket setType(@Nullable String type) {
        this.type = type;
        return this;
    }

}
