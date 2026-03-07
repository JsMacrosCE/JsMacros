package com.jsmacrosce.jsmacros.client.mixin.access;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//? if >=1.21.10 {
/*import net.minecraft.client.renderer.item.ItemStackRenderState;

@Mixin(ItemStackRenderState.class)
public interface MixinItemStackRenderState {

    @Accessor("activeLayerCount")
    int jsmacros$getActiveLayerCount();

    @Accessor("layers")
    ItemStackRenderState.LayerRenderState[] jsmacros$getLayers();

}
*///?} else {

public interface MixinItemStackRenderState {
}
//?}
