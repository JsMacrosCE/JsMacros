package com.jsmacrosce.jsmacros.client.util;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventClickSlot;
import com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventDropSlot;

/**
 * Shared slot-click event dispatch for the container screen mixins.
 * <p>
 * {@code MixinHandledScreen} and {@code MixinCreativeInventoryScreen} fire the same pair
 * of events. The injection signatures still differ per Minecraft version, but everything
 * after that is version-independent and lives here so the two mixins cannot drift apart.
 * <p>
 * Version-specific action handling is delegated to {@link ContainerInputCompat}.
 */
public final class SlotClickEvents {

    private SlotClickEvents() {
    }

    /**
     * Fires the click (and, when applicable, drop) events for one slot click.
     *
     * @return {@code true} when the click should be cancelled
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
