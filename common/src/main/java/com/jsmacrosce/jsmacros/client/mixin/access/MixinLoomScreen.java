package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.client.gui.screens.inventory.LoomScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.jsmacrosce.jsmacros.client.access.ILoomScreen;

@Mixin(LoomScreen.class)
public class MixinLoomScreen implements ILoomScreen {

    @Shadow
    private boolean displayPatterns;

    @Override
    public boolean jsmacros_canApplyDyePattern() {
        return displayPatterns;
    }

}
