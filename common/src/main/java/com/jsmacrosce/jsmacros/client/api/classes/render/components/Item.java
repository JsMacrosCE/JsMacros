package com.jsmacrosce.jsmacros.client.api.classes.render.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///? } else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;
import com.jsmacrosce.jsmacros.client.api.classes.render.IDraw2D;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

//? if >=26.1 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;
*///? } else {
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;
//? }

//? if >=1.21.10 <26.1 {
/*import com.jsmacrosce.jsmacros.client.mixin.access.MixinItemRenderer;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinItemStackRenderState;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinItemStackRenderStateLayer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemStackRenderState;
//? if >=1.21.11 {
/^import net.minecraft.client.renderer.rendertype.RenderType;
^///?} else {
import net.minecraft.client.renderer.RenderType;
//?}
import java.util.List;
*///? }

/**
 * @author Wagyourtail
 * @since 1.0.5
 */
@SuppressWarnings("unused")
public class Item implements RenderElement, Alignable<Item> {

    private static final int DEFAULT_ITEM_SIZE = 16;
    private static final float FLAT_ITEM_DEPTH_SCALE = 0.001f;
    private static final Minecraft mc = Minecraft.getInstance();

    //? if >=26.1 {
    /*private static final float OVERLAY_TEXT_Z_OFFSET = 0.001f;
    private static SubmitNodeStorage directItemStorage;
    private static final ItemFeatureRenderer DIRECT_ITEM_RENDERER = new ItemFeatureRenderer();
    *///? }

    @Nullable
    public IDraw2D<?> parent;
    public ItemStack item;
    public String ovText;
    public boolean overlay;
    public double scale;
    public float rotation;
    public boolean rotateCenter;
    public int x;
    public int y;
    public int zIndex;

    @DocletReplaceParams("x: int, y: int, zIndex: int, id: CanOmitNamespace<ItemId>, overlay: boolean, scale: double, rotation: float")
    public Item(int x, int y, int zIndex, String id, boolean overlay, double scale, float rotation) {
        this(x, y, zIndex, new ItemStackHelper(id, 1), overlay, scale, rotation);
    }

    public Item(int x, int y, int zIndex, ItemStackHelper i, boolean overlay, double scale, float rotation) {
        this(x, y, zIndex, i, overlay, scale, rotation, null);
    }

    public Item(int x, int y, int zIndex, ItemStackHelper itemStack, boolean overlay, double scale, float rotation, String ovText) {
        this.x = x;
        this.y = y;
        this.item = itemStack.getRaw();
        this.overlay = overlay;
        this.scale = scale;
        this.rotation = rotation;
        this.zIndex = zIndex;
        this.ovText = ovText;
    }

    /**
     * @param i
     * @return
     * @since 1.0.5 [citation needed]
     */
    public Item setItem(ItemStackHelper i) {
        if (i != null) {
            this.item = i.getRaw();
        } else {
            this.item = null;
        }
        return this;
    }

    /**
     * @param id
     * @param count
     * @return
     * @since 1.0.5 [citation needed]
     */
    @DocletReplaceParams("id: CanOmitNamespace<ItemId>, count: int")
    public Item setItem(String id, int count) {
        this.item = new ItemStack(BuiltInRegistries.ITEM.getValue(RegistryHelper.parseIdentifier(id)), count);
        return this;
    }

    /**
     * @return
     * @since 1.0.5 [citation needed]
     */
    public ItemStackHelper getItem() {
        return new ItemStackHelper(item);
    }

    /**
     * @param x the new x position of this element
     * @return self for chaining.
     * @since 1.8.4
     */
    public Item setX(int x) {
        this.x = x;
        return this;
    }

    /**
     * @return the x position of this element.
     * @since 1.8.4
     */
    public int getX() {
        return x;
    }

    /**
     * @param y the new y position of this element
     * @return self for chaining.
     * @since 1.8.4
     */
    public Item setY(int y) {
        this.y = y;
        return this;
    }

    /**
     * @return the y position of this element.
     * @since 1.8.4
     */
    public int getY() {
        return y;
    }

