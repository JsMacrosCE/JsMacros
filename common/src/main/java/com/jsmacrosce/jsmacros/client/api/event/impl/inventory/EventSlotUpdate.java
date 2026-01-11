package com.jsmacrosce.jsmacros.client.api.event.impl.inventory;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @since 1.9.0
 */
@Event(value = "SlotUpdate")
public class EventSlotUpdate extends BaseEvent {
    protected final AbstractContainerScreen<?> screen;
    @DocletReplaceReturn("SlotUpdateType")
    @DocletDeclareType(name = "SlotUpdateType", type = "'HELD' | 'INVENTORY' | 'SCREEN'")
    public final String type;
    public final int slot;
    public final ItemStackHelper oldStack;
    public final ItemStackHelper newStack;

    public EventSlotUpdate(AbstractContainerScreen<?> screen, String type, int slot, ItemStack oldStack, ItemStack newStack) {
        super(JsMacrosClient.clientCore);
        this.screen = screen;
        this.type = type;
        this.slot = slot;
        this.oldStack = new ItemStackHelper(oldStack);
        this.newStack = new ItemStackHelper(newStack);
    }

    /**
     * @return inventory associated with the event
     */
    public Inventory<?> getInventory() {
        return Inventory.create(screen);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"slot\": %d, \"screen\": \"%s\"}", this.getEventName(), slot, JsMacrosClient.getScreenName(screen));
    }

}
