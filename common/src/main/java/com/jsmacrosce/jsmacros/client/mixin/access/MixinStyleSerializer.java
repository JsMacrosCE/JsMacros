package com.jsmacrosce.jsmacros.client.mixin.access;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Style.Serializer.class, priority = 1001)
public class MixinStyleSerializer {

    @ModifyExpressionValue(
        method = {"method_54215", "lambda$static$7"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/network/chat/Style;clickEvent:Lnet/minecraft/network/chat/ClickEvent;", opcode = Opcodes.GETFIELD)
    )
    private static ClickEvent redirectClickGetAction(ClickEvent original) {
        if (original == null) return null;
        if (original.action() == null) {
            return null;
        }
        return original;
    }

}
