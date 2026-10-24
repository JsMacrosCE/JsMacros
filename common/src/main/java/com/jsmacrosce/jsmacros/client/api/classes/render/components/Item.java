package com.jsmacrosce.jsmacros.client.api.classes.render.components;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.render.RenderSink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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

//? if >=26.2 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;
*///? } else if >=26.1 {
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
  * Draws an item stack using its model in a 16-by-16 GUI-pixel box; scale changes its
  * reported dimensions, and rotation affects drawing. The element's color does not tint
  * the icon. Optional game decorations (durability bar, cooldown, and count or custom
  * text) are disabled by default; a null stack draws nothing.
  * <p>
  * The world-space surface path flattens the item's model depth. In the GUI-context path,
  * versions newer than Minecraft 1.21.5 use the same item-drawing call for {@code render3D}
  * as for {@code render}; the older 3D-matrix path also adjusts model depth.
  * <p>
 * <pre>
 * const draw = Hud.createDraw2D();
 * draw.itemBuilder().item("minecraft:diamond").pos(10, 10).buildAndAdd();
 * draw.register();
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.0.5
 */
@SuppressWarnings("unused")
@DocletCategory("Rendering/Graphics")
public class Item implements RenderElement, Alignable<Item> {

    private static final int DEFAULT_ITEM_SIZE = 16;
    private static final float FLAT_ITEM_DEPTH_SCALE = 0.001f;
    private static final float OVERLAY_TEXT_Z_OFFSET = 0.001f;
    private static final Minecraft mc = Minecraft.getInstance();

    //? if >=26.1 <26.2 {
    /*private static SubmitNodeStorage directItemStorage;
    private static final ItemFeatureRenderer DIRECT_ITEM_RENDERER = new ItemFeatureRenderer();
    *///? }

    /**
    * the overlay this icon belongs to, or {@code null} when unbound. Parent dimensions
    * come from this overlay and fall back to the window when it is null.
    */
    @Nullable
    public IDraw2D<?> parent;
    /**
    * the stack whose icon is drawn, or {@code null} for no icon.
    */
    public ItemStack item;
    /**
    * the custom text drawn when the overlay is on, or {@code null} to use the stack
    * count. Builders initialize this to an empty string, which requests an empty label.
    */
    public String ovText;
    /**
    * whether the game's decorations (bar, cooldown, and count or custom text) are drawn.
    * Defaults to the constructor argument, or {@code false} in a builder.
    */
    public boolean overlay;
    /**
    * the scale applied to the fixed 16-by-16 icon; builders default to {@code 1}.
    * Negative values are accepted on the element but rejected by its builder.
    */
    public double scale;
    /**
    * the rotation in degrees. Constructors and setters normalize finite angles to
    * {@code [-180, 180)}; direct field writes do not.
    */
    public float rotation;
    /**
    * whether rotation is about the icon's center; defaults to {@code false} for direct
    * construction and {@code true} for builder-created icons.
    */
    public boolean rotateCenter;
    /**
    * the x position of this icon, which is the top left of its 16 by 16 box.
    */
    public int x;
    /**
    * the y coordinate of the icon's top-left position.
    */
    public int y;
    /**
    * The overlay draw-order value for this icon; lower values are drawn first.
    */
    public int zIndex;

    /**
    * Constructs an icon using the default instance of the identified item.
    * Bare names use the {@code minecraft} namespace; an unknown item id resolves to
    * the registry's default item (air in Minecraft 1.21.8). Finite rotations are
    * normalized to {@code [-180, 180)}.
    *
    * @param x        the x position
    * @param y        the y position
    * @param zIndex   the z-index against the other elements on the overlay
    * @param id       the item to draw, with the namespace optional
    * @param overlay  whether to draw the game's decorations over the icon
    * @param scale    the scale to apply
    * @param rotation the rotation in degrees, normalized to {@code [-180, 180)}
    */
    @DocletReplaceParams("x: int, y: int, zIndex: int, id: CanOmitNamespace<ItemId>, overlay: boolean, scale: double, rotation: float")
    public Item(int x, int y, int zIndex, String id, boolean overlay, double scale, float rotation) {
        this(x, y, zIndex, new ItemStackHelper(id, 1), overlay, scale, rotation);
    }

