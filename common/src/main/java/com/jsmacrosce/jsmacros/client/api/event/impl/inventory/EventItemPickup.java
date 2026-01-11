package com.jsmacrosce.jsmacros.client.api.event.impl.inventory;

import net.minecraft.world.item.ItemStack;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "ItemPickup", oldName = "ITEM_PICKUP")
public class EventItemPickup extends BaseEvent {
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
