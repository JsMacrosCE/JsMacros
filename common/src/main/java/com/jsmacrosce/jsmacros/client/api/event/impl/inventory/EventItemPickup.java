package com.jsmacrosce.jsmacros.client.api.event.impl.inventory;

import net.minecraft.world.item.ItemStack;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the local player picks up an item that was lying on the ground, which is also when the
 * game plays the pickup sound. The client does not put the item into the inventory itself, the
 * server does that and sends the slot updates separately, so this event fires before any slot on
 * the client has changed.<br>
 * It only fires for the local player. Another player picking up an item nearby does not raise it,
 * and neither does the client picking up an experience orb, since the game only sends this packet
 * with an item entity in it.<br>
 * This event is not cancellable, the pickup has already been decided. To watch the individual
 * slots change, listen for
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventSlotUpdate} instead.
 * example:
 * <pre>
 * JsMacros.on("ItemPickup", JavaWrapper.methodToJava(function (event) {
 *   const item = event.item;
 *   if (item.getItemID() === "minecraft:diamond") {
 *     if (item.getCount() >= 5) {
 *       Chat.log(`picked up ${item.getCount()} diamonds`);
 *     }
 *   }
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Inventory")
@Event(value = "ItemPickup", oldName = "ITEM_PICKUP")
public class EventItemPickup extends BaseEvent {
    /**
     * the stack that was picked up, with its count set to how many were taken.<br>
     * This is a copy of the dropped item entity's stack, so it is a snapshot rather than the live
     * item. Writing to it changes the copy and nothing else, so a listener can freely overwrite
     * the damage with {@link ItemStackHelper#setDamage(int) setDamage()} without the game ever
     * seeing it. The id is available from {@link ItemStackHelper#getItemID() getItemID()}, the
     * name from {@link ItemStackHelper#getName() getName()} and the data from
     * {@link ItemStackHelper#getNBT() getNBT()}.
     */
    public final ItemStackHelper item;

    public EventItemPickup(ItemStack item) {
        super(JsMacrosClient.clientCore);
        this.item = new ItemStackHelper(item);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"item\": %s}", this.getEventName(), item.toString());
    }

}
