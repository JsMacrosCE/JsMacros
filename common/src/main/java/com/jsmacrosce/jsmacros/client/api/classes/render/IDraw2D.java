package com.jsmacrosce.jsmacros.client.api.classes.render;

import net.minecraft.client.gui.GuiGraphics;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.jsmacros.client.api.classes.render.components.*;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

import java.util.List;
import java.util.function.Consumer;

/**
 * A 2D overlay that holds render elements; implemented by standalone overlays and screens.
 * <p>
 * Both implementations expose the same element API.
 * <p>
 * <pre>
 * // a plain overlay, the usual implementation
 * const draw = Hud.createDraw2D();
 * draw.addText("hello", 10, 10, Color.rgb(0xFF, 0xFF, 0xFF), true);
 * draw.register();
 * </pre>
 *
 * @param <T> the type returned by removal methods
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Rendering/Graphics")
public interface IDraw2D<T> {

    /**
     * Returns the current width supplied by this overlay, in its element coordinate
     * space. Plain overlays default to the window's GUI-scaled width; screens use their
     * current screen width, nested overlays their supplied width, and surfaces surface
     * pixels. A screen's width can differ from the window's GUI-scaled width.
     *
     * @return the supplied width
     * @since 1.2.7
     */
    int getWidth();

    /**
     * Returns the current height supplied by this overlay, in its element coordinate
     * space. Plain overlays default to the window's GUI-scaled height; screens use their
     * current screen height, nested overlays their supplied height, and surfaces surface
     * pixels.
     *
     * @return the supplied height
     * @since 1.2.7
     */
    int getHeight();

    /**
     * Returns this overlay's text elements in insertion order; the returned list is independent of the overlay.
     * <p>
     * A new list each call, filtered by type, so changing it does not change what this
     * overlay holds. Nested overlays are not included, so text inside a panel is not in
     * here even though it is drawn.
     * <p>
     * Deprecated only because the per-type getters were added after this one; it is the
     * same kind of thing and is not going anywhere.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addText("one", 10, 10, Color.rgb(0xFF, 0xFF, 0xFF), true);
     * draw.register();
     * Chat.log(`${draw.getTexts().size()} texts`);
     * </pre>
     *
     * @return text elements
     * @since 1.2.7
     */
    @DocletIgnore
    @Deprecated
    List<Text> getTexts();

    /**
     * Returns this overlay's rectangles in insertion order; the returned list is independent of the overlay.
     * <p>
     * A new list each call, filtered by type, so changing it does not change what this
     * overlay holds. Nested overlays are not included.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addRect(0, 0, 10, 10, Color.rgb(0xFF, 0x00, 0x00));
     * draw.register();
     * Chat.log(`${draw.getRects().size()} rects`);
     * </pre>
     *
     * @return rect elements
     * @since 1.2.7
     */
    @DocletIgnore
    @Deprecated
    List<Rect> getRects();

    /**
     * Returns a new, mutable list of this overlay's line elements in insertion order.
     * Nested overlays' contents are not included.
     *
     * @return all registered line elements.
     * @since 1.8.4
     */
    List<Line> getLines();

    /**
     * Returns this overlay's item icons in insertion order; the returned list is independent of the overlay.
     * <p>
     * A new list each call, filtered by type, so changing it does not change what this
     * overlay holds. Nested overlays are not included.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addItem(10, 10, "minecraft:diamond");
     * draw.register();
     * Chat.log(`${draw.getItems().size()} items`);
     * </pre>
     *
     * @return item elements
     * @since 1.2.7
     */
    @DocletIgnore
    @Deprecated
    List<Item> getItems();

    /**
     * Returns this overlay's images in insertion order; the returned list is independent of the overlay.
     * <p>
     * A new list each call, filtered by type, so changing it does not change what this
     * overlay holds. Nested overlays are not included.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addImage(10, 10, 16, 16, "minecraft:item/diamond", 0, 0, 16, 16, 16, 16);
     * draw.register();
     * Chat.log(`${draw.getImages().size()} images`);
     * </pre>
     *
     * @return image elements
     * @since 1.2.7
     */
    @DocletIgnore
    @Deprecated
    List<Image> getImages();

    /**
     * Returns a new, mutable list of nested-overlay wrappers in insertion order. Each
     * wrapper carries the nested overlay's placement and displayed dimensions; nested
     * overlays and their contents are not returned directly.
     *
     * @return all registered draw2d elements.
     * @since 1.8.4
     */
    List<Draw2DElement> getDraw2Ds();

    /**
     * Returns an immutable snapshot of all elements in insertion order. It does not
     * reflect elements added or removed afterward; the elements themselves are not
     * made immutable. Nested overlays appear as wrappers, and their contents are not
     * included.
     *
     * @return an immutable snapshot of this overlay's elements
     * @since 1.2.9
     */
    List<RenderElement> getElements();

    /**
     * Removes an element of any type from this overlay. If absent, it has no effect;
     * the element itself is unchanged and can be added again with
     * {@link #reAddElement(RenderElement)}. The deprecated typed removals do the same.
     *
     * @param e the element to take off
     * @return self for chaining
     * @since 1.2.9
     */
    T removeElement(RenderElement e);

    /**
     * Adds an element to this overlay and returns it. This can restore an element removed
     * with {@link #removeElement(RenderElement)} or add one built without
     * {@code buildAndAdd()}. For nested-overlay wrappers, returns {@code null} if the
     * wrapped overlay is missing, is this overlay, or would create a cycle.
     *
     * @param <T> the type of element to add
     * @param e the element to put back
     * @return the element, or {@code null} if it is a nested overlay that was refused
     * @since 1.2.9
     */
    <T extends RenderElement> T reAddElement(T e);

    /**
     * Adds text at a position, with a color and optional shadow.
     * <p>
     * Coordinates are screen pixels from the top left; {@code color} is packed with
     * alpha in its top byte. This overload uses z-index 0, scale 1, and no rotation.
     *
     * @param text the text to display
     * @param x      screen x
     * @param y      screen y
     * @param color  text color
     * @param shadow include shadow layer
     * @return added text
     * @since 1.2.7
     */
    Text addText(String text, int x, int y, int color, boolean shadow);

    /**
     * Adds text at a position with a color and optional shadow. Coordinates are screen
     * pixels from the top left; {@code color} is packed with alpha in its top byte.
     * Although this overload accepts {@code zIndex}, it currently uses z-index 0.
     * Scale is 1 and rotation is 0.
     *
     * @param text the text to display
     * @param x      screen x
     * @param y      screen y
     * @param color  text color
     * @param zIndex currently ignored; the added text uses z-index 0
     * @param shadow include shadow layer
     * @return added text
     * @since 1.4.0
     */
    Text addText(String text, int x, int y, int color, int zIndex, boolean shadow);

    /**
     * Adds text at a position, with a color, optional shadow, scale, and rotation.
     * Coordinates are screen pixels from the top left; {@code color} is packed with
     * alpha in its top byte. This overload uses z-index 0.
     *
     * @param text the text to display
     * @param x        screen x
     * @param y        screen y
     * @param color    text color
     * @param shadow   include shadow layer
     * @param scale    text scale (as double)
     * @param rotation text rotation (as degrees)
     * @return added text
     * @since 1.2.7
     */
    Text addText(String text, int x, int y, int color, boolean shadow, double scale, double rotation);

    /**
     * Adds text at a position, with a color, optional shadow, scale, and rotation,
     * at the specified z-index. Coordinates are screen pixels from the top left;
     * {@code color} is packed with alpha in its top byte.
     *
     * @param text the text to display
     * @param x        screen x
     * @param y        screen y
     * @param color    text color
     * @param zIndex   z-index
     * @param shadow   include shadow layer
     * @param scale    text scale (as double)
     * @param rotation text rotation (as degrees)
     * @return added text
     * @since 1.4.0
     */
    Text addText(String text, int x, int y, int color, int zIndex, boolean shadow, double scale, double rotation);

    /**
     * Adds styled text at a position, with a color and optional shadow.
     * Coordinates are screen pixels from the top left; {@code color} is packed
     * with alpha in its top byte. This overload uses z-index 0, scale 1, and no rotation.
     *
     * @param text the styled text to display
     * @param x      screen x
     * @param y      screen y
     * @param color  text color
     * @param shadow include shadow layer
     * @return added text
     * @since 1.2.7
     */
    Text addText(TextHelper text, int x, int y, int color, boolean shadow);

    /**
     * Adds styled text from a {@link TextHelper}; {@code color} tints its existing
     * styling. Coordinates are screen pixels from the top left. Scale is 1 and
     * rotation is 0. The supplied z-index is applied.
     *
     * @param text the styled text to display
     * @param x      screen x
     * @param y      screen y
     * @param color  tint applied to the text's existing styling
     * @param zIndex z-index
     * @param shadow include shadow layer
     * @return added text
     * @since 1.4.0
     */
    Text addText(TextHelper text, int x, int y, int color, int zIndex, boolean shadow);

    /**
     * Adds styled text at a position, with a color, optional shadow, scale, and rotation.
     * Coordinates are screen pixels from the top left; {@code color} is packed with
     * alpha in its top byte. This overload uses z-index 0.
     *
     * @param text the styled text to display
     * @param x        screen x
     * @param y        screen y
     * @param color    text color
     * @param shadow   include shadow layer
     * @param scale    text scale (as double)
     * @param rotation text rotation (as degrees)
     * @return added text
     * @since 1.2.7
     */
    Text addText(TextHelper text, int x, int y, int color, boolean shadow, double scale, double rotation);

    /**
     * Adds styled text at a position, with a color, optional shadow, scale, and rotation,
     * at the specified z-index. Coordinates are screen pixels from the top left;
     * {@code color} is packed with alpha in its top byte.
     *
     * @param text the styled text to display
     * @param x        screen x
     * @param y        screen y
     * @param color    text color
     * @param zIndex   z-index
     * @param shadow   include shadow layer
     * @param scale    text scale (as double)
     * @param rotation text rotation (as degrees)
     * @return added text
     * @since 1.4.0
     */
    Text addText(TextHelper text, int x, int y, int color, int zIndex, boolean shadow, double scale, double rotation);

    /**
     * Removes the text element from this overlay; the element itself is unchanged.
     *
     * @param t text element to remove
     * @return this overlay
     * @since 1.2.7
     */
    @DocletIgnore
    @Deprecated
    T removeText(Text t);

    /**
     * Adds an unrotated texture region as an image with the default z-index and white tint.
     *
     * @param x image's screen-space left edge
     * @param y image's screen-space top edge
     * @param width displayed width in screen pixels
     * @param height displayed height in screen pixels
     * @param id texture identifier
     * @param imageX region's left coordinate in the texture
     * @param imageY region's top coordinate in the texture
     * @param regionWidth region width in texture pixels
     * @param regionHeight region height in texture pixels
     * @param textureWidth full texture width in pixels
     * @param textureHeight full texture height in pixels
     * @return added image
     * @since 1.2.7
     */
    Image addImage(int x, int y, int width, int height, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight);

    /**
     * Adds an unrotated texture region as an image with a specified z-index and white tint.
     *
     * @param x image's screen-space left edge
     * @param y image's screen-space top edge
     * @param width displayed width in screen pixels
     * @param height displayed height in screen pixels
     * @param zIndex ordering against elements on this overlay; lower values draw first
     * @param id texture identifier
     * @param imageX region's left coordinate in the texture
     * @param imageY region's top coordinate in the texture
     * @param regionWidth region width in texture pixels
     * @param regionHeight region height in texture pixels
     * @param textureWidth full texture width in pixels
     * @param textureHeight full texture height in pixels
     * @return added image
     * @since 1.4.0
     */
    Image addImage(int x, int y, int width, int height, int zIndex, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight);

    /**
     * Adds a rotated texture region as an image with the default z-index and white tint.
     * Rotation is in degrees about the image's center.
     *
     * @param x image's screen-space left edge
     * @param y image's screen-space top edge
     * @param width displayed width in screen pixels
     * @param height displayed height in screen pixels
     * @param id texture identifier
     * @param imageX region's left coordinate in the texture
     * @param imageY region's top coordinate in the texture
     * @param regionWidth region width in texture pixels
     * @param regionHeight region height in texture pixels
     * @param textureWidth full texture width in pixels
     * @param textureHeight full texture height in pixels
     * @param rotation rotation in degrees
     * @return added image
     * @since 1.2.7
     */
    Image addImage(int x, int y, int width, int height, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight, double rotation);

    /**
     * Adds a rotated texture region as an image with a specified z-index and white tint.
     * Rotation is in degrees about the image's center.
     *
     * @param x image's screen-space left edge
     * @param y image's screen-space top edge
     * @param width displayed width in screen pixels
     * @param height displayed height in screen pixels
     * @param zIndex ordering against elements on this overlay; lower values draw first
     * @param id texture identifier
     * @param imageX region's left coordinate in the texture
     * @param imageY region's top coordinate in the texture
     * @param regionWidth region width in texture pixels
     * @param regionHeight region height in texture pixels
     * @param textureWidth full texture width in pixels
     * @param textureHeight full texture height in pixels
     * @param rotation rotation in degrees
     * @return added image
     * @since 1.4.0
     */
    Image addImage(int x, int y, int width, int height, int zIndex, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight, double rotation);

    /**
     * Adds a rotated, tinted texture region as an image; its alpha is included in {@code color}.
     * Rotation is in degrees about the image's center.
     *
     * @param x image's screen-space left edge
     * @param y image's screen-space top edge
     * @param width displayed width in screen pixels
     * @param height displayed height in screen pixels
     * @param zIndex ordering against elements on this overlay; lower values draw first
     * @param color tint, packed with alpha in the top byte
     * @param id texture identifier
     * @param imageX region's left coordinate in the texture
     * @param imageY region's top coordinate in the texture
     * @param regionWidth region width in texture pixels
     * @param regionHeight region height in texture pixels
     * @param textureWidth full texture width in pixels
     * @param textureHeight full texture height in pixels
     * @param rotation rotation in degrees
     * @return added image
     * @since 1.6.5
     */
    Image addImage(int x, int y, int width, int height, int zIndex, int color, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight, double rotation);

    /**
     * Adds a rotated, tinted texture region as an image, with alpha supplied separately
     * from the tint. Rotation is in degrees about the image's center.
     *
     * @param x image's screen-space left edge
     * @param y image's screen-space top edge
     * @param width displayed width in screen pixels
     * @param height displayed height in screen pixels
     * @param zIndex ordering against elements on this overlay; lower values draw first
     * @param alpha image alpha, used as the top byte of the packed color
     * @param color tint, whose top byte is replaced by {@code alpha}
     * @param id texture identifier
     * @param imageX region's left coordinate in the texture
     * @param imageY region's top coordinate in the texture
     * @param regionWidth region width in texture pixels
     * @param regionHeight region height in texture pixels
     * @param textureWidth full texture width in pixels
     * @param textureHeight full texture height in pixels
     * @param rotation rotation in degrees
     * @return added image
     * @since 1.6.5
     */
    Image addImage(int x, int y, int width, int height, int zIndex, int alpha, int color, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight, double rotation);

    /**
     * Removes the image element from this overlay; the element itself is unchanged.
     *
     * @param i image element to remove
     * @return this overlay
     * @since 1.2.7
     */
    @DocletIgnore
    @Deprecated
    T removeImage(Image i);

    /**
     * Adds a filled rectangle between two corners, using the packed color's alpha.
     *
     * @param x1 first corner's x coordinate
     * @param y1 first corner's y coordinate
     * @param x2 opposite corner's x coordinate
     * @param y2 opposite corner's y coordinate
     * @param color packed color; alpha is in the top byte
     * @return added rect
     * @since 1.2.7
     */
    Rect addRect(int x1, int y1, int x2, int y2, int color);

    /**
     * Adds a filled rectangle between two corners, with alpha supplied separately.
     *
     * @param x1 first corner's x coordinate
     * @param y1 first corner's y coordinate
     * @param x2 opposite corner's x coordinate
     * @param y2 opposite corner's y coordinate
     * @param color packed color; its top byte is replaced by {@code alpha}
     * @param alpha alpha value used as the packed color's top byte
     * @return added rect
     * @since 1.2.7
     */
    Rect addRect(int x1, int y1, int x2, int y2, int color, int alpha);

    /** Adds a rectangle between two corners; in this overload, rotation is ignored.
     *
     * @param x1 first corner's x coordinate
     * @param y1 first corner's y coordinate
     * @param x2 opposite corner's x coordinate
     * @param y2 opposite corner's y coordinate
     * @param color rectangle color
     * @param alpha alpha value from 0 (transparent) to 255 (opaque)
     * @param rotation ignored
     * @return the added rectangle
     * @since 1.2.7
     */
    Rect addRect(int x1, int y1, int x2, int y2, int color, int alpha, double rotation);

    /** Adds a rectangle between two corners; in this overload, rotation is ignored.
     *
     * @param x1 first corner's x coordinate
     * @param y1 first corner's y coordinate
     * @param x2 opposite corner's x coordinate
     * @param y2 opposite corner's y coordinate
     * @param color rectangle color
     * @param alpha alpha value from 0 (transparent) to 255 (opaque)
     * @param rotation ignored
     * @param zIndex draw order relative to other overlay elements; lower values draw first
     * @return the added rectangle
     * @since 1.4.0
     */
    Rect addRect(int x1, int y1, int x2, int y2, int color, int alpha, double rotation, int zIndex);

    /**
     * Removes the rectangle from this overlay without modifying it. It can be added
     * again with {@link #reAddElement(RenderElement)}.
     *
     * @param r the rectangle to remove
     * @return this overlay for chaining
     * @since 1.2.7
     */
    @DocletIgnore
    @Deprecated
    T removeRect(Rect r);

    /** Adds a one-pixel-wide line between its endpoints, at z-index 0.
     *
     * @param x1 start x coordinate
     * @param y1 start y coordinate
     * @param x2 end x coordinate
     * @param y2 end y coordinate
     * @param color line color, including alpha
     * @return the added line
     * @since 1.8.4
     */
    Line addLine(int x1, int y1, int x2, int y2, int color);

    /** Adds a one-pixel-wide line between its endpoints. Lower z-index elements draw first.
     *
     * @param x1 start x coordinate
     * @param y1 start y coordinate
     * @param x2 end x coordinate
     * @param y2 end y coordinate
     * @param color line color, including alpha
     * @param zIndex draw order relative to other overlay elements
     * @return the added line
     * @since 1.8.4
     */
    Line addLine(int x1, int y1, int x2, int y2, int color, int zIndex);

    /** Adds a line with the specified width, at z-index 0.
     *
     * @param x1 start x coordinate
     * @param y1 start y coordinate
     * @param x2 end x coordinate
     * @param y2 end y coordinate
     * @param color line color, including alpha
     * @param width line width in screen pixels
     * @return the added line
     * @since 1.8.4
     */
    Line addLine(int x1, int y1, int x2, int y2, int color, double width);

    /** Adds a line with the specified z-index and width. Lower z-index elements draw first.
     *
     * @param x1 start x coordinate
     * @param y1 start y coordinate
     * @param x2 end x coordinate
     * @param y2 end y coordinate
     * @param color line color, including alpha
     * @param zIndex draw order relative to other overlay elements
     * @param width line width in screen pixels
     * @return the added line
     * @since 1.8.4
     */
    Line addLine(int x1, int y1, int x2, int y2, int color, int zIndex, double width);

    /** Adds a line with the specified width and rotation about its midpoint, at z-index 0.
     *
     * @param x1 start x coordinate
     * @param y1 start y coordinate
     * @param x2 end x coordinate
     * @param y2 end y coordinate
     * @param color line color, including alpha
     * @param width line width in screen pixels
     * @param rotation rotation in degrees
     * @return the added line
     * @since 1.8.4
     */
    Line addLine(int x1, int y1, int x2, int y2, int color, double width, double rotation);

    /** Adds a line with the specified z-index, width, and rotation about its midpoint.
     * Lower z-index elements draw first.
     *
     * @param x1 start x coordinate
     * @param y1 start y coordinate
     * @param x2 end x coordinate
     * @param y2 end y coordinate
     * @param color line color, including alpha
     * @param zIndex draw order relative to other overlay elements
     * @param width line width in screen pixels
     * @param rotation rotation in degrees
     * @return the added line
     * @since 1.8.4
     */
    Line addLine(int x1, int y1, int x2, int y2, int color, int zIndex, double width, double rotation);

    /**
     * Removes the line from this overlay without modifying it. It can be added
     * again with {@link #reAddElement(RenderElement)}.
     *
     * @param l the line to remove
     * @return this overlay for chaining
     * @since 1.8.4
     */
    T removeLine(Line l);

    /** Adds an item icon with the default overlay enabled.
     * @param x left coordinate
     * @param y top coordinate
     * @param id item identifier
     * @return the added item element
     * @since 1.2.7
     */
    @DocletReplaceParams("x: int, y: int, id: CanOmitNamespace<ItemId>")
    Item addItem(int x, int y, String id);

    /** Adds an item icon with the default overlay enabled and the specified draw order.
     * @param x left coordinate
     * @param y top coordinate
     * @param zIndex draw order
     * @param id item identifier
     * @return the added item element
     * @since 1.4.0
     */
    @DocletReplaceParams("x: int, y: int, zIndex: int, id: CanOmitNamespace<ItemId>")
    Item addItem(int x, int y, int zIndex, String id);

    /** Adds an item icon and controls whether its game-provided decorations are drawn.
     * @param x left coordinate
     * @param y top coordinate
     * @param id item identifier
     * @param overlay whether to draw item decorations, including durability and count
     * @return the added item element
     * @since 1.2.7
     */
    @DocletReplaceParams("x: int, y: int, id: CanOmitNamespace<ItemId>, overlay: boolean")
    Item addItem(int x, int y, String id, boolean overlay);

    /** Adds an item icon with a draw order and controls whether its game-provided decorations are drawn.
     * @param x left coordinate
     * @param y top coordinate
     * @param zIndex draw order
     * @param id item identifier
     * @param overlay whether to draw item decorations, including durability and count
     * @return the added item element
     * @since 1.4.0
     */
    @DocletReplaceParams("x: int, y: int, zIndex: int, id: CanOmitNamespace<ItemId>, overlay: boolean")
    Item addItem(int x, int y, int zIndex, String id, boolean overlay);

    /** Adds a scaled and rotated item icon; rotation is in degrees about its center.
     * @param x left coordinate
     * @param y top coordinate
     * @param id item identifier
     * @param overlay whether to draw item decorations
     * @param scale size multiplier
     * @param rotation rotation in degrees
     * @return the added item element
     * @since 1.2.7
     */
    @DocletReplaceParams("x: int, y: int, id: CanOmitNamespace<ItemId>, overlay: boolean, scale: double, rotation: double")
    Item addItem(int x, int y, String id, boolean overlay, double scale, double rotation);

    /** Adds a scaled and rotated item icon with the specified draw order; rotation is in degrees about its center.
     * @param x left coordinate
     * @param y top coordinate
     * @param zIndex draw order
     * @param id item identifier
     * @param overlay whether to draw item decorations
     * @param scale size multiplier
     * @param rotation rotation in degrees
     * @return the added item element
     * @since 1.4.0
     */
    @DocletReplaceParams("x: int, y: int, zIndex: int, id: CanOmitNamespace<ItemId>, overlay: boolean, scale: double, rotation: double")
    Item addItem(int x, int y, int zIndex, String id, boolean overlay, double scale, double rotation);

    /** Adds an item icon from a stack, with the default overlay enabled.
     * @param x left coordinate
     * @param y top coordinate
     * @param item item stack helper
     * @return the added item element
     * @since 1.2.7
     */
    Item addItem(int x, int y, ItemStackHelper item);

    /** Adds an item icon from a stack, with the default overlay and specified draw order.
     * @param x left coordinate
     * @param y top coordinate
     * @param zIndex draw order
     * @param item item stack helper
     * @return the added item element
     * @since 1.4.0
     */
    Item addItem(int x, int y, int zIndex, ItemStackHelper item);

    /** Adds an item icon from a stack and controls whether its game-provided decorations are drawn.
     * @param x left coordinate
     * @param y top coordinate
     * @param item item stack helper
     * @param overlay whether to draw item decorations, including durability and count
     * @return the added item element
     * @since 1.2.7
     */
    Item addItem(int x, int y, ItemStackHelper item, boolean overlay);

    /** Adds an item icon from a stack with a draw order and control over its decorations.
     * @param x left coordinate
     * @param y top coordinate
     * @param zIndex draw order
     * @param item item stack helper
     * @param overlay whether to draw item decorations, including durability and count
     * @return the added item element
     * @since 1.4.0
     */
    Item addItem(int x, int y, int zIndex, ItemStackHelper item, boolean overlay);

    /** Adds a scaled and rotated item icon from a stack; rotation is in degrees about its center.
     * @param x left coordinate
     * @param y top coordinate
     * @param item item stack helper
     * @param overlay whether to draw item decorations
     * @param scale size multiplier
     * @param rotation rotation in degrees
     * @return the added item element
     * @since 1.2.7
     */
    Item addItem(int x, int y, ItemStackHelper item, boolean overlay, double scale, double rotation);

    /** Adds a scaled and rotated item icon from a stack with the specified draw order among this overlay's elements; rotation is in degrees about its center.
     * @param x left coordinate
     * @param y top coordinate
     * @param zIndex draw order
     * @param item item stack helper
     * @param overlay whether to draw item decorations
     * @param scale size multiplier
     * @param rotation rotation in degrees
     * @return the added item element
     * @since 1.4.0
     */
    Item addItem(int x, int y, int zIndex, ItemStackHelper item, boolean overlay, double scale, double rotation);

    /**
     * Removes an item element from this overlay without changing the element itself.
     *
     * @param i the item to remove
     * @return self for chaining
     * @since 1.2.7
     */
    @DocletIgnore
    @Deprecated
    T removeItem(Item i);

    /**
     * Adds another overlay as a child, or returns {@code null} if that would create a cycle.
     *
     * @param draw2D the overlay to draw inside this one
     * @param x      the x position on this draw2d
     * @param y      the y position on this draw2d
     * @param width  the nested overlay's width
     * @param height the nested overlay's height
     * @return a wrapper for the nested overlay, or {@code null} if nesting would create a cycle
     * @since 1.8.4
     */
    Draw2DElement addDraw2D(Draw2D draw2D, int x, int y, int width, int height);

    /**
     * Adds another overlay as a child with the specified draw order, or returns {@code null} if that would create a cycle.
     *
     * @param draw2D the overlay to draw inside this one
     * @param x      the x position on this draw2d
     * @param y      the y position on this draw2d
     * @param width  the nested overlay's width
     * @param height the nested overlay's height
     * @param zIndex the nested overlay's draw order among this overlay's elements
     * @return a wrapper for the nested overlay, or {@code null} if nesting would create a cycle
     * @since 1.8.4
     */
    Draw2DElement addDraw2D(Draw2D draw2D, int x, int y, int width, int height, int zIndex);

    /**
     * Removes the nested-overlay wrapper from this overlay without changing the nested overlay.
     * @param draw2D the wrapper to remove
     * @return this overlay for chaining
     * @since 1.8.4
     */
    T removeDraw2D(Draw2DElement draw2D);

    /**
     * Returns an item builder bound to this overlay; building does not add an element, while {@code buildAndAdd()} does.
     *
     * @return a builder for an {@link Item}.
     * @since 1.8.4
     */
    default Item.Builder itemBuilder() {
        return new Item.Builder(this);
    }

    /**
     * Returns an item builder with the supplied stack set, bound to this overlay.
     *
     * @param item the item to use
     * @return a builder for an {@link Item}.
     * @since 1.8.4
     */
    default Item.Builder itemBuilder(ItemStackHelper item) {
        return new Item.Builder(this).item(item);
    }

    /**
     * Returns an image builder bound to this overlay. Building does not add an element; an image without a texture draws nothing.
     *
     * @return a builder for an {@link Image}.
     * @since 1.8.4
     */
    default Image.Builder imageBuilder() {
        return new Image.Builder(this);
    }

    /**
     * Returns an image builder bound to this overlay with its texture identifier set.
     *
     * @param id the id of the image
     * @return a builder for an {@link Image}.
     * @since 1.8.4
     */
    default Image.Builder imageBuilder(String id) {
        return new Image.Builder(this).identifier(id);
    }

    /**
     * Returns a rectangle builder bound to this overlay; {@code buildAndAdd()} adds
     * the built rectangle to it.
     *
     * @return a builder for a {@link Rect}.
     * @since 1.8.4
     */
    default Rect.Builder rectBuilder() {
        return new Rect.Builder(this);
    }

    /**
     * Returns a rectangle builder with its first corner and size set.
     *
     * @param x      the x position of the rectangle
     * @param y      the y position of the rectangle
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @return a builder for a {@link Rect}.
     * @since 1.8.4
     */
    default Rect.Builder rectBuilder(int x, int y, int width, int height) {
        return new Rect.Builder(this).size(width, height).pos1(x, y);
    }

    /**
     * Returns a line builder bound to this overlay; {@code buildAndAdd()} adds the
     * built line to it.
     *
     * @return a builder for a {@link Line}.
     * @since 1.8.4
     */
    default Line.Builder lineBuilder() {
        return new Line.Builder(this);
    }

    /**
     * Returns a line builder with both endpoints set.
     *
     * @param x1 the x position of the first point
     * @param y1 the y position of the first point
     * @param x2 the x position of the second point
     * @param y2 the y position of the second point
     * @return a builder for a {@link Line}.
     * @since 1.8.4
     */
    default Line.Builder lineBuilder(int x1, int y1, int x2, int y2) {
        return new Line.Builder(this).pos(x1, y1, x2, y2);
    }

    /**
     * Returns a text builder bound to this overlay; {@code buildAndAdd()} adds the
     * built text to it.
     *
     * @return a builder for a {@link Text}.
     * @since 1.8.4
     */
    default Text.Builder textBuilder() {
        return new Text.Builder(this);
    }

    /**
     * Returns a text builder with its text set.
     *
     * @param text the text to display
     * @return a builder for a {@link Text}.
     * @since 1.8.4
     */
    default Text.Builder textBuilder(String text) {
        return new Text.Builder(this).text(text);
    }

    /**
     * Returns a text builder initialized with the supplied styled text.
     *
     * @param text the text to display
     * @return a builder for a {@link Text}.
     * @since 1.8.4
     */
    default Text.Builder textBuilder(TextHelper text) {
        return new Text.Builder(this).text(text);
    }

    /**
     * Returns a builder for nesting {@code draw2D} in this overlay. Adding an element
     * that would create a cycle is rejected by the overlay.
     *
     * @param draw2D the draw2d to add
     * @return a builder for a {@link Draw2D}.
     * @since 1.8.4
     */
    default Draw2DElement.Builder draw2DBuilder(Draw2D draw2D) {
        return new Draw2DElement.Builder(this, draw2D);
    }

    /**
     * Sets the callback run when this overlay initializes. If a callback is set, its
     * elements are cleared first; nested overlays are initialized afterward. Callback
     * failures are passed to the fail callback or logged.
     *
     * @param onInit calls your method as a {@link Consumer}&lt;{@link T}&gt;
     * @return self for chaining
     * @since 1.2.7
     */
    T setOnInit(MethodWrapper<T, Object, Object, ?> onInit);

    /**
     * Sets the callback that receives an initialization failure's {@code toString()}
     * value. If unset, the failure is logged. This handles initialization failures
     * only.
     *
     * @param catchInit calls your method as a {@link Consumer}&lt;{@link String}&gt;
     * @return self for chaining
     * @since 1.2.7
     */
    T setOnFailInit(MethodWrapper<String, Object, Object, ?> catchInit);

    /**
     * Renders this overlay using the supplied graphics context.
     *
     * @param drawContext the graphics context to render with
     */
    @DocletIgnore
    void render(GuiGraphics drawContext);

    /**
     * Sets this overlay's draw order relative to other registered overlays; lower values
     * draw first. This orders whole overlays, not their elements, and does not affect
     * screen overlays.
     *
     * @param zIndex the overlay's draw order
     * @since 1.8.4
     */
    void setZIndex(int zIndex);

    /**
     * Returns this overlay's draw order relative to other registered overlays, not the
     * z-index of its elements.
     *
     * @return this overlay's draw order
     * @since 1.8.4
     */
    int getZIndex();

}
