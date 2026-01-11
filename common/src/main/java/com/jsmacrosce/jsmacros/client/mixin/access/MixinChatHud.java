package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
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
import com.jsmacrosce.jsmacros.client.access.IChatHud;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class MixinChatHud implements IChatHud {

    @Shadow
    private void addMessage(Component message, @Nullable MessageSignature signature, @Nullable GuiMessageTag indicator) {
    }

    @Shadow
    @Final
    private List<GuiMessage> allMessages;

    @Override
    public void jsmacros_addMessageBypass(Component message) {
        addMessage(message, null, GuiMessageTag.system());
    }

    @Unique
    ThreadLocal<Integer> jsmacros$positionOverride = ThreadLocal.withInitial(() -> 0);

    @Override
    public void jsmacros_addMessageAtIndexBypass(Component message, int index, int time) {
        jsmacros$positionOverride.set(index);
        addMessage(message, null, GuiMessageTag.system());
        jsmacros$positionOverride.set(0);
    }

    @ModifyArg(method = "addMessageToQueue(Lnet/minecraft/client/GuiMessage;)V", at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", remap = false))
    public int overrideMessagePos(int pos) {
        return jsmacros$positionOverride.get();
    }


}
