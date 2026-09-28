package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Fires when the local player dies, that is when the server tells the client the player was
 * killed in combat.<br>
 * This is not cancellable, the death has already been applied when the event fires. The death
 * screen appears a moment later, so call
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.player.EventDeath#respawn() respawn()} to
 * skip it, but only from a later tick.<br>
 * In a singleplayer world the client also runs the integrated server, and the event is not raised
 * for that server's own copy of the player, so it still fires exactly once.
 * example:
 * <pre>
 * JsMacros.on("Death", JavaWrapper.methodToJava(function (event) {
 *   const pos = event.deathPos;
 *   Chat.log(`Died at ${pos.getX()}, ${pos.getY()}, ${pos.getZ()}`);
 *   let filled = 0;
 *   for (const stack of event.inventory) {
 *     if (!stack.isEmpty()) {
 *       filled += 1;
 *     }
 *   }
 *   Chat.log(`Inventory had ${filled} non empty stacks when the event fired`);
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Player/Stats")
@Event(value = "Death", oldName = "DEATH")
public class EventDeath extends BaseEvent {

    /**
     * the block the player was standing in when the death packet arrived, which is where the
     * player was killed. Its coordinates are available from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper#getX() getX()},
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper#getY() getY()} and
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper#getZ() getZ()}.
     */
    public final BlockPosHelper deathPos;
    /**
     * a snapshot of the player's whole inventory at the moment of death, one entry per inventory
     * slot in the inventory's own order, so the list index is the slot index. This covers every
     * slot the player inventory has, not just the 36 main ones, so the armor and offhand are
     * included too. Empty slots are present as empty
     * {@link com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper} entries, so
     * check {@link com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper#isEmpty() isEmpty()}
     * before reading them.
     */
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
     * Respawns the player. Should be used with some delay, one tick should be enough.<br>
     * It does nothing at all if the player is already alive, so calling it more than once is
     * harmless. It puts the player straight back into the world, skipping the death screen,
     * which means the player is back before the server has necessarily finished applying the
     * death.
     * example:
     * <pre>
     * JsMacros.on("Death", JavaWrapper.methodToJava(function (event) {
     *   // the "Death" listener stays registered, so every death is caught
     *   // "once" is used for the tick, so nothing is left behind once it has fired
     *   JsMacros.once("Tick", JavaWrapper.methodToJava(function () {
     *     event.respawn();
     *   }))
     * }))
     * </pre>
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
