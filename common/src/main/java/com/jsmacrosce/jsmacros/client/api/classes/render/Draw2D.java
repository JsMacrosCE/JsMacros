package com.jsmacrosce.jsmacros.client.api.classes.render;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.render.components.*;
import com.jsmacrosce.jsmacros.client.api.classes.render.components3d.Surface;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.library.impl.FHud;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.classes.Registrable;

import java.util.*;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

/**
 * A 2D overlay that stores and draws screen elements.
 * <p>
 * Element coordinates use GUI-scaled pixels from the window's top-left corner.
 * <p>
 * The overlay renders only while registered with {@link #register()}; call
 * {@link #unregister()} to stop it.
 * <p>
 * Registering also runs initialization. When an init function is installed, the overlay is
 * emptied before that function rebuilds it; without one, hand-built elements are preserved.
 * <p>
 * Coordinates are absolute. {@link #getZIndex()} orders whole overlays (lower first),
 * while each element's z-index orders elements within its overlay.
 * <p>
 * Elements are stored in insertion order in a set; adding an equal element does not
 * create a duplicate.
 * <p>
 * Overlays can be nested with {@link #addDraw2D(Draw2D, int, int, int, int)}; cyclic
 * nesting is rejected. A {@link Surface} displays overlays in the world.
 * <pre>
 * // an overlay that builds itself, registered so it renders
 * const draw = Hud.createDraw2D();
 * draw.setOnInit(JavaWrapper.methodToJava(function (self) {
 *   self.addRect(10, 10, 110, 60, Color.argb(0x80, 0, 0, 0));
 *   self.addText("a panel", 16, 20, Color.rgb(0xFF, 0xFF, 0xFF), true);
 * }));
 * draw.register();
 *
 * // taken off again, which keeps the elements for next time
 * draw.unregister();
 * </pre>
 *
 * @author Wagyourtail
 * @see IDraw2D
 * @since 1.0.5
 */
@DocletCategory("Rendering/Graphics")
@SuppressWarnings("deprecation")
public class Draw2D implements IDraw2D<Draw2D>, Registrable<Draw2D> {
    /**
     * Elements held by this overlay in insertion order; equal elements are not duplicated.
     *
     * @since 1.0.5
     */
    protected final Set<RenderElement> elements = new LinkedHashSet<>();
    /**
     * Supplies the value returned by {@link #getWidth()}. Defaults to the window width
     * in GUI-scaled pixels; nested overlays replace it with their displayed width.
     *
     * @since 1.0.5
     */
    public IntSupplier widthSupplier;
    /**
     * Supplies the value returned by {@link #getHeight()}. Defaults to the window height
     * in GUI-scaled pixels; nested overlays replace it with their displayed height.
     *
     * @since 1.0.5
     */
    public IntSupplier heightSupplier;
    /**
     * Orders this overlay against other registered overlays, lower values first; it does
     * not affect element ordering. Defaults to {@code 0}; use {@link #setZIndex(int)} to set it.
     *
     * @since 1.0.5
     */
    public int zIndex;
    /**
     * Whether this overlay is rendered while registered. Changing visibility does not
     * unregister it or alter its elements; defaults to {@code true}.
     *
     * @since 1.8.4
     */
    public boolean visible = true;

    /**
     * @since 1.0.5
     * @deprecated please use {@link Draw2D#setOnInit(MethodWrapper)}
     */
    @Deprecated
    @Nullable
    @DocletIgnore
    public MethodWrapper<Draw2D, Object, Object, ?> onInit;
    /**
     * @since 1.1.7
     * @deprecated please use {@link Draw2D#setOnFailInit(MethodWrapper)}
     */
    @Deprecated
    @Nullable
    @DocletIgnore
    public MethodWrapper<String, Object, Object, ?> catchInit;

    protected final Minecraft mc;

    /**
     * Sets up an empty, unregistered overlay with width and height suppliers that read
     * the current window's GUI-scaled dimensions when invoked. Call {@link #register()}
     * separately to initialize and register it.
     */
    public Draw2D() {
        this.mc = Minecraft.getInstance();
        this.widthSupplier = () -> mc.getWindow().getGuiScaledWidth();
        this.heightSupplier = () -> mc.getWindow().getGuiScaledHeight();
    }

    /**
     * Returns the width supplied by {@link #widthSupplier}, in GUI-scaled pixels.
     *
     * @return the supplied width
     * @since 1.0.5
     */
    @Override
    public int getWidth() {
        return widthSupplier.getAsInt();
    }

    /**
     * Returns the height supplied by {@link #heightSupplier}, in GUI-scaled pixels.
     *
     * @return the supplied height
     * @since 1.0.5
     */
    @Override
    public int getHeight() {
        return heightSupplier.getAsInt();
    }

    /**
     * Returns this overlay's text elements in insertion order. The returned list is a
     * separate, mutable list; nested overlays' contents are not included.
     *
     * @return text elements
     * @see IDraw2D#getTexts()
     * @since 1.0.5
     */
    @DocletIgnore
    @Override
    public List<Text> getTexts() {
        List<Text> list = new LinkedList<>();
        synchronized (elements) {
            for (Renderable e : elements) {
                if (e instanceof Text) {
                    list.add((Text) e);
                }
            }
        }
        return list;
    }

    /**
     * Returns this overlay's rectangles in insertion order. The returned list is a
     * separate, mutable list; nested overlays' contents are not included.
     *
     * @return rect elements
     * @see IDraw2D#getRects()
     * @since 1.0.5
     */
    @DocletIgnore
    @Override
    public List<Rect> getRects() {
        List<Rect> list = new LinkedList<>();
        synchronized (elements) {
            for (Renderable e : elements) {
                if (e instanceof Rect) {
                    list.add((Rect) e);
                }
            }
        }
        return list;
    }

