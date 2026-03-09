package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.access.IInventory;
import com.jsmacrosce.jsmacros.client.config.ClientConfigV2;

@Mixin(AbstractContainerScreen.class)
public class MixinHandledScreen<T extends AbstractContainerMenu> extends Screen implements IInventory {

    protected MixinHandledScreen(Component title) {
        super(title);
    }

    @Shadow
    private Slot getHoveredSlot(double x, double y) {
        return null;
    }

    @Shadow
    @Final
    protected T menu;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Override
    public int jsmacros$getX() {
        return leftPos;
    }

    @Override
    public int jsmacros$getY() {
        return topPos;
    }

    @Override
    public Slot jsmacros_getSlotUnder(double x, double y) {
        return getHoveredSlot(x, y);
    }

    @Inject(method = "renderSlot", at = @At("TAIL"))
    //? if >=1.21.11 {
    /*private void onDrawSlot(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
    *///? } else {
    private void onDrawSlot(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
    //? }
        if (!JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).showSlotIndexes) return;

        if (!slot.isActive()) return;

        int index = menu.slots.indexOf(slot);
        guiGraphics.drawString(Minecraft.getInstance().font, String.valueOf(index), slot.x, slot.y, 0xCCFFFFFF, false);
    }

}
