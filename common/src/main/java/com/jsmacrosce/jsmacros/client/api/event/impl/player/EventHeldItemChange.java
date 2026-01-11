package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.world.item.ItemStack;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "HeldItemChange", oldName = "HELD_ITEM")
public class EventHeldItemChange extends BaseEvent {
    public final boolean offHand;
    public final ItemStackHelper item;
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
