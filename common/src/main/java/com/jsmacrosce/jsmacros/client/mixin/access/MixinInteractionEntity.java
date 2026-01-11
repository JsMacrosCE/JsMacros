package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.world.entity.Interaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.jsmacrosce.jsmacros.client.access.IMixinInteractionEntity;

@Mixin(Interaction.class)
public abstract class MixinInteractionEntity implements IMixinInteractionEntity {

    @Unique
    private boolean jsmacros$canHitOverride = true;

    @Override
    public void jsmacros_setCanHitOverride(boolean value) {
        jsmacros$canHitOverride = value;
    }

    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    public void overrideCanHit(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(jsmacros$canHitOverride);
    }

}
