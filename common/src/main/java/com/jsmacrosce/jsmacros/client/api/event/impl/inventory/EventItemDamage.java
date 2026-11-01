package com.jsmacrosce.jsmacros.client.api.event.impl.inventory;

import net.minecraft.world.item.ItemStack;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when polling detects a main-hand, off-hand, or armor stack change attributable to its damage
 * value. Increased and decreased values both trigger it. When the stack also differs in a way other
 * than damage, only the corresponding {@link
 * com.jsmacrosce.jsmacros.client.api.event.impl.player.EventHeldItemChange} or {@link
 * com.jsmacrosce.jsmacros.client.api.event.impl.player.EventArmorChange} fires; a damage-only change
 * triggers this event first and then the corresponding stack-change event.
 * {@link #damage} is the resulting value, not the amount changed;
 * {@link #item} wraps the live stack and can be used to modify it.
 *
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
     * The changed stack in its current state. This wraps the live stack; modifications affect the
     * player's item.
     */
    public final ItemStackHelper item;
    /**
     * The stack's current damage value, not the change since the previous tick or remaining
     * durability.
     */
    public final int damage;

    /**
     * Wraps the supplied stack and stores the supplied resulting damage value; it does not apply
     * that value to the stack or validate it against the stack's damage or durability. A null raw
     * stack is retained by the helper and causes a {@link NullPointerException} when its accessors
     * are used.
     *
     * @param stack raw stack to retain; may be {@code null}
     * @param damage resulting damage value to store, not required to match the stack
     */
    public EventItemDamage(ItemStack stack, int damage) {
        super(JsMacrosClient.clientCore);
        this.item = new ItemStackHelper(stack);
        this.damage = damage;
    }

    /**
     * Returns a diagnostic string containing the event name and the current helper string for the
     * stored item. The format is {@code %s:{"item": %s}}; the {@code damage} field is omitted,
     * though the helper's string may itself include the raw stack's current damage.
     *
     * @return diagnostic event-name and item string
     */
    @Override
    public String toString() {
        return String.format("%s:{\"item\": %s}", this.getEventName(), item.toString());
    }

}