    /**
    * Constructs an icon from a stack, leaving overlay text {@code null} so the stack
    * count is shown when greater than one if the overlay is enabled.
    *
    * @param x        the x position
    * @param y        the y position
    * @param zIndex   the z-index against the other elements on the overlay
    * @param i        the stack whose icon to draw
    * @param overlay  whether to draw the game's decorations over the icon
    * @param scale    the scale to apply
    * @param rotation the rotation in degrees, normalized to {@code [-180, 180)}
    */
    public Item(int x, int y, int zIndex, ItemStackHelper i, boolean overlay, double scale, float rotation) {
        this(x, y, zIndex, i, overlay, scale, rotation, null);
    }

    /**
    * Constructs an icon from a stack with optional overlay text, which replaces rather
    * than supplements the stack count. A {@code null} text uses the count instead.
    *
    * @param x         the x position
    * @param y         the y position
    * @param zIndex    the z-index against the other elements on the overlay
    * @param itemStack the stack whose icon to draw
    * @param overlay   whether to draw the game's decorations over the icon
    * @param scale     the scale to apply
    * @param rotation  the rotation in degrees, normalized to {@code [-180, 180)}
    * @param ovText    the text to draw over the icon, or {@code null} for the count
    */
    public Item(int x, int y, int zIndex, ItemStackHelper itemStack, boolean overlay, double scale, float rotation, String ovText) {
        this.x = x;
        this.y = y;
        this.item = itemStack.getRaw();
        this.overlay = overlay;
        this.scale = scale;
        this.rotation = Mth.wrapDegrees(rotation);
        this.zIndex = zIndex;
        this.ovText = ovText;
    }

    /**
    * Sets the icon's stack, or clears the icon when {@code i} is {@code null}. The
    * helper's underlying stack is used without copying.
    *
    * @param i the stack to draw, or {@code null} for nothing
    * @return self for chaining.
    * @since 1.0.5
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
    * Replaces the icon's stack with a newly constructed stack for the given id and count.
    * Bare names use the {@code minecraft} namespace; unknown ids resolve to the
    * registry's default item (air in Minecraft 1.21.8).
    *
    * @param id    the item to draw, with the namespace optional
    * @param count the new stack count
    * @return self for chaining.
    * @since 1.0.5
    */
    @DocletReplaceParams("id: CanOmitNamespace<ItemId>, count: int")
    public Item setItem(String id, int count) {
        this.item = new ItemStack(BuiltInRegistries.ITEM.getValue(RegistryHelper.parseIdentifier(id)), count);
        return this;
    }

    /**
    * Returns the icon's stack wrapped as a helper.
    *
    * @return the stack being drawn, as a helper
    * @since 1.2.0
    */
    public ItemStackHelper getItem() {
        return new ItemStackHelper(item);
    }

    /**
    * Sets the icon's x position without changing its y position.
    *
    * @param x the new x position of this element
    * @return self for chaining.
    * @since 1.8.4
    */
    public Item setX(int x) {
        this.x = x;
        return this;
    }

    /**
    * Returns the x coordinate of the icon's box; scaling does not change this position.
    *
    * @return the x position of this element.
    * @since 1.8.4
    */
    public int getX() {
        return x;
    }

    /**
    * Sets the icon's y position without changing its x position.
    *
    * @param y the new y position of this element
    * @return self for chaining.
    * @since 1.8.4
    */
    public Item setY(int y) {
        this.y = y;
        return this;
    }

    /**
    * Returns the icon's y coordinate; scaling does not change its position.
    *
    * @return the y position of this element.
    * @since 1.8.4
    */
    public int getY() {
        return y;
    }

