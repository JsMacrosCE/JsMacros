package com.jsmacrosce.jsmacros.client.mixin.events;

import net.minecraft.ChatFormatting;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
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

import javax.annotation.Nullable;

@Mixin(ChatComponent.class)
class MixinChatHud {
    @Unique
    private EventRecvMessage jsmacros$eventRecvMessage;
    @Unique
    private Component jsmacros$originalMessage;

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAddMessage1(Component message, @Nullable MessageSignature signature, @Nullable GuiMessageTag indicator, CallbackInfo ci) {
        jsmacros$originalMessage = message;
        jsmacros$eventRecvMessage = new EventRecvMessage(message, signature, indicator);
        jsmacros$eventRecvMessage.trigger();
        if (jsmacros$eventRecvMessage.isCanceled()) {
            ci.cancel();
        }
    }

    @Unique
    private boolean jsmacros$modifiedEventRecieve;

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At(value = "HEAD"),
            argsOnly = true
    )
    private Component modifyChatMessage(Component text) {
        jsmacros$modifiedEventRecieve = false;
        if (text == null || jsmacros$eventRecvMessage == null) {
            return null;
        }

        final TextHelper result = jsmacros$eventRecvMessage.text;
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
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At(value = "HEAD"),
            argsOnly = true
    )
    private GuiMessageTag modifyChatMessageSignature(GuiMessageTag signature) {
        if (!jsmacros$modifiedEventRecieve) {
            return signature;
        }

        MutableComponent text = Component.empty().append(MODIFIED_TEXT).append(CommonComponents.NEW_LINE);
        text.append(jsmacros$originalMessage);
        if (signature != null && signature.text() != null) {
            text.append(CommonComponents.NEW_LINE).append(signature.text());
        }

        return new GuiMessageTag(0xEAC864, GuiMessageTag.Icon.CHAT_MODIFIED, text, "Modified");
    }

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

}
