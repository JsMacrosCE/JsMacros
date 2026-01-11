package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.world.entity.Interaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Interaction.class)
public interface MixinInteractionEntity2 {

    @Invoker("getWidth")
    float callGetInteractionWidth();

    @Invoker("getHeight")
    float callGetInteractionHeight();

    @Invoker("getResponse")
    boolean callShouldRespond();

}