    /**
     * Returns this overlay's lines in insertion order. The returned list is a
     * separate, mutable list; nested overlays' contents are not included.
     *
     * @return the line elements
     * @since 1.8.4
     */
    @Override
    public List<Line> getLines() {
        List<Line> list = new LinkedList<>();
        synchronized (elements) {
            for (Renderable e : elements) {
                if (e instanceof Line) {
                    list.add((Line) e);
                }
            }
        }
        return list;
    }

    /**
     * Returns this overlay's item icons in insertion order. The returned list is a
     * separate, mutable list; nested overlays' contents are not included.
     *
     * @return item elements
     * @see IDraw2D#getItems()
     * @since 1.0.5
     */
    @DocletIgnore
    @Override
    public List<Item> getItems() {
        List<Item> list = new LinkedList<>();
        synchronized (elements) {
            for (Renderable e : elements) {
                if (e instanceof Item) {
                    list.add((Item) e);
                }
            }
        }
        return list;
    }

    /**
     * Returns this overlay's images in insertion order. The returned list is a
     * separate, mutable list; nested overlays' contents are not included.
     *
     * @return image elements
     * @see IDraw2D#getImages()
     * @since 1.2.3
     */
    @DocletIgnore
    @Override
    public List<Image> getImages() {
        List<Image> list = new LinkedList<>();
        synchronized (elements) {
            for (Renderable e : elements) {
                if (e instanceof Image) {
                    list.add((Image) e);
                }
            }
        }
        return list;
    }

    /**
     * Returns the nested-overlay wrappers in insertion order, not the nested overlays
     * or their contents. The returned list is a separate, mutable list; each wrapper
     * carries the nested overlay's placement and displayed dimensions.
     *
     * @return the nested-overlay wrappers
     * @since 1.8.4
     */
    @Override
    public List<Draw2DElement> getDraw2Ds() {
        List<Draw2DElement> list = new LinkedList<>();
        synchronized (elements) {
            for (Renderable e : elements) {
                if (e instanceof Draw2DElement) {
                    list.add((Draw2DElement) e);
                }
            }
        }
        return list;
    }

    /**
     * Returns an immutable snapshot of all elements on this overlay in insertion order.
     * Nested overlays appear as wrappers; their contents are not included.
     *
     * @return an immutable snapshot of this overlay's elements
     * @since 1.2.9
     */
    @Override
    public List<RenderElement> getElements() {
        return ImmutableList.copyOf(elements);
    }

    /**
     * Removes an element from this overlay. If it is absent, this has no effect; the
     * element itself is unchanged and can be added again with
     * {@link #reAddElement(RenderElement)}. Unlike the deprecated typed removals, this
     * accepts any render-element type.
     *
     * @param e the element to take off
     * @return self for chaining
     * @since 1.2.9
     */
    @Override
    public Draw2D removeElement(RenderElement e) {
        synchronized (elements) {
            elements.remove(e);
        }
        return this;
    }

    /**
     * Adds an element to this overlay and returns it. An already-present element is not
     * duplicated. Nested-overlay wrappers are rejected with {@code null} if they have no
     * underlying overlay, refer to this overlay, or would create a cycle. When this
     * overlay is a {@code Surface}, adding a nested overlay initializes it immediately.
     *
     * @param <T> the type of element to add
     * @param e the element to put back
     * @return the element, or {@code null} if it is a nested overlay that was refused
     * @since 1.2.9
     */
    @Override
    public <T extends RenderElement> T reAddElement(T e) {
        if (e instanceof Draw2DElement) {
            Draw2DElement draw2DElement = (Draw2DElement) e;
            Draw2D draw2D = draw2DElement.getDraw2D();
            if (draw2DElement.getDraw2D() == null || draw2DElement.getDraw2D() == this || hasCyclicDependencies(draw2D)) {
                return null;
            }
            if (this instanceof Surface) {
                draw2DElement.getDraw2D().init();
            }
        }
        synchronized (elements) {
            elements.add(e);
        }
        return e;
    }

    /**
     * Sets whether this overlay is rendered. Changing visibility does not unregister the
     * overlay or alter its elements.
     *
     * @param visible whether this overlay should be rendered
     * @return self for chaining.
     * @since 1.8.4
     */
    public Draw2D setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * Returns whether this overlay is set to render; this does not indicate whether it
     * is registered.
     *
     * @return {@code true} if this draw2d is visible, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Adds a nested overlay at the given position and displayed size. The nested overlay
     * has that width and height for its own elements; its elements are not included in
     * this overlay's element lists, while its wrapper appears in {@link #getDraw2Ds()}.
     * Returns {@code null} if nesting would create a cycle, including nesting this
     * overlay inside itself.
     *
     * @param draw2D the overlay to draw inside this one
     * @param x the x position of the nested overlay within this one
     * @param y the y position of the nested overlay within this one
     * @param width how wide the nested overlay thinks it is
     * @param height how tall the nested overlay thinks it is
     * @return a wrapper for the draw2d, or {@code null} if the nesting would make a cycle
     * @since 1.8.4
     */
    @Override
    public Draw2DElement addDraw2D(Draw2D draw2D, int x, int y, int width, int height) {
        return addDraw2D(draw2D, x, y, width, height, 0);
    }

