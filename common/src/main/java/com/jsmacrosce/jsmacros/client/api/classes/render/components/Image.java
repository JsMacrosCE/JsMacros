package com.jsmacrosce.jsmacros.client.api.classes.render.components;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import com.jsmacrosce.jsmacros.client.api.classes.CustomImage;
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;
import com.jsmacrosce.jsmacros.client.api.classes.render.IDraw2D;
import com.jsmacrosce.jsmacros.client.util.ColorUtil;
import com.jsmacrosce.doclet.DocletIgnore;

//? if <=1.21.5 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
*///? }

//? if >=1.21.11 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Quaternionf;
import com.jsmacrosce.jsmacros.client.api.classes.render.components3d.SurfaceRenderTypes;
*///? } else {
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Quaternionf;
import com.jsmacrosce.jsmacros.client.api.classes.render.components3d.SurfaceRenderTypes;
//? }

 /**
  * Draws a texture region at a position and destination size. {@code width} and
  * {@code height} set the drawn size; {@code imageX} and {@code imageY} are pixel
  * offsets normalized by {@code textureWidth} and {@code textureHeight}. Builders
  * default texture dimensions to 256; constructors take them explicitly, so supply
  * the actual dimensions when they differ.
  * <p>
  * {@code regionWidth} and {@code regionHeight} affect 3D rendering only; 2D rendering
  * derives the sampled extent from destination and texture dimensions.
 * <pre>
 * const draw = Hud.createDraw2D();
 * const icon = draw.imageBuilder("minecraft:item/diamond").size(16, 16).buildAndAdd();
 * draw.register();
 * Chat.log(`texture is ${icon.textureWidth} by ${icon.textureHeight}`);
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.2.3
 */
@SuppressWarnings("unused")
@DocletCategory("Rendering/Graphics")
public class Image implements RenderElement, Alignable<Image> {

    private static final Minecraft mc = Minecraft.getInstance();

    /**
    * The texture identifier, or {@code null} when no texture is set.
    */
    private ResourceLocation imageid;
    /**
    * The overlay this image belongs to, or {@code null} when it is not bound to an overlay.
    */
    @Nullable
    public IDraw2D<?> parent;
    /**
    * The rotation in degrees. Constructors and setters normalize angles to
    * {@code [-180, 180)}; direct field writes do not.
    */
    public float rotation;
    /**
    * Whether rotation is about the center of the destination rectangle rather than its position.
    */
    public boolean rotateCenter;
    /**
    * The x coordinate where this image is drawn.
    */
    public int x;
    /**
    * The y coordinate where this image is drawn.
    */
    public int y;
    /**
    * The destination width, independent of the source region width.
    */
    public int width;
    /**
    * The destination height, independent of the source region height.
    */
    public int height;
    /**
    * The horizontal source offset in pixels, normalized by {@code textureWidth} for rendering.
    */
    public int imageX;
    /**
    * the vertical source offset in the texture, in pixels.
    */
    public int imageY;
    /**
    * the source-region width; used by the 3D path, but not by the 2D overlay.
    */
    public int regionWidth;
    /**
    * the source-region height; used by the 3D path, but not by the 2D overlay.
    */
    public int regionHeight;
    /**
    * the texture width used to convert the pixel source offset to a texture coordinate.
    * Builders default this to 256; set it to the actual texture width otherwise.
    */
    public int textureWidth;
    /**
    * the texture height used to convert the pixel source offset to a texture coordinate.
    * Builders default this to 256; set it to the actual texture height otherwise.
    */
    public int textureHeight;
    /**
    * the tint, packed with the alpha in the top byte.
    * <p>
    * A colour with no alpha of its own is made opaque unless it was pure black, so
    * {@code 0xFF0000} is stored as {@code 0xFFFF0000}.
    */
    public int color;
    /**
    * the number this image is ordered against the other elements on its overlay, lower
    * drawn first.
    */
    public int zIndex;

