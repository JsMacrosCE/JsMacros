package com.jsmacrosce.jsmacros.client.util;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventClickSlot;
import com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventDropSlot;

/**
 * Dispatches slot-click and applicable drop events for container-screen mixins.
 * The click is triggered first; if it is cancelled, no drop event is triggered. Otherwise,
 * a drop event is triggered for a throw action or an outside-window slot click.
 */
public final class SlotClickEvents {

    private SlotClickEvents() {
    }

    /**
     * Fires the click event and, unless it is cancelled, a drop event for a throw or
     * outside-window click.
     *
     * @param screen screen receiving the click
     * @param actionId numeric action identifier supplied by the version-specific caller
     * @param isThrow whether the action is a throw
     * @param button button value supplied by the screen handler
     * @param slotId clicked menu slot, or the outside-window sentinel
     *
     * @return {@code true} when the click event is cancelled, or when the applicable drop event
     *         is cancelled; {@code false} otherwise
     */
    public static boolean fire(AbstractContainerScreen<?> screen, int actionId, boolean isThrow, int button, int slotId) {
        EventClickSlot event = new EventClickSlot(screen, actionId, button, slotId);
        event.trigger();
        if (event.isCanceled()) {
            return true;
        }

        if (isThrow || slotId == AbstractContainerMenu.SLOT_CLICKED_OUTSIDE) {
            EventDropSlot eventDrop = new EventDropSlot(screen, slotId, button == 1);
            eventDrop.trigger();
            return eventDrop.isCanceled();
        }

        return false;
    }
}