    /**
     * Adds a nested overlay as in {@link #addDraw2D(Draw2D, int, int, int, int)}, with
     * the wrapper's z-index ordering it against this overlay's other elements. The
     * nested overlay's elements are ordered within that overlay. Returns {@code null}
     * if nesting would create a cycle.
     *
     * @param draw2D the overlay to draw inside this one
     * @param x the x position of the nested overlay within this one
     * @param y the y position of the nested overlay within this one
     * @param width how wide the nested overlay thinks it is
     * @param height how tall the nested overlay thinks it is
     * @param zIndex the z-index of the nested overlay against this one's other elements
     * @return a wrapper for the draw2d, or {@code null} if the nesting would make a cycle
     * @since 1.8.4
     */
    @Override
    public Draw2DElement addDraw2D(Draw2D draw2D, int x, int y, int width, int height, int zIndex) {
        Draw2DElement d = draw2DBuilder(draw2D).pos(x, y).size(width, height).zIndex(zIndex).build();
        return reAddElement(d);
    }

    /**
     * @param draw2d the draw2d to check for cyclic dependencies
     * @return whether this overlay is reachable from the proposed child through nested overlays.
     * @since 1.8.4
     */
    private boolean hasCyclicDependencies(Draw2D draw2d) {
        Deque<Draw2D> queue = new ArrayDeque<>();
        queue.addFirst(draw2d);
        // Basic BFS algorithm to check whether this instance is a descendant of the specified draw2d
        while (!queue.isEmpty()) {
            Draw2D draw2D = queue.removeFirst();
            if (this == draw2D) {
                return true;
            }
            queue.addAll(draw2D.getDraw2Ds().stream().map(Draw2DElement::getDraw2D).collect(Collectors.toList()));
        }
        return false;
    }

    /**
     * Removes the wrapper for a nested overlay from this overlay; the nested overlay and
     * its elements are unchanged.
     *
     * @param draw2D the wrapper to take off
     * @return self chaining.
     * @since 1.8.4
     */
    @Override
    public Draw2D removeDraw2D(Draw2DElement draw2D) {
        synchronized (elements) {
            elements.remove(draw2D);
        }
        return this;
    }

    /**
     * Adds text at the given GUI-scaled screen position. The color is packed with alpha
     * in the top byte; this overload uses z-index {@code 0}, scale {@code 1}, and no rotation.
     * @param text the text to display
     * @param x screen x
     * @param y screen y
     * @param color text color
     * @param shadow include shadow layer
     * @return added text
     * @see IDraw2D#addText(String, int, int, int, boolean)
     * @since 1.0.5
     */
    @Override
    public Text addText(String text, int x, int y, int color, boolean shadow) {
        return addText(text, x, y, color, 0, shadow, 1, 0);

    }

    /**
     * Adds text; despite accepting {@code zIndex}, this overload currently uses
     * z-index {@code 0}. Use the full overload to specify the element z-index.
     *
     * @param text the text to display
     * @param x screen x
     * @param y screen y
     * @param color text color
     * @param zIndex currently ignored; the added text uses z-index {@code 0}
     * @param shadow include shadow layer
     * @return added text
     * @since 1.0.5
     */
    @Override
    public Text addText(String text, int x, int y, int color, int zIndex, boolean shadow) {
        return addText(text, x, y, color, 0, shadow, 1, 0);
    }

    /**
     * Adds text with the given scale and rotation in degrees, using z-index {@code 0}.
     *
     * @param text the text to display
     * @param x screen x
     * @param y screen y
     * @param color text color
     * @param shadow include shadow layer
     * @param scale text scale (as double)
     * @param rotation text rotation (as degrees)
     * @return added text
     * @see IDraw2D#addText(String, int, int, int, boolean, double, double)
     * @since 1.2.6
     */
    @Override
    public Text addText(String text, int x, int y, int color, boolean shadow, double scale, double rotation) {
        return addText(text, x, y, color, 0, shadow, scale, rotation);
    }

    /**
     * Adds text with the specified z-index, scale, and rotation in degrees.
     *
     * @param text the text to display
     * @param x screen x
     * @param y screen y
     * @param color text color
     * @param zIndex z-index
     * @param shadow include shadow layer
     * @param scale text scale (as double)
     * @param rotation text rotation (as degrees)
     * @return added text
     * @see IDraw2D#addText(String, int, int, int, boolean, double, double)
     * @since 1.2.6
     */
    @Override
    public Text addText(String text, int x, int y, int color, int zIndex, boolean shadow, double scale, double rotation) {
        return reAddElement(new Text(text, x, y, color, zIndex, shadow, scale, (float) rotation).setParent(this));
    }

    /**
     * Adds styled text from a {@link TextHelper}; {@code color} tints its existing
     * styling. This overload uses z-index {@code 0}, scale {@code 1}, and no rotation.
     *
     * @param text the text to display
     * @param x screen x
     * @param y screen y
     * @param color text color
     * @param shadow include shadow layer
     * @return added text
     * @see IDraw2D#addText(String, int, int, int, boolean)
     * @since 1.0.5
     */
    @Override
    public Text addText(TextHelper text, int x, int y, int color, boolean shadow) {
        return addText(text, x, y, color, 0, shadow, 1, 0);
    }

    /**
     * Adds styled text from a {@link TextHelper}; {@code color} tints its existing
     * styling. This overload uses scale {@code 1} and no rotation.
     *
     * @param text the text to display
     * @param x screen x
     * @param y screen y
     * @param color text color
     * @param zIndex z-index
     * @param shadow include shadow layer
     * @return added text
     * @see IDraw2D#addText(String, int, int, int, boolean)
     * @since 1.0.5
     */
    @Override
    public Text addText(TextHelper text, int x, int y, int color, int zIndex, boolean shadow) {
        return addText(text, x, y, color, zIndex, shadow, 1, 0);
    }

    /**
     * Adds styled text from a {@link TextHelper}; {@code color} tints its existing
     * styling. This overload uses z-index {@code 0}.
     *
     * @param text the text to display
     * @param x screen x
     * @param y screen y
     * @param color text color
     * @param shadow include shadow layer
     * @param scale text scale (as double)
     * @param rotation text rotation (as degrees)
     * @return added text
     * @see IDraw2D#addText(String, int, int, int, boolean, double, double)
     * @since 1.0.5
     */
    @Override
    public Text addText(TextHelper text, int x, int y, int color, boolean shadow, double scale, double rotation) {
        return addText(text, x, y, color, 0, shadow, scale, rotation);
    }

