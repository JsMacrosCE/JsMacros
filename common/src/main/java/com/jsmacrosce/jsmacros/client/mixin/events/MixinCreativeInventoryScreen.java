package com.jsmacrosce.jsmacros.client.mixin.events;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
//? if >=26.1 {
/*import net.minecraft.world.inventory.ContainerInput;
*///?} else {
import net.minecraft.world.inventory.ClickType;
//?}
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.util.SlotClickEventHelper;

import java.lang.reflect.Field;
import java.util.Arrays;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class MixinCreativeInventoryScreen {

    @Unique
    private static Class<? extends Slot> jsmacros$lockableSlot;

    @Unique
    private static Class<? extends Slot> jsmacros$creativeSlot;

    @Unique
    private static Field jsmacros$slotInCreativeSlot;

    @Unique
    private synchronized Slot jsmacros$getSlotFromCreativeSlot(Slot in) {
        if (in.getClass().equals(Slot.class)) {
            return in;
        }
        boolean lockable = in.getClass().equals(jsmacros$lockableSlot);
        boolean creative = in.getClass().equals(jsmacros$creativeSlot);
        if (lockable) {
            return in;
        }
        if (creative) {
            try {
                return (Slot) jsmacros$slotInCreativeSlot.get(in);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        if (jsmacros$lockableSlot == null || jsmacros$creativeSlot == null) {
            // define creative/lockable slot classes
            try {
                Class<? extends Slot> unknown = in.getClass();
                Field slotField = Arrays.stream(unknown.getDeclaredFields())
                        .filter(e -> e.getType().equals(Slot.class))
                        .findFirst()
                        .orElse(null);
                if (slotField == null) {
                    jsmacros$lockableSlot = unknown;
                } else {
                    jsmacros$slotInCreativeSlot = slotField;
                    jsmacros$slotInCreativeSlot.setAccessible(true);
                    jsmacros$creativeSlot = unknown;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return jsmacros$getSlotFromCreativeSlot(in);
        }
        throw new NullPointerException("Unknown slot class");
    }

    //? if >=26.1 {
    /*@Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    public void beforeMouseClick(Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo ci) {
        if (slot != null) {
            slotId = jsmacros$getSlotFromCreativeSlot(slot).index;
        }
        SlotClickEventHelper.fire((AbstractContainerScreen<?>) (Object) this, actionType.id(), actionType == ContainerInput.THROW, button, slotId, ci);
    }
    *///?} else {
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    public void beforeMouseClick(Slot slot, int slotId, int button, ClickType actionType, CallbackInfo ci) {
        if (slot != null) {
            slotId = jsmacros$getSlotFromCreativeSlot(slot).index;
        }
        SlotClickEventHelper.fire((AbstractContainerScreen<?>) (Object) this, actionType.ordinal(), actionType == ClickType.THROW, button, slotId, ci);
    }
    //?}

}
