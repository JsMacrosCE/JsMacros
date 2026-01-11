package com.jsmacrosce.jsmacros.client.mixin.events;

import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import com.jsmacrosce.jsmacros.client.api.event.impl.EventTitle;

@Mixin(ChatListener.class)
public class MixinMessageHandler {

    @ModifyArg(method = "handleSystemMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private Component modifyOverlayMessage(Component text) {
        EventTitle et = new EventTitle("ACTIONBAR", text);
        et.trigger();
        if (et.message == null || et.isCanceled()) {
            return null;
        } else {
            return et.message.getRaw();
        }
    }

}
