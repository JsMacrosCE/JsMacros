package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.jsmacrosce.jsmacros.client.access.IClientPlayerInteractionManager;
import com.jsmacrosce.jsmacros.client.api.classes.InteractionProxy;

@Mixin(MultiPlayerGameMode.class)
class MixinClientPlayerInteractionManager implements IClientPlayerInteractionManager {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private int destroyDelay;

    @Inject(at = @At("RETURN"), method = "destroyBlock")
    public void onBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        InteractionProxy.Break.onBreakBlock(pos, cir.getReturnValueZ());
    }

    @Inject(at = @At("RETURN"), method = "continueDestroyBlock")
    public void breakingBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()
                && InteractionProxy.Break.isBreaking()
                && minecraft.hitResult != null
                && minecraft.hitResult.getType() == HitResult.Type.BLOCK
                && ((BlockHitResult) minecraft.hitResult).getBlockPos().equals(pos)
        ) InteractionProxy.Break.setOverride(false, "NOT_BREAKING");
    }

    @Override
    public int jsmacros_getBlockBreakingCooldown() {
        return destroyDelay;
    }

}
