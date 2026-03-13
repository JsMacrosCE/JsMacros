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
 * Fired when the {@code ClientboundPlayerInfoUpdatePacket} packet is received and a new player is
 * added to the {@code PlayerSocialManager}.
 * <br>
 * Note: This event may not be fired on all servers.
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Network/Chat")
@Event(value = "PlayerJoin", oldName = "PLAYER_JOIN")
public class EventPlayerJoin extends BaseEvent {
    /**
     * The UUID of the player that was added.
     * <br>
     * For example, {@code "069a79f4-44e9-4726-a5be-fca90e38aaf5"} is the value for Notch.
     */
    public final String UUID;

    /**
     * A helper for the added player list entry.
     */
    @NotNull
    public final PlayerListEntryHelper player;

    public EventPlayerJoin(UUID uuid, @NotNull PlayerInfo playerInfo) {
        super(JsMacrosClient.clientCore);
        this.UUID = uuid.toString();
        this.player = new PlayerListEntryHelper(playerInfo);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"player\": %s}", this.getEventName(), player);
    }

}
