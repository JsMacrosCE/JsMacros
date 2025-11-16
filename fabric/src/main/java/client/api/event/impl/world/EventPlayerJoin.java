package xyz.wagyourtail.jsmacros.client.api.event.impl.world;

import net.minecraft.client.multiplayer.PlayerInfo;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.client.api.helper.world.PlayerListEntryHelper;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.event.Event;

import java.util.UUID;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "PlayerJoin", oldName = "PLAYER_JOIN")
public class EventPlayerJoin extends BaseEvent {
    public final String UUID;
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
