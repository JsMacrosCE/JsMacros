package xyz.wagyourtail.jsmacros.client.mixin.access;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.client.api.classes.InteractionProxy;
import xyz.wagyourtail.jsmacros.client.api.classes.render.Draw2D;
import xyz.wagyourtail.jsmacros.client.api.classes.render.IDraw2D;
import xyz.wagyourtail.jsmacros.client.api.classes.render.IScreen;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FHud;

import java.util.function.Consumer;

//? if <=1.21.8 {
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
//?}

@Mixin(Minecraft.class)
abstract
class MixinMinecraftClient {

    @Shadow
    protected abstract void continueAttack(boolean breaking);

    @Shadow
    protected int missTime;

    @Shadow
    public Screen screen;

    @Shadow
    private Overlay overlay;

    @Shadow
    private volatile boolean pause;

    @Shadow
    @Final
    public Options options;

    @Shadow
    private int rightClickDelay;

    @Shadow
    @Nullable
    public ClientLevel level;

    @Inject(at = @At("TAIL"), method = "resizeDisplay")
    public void onResolutionChanged(CallbackInfo info) {

        synchronized (FHud.overlays) {
            for (IDraw2D<Draw2D> h : FHud.overlays) {
                try {
                    ((Draw2D) h).init();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;removed()V"), method = "setScreen")
    public void onCloseScreen(Screen screen, CallbackInfo ci) {
        if (screen == null) return;
        Consumer<IScreen> onClose = ((IScreen) screen).getOnClose();
        try {
            if (onClose != null) {
                onClose.accept((IScreen) screen);
            }
        } catch (Throwable e) {
            JsMacrosClient.clientCore.profile.logError(e);
        }
    }

    @Inject(at = @At("HEAD"), method = "setLevel")
    public void onJoinWorld(
            ClientLevel world,
            //? if <=1.21.8 {
            ReceivingLevelScreen.Reason reason,
            //?}
            CallbackInfo ci) {
        InteractionProxy.reset();
    }

    @Inject(
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;isLocalServer:Z", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER),
            method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V"
    )
    public void onDisconnect(Screen disconnectionScreen, boolean transferring, CallbackInfo ci) {
        InteractionProxy.reset();
    }

    @Inject(at = @At("HEAD"), method = "continueAttack", cancellable = true)
    private void overrideBlockBreaking(boolean breaking, CallbackInfo ci) {
        if (InteractionProxy.Break.isBreaking()) {
            if (options.keyAttack.isDown()) {
                InteractionProxy.Break.setOverride(false, "INTERRUPTED");
                return;
            }
            if (this.missTime > 20) this.missTime = 0; // prevent mc from setting it to 10000 while in screen
            if (!breaking) {
                ci.cancel();
                continueAttack(true);
            }
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startDestroyBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"), method = "startAttack", cancellable = true)
    private void blockAttackBlock(CallbackInfoReturnable<Boolean> cir) {
        if (InteractionProxy.Break.isBreaking()) {
            cir.cancel();
            cir.setReturnValue(false);
        }
    }

    @Inject(at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=gameRenderer"), method = "tick")
    private void ensureOverrideInteractions(CallbackInfo ci) {
        if (!(overlay == null && screen == null) && !pause) {
            if (InteractionProxy.Break.isBreaking()) {
                continueAttack(true);
                if (missTime > 0) --missTime;
            }
            InteractionProxy.Interact.ensureInteracting(rightClickDelay);
        }
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;releaseUsingItem(Lnet/minecraft/world/entity/player/Player;)V"), method = "handleKeybinds")
    private void continueInteracting(MultiPlayerGameMode im, Player player) {
        if (!InteractionProxy.Interact.isInteracting()) im.releaseUsingItem(player);
    }

    @Inject(at = @At("TAIL"), method = "handleKeybinds")
    private void ensureInteracting(CallbackInfo ci) {
        InteractionProxy.Interact.ensureInteracting(rightClickDelay);
    }

    // TODO: Fix the below todo. In the meantime, I'll keep the pre 1.21.9 code in
    //? if <=1.21.8 {
    @ModifyExpressionValue(method = "continueAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"))
    private boolean catchEmptyShapeException(boolean value, @Local BlockPos blockPos) {
        if (value) return true;
        assert level != null;
        return level.getBlockState(blockPos).getShape(level, blockPos).isEmpty();
    }
    //?}

    // TODO: Currently MixinStyleSerializer.redirectClickGetAction and MixinMinecraftClient.catchEmptyShapeException are
    //  broken in production. I do not know why this is, but it works in dev (I think).
    /*
    @WrapOperation(method = "continueAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"))
    private boolean catchEmptyShapeException(BlockState state, Operation<Boolean> original, @Local BlockPos blockPos) {
        // this.level.getBlockState(blockPos).isAir()
        if (original.call(state)) {
            return true;
        }

        return state.getShape(level, blockPos).isEmpty();
    }
    */
}