    /**
    * constructs an image from a texture region, applying the single-argument tint rules.
    * <p>
    * A bare texture name gets the {@code minecraft} namespace. Finite rotations are
    * normalized to {@code [-180, 180)}. Tint handling follows {@link #setColor(int)},
    * including its special handling of {@code 0x000000}.
    *
    * @param x             the x position of the image
    * @param y             the y position of the image
    * @param width         the width the piece is drawn at
    * @param height        the height the piece is drawn at
    * @param zIndex        the z-index against the other elements on the overlay
    * @param color         the tint, with the alpha in the top byte
    * @param id            the texture to draw from, with the namespace optional
    * @param imageX        the x position in the texture to start from
    * @param imageY        the y position in the texture to start from
    * @param regionWidth   the width of the region to take
    * @param regionHeight  the height of the region to take
    * @param textureWidth  the width of the whole texture
    * @param textureHeight the height of the whole texture
    * @param rotation      the rotation in degrees, normalized to {@code [-180, 180)}
    */
    public Image(int x, int y, int width, int height, int zIndex, int color, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight, float rotation) {
        this(
                x,
                y,
                width,
                height,
                zIndex,
                0xFF,
                color,
                id,
                imageX,
                imageY,
                regionWidth,
                regionHeight,
                textureWidth,
                textureHeight,
                rotation
        );
        setColor(color);
    }

    /**
    * constructs an image from a texture region, with alpha supplied separately.
    * <p>
    * The alpha replaces the top byte of the colour outright, so a colour that came with
    * an alpha of its own loses it, and a value outside 0 to 255 is not clamped. As in
    * the shorter constructor finite rotations are normalized to {@code [-180, 180)}.
    *
    * @param x             the x position of the image
    * @param y             the y position of the image
    * @param width         the width the piece is drawn at
    * @param height        the height the piece is drawn at
    * @param zIndex        the z-index against the other elements on the overlay
    * @param alpha         the alpha, in the top byte
    * @param color         the colour, whose top byte is replaced
    * @param id            the texture to draw from, with the namespace optional
    * @param imageX        the x position in the texture to start from
    * @param imageY        the y position in the texture to start from
    * @param regionWidth   the width of the region to take
    * @param regionHeight  the height of the region to take
    * @param textureWidth  the width of the whole texture
    * @param textureHeight the height of the whole texture
    * @param rotation      the rotation in degrees, normalized to {@code [-180, 180)}
    */
    public Image(int x, int y, int width, int height, int zIndex, int alpha, int color, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight, float rotation) {
        setPos(x, y, width, height);
        setColor(color, alpha);
        setImage(id, imageX, imageY, regionWidth, regionHeight, textureWidth, textureHeight);
        this.rotation = Mth.wrapDegrees(rotation);
    }

    /**
    * sets the texture identifier, source offset, region size, and texture dimensions.
    * The destination size is unchanged. On a 2D overlay, region dimensions are stored
    * but ignored; the draw uses the offset, texture dimensions, and destination size.
    *
    * @param id            the texture to draw from, with the namespace optional
    * @param imageX        the x position in the texture to start from
    * @param imageY        the y position in the texture to start from
    * @param regionWidth   the width of the region to take
    * @param regionHeight  the height of the region to take
    * @param textureWidth  the width of the whole texture
    * @param textureHeight the height of the whole texture
    * @return self for chaining.
    * @since 1.2.3
    */
    public Image setImage(String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        imageid = RegistryHelper.parseIdentifier(id);
        this.imageX = imageX;
        this.imageY = imageY;
        this.regionWidth = regionWidth;
        this.regionHeight = regionHeight;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        return this;
    }

    /**
    * Returns the resolved texture identifier in {@code namespace:path} form. Throws
    * {@link NullPointerException} if no texture has been set.
    *
    * @return the texture identifier
    * @since 1.2.3
    */
    public String getImage() {
        return imageid.toString();
    }

    /**
    * Sets the x position without changing the y position or texture.
    *
    * @param x the new x position of this image
    * @return self for chaining.
    * @since 1.8.4
    */
    public Image setX(int x) {
        this.x = x;
        return this;
    }

    /**
    * Returns the x position where this image is drawn.
    *
    * @return the x position of this image.
    * @since 1.8.4
    */
    public int getX() {
        return x;
    }

    /**
    * Sets the y position without changing the x position or texture.
    *
    * @param y the new y position of this image
    * @return self for chaining.
    * @since 1.8.4
    */
    public Image setY(int y) {
        this.y = y;
        return this;
    }

