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
 * What a 2D overlay has to be able to do, whichever class implements it.
 * <p>
 * Almost every script meets this through a {@code Draw2D}, which is the one that draws
 * over the game on its own. The other implementation is a screen, which is a 2D overlay
 * that is the game's own menu class: it is opened rather than registered, it pauses the
 * game, and it can hold buttons and text fields. Both can be read with the same code
 * because both are this.
 * <p>
 * The {@code T} is what the removal methods hand back. On a plain overlay that is the
 * overlay itself, so removals chain; on a screen it is the screen, for the same reason.
 * The element getters and the {@code add} methods return the element they made rather
 * than the overlay, so those never chain into anything.
 * <p>
 * There is a builder for each kind of element, and they are the richer route: the
 * {@code add} methods here take their arguments positionally and the builders name them.
 * Both produce the same elements, and a builder that was made from an overlay is bound
 * to it, so {@code buildAndAdd()} on it puts the element where the {@code add} call
 * would have.
 * <p>
 * The names here are the ones a script sees. Two are worth reading carefully, because
 * the same word means something else elsewhere: {@link #getWidth()} and
 * {@link #getHeight()} are the size of the <em>screen</em> as far as this overlay is
 * concerned, not the size of its content, and on a surface they are a count of surface
 * pixels rather than either of those.
 * example:
     * <pre>
 * // a plain overlay, the usual implementation
 * const draw = Hud.createDraw2D();
 * draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
 * draw.register();
 * </pre>
 *
 * @param <T> what the removal methods hand back, so that they chain
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Rendering/Graphics")
public interface IDraw2D<T> {

    /**
     * how wide the screen is, in scaled pixels, as far as this overlay is concerned.
     * <p>
     * Every element coordinate on this overlay is in that space, so a position up to this
     * width is on screen. On a plain overlay it is the window's width after the GUI scale
     * has been applied, which is the same space the mouse readings are in. On a screen
     * it is the screen's own width, which is usually the same number but is not when the
     * screen has been given a different size.
     * <p>
     * This is not the width of the overlay's content. Elements are placed by absolute
     * coordinate and there is no such thing as the overlay's own width.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.register();
     * Chat.log(`screen is ${draw.getWidth()} pixels wide to this overlay`);
     * </pre>
     *
     * @return screen width
     * @since 1.2.7
     */
    int getWidth();

    /**
     * how tall the screen is, in scaled pixels, as far as this overlay is concerned.
     * <p>
     * The counterpart of {@link #getWidth()}, and in the same space as every element
     * coordinate on this overlay.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.register();
     * Chat.log(`screen is ${draw.getHeight()} pixels tall to this overlay`);
     * </pre>
     *
     * @return screen height
     * @since 1.2.7
     */
    int getHeight();

    /**
     * every text element on this overlay, in the order they were added.
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
     * draw.addText("one", 10, 10, 0xFFFFFFFF, true);
     * draw.register();
     * Chat.log(`${draw.getTexts().size()} texts`);
     * </pre>
     *
     * @return text elements
     * @since 1.2.7
     */
    @Deprecated
    List<Text> getTexts();

    /**
     * every rectangle on this overlay, in the order they were added.
     * <p>
     * A new list each call, filtered by type, so changing it does not change what this
     * overlay holds. Nested overlays are not included.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addRect(0, 0, 10, 10, 0xFFFF0000);
     * draw.register();
     * Chat.log(`${draw.getRects().size()} rects`);
     * </pre>
     *
     * @return rect elements
     * @since 1.2.7
     */
    @Deprecated
    List<Rect> getRects();

    /**
     * every line on this overlay, in the order they were added.
     * <p>
     * A new list each call, filtered by type, so changing it does not change what this
     * overlay holds. Nested overlays are not included.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addLine(0, 0, 10, 10, 0xFFFFFFFF);
     * draw.register();
     * Chat.log(`${draw.getLines().size()} lines`);
     * </pre>
     *
     * @return all registered line elements.
     * @since 1.8.4
     */
    List<Line> getLines();

    /**
     * every item icon on this overlay, in the order they were added.
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
    @Deprecated
    List<Item> getItems();

    /**
     * every image on this overlay, in the order they were added.
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
    @Deprecated
    List<Image> getImages();

    /**
     * every nested overlay on this one, in the order they were added.
     * <p>
     * A new list each call, filtered by type, so changing it does not change what this
     * overlay holds. This is the list of wrappers rather than of the overlays
     * themselves; the contents of the nested overlays are not in here.
     * example:
     * <pre>
     * const outer = Hud.createDraw2D();
     * const panel = Hud.createDraw2D();
     * panel.addText("inside", 5, 5, 0xFFFFFFFF, true);
     * outer.addDraw2D(panel, 10, 10, 100, 50);
     * outer.register();
     * Chat.log(`${outer.getDraw2Ds().size()} nested overlays`);
     * </pre>
     *
     * @return all registered draw2d elements.
     * @since 1.8.4
     */
    List<Draw2DElement> getDraw2Ds();

    /**
     * Everything on this overlay of every kind, in the order things were added. The
     * snapshot is read only and is taken when it is called, so changing it does not
     * change what this overlay holds and elements taken off afterwards do not disappear
     * from a copy already taken. The types have to be told apart with their own getters.
     * <p>
     * Nested overlays are wrappers in this list rather than the overlays themselves, and
     * the contents of those nested overlays are not in here.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addText("one", 10, 10, 0xFFFFFFFF, true);
     * draw.addRect(0, 0, 10, 10, 0xFFFF0000);
     * draw.register();
     * // both, where the per-type getters would each give one
     * Chat.log(`${draw.getElements().size()} elements in total`);
     * </pre>
     *
     * @return a read only copy of the list of all elements added by scripts.
     * @since 1.2.9
     */
    List<RenderElement> getElements();

    /**
     * removes any element regardless of type.
     * <p>
     * This is the one removal that does not need to know the type, which is what makes
     * it the right one for an element read back out of {@link #getElements()}. The typed
     * removals do the same thing and are deprecated.
     * <p>
     * The element itself is left alone, so it can be put back with
     * {@link #reAddElement(RenderElement)} and comes back as it was. Removing something
     * that is not on this overlay does nothing.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * const text = draw.addText("temporary", 10, 10, 0xFFFFFFFF, true);
     * draw.register();
     * draw.removeElement(text);
     * Chat.log(`${draw.getTexts().size()} texts left`);
     * </pre>
     *
     * @param e the element to take off
     * @return self for chaining
     * @since 1.2.9
     */
    T removeElement(RenderElement e);

    /**
     * re-add an element you removed with {@link #removeElement(RenderElement)}
     * <p>
     * For an element taken off, or one built with {@code build()} rather than
     * {@code buildAndAdd()}. Adding an element that is already on the overlay leaves it
     * as it is, and still reports the element back.
     * <p>
     * A nested overlay is the case that can fail, and this is where that is checked. One
     * is refused, and {@code null} is returned, when it has no overlay behind it, when it
     * is this overlay, or when it already contains this one further down. That last one
     * is the cycle case: without the check the renderer would follow the nesting round
     * and round.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * const text = draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
     * draw.register();
     * draw.removeElement(text);
     * // back on, and still there
     * draw.reAddElement(text);
     * </pre>
     *
     * @param e the element to put back
     * @return the element, or {@code null} if it is a nested overlay that was refused
     * @since 1.2.9
     */
    <T extends RenderElement> T reAddElement(T e);

    /**
     * adds a piece of text at a position, with a colour and a shadow.
     * <p>
     * The position is in screen pixels from the top left, the colour is packed as the
     * game packs colours with the alpha in the top byte, and the shadow is the game's
     * own drop shadow behind the glyphs. This is the shortest form and puts the text at
     * z-index 0 with a scale of 1 and no rotation.
     * <p>
     * There are eight forms of this. The ones that take a {@link TextHelper} keep that
     * text's own styling rather than taking a plain string, the ones that take a z-index
     * order the text against the other elements here, and the ones that take a scale and
     * a rotation change how big and which way round the text is. Every one of them ends
     * up at the same full form.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
     * draw.register();
     * </pre>
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
     * @param text
     * @param x      screen x
     * @param y      screen y
     * @param color  text color
     * @param zIndex z-index
     * @param shadow include shadow layer
     * @return added text
     * @since 1.4.0
     */
    Text addText(String text, int x, int y, int color, int zIndex, boolean shadow);

    /**
     * @param text
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
     * @param text
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
     * @param text
     * @param x      screen x
     * @param y      screen y
     * @param color  text color
     * @param shadow include shadow layer
     * @return added text
     * @since 1.2.7
     */
    Text addText(TextHelper text, int x, int y, int color, boolean shadow);

    /**
     * @param text
     * @param x      screen x
     * @param y      screen y
     * @param color  text color
     * @param zIndex z-index
     * @param shadow include shadow layer
     * @return added text
     * @since 1.4.0
     */
    Text addText(TextHelper text, int x, int y, int color, int zIndex, boolean shadow);

    /**
     * @param text
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
     * @param text
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
     * @param t
     * @return self for chaining
     * @since 1.2.7
     */
    @Deprecated
    T removeText(Text t);

    /**
     * @param x             screen x, top left corner
     * @param y             screen y, top left corner
     * @param width         width on screen
     * @param height        height on screen
     * @param id            image id, in the form {@code minecraft:textures} path'd as found in texture packs, ie {@code assets/minecraft/textures/gui/recipe_book.png} becomes {@code minecraft:textures/gui/recipe_book.png}
     * @param imageX        the left-most coordinate of the texture region
     * @param imageY        the top-most coordinate of the texture region
     * @param regionWidth   the width the texture region
     * @param regionHeight  the height the texture region
     * @param textureWidth  the width of the entire texture
     * @param textureHeight the height of the entire texture
     * @return added image
     * @since 1.2.7
     */
    Image addImage(int x, int y, int width, int height, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight);

    /**
     * @param x             screen x, top left corner
     * @param y             screen y, top left corner
     * @param width         width on screen
     * @param height        height on screen
     * @param zIndex        z-index
     * @param id            image id, in the form {@code minecraft:textures} path'd as found in texture packs, ie {@code assets/minecraft/textures/gui/recipe_book.png} becomes {@code minecraft:textures/gui/recipe_book.png}
     * @param imageX        the left-most coordinate of the texture region
     * @param imageY        the top-most coordinate of the texture region
     * @param regionWidth   the width the texture region
     * @param regionHeight  the height the texture region
     * @param textureWidth  the width of the entire texture
     * @param textureHeight the height of the entire texture
     * @return added image
     * @since 1.4.0
     */
    Image addImage(int x, int y, int width, int height, int zIndex, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight);

    /**
     * @param x             screen x, top left corner
     * @param y             screen y, top left corner
     * @param width         width on screen
     * @param height        height on screen
     * @param id            image id, in the form {@code minecraft:textures} path'd as found in texture packs, ie {@code assets/minecraft/textures/gui/recipe_book.png} becomes {@code minecraft:textures/gui/recipe_book.png}
     * @param imageX        the left-most coordinate of the texture region
     * @param imageY        the top-most coordinate of the texture region
     * @param regionWidth   the width the texture region
     * @param regionHeight  the height the texture region
     * @param textureWidth  the width of the entire texture
     * @param textureHeight the height of the entire texture
     * @param rotation      the rotation (clockwise) of the texture (as degrees)
     * @return added image
     * @since 1.2.7
     */
    Image addImage(int x, int y, int width, int height, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight, double rotation);

    /**
     * @param x             screen x, top left corner
     * @param y             screen y, top left corner
     * @param width         width on screen
     * @param height        height on screen
     * @param zIndex        z-index
     * @param id            image id, in the form {@code minecraft:textures} path'd as found in texture packs, ie {@code assets/minecraft/textures/gui/recipe_book.png} becomes {@code minecraft:textures/gui/recipe_book.png}
     * @param imageX        the left-most coordinate of the texture region
     * @param imageY        the top-most coordinate of the texture region
     * @param regionWidth   the width the texture region
     * @param regionHeight  the height the texture region
     * @param textureWidth  the width of the entire texture
     * @param textureHeight the height of the entire texture
     * @param rotation      the rotation (clockwise) of the texture (as degrees)
     * @return added image
     * @since 1.4.0
     */
    Image addImage(int x, int y, int width, int height, int zIndex, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight, double rotation);

    /**
     * @param x
     * @param y
     * @param width
     * @param height
     * @param zIndex
     * @param color
     * @param id
     * @param imageX
     * @param imageY
     * @param regionWidth
     * @param regionHeight
     * @param textureWidth
     * @param textureHeight
     * @param rotation
     * @return
     * @since 1.6.5
     */
    Image addImage(int x, int y, int width, int height, int zIndex, int color, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight, double rotation);

    /**
     * @param x
     * @param y
     * @param width
     * @param height
     * @param zIndex
     * @param alpha
     * @param color
     * @param id
     * @param imageX
     * @param imageY
     * @param regionWidth
     * @param regionHeight
     * @param textureWidth
     * @param textureHeight
     * @param rotation
     * @return
     * @since 1.6.5
     */
    Image addImage(int x, int y, int width, int height, int zIndex, int alpha, int color, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight, double rotation);

    /**
     * takes an image off this overlay.
     * <p>
     * The image itself
     * self is left alone, so it can be put back with
     * {@link #reAddElement(RenderElement)} and comes back as it was.
     * {@link #removeElement(RenderElement)} does the same thing without needing the type.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * const image = draw.addImage(10, 10, 16, 16, "minecraft:item/diamond", 0, 0, 16, 16, 16, 16);
     * draw.register();
     * draw.removeImage(image);
     * Chat.log(`${draw.getImages().size()} images left`);
     * </pre>
     *
     * @param i the i to take off
     * @return self for chaining
     * @since 1.2.7
     */
    @Deprecated
    T removeImage(Image i);

    /**
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param color as hex, with alpha channel
     * @return added rect
     * @since 1.2.7
     */
    Rect addRect(int x1, int y1, int x2, int y2, int color);

    /**
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param color as hex
     * @param alpha alpha channel 0-255
     * @return added rect
     * @since 1.2.7
     */
    Rect addRect(int x1, int y1, int x2, int y2, int color, int alpha);

    /**
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param color    as hex
     * @param alpha    alpha channel 0-255
     * @param rotation as degrees
     * @return added rect
     * @since 1.2.7
     */
    Rect addRect(int x1, int y1, int x2, int y2, int color, int alpha, double rotation);

    /**
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param color    as hex
     * @param alpha    alpha channel 0-255
     * @param rotation as degrees
     * @param zIndex   z-index
     * @return added rect
     * @since 1.4.0
     */
    Rect addRect(int x1, int y1, int x2, int y2, int color, int alpha, double rotation, int zIndex);

    /**
     * takes a rectangle off this overlay.
     * <p>
     * The rectangle itself
     * self is left alone, so it can be put back with
     * {@link #reAddElement(RenderElement)} and comes back as it was.
     * {@link #removeElement(RenderElement)} does the same thing without needing the type.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * const rect = draw.addRect(10, 10, 110, 60, 0x80000000);
     * draw.register();
     * draw.removeRect(rect);
     * Chat.log(`${draw.getRects().size()} rects left`);
     * </pre>
     *
     * @param r the r to take off
     * @return self for chaining
     * @since 1.2.7
     */
    @Deprecated
    T removeRect(Rect r);

    /**
     * @param x1    the x position of the start
     * @param y1    the y position of the start
     * @param x2    the x position of the end
     * @param y2    the y position of the end
     * @param color the color of the line, can include alpha value
     * @return the added line.
     * @since 1.8.4
     */
    Line addLine(int x1, int y1, int x2, int y2, int color);

    /**
     * @param x1     the x position of the start
     * @param y1     the y position of the start
     * @param x2     the x position of the end
     * @param y2     the y position of the end
     * @param color  the color of the line, can include alpha value
     * @param zIndex the z-index of the line
     * @return the added line.
     * @since 1.8.4
     */
    Line addLine(int x1, int y1, int x2, int y2, int color, int zIndex);

    /**
     * @param x1    the x position of the start
     * @param y1    the y position of the start
     * @param x2    the x position of the end
     * @param y2    the y position of the end
     * @param color the color of the line, can include alpha value
     * @param width the width of the line
     * @return the added line.
     * @since 1.8.4
     */
    Line addLine(int x1, int y1, int x2, int y2, int color, double width);

    /**
     * @param x1     the x position of the start
     * @param y1     the y position of the start
     * @param x2     the x position of the end
     * @param y2     the y position of the end
     * @param color  the color of the line, can include alpha value
     * @param zIndex the z-index of the line
     * @param width  the width of the line
     * @return the added line.
     * @since 1.8.4
     */
    Line addLine(int x1, int y1, int x2, int y2, int color, int zIndex, double width);

    /**
     * @param x1       the x position of the start
     * @param y1       the y position of the start
     * @param x2       the x position of the end
     * @param y2       the y position of the end
     * @param color    the color of the line, can include alpha value
     * @param width    the width of the line
     * @param rotation the rotation (clockwise) of the line (as degrees)
     * @return the added line.
     * @since 1.8.4
     */
    Line addLine(int x1, int y1, int x2, int y2, int color, double width, double rotation);

    /**
     * @param x1       the x position of the start
     * @param y1       the y position of the start
     * @param x2       the x position of the end
     * @param y2       the y position of the end
     * @param color    the color of the line, can include alpha value
     * @param zIndex   the z-index of the line
     * @param width    the width of the line
     * @param rotation the rotation (clockwise) of the line (as degrees)
     * @return the added line.
     * @since 1.8.4
     */
    Line addLine(int x1, int y1, int x2, int y2, int color, int zIndex, double width, double rotation);

    /**
     * @param l the line to remove
     * @return self chaining.
     * @since 1.8.4
     */
    T removeLine(Line l);

    /**
     * @param x  left most corner
     * @param y  top most corner
     * @param id item id
     * @return added item
     * @since 1.2.7
     */
    @DocletReplaceParams("x: int, y: int, id: CanOmitNamespace<ItemId>")
    Item addItem(int x, int y, String id);

    /**
     * @param x      left most corner
     * @param y      top most corner
     * @param zIndex z-index
     * @param id     item id
     * @return added item
     * @since 1.4.0
     */
    @DocletReplaceParams("x: int, y: int, zIndex: int, id: CanOmitNamespace<ItemId>")
    Item addItem(int x, int y, int zIndex, String id);

    /**
     * @param x       left most corner
     * @param y       top most corner
     * @param id      item id
     * @param overlay should include overlay health and count
     * @return added item
     * @since 1.2.7
     */
    @DocletReplaceParams("x: int, y: int, id: CanOmitNamespace<ItemId>, overlay: boolean")
    Item addItem(int x, int y, String id, boolean overlay);

    /**
     * @param x       left most corner
     * @param y       top most corner
     * @param zIndex  z-index
     * @param id      item id
     * @param overlay should include overlay health and count
     * @return added item
     * @since 1.4.0
     */
    @DocletReplaceParams("x: int, y: int, zIndex: int, id: CanOmitNamespace<ItemId>, overlay: boolean")
    Item addItem(int x, int y, int zIndex, String id, boolean overlay);

    /**
     * @param x        left most corner
     * @param y        top most corner
     * @param id       item id
     * @param overlay  should include overlay health and count
     * @param scale    scale of item
     * @param rotation rotation of item
     * @return added item
     * @since 1.2.7
     */
    @DocletReplaceParams("x: int, y: int, id: CanOmitNamespace<ItemId>, overlay: boolean, scale: double, rotation: double")
    Item addItem(int x, int y, String id, boolean overlay, double scale, double rotation);

    /**
     * @param x        left most corner
     * @param y        top most corner
     * @param zIndex   z-index
     * @param id       item id
     * @param overlay  should include overlay health and count
     * @param scale    scale of item
     * @param rotation rotation of item
     * @return added item
     * @since 1.4.0
     */
    @DocletReplaceParams("x: int, y: int, zIndex: int, id: CanOmitNamespace<ItemId>, overlay: boolean, scale: double, rotation: double")
    Item addItem(int x, int y, int zIndex, String id, boolean overlay, double scale, double rotation);

    /**
     * @param x    left most corner
     * @param y    top most corner
     * @param item from inventory as helper
     * @return added item
     * @since 1.2.7
     */
    Item addItem(int x, int y, ItemStackHelper item);

    /**
     * @param x      left most corner
     * @param y      top most corner
     * @param zIndex z-index
     * @param item   from inventory as helper
     * @return added item
     * @since 1.4.0
     */
    Item addItem(int x, int y, int zIndex, ItemStackHelper item);

    /**
     * @param x       left most corner
     * @param y       top most corner
     * @param item    from inventory as helper
     * @param overlay should include overlay health and count
     * @return added item
     * @since 1.2.7
     */
    Item addItem(int x, int y, ItemStackHelper item, boolean overlay);

    /**
     * @param x       left most corner
     * @param y       top most corner
     * @param zIndex  z-index
     * @param item    from inventory as helper
     * @param overlay should include overlay health and count
     * @return added item
     * @since 1.4.0
     */
    Item addItem(int x, int y, int zIndex, ItemStackHelper item, boolean overlay);

    /**
     * @param x        left most corner
     * @param y        top most corner
     * @param item     from inventory as helper
     * @param overlay  should include overlay health and count
     * @param scale    scale of item
     * @param rotation rotation of item
     * @return added item
     * @since 1.2.7
     */
    Item addItem(int x, int y, ItemStackHelper item, boolean overlay, double scale, double rotation);

    /**
     * @param x        left most corner
     * @param y        top most corner
     * @param zIndex   z-index
     * @param item     from inventory as helper
     * @param overlay  should include overlay health and count
     * @param scale    scale of item
     * @param rotation rotation of item
     * @return added item
     * @since 1.4.0
     */
    Item addItem(int x, int y, int zIndex, ItemStackHelper item, boolean overlay, double scale, double rotation);

    /**
     * takes an item icon off this overlay.
     * <p>
     * The icon itself
     * self is left alone, so it can be put back with
     * {@link #reAddElement(RenderElement)} and comes back as it was.
     * {@link #removeElement(RenderElement)} does the same thing without needing the type.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * const item = draw.addItem(10, 10, "minecraft:diamond");
     * draw.register();
     * draw.removeItem(item);
     * Chat.log(`${draw.getItems().size()} items left`);
     * </pre>
     *
     * @param i the i to take off
     * @return self for chaining
     * @since 1.2.7
     */
    @Deprecated
    T removeItem(Item i);

    /**
     * Tries to add the given draw2d as a child. Fails if cyclic dependencies are detected.
     *
     * @param draw2D the draw2d to add
     * @param x      the x position on this draw2d
     * @param y      the y position on this draw2d
     * @param width  the width of the given draw2d
     * @param height the height of the given draw2d
     * @return a wrapper for the draw2d.
     * @since 1.8.4
     */
    Draw2DElement addDraw2D(Draw2D draw2D, int x, int y, int width, int height);

    /**
     * Tries to add the given draw2d as a child. Fails if cyclic dependencies are detected.
     *
     * @param draw2D the draw2d to add
     * @param x      the x position on this draw2d
     * @param y      the y position on this draw2d
     * @param width  the width of the given draw2d
     * @param height the height of the given draw2d
     * @param zIndex the z-index for the draw2d
     * @return a wrapper for the draw2d.
     * @since 1.8.4
     */
    Draw2DElement addDraw2D(Draw2D draw2D, int x, int y, int width, int height, int zIndex);

    /**
     * @param draw2D the draw2d to remove
     * @return self chaining.
     * @since 1.8.4
     */
    T removeDraw2D(Draw2DElement draw2D);

    /**
     * The builder is bound to this overlay, so {@code buildAndAdd()} on it puts the icon
     * where the matching {@code addItem} call would have. Nothing is added until it is
     * built.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.itemBuilder().item("minecraft:diamond").pos(10, 10).buildAndAdd();
     * draw.register();
     * </pre>
     *
     * @return a builder for an {@link Item}.
     * @since 1.8.4
     */
    default Item.Builder itemBuilder() {
        return new Item.Builder(this);
    }

    /**
     * The builder comes with the stack already set, so it only needs a position and a
     * build.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const draw = Hud.createDraw2D();
     *   draw.itemBuilder(player.getMainHand()).pos(10, 10).buildAndAdd();
     *   draw.register();
     * }
     * </pre>
     *
     * @param item the item to use
     * @return a builder for an {@link Item}.
     * @since 1.8.4
     */
    default Item.Builder itemBuilder(ItemStackHelper item) {
        return new Item.Builder(this).item(item);
    }

    /**
     * The builder is bound to this overlay, so {@code buildAndAdd()} on it puts the image
     * where the matching {@code addImage} call would have. Nothing is added until it is
     * built, and an image with no texture named draws nothing.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).buildAndAdd();
     * draw.register();
     * </pre>
     *
     * @return a builder for an {@link Image}.
     * @since 1.8.4
     */
    default Image.Builder imageBuilder() {
        return new Image.Builder(this);
    }

    /**
     * The builder comes with the texture already named, so it only needs a position, a
     * size and a build. The id is the way a texture pack names the file, so
     * {@code assets/minecraft/textures/gui/recipe_book.png} is
     * {@code minecraft:textures/gui/recipe_book.png}.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.imageBuilder("minecraft:item/diamond").pos(10, 10).size(16, 16).buildAndAdd();
     * draw.register();
     * </pre>
     *
     * @param id the id of the image
     * @return a builder for an {@link Image}.
     * @since 1.8.4
     */
    default Image.Builder imageBuilder(String id) {
        return new Image.Builder(this).identifier(id);
    }

    /**
     * The builder is bound to this overlay, so {@code buildAndAdd()} on it puts the
     * rectangle where the matching {@code addRect} call would have. Nothing is added
     * until it is built.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.rectBuilder().pos(10, 10, 110, 60).color(0x80000000).buildAndAdd();
     * draw.register();
     * </pre>
     *
     * @return a builder for a {@link Rect}.
     * @since 1.8.4
     */
    default Rect.Builder rectBuilder() {
        return new Rect.Builder(this);
    }

    /**
     * The builder comes with the first corner and the size already set. Note that this
     * is a corner and a size rather than two corners, so the second corner is worked out
     * from them rather than being given.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.rectBuilder(10, 10, 100, 50).color(0x80000000).buildAndAdd();
     * draw.register();
     * </pre>
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
     * The builder is bound to this overlay, so {@code buildAndAdd()} on it puts the line
     * where the matching {@code addLine} call would have. Nothing is added until it is
     * built.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.lineBuilder().pos(0, 0, 50, 50).color(0xFFFFFFFF).buildAndAdd();
     * draw.register();
     * </pre>
     *
     * @return a builder for a {@link Line}.
     * @since 1.8.4
     */
    default Line.Builder lineBuilder() {
        return new Line.Builder(this);
    }

    /**
     * The builder comes with both ends already set, so it only needs a colour and a
     * build. The two points are the start and the end rather than two corners, so the
     * line has a direction.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.lineBuilder(0, 0, 50, 50).color(0xFFFFFFFF).buildAndAdd();
     * draw.register();
     * </pre>
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
     * The builder is bound to this overlay, so {@code buildAndAdd()} on it puts the text
     * where the matching {@code addText} call would have. Nothing is added until it is
     * built, and a text with nothing in it draws nothing.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.textBuilder().text("hello").pos(10, 10).color(0xFFFFFFFF).buildAndAdd();
     * draw.register();
     * </pre>
     *
     * @return a builder for a {@link Text}.
     * @since 1.8.4
     */
    default Text.Builder textBuilder() {
        return new Text.Builder(this);
    }

    /**
     * The builder comes with the text already set, so it only needs a position, a colour
     * and a build.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.textBuilder("hello").pos(10, 10).color(0xFFFFFFFF).buildAndAdd();
     * draw.register();
     * </pre>
     *
     * @param text the text to display
     * @return a builder for a {@link Text}.
     * @since 1.8.4
     */
    default Text.Builder textBuilder(String text) {
        return new Text.Builder(this).text(text);
    }

    /**
     * The same as the string form, except the text keeps whatever styling the helper
     * carries rather than being a plain string.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.textBuilder(Chat.createTextHelperFromString("a green line")).pos(10, 10).color(0xFFFFFFFF).buildAndAdd();
     * draw.register();
     * </pre>
     *
     * @param text the text to display
     * @return a builder for a {@link Text}.
     * @since 1.8.4
     */
    default Text.Builder textBuilder(TextHelper text) {
        return new Text.Builder(this).text(text);
    }

    /**
     * The builder is bound to the overlay it came from and the overlay being nested, so
     * {@code buildAndAdd()} on it puts the nested overlay where the matching
     * {@code addDraw2D} call would have. The cycle check happens when the element is
     * added rather than here, so a builder happily builds something that will then be
     * refused.
     * example:
     * <pre>
     * const outer = Hud.createDraw2D();
     * const panel = Hud.createDraw2D();
     * panel.addText("inside", 5, 5, 0xFFFFFFFF, true);
     * outer.draw2DBuilder(panel).pos(20, 20).size(100, 50).buildAndAdd();
     * outer.register();
     * </pre>
     *
     * @param draw2D the draw2d to add
     * @return a builder for a {@link Draw2D}.
     * @since 1.8.4
     */
    default Draw2DElement.Builder draw2DBuilder(Draw2D draw2D) {
        return new Draw2DElement.Builder(this, draw2D);
    }

    /**
     * The overlay is emptied before the init function is called, so it is what puts
     * anything back on it. That is why an overlay should be registered or opened rather
     * than filled in by hand: a resize calls the init again, and an overlay filled in by
     * hand would lose its elements to it.
     * <p>
     * The function is given this overlay, so it adds to it. If it throws, the failure
     * goes to the fail function if one is set and is logged if it is not, and neither
     * takes the game down.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.setOnInit(JavaWrapper.methodToJava(function (self) {
     *   // self is this overlay, and it has just been emptied
     *   self.addRect(10, 10, 110, 60, 0x80000000);
     *   self.addText("rebuilt on every init", 16, 20, 0xFFFFFFFF, true);
     * }));
     * draw.register();
     * </pre>
     *
     * @param onInit calls your method as a {@link Consumer}&lt;{@link T}&gt;
     * @return self for chaining
     * @since 1.2.7
     */
    T setOnInit(MethodWrapper<T, Object, Object, ?> onInit);

    /**
     * The function is given the failure as a string, which is the throwable's own
     * {@code toString} rather than just its message, so it carries the type and the
     * message together. With no fail function set the failure is logged to the current
     * profile instead, and either way the game carries on.
     * <p>
     * This only covers the init function. Nothing else on this overlay is guarded this
     * way.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.setOnFailInit(JavaWrapper.methodToJava(function (message) {
     *   Chat.log(`panel failed to build: ${message}`);
     * }));
     * draw.setOnInit(JavaWrapper.methodToJava(function (self) {
     *   self.addText("this one is fine", 10, 10, 0xFFFFFFFF, true);
     * }));
     * draw.register();
     * </pre>
     *
     * @param catchInit calls your method as a {@link Consumer}&lt;{@link String}&gt;
     * @return self for chaining
     * @since 1.2.7
     */
    T setOnFailInit(MethodWrapper<String, Object, Object, ?> catchInit);

    /**
     * internal
     *
     * @param drawContext
     */
    @DocletIgnore
    void render(GuiGraphics drawContext);

    /**
     * Where this overlay sits against the other registered overlays, lower drawn first.
     * It orders whole overlays rather than the elements within one, and is not a screen
     * position. On a plain overlay that is the window it draws over; a screen does not
     * draw over other things and this has no effect on one.
     * example:
     * <pre>
     * const back = Hud.createDraw2D();
     * back.addRect(0, 0, 50, 50, 0x80000000);
     * back.setZIndex(0);
     * back.register();
     * </pre>
     *
     * @param zIndex
     * @since 1.8.4
     */
    void setZIndex(int zIndex);

    /**
     * Where this overlay sits against the other registered overlays, lower drawn first.
     * This is not the z-index of any element on it; those are set per element and are on
     * the elements themselves.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * Chat.log(`overlay z-index: ${draw.getZIndex()}`);
     * </pre>
     *
     * @return
     * @since 1.8.4
     */
    int getZIndex();

}
