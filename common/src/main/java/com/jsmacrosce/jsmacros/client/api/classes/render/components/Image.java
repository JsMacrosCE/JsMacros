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
 * a piece of a texture, drawn at a position and a size.
 * <p>
 * Four separate sizes are involved here, which is the one thing worth reading before
 * using it. {@code width} and {@code height} are how big the piece is drawn.
 * {@code imageX} and {@code imageY} are where in the texture it comes from, and
 * {@code textureWidth} and {@code textureHeight} are how big the whole texture is, since
 * a texture coordinate is a fraction of the texture and not a pixel. Those last two start
 * at 256 on a builder, which is the size of a vanilla texture sheet, and are left at zero
 * on an element made directly, so a texture that is not 256 needs them set on an element
 * or the piece comes from the wrong place.
 * <p>
 * The fourth pair, {@code regionWidth} and {@code regionHeight}, is the size of the
 * region within the texture, and on a 2D overlay it is not read at all: the 2D draw
 * takes the destination size and the texture size and works the rest out itself, so
 * setting the region there changes nothing that is drawn. The 3D path is the one that
 * uses it, along with the texture size, to work out the far corner of the region.
 * example:
 * <pre>
 * const draw = Hud.createDraw2D();
 * // the icon in the middle of the item sheet, drawn at 16 by 16
 * const icon = draw.imageBuilder("minecraft:item/diamond").regions(208, 0, 16, 16).size(16, 16).buildAndAdd();
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
    * the texture being drawn, and {@code null} for an image that draws nothing.
    * <p>
    * Not public, so the only way in is {@code setImage} or a builder, both of which run
    * the id through the usual parser. {@code getImage} reads it back as a string.
    */
    private ResourceLocation imageid;
    /**
    * the overlay this image was added to, and {@code null} when it has not been put on
    * one.
    * <p>
    * This is what the parent width and height are read from, and they fall back to the
    * window when it is null.
    */
    @Nullable
    public IDraw2D<?> parent;
    /**
    * the rotation in degrees, wrapped into a single turn.
    * <p>
    * Constructors and setters normalize finite angles to {@code [-180, 180)}, so 450
    * reads back as 90. Direct field writes bypass normalization.
    */
    public float rotation;
    /**
    * whether the rotation is about the middle of this image or about its position.
    * <p>
    * False on an image made directly and true on one from a builder, so the two turn
    * differently at the same angle.
    */
    public boolean rotateCenter;
    /**
    * the x position of this image, which is where the piece is drawn.
    */
    public int x;
    /**
    * the y position of this image, which is where the piece is drawn.
    * <p>
    * The counterpart of {@code x}, and read the same way.
    */
    public int y;
    /**
    * the width the piece is drawn at, which is not the width of the region it comes
    * from.
    * <p>
    * This is the destination size. A whole sheet drawn at 16 by 16 shows one sixteenth
    * of it rather than the whole thing shrunk.
    */
    public int width;
    /**
    * the height the piece is drawn at, which is not the height of the region it comes
    * from.
    * <p>
    * The counterpart of {@code width}, and read the same way.
    */
    public int height;
    /**
    * the x position in the texture to start drawing from, in pixels.
    * <p>
    * Divided by the texture width to make the fraction the 2D draw wants, so this is a
    * pixel offset into the whole sheet rather than a coordinate between 0 and 1.
    */
    public int imageX;
    /**
    * the y position in the texture to start drawing from, in pixels.
    * <p>
    * The counterpart of {@code imageX}, and used the same way.
    */
    public int imageY;
    /**
    * the width of the region being taken from the texture.
    * <p>
    * Read by the 3D path and not by the 2D one, which takes the destination size and
    * the texture size and works the rest out itself. So on a 2D overlay this is
    * remembered and nothing more.
    */
    public int regionWidth;
    /**
    * the height of the region being taken from the texture.
    * <p>
    * The counterpart of {@code regionWidth}, and unused on a 2D overlay in the same
    * way.
    */
    public int regionHeight;
    /**
    * the width of the whole texture, which is what the offset is a fraction of.
    * <p>
    * A builder starts this at 256, which is the size of a vanilla texture sheet. A
    * texture that is not 256 needs this set or the piece comes from the wrong place.
    */
    public int textureWidth;
    /**
    * the height of the whole texture, which is what the offset is a fraction of.
    * <p>
    * The counterpart of {@code textureWidth}, and a builder starts it at 256 as well.
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
    * makes an image from a texture and a region of it, with the tint made opaque.
    * <p>
    * The id goes through the usual parser, so a bare name gets the {@code minecraft}
    * namespace. Finite rotations are normalized to {@code [-180, 180)}, and the colour
    * is put through the same fix-up as {@link #setColor(int)}, so one with no alpha of
    * its own comes out opaque. The one thing left alone is {@code 0x000000}, which that
    * fix-up leaves fully transparent; {@link #setColor(int)} says why.
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
    * makes an image from a texture and a region of it, with the alpha given separately.
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
    * points this image at a texture and a region of it, in one call.
    * <p>
    * This is the whole of the texture side in one method: which file, where in it to
    * start, how big the region is, and how big the file is. The size the piece is drawn
    * at is not among them and is set separately, because that is the destination size
    * rather than the source one.
    * <p>
    * The region is remembered rather than drawn from on a 2D overlay: the 2D draw takes
    * the destination size and the texture size and works the rest out itself. So on an
    * overlay only the offset and the texture size change what appears.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.addImage(10, 10, 16, 16, 0, "minecraft:item/diamond", 208, 0, 16, 16, 256, 256);
    * draw.register();
    * // a different icon off the same sheet
    * icon.setImage("minecraft:item/emerald", 208, 0, 16, 16, 256, 256);
    * </pre>
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
    * the texture this image is drawing, as an id.
    * <p>
    * Read back as a string, so it comes out in the full {@code namespace:path} form
    * even when a bare name was given to set it.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("item/diamond").pos(10, 10).size(16, 16).buildAndAdd();
    * draw.register();
    * Chat.log(`drawing ${icon.getImage()}`);
    * </pre>
    *
    * @return the id of the texture, or null if none has been set
    * @since 1.2.3
    */
    public String getImage() {
        return imageid.toString();
    }

    /**
    * moves this image along the x axis, and leaves the y axis alone.
    * <p>
    * The texture side is untouched, so this only changes where the piece is drawn.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(0, 0).size(16, 16).buildAndAdd();
    * icon.setX(40);
    * draw.register();
    * </pre>
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
    * the x position of this image, which is where the piece is drawn.
    * <p>
    * Unchanged by anything about the texture, and the left of the destination rather
    * than of the region it comes from.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(40, 20).size(16, 16).buildAndAdd();
    * draw.register();
    * Chat.log(`drawn at ${icon.getX()}, ${icon.getY()}`);
    * </pre>
    *
    * @return the x position of this image.
    * @since 1.8.4
    */
    public int getX() {
        return x;
    }

    /**
    * moves this image along the y axis, and leaves the x axis alone.
    * <p>
    * The counterpart of {@code setX}, and the texture side is untouched here as well.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(0, 0).size(16, 16).buildAndAdd();
    * icon.setY(40);
    * draw.register();
    * </pre>
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
    * the y position of this image, which is where the piece is drawn.
    * <p>
    * The counterpart of {@code getX}, and read the same way.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(40, 20).size(16, 16).buildAndAdd();
    * draw.register();
    * Chat.log(`drawn at ${icon.getX()}, ${icon.getY()}`);
    * </pre>
    *
    * @return the y position of this image.
    * @since 1.8.4
    */
    public int getY() {
        return y;
    }

    /**
    * moves this image, and leaves the texture side alone.
    * <p>
    * Both coordinates are set and nothing is re-measured, so this is where the piece is
    * drawn rather than anything about what is drawn.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(0, 0).size(16, 16).buildAndAdd();
    * icon.setPos(40, 20);
    * draw.register();
    * </pre>
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
    * moves this image and resizes it at the same time.
    * <p>
    * The two-argument form moves the piece and leaves the destination size alone; this
    * is the one that changes both. What is resized is the drawn size, not the region of
    * the texture it comes from.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(0, 0).size(16, 16).buildAndAdd();
    * icon.setPos(40, 20, 32, 32);
    * draw.register();
    * Chat.log(`now ${icon.getWidth()} by ${icon.getHeight()}`);
    * </pre>
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
    * changes the width the piece is drawn at.
    * <p>
    * The destination size, not the region it comes from: making a 16 wide icon draw at
    * 32 wide makes it bigger rather than taking twice as much of the texture. Nothing
    * is checked, so a negative width is accepted.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).buildAndAdd();
    * icon.setWidth(32);
    * draw.register();
    * Chat.log(`now ${icon.getWidth()} wide`);
    * </pre>
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
    * the width the piece is drawn at.
    * <p>
    * The destination size, and not the width of the region being taken from the
    * texture. There is no scale on an image, so this is also what is drawn and what the
    * align methods measure.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(32, 32).buildAndAdd();
    * draw.register();
    * Chat.log(`drawn ${icon.getWidth()} by ${icon.getHeight()}, from a ${icon.regionWidth} wide region`);
    * </pre>
    *
    * @return the width of this image.
    * @since 1.8.4
    */
    public int getWidth() {
        return width;
    }

    /**
    * changes the height the piece is drawn at.
    * <p>
    * The counterpart of {@code setWidth}, and the same distinction: the destination size
    * rather than the region. Nothing is checked, so a negative height is accepted.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).buildAndAdd();
    * icon.setHeight(32);
    * draw.register();
    * Chat.log(`now ${icon.getHeight()} tall`);
    * </pre>
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
    * the height the piece is drawn at.
    * <p>
    * The destination size, and not the height of the region being taken from the
    * texture.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).buildAndAdd();
    * draw.register();
    * Chat.log(`drawn ${icon.getWidth()} by ${icon.getHeight()}`);
    * </pre>
    *
    * @return the height of this image.
    * @since 1.8.4
    */
    public int getHeight() {
        return height;
    }

    /**
    * changes the size the piece is drawn at, on both axes.
    * <p>
    * The destination size. The width is set first, and neither is checked, so negative
    * numbers are accepted.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).buildAndAdd();
    * icon.setSize(32, 32);
    * draw.register();
    * </pre>
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
    * sets the tint, alpha and all, from one packed number.
    * <p>
    * A colour that arrives with no alpha of its own is made opaque rather than
    * invisible, so {@code 0xFF0000} is stored as {@code 0xFFFF0000}. Pure black is the
    * one thing left alone, because there is no colour there to make opaque and
    * {@code 0x000000} stays fully transparent, which on a texture is usually what
    * someone means by it.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).buildAndAdd();
    * icon.setColor(0xFF0000);
    * draw.register();
    * Chat.log(`0x${icon.getColor().toString(16)}`);
    * </pre>
    *
    * @param color the packed colour, with the alpha in the top byte
    * @return self for chaining.
    * @since 1.6.5
    */
    public Image setColor(int color) {
        this.color = ColorUtil.fixAlpha(color);
        return this;
    }

    /**
    * sets the tint and the transparency separately.
    * <p>
    * The alpha replaces the top byte of the colour rather than being blended with it.
    * A value outside 0 to 255 is not clamped: over 255 shifts into the colour channels
    * and exactly 256 wraps round to no alpha at all. On a texture this is how a piece
    * is made partly see-through, which the one-argument form would not do to a colour
    * with no alpha.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).buildAndAdd();
    * icon.setColor(0xFF0000, 128);
    * draw.register();
    * Chat.log(`alpha ${icon.getAlpha()}`);
    * </pre>
    *
    * @param color the colour, whose top byte is replaced
    * @param alpha the alpha, in the top byte
    * @return self for chaining.
    * @since 1.6.5
    */
    public Image setColor(int color, int alpha) {
        this.color = (alpha << 24) | (color & 0xFFFFFF);
        return this;
    }

    /**
    * the tint, packed with the alpha in the top byte.
    * <p>
    * What the element actually holds, which is not always what was last handed to a
    * setter: a colour given with no alpha reads back opaque. On a texture this is a
    * multiply over what is already in the texture rather than a replacement of it, so a
    * white texture takes the tint and a coloured one does not.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).buildAndAdd();
    * draw.register();
    * Chat.log(`0x${icon.getColor().toString(16)}`);
    * </pre>
    *
    * @return the color of this image.
    * @since 1.8.4
    */
    public int getColor() {
        return color;
    }

    /**
    * the transparency of this image, which is the top byte of the tint.
    * <p>
    * From 0 for invisible to 255 for opaque. A colour set without an alpha of its own
    * reads back as 255 rather than 0, unless it was pure black.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).buildAndAdd();
    * // the element has no alpha setter of its own, so the colour is set with one
    * icon.setColor(0xFF0000, 64);
    * draw.register();
    * Chat.log(`alpha ${icon.getAlpha()}`);
    * </pre>
    *
    * @return the alpha value of this image.
    * @since 1.8.4
    */
    public int getAlpha() {
        return (color >> 24) & 0xFF;
    }

    /**
    * turns this image by an angle in degrees.
    * <p>
    * Finite angles are normalized to {@code [-180, 180)}, so 450 and -270 both read back
    * as 90. Constructors apply the same normalization.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(20, 20).size(16, 16).buildAndAdd();
    * icon.setRotateCenter(true).setRotation(45);
    * draw.register();
    * Chat.log(`reads back as ${icon.getRotation()}`);
    * </pre>
    *
    * @param rotation the angle in degrees
    * @return self for chaining.
    * @since 1.2.6
    */
    public Image setRotation(double rotation) {
        this.rotation = Mth.wrapDegrees((float) rotation);
        return this;
    }

    /**
    * the rotation on this image in degrees, folded into a single turn.
    * <p>
    * Constructors and setters normalize finite angles to {@code [-180, 180)}. Direct
    * writes to the public field bypass that normalization.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(20, 20).size(16, 16).rotation(450).buildAndAdd();
    * draw.register();
    * Chat.log(`reads back as ${icon.getRotation()}`);
    * </pre>
    *
    * @return the rotation of this image.
    * @since 1.8.4
    */
    public float getRotation() {
        return rotation;
    }

    /**
    * chooses whether the rotation is about the middle of this image or about its
    * position.
    * <p>
    * An image made directly turns about its position and an image from a builder turns
    * about the middle, so this is what makes the two agree. The middle is half the
    * drawn size, so the two halves of the destination rather than of the region.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(20, 20).size(16, 16).buildAndAdd();
    * icon.setRotateCenter(true).setRotation(90);
    * draw.register();
    * </pre>
    *
    * @param rotateCenter whether the image should be rotated around its center
    * @return self for chaining.
    * @since 1.8.4
    */
    public Image setRotateCenter(boolean rotateCenter) {
        this.rotateCenter = rotateCenter;
        return this;
    }

    /**
    * whether the rotation on this image is about its middle.
    * <p>
    * False on an image made directly and true on one from a builder, since only the
    * builder sets it.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(20, 20).size(16, 16).buildAndAdd();
    * draw.register();
    * Chat.log(`turns about the middle: ${icon.isRotatingCenter()}`);
    * </pre>
    *
    * @return {@code true} if this image should be rotated around its center, {@code false}
    * otherwise.
    * @since 1.8.4
    */
    public boolean isRotatingCenter() {
        return rotateCenter;
    }

    /**
    * sets the number this image is ordered against the other elements on its overlay,
    * lower drawn first.
    * <p>
    * Two images drawn over the same place are ordered by this and by nothing else, and
    * a tie keeps the order they were added in.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const under = draw.imageBuilder("minecraft:item/diamond").pos(0, 0).size(16, 16).buildAndAdd();
    * const over = draw.imageBuilder("minecraft:item/diamond").pos(0, 0).size(16, 16).buildAndAdd();
    * under.setZIndex(2);
    * over.setZIndex(1);
    * draw.register();
    * </pre>
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
    * the number this image is ordered against the other elements on its overlay.
    * <p>
    * Zero is what an image comes with, and the overlay sorts by this lowest first.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).buildAndAdd();
    * icon.setZIndex(3);
    * draw.register();
    * Chat.log(`z-index ${icon.getZIndex()}`);
    * </pre>
    *
    * @return the z-index of this element
    */
    @Override
    public int getZIndex() {
        return zIndex;
    }

    /**
    * draws this image, and is called by the overlay rather than by a script.
    * <p>
    * The element's own transform is put on first, so the position, the turn and the
    * tint land on the piece. The 2D draw is given the offset as a fraction of the
    * texture, worked out from the pixel offset and the texture size, together with the
    * destination size and the texture size, and it scales the texture to fit. That is
    * why the region size is not part of it: on a 2D overlay a piece of the given
    * destination size is drawn from the given offset, whether or not a region was set.
    * <p>
    * The mouse arguments are not used: they are the real mouse on a screen and zero on a
    * plain overlay, and neither is looked at here.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).buildAndAdd();
    * // the overlay calls render on it; a script does not
    * draw.register();
    * </pre>
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
    * binds this image to the overlay it belongs to, which is what the parent width and
    * height are then read from.
    * <p>
    * The element builders call this as they build, so an image that has been built is
    * already bound. Binding is what makes the parent-relative align methods measure
    * against that overlay rather than against the window, and {@code null} puts it back
    * to the window.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).build();
    * // a built image is already bound, and can be bound again by hand
    * icon.setParent(draw);
    * draw.reAddElement(icon);
    * draw.register();
    * </pre>
    *
    * @param parent the overlay this element is on, or {@code null} for the window
    * @return self for chaining.
    */
    public Image setParent(IDraw2D<?> parent) {
        this.parent = parent;
        return this;
    }

    /**
    * the width of this image as it is drawn, which is the same as the width.
    * <p>
    * There is no scale on an image, so this is exactly {@code getWidth}, and it is here
    * because the align methods measure against it.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(32, 32).buildAndAdd();
    * draw.register();
    * Chat.log(`drawn ${icon.getScaledWidth()} wide`);
    * </pre>
    *
    * @return the scaled width of this element
    */
    @Override
    public int getScaledWidth() {
        return width;
    }

    /**
    * the width of the overlay this image is on, or the window's width if it has not been
    * put on one.
    * <p>
    * This is what the parent-relative align methods measure against, so an image that
    * has not been added still aligns against the screen.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).buildAndAdd();
    * draw.register();
    * Chat.log(`measured against ${icon.getParentWidth()}`);
    * </pre>
    *
    * @return the width of the parent
    */
    @Override
    public int getParentWidth() {
        return parent != null ? parent.getWidth() : mc.getWindow().getGuiScaledWidth();
    }

    /**
    * the height of this image as it is drawn, which is the same as the height.
    * <p>
    * The counterpart of {@code getScaledWidth}, and the same number as
    * {@code getHeight} for the same reason: there is no scale on an image.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(32, 32).buildAndAdd();
    * draw.register();
    * Chat.log(`drawn ${icon.getScaledHeight()} tall`);
    * </pre>
    *
    * @return the scaled height of this element
    */
    @Override
    public int getScaledHeight() {
        return height;
    }

    /**
    * the height of the overlay this image is on, or the window's height if it has not
    * been put on one.
    * <p>
    * The counterpart of {@code getParentWidth}, and read the same way.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).buildAndAdd();
    * draw.register();
    * Chat.log(`measured against ${icon.getParentHeight()}`);
    * </pre>
    *
    * @return the height of the parent
    */
    @Override
    public int getParentHeight() {
        return parent != null ? parent.getHeight() : mc.getWindow().getGuiScaledHeight();
    }

    /**
    * the left edge of this image, which is its x position.
    * <p>
    * The same as {@code getX}. There is no scale, and the destination rather than the
    * region is what is measured, so the width covered is {@code getScaledWidth} from
    * here.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(40, 20).size(16, 16).buildAndAdd();
    * draw.register();
    * Chat.log(`from ${icon.getScaledLeft()} to ${icon.getScaledRight()}`);
    * </pre>
    *
    * @return the position of the scaled element's left side
    */
    @Override
    public int getScaledLeft() {
        return x;
    }

    /**
    * the top edge of this image, which is its y position.
    * <p>
    * The counterpart of {@code getScaledLeft}, and the same as {@code getY}.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(40, 20).size(16, 16).buildAndAdd();
    * draw.register();
    * Chat.log(`from ${icon.getScaledTop()} to ${icon.getScaledBottom()}`);
    * </pre>
    *
    * @return the position of the scaled element's top side
    */
    @Override
    public int getScaledTop() {
        return y;
    }

    /**
    * puts this image at a position and keeps the size it has.
    * <p>
    * The same as {@code setPos(x, y, width, height)}, so the destination size survives
    * being moved. This is what the align methods call.
    * example:
    * <pre>
    * const draw = Hud.createDraw2D();
    * const icon = draw.imageBuilder("minecraft:item/diamond").pos(0, 0).size(16, 16).buildAndAdd();
    * icon.moveTo(40, 20);
    * draw.register();
    * Chat.log(`at ${icon.getX()}, ${icon.getY()} and still ${icon.getWidth()} wide`);
    * </pre>
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
    * the builder for an image, made by the overlay's own {@code imageBuilder} method.
    * <p>
    * This is where the four sizes are set, and it is worth holding on to which is
    * which. {@code width} and {@code height} are the destination, the size the piece is
    * drawn at. {@code regions} sets where in the texture to start and how much of it to
    * take, and {@code textureSize} sets how big the texture itself is, which is what
    * those offsets are a fraction of. That last one starts at 256, which is the size of
    * a vanilla texture sheet, so a sheet that is a different size needs it set.
    * <p>
    * The position starts at zero, the destination size starts at zero so an image built
    * with nothing but a texture draws nothing, the colour starts opaque white, the
    * rotation starts at zero and is not folded into a single turn, and the rotation is
    * about the middle of the image until {@code rotateCenter} says otherwise. That last
    * one is the opposite of an image made directly.
    * <p>
    * A texture with nothing in the identifier is remembered and drawn as nothing rather
    * than refused, so an image can be built now and pointed at a texture later.
    * example:
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
        * makes a builder for images on one overlay.
        * <p>
        * A script does not call this directly: the overlay's own {@code imageBuilder}
        * method is what fills in the overlay, and its one-argument form sets the
        * identifier as it goes.
        *
        * @param draw2D the overlay the images will be added to
        */
        public Builder(IDraw2D<?> draw2D) {
            super(draw2D);
        }

        /**
        * sets the texture, the region and the texture size all at once from a custom
        * image.
        * <p>
        * Will automatically set all attributes to the default values of the custom
        * image. Values set before the call of this method will be overwritten.
        * <p>
        * Which attributes those are, read off the body: the region is taken to be the
        * whole image, the texture size is the image's own width and height rather than
        * 256, the destination size is the image's size too, and the position is set
        * back to zero. So the position has to be set after this rather than before.
        * example:
        * <pre>
        * // a canvas made by the game, painted and then drawn onto an overlay
        * const custom = Hud.createTexture(16, 16, "my icon");
        * custom.setGraphicsColor(0xFFFF0000);
        * custom.fillRect(0, 0, 16, 16);
        *
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder().fromCustomImage(custom).pos(10, 10).buildAndAdd();
        * draw.register();
        * </pre>
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
        * names the texture to draw from, without touching anything else.
        * <p>
        * The id goes through the usual parser, so a bare name gets the {@code minecraft}
        * namespace. This is the only thing the builder needs before it will draw, since
        * the destination size can be set separately and a texture with no name draws
        * nothing rather than failing.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder("item/diamond").pos(10, 10).size(16, 16).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the texture this builder will draw from, or {@code null} if none has been
        * named.
        * <p>
        * The identifier as it was given, rather than the full resolved form the
        * built image reports, so a bare name reads back the way it was typed.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder("item/diamond");
        * Chat.log(`named ${builder.getIdentifier()}`);
        * </pre>
        *
        * @return the identifier of the used image or {@code null} if no image is used.
        * @since 1.8.4
        */
        public String getIdentifier() {
            return identifier;
        }

        /**
        * puts the image at an x position, and leaves the y position alone.
        * <p>
        * Zero is where an element starts, and neither the destination size nor the
        * texture side is changed by moving it.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder("minecraft:item/diamond").x(40).y(20).size(16, 16).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the x position this builder will build the image at.
        * <p>
        * Zero until an {@code x} or a {@code pos} call says otherwise, and note that
        * {@code fromCustomImage} sets it back to zero.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder().x(40);
        * Chat.log(`will be built at x ${builder.getX()}`);
        * </pre>
        *
        * @return the x position of the image.
        * @since 1.8.4
        */
        public int getX() {
            return x;
        }

        /**
        * puts the image at a y position, and leaves the x position alone.
        * <p>
        * The counterpart of {@code x}, and nothing else is changed by moving it.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder("minecraft:item/diamond").y(20).size(16, 16).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the y position this builder will build the image at.
        * <p>
        * Zero until a {@code y} or a {@code pos} call says otherwise, and note that
        * {@code fromCustomImage} sets it back to zero.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder().y(20);
        * Chat.log(`will be built at y ${builder.getY()}`);
        * </pre>
        *
        * @return the y position of the image.
        * @since 1.8.4
        */
        public int getY() {
            return y;
        }

        /**
        * puts the image at a position, and leaves the size and the texture side alone.
        * <p>
        * Both coordinates are set and nothing else is, so this is where the piece is
        * drawn rather than what is drawn or how big.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).buildAndAdd();
        * draw.register();
        * </pre>
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
        * sets the width the piece is drawn at, which starts at zero.
        * <p>
        * The destination size and not the width of the region being taken from the
        * texture: a 16 wide icon drawn at 32 wide is a bigger icon rather than twice as
        * much texture. Nothing is checked, so a negative width is accepted.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder("minecraft:item/diamond").pos(10, 10).width(32).height(32).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the width this builder will build the image at.
        * <p>
        * Zero until a {@code width} or a {@code size} call says otherwise, and the
        * destination size rather than the region.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder().size(32, 32);
        * Chat.log(`will be built ${builder.getWidth()} wide`);
        * </pre>
        *
        * @return the width of the image.
        * @since 1.8.4
        */
        public int getWidth() {
            return width;
        }

        /**
        * sets the height the piece is drawn at, which starts at zero.
        * <p>
        * The counterpart of {@code width}, and the same distinction between the
        * destination and the region. Nothing is checked.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder("minecraft:item/diamond").pos(10, 10).width(32).height(32).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the height this builder will build the image at.
        * <p>
        * Zero until a {@code height} or a {@code size} call says otherwise, and the
        * destination size rather than the region.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder().size(32, 32);
        * Chat.log(`will be built ${builder.getHeight()} tall`);
        * </pre>
        *
        * @return the height of the image.
        * @since 1.8.4
        */
        public int getHeight() {
            return height;
        }

        /**
        * sets the size the piece is drawn at, on both axes.
        * <p>
        * The destination size. With a builder that starts at zero this is the pair of
        * calls an image needs before it draws anything at all.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).buildAndAdd();
        * draw.register();
        * </pre>
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
        * sets the x position in the texture to start drawing from, in pixels.
        * <p>
        * An offset into the whole sheet rather than a fraction, and it is divided by the
        * texture width to make the fraction the draw wants. So a texture that is not 256
        * wide needs its texture width set as well or the offset is read against the
        * wrong size.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).imageX(208).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the x position in the texture this builder will start drawing from.
        * <p>
        * Zero until an {@code imageX} or an {@code imagePos} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder().imageX(208);
        * Chat.log(`will be built starting at ${builder.getImageX()}, ${builder.getImageY()}`);
        * </pre>
        *
        * @return the x position in the image texture to start drawing from.
        * @since 1.8.4
        */
        public int getImageX() {
            return imageX;
        }

        /**
        * sets the y position in the texture to start drawing from, in pixels.
        * <p>
        * The counterpart of {@code imageX}, divided by the texture height the same way.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).imageY(208).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the y position in the texture this builder will start drawing from.
        * <p>
        * Zero until an {@code imageY} or an {@code imagePos} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder().imageY(208);
        * Chat.log(`will be built starting at ${builder.getImageX()}, ${builder.getImageY()}`);
        * </pre>
        *
        * @return the y position in the image texture to start drawing from.
        * @since 1.8.4
        */
        public int getImageY() {
            return imageY;
        }

        /**
        * sets where in the texture to start drawing from, in both directions.
        * <p>
        * The counterpart of the two single-axis calls, and the two numbers are pixel
        * offsets into the whole texture rather than a coordinate between 0 and 1.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).imagePos(208, 0).buildAndAdd();
        * draw.register();
        * </pre>
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
        * sets the width of the region being taken from the texture, which starts at
        * zero.
        * <p>
        * This one is read by the 3D path rather than the 2D one, so on a 2D overlay it
        * is remembered and changes nothing that is drawn: the 2D draw is given the
        * destination size and the texture size and works the far corner out itself. It
        * matters for a surface in the world, where the region is the piece that is
        * taken.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder("minecraft:item/diamond").regionWidth(16);
        * Chat.log(`region ${builder.getRegionWidth()} by ${builder.getRegionHeight()}`);
        * </pre>
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
        * the width of the region this builder will take from the texture.
        * <p>
        * Zero until a {@code regionWidth} or a {@code regionSize} call says otherwise,
        * and not the width the piece is drawn at.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder("minecraft:item/diamond").regionWidth(16);
        * Chat.log(`region ${builder.getRegionWidth()} wide, drawn at ${builder.getWidth()}`);
        * </pre>
        *
        * @return the width of the region to draw.
        * @since 1.8.4
        */
        public int getRegionWidth() {
            return regionWidth;
        }

        /**
        * sets the height of the region being taken from the texture, which starts at
        * zero.
        * <p>
        * The counterpart of {@code regionWidth}, and read by the 3D path rather than
        * the 2D one in the same way.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder("minecraft:item/diamond").regionHeight(16);
        * Chat.log(`region ${builder.getRegionWidth()} by ${builder.getRegionHeight()}`);
        * </pre>
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
        * the height of the region this builder will take from the texture.
        * <p>
        * Zero until a {@code regionHeight} or a {@code regionSize} call says otherwise,
        * and not the height the piece is drawn at.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder("minecraft:item/diamond").regionHeight(16);
        * Chat.log(`region ${builder.getRegionHeight()} tall, drawn at ${builder.getHeight()}`);
        * </pre>
        *
        * @return the height of the region to draw.
        * @since 1.8.4
        */
        public int getRegionHeight() {
            return regionHeight;
        }

        /**
        * sets the size of the region being taken from the texture, on both axes.
        * <p>
        * As with the region size on its own, this is what the 3D path uses to find the
        * far corner of the piece and the 2D path does not read.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).regionSize(16, 16).buildAndAdd();
        * draw.register();
        * </pre>
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
        * sets where in the texture to start and how much of it to take, in one call.
        * <p>
        * All four numbers at once, and the texture size is left as it was, which starts
        * at 256. So this is the form for a vanilla sheet, and a texture of another size
        * wants the six-argument form or a {@code textureSize} call as well.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).regions(208, 0, 16, 16).buildAndAdd();
        * draw.register();
        * </pre>
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
        * sets the region and the size of the texture it comes from, all six numbers.
        * <p>
        * The complete form of the texture side, and the one to use for a sheet that is
        * not 256 by 256, since the offsets are read as a fraction of the texture size
        * and a wrong texture size puts the piece in the wrong place rather than
        * refusing.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).regions(208, 0, 16, 16, 256, 256).buildAndAdd();
        * draw.register();
        * </pre>
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
        * sets the width of the whole texture, which starts at 256.
        * <p>
        * This is what the offset in the texture is a fraction of, so getting it wrong
        * draws the piece from the wrong place rather than failing. 256 is the size of a
        * vanilla texture sheet, so this only needs setting for a texture of another
        * size.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder("my_pack:panel").pos(10, 10).size(16, 16).regions(0, 0, 16, 16).textureWidth(64).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the width of the texture this builder will draw from.
        * <p>
        * 256 to begin with, the size of a vanilla texture sheet, so a builder that has
        * been given nothing but a texture reports that.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder("minecraft:item/diamond");
        * Chat.log(`texture is ${builder.getTextureWidth()} by ${builder.getTextureHeight()}`);
        * </pre>
        *
        * @return the width of the used texture.
        * @since 1.8.4
        */
        public int getTextureWidth() {
            return textureWidth;
        }

        /**
        * sets the height of the whole texture, which starts at 256.
        * <p>
        * The counterpart of {@code textureWidth}, and the same reason for setting it:
        * the offset in the texture is a fraction of it.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder("my_pack:panel").pos(10, 10).size(16, 16).regions(0, 0, 16, 16).textureHeight(64).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the height of the texture this builder will draw from.
        * <p>
        * 256 to begin with, the size of a vanilla texture sheet, the counterpart of
        * {@code getTextureWidth}.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder("minecraft:item/diamond");
        * Chat.log(`texture is ${builder.getTextureWidth()} by ${builder.getTextureHeight()}`);
        * </pre>
        *
        * @return the height of the used texture.
        * @since 1.8.4
        */
        public int getTextureHeight() {
            return textureHeight;
        }

        /**
        * sets the size of the whole texture, on both axes, and it starts at 256 by 256.
        * <p>
        * This is the size the offsets into the texture are measured against, so it is
        * the pair to set for a sheet that is not 256 by 256 rather than the pair that
        * decides how big the piece is drawn.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder("my_pack:panel").pos(10, 10).size(16, 16).regions(0, 0, 16, 16).textureSize(64, 64).buildAndAdd();
        * draw.register();
        * </pre>
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
        * sets the tint from one packed number, and takes its alpha from it as well.
        * <p>
        * The alpha is read out of the number and kept separately, so a later
        * {@code alpha} call changes the transparency without touching the colour. A
        * colour with no alpha of its own comes out opaque unless it was pure black, and
        * on a texture the tint multiplies what is already in the texture.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).color(0x80FF0000).buildAndAdd();
        * draw.register();
        * </pre>
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
        * sets the tint from three components, each from 0 to 255.
        * <p>
        * The three are packed into the low 24 bits and the separate alpha is left as it
        * was, which is opaque unless something has changed it. So the number this
        * builder reports from {@code getColor} is {@code 0x00RRGGBB} while the built
        * image is {@code 0xFFRRGGBB}, the alpha being put back on when the element is
        * made. The four-component form is the one to use when the transparency has to
        * travel with the colour.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const icon = draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).color(255, 128, 0).buildAndAdd();
        * draw.register();
        * Chat.log(`built at 0x${icon.getColor().toString(16)}`);
        * </pre>
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
        * sets the tint from four components, each from 0 to 255.
        * <p>
        * The same three-component packing with the fourth going into the separate
        * alpha, so as with the shorter form the number this builder reports is the low
        * 24 bits and the built image has the alpha put back on.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const icon = draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).color(255, 128, 0, 128).buildAndAdd();
        * draw.register();
        * Chat.log(`built at 0x${icon.getColor().toString(16)}`);
        * </pre>
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
        * sets the tint and the alpha as two separate numbers.
        * <p>
        * Both stored as given with no fix-up, so this is the form that will hold a
        * colour whose alpha byte is zero, which the one-argument form would make opaque.
        * The two are combined when the image is built.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).color(0xFF0000, 128).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the tint this builder will build the image with, as it is stored here.
        * <p>
        * After {@code color(int)} and {@code color(int, int)} this is the full packed
        * number, since the alpha was pulled out of it into its own field. After the
        * three- and four-component forms it is the low 24 bits on their own, so it
        * reads as {@code 0x00RRGGBB} and not as the {@code 0xFFRRGGBB} the built image
        * ends up with. The built image's own {@code getColor} is the honest one.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder("minecraft:item/diamond").color(255, 0, 0);
        * Chat.log(`builder says 0x${builder.getColor().toString(16)}`);
        * const icon = builder.pos(10, 10).size(16, 16).buildAndAdd();
        * draw.register();
        * Chat.log(`the image says 0x${icon.getColor().toString(16)}`);
        * </pre>
        *
        * @return the color of the image.
        * @since 1.8.4
        */
        public int getColor() {
            return color;
        }

        /**
        * sets only the transparency, leaving the colour alone.
        * <p>
        * The number is not clamped, so a value over 255 is a shift into the colour
        * channels and 256 is no alpha at all. This is the form to use for a texture with
        * colour in it, where the one-argument colour setter would make an alpha of zero
        * opaque instead.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const icon = draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).color(0xFF0000).alpha(64).buildAndAdd();
        * draw.register();
        * Chat.log(`built at alpha ${icon.getAlpha()}`);
        * </pre>
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
        * the transparency this builder will build the image with, from 0 to 255.
        * <p>
        * Opaque is what it starts at, and it is a number of its own here rather than
        * being read out of the colour. So the three- and four-component colour forms
        * leave it alone, and a builder can report an alpha the built image does not
        * have.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder("minecraft:item/diamond").color(0xFF0000).alpha(64);
        * Chat.log(`will be built at alpha ${builder.getAlpha()}`);
        * </pre>
        *
        * @return the alpha value of the color.
        * @since 1.8.4
        */
        public int getAlpha() {
            return alpha;
        }

        /**
        * turns the image by an angle in degrees.
        * <p>
        * Stored as given rather than folded into a single turn, so this reads back as
        * 450 on the builder. The built image's constructor normalizes it to 90.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder("minecraft:item/diamond").pos(20, 20).size(16, 16).rotation(45).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the rotation this builder will build the image at, in degrees.
        * <p>
        * Zero until a {@code rotation} call says otherwise, and not folded into a
        * single turn.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder().rotation(45);
        * Chat.log(`will be built turned ${builder.getRotation()} degrees`);
        * </pre>
        *
        * @return the rotation (clockwise) of the image in degrees.
        * @since 1.8.4
        */
        public float getRotation() {
            return rotation;
        }

        /**
        * chooses whether the rotation is about the middle of the image or about its
        * position.
        * <p>
        * True is what a builder starts at, which is the opposite of an image made
        * directly, so the two turn differently until one of them is told otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const icon = draw.imageBuilder("minecraft:item/diamond").pos(20, 20).size(16, 16).rotateCenter(false).buildAndAdd();
        * draw.register();
        * Chat.log(`turns about its position: ${!icon.isRotatingCenter()}`);
        * </pre>
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
        * whether the image this builder builds will turn about its middle.
        * <p>
        * True until a {@code rotateCenter} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder();
        * Chat.log(`builds turning about the middle: ${builder.isRotatingCenter()}`);
        * </pre>
        *
        * @return {@code true} if this image should be rotated around its center,
        * {@code false} otherwise.
        * @since 1.8.4
        */
        public boolean isRotatingCenter() {
            return rotateCenter;
        }

        /**
        * sets the number the image will be ordered against the other elements on its
        * overlay, lower drawn first.
        * <p>
        * Zero is what a builder starts at, and a tie keeps the order the elements were
        * added in.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).zIndex(5).buildAndAdd();
        * draw.register();
        * </pre>
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
        * the z-index this builder will build the image at.
        * <p>
        * Zero until a {@code zIndex} call says otherwise.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder().zIndex(5);
        * Chat.log(`will be built at z-index ${builder.getZIndex()}`);
        * </pre>
        *
        * @return the z-index of the image.
        * @since 1.8.4
        */
        public int getZIndex() {
            return zIndex;
        }

        /**
        * makes the image out of everything set on this builder, and binds it to the
        * overlay this builder was made from.
        * <p>
        * A new image each time, so the same builder can build more than one. This is
        * what {@code build} and {@code buildAndAdd} call.
        * <p>
        * The colour is put together here: the separate alpha becomes the top byte and
        * the low 24 bits of the stored tint are kept, which is why a builder set with
        * one of the component colour forms comes out opaque.
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
        * the width this builder will build the image at, which is the width as drawn.
        * <p>
        * There is no scale on an image, so this is the same number as
        * {@code getWidth}, and it is here because the align methods measure against it.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder().size(32, 32);
        * Chat.log(`will be drawn ${builder.getScaledWidth()} wide, ${builder.getWidth()} set`);
        * </pre>
        *
        * @return the scaled width
        */
        @Override
        public int getScaledWidth() {
            return width;
        }

        /**
        * the width of the overlay this builder was made from, which is what the
        * parent-relative align methods measure against.
        * <p>
        * There is no window fallback here, because a builder always has an overlay.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder();
        * Chat.log(`measuring against ${builder.getParentWidth()}`);
        * </pre>
        *
        * @return the width of the parent
        */
        @Override
        public int getParentWidth() {
            return parent.getWidth();
        }

        /**
        * the height this builder will build the image at, which is the height as drawn.
        * <p>
        * The counterpart of {@code getScaledWidth}, and the same number as
        * {@code getHeight} for the same reason.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder().size(32, 32);
        * Chat.log(`will be drawn ${builder.getScaledHeight()} tall, ${builder.getHeight()} set`);
        * </pre>
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
        * <p>
        * The counterpart of {@code getParentWidth}, and with no window fallback either.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder();
        * Chat.log(`measuring against ${builder.getParentHeight()}`);
        * </pre>
        *
        * @return the height of the parent
        */
        @Override
        public int getParentHeight() {
            return parent.getHeight();
        }

        /**
        * the x position this builder will build the image at.
        * <p>
        * The same as {@code getX}: there is no scale on an image, and what is measured
        * is the destination rather than the region.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder().pos(40, 20);
        * Chat.log(`will be at ${builder.getScaledLeft()}, ${builder.getScaledTop()}`);
        * </pre>
        *
        * @return the position of the scaled element's left side
        */
        @Override
        public int getScaledLeft() {
            return x;
        }

        /**
        * the y position this builder will build the image at.
        * <p>
        * The counterpart of {@code getScaledLeft}, and the same as {@code getY}.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder().pos(40, 20);
        * Chat.log(`will be at ${builder.getScaledLeft()}, ${builder.getScaledTop()}`);
        * </pre>
        *
        * @return the position of the scaled element's top side
        */
        @Override
        public int getScaledTop() {
            return y;
        }

        /**
        * puts the image at a position, which is the same as {@code pos}.
        * <p>
        * Nothing is resized, so the destination size and the texture side both survive
        * being moved. This is what the align methods call.
        * example:
        * <pre>
        * const draw = Hud.createDraw2D();
        * const builder = draw.imageBuilder("minecraft:item/diamond").size(16, 16);
        * builder.moveTo(40, 20);
        * builder.buildAndAdd();
        * draw.register();
        * Chat.log(`will be built at ${builder.getX()}, ${builder.getY()}`);
        * </pre>
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
