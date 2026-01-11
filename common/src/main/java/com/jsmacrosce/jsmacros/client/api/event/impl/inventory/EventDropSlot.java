package com.jsmacrosce.jsmacros.client.api.event.impl.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * event triggered when an item is dropped
 *
 * @author Wagyourtail
 * @since 1.6.4
 */
@Event(value = "DropSlot", cancellable = true)
public class EventDropSlot extends BaseEvent {
    protected static final Minecraft mc = Minecraft.getInstance();

    protected final AbstractContainerScreen<?> screen;
    public final int slot;
    /**
     * whether it's all or a single item being dropped
     */
    public final boolean all;

    public EventDropSlot(AbstractContainerScreen<?> screen, int slot, boolean all) {
        super(JsMacrosClient.clientCore);
        this.screen = screen;
        this.slot = slot;
        this.all = all;
    }

    /**
     * @return inventory associated with the event
     */
    public Inventory<?> getInventory() {
        if (screen == null) {
            assert mc.player != null;
            return Inventory.create(new InventoryScreen(mc.player));
        }
        return Inventory.create(screen);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"slot\": %d, \"screen\": \"%s\"}", this.getEventName(), slot, JsMacrosClient.getScreenName(screen));
    }

}
