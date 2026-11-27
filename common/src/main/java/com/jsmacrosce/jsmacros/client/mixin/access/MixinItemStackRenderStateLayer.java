package com.jsmacrosce.jsmacros.client.mixin.access;

import com.jsmacrosce.doclet.DocletIgnore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.jetbrains.annotations.ApiStatus;

//? if >=1.21.10 <26.1 {
/*import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemStackRenderState;
//? if >=1.21.11 {
/^import net.minecraft.client.renderer.rendertype.RenderType;
^///?} else {
import net.minecraft.client.renderer.RenderType;
//?}

import java.util.List;

@ApiStatus.Internal
@DocletIgnore
@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface MixinItemStackRenderStateLayer {

    @Accessor("quads")
    List<BakedQuad> jsmacros$getQuads();

    @Accessor("tintLayers")
    int[] jsmacros$getTintLayers();

    @Accessor("renderType")
    RenderType jsmacros$getRenderType();

    @Accessor("foilType")
    ItemStackRenderState.FoilType jsmacros$getFoilType();

    @Accessor("transform")
    ItemTransform jsmacros$getTransform();

}
*///?} else {

@ApiStatus.Internal
@DocletIgnore
public interface MixinItemStackRenderStateLayer {
}
//?}
