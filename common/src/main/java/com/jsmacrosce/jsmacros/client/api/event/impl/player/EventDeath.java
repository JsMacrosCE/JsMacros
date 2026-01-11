package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "Death", oldName = "DEATH")
public class EventDeath extends BaseEvent {

    public final BlockPosHelper deathPos;
    public final List<ItemStackHelper> inventory;

    public EventDeath() {
        super(JsMacrosClient.clientCore);
        this.deathPos = new BlockPosHelper(Minecraft.getInstance().player.blockPosition());
        Inventory inv = Minecraft.getInstance().player.getInventory();
        inventory = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            this.inventory.add(new ItemStackHelper(inv.getItem(i)));
        }
    }

    /**
     * Respawns the player. Should be used with some delay, one tick should be enough.
     *
     * @since 1.8.4
     */
    public void respawn() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (!player.isAlive()) {
            player.respawn();
        }
    }

    @Override
    public String toString() {
        return String.format("%s:{\"deathPos\": %s}", this.getEventName(), deathPos);
    }

}
