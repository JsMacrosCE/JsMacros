package com.jsmacrosce.jsmacros.client.mixin.events;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventClickSlot;
import com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventDropSlot;

//? if >=26.1 {
/*import net.minecraft.world.inventory.ContainerInput;
*///? } else {
import net.minecraft.world.inventory.ClickType;
 //? }

@Mixin(AbstractContainerScreen.class)
public class MixinHandledScreen {

    //? if >=26.1 {
    /*@Inject(
            method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handleContainerInput(IIILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V"
            ),
            cancellable = true
    )
    public void beforeMouseClick(Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo ci) {
    *///? } else {
    @Inject(
        method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handleInventoryMouseClick(IIILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V"
        ),
        cancellable = true
    )
    public void beforeMouseClick(Slot slot, int slotId, int button, ClickType actionType, CallbackInfo ci) {
    //? }
        EventClickSlot event = new EventClickSlot((AbstractContainerScreen<?>) (Object) this, actionType.ordinal(), button, slotId);
        event.trigger();
        if (event.isCanceled()) {
            ci.cancel();
            return;
        }

        //? if >=26.1 {
        /*if (actionType == ContainerInput.THROW || slotId == -999) {
        *///? } else {
        if (actionType == ClickType.THROW || slotId == -999) {
        //? }
            EventDropSlot eventDrop = new EventDropSlot((AbstractContainerScreen<?>) (Object) this, slotId, button == 1);
            eventDrop.trigger();
            if (eventDrop.isCanceled()) {
                ci.cancel();
            }
        }
    }

}
