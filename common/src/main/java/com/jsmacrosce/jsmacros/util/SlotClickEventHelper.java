package com.jsmacrosce.jsmacros.util;

import com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventClickSlot;
import com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventDropSlot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class SlotClickEventHelper {
    private SlotClickEventHelper() {}

    public static void fire(AbstractContainerScreen<?> screen, int actionId, boolean isThrow, int button, int slotId, CallbackInfo ci) {
        EventClickSlot event = new EventClickSlot(screen, actionId, button, slotId);
        event.trigger();
        if (event.isCanceled()) {
            ci.cancel();
            return;
        }
        if (isThrow || slotId == -999) {
            EventDropSlot eventDrop = new EventDropSlot(screen, slotId, button == 1);
            eventDrop.trigger();
            if (eventDrop.isCanceled()) {
                ci.cancel();
            }
        }
    }
}
