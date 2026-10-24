package com.jsmacrosce.jsmacros.client.mixin.events;

//? if >=26.2 {
/*import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.render.IScreen;
import com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventOpenContainer;
import com.jsmacrosce.jsmacros.client.api.event.impl.player.EventOpenScreen;
import com.jsmacrosce.jsmacros.client.gui.screens.KeyMacrosScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(Gui.class)
@DocletIgnore
public abstract class MixinGui {

    @Shadow
    public abstract @Nullable Screen screen();

    @Shadow
    public abstract void setScreen(@Nullable Screen screen);

    @Unique
    private Screen jsmacros$prevScreen;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;removed()V"), method = "setScreen")
    public void onCloseScreen(@Nullable Screen closedScreen, CallbackInfo ci) {
        if (closedScreen == null) {
            return;
        }
        Screen current = screen();
        if (current == null) {
            return;
        }
        Consumer<IScreen> onClose = ((IScreen) current).getOnClose();
        try {
            if (onClose != null) {
                onClose.accept((IScreen) current);
            }
        } catch (Throwable e) {
            JsMacrosClient.clientCore.profile.logError(e);
        }
    }

    @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;screen:Lnet/minecraft/client/gui/screens/Screen;", opcode = Opcodes.PUTFIELD), method = "setScreen")
    public void onOpenScreen(@Nullable Screen newScreen, CallbackInfo info) {
        if (jsmacros$prevScreen == null) {
            jsmacros$prevScreen = new KeyMacrosScreen(null);
        }

        if (newScreen != screen()) {
            jsmacros$prevScreen = screen();
            new EventOpenScreen(newScreen).trigger();
        }
    }

    @Inject(at = @At("TAIL"), method = "setScreen")
    public void afterOpenScreen(@Nullable Screen screen, CallbackInfo info) {
        if (screen instanceof AbstractContainerScreen<?>) {
            MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
            if (gameMode == null) {
                jsmacros$prevScreen = null;
                return;
            }
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
}
*///?} else {
final class MixinGui {
    private MixinGui() {
    }
}
//?}
