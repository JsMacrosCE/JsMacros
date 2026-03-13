package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import net.minecraft.client.multiplayer.PlayerInfo;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.PlayerListEntryHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired when the {@code ClientboundPlayerInfoRemovePacket} packet is received and a known player
 * uuid is removed from the playerInfoMap map.
 * <br>
 * Note: This event may not be fired on all servers.
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
     */
    @NotNull
    public final String UUID;

    /**
     * A helper for the removed player list entry.
     */
    @NotNull
    public final PlayerListEntryHelper player;

    public EventPlayerLeave(@NotNull UUID uuid, @NotNull PlayerInfo player) {
        super(JsMacrosClient.clientCore);
        this.UUID = uuid.toString();
        this.player = new PlayerListEntryHelper(player);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"player\": %s}", this.getEventName(), player);
    }

}
