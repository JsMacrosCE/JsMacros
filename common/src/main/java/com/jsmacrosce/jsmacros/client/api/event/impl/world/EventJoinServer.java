package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import net.minecraft.client.player.LocalPlayer;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.ClientPlayerEntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires after the login packet is handled and the local player exists; the world may still be
 * loading. This event is not cancellable.
 * <pre>
 * JsMacros.on("JoinServer", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`Joined ${event.address} as ${event.player.getName().getString()}`);
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Network/Chat")
@Event(value = "JoinServer", oldName = "JOIN_SERVER")
public class EventJoinServer extends BaseEvent {
    /**
     * Local player for this connection.
     */
    public final ClientPlayerEntityHelper<LocalPlayer> player;
    /**
     * Remote address used for the connection.
     */
    public final String address;

    /**
     * Initializes this event with the supplied player and connection address.
     * The player is stored in a {@link ClientPlayerEntityHelper} wrapper, including when
     * {@code player} is {@code null}; the address is stored unchanged. This constructor
     * does not perform connection setup or dispatch the event.
     *
     * @param player the local player to wrap; {@code null} is retained by the wrapper
     * @param address the supplied connection address, stored unchanged and possibly {@code null}
     */
    public EventJoinServer(LocalPlayer player, String address) {
        super(JsMacrosClient.clientCore);
        this.player = new ClientPlayerEntityHelper<>(player);
        this.address = address;
    }

    /**
     * Returns a diagnostic string containing the event name and address.
     * The address is interpolated as a quoted string without escaping; a {@code null}
     * address therefore appears as the text {@code "null"}. This is not a JSON serializer.
     *
     * @return the event name followed by {@code :{"address": "..."}}, with the supplied
     * address inserted between the quotes
     */
    @Override
    public String toString() {
        return String.format("%s:{\"address\": \"%s\"}", this.getEventName(), address);
    }

}
