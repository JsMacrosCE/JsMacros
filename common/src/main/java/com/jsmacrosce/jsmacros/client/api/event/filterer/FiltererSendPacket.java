package com.jsmacrosce.jsmacros.client.api.event.filterer;

import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.event.impl.EventSendPacket;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.EventFilterer;

/**
 * Filters callbacks for the {@code SendPacket} event. Create it with
 * {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#createEventFilterer(java.lang.String) createEventFilterer()}
 * and pass it to the filterer-taking {@code JsMacros.on} overload. It matches packet names
 * exactly; {@code null} accepts every sent packet. Filtering only controls whether the listener
 * runs. Packet changes or cancellation must be done by the listener. This filterer is valid only
 * for {@code SendPacket}.
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
     * Exact packet event type to accept, or {@code null} to accept every packet. Use a registered
     * shortcut from {@link com.jsmacrosce.jsmacros.client.api.helper.PacketByteBufferHelper#getPacketNames() getPacketNames()}
     * when available; otherwise the type is the packet's simple class name. Shortcuts can differ
     * from class names and vary between game versions.
     */
    @Nullable
    @DocletReplaceReturn("PacketName | null")
    public String type;

    /**
     * Whether this filterer supports the named event; the filterer-taking {@code JsMacros.on}
     * overload rejects unsupported event names.
     *
     * @param event the name of the event being listened to
     * @return {@code true} only for {@code "SendPacket"}
     */
    @Override
    public boolean canFilter(String event) {
        return "SendPacket".equals(event);
    }

    /**
     * Tests whether the event is a sent-packet event matching {@link #type}, or accepts every sent
     * packet when {@code type} is {@code null}. Other event types are rejected. This only controls
     * listener invocation and does not modify or cancel the packet.
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
     * Sets the exact packet name to accept; {@code null} disables type filtering. Returns this
     * filterer for chaining.
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
