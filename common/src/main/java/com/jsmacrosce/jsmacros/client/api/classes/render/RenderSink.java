package com.jsmacrosce.jsmacros.client.api.classes.render;

import com.jsmacrosce.doclet.DocletIgnore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

//? if >=1.21.11 {
/*import net.minecraft.client.renderer.rendertype.RenderType;
*///?} else {
import net.minecraft.client.renderer.RenderType;
//?}

//? if >=26.2 {
/*import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
*///?} else {
import net.minecraft.client.renderer.MultiBufferSource;
//?}

/**
 * Internal world-overlay rendering target. Geometry callbacks must capture frame values,
 * not mutable element fields: older targets execute them immediately, while 26.2 defers them.
 * On 26.2 every submission gets its own ordered layer in the selected depth phase, including
 * text and item features, so Minecraft cannot batch across surface elements.
 * Item nodes use vanilla feature extraction, including special models, before phase routing.
 */
@DocletIgnore
public final class RenderSink {
    @FunctionalInterface
    @DocletIgnore
    public interface Geometry {
        void render(PoseStack.Pose pose, VertexConsumer consumer);
    }

    //? if >=26.2 {
    /*private final SubmitNodeStorage storage;
    private final boolean alwaysOnTop;
    private final SubmitNodeStorage itemSubmits = new SubmitNodeStorage();
    private int nextOrder = 1;

    public RenderSink(SubmitNodeStorage storage, boolean alwaysOnTop) {
        this.storage = storage;
        this.alwaysOnTop = alwaysOnTop;
    }

    public void submitGeometry(PoseStack matrices, RenderType renderType, Geometry geometry) {
        submit(new CustomFeatureRenderer.Submit(matrices.last().copy(), renderType, geometry::render));
    }

    public void drawText(PoseStack matrices, FormattedCharSequence text, float x, float y, int color,
                         boolean shadow, Font.DisplayMode displayMode, int light) {
        submit(new TextFeatureRenderer.Submit(new Matrix4f(matrices.last().pose()), x, y, text,
                shadow, displayMode, light, color, 0, 0));
    }

    public void submitItem(PoseStack matrices, ItemStackRenderState state, int light) {
        state.submit(matrices, itemSubmits, light, OverlayTexture.NO_OVERLAY, 0);
        itemSubmits.drainPhases(phase -> phase.sortInto((node, sorted) -> submit(node)));
    }

    private void submit(SubmitNode node) {
        SubmitNodeCollection layer;
        do {
            layer = storage.order(nextOrder);
            nextOrder = Math.incrementExact(nextOrder);
        } while (layer.allPhases().stream().anyMatch(phase -> !phase.isEmpty()));
        (alwaysOnTop ? layer.alwaysOnTop : layer.afterTerrain).submit(node);
    }
    *///?} else {
    private final MultiBufferSource source;

    public RenderSink(MultiBufferSource source) {
        this.source = source;
    }

    public void submitGeometry(PoseStack matrices, RenderType renderType, Geometry geometry) {
        geometry.render(matrices.last(), source.getBuffer(renderType));
    }

    public VertexConsumer getBuffer(RenderType renderType) {
        return source.getBuffer(renderType);
    }

    public void endBatch() {
        if (source instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch();
        }
    }

    public MultiBufferSource getSource() {
        return source;
    }
    //?}
}
