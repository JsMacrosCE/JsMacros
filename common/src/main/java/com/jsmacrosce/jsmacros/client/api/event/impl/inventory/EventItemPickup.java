package com.jsmacrosce.jsmacros.client.api.event.impl.inventory;

import net.minecraft.world.item.ItemStack;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires while handling an item-pickup packet for the local player, before the corresponding client
 * inventory update. To watch individual slot changes, listen for
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventSlotUpdate}.
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
     * A snapshot of the picked-up item stack, with its count set to the packet's pickup amount.
     * Mutating it does not modify the dropped item or inventory.
     */
    public final ItemStackHelper item;

    /**
     * Wraps the supplied raw stack as the event's item; the wrapper retains the same reference
     * without copying it. A {@code null} stack is retained as the wrapper's raw value.
     * @param item raw stack to wrap; may be {@code null}
     */
    public EventItemPickup(ItemStack item) {
        super(JsMacrosClient.clientCore);
        this.item = new ItemStackHelper(item);
    }

    /**
     * Returns a diagnostic string containing the event name and the stored item's string form.
     * Calling this method throws {@link NullPointerException} if the wrapped raw stack is
     * {@code null}.
     * @return the string in the form {@code %s:{"item": %s}}, with the event name and item string
     */
    @Override
    public String toString() {
        return String.format("%s:{\"item\": %s}", this.getEventName(), item.toString());
    }

}
