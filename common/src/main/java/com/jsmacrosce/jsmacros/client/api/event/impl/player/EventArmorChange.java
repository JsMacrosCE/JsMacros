package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.world.item.ItemStack;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "ArmorChange", oldName = "ARMOR_CHANGE")
public class EventArmorChange extends BaseEvent {
    @DocletReplaceReturn("ArmorSlot")
    @DocletDeclareType(name = "ArmorSlot", type = "'HEAD' | 'CHEST' | 'LEGS' | 'FEET'")
    public final String slot;
    public final ItemStackHelper item;
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