    /**
     * @param x
     * @param y
     * @return
     * @since 1.0.5
     */
    public Item setPos(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /**
     * @param scale
     * @return
     * @throws IllegalArgumentException
     * @since 1.2.6
     */
    public Item setScale(double scale) throws IllegalArgumentException {
        if (scale == 0) {
            throw new IllegalArgumentException("Scale can't be 0");
        }
        this.scale = scale;
        return this;
    }

    /**
     * @return the scale of this item.
     * @since 1.8.4
     */
    public double getScale() {
        return scale;
    }

    /**
     * @param rotation
     * @return
     * @since 1.2.6
     */
    public Item setRotation(double rotation) {
        this.rotation = Mth.wrapDegrees((float) rotation);
        return this;
    }

    /**
     * @return the rotation of this item.
     * @since 1.8.4
     */
    public float getRotation() {
        return rotation;
    }

    /**
     * @param rotateCenter whether the item should be rotated around its center
     * @return self for chaining.
     * @since 1.8.4
     */
    public Item setRotateCenter(boolean rotateCenter) {
        this.rotateCenter = rotateCenter;
        return this;
    }

    /**
     * @return {@code true} if this item should be rotated around its center, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isRotatingCenter() {
        return rotateCenter;
    }

    /**
     * @param overlay
     * @return
     * @since 1.2.0
     */
    public Item setOverlay(boolean overlay) {
        this.overlay = overlay;
        return this;
    }

    /**
     * @return {@code true}, if the overlay, i.e. the durability bar, and the overlay text or
     * item count should be shown, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean shouldShowOverlay() {
        return overlay;
    }

    /**
     * @param ovText
     * @return
     * @since 1.2.0
     */
    public Item setOverlayText(String ovText) {
        this.ovText = ovText;
        return this;
    }

    /**
     * @return the overlay text of this item.
     * @since 1.8.4
     */
    public String getOverlayText() {
        return ovText;
    }

    /**
     * @param zIndex the new z-index of this item
     * @return self for chaining.
     * @since 1.8.4
     */
    public Item setZIndex(int zIndex) {
        this.zIndex = zIndex;
        return this;
    }

    @Override
    public int getZIndex() {
        return zIndex;
    }

    @Override
    //? if >=26.1 {
    /*public void render(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
    *///?} else {
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
    //?}
        render(drawContext, mouseX, mouseY, delta, false);
    }

    @Override
    @DocletIgnore
    //? if >=26.1 {
    /*public void render3D(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
    *///?} else {
    public void render3D(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
    //?}
        render(drawContext, mouseX, mouseY, delta, true);
    }

