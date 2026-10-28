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
 * Fires while handling the server's player-combat-kill packet, before the client shows the death
 * screen. Call {@link #respawn()} on a later tick rather than during
 * packet handling; this requests respawn but does not guarantee that the death screen never opens.
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
     * The player's block position when the death packet is handled.
     */
    public final BlockPosHelper deathPos;
    /**
     * A snapshot of the player inventory's slots in inventory order, including armor and offhand.
     * Empty slots are represented by empty entries; check {@code isEmpty()} before reading one.
     */
    public final List<ItemStackHelper> inventory;

    /**
     * Captures the current local player's block position and inventory entries. Construction
     * requires a non-null local player; otherwise reading its block position throws a
     * {@link NullPointerException}. The list and wrappers are created at construction time,
     * without copying the underlying item stacks.
     */
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
     * Requests respawn if the local player is not alive. Use from a later tick rather than
     * during death-packet handling. The server can ignore the request while its player is
     * still alive; sending it does not confirm respawn or guarantee the death screen never opens.
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

    /**
     * Formats the event name and stored death position as {@code %s:{"deathPos": %s}}; the
     * inventory is not included.
     * @return the diagnostic event-name and death-position string
     */
    @Override
    public String toString() {
        return String.format("%s:{\"deathPos\": %s}", this.getEventName(), deathPos);
    }

}