    /**
    * Sets both coordinates without changing the stack.
    *
    * @param x the new x position
    * @param y the new y position
    * @return self for chaining.
    * @since 1.0.5
    */
    public Item setPos(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /**
    * Sets the icon scale. The base size is 16 by 16; zero is rejected, but negative
    * values are accepted here (the builder rejects them).
    *
    * @param scale the scale to apply
    * @return self for chaining.
    * @throws IllegalArgumentException if the scale is zero
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
    * Returns the scale applied to the icon's 16-by-16 base size.
    *
    * @return the scale of this item.
    * @since 1.8.4
    */
    public double getScale() {
        return scale;
    }

    /**
    * Sets the rotation in degrees, wrapping finite float-converted angles to
    * {@code [-180, 180)}; for example, {@code 450} becomes {@code 90}. Constructors
    * apply the same wrapping.
    *
    * @param rotation the angle in degrees
    * @return self for chaining.
    * @since 1.2.6
    */
    public Item setRotation(double rotation) {
        this.rotation = Mth.wrapDegrees((float) rotation);
        return this;
    }

    /**
    * Returns the stored rotation in degrees. Constructors and setters normalize it;
    * direct writes to the public field do not.
    *
    * @return the rotation of this item.
    * @since 1.8.4
    */
    public float getRotation() {
        return rotation;
    }

    /**
    * Sets whether rotation is about the center of the 16-by-16 icon box rather than
    * its top-left position.
    *
    * @param rotateCenter whether the item should be rotated around its center
    * @return self for chaining.
    * @since 1.8.4
    */
    public Item setRotateCenter(boolean rotateCenter) {
        this.rotateCenter = rotateCenter;
        return this;
    }

    /**
    * Returns whether rotation is about the icon's center. Directly constructed icons
    * default to {@code false}; builder-created icons default to {@code true}.
    *
    * @return {@code true} if this item should be rotated around its center, {@code false}
    * otherwise.
    * @since 1.8.4
    */
    public boolean isRotatingCenter() {
        return rotateCenter;
    }

    /**
    * Enables or disables item decorations. In 2D rendering these include the game's
    * durability bar, cooldown overlay, and stack count or custom text; the custom 3D
    * surface renderer draws the bar and count or text, but not the cooldown overlay.
    * Builder-created icons start with empty overlay text, so set text separately if a
    * label is wanted.
    *
    * @param overlay whether to draw the decorations
    * @return self for chaining.
    * @since 1.2.0
    */
    public Item setOverlay(boolean overlay) {
        this.overlay = overlay;
        return this;
    }

    /**
    * Returns whether the game's item decorations are enabled. This flag alone does not
    * guarantee visible decoration; for example, a builder's default empty text draws
    * no count label.
    *
    * @return {@code true} if decorations are enabled; {@code false} otherwise.
    * @since 1.8.4
    */
    public boolean shouldShowOverlay() {
        return overlay;
    }

    /**
    * Sets the text used for the overlay label; this does not enable the overlay. When
    * enabled, a non-null value replaces the stack count.
    *
    * @param ovText the text to draw over the icon
    * @return self for chaining.
    * @since 1.2.0
    */
    public Item setOverlayText(String ovText) {
        this.ovText = ovText;
        return this;
    }

    /**
    * Returns the custom overlay label, or {@code null} when the stack count is used.
    *
    * @return the overlay text of this item.
    * @since 1.8.4
    */
    public String getOverlayText() {
        return ovText;
    }

    /**
    * Sets this element's z-index; lower values are drawn first.
    *
    * @param zIndex the new z-index for this item
    * @return self for chaining.
    * @since 1.8.4
    */
    public Item setZIndex(int zIndex) {
        this.zIndex = zIndex;
        return this;
    }

    /**
    * Returns this element's z-index; lower values are drawn first.
    *
    * @return the z-index of this element
    */
    @Override
    public int getZIndex() {
        return zIndex;
    }

    /**
    * Renders this icon using its position, scale, and rotation. The overlay calls this
    * method; a {@code null} item draws nothing. Mouse coordinates and frame delta are
    * unused.
    *
    * @param drawContext the graphics context to draw into
    * @param mouseX      the mouse x, not used by this element
    * @param mouseY      the mouse y, not used by this element
    * @param delta       the frame delta, not used by this element
    */
    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        render(drawContext, mouseX, mouseY, delta, false);
    }

    @Override
    @DocletIgnore
    public void render3D(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        render(drawContext, mouseX, mouseY, delta, true);
    }

