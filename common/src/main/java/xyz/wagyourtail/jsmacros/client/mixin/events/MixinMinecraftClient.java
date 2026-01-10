package xyz.wagyourtail.jsmacros.client.mixin.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.wagyourtail.jsmacros.client.api.event.impl.EventLaunchGame;
import xyz.wagyourtail.jsmacros.client.api.event.impl.inventory.EventOpenContainer;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventOpenScreen;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventDimensionChange;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventDisconnect;
import xyz.wagyourtail.jsmacros.client.mixin.access.MixinDisconnectedScreen;

//? if <=1.21.8 {
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
//?}

@Mixin(Minecraft.class)
public abstract class MixinMinecraftClient {

    @Shadow
    public Screen screen;

    @Shadow
    public abstract void setScreen(@Nullable Screen screen);

    @Shadow
    @Nullable
    public MultiPlayerGameMode gameMode;

    @Shadow
    @Final
    private User user;

    @Inject(at = @At("HEAD"), method = "setLevel")
    public void onJoinWorld(
            ClientLevel world,
            //? if <=1.21.8 {
            ReceivingLevelScreen.Reason worldEntryReason,
            //?}
            CallbackInfo ci) {
        if (world != null) {
            new EventDimensionChange(world.dimension().location().toString()).trigger();
        }
    }

    @Unique
    private Screen jsmacros$prevScreen;

    @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;", opcode = Opcodes.PUTFIELD), method = "setScreen")
    public void onOpenScreen(Screen newScreen, CallbackInfo info) {
        if (newScreen != screen) {
            assert gameMode != null;
            jsmacros$prevScreen = screen;
            new EventOpenScreen(newScreen).trigger();
        }
    }

    @Inject(at = @At("TAIL"), method = "setScreen")
    public void afterOpenScreen(Screen screen, CallbackInfo info) {
        if (screen instanceof AbstractContainerScreen<?>) {
            assert gameMode != null;
            if (gameMode.getPlayerMode().isCreative() && !(screen instanceof CreativeModeInventoryScreen)) {
                return;
            }
            EventOpenContainer event = new EventOpenContainer(((AbstractContainerScreen<?>) screen));
            event.trigger();
            if (event.isCanceled()) {
                setScreen(jsmacros$prevScreen);
            }
        }
        jsmacros$prevScreen = null;
    }

    @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;isLocalServer:Z", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER), method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V")
    public void onDisconnect(Screen s, boolean transferring, CallbackInfo ci) {
        if (s instanceof DisconnectedScreen) {
            new EventDisconnect(((MixinDisconnectedScreen) s).getInfo().reason()).trigger();
        } else {
            new EventDisconnect(null).trigger();
        }
    }

    @Inject(at = @At("HEAD"), method = "run")
    private void onStart(CallbackInfo ci) {
        new EventLaunchGame(this.user.getName()).trigger();
    }

}