    /**
     * Adds styled text from a {@link TextHelper}, applying the supplied tint, z-index,
     * scale, and rotation.
     *
     * @param text the text to display
     * @param x screen x
     * @param y screen y
     * @param color text color
     * @param zIndex z-index
     * @param shadow include shadow layer
     * @param scale text scale (as double)
     * @param rotation text rotation (as degrees)
     * @return added text
     * @see IDraw2D#addText(String, int, int, int, boolean, double, double)
     * @since 1.0.5
     */
    @Override
    public Text addText(TextHelper text, int x, int y, int color, int zIndex, boolean shadow, double scale, double rotation) {
        return reAddElement(new Text(text, x, y, color, zIndex, shadow, scale, (float) rotation).setParent(this));
    }

    /**
     * Removes the text element from this overlay; it does not otherwise modify it.
     *
     * @param t the text to take off
     * @return self for chaining
     * @see IDraw2D#removeText(Text)
     * @since 1.0.5
     */
    @DocletIgnore
    @Override
    public Draw2D removeText(Text t) {
        synchronized (elements) {
            elements.remove(t);
        }
        return this;
    }

    /**
     * Draws a texture region at the specified screen position and size. The region
     * coordinates and dimensions are in texture pixels; {@code width} and
     * {@code height} are the on-screen dimensions.
     *
     * @param x screen x, top left corner
     * @param y screen y, top left corner
     * @param width width on screen
     * @param height height on screen
     * @param id image id, in the form {@code minecraft:textures} path'd as found in texture packs, ie {@code assets/minecraft/textures/gui/recipe_book.png} becomes {@code minecraft:textures/gui/recipe_book.png}
     * @param imageX the left-most coordinate of the texture region
     * @param imageY the top-most coordinate of the texture region
     * @param regionWidth the width the texture region
     * @param regionHeight the height the texture region
     * @param textureWidth the width of the entire texture
     * @param textureHeight the height of the entire texture
     * @return added image
     * @see IDraw2D#addImage(int, int, int, int, String, int, int, int, int, int, int)
     * @since 1.2.3
     */
    @Override
    public Image addImage(int x, int y, int width, int height, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        return addImage(x, y, width, height, id, imageX, imageY, regionWidth, regionHeight, textureWidth, textureHeight, 0);
    }

    /**
     * Draws a texture region at the specified screen position and size, ordered by
     * {@code zIndex} against other overlay elements.
     *
     * @param x screen x, top left corner
     * @param y screen y, top left corner
     * @param width width on screen
     * @param height height on screen
     * @param zIndex z-index
     * @param id image id, in the form {@code minecraft:textures} path'd as found in texture packs, ie {@code assets/minecraft/textures/gui/recipe_book.png} becomes {@code minecraft:textures/gui/recipe_book.png}
     * @param imageX the left-most coordinate of the texture region
     * @param imageY the top-most coordinate of the texture region
     * @param regionWidth the width the texture region
     * @param regionHeight the height the texture region
     * @param textureWidth the width of the entire texture
     * @param textureHeight the height of the entire texture
     * @return added image
     * @see IDraw2D#addImage(int, int, int, int, String, int, int, int, int, int, int)
     * @since 1.2.3
     */
    @Override
    public Image addImage(int x, int y, int width, int height, int zIndex, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        return addImage(x, y, width, height, zIndex, id, imageX, imageY, regionWidth, regionHeight, textureWidth, textureHeight, 0);
    }

    /**
     * Draws a texture region at the specified screen position and size, rotated about
     * its center by {@code rotation} degrees. The position remains the unrotated
     * image's top-left corner.
     *
     * @param x screen x, top left corner
     * @param y screen y, top left corner
     * @param width width on screen
     * @param height height on screen
     * @param id image id, in the form {@code minecraft:textures} path'd as found in texture packs, ie {@code assets/minecraft/textures/gui/recipe_book.png} becomes {@code minecraft:textures/gui/recipe_book.png}
     * @param imageX the left-most coordinate of the texture region
     * @param imageY the top-most coordinate of the texture region
     * @param regionWidth the width the texture region
     * @param regionHeight the height the texture region
     * @param textureWidth the width of the entire texture
     * @param textureHeight the height of the entire texture
     * @param rotation the rotation (clockwise) of the texture (as degrees)
     * @return added image
     * @see IDraw2D#addImage(int, int, int, int, String, int, int, int, int, int, int, double)
     * @since 1.2.6
     */
    @Override
    public Image addImage(int x, int y, int width, int height, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight, double rotation) {
        return addImage(x, y, width, height, 0, id, imageX, imageY, regionWidth, regionHeight, textureWidth, textureHeight, rotation);
    }

    /**
     * @see IDraw2D#addImage(int, int, int, int, int, String, int, int, int, int, int, int, double)
     * @since 1.4.0
     */
    /**
     * Adds a texture region with a z-index and rotation about the image's center, in degrees.
     * See {@link IDraw2D#addImage(int, int, int, int, int, String, int, int, int, int, int, int, double)}.
     *
     * @param x screen x, top left corner
     * @param y screen y, top left corner
     * @param width width on screen
     * @param height height on screen
     * @param zIndex z-index
     * @param id image id, in the form {@code minecraft:textures} path'd as found in texture packs, ie {@code assets/minecraft/textures/gui/recipe_book.png} becomes {@code minecraft:textures/gui/recipe_book.png}
     * @param imageX the left-most coordinate of the texture region
     * @param imageY the top-most coordinate of the texture region
     * @param regionWidth the width the texture region
     * @param regionHeight the height the texture region
     * @param textureWidth the width of the entire texture
     * @param textureHeight the height of the entire texture
     * @param rotation the rotation (clockwise) of the texture (as degrees)
     * @return added image
     * @since 1.2.6
     */
    @Override
    public Image addImage(int x, int y, int width, int height, int zIndex, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight, double rotation) {
        return addImage(x, y, width, height, zIndex, 0xFFFFFFFF, id, imageX, imageY, regionWidth, regionHeight, textureWidth, textureHeight, rotation);
    }