    /**
    * Returns the y position where this image is drawn.
    *
    * @return the y position of this image.
    * @since 1.8.4
    */
    public int getY() {
        return y;
    }

    /**
    * Sets both position coordinates without changing the size or texture.
    *
    * @param x the new x position of this image
    * @param y the new y position of this image
    * @return self for chaining.
    * @since 1.8.4
    */
    public Image setPos(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /**
    * Sets the position and destination size; the texture region is unchanged.
    *
    * @param x      the new x position of this image
    * @param y      the new y position of this image
    * @param width  the width to draw it at
    * @param height the height to draw it at
    * @return self for chaining.
    * @since 1.2.3
    */
    public Image setPos(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    /**
    * Sets the destination width, without changing the texture region. No range check is
    * performed, so negative values are stored.
    *
    * @param width the new width of this image
    * @return self for chaining.
    * @since 1.8.4
    */
    public Image setWidth(int width) {
        this.width = width;
        return this;
    }

    /**
    * Returns the destination width, which is independent of the texture region.
    *
    * @return the width of this image.
    * @since 1.8.4
    */
    public int getWidth() {
        return width;
    }

    /**
    * Sets the destination height, without changing the texture region. No range check is
    * performed, so negative values are stored.
    *
    * @param height the new height of this image
    * @return self for chaining.
    * @since 1.8.4
    */
    public Image setHeight(int height) {
        this.height = height;
        return this;
    }

    /**
    * Returns the destination height, independent of the texture region.
    *
    * @return the height of this image.
    * @since 1.8.4
    */
    public int getHeight() {
        return height;
    }

    /**
    * Sets the destination width and height without changing the texture region. Negative
    * values are stored without validation.
    *
    * @param width  the width to draw it at
    * @param height the height to draw it at
    * @return self for chaining.
    * @since 1.8.4
    */
    public Image setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
    * Sets the packed ARGB tint. A zero alpha byte becomes opaque when RGB is nonzero;
    * zero-alpha black remains transparent, and nonzero alpha is preserved.
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).buildAndAdd();
    * icon.setColor(0xFF0000);
    * draw.register();
    * Chat.log(`0x${(icon.getColor() >>> 0).toString(16)}`); // ffff0000
    * </pre>
    *
    * @param color packed ARGB colour, with alpha in the top byte
    * @return self for chaining
    * @since 1.6.5
    */
    public Image setColor(int color) {
        this.color = ColorUtil.fixAlpha(color);
        return this;
    }

    /**
    * Sets the RGB tint with an explicit alpha byte, allowing transparent nonblack colours.
    * Uses the low 24 bits of {@code color} and the low 8 bits of {@code alpha}; values outside
    * {@code 0..255} wrap rather than clamp, without changing the RGB bits.
    *
    * @param color packed RGB colour; its existing alpha byte is ignored
    * @param alpha alpha from {@code 0} (transparent) to {@code 255} (opaque); only the low byte is used
    * @return self for chaining
    * @since 1.6.5
    */
    public Image setColor(int color, int alpha) {
        this.color = (alpha << 24) | (color & 0xFFFFFF);
        return this;
    }

    /**
    * Returns the packed tint currently stored by this image, with alpha in the top byte.
    *
    * @return the packed ARGB tint
    * @since 1.8.4
    */
    public int getColor() {
        return color;
    }

    /**
    * Returns the alpha byte of the stored tint, from 0 (transparent) to 255 (opaque).
    * Use {@link #setColor(int, int)} to set alpha explicitly.
    *
    * @return the unsigned alpha byte from {@code 0} to {@code 255}
    * @since 1.8.4
    */
    public int getAlpha() {
        return (color >> 24) & 0xFF;
    }

    /**
    * Sets the rotation in degrees after converting to a float. Finite float values wrap to
    * {@code [-180, 180)}; {@link #setRotateCenter(boolean)} selects the pivot.
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(20, 20).size(16, 16).buildAndAdd();
    * icon.setRotateCenter(true).setRotation(450);
    * draw.register();
    * Chat.log(`reads back as ${icon.getRotation()}`); // 90
    * </pre>
    *
    * @param rotation the angle in degrees
    * @return self for chaining
    * @since 1.2.6
    */
    public Image setRotation(double rotation) {
        this.rotation = Mth.wrapDegrees((float) rotation);
        return this;
    }

    /**
    * Returns the stored rotation in degrees.
    * Constructors and setters wrap the angle; direct writes to the public field do not.
    *
    * @return the stored angle in degrees
    * @since 1.8.4
    */
    public float getRotation() {
        return rotation;
    }

    /**
    * Sets whether rotation is about the center of the destination rectangle rather than
    * the image position.
    *
    * @param rotateCenter whether the image should be rotated around its center
    * @return self for chaining
    * @since 1.8.4
    */
    public Image setRotateCenter(boolean rotateCenter) {
        this.rotateCenter = rotateCenter;
        return this;
    }

    /**
    * Returns whether rotation is about the center of the destination rectangle.
    *
    * @return {@code true} for the destination rectangle's center; {@code false} for the image position
    * @since 1.8.4
    */
    public boolean isRotatingCenter() {
        return rotateCenter;
    }

    /**
    * Sets this image's ordering value on its overlay; lower values are drawn first.
    *
    * @param zIndex the new z-index for this image
    * @return self for chaining.
    * @since 1.8.4
    */
    public Image setZIndex(int zIndex) {
        this.zIndex = zIndex;
        return this;
    }

    /**
    * Returns this image's ordering value on its overlay; lower values are drawn first.
    *
    * @return the z-index of this element
    */
    @Override
    public int getZIndex() {
        return zIndex;
    }

    /**
    * Draws this image as part of its overlay, applying its position, rotation, and tint.
    * The mouse coordinates and frame delta are unused. The 2D draw uses the texture
    * offset and destination size; it does not use the configured region width or height.
    *
    * @param drawContext the graphics context to draw into
    * @param mouseX      the mouse x, not used by this element
    * @param mouseY      the mouse y, not used by this element
    * @param delta       the frame delta, not used by this element
    */
    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        //? if >1.21.5 {
        Matrix3x2fStack matrices = drawContext.pose();
        matrices.pushMatrix();
        //?} else {
        /*PoseStack matrices = drawContext.pose();
        matrices.pushPose();
        *///?}
        setupMatrix(matrices, x, y, 1, rotation, getWidth(), getHeight(), rotateCenter);
        float u = this.imageX / (float) this.textureWidth;
        float v = this.imageY / (float) this.textureHeight;

        drawContext.blit(
                //? if >1.21.5 {
                RenderPipelines.GUI_TEXTURED,
                //?} else {
                /*RenderType::guiTextured,
                *///?}
                this.imageid,
                this.x,
                this.y,
                u,
                v,
                this.width,
                this.height,
                this.textureWidth,
                this.textureHeight,
                this.color);
        //? if >1.21.5 {
        matrices.popMatrix();
        //?} else {
        /*matrices.popPose();
        *///?}
    }

