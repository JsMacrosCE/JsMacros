package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.world.item.ItemStack;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the item in one of the local player's four armor slots changes.<br>
 * This is not driven by a packet. The player's equipment is polled once per client tick and
 * compared against the stack that was seen the last time, so the event reports the change the
 * client noticed. A slot that is empty both before and after produces no event at all.<br>
 * When the new and old stacks are the same item with the same count and the same components
 * apart from damage, and only the damage value moved, the
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventItemDamage} event fires
 * first with that item, and this event follows immediately after. A slot that only gained or
 * lost stack count does not go through that path, so no {@code ItemDamage} is raised for it and
 * this event is the only one that fires.<br>
 * This event is not cancellable, the armor the client tracks is the armor the player actually
 * has.
 * example:
 * <pre>
 * JsMacros.on("ArmorChange", JavaWrapper.methodToJava(function (event) {
 *   const now = event.item.isEmpty() ? "empty" : `${event.item.getItemId()} x${event.item.getCount()}`;
 *   const before = event.oldItem.isEmpty() ? "empty" : `${event.oldItem.getItemId()} x${event.oldItem.getCount()}`;
 *   Chat.log(`Armor ${event.slot}: ${before} to ${now}`);
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Player/Stats")
@Event(value = "ArmorChange", oldName = "ARMOR_CHANGE")
public class EventArmorChange extends BaseEvent {
    /**
     * which of the four armor slots changed. Script type definitions narrow this to an
     * {@code ArmorSlot} union.
     */
    @DocletReplaceReturn("ArmorSlot")
    @DocletDeclareType(name = "ArmorSlot", type = "'HEAD' | 'CHEST' | 'LEGS' | 'FEET'")
    public final String slot;
    /**
     * the item that is in the slot now. An empty slot is reported as an empty
     * {@link com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper}, so check
     * {@link com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper#isEmpty() isEmpty()}
     * before reading the id or the count.
     */
    public final ItemStackHelper item;
    /**
     * the item that was in the slot the last time this event fired for it, which is an empty
     * {@link com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper} if the slot was
     * empty.
     */
    public final ItemStackHelper oldItem;

    public EventArmorChange(String slot, ItemStack item, ItemStack old) {
        super(JsMacrosClient.clientCore);
        this.slot = slot;
        this.item = new ItemStackHelper(item);
        this.oldItem = new ItemStackHelper(old);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"slot\": %s}", this.getEventName(), slot);
    }

}
