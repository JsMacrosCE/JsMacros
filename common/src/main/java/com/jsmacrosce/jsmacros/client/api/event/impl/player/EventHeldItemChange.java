package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.world.item.ItemStack;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the item stack in the local player's main hand or off hand changes.<br>
 * Despite the name this is about the item in the hand, not about the selected hotbar slot.
 * Scrolling to a different hotbar slot that holds the same item does not fire this event, and
 * replacing the item in the currently held slot does.<br>
 * This is not driven by a packet. Both hands are polled once per client tick and compared against
 * the stack that was seen the last time, so the event reports the change the client noticed. A
 * hand that is empty both before and after produces no event at all.<br>
 * When the new and old stacks are the same item with the same count and the same components
 * apart from damage, and only the damage value moved, the
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventItemDamage} event fires
 * first with that item, and this event follows immediately after. A hand that only gained or
 * lost stack count does not go through that path, so no {@code ItemDamage} is raised for it and
 * this event is the only one that fires.<br>
 * This event is not cancellable, the hand contents the client tracks are the hand contents the
 * player actually has.
 * example:
 * <pre>
 * JsMacros.on("HeldItemChange", JavaWrapper.methodToJava(function (event) {
 *   const hand = event.offHand ? "off hand" : "main hand";
 *   const now = event.item.isEmpty() ? "empty" : `${event.item.getItemId()} x${event.item.getCount()}`;
 *   const before = event.oldItem.isEmpty() ? "empty" : `${event.oldItem.getItemId()} x${event.oldItem.getCount()}`;
 *   Chat.log(`${hand}: ${before} to ${now}`);
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Inventory")
@Event(value = "HeldItemChange", oldName = "HELD_ITEM")
public class EventHeldItemChange extends BaseEvent {
    /**
     * {@code true} if the off hand changed, {@code false} if the main hand changed.
     */
    public final boolean offHand;
    /**
     * the item stack that is in the hand now. An empty hand is reported as an empty
     * {@link com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper}, so check
     * {@link com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper#isEmpty() isEmpty()}
     * before reading the id or the count.
     */
    public final ItemStackHelper item;
    /**
     * the stack that was in the hand the last time this event fired for it, which is an empty
     * {@link com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper} if the hand was
     * empty.
     */
    public final ItemStackHelper oldItem;

    public EventHeldItemChange(ItemStack item, ItemStack oldItem, boolean offHand) {
        super(JsMacrosClient.clientCore);
        this.item = new ItemStackHelper(item);
        this.oldItem = new ItemStackHelper(oldItem);
        this.offHand = offHand;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"item\": %s}", this.getEventName(), item.toString());
    }

}
