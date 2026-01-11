package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.jsmacrosce.jsmacros.client.access.IMerchantScreen;

@Mixin(MerchantScreen.class)
public abstract class MixinMerchantScreen implements IMerchantScreen {

    @Shadow
    private int shopItem;

    @Shadow
    protected abstract void postButtonClick();

    @Override
    public void jsmacros_selectIndex(int index) {
        shopItem = index;
        postButtonClick();
    }

}