    /**
     * Adds a tinted texture region with a z-index and rotation about the image's center, in degrees.
     * A white tint leaves the texture colors unchanged; shorter overloads use a white tint.
     *
     * @param x screen x, top left corner
     * @param y screen y, top left corner
     * @param width width on screen
     * @param height height on screen
     * @param zIndex z-index
     * @param color the tint laid over the image
     * @param id image id, in the form {@code minecraft:textures} path'd as found in texture packs, ie {@code assets/minecraft/textures/gui/recipe_book.png} becomes {@code minecraft:textures/gui/recipe_book.png}
     * @param imageX the left-most coordinate of the texture region
     * @param imageY the top-most coordinate of the texture region
     * @param regionWidth the width the texture region
     * @param regionHeight the height the texture region
     * @param textureWidth the width of the entire texture
     * @param textureHeight the height of the entire texture
     * @param rotation the rotation (clockwise) of the texture (as degrees)
     * @return added image
     * @see IDraw2D#addImage(int, int, int, int, int, int, String, int, int, int, int, int, int, double)
     * @since 1.6.5
     */
    @Override
    public Image addImage(int x, int y, int width, int height, int zIndex, int color, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight, double rotation) {
        return reAddElement(new Image(x, y, width, height, zIndex, color, id, imageX, imageY, regionWidth, regionHeight, textureWidth, textureHeight, (float) rotation).setParent(this));
    }

    /**
     * Adds a tinted texture region with explicit alpha, z-index, and rotation about its center, in degrees.
     *
     * @param x screen x, top left corner
     * @param y screen y, top left corner
     * @param width width on screen
     * @param height height on screen
     * @param zIndex z-index
     * @param alpha the transparency of the image, from 0 to 255
     * @param color the tint laid over the image
     * @param id image id, in the form {@code minecraft:textures} path'd as found in texture packs, ie {@code assets/minecraft/textures/gui/recipe_book.png} becomes {@code minecraft:textures/gui/recipe_book.png}
     * @param imageX the left-most coordinate of the texture region
     * @param imageY the top-most coordinate of the texture region
     * @param regionWidth the width the texture region
     * @param regionHeight the height the texture region
     * @param textureWidth the width of the entire texture
     * @param textureHeight the height of the entire texture
     * @param rotation the rotation (clockwise) of the texture (as degrees)
     * @return added image
     * @see IDraw2D#addImage(int, int, int, int, int, int, int, String, int, int, int, int, int, int, double)
     * @since 1.6.5
     */
    @Override
    public Image addImage(int x, int y, int width, int height, int zIndex, int alpha, int color, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight, double rotation) {
        return reAddElement(new Image(x, y, width, height, zIndex, alpha, color, id, imageX, imageY, regionWidth, regionHeight, textureWidth, textureHeight, (float) rotation).setParent(this));
    }

    /**
     * Removes the image from this overlay; the image object is not otherwise modified.
     *
     * @param i the image to take off
     * @return self for chaining
     * @see IDraw2D#removeImage(Image)
     * @since 1.2.3
     */
    @DocletIgnore
    @Override
    public Draw2D removeImage(Image i) {
        synchronized (elements) {
            elements.remove(i);
        }
        return this;
    }

    /**
     * Adds a filled rectangle between two corners, using the color's alpha channel and z-index 0.
     *
     * @param x1 the x coordinate of the first corner
     * @param y1 the y coordinate of the first corner
     * @param x2 the x coordinate of the second corner
     * @param y2 the y coordinate of the second corner
     * @param color as hex, with alpha channel
     * @return added rect
     * @see IDraw2D#addRect(int, int, int, int, int)
     * @since 1.0.5
     */
    @Override
    public Rect addRect(int x1, int y1, int x2, int y2, int color) {
        return reAddElement(new Rect(x1, y1, x2, y2, color, 0F, 0).setParent(this));
    }

    /**
     * Adds a rectangle between two corners with a separate alpha value; rotation and z-index are zero.
     *
     * @param x1 the x coordinate of the first corner
     * @param y1 the y coordinate of the first corner
     * @param x2 the x coordinate of the second corner
     * @param y2 the y coordinate of the second corner
     * @param color as hex
     * @param alpha alpha channel 0-255
     * @return added rect
     * @see IDraw2D#addRect(int, int, int, int, int, int)
     * @since 1.1.8
     */
    @Override
    public Rect addRect(int x1, int y1, int x2, int y2, int color, int alpha) {
        return addRect(x1, y1, x2, y2, color, alpha, 0, 0);
    }

    /**
     * @see IDraw2D#addRect(int, int, int, int, int, int, double)
     * @since 1.2.6
     */
    /**
     * Adds a rectangle between two corners with a separate alpha value. This overload
     * ignores {@code rotation} and uses rotation and z-index zero; use the full overload
     * to set both.
     *
     * @param x1 the x coordinate of the first corner
     * @param y1 the y coordinate of the first corner
     * @param x2 the x coordinate of the second corner
     * @param y2 the y coordinate of the second corner
     * @param color    as hex
     * @param alpha    alpha channel 0-255
     * @param rotation ignored; this overload always uses zero rotation
     * @return added rect
     * @see IDraw2D#addRect(int, int, int, int, int, int, double)
     * @since 1.2.6
     */
    @Override
    public Rect addRect(int x1, int y1, int x2, int y2, int color, int alpha, double rotation) {
        return addRect(x1, y1, x2, y2, color, alpha, 0, 0);
    }

