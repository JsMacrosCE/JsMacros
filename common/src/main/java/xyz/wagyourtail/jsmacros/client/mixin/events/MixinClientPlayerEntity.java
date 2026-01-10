package xyz.wagyourtail.jsmacros.client.mixin.events;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.wagyourtail.jsmacros.api.PlayerInput;
import xyz.wagyourtail.jsmacros.client.api.event.impl.inventory.EventDropSlot;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventAirChange;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventEXPChange;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventRiding;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventSignEdit;
import xyz.wagyourtail.jsmacros.client.movement.MovementQueue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Mixin(LocalPlayer.class)
abstract class MixinClientPlayerEntity extends AbstractClientPlayer {

    @Shadow
    public ClientInput input;
    @Shadow
    @Final
    public ClientPacketListener connection;
    @Shadow
    @Final
    protected Minecraft minecraft;

    // IGNORE
    public MixinClientPlayerEntity(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Shadow
    public abstract boolean isMovingSlowly();

    @Override
    public void setAirSupply(int air) {
        if (air % 20 == 0) {
            new EventAirChange(air).trigger();
        }
        super.setAirSupply(air);
    }

    @Inject(at = @At("HEAD"), method = "setExperienceValues")
    public void onSetExperience(float progress, int total, int level, CallbackInfo info) {
        new EventEXPChange(progress, total, level, this.experienceProgress, this.totalExperience, this.experienceLevel).trigger();
    }

    @Inject(at = @At("HEAD"), method = "openTextEdit", cancellable = true)
    public void onOpenEditSignScreen(SignBlockEntity sign, boolean front, CallbackInfo ci) {
        var originalLines = Arrays.stream(sign.getText(front)
                .getMessages(minecraft.isTextFilteringEnabled()))
                .map(Component::getString)
                .toList();

        final EventSignEdit event = new EventSignEdit(new ArrayList<>(originalLines),
                sign.getBlockPos().getX(), sign.getBlockPos().getY(), sign.getBlockPos().getZ(), front);
        event.trigger();

        // Cleanup sign edit result, null or lines != 4 need to be fixed.
        List<String> lines = event.signText;
        if (lines == null || lines.size() != 4) lines = Arrays.asList("", "", "", "");
        if (event.signText != null) {
            for (int i = 0; i < Math.min(4, event.signText.size()); i++) {
                lines.set(i, event.signText.get(i));
            }
        }

        // Replace text only if needed
        if (!Objects.equals(originalLines, lines)) {
            SignText text = new SignText();
            for (int i = 0; i < 4; ++i) {
                text = text.setMessage(i, Component.nullToEmpty(lines.get(i)));
            }
            sign.setText(text, front);
            sign.setChanged();
        }

        // Cancel only if needed
        if (event.closeScreen || event.isCanceled()) {
            connection.send(new ServerboundSignUpdatePacket(sign.getBlockPos(), front, lines.get(0), lines.get(1), lines.get(2), lines.get(3)));
            ci.cancel();
        }
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/tutorial/Tutorial;onInput(Lnet/minecraft/client/player/ClientInput;)V"))
    public void overwriteInputs(CallbackInfo ci) {
        PlayerInput moveInput = MovementQueue.tick(minecraft.player);
        if (moveInput == null) {
            return;
        }
        // Replicates KeyboardInput#tick
        this.input.keyPresses = new net.minecraft.world.entity.player.Input(
                moveInput.movementForward > 0,
                moveInput.movementForward < 0,
                moveInput.movementSideways > 0,
                moveInput.movementSideways < 0,
                moveInput.jumping,
                moveInput.sneaking,
                moveInput.sprinting
        );
        var plIn = this.input.keyPresses;
        float f = KeyboardInput.calculateImpulse(plIn.forward(), plIn.backward());
        float g = KeyboardInput.calculateImpulse(plIn.left(), plIn.right());
        this.input.moveVector = new Vec2(g, f).normalized();
    }

    @Inject(method = "startRiding", at = @At(value = "RETURN", ordinal = 1))
    public void onStartRiding(Entity entity, boolean force,
                              //? if >1.21.8 {
                              /*boolean unknown,
                              *///?}
                              CallbackInfoReturnable<Boolean> cir) {
        new EventRiding(true, entity).trigger();
    }

    @Inject(method = "removeVehicle", at = @At("HEAD"))
    public void onStopRiding(CallbackInfo ci) {
        if (this.getVehicle() != null) {
            new EventRiding(false, this.getVehicle()).trigger();
        }
    }

    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    public void onDropSelected(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        int selectedHotbarIndex = getInventory().getSelectedSlot();
        EventDropSlot event = new EventDropSlot(null, 36 + selectedHotbarIndex, entireStack);
        event.trigger();
        if (event.isCanceled()) {
            cir.setReturnValue(false);
        }
    }

}
