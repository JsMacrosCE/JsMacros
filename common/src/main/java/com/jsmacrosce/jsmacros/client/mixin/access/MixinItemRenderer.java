package com.jsmacrosce.jsmacros.client.mixin.access;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

//? if >=1.21.10 {
/*import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.PoseStack;
//? if >=1.21.11 {
/^import net.minecraft.client.renderer.rendertype.RenderType;
^///?} else {
import net.minecraft.client.renderer.RenderType;
//?}

import java.util.List;

@Mixin(ItemRenderer.class)
public interface MixinItemRenderer {

    @Invoker("renderItem")
    void jsmacros$renderItem(
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            int[] tintLayers,
            List<BakedQuad> quads,
            RenderType renderType,
            ItemStackRenderState.FoilType foilType
    );

}
*///?} else {

public interface MixinItemRenderer {
}
//?}