    /**
     * Adds a rectangle between two corners, with rotation and a z-index. Lower z-index
     * elements are drawn first.
     *
     * @param x1 the x coordinate of the first corner
     * @param y1 the y coordinate of the first corner
     * @param x2 the x coordinate of the second corner
     * @param y2 the y coordinate of the second corner
     * @param color    rectangle color
     * @param alpha    transparency from 0 (transparent) to 255 (opaque)
     * @param rotation rotation in degrees
     * @param zIndex   draw order relative to other overlay elements
     * @return the added rectangle
     * @see IDraw2D#addRect(int, int, int, int, int, int, double)
     * @since 1.2.6
     */
    @Override
    public Rect addRect(int x1, int y1, int x2, int y2, int color, int alpha, double rotation, int zIndex) {
        return reAddElement(new Rect(x1, y1, x2, y2, color, alpha, (float) rotation, zIndex).setParent(this));
    }

    /**
     * Removes a rectangle from this overlay without modifying the rectangle itself.
     * It can be added again with {@link #reAddElement(RenderElement)}.
     *
     * @param r the rectangle to take off
     * @return self for chaining
     * @see IDraw2D#removeRect(Rect)
     * @since 1.0.5
     */
    @DocletIgnore
    @Override
    public Draw2D removeRect(Rect r) {
        synchronized (elements) {
            elements.remove(r);
        }
        return this;
    }

    /**
     * Adds a one-pixel-wide line between its start and end points, at z-index 0.
     *
     * @param x1 the x position of the start
     * @param y1 the y position of the start
     * @param x2 the x position of the end
     * @param y2 the y position of the end
     * @param color the line color, including any alpha value
     * @return the added line
     * @see IDraw2D#addLine(int, int, int, int, int)
     * @since 1.8.4
     */
    @Override
    public Line addLine(int x1, int y1, int x2, int y2, int color) {
        return addLine(x1, y1, x2, y2, color, 0);
    }

    /**
     * Adds a one-pixel-wide line between its start and end points. Lower z-index
     * elements are drawn first.
     *
     * @param x1 the x position of the start
     * @param y1 the y position of the start
     * @param x2 the x position of the end
     * @param y2 the y position of the end
     * @param color the line color, including any alpha value
     * @param zIndex draw order relative to other overlay elements
     * @return the added line
     * @see IDraw2D#addLine(int, int, int, int, int)
     * @since 1.8.4
     */
    @Override
    public Line addLine(int x1, int y1, int x2, int y2, int color, int zIndex) {
        return addLine(x1, y1, x2, y2, color, zIndex, 1);
    }

    /**
     * Adds a line with the specified width, at z-index 0.
     *
     * @param x1 the x position of the start
     * @param y1 the y position of the start
     * @param x2 the x position of the end
     * @param y2 the y position of the end
     * @param color the line color, including any alpha value
     * @param width the line width in screen pixels
     * @return the added line
     * @see IDraw2D#addLine(int, int, int, int, int)
     * @since 1.8.4
     */
    @Override
    public Line addLine(int x1, int y1, int x2, int y2, int color, double width) {
        return addLine(x1, y1, x2, y2, color, 0, width);
    }

    /**
     * Adds a line with the specified z-index and width. Lower z-index elements are
     * drawn first.
     *
     * @param x1 the x position of the start
     * @param y1 the y position of the start
     * @param x2 the x position of the end
     * @param y2 the y position of the end
     * @param color the line color, including any alpha value
     * @param zIndex draw order relative to other overlay elements
     * @param width the line width in screen pixels
     * @return the added line
     * @see IDraw2D#addLine(int, int, int, int, int)
     * @since 1.8.4
     */
    @Override
    public Line addLine(int x1, int y1, int x2, int y2, int color, int zIndex, double width) {
        return addLine(x1, y1, x2, y2, color, zIndex, width, 0);
    }

    /**
     * Adds a line with the specified width and rotation about its first point, at
     * z-index 0.
     *
     * @param x1 the x position of the start
     * @param y1 the y position of the start
     * @param x2 the x position of the end
     * @param y2 the y position of the end
     * @param color the line color, including any alpha value
     * @param width the line width in screen pixels
     * @param rotation rotation in degrees
     * @return the added line
     * @see IDraw2D#addLine(int, int, int, int, int)
     * @since 1.8.4
     */
    @Override
    public Line addLine(int x1, int y1, int x2, int y2, int color, double width, double rotation) {
        return addLine(x1, y1, x2, y2, color, 0, width, rotation);
    }

    /**
     * Adds a line with the specified z-index, width, and rotation about its first point.
     * Lower z-index elements are drawn first.
     *
     * @param x1 the x position of the start
     * @param y1 the y position of the start
     * @param x2 the x position of the end
     * @param y2 the y position of the end
     * @param color the line color, including any alpha value
     * @param zIndex draw order relative to other overlay elements
     * @param width the line width in screen pixels
     * @param rotation rotation in degrees
     * @return the added line
     * @see IDraw2D#addLine(int, int, int, int, int)
     * @since 1.8.4
     */
    @Override
    public Line addLine(int x1, int y1, int x2, int y2, int color, int zIndex, double width, double rotation) {
        Line r = new Line(x1, y1, x2, y2, color, (float) rotation, (float) width, zIndex).setParent(this);
        synchronized (elements) {
            elements.add(r);
        }
        return r;
    }

