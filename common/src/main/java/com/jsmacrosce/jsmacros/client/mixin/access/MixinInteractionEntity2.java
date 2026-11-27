package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;

import net.minecraft.world.entity.Interaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.jetbrains.annotations.ApiStatus;

@Mixin(Interaction.class)
@ApiStatus.Internal
@DocletIgnore
public interface MixinInteractionEntity2 {

    @Invoker("getWidth")
    float callGetInteractionWidth();

    @Invoker("getHeight")
    float callGetInteractionHeight();

    @Invoker("getResponse")
    boolean callShouldRespond();

}
