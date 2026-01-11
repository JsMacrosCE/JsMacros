package com.jsmacrosce.jsmacros.client.mixin.events;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.client.api.event.impl.EventSendMessage;

@Mixin(ChatScreen.class)
public abstract class MixinChatScreen extends Screen {

    //IGNORE
    protected MixinChatScreen(Component title) {
        super(title);
    }

    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    public void onSendChatMessage(String chatText, boolean addToHistory, CallbackInfo ci) {
        final EventSendMessage event = new EventSendMessage(chatText);
        event.trigger();
        if (event.message == null || event.message.equals("") || event.isCanceled()) {
            ci.cancel();
        } else if (!event.message.equals(chatText)) {
            ci.cancel();
            assert this.minecraft != null;
            assert this.minecraft.player != null;
            if (event.message.startsWith("/")) {
                this.minecraft.player.connection.sendCommand(event.message.substring(1));
            } else {
                this.minecraft.player.connection.sendChat(event.message);
            }
        }
    }

}
