package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import net.minecraft.client.multiplayer.PlayerInfo;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.PlayerListEntryHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

import java.util.UUID;

/**
 * Fired when the {@code ClientboundPlayerInfoUpdatePacket} packet is received and a new player is
 * added to the {@code PlayerSocialManager}.
 * <br>
 * Note: This event may not be fired on all servers.
 * <br>
 * The event is raised once per entry the packet adds, as each one is handed to the player list,
 * so one packet carrying several new players raises it several times. It is not cancellable, and
 * there is nothing to stop: the entry goes into the player list either way.
 * example:
 * <pre>
 * JsMacros.on("PlayerJoin", JavaWrapper.methodToJava(function (event) {
 *   const name = event.player.getName();
 *   Chat.log(`${name === null ? event.UUID : name} joined, ping ${event.player.getPing()} ms`);
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "PlayerJoin", oldName = "PLAYER_JOIN")
public class EventPlayerJoin extends BaseEvent {
    /**
     * The UUID of the player that was added.
     * <br>
     * For example, {@code "069a79f4-44e9-4726-a5be-fca90e38aaf5"} is the value for Notch.
     * <br>
     * This is the profile id of the entry, the same value
     * {@link PlayerListEntryHelper#getUUID() getUUID()} reports on {@link #player}.
     */
    public final String UUID;
    /**
     * A helper for the added player list entry.
     * <br>
     * It is the entry as it was just received, so the name, ping, gamemode and display name all
     * read from it come from the same packet. {@link PlayerListEntryHelper#getName() getName()}
     * can be {@code null} for a profile the server has not given a name for.
     */
    public final PlayerListEntryHelper player;

    public EventPlayerJoin(UUID uuid, PlayerInfo player) {
        super(JsMacrosClient.clientCore);
        this.UUID = uuid.toString();
        this.player = new PlayerListEntryHelper(player);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"player\": %s}", this.getEventName(), player.toString());
    }

}