    /**
     * Removes a line from this overlay without modifying the line itself. It can be
     * added again with {@link #reAddElement(RenderElement)}.
     *
     * @param l the line to remove
     * @return this overlay for chaining
     * @see IDraw2D#removeLine(Line)
     * @since 1.8.4
     */
    @Override
    public Draw2D removeLine(Line l) {
        synchronized (elements) {
            elements.remove(l);
        }
        return this;
    }

    /**
     * Adds an item icon at the given screen position, with the game's item decorations.
     * The item ID may omit the {@code minecraft:} namespace.
     *
     * @param x      left most corner
     * @param y      top most corner
     * @param id item id
     * @return added item
     * @see IDraw2D#addItem(int, int, String)
     * @since 1.0.5
     */
    @Override
    @DocletReplaceParams("x: int, y: int, id: CanOmitNamespace<ItemId>")
    public Item addItem(int x, int y, String id) {
        return addItem(x, y, id, true);
    }

    /**
     * Adds an item icon at a position, using the default stack overlay.
     *
     * @param x      left most corner
     * @param y      top most corner
     * @param zIndex z-index
     * @param id     item id
     * @return the added item element
     * @since 1.0.5
     */
    @Override
    @DocletReplaceParams("x: int, y: int, zIndex: int, id: CanOmitNamespace<ItemId>")
    public Item addItem(int x, int y, int zIndex, String id) {
        return addItem(x, y, zIndex, id, true);
    }

    /**
     * Adds an item icon and selects whether its game-provided overlay is drawn.
     *
     * @param x       left most corner
     * @param y       top most corner
     * @param id      item id
     * @param overlay whether to draw the game's item decorations, including durability and count
     * @return added item
     * @see IDraw2D#addItem(int, int, String, boolean)
     * @since 1.2.0
     */
    @Override
    @DocletReplaceParams("x: int, y: int, id: CanOmitNamespace<ItemId>, overlay: boolean")
    public Item addItem(int x, int y, String id, boolean overlay) {
        return addItem(x, y, 0, id, overlay, 1, 0);
    }

    /**
     * Adds an item icon with a z-index and control over its game-provided overlay.
     *
     * @param x       left most corner
     * @param y       top most corner
     * @param zIndex  z-index
     * @param id      item id
     * @param overlay whether to draw the game's item decorations, including durability and count
     * @return added item
     * @since 1.2.0
     */
    @Override
    @DocletReplaceParams("x: int, y: int, zIndex: int, id: CanOmitNamespace<ItemId>, overlay: boolean")
    public Item addItem(int x, int y, int zIndex, String id, boolean overlay) {
        return addItem(x, y, zIndex, id, overlay, 1, 0);
    }

    /**
     * Adds a scaled and rotated item icon, without an explicit z-index.
     * <p>
     * Scale multiplies the icon's size; rotation is in degrees about its center.
     *
     * @param x        left most corner
     * @param y        top most corner
     * @param id       item id
     * @param overlay  whether to draw the game's item decorations
     * @param scale    size multiplier
     * @param rotation rotation in degrees
     * @return added item
     * @see IDraw2D#addItem(int, int, String, boolean, double, double)
     * @since 1.2.0
     */
    @Override
    @DocletReplaceParams("x: int, y: int, id: CanOmitNamespace<ItemId>, overlay: boolean, scale: double, rotation: double")
    public Item addItem(int x, int y, String id, boolean overlay, double scale, double rotation) {
        return addItem(x, y, 0, id, overlay, scale, rotation);
    }

    /**
     * Adds an item icon with a z-index, scale, and rotation.
     * <p>
     * Scale multiplies the icon's size; rotation is in degrees about its center.
     *
     * @param x        left most corner
     * @param y        top most corner
     * @param zIndex   z-index
     * @param id       item id
     * @param overlay  whether to draw the game's item decorations
     * @param scale    size multiplier
     * @param rotation rotation in degrees
     * @return added item
     * @since 1.2.0
     */
    @Override
    @DocletReplaceParams("x: int, y: int, zIndex: int, id: CanOmitNamespace<ItemId>, overlay: boolean, scale: double, rotation: double")
    public Item addItem(int x, int y, int zIndex, String id, boolean overlay, double scale, double rotation) {
        return reAddElement(new Item(x, y, zIndex, id, overlay, scale, (float) rotation).setParent(this));
    }

    /**
     * Adds an item icon from a stack, with the default stack overlay enabled.
     *
     * @param x    screen-pixel x-coordinate of the icon's top-left corner
     * @param y    screen-pixel y-coordinate of the icon's top-left corner
     * @param Item item stack to draw, such as one from an inventory
     * @return added item
     * @see IDraw2D#addItem(int, int, ItemStackHelper)
     * @since 1.0.5
     */
    @Override
    public Item addItem(int x, int y, ItemStackHelper Item) {
        return addItem(x, y, Item, true);
    }

    /**
     * Adds an item icon from a stack with a z-index and the default overlay enabled.
     *
     * @param x      left most corner
     * @param y      top most corner
     * @param zIndex z-index
     * @param item   from inventory as helper
     * @return the added item element
     * @since 1.0.5
     */
    @Override
    public Item addItem(int x, int y, int zIndex, ItemStackHelper item) {
        return addItem(x, y, zIndex, item, true);
    }

    /**
     * Adds an item icon from a stack and selects whether its game-provided overlay is drawn.
     *
     * @param x       left most corner
     * @param y       top most corner
     * @param Item    item stack to draw, such as one from an inventory
     * @param overlay whether to draw the game's item decorations, including durability and count
     * @return added item
     * @see IDraw2D#addItem(int, int, ItemStackHelper, boolean)
     * @since 1.2.0
     */
    @Override
    public Item addItem(int x, int y, ItemStackHelper Item, boolean overlay) {
        return addItem(x, y, Item, overlay, 1, 0);
    }

