package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import net.minecraft.client.player.LocalPlayer;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.ClientPlayerEntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the client has just joined a server, right after the login packet was handled and
 * the local player exists.<br>
 * The world is not fully loaded at this point, most chunks and entities are still on their way,
 * so use {@link com.jsmacrosce.jsmacros.client.api.event.impl.world.EventChunkLoad ChunkLoad} to
 * react to the world filling in.<br>
 * This event is not cancellable.
 * example:
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
     * the local player that was created for this connection. Being a
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.ClientPlayerEntityHelper}, it
     * also exposes the movement and interaction helpers, such as
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.ClientPlayerEntityHelper#lookAt(String) lookAt(String)}
     * and
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.entity.ClientPlayerEntityHelper#attack(EntityHelper) attack(EntityHelper)}.
     */
    public final ClientPlayerEntityHelper<LocalPlayer> player;
    /**
     * the address of the server the client connected to, the remote address the connection was
     * opened with.
     */
    public final String address;

    public EventJoinServer(LocalPlayer player, String address) {
        super(JsMacrosClient.clientCore);
        this.player = new ClientPlayerEntityHelper<>(player);
        this.address = address;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"address\": \"%s\"}", this.getEventName(), address);
    }

}
