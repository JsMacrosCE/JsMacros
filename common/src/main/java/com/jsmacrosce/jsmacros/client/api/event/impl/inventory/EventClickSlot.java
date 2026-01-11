package com.jsmacrosce.jsmacros.client.api.event.impl.inventory;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * event triggered when the user "clicks" a slot in an inventory
 *
 * @author Wagyourtail
 * @since 1.6.4
 */
@Event(value = "ClickSlot", cancellable = true)
public class EventClickSlot extends BaseEvent {
    protected final AbstractContainerScreen<?> screen;
    /**
     * <a href="https://wiki.vg/Protocol#Click_Window" target="_blank">https://wiki.vg/Protocol#Click_Window</a>
     */
    public final int mode;
    @DocletReplaceReturn("ClickSlotButton")
    public final int button;
    public final int slot;

    public EventClickSlot(AbstractContainerScreen<?> screen, int mode, int button, int slot) {
        super(JsMacrosClient.clientCore);
        this.screen = screen;
        this.mode = mode;
        this.button = button;
        this.slot = slot;
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
