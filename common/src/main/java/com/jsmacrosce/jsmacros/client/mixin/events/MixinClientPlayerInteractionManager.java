package com.jsmacrosce.jsmacros.client.mixin.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.jsmacrosce.jsmacros.client.api.event.impl.player.EventAttackBlock;
import com.jsmacrosce.jsmacros.client.api.event.impl.player.EventAttackEntity;
import com.jsmacrosce.jsmacros.client.api.event.impl.player.EventInteractBlock;
import com.jsmacrosce.jsmacros.client.api.event.impl.player.EventInteractEntity;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockDataHelper;

@Mixin(MultiPlayerGameMode.class)
public class MixinClientPlayerInteractionManager {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(at = @At("RETURN"), method = "useItemOn")
    public void onInteractBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue() != InteractionResult.FAIL) {
            BlockPos pos = hitResult.getBlockPos();
            new EventInteractBlock(
                    hand != InteractionHand.MAIN_HAND,
                    cir.getReturnValue().consumesAction(),
                    new BlockDataHelper(player.level().getBlockState(pos), player.level().getBlockEntity(pos), pos),
                    hitResult.getDirection().get3DDataValue()
            ).trigger();
        }
    }

    @Inject(at = @At("RETURN"), method = "startDestroyBlock")
    public void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            assert minecraft.level != null;
            new EventAttackBlock(
                    new BlockDataHelper(minecraft.level.getBlockState(pos), minecraft.level.getBlockEntity(pos), pos),
                    direction.get3DDataValue()
            ).trigger();
        }
    }

    @Inject(at = @At("RETURN"), method = "attack")
    public void onAttackEntity(Player player, Entity target, CallbackInfo ci) {
        new EventAttackEntity(target).trigger();
    }

    @Inject(at = @At("RETURN"), method = "interact")
    public void onInteractEntity(Player player, Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue() != InteractionResult.FAIL) {
            new EventInteractEntity(hand != InteractionHand.MAIN_HAND, cir.getReturnValue().consumesAction(), entity).trigger();
        }
    }

}
