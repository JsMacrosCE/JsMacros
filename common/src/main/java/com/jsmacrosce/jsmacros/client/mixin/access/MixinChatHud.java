package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;

import org.jetbrains.annotations.Nullable;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.jsmacrosce.jsmacros.client.access.IChatHud;


import java.util.List;

//? if >=26.1 {
/*import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
*///? } else {
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
//? }

@Mixin(ChatComponent.class)
public abstract class MixinChatHud implements IChatHud {

    //? if >=26.1 {
    /*@Shadow
    private void addMessage(
            Component contents,
            @Nullable MessageSignature signature,
            GuiMessageSource source,
            @Nullable GuiMessageTag tag) {
    }
    *///? } else {
    @Shadow
    private void addMessage(Component message, @Nullable MessageSignature signature, @Nullable GuiMessageTag indicator) {
    }
    //? }

    @Shadow
    @Final
    public List<GuiMessage> allMessages;

    @Override
    public void jsmacros_addMessageBypass(Component message) {
        addMessage(
                message,
                null,
                //? if >=26.1 {
                /*GuiMessageSource.SYSTEM_CLIENT,
                *///? }
                GuiMessageTag.system()
        );
    }

    @Unique
    ThreadLocal<Integer> jsmacros$positionOverride = ThreadLocal.withInitial(() -> 0);

    @Override
    public void jsmacros_addMessageAtIndexBypass(Component message, int index, int time) {
        jsmacros$positionOverride.set(index);
        addMessage(
                message,
                null,
                //? if >=26.1 {
                /*GuiMessageSource.SYSTEM_CLIENT,
                *///? }
                GuiMessageTag.system());
        jsmacros$positionOverride.set(0);
    }

    //? if >=26.1 {
    /*@Redirect(
            method = "addMessageToQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V")
    )
    *///? } else if >=1.21.11 {
    /*@Redirect(
            method = "addMessageToQueue(Lnet/minecraft/client/GuiMessage;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V")
    )
    *///? }
    //? if >= 1.21.11 {
    /*public <E> void overrideMessagePos(List<GuiMessage> instance, E guiMessage) {
        this.allMessages.add(jsmacros$positionOverride.get(), (GuiMessage) guiMessage);
    }
    *///? } else {
    @ModifyArg(
        method = "addMessageToQueue(Lnet/minecraft/client/GuiMessage;)V",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;add(ILjava/lang/Object;)V",
            remap = false
        )
    )
    public int overrideMessagePos(int pos) {
        return jsmacros$positionOverride.get();
    }
    //? }

}
