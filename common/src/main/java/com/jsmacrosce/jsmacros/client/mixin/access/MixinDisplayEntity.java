package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface MixinDisplayEntity {

    @Invoker("getBrightnessOverride")
    Brightness callGetBrightnessUnpacked();

    @Invoker
    float callGetViewRange();

    @Invoker
    float callGetShadowRadius();

    @Invoker
    float callGetShadowStrength();

    @Invoker("getWidth")
    float callGetDisplayWidth();

    @Invoker
    int callGetGlowColorOverride();

    @Invoker("getHeight")
    float callGetDisplayHeight();

    @Invoker("getBillboardConstraints")
    Display.BillboardConstraints callGetBillboardMode();

}