    private void renderDamageBar3D(PoseStack matrixStack, RenderSink sink, int light, boolean seeThrough, float delta) {
        if (!item.isDamageableItem()) {
            return;
        }

        int maxDamage = item.getMaxDamage();
        if (maxDamage <= 0) {
            return;
        }

        int damage = item.getDamageValue();
        if (damage <= 0) {
            return;
        }

        float durability = Math.max(0.0F, (float) (maxDamage - damage) / (float) maxDamage);
        int barWidth = Mth.clamp(Math.round(13.0F * durability), 0, 13);
        int barColor = 0xFF000000 | Mth.hsvToRgb(durability / 3.0F, 1.0F, 1.0F);

        matrixStack.pushPose();
        //? if >=26.1 {
        /*matrixStack.translate(0, 0, OVERLAY_TEXT_Z_OFFSET);
        *///? }
        new Rect(2, 13, 15, 15, 0xFF000000, 0, 0).render3D(matrixStack, sink, light, seeThrough, delta);
        if (barWidth > 0) {
            new Rect(2, 13, 2 + barWidth, 14, barColor, 0, 0).render3D(matrixStack, sink, light, seeThrough, delta);
        }
        matrixStack.popPose();
    }

    @DocletIgnore
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta, boolean is3dRender) {
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
    public void render3D(PoseStack matrixStack, RenderSink sink, int light, boolean seeThrough, float delta) {
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

        //? if >=26.2 {
        /*ItemStackRenderState renderState = new ItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(renderState, item, ItemDisplayContext.GUI, mc.level, mc.player, 0);

        matrixStack.pushPose();
        matrixStack.translate(DEFAULT_ITEM_SIZE / 2d, DEFAULT_ITEM_SIZE / 2d, 0);
        matrixStack.scale(1, -1, 1);
        matrixStack.scale(DEFAULT_ITEM_SIZE, DEFAULT_ITEM_SIZE, DEFAULT_ITEM_SIZE);
        matrixStack.scale(1, 1, FLAT_ITEM_DEPTH_SCALE);
        sink.submitItem(matrixStack, renderState, light);
        matrixStack.popPose();
        *///? } else if >=26.1 {
        /*MultiBufferSource.BufferSource bufferSource = sink.getSource() instanceof MultiBufferSource.BufferSource source ? source : null;
        if (bufferSource != null) {
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
                itemRenderer.jsmacros$renderItem(ItemDisplayContext.GUI, matrixStack, sink.getSource(), light, OverlayTexture.NO_OVERLAY,
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
        mc.getItemRenderer().renderStatic(item, ItemDisplayContext.GUI, light, OverlayTexture.NO_OVERLAY, matrixStack, sink.getSource(), null, 0);
        matrixStack.popPose();
        //? }

        if (overlay) {
            renderDamageBar3D(matrixStack, sink, light, seeThrough, delta);
            String text = ovText != null ? ovText : (item.getCount() > 1 ? String.valueOf(item.getCount()) : null);
            if (text != null) {
                float tx = DEFAULT_ITEM_SIZE + 1 - mc.font.width(text);
                float ty = 9;
                matrixStack.pushPose();
                //? if >=26.1 {
                /*matrixStack.translate(0, 0, OVERLAY_TEXT_Z_OFFSET);
                *///? }
                //? if >=26.2 {
                /*sink.drawText(matrixStack, net.minecraft.network.chat.Component.literal(text).getVisualOrderText(),
                        tx, ty, 0xFFFFFFFF, true, Font.DisplayMode.POLYGON_OFFSET, light);
                *///?} else {
                mc.font.drawInBatch(text, tx, ty, 0xFFFFFFFF, true, matrixStack.last().pose(), sink.getSource(), Font.DisplayMode.POLYGON_OFFSET, 0, light);
                //?}
                matrixStack.popPose();
            }
        }
        matrixStack.popPose();
    }

    /**
    * Sets the overlay parent used for parent-relative alignment and dimensions. A
    * {@code null} parent makes dimensions fall back to the window.
    *
    * @param parent the overlay this element is on, or {@code null} for the window
    * @return self for chaining.
    */
    public Item setParent(IDraw2D<?> parent) {
        this.parent = parent;
        return this;
    }

    /**
    * Returns the icon's scaled width, computed as {@code (int) (scale * 16)} (truncated
    * toward zero).
    *
    * @return the scaled width of this element
    */
    @Override
    public int getScaledWidth() {
        return (int) (scale * DEFAULT_ITEM_SIZE);
    }

    /**
    * Returns the parent overlay's width, or the window's GUI-scaled width when no parent
    * is set.
    *
    * @return the width of the parent
    */
    @Override
    public int getParentWidth() {
        return parent != null ? parent.getWidth() : mc.getWindow().getGuiScaledWidth();
    }

    /**
    * Returns the icon's scaled height, computed as {@code (int) (scale * 16)} (truncated
    * toward zero).
    *
    * @return the scaled height of this element
    */
    @Override
    public int getScaledHeight() {
        return (int) (scale * DEFAULT_ITEM_SIZE);
    }

    /**
    * Returns the parent overlay's height, or the window's GUI-scaled height when no
    * parent is set.
    *
    * @return the height of the parent
    */
    @Override
    public int getParentHeight() {
        return parent != null ? parent.getHeight() : mc.getWindow().getGuiScaledHeight();
    }

    /**
    * Returns the icon's x position; scaling changes its rendered size, not this position.
    *
    * @return the icon's x position
    */
    @Override
    public int getScaledLeft() {
        return x;
    }

    /**
    * Returns the icon's y position; scaling changes its rendered size, not this position.
    *
    * @return the icon's y position
    */
    @Override
    public int getScaledTop() {
        return y;
    }

    /**
    * Sets this icon's position. Scaling and rotation settings are unchanged.
    *
    * @param x the new x position
    * @param y the new y position
    * @return self for chaining.
    */
    @Override
    public Item moveTo(int x, int y) {
        return setPos(x, y);
    }

    /**
    * Builds item icons for an overlay. Set the stack, position, scale, rotation, and
    * optional count overlay before building; the default stack is empty.
    *
    * @author Etheradon
    * @since 1.8.4
    */
    @DocletCategory("Rendering/Graphics")
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

        /**
        * Creates a builder associated with the supplied overlay.
        *
        * @param draw2D the overlay the icons will be added to
        */
        public Builder(IDraw2D<?> draw2D) {
            super(draw2D);
        }

        /**
        * Sets the icon's x position without changing its y position.
        *
        * @param x the x position of the item
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder x(int x) {
            this.x = x;
            return this;
        }

        /**
        * Returns the x position configured for this builder (zero by default).
        *
        * @return the x position of the item.
        * @since 1.8.4
        */
        public int getX() {
            return x;
        }

        /**
        * Sets the icon's y position without changing its x position.
        *
        * @param y the y position of the item
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder y(int y) {
            this.y = y;
            return this;
        }

        /**
        * Returns the y position configured for this builder (zero by default).
        *
        * @return the y position of the item.
        * @since 1.8.4
        */
        public int getY() {
            return y;
        }

        /**
        * Sets both icon coordinates without changing its stack.
        *
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
        * Sets the stack to draw, copying it into the builder. A {@code null} helper leaves
        * the builder's current stack unchanged.
        *
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
        * Sets the item to draw from its id, using the item's default stack. The namespace
        * may be omitted; an unknown id resolves to the registry's default item.
        *
        * @param id the id of the item to draw, with the namespace optional
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
        * Sets the item stack from its id and count. Unlike the one-argument overload,
        * this overload's script typing requires a namespace, although runtime parsing
        * accepts a bare name. Unknown ids resolve to the registry's default item. The
        * count does not appear as overlay text by default: the builder's empty overlay
        * text differs from {@code null}, which requests the stack count.
        *
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
        * Returns a copy of the stack the builder will use.
        *
        * @return the item to be drawn.
        * @since 1.8.4
        */
        public ItemStackHelper getItem() {
            return itemStack.copy();
        }

        /**
        * Sets the overlay text and enables the overlay.
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
        * Returns the builder's overlay text, initially an empty string. Unlike
        * {@code null}, an empty string requests an empty label rather than the stack count.
        *
        * @return the overlay text.
        * @since 1.8.4
        */
        public String getOverlayText() {
            return ovText;
        }

        /**
        * Sets whether the built icon draws the game's overlay decorations. This does not
        * change the overlay text; the builder's default empty string produces an empty
        * label rather than the stack count.
        *
        * @param visible whether to draw the decorations
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder overlayVisible(boolean visible) {
            this.overlay = visible;
            return this;
        }

        /**
        * Returns whether the built icon will draw its overlay decorations; this is
        * {@code false} initially.
        *
        * @return {@code true} if the overlay should be visible, {@code false} otherwise.
        * @since 1.8.4
        */
        public boolean isOverlayVisible() {
            return overlay;
        }

        /**
        * Sets the icon scale; the builder defaults to {@code 1}. The icon's base size is
        * 16 by 16, and the scale must be greater than zero (unlike the element setter,
        * which permits negative values).
        *
        * @param scale the scale of the item
        * @return self for chaining.
        * @throws IllegalArgumentException if the scale is zero or negative
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
        * Returns the scale the builder will use, initially {@code 1}.
        *
        * @return the scale of the item.
        * @since 1.8.4
        */
        public double getScale() {
            return scale;
        }

        /**
        * Sets the rotation used when building the icon, in degrees. The builder stores
        * the float-converted angle without wrapping (so {@code 450} remains {@code 450});
        * building wraps finite values to {@code [-180, 180)}, making that angle {@code 90}.
        *
        * @param rotation the rotation (clockwise) of the item in degrees
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder rotation(double rotation) {
            this.rotation = (float) rotation;
            return this;
        }

        /**
        * Returns the builder's rotation in degrees, before the built icon normalizes it.
        *
        * @return the rotation (clockwise) of the item in degrees.
        * @since 1.8.4
        */
        public float getRotation() {
            return rotation;
        }

        /**
        * Sets whether the built icon rotates about its center rather than its position.
        *
        * @param rotateCenter whether the item should be rotated around its center
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder rotateCenter(boolean rotateCenter) {
            this.rotateCenter = rotateCenter;
            return this;
        }

        /**
        * Returns whether the built icon will rotate about its center.
        *
        * @return {@code true} if this item should be rotated around its center,
        * {@code false} otherwise.
        * @since 1.8.4
        */
        public boolean isRotatingCenter() {
            return rotateCenter;
        }

        /**
        * Sets the icon's draw-order value on its overlay; lower values draw first.
        *
        * @param zIndex the z-index of the item
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder zIndex(int zIndex) {
            this.zIndex = zIndex;
            return this;
        }

        /**
        * Returns the draw-order value that the builder will assign to the icon.
        *
        * @return the z-index of the item.
        * @since 1.8.4
        */
        public int getZIndex() {
            return zIndex;
        }

        /**
        * Creates a new icon from the builder's current values and binds it to its overlay.
        * Each call creates a separate icon. Unset overlay text is passed as an empty
        * string, unlike the constructors' {@code null} default that requests the stack count.
        *
        * @return the new icon.
        */
        @Override
        protected Item createElement() {
            return new Item(x, y, zIndex, itemStack, overlay, scale, rotation, ovText).setRotateCenter(rotateCenter)
                    .setParent(this.parent);
        }

        /**
        * Returns the icon's scaled width in whole pixels; fractional results are truncated.
        *
        * @return the scaled width
        */
        @Override
        public int getScaledWidth() {
            return (int) (DEFAULT_ITEM_SIZE * scale);
        }

        /**
        * Returns the width of the builder's overlay.
        *
        * @return the width of the parent
        */
        @Override
        public int getParentWidth() {
            return this.parent.getWidth();
        }

        /**
        * Returns the icon's scaled height in whole pixels; it equals its scaled width.
        *
        * @return the scaled height
        */
        @Override
        public int getScaledHeight() {
            return (int) (DEFAULT_ITEM_SIZE * scale);
        }

        /**
        * Returns the height of the overlay this builder belongs to, used by
        * parent-relative alignment.
        *
        * @return the height of the parent
        */
        @Override
        public int getParentHeight() {
            return this.parent.getHeight();
        }

        /**
        * Returns the icon's x position; scaling changes its size, not this position.
        *
        * @return the position of the scaled element's left side
        */
        @Override
        public int getScaledLeft() {
            return x;
        }

        /**
        * Returns the icon's y position; scaling changes its size, not this position.
        *
        * @return the position of the scaled element's top side
        */
        @Override
        public int getScaledTop() {
            return y;
        }

        /**
        * Sets the icon position, equivalent to {@code pos}; alignment methods use this
        * operation. It does not change the icon's size.
        *
        * @param x the new x position
        * @param y the new y position
        * @return self for chaining.
        */
        @Override
        public Builder moveTo(int x, int y) {
            return pos(x, y);
        }

    }

}