    @DocletIgnore
    //? if >=26.1 {
    /*public void render(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta, boolean is3dRender) {
    *///?} else {
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta, boolean is3dRender) {
    //?}
        if (item == null) {
            return;
        }

        //? if >1.21.5 {
        Matrix3x2fStack matrices = drawContext.pose();
        matrices.pushMatrix();
        //?} else {
        /*var matrices = drawContext.pose();
        matrices.pushPose();
        *///?}

        // TODO: This looks like it wasn't properly updated between 1.21.5 and 1.21.7, why does this pass matrices?
        setupMatrix(matrices, x, y, (float) scale, rotation, DEFAULT_ITEM_SIZE, DEFAULT_ITEM_SIZE, rotateCenter);
        Font textRenderer = Minecraft.getInstance().font;
        if (is3dRender) {
            // The item model has a z offset (100, and 200 for its decorations) that must
            // be collapsed so the item lies in the surface plane: translate by
            // FLAT_ITEM_DEPTH_SCALE * 100, render, then undo. Keep the scale large enough
            // to avoid z-fighting for deep items like anvils. The 1.21.5+ Matrix3x2fStack
            // is 2D and has no z axis, so those items are already flat.
            //? if >1.21.5 {
            //? if >=26.1 {
            /*drawContext.item(item, x, y);
            *///? } else {
            drawContext.renderItem(item, x, y);
            //? }
            //?} else {
            /*matrices.translate(0, 0, -0.1f);
            matrices.scale(1, 1, FLAT_ITEM_DEPTH_SCALE);
            drawContext.renderItem(item, x, y);
            matrices.scale(1, 1, 1 / FLAT_ITEM_DEPTH_SCALE);
            *///?}
        } else {
            //? if >=26.1 {
            /*drawContext.item(item, x, y);
            *///?} else {
            drawContext.renderItem(item, x, y);
            //?}
        }
        if (overlay) {
            // The decorations carry a z offset of 200; the PoseStack path pushes them in
            // front of the item. The 1.21.5+ 2D stack has no z axis, so nothing is needed.
            //? if <=1.21.5 {
            /*if (is3dRender) {
                matrices.translate(0, 0, -199.5);
            }
            *///?}
            //? if >=26.1 {
            /*drawContext.itemDecorations(mc.font, item, x, y, ovText);
            *///?} else {
            drawContext.renderItemDecorations(mc.font, item, x, y, ovText);
            //?}
        }

        //? if >1.21.5 {
        matrices.popMatrix();
        //?} else {
        /*matrices.popPose();
        *///?}
    }

    // Draws the item into the surface's world-space buffer source. Items cannot be
    // gizmos, so a surface draws them directly. The transform mirrors the 2D path:
    // cancel the surface's Y-flip, map the unit model to a 16px slot, flatten depth.
    @DocletIgnore
    @Override
    public void render3D(PoseStack matrixStack, MultiBufferSource consumers, int light, boolean seeThrough, float delta) {
        if (item == null || item.isEmpty()) {
            return;
        }
        matrixStack.pushPose();
        matrixStack.translate(x, y, 0);
        matrixStack.scale((float) scale, (float) scale, 1);
        if (rotateCenter) {
            matrixStack.translate(DEFAULT_ITEM_SIZE / 2d, DEFAULT_ITEM_SIZE / 2d, 0);
        }
        matrixStack.mulPose(new Quaternionf().rotateLocalZ((float) Math.toRadians(rotation)));
        if (rotateCenter) {
            matrixStack.translate(-DEFAULT_ITEM_SIZE / 2d, -DEFAULT_ITEM_SIZE / 2d, 0);
        }

        //? if >=26.1 {
        /*if (consumers instanceof MultiBufferSource.BufferSource bufferSource) {
            ItemStackRenderState renderState = new ItemStackRenderState();
            mc.getItemModelResolver().updateForTopItem(renderState, item, ItemDisplayContext.GUI, mc.level, mc.player, 0);

            SubmitNodeStorage storage = directItemStorage;
            if (storage == null) {
                storage = directItemStorage = new SubmitNodeStorage();
            } else {
                storage.clear();
            }

            matrixStack.pushPose();
            matrixStack.translate(DEFAULT_ITEM_SIZE / 2d, DEFAULT_ITEM_SIZE / 2d, 0);
            matrixStack.scale(1, -1, 1);
            matrixStack.scale(DEFAULT_ITEM_SIZE, DEFAULT_ITEM_SIZE, DEFAULT_ITEM_SIZE);
            matrixStack.scale(1, 1, FLAT_ITEM_DEPTH_SCALE);
            renderState.submit(matrixStack, storage, light, OverlayTexture.NO_OVERLAY, 0);
            matrixStack.popPose();

            SubmitNodeCollection collection = storage.order(0);
            // outlineColor is 0 above, so the feature renderer never dereferences this.
            DIRECT_ITEM_RENDERER.renderSolid(collection, bufferSource, null);
            DIRECT_ITEM_RENDERER.renderTranslucent(collection, bufferSource, null);
            // Item render types are fixed buffers that only flush on endBatch; flush now
            // so the overlay text draws on top of the item instead of behind it.
            bufferSource.endBatch();
        }
        *///? } else if >=1.21.10 {
        /*ItemStackRenderState renderState = new ItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(renderState, item, ItemDisplayContext.GUI, mc.level, mc.player, 0);
        MixinItemRenderer itemRenderer = (MixinItemRenderer) mc.getItemRenderer();
        MixinItemStackRenderState stateAccessor = (MixinItemStackRenderState) (Object) renderState;
        ItemStackRenderState.LayerRenderState[] layers = stateAccessor.jsmacros$getLayers();
        int layerCount = stateAccessor.jsmacros$getActiveLayerCount();
        for (int i = 0; i < layerCount; i++) {
            MixinItemStackRenderStateLayer layerAccessor = (MixinItemStackRenderStateLayer) (Object) layers[i];
            matrixStack.pushPose();
            matrixStack.translate(DEFAULT_ITEM_SIZE / 2d, DEFAULT_ITEM_SIZE / 2d, 0);
            matrixStack.scale(1, -1, 1);
            matrixStack.scale(DEFAULT_ITEM_SIZE, DEFAULT_ITEM_SIZE, DEFAULT_ITEM_SIZE);
            matrixStack.scale(1, 1, FLAT_ITEM_DEPTH_SCALE);
            ItemTransform transform = layerAccessor.jsmacros$getTransform();
            if (transform != null) {
                transform.apply(false, matrixStack.last());
            }
            RenderType renderType = layerAccessor.jsmacros$getRenderType();
            List<BakedQuad> quads = layerAccessor.jsmacros$getQuads();
            if (renderType != null && quads != null && !quads.isEmpty()) {
                ItemStackRenderState.FoilType foilType = layerAccessor.jsmacros$getFoilType();
                itemRenderer.jsmacros$renderItem(ItemDisplayContext.GUI, matrixStack, consumers, light, OverlayTexture.NO_OVERLAY,
                        layerAccessor.jsmacros$getTintLayers(), quads, renderType,
                        foilType != null ? foilType : ItemStackRenderState.FoilType.NONE);
            }
            matrixStack.popPose();
        }
        *///? } else {
        matrixStack.pushPose();
        matrixStack.translate(DEFAULT_ITEM_SIZE / 2d, DEFAULT_ITEM_SIZE / 2d, 0);
        matrixStack.scale(1, -1, 1);
        matrixStack.scale(DEFAULT_ITEM_SIZE, DEFAULT_ITEM_SIZE, DEFAULT_ITEM_SIZE);
        matrixStack.scale(1, 1, FLAT_ITEM_DEPTH_SCALE);
        mc.getItemRenderer().renderStatic(item, ItemDisplayContext.GUI, light, OverlayTexture.NO_OVERLAY, matrixStack, consumers, null, 0);
        matrixStack.popPose();
        //? }

        if (overlay) {
            String text = ovText != null ? ovText : (item.getCount() > 1 ? String.valueOf(item.getCount()) : null);
            if (text != null) {
                float tx = DEFAULT_ITEM_SIZE + 1 - mc.font.width(text);
                float ty = 9;
                matrixStack.pushPose();
                //? if >=26.1 {
                /*matrixStack.translate(0, 0, OVERLAY_TEXT_Z_OFFSET);
                *///? }
                mc.font.drawInBatch(text, tx, ty, 0xFFFFFFFF, true, matrixStack.last().pose(), consumers, Font.DisplayMode.POLYGON_OFFSET, 0, light);
                matrixStack.popPose();
            }
        }
        matrixStack.popPose();
    }

    public Item setParent(IDraw2D<?> parent) {
        this.parent = parent;
        return this;
    }

    @Override
    public int getScaledWidth() {
        return (int) (scale * DEFAULT_ITEM_SIZE);
    }

    @Override
    public int getParentWidth() {
        return parent != null ? parent.getWidth() : mc.getWindow().getGuiScaledWidth();
    }

    @Override
    public int getScaledHeight() {
        return (int) (scale * DEFAULT_ITEM_SIZE);
    }

    @Override
    public int getParentHeight() {
        return parent != null ? parent.getHeight() : mc.getWindow().getGuiScaledHeight();
    }

    @Override
    public int getScaledLeft() {
        return x;
    }

    @Override
    public int getScaledTop() {
        return y;
    }

    @Override
    public Item moveTo(int x, int y) {
        return setPos(x, y);
    }

    /**
     * @author Etheradon
     * @since 1.8.4
     */
    public static final class Builder extends RenderElementBuilder<Item> implements Alignable<Builder> {
        private int x = 0;
        private int y = 0;
        private ItemStackHelper itemStack = new ItemStackHelper(ItemStack.EMPTY);
        private String ovText = "";
        private boolean overlay = false;
        private double scale = 1;
        private float rotation = 0;
        private boolean rotateCenter = true;
        private int zIndex = 0;

        public Builder(IDraw2D<?> draw2D) {
            super(draw2D);
        }

        /**
         * @param x the x position of the item
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder x(int x) {
            this.x = x;
            return this;
        }

        /**
         * @return the x position of the item.
         * @since 1.8.4
         */
        public int getX() {
            return x;
        }

        /**
         * @param y the y position of the item
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder y(int y) {
            this.y = y;
            return this;
        }

        /**
         * @return the y position of the item.
         * @since 1.8.4
         */
        public int getY() {
            return y;
        }

        /**
         * @param x the x position of the item
         * @param y the y position of the item
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        /**
         * @param item the item to draw
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder item(ItemStackHelper item) {
            if (item != null) {
                this.itemStack = item.copy();
            }
            return this;
        }

        /**
         * @param id the id of the item to draw
         * @return self for chaining.
         * @since 1.8.4
         */
        @DocletReplaceParams("id: CanOmitNamespace<ItemId>")
        public Builder item(String id) {
            this.itemStack = new ItemStackHelper(BuiltInRegistries.ITEM.getValue(RegistryHelper.parseIdentifier(id))
                    .getDefaultInstance());
            return this;
        }

        /**
         * @param id    the id of the item to draw
         * @param count the stack size
         * @return self for chaining.
         * @since 1.8.4
         */
        @DocletReplaceParams("id: ItemId, count: int")
        public Builder item(String id, int count) {
            this.itemStack = new ItemStackHelper(id, count);
            return this;
        }

        /**
         * @return the item to be drawn.
         * @since 1.8.4
         */
        public ItemStackHelper getItem() {
            return itemStack.copy();
        }

        /**
         * This also sets the overlay to be shown.
         *
         * @param overlayText the overlay text
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder overlayText(String overlayText) {
            this.ovText = overlayText;
            this.overlay = true;
            return this;
        }

        /**
         * @return the overlay text.
         * @since 1.8.4
         */
        public String getOverlayText() {
            return ovText;
        }

        /**
         * @param visible whether the overlay should be visible or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder overlayVisible(boolean visible) {
            this.overlay = visible;
            return this;
        }

        /**
         * @return {@code true} if the overlay should be visible, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isOverlayVisible() {
            return overlay;
        }

        /**
         * @param scale the scale of the item
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder scale(double scale) {
            if (scale <= 0) {
                throw new IllegalArgumentException("Scale must be greater than 0");
            }
            this.scale = scale;
            return this;
        }

        /**
         * @return the scale of the item.
         * @since 1.8.4
         */
        public double getScale() {
            return scale;
        }

        /**
         * @param rotation the rotation (clockwise) of the item in degrees
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder rotation(double rotation) {
            this.rotation = (float) rotation;
            return this;
        }

        /**
         * @return the rotation (clockwise) of the item in degrees.
         * @since 1.8.4
         */
        public float getRotation() {
            return rotation;
        }

        /**
         * @param rotateCenter whether the item should be rotated around its center
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder rotateCenter(boolean rotateCenter) {
            this.rotateCenter = rotateCenter;
            return this;
        }

        /**
         * @return {@code true} if this item should be rotated around its center, {@code false}
         * otherwise.
         * @since 1.8.4
         */
        public boolean isRotatingCenter() {
            return rotateCenter;
        }

        /**
         * @param zIndex the z-index of the item
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder zIndex(int zIndex) {
            this.zIndex = zIndex;
            return this;
        }

        /**
         * @return the z-index of the item.
         * @since 1.8.4
         */
        public int getZIndex() {
            return zIndex;
        }

        @Override
        protected Item createElement() {
            return new Item(x, y, zIndex, itemStack, overlay, scale, rotation, ovText).setRotateCenter(rotateCenter)
                    .setParent(parent);
        }

        @Override
        public int getScaledWidth() {
            return (int) (DEFAULT_ITEM_SIZE * scale);
        }

        @Override
        public int getParentWidth() {
            return parent.getWidth();
        }

        @Override
        public int getScaledHeight() {
            return (int) (DEFAULT_ITEM_SIZE * scale);
        }

        @Override
        public int getParentHeight() {
            return parent.getHeight();
        }

        @Override
        public int getScaledLeft() {
            return x;
        }

        @Override
        public int getScaledTop() {
            return y;
        }

        @Override
        public Builder moveTo(int x, int y) {
            return pos(x, y);
        }

    }

}
