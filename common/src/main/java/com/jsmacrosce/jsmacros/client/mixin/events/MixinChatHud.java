package com.jsmacrosce.jsmacros.client.mixin.events;

import net.minecraft.ChatFormatting;
//? if >=26.1 {
/*import net.minecraft.client.multiplayer.chat.GuiMessageTag;
*///?} else {
import net.minecraft.client.GuiMessageTag;
//?}
import net.minecraft.client.gui.components.ChatComponent;
//? if >=26.1 {
/*import net.minecraft.client.multiplayer.chat.GuiMessageSource;
*///?}
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.client.api.event.impl.EventRecvMessage;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;

@Mixin(ChatComponent.class)
class MixinChatHud {
    @Unique
    private EventRecvMessage jsmacros$eventRecvMessage;
    @Unique
    private Component jsmacros$originalMessage;

    //? if >=26.1 {
    /*@Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAddMessage1(Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag indicator, CallbackInfo ci) {
        jsmacros$originalMessage = message;
        jsmacros$eventRecvMessage = new EventRecvMessage(message, signature, indicator);
        jsmacros$eventRecvMessage.trigger();
        if (jsmacros$eventRecvMessage.isCanceled()) {
            ci.cancel();
        }
    }
    *///?} else {
    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAddMessage1(Component message, MessageSignature signature, GuiMessageTag indicator, CallbackInfo ci) {
        jsmacros$originalMessage = message;
        jsmacros$eventRecvMessage = new EventRecvMessage(message, signature, indicator);
        jsmacros$eventRecvMessage.trigger();
        if (jsmacros$eventRecvMessage.isCanceled()) {
            ci.cancel();
        }
    }
    //?}

    @Unique
    private boolean jsmacros$modifiedEventRecieve;

    @ModifyVariable(
            //? if >=26.1 {
            /*method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            *///?} else {
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            //?}
            at = @At(value = "HEAD"),
            argsOnly = true
    )
    private Component modifyChatMessage(Component text) {
        jsmacros$modifiedEventRecieve = false;
        if (text == null) {
            return null;
        }
        final TextHelper result = jsmacros$eventRecvMessage.text;
        if (result == null) {
            return null;
        }
        if (!result.getRaw().equals(text)) {
            jsmacros$modifiedEventRecieve = true;
            return result.getRaw();
        } else {
            return text;
        }
    }

    @Unique
    private final Component MODIFIED_TEXT = Component.translatable("jsmacrosce.chat.tag.modified").withStyle(ChatFormatting.UNDERLINE);

    @ModifyVariable(
            //? if >=26.1 {
            /*method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            *///?} else {
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            //?}
            at = @At(value = "HEAD"),
            argsOnly = true
    )
    private GuiMessageTag modifyChatMessageSignature(GuiMessageTag signature) {
        if (jsmacros$modifiedEventRecieve) {
            MutableComponent text2 = Component.empty().append(MODIFIED_TEXT).append(CommonComponents.NEW_LINE);
            if (signature != null && signature.text() != null) {
                text2.append(jsmacros$originalMessage).append(CommonComponents.NEW_LINE).append(signature.text());
            } else {
                text2.append(jsmacros$originalMessage);
            }
            return new GuiMessageTag(15386724, GuiMessageTag.Icon.CHAT_MODIFIED, text2, "Modified");
        } else {
            return signature;
        }
    }

    //? if >=26.1 {
    /*@Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAddChatMessage(Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag indicator, CallbackInfo ci) {
        if (message == null) {
            ci.cancel();
        }
    }
    *///?} else {
    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAddChatMessage(Component message, MessageSignature signature, GuiMessageTag indicator, CallbackInfo ci) {
        if (message == null) {
            ci.cancel();
        }
    }
    //?}

}