    // Draws the image into the surface's world-space buffer source. Textured
    // images cannot be represented as gizmos, so this is the direct path.
    @DocletIgnore
    @Override
    public void render3D(PoseStack matrixStack, MultiBufferSource consumers, int light, boolean seeThrough, float delta) {
        matrixStack.pushPose();
        matrixStack.translate(x, y, 0);
        if (rotateCenter) {
            matrixStack.translate(width / 2d, height / 2d, 0);
        }
        matrixStack.mulPose(new Quaternionf().rotateLocalZ((float) Math.toRadians(rotation)));
        if (rotateCenter) {
            matrixStack.translate(-width / 2d, -height / 2d, 0);
        }
        matrixStack.translate(-x, -y, 0);

        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float u0 = imageX / (float) textureWidth;
        float v0 = imageY / (float) textureHeight;
        float u1 = (imageX + regionWidth) / (float) textureWidth;
        float v1 = (imageY + regionHeight) / (float) textureHeight;

        VertexConsumer vc = consumers.getBuffer(SurfaceRenderTypes.images(imageid, !seeThrough));
        PoseStack.Pose pose = matrixStack.last();
        vc.addVertex(pose, x, y, 0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x, y + height, 0).setColor(r, g, b, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x + width, y + height, 0).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, x + width, y, 0).setColor(r, g, b, a).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        matrixStack.popPose();
    }

    /**
    * Sets the overlay used as this image's parent for parent-relative alignment.
    * Passing {@code null} makes parent dimensions fall back to the window dimensions.
    *
    * @param parent the overlay this element is on, or {@code null} for the window
    * @return self for chaining.
    */
    public Image setParent(IDraw2D<?> parent) {
        this.parent = parent;
        return this;
    }

    /**
    * Returns the destination width used for alignment; images have no additional scale.
    *
    * @return the scaled width of this element
    */
    @Override
    public int getScaledWidth() {
        return width;
    }

    /**
    * Returns the parent overlay width, or the window's GUI-scaled width when no parent is set.
    *
    * @return the width of the parent
    */
    @Override
    public int getParentWidth() {
        return parent != null ? parent.getWidth() : mc.getWindow().getGuiScaledWidth();
    }

    /**
   * Returns the destination height used for alignment; images have no additional scale.
    *
    * @return the scaled height of this element
    */
    @Override
    public int getScaledHeight() {
        return height;
    }

    /**
    * Returns the parent overlay height, or the window's GUI-scaled height when no parent is set.
    *
    * @return the height of the parent
    */
    @Override
    public int getParentHeight() {
        return parent != null ? parent.getHeight() : mc.getWindow().getGuiScaledHeight();
    }

    /**
    * Returns the image's x position, which is its left edge.
    *
    * @return the position of the scaled element's left side
    */
    @Override
    public int getScaledLeft() {
        return x;
    }

    /**
    * Returns the image's y position, which is its top edge.
    *
    * @return the position of the scaled element's top side
    */
    @Override
    public int getScaledTop() {
        return y;
    }

    /**
    * Moves this image without changing its destination size.
    *
    * @param x the new x position
    * @param y the new y position
    * @return self for chaining.
    */
    @Override
    public Image moveTo(int x, int y) {
        return setPos(x, y, width, height);
    }

    /**
    * Builds an image element for an overlay. Destination dimensions and texture-region
    * dimensions are configured independently; texture dimensions default to 256 by 256.
    * Position and destination dimensions default to zero, tint to opaque white, rotation
    * to zero, and rotation about the center to {@code true}. Without an identifier, the
    * built image has no texture to draw.
    * <pre>
    * const draw = Hud.createDraw2D();
    * draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).buildAndAdd();
    * draw.register();
    * </pre>
    *
    * @author Etheradon
    * @since 1.8.4
    */
    @DocletCategory("Rendering/Graphics")
    public static final class Builder extends RenderElementBuilder<Image> implements Alignable<Builder> {
        private String identifier;
        private int x = 0;
        private int y = 0;
        private int width = 0;
        private int height = 0;
        private int imageX = 0;
        private int imageY = 0;
        private int regionWidth = 0;
        private int regionHeight = 0;
        private int textureWidth = 256;
        private int textureHeight = 256;
        private int color = 0xFFFFFFFF;
        private int alpha = 0xFF;
        private float rotation = 0;
        private boolean rotateCenter = true;
        private int zIndex = 0;

        /**
        * Creates a builder associated with the given overlay.
        *
        * @param draw2D the overlay the images will be added to
        */
        public Builder(IDraw2D<?> draw2D) {
            super(draw2D);
        }

        /**
        * Uses the custom image's identifier and dimensions for the texture, full region,
        * and destination size, and resets the position to zero. Other builder settings
        * are unchanged.
        *
        * @param customImage the custom image to use
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder fromCustomImage(CustomImage customImage) {
            this.width = customImage.getWidth();
            this.height = customImage.getHeight();
            this.imageX = 0;
            this.imageY = 0;
            this.regionWidth = customImage.getWidth();
            this.regionHeight = customImage.getHeight();
            this.textureWidth = customImage.getWidth();
            this.textureHeight = customImage.getHeight();
            this.identifier = customImage.getIdentifier();
            return this;
        }

        /**
        * Sets the identifier of the texture to draw. Other builder settings are unchanged.
        *
        * @param identifier the identifier of the image to use
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder identifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
        * Returns the identifier set on this builder, or {@code null} if none was set.
        *
        * @return the identifier of the used image or {@code null} if no image is used.
        * @since 1.8.4
        */
        public String getIdentifier() {
            return identifier;
        }

        /**
        * Sets the x position without changing the y position or other builder settings.
        *
        * @param x the x position of the image
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder x(int x) {
            this.x = x;
            return this;
        }

        /**
        * Returns the configured x position, initially zero. {@code fromCustomImage}
        * resets it to zero.
        *
        * @return the x position of the image.
        * @since 1.8.4
        */
        public int getX() {
            return x;
        }

        /**
        * Sets the y position without changing the x position or other builder settings.
        *
        * @param y the y position of the image
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder y(int y) {
            this.y = y;
            return this;
        }

        /**
        * Returns the configured y position, initially zero. {@code fromCustomImage}
        * resets it to zero.
        *
        * @return the y position of the image.
        * @since 1.8.4
        */
        public int getY() {
            return y;
        }

        /**
        * Sets the image position without changing its size or texture coordinates.
        *
        * @param x the x position of the image
        * @param y the y position of the image
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        /**
        * Sets the destination width, independently of the texture region. The default is
        * zero; values, including negative values, are stored without validation.
        *
        * @param width the width of the image
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder width(int width) {
            this.width = width;
            return this;
        }

        /**
        * Returns the configured destination width (zero by default), not the texture-region width.
        *
        * @return the width of the image.
        * @since 1.8.4
        */
        public int getWidth() {
            return width;
        }

        /**
        * Sets the destination height, independently of the texture region. The default is
        * zero; values, including negative values, are stored without validation.
        *
        * @param height the height of the image
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder height(int height) {
            this.height = height;
            return this;
        }

        /**
        * Returns the configured destination height (zero by default), not the texture-region height.
        *
        * @return the height of the image.
        * @since 1.8.4
        */
        public int getHeight() {
            return height;
        }

        /**
        * Sets the destination width and height, independently of the texture region.
        *
        * @param width  the width of the image
        * @param height the height of the image
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        /**
        * Sets the horizontal source offset in pixels. It is converted to a texture
        * coordinate using the configured texture width (256 by default).
        *
        * @param imageX the x position in the image texture to start drawing from
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder imageX(int imageX) {
            this.imageX = imageX;
            return this;
        }

        /**
        * Returns the horizontal source offset in pixels (zero by default).
        *
        * @return the x position in the image texture to start drawing from.
        * @since 1.8.4
        */
        public int getImageX() {
            return imageX;
        }

        /**
        * Sets the vertical source offset in pixels. It is converted to a texture
        * coordinate using the configured texture height (256 by default).
        *
        * @param imageY the y position in the image texture to start drawing from
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder imageY(int imageY) {
            this.imageY = imageY;
            return this;
        }

        /**
        * Returns the vertical source offset in pixels (zero by default).
        *
        * @return the y position in the image texture to start drawing from.
        * @since 1.8.4
        */
        public int getImageY() {
            return imageY;
        }

        /**
        * Sets the source offset on both axes, in pixels from the texture's top-left.
        *
        * @param imageX the x position in the image texture to start drawing from
        * @param imageY the y position in the image texture to start drawing from
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder imagePos(int imageX, int imageY) {
            this.imageX = imageX;
            this.imageY = imageY;
            return this;
        }

        /**
        * Sets the source-region width. It affects 3D rendering; 2D overlays derive the
        * sampled extent from destination and texture dimensions instead.
        *
        * @param regionWidth the width of the region to draw
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder regionWidth(int regionWidth) {
            this.regionWidth = regionWidth;
            return this;
        }

        /**
        * Returns the configured source-region width (zero by default), used by 3D
        * rendering and distinct from the destination width.
        *
        * @return the width of the region to draw.
        * @since 1.8.4
        */
        public int getRegionWidth() {
            return regionWidth;
        }

        /**
        * Sets the source-region height. It affects 3D rendering; 2D overlays derive the
        * sampled extent from destination and texture dimensions instead.
        *
        * @param regionHeight the height of the region to draw
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder regionHeight(int regionHeight) {
            this.regionHeight = regionHeight;
            return this;
        }

        /**
        * Returns the configured source-region height (zero by default), used by 3D
        * rendering and distinct from the destination height.
        *
        * @return the height of the region to draw.
        * @since 1.8.4
        */
        public int getRegionHeight() {
            return regionHeight;
        }

        /**
        * Sets the source-region width and height. These dimensions affect 3D rendering;
        * 2D overlays derive the sampled extent from destination and texture dimensions.
        *
        * @param regionWidth  the width of the region to draw
        * @param regionHeight the height of the region to draw
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder regionSize(int regionWidth, int regionHeight) {
            this.regionWidth = regionWidth;
            this.regionHeight = regionHeight;
            return this;
        }

        /**
        * Sets the source offset and region dimensions. Texture dimensions are unchanged
        * (256 by 256 by default); set them separately for textures of another size.
        *
        * @param x      the x position in the image texture to start drawing from
        * @param y      the y position in the image texture to start drawing from
        * @param width  the width of the region to draw
        * @param height the height of the region to draw
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder regions(int x, int y, int width, int height) {
            this.imageX = x;
            this.imageY = y;
            this.regionWidth = width;
            this.regionHeight = height;
            return this;
        }

        /**
        * Sets the source offset, region dimensions, and texture dimensions. Supply the
        * actual texture dimensions so pixel offsets are normalized correctly.
        *
        * @param x             the x position in the image texture to start drawing from
        * @param y             the y position in the image texture to start drawing from
        * @param width         the width of the region to draw
        * @param height        the height of the region to draw
        * @param textureWidth  the width of the used texture
        * @param textureHeight the height of the used texture
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder regions(int x, int y, int width, int height, int textureWidth, int textureHeight) {
            this.imageX = x;
            this.imageY = y;
            this.regionWidth = width;
            this.regionHeight = height;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            return this;
        }

        /**
        * Sets the texture width used to normalize the horizontal source offset; the
        * default is 256.
        *
        * @param textureWidth the width of the used texture
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder textureWidth(int textureWidth) {
            this.textureWidth = textureWidth;
            return this;
        }

        /**
        * Returns the configured texture width used to normalize source offsets (256 by
        * default).
        *
        * @return the width of the used texture.
        * @since 1.8.4
        */
        public int getTextureWidth() {
            return textureWidth;
        }

        /**
        * Sets the texture height used to normalize texture-region coordinates; defaults to 256.
        *
        * @param textureHeight the height of the used texture
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder textureHeight(int textureHeight) {
            this.textureHeight = textureHeight;
            return this;
        }

        /**
        * Returns the texture height used to normalize texture-region coordinates; defaults to 256.
        *
        * @return the height of the used texture.
        * @since 1.8.4
        */
        public int getTextureHeight() {
            return textureHeight;
        }

        /**
        * Sets the texture dimensions used to normalize texture-region coordinates; both default to 256.
        *
        * @param textureWidth  the width of the used texture
        * @param textureHeight the height of the used texture
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder textureSize(int textureWidth, int textureHeight) {
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            return this;
        }

        /**
        * Sets the packed tint and derives alpha from its top byte, using {@link ColorUtil#fixAlpha(int)}.
        *
        * @param color the color of the image
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder color(int color) {
            this.color = color;
            this.alpha = ColorUtil.fixAlpha(color) >>> 24;
            return this;
        }

        /**
        * Packs the RGB components into the low 24 bits and leaves the builder's alpha unchanged.
        *
        * @param r the red component of the color
        * @param g the green component of the color
        * @param b the blue component of the color
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder color(int r, int g, int b) {
            this.color = (r << 16) | (g << 8) | b;
            return this;
        }

        /**
        * Packs RGB into the low 24 bits and stores {@code a} separately as alpha.
        *
        * @param r the red component of the color
        * @param g the green component of the color
        * @param b the blue component of the color
        * @param a the alpha component of the color
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder color(int r, int g, int b, int a) {
            this.color = (r << 16) | (g << 8) | b;
            this.alpha = a;
            return this;
        }

        /**
        * Stores the tint and alpha separately without adjusting either value.
        *
        * @param color the color of the image
        * @param alpha the alpha value of the color
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder color(int color, int alpha) {
            this.color = color;
            this.alpha = alpha;
            return this;
        }

        /**
        * Returns the stored tint; component overloads store RGB in the low 24 bits, while packed overloads store the supplied color.
        *
        * @return the color of the image.
        * @since 1.8.4
        */
        public int getColor() {
            return color;
        }

        /**
        * Sets alpha separately without clamping; when the image is built, it is shifted into the top byte.
        *
        * @param alpha the alpha value of the color
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder alpha(int alpha) {
            this.alpha = alpha;
            return this;
        }

        /**
        * Returns the separately stored alpha, initially opaque; RGB and RGBA component setters leave it unchanged.
        *
        * @return the alpha value of the color.
        * @since 1.8.4
        */
        public int getAlpha() {
            return alpha;
        }

        /**
        * Sets the image rotation in degrees; the builder stores the supplied value, while
        * the built image normalizes it to {@code [-180, 180)}.
        *
        * @param rotation the rotation (clockwise) of the image in degrees
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder rotation(double rotation) {
            this.rotation = (float) rotation;
            return this;
        }

        /**
        * Returns the builder's stored rotation in degrees, before the built image
        * normalizes finite values to {@code [-180, 180)}.
        *
        * @return the rotation (clockwise) of the image in degrees.
        * @since 1.8.4
        */
        public float getRotation() {
            return rotation;
        }

        /**
        * Sets whether the built image rotates about its center or its position. Builders
        * default to rotating about the center; images made directly default to the
        * position.
        *
        * @param rotateCenter whether the image should be rotated around its center
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder rotateCenter(boolean rotateCenter) {
            this.rotateCenter = rotateCenter;
            return this;
        }

        /**
        * Returns whether the image built by this builder will rotate about its center;
        * builders default to {@code true}.
        *
        * @return {@code true} if this image should be rotated around its center,
        * {@code false} otherwise.
        * @since 1.8.4
        */
        public boolean isRotatingCenter() {
            return rotateCenter;
        }

        /**
        * Sets this image's overlay ordering value; lower values are drawn first. A
        * builder defaults to zero.
        *
        * @param zIndex the z-index of the image
        * @return self for chaining.
        * @since 1.8.4
        */
        public Builder zIndex(int zIndex) {
            this.zIndex = zIndex;
            return this;
        }

        /**
        * Returns this builder's overlay ordering value, initially zero.
        *
        * @return the z-index of the image.
        * @since 1.8.4
        */
        public int getZIndex() {
            return zIndex;
        }

        /**
        * Creates a new image from the builder's current values and binds it to its
        * overlay. Each call creates a distinct image; adding it to the overlay is a
        * separate operation. The separate alpha replaces the tint's top byte.
        *
        * @return the new image.
        */
        @Override
        public Image createElement() {
            return new Image(
                    x,
                    y,
                    width,
                    height,
                    zIndex,
                    alpha,
                    color,
                    identifier,
                    imageX,
                    imageY,
                    regionWidth,
                    regionHeight,
                    textureWidth,
                    textureHeight,
                    rotation
            ).setRotateCenter(rotateCenter).setParent(parent);
        }

        /**
        * Returns the image width as drawn; images have no additional scale.
        *
        * @return the scaled width
        */
        @Override
        public int getScaledWidth() {
            return width;
        }

        /**
        * Returns the width of the overlay this builder was created from, used by
        * parent-relative alignment.
        *
        * @return the width of the parent
        */
        @Override
        public int getParentWidth() {
            return parent.getWidth();
        }

        /**
        * Returns the image height as drawn; images have no additional scale.
        *
        * @return the scaled height
        */
        @Override
        public int getScaledHeight() {
            return height;
        }

        /**
        * the height of the overlay this builder was made from, which is what the
        * parent-relative align methods measure against.
        *
        * @return the height of the parent
        */
        @Override
        public int getParentHeight() {
            return parent.getHeight();
        }

        /**
        * the x position this builder will build the image at.
        *
        * @return the position of the scaled element's left side
        */
        @Override
        public int getScaledLeft() {
            return x;
        }

        /**
        * the y position this builder will build the image at.
        *
        * @return the position of the scaled element's top side
        */
        @Override
        public int getScaledTop() {
            return y;
        }

        /**
        * Sets the image position without changing its destination size.
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
