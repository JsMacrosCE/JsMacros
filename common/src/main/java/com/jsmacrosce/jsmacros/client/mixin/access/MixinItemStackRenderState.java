package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.jetbrains.annotations.ApiStatus;

//? if >=1.21.10 <26.1 {
/*import net.minecraft.client.renderer.item.ItemStackRenderState;

@ApiStatus.Internal
@DocletIgnore
@Mixin(ItemStackRenderState.class)
public interface MixinItemStackRenderState {

    @Accessor("activeLayerCount")
    int jsmacros$getActiveLayerCount();

    @Accessor("layers")
    ItemStackRenderState.LayerRenderState[] jsmacros$getLayers();

}
*///?} else {
@ApiStatus.Internal
@DocletIgnore
public interface MixinItemStackRenderState {
}
//?}
