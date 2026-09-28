package com.jsmacrosce.jsmacros.client.api.event.impl.inventory;

import net.minecraft.world.item.ItemStack;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when one of the player's worn or held items loses durability, so a tool, a piece of
 * armour or a shield taking damage.<br>
 * It is checked once per client tick for the main hand, the off hand and each of the four armour
 * pieces, but it is only raised when the stack is still the same item, with the same count and the
 * same other data, and the only thing that changed is its damage value. That means it covers wear
 * and repair alike: a mending or an anvil repair that lowers the damage value again raises this
 * event too, with the smaller value. Switching to a different item does not raise it, it reports
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.player.EventHeldItemChange} or
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.player.EventArmorChange} on its own, while
 * a pure damage change reports both this event and that one.<br>
 * This event is not cancellable. By the time the tick sees the change the new damage is already on
 * the stack, so there is nothing to stop. What can still be done is correcting the stack itself,
 * see {@link #item}.
 * <br>
 * Because the check is per tick, a large durability loss that the server applies in one step is
 * reported once, with the damage it ended up at rather than the amount it went down by.
 * example:
 * <pre>
 * JsMacros.on("ItemDamage", JavaWrapper.methodToJava(function (event) {
 *   const item = event.item;
 *   if (!item.isDamageable()) {
 *     return;
 *   }
 *   const left = item.getMaxDurability() - event.damage;
 *   if (left === 1) {
 *     Chat.log(`${item.getName().getString()} is one hit from breaking`);
 *   }
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Inventory")
@Event(value = "ItemDamage", oldName = "ITEM_DAMAGE")
public class EventItemDamage extends BaseEvent {
    /**
     * the item that took the damage, in its state after the damage was applied.<br>
     * This wraps the stack the player is actually holding, not a copy of it, so writing to it
     * changes the real item. That is the only way to act on this event, since the event itself
     * cannot be cancelled: for example
     * {@link ItemStackHelper#setDamage(int) setDamage()} rewrites the durability of the live
     * stack. Read the id from {@link ItemStackHelper#getItemID() getItemID()}, the name from
     * {@link ItemStackHelper#getName() getName()} and the durability from
     * {@link ItemStackHelper#getMaxDurability() getMaxDurability()}.
     */
    public final ItemStackHelper item;
    /**
     * how much damage the item has taken in total, which is the same value as
     * {@link ItemStackHelper#getDamage() item.getDamage()}. It is the damage the stack has ended
     * up at, not the amount it went up by since the last tick, and it is not the remaining
     * durability, which is {@link ItemStackHelper#getMaxDurability() getMaxDurability()} minus
     * this value.
     */
    public final int damage;

    public EventItemDamage(ItemStack stack, int damage) {
        super(JsMacrosClient.clientCore);
        this.item = new ItemStackHelper(stack);
        this.damage = damage;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"item\": %s}", this.getEventName(), item.toString());
    }

}
