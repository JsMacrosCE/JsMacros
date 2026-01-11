package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.jsmacrosce.jsmacros.client.access.IPlayerListHud;

@Mixin(PlayerTabOverlay.class)
public class MixinPlayerListHud implements IPlayerListHud {

    @Shadow
    private Component header;

    @Shadow
    private Component footer;

    @Override
    public Component jsmacros_getHeader() {
        return this.header;
    }

    @Override
    public Component jsmacros_getFooter() {
        return this.footer;
    }

}
