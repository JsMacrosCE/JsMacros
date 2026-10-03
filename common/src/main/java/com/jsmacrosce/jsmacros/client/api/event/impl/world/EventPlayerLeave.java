package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.client.multiplayer.PlayerInfo;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.PlayerListEntryHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

import java.util.UUID;

/**
 * Fired when the {@code ClientboundPlayerInfoRemovePacket} packet is received and a known player
 * uuid is removed from the playerInfoMap map.
 * <br>
 * Note: This event may not be fired on all servers.
 * <br>
 * The event is raised once per entry the packet removes, as each one is taken back out of the
 * player list, so one packet carrying several uuids raises it several times. It is not
 * cancellable, and there is nothing to stop: the entry leaves the player list either way.
 * example:
 * <pre>
 * JsMacros.on("PlayerLeave", JavaWrapper.methodToJava(function (event) {
 *   const name = event.player.getName();
 *   Chat.log(`${name === null ? event.UUID : name} left the player list`);
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Network/Chat")
@Event(value = "PlayerLeave", oldName = "PLAYER_LEAVE")
public class EventPlayerLeave extends BaseEvent {
    /**
     * The UUID of the player that was removed.
     * <br>
     * For example, {@code "069a79f4-44e9-4726-a5be-fca90e38aaf5"} is the value for Notch.
     * <br>
     * This is the uuid the packet addressed, so it is also the key the entry was stored under,
     * and it is what {@link #player} would report through
     * {@link PlayerListEntryHelper#getUUID() getUUID()}.
     */
    public final String UUID;
    /**
     * A helper for the removed player list entry.
     * <br>
     * It wraps the entry that is going away rather than a live tab list entry, so read what you
     * need while the event is running. {@link PlayerListEntryHelper#getName() getName()} can be
     * {@code null} for a profile the server has not given a name for.
     */
    public final PlayerListEntryHelper player;

    public EventPlayerLeave(UUID uuid, PlayerInfo player) {
        super(JsMacrosClient.clientCore);
        this.UUID = uuid.toString();
        this.player = new PlayerListEntryHelper(player);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"player\": %s}", this.getEventName(), player.toString());
    }

}