    /**
     * Adds an item icon from a stack with a z-index and control over its overlay.
     *
     * @param x       left most corner
     * @param y       top most corner
     * @param zIndex  z-index
     * @param item    from inventory as helper
     * @param overlay whether to draw the game's item decorations, including durability and count
     * @return added item
     * @since 1.2.0
     */
    @Override
    public Item addItem(int x, int y, int zIndex, ItemStackHelper item, boolean overlay) {
        return addItem(x, y, zIndex, item, overlay, 1, 0);
    }

    /**
     * Adds a scaled and rotated item icon from a stack, without an explicit z-index.
     * <p>
     * Scale multiplies the icon's size; rotation is in degrees about its center.
     *
     * @param x        left most corner
     * @param y        top most corner
     * @param item     from inventory as helper
     * @param overlay  whether to draw the game's item decorations
     * @param scale    size multiplier
     * @param rotation rotation in degrees
     * @return added item
     * @see IDraw2D#addItem(int, int, ItemStackHelper, boolean, double, double)
     * @since 1.2.6
     */
    @Override
    public Item addItem(int x, int y, ItemStackHelper item, boolean overlay, double scale, double rotation) {
        return addItem(x, y, 0, item, overlay, scale, rotation);
    }

    /**
     * Adds an item icon with the specified z-index, scale, and rotation.
     *
     * @param x        left coordinate
     * @param y        top coordinate
     * @param zIndex   element draw order; lower values draw first
     * @param item     item stack to draw
     * @param overlay  whether to draw the item overlay
     * @param scale    icon scale
     * @param rotation rotation in degrees
     * @return the added item element
     * @since 1.2.6
     */
    @Override
    public Item addItem(int x, int y, int zIndex, ItemStackHelper item, boolean overlay, double scale, double rotation) {
        return reAddElement(new Item(x, y, zIndex, item, overlay, scale, (float) rotation).setParent(this));
    }

    /**
     * Removes an item element from this overlay without changing the element itself.
     *
     * @param i the item to remove
     * @return self for chaining
     * @since 1.0.5
     */
    @DocletIgnore
    @Override
    public Draw2D removeItem(Item i) {
        synchronized (elements) {
            elements.remove(i);
        }
        return this;
    }

    /**
     * Initializes this overlay and its nested overlays. If an init callback is set,
     * this overlay's elements are cleared before it runs; otherwise they are preserved.
     * Callback failures are passed as strings to the fail callback when set, or logged
     * when it is not. Nested overlays are initialized afterward.
     *
     * @since 1.0.5
     */
    public void init() {
        if (onInit != null) {
            synchronized (elements) {
                elements.clear();
            }
            try {
                onInit.accept(this);
            } catch (Throwable e) {
                e.printStackTrace();
                try {
                    if (catchInit != null) {
                        catchInit.accept(e.toString());
                    } else {
                        throw e;
                    }
                } catch (Throwable f) {
                    JsMacrosClient.clientCore.profile.logError(f);
                }
            }
        }
        getDraw2Ds().forEach(e -> e.getDraw2D().init());
    }

    @Override
    @DocletIgnore
    public void render(GuiGraphics drawContext) {
        if (drawContext == null || !visible) {
            return;
        }

        synchronized (elements) {
            Iterator<RenderElement> iter = getElementsByZIndex();
            while (iter.hasNext()) {
                iter.next().render(drawContext, 0, 0, 0);
            }
        }
    }

    /**
     * Returns an iterator over this overlay's elements sorted by z-index, lowest first.
     *
     * @return elements in ascending z-index order
     * @since 1.8.4
     */
    public Iterator<RenderElement> getElementsByZIndex() {
        return elements.stream().sorted(Comparator.comparingInt(RenderElement::getZIndex)).iterator();
    }

    /**
     * Sets the callback run during initialization after this overlay's elements are
     * cleared. Initialization also initializes nested overlays; a callback failure is
     * handled by the configured fail callback or logged.
     *
     * @param onInit calls your method as a {@link java.util.function.Consumer Consumer}&lt;{@link Draw2D}&gt;
     * @return self for chaining
     * @since 1.2.7
     */
    @Override
    public Draw2D setOnInit(@Nullable MethodWrapper<Draw2D, Object, Object, ?> onInit) {
        this.onInit = onInit;
        return this;
    }

    /**
     * Sets the callback that receives the init callback's failure as its
     * {@code toString()} value. If unset, the failure is logged. This handles failures
     * from initialization only.
     *
     * @param catchInit calls your method as a {@link java.util.function.Consumer Consumer}&lt;{@link String String}&gt;
     * @return self for chaining
     * @since 1.2.7
     */
    @Override
    public Draw2D setOnFailInit(@Nullable MethodWrapper<String, Object, Object, ?> catchInit) {
        this.catchInit = catchInit;
        return this;
    }

    /**
     * Initializes and registers this overlay for HUD rendering.
     *
     * @return self for chaining
     * @since 1.6.5
     */
    @Override
    public Draw2D register() {
        this.init();
        FHud.overlays.add(this);
        return this;
    }

    /**
     * Removes this overlay from HUD rendering without removing its elements.
     *
     * @return self for chaining
     * @since 1.6.5
     */
    @Override
    public Draw2D unregister() {
        FHud.overlays.remove(this);
        return this;
    }

    /**
     * Sets this overlay's draw order relative to other overlays; lower values draw first.
     * This does not change the order of elements within the overlay.
     *
     * @param zIndex this overlay's order relative to other overlays
     * @since 1.8.4
     */
    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }

    /**
     * Returns this overlay's draw order relative to other overlays; lower values draw first.
     *
     * @return this overlay's order relative to other overlays
     * @since 1.8.4
     */
    public int getZIndex() {
        return zIndex;
    }

}
