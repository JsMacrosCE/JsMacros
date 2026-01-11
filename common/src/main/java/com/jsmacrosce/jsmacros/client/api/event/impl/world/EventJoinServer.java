package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import net.minecraft.client.player.LocalPlayer;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.ClientPlayerEntityHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "JoinServer", oldName = "JOIN_SERVER")
public class EventJoinServer extends BaseEvent {
    public final ClientPlayerEntityHelper<LocalPlayer> player;
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
