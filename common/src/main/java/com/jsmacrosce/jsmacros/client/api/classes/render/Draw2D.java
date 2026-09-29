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
 * A 2D overlay: a list of things to draw on the screen, and the thing that draws them.
 * <p>
 * It holds text, rectangles, lines, images and item icons, all in screen space: pixels
 * from the top left of the window, measured after the GUI scale has been applied. That
 * is the same space the mouse readings are in, so a mouse position can be handed
 * straight to a drawing position, and the same space {@code Hud.getWindowWidth()} and
 * {@code Hud.getWindowHeight()} report.
 * <p>
 * Nothing renders until {@link #register()} is called. Building the elements is not
 * registering the overlay, so a script that fills one in and returns draws nothing.
 * The older {@code Hud.registerDraw2D} does the same thing and is deprecated, and
 * {@link #unregister()} is how it comes off again.
 * <p>
 * Registering is also what runs the init function, and an init empties the overlay
 * first. An overlay whose elements are built in {@link #setOnInit} should therefore be
 * registered rather than filled in directly.
 * <p>
 * Coordinates are absolute rather than relative to anything, so two overlays on top of
 * each other do not stack. {@link #getZIndex()} is the exception: it orders whole
 * overlays against each other, lower drawn first, rather than ordering the elements
 * within one.
 * <p>
 * The elements are held in a set rather than a list, so adding an element that compares
 * equal to one already there leaves the overlay as it was. None of the flat elements in
 * this package compare equal to anything but themselves, so in practice each one is
 * distinct and it behaves like an ordered list.
 * <p>
 * A 2D overlay can be nested inside another with {@link #addDraw2D(Draw2D, int, int,
 * int, int)}, which is how a panel is built up out of smaller ones. Nesting is checked
 * when it is added, so a cycle is refused rather than sending the renderer round in
 * circles. A flat panel in the <em>world</em> is a different thing entirely: that is a
 * surface, and a 3D overlay holds those.
 * example:
 * <pre>
 * // an overlay that builds itself, registered so it renders
 * const draw = Hud.createDraw2D();
 * draw.setOnInit(JavaWrapper.methodToJava(function (self) {
 *   self.addRect(10, 10, 110, 60, 0x80000000);
 *   self.addText("a panel", 16, 20, 0xFFFFFFFF, true);
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
     * everything this overlay holds, in the order things were added.
     * <p>
     * A set rather than a list, so adding an element that compares equal to one already
     * there leaves the overlay as it is. None of the flat elements in this package
     * compare equal to anything but themselves, so in practice each one is distinct.
     *
     * @since 1.0.5
     */
    protected final Set<RenderElement> elements = new LinkedHashSet<>();
    /**
     * where {@link #getWidth()} reads its number from.
     * <p>
     * It defaults to the window's width after the GUI scale has been applied, and
     * anything that reads the overlay's width goes through this rather than reading the
     * window. Replacing it is how a nested overlay is made to report the size of the
     * panel it is drawn on instead of the window's.
     *
     * @since 1.0.5
     */
    public IntSupplier widthSupplier;
    /**
     * where {@link #getHeight()} reads its number from.
     * <p>
     * The counterpart of {@code widthSupplier}, and replaced the same way.
     *
     * @since 1.0.5
     */
    public IntSupplier heightSupplier;
    /**
     * where this overlay sits against the other registered overlays, lower drawn first.
     * <p>
     * This orders whole overlays rather than the elements within one, so it is what to
     * reach for when two overlays would otherwise fight over the same part of the
     * screen. The elements inside this overlay are ordered by their own z-indexes
     * instead, and are not affected by this. {@link #setZIndex(int)} sets it.
     * <p>
     * The default is {@code 0}, and every overlay starts there.
     *
     * @since 1.0.5
     */
    public int zIndex;
    /**
     * whether this overlay is drawn at all.
     * <p>
     * Turning it off stops the overlay drawing without unregistering it, so it can be
     * hidden and shown again without rebuilding anything. The elements are untouched
     * either way. This is the same thing {@link #setVisible(boolean)} does.
     * <p>
     * The default is {@code true}, so a new overlay draws as soon as it is registered.
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
    public MethodWrapper<Draw2D, Object, Object, ?> onInit;
    /**
     * @since 1.1.9 [citation needed]
     * @deprecated please use {@link Draw2D#setOnFailInit(MethodWrapper)}
     */
    @Deprecated
    @Nullable
    public MethodWrapper<String, Object, Object, ?> catchInit;

    protected final Minecraft mc;

    public Draw2D() {
        this.mc = Minecraft.getInstance();
        this.widthSupplier = () -> mc.getWindow().getGuiScaledWidth();
        this.heightSupplier = () -> mc.getWindow().getGuiScaledHeight();
    }

    /**
     * how wide this overlay thinks the screen is, in scaled pixels.
     * <p>
     * This is the window's width after the GUI scale has been applied, and it is the
     * same space every element coordinate is in, so a position up to this width is on
     * screen. It reads through {@link #widthSupplier}, which anything nesting this
     * overlay replaces so a panel reports the size of what it is drawn on rather than
     * the window's.
     * <p>
     * This says nothing about how wide the overlay's own content is. There is no such
     * thing here: elements are placed by absolute coordinate and the overlay is as big
     * as whatever it is drawn over.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.register();
     * Chat.log(`screen is ${draw.getWidth()} pixels wide to this overlay`);
     * </pre>
     *
     * @return screen width
     * @see IDraw2D#getWidth()
     * @since 1.0.5
     */
    @Override
    public int getWidth() {
        return widthSupplier.getAsInt();
    }

    /**
     * how tall this overlay thinks the screen is, in scaled pixels.
     * <p>
     * The counterpart of {@link #getWidth()}, and read through
     * {@link #heightSupplier} in the same way. A position up to this height is on
     * screen.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.register();
     * Chat.log(`screen is ${draw.getHeight()} pixels tall to this overlay`);
     * </pre>
     *
     * @return screen height
     * @see IDraw2D#getHeight()
     * @since 1.0.5
     */
    @Override
    public int getHeight() {
        return heightSupplier.getAsInt();
    }

    /**
     * every text element on this overlay, in the order they were added.
     * <p>
     * A new list each call, filtered by type, so changing it does not change what this
     * overlay holds. Only text is in here; the other kinds of element have their own
     * getters.
     * <p>
     * Nested overlays are not included, so text inside a panel added with
     * {@link #addDraw2D(Draw2D, int, int, int, int)} does not show up here even though
     * it is drawn.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addText("one", 10, 10, 0xFFFFFFFF, true);
     * draw.addRect(0, 0, 10, 10, 0xFFFFFFFF);
     * draw.register();
     * // one text and one rectangle, told apart by their own getters
     * Chat.log(`${draw.getTexts().size()} texts, ${draw.getRects().size()} rects`);
     * </pre>
     *
     * @return text elements
     * @see IDraw2D#getTexts()
     * @since 1.0.5
     */
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
     * every rectangle on this overlay, in the order they were added.
     * <p>
     * A new list each call, filtered by type, so changing it does not change what this
     * overlay holds. Nested overlays are not included.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addRect(0, 0, 10, 10, 0xFFFF0000);
     * draw.addText("one", 10, 10, 0xFFFFFFFF, true);
     * draw.register();
     * Chat.log(`${draw.getRects().size()} rects, ${draw.getTexts().size()} texts`);
     * </pre>
     *
     * @return rect elements
     * @see IDraw2D#getRects()
     * @since 1.0.5
     */
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
     * @see IDraw2D#getItems()
     * @since 1.0.5
     */
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
     * @see IDraw2D#getImages()
     * @since 1.2.3
     */
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
     * every nested overlay on this one, in the order they were added.
     * <p>
     * A new list each call, filtered by type, so changing it does not change what this
     * overlay holds. This is the list of wrappers rather than of the overlays
     * themselves; each wrapper reports the position and size the nested overlay is drawn
     * at, which is not the same as the nested overlay's own idea of its size.
     * <p>
     * The contents of the nested overlays are not in here, only the wrappers.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * const panel = Hud.createDraw2D();
     * panel.addText("inside", 5, 5, 0xFFFFFFFF, true);
     * draw.addDraw2D(panel, 10, 10, 100, 50);
     * draw.register();
     * // one nested panel; its text is not in this overlay's own text list
     * Chat.log(`${draw.getDraw2Ds().size()} nested, ${draw.getTexts().size()} of our own texts`);
     * </pre>
     *
     * @return all registered draw2d elements.
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
     * everything on this overlay, of every kind, in the order things were added.
     * <p>
     * A read-only snapshot rather than the overlay's own collection, so changing it
     * does not change what this overlay holds and elements taken off afterwards do not
     * disappear from a copy already taken. The types have to be told apart with their
     * own getters; the getters are the easier route.
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
    @Override
    public List<RenderElement> getElements() {
        return ImmutableList.copyOf(elements);
    }

    /**
     * takes any element off this overlay, whatever kind it is.
     * <p>
     * This is the one removal that does not need to know the type, which is what makes
     * it the right one for an element read back out of {@link #getElements()}. The typed
     * removals such as {@link #removeText(Text)} do the same thing and are deprecated.
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
    @Override
    public Draw2D removeElement(RenderElement e) {
        synchronized (elements) {
            elements.remove(e);
        }
        return this;
    }

    /**
     * puts an element back onto this overlay.
     * <p>
     * For one taken off with {@link #removeElement(RenderElement)}, or built with
     * {@code build()} rather than {@code buildAndAdd()}. Adding an element that is
     * already on the overlay leaves it as it is, because the overlay holds a set, and
     * still reports the element back.
     * <p>
     * A nested overlay is the case that can fail, and this is where that is checked. A
     * nested overlay is refused, and {@code null} is returned, when it has no overlay
     * behind it, when it is this overlay, or when it already contains this one further
     * down. That last one is the cycle case: without the check the renderer would follow
     * the nesting round and round.
     * <p>
     * A nested overlay added to a surface is initialised as it is added, so its own
     * init function runs at that point rather than waiting.
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
     * whether to render this element.
     * <p>
     * This hides and shows the overlay without unregistering it, so it can be toggled
     * without rebuilding anything. The elements are untouched either way, so showing it
     * again brings back exactly what was there before. It is the same thing as writing
     * the {@code visible} field.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
     * draw.register();
     * // hidden, but still registered and still holding its text
     * draw.setVisible(false);
     * Chat.log(`${draw.getTexts().size()} texts, none of them drawn`);
     * </pre>
     *
     * @param visible whether to render this element.
     * @return self for chaining.
     * @since 1.8.4
     */
    public Draw2D setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * A new overlay is visible before it has been registered, so this says whether the
     * overlay would draw rather than whether it is currently being drawn.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * Chat.log(`visible before registering: ${draw.isVisible()}`);
     * </pre>
     *
     * @return {@code true} if this draw2d is visible, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * draws another 2D overlay inside this one, at a position and size.
     * <p>
     * This is how a panel is built up out of smaller overlays. The nested overlay is
     * drawn at {@code x}, {@code y} inside this one, and reports the width and height
     * given here as its own screen size, so its elements are placed relative to that
     * rather than to the window.
     * <p>
     * What the nested overlay draws is not in this overlay's own element lists, so
     * {@link #getTexts()} and its siblings do not show it; it appears in
     * {@link #getDraw2Ds()} as a wrapper.
     * <p>
     * A cycle is refused and this returns {@code null}: an overlay cannot be put inside
     * itself, and it cannot be put inside something it already contains.
     * example:
     * <pre>
     * const outer = Hud.createDraw2D();
     * const panel = Hud.createDraw2D();
     * panel.addText("inside a panel", 5, 5, 0xFFFFFFFF, true);
     * // the panel is 100 by 50 as far as its own elements are concerned
     * outer.addDraw2D(panel, 20, 20, 100, 50);
     * outer.register();
     * </pre>
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
     * draws another 2D overlay inside this one, with its own z-index.
     * <p>
     * The same as the shorter form, with the z-index of the wrapper set so this nested
     * overlay can be ordered against the other elements on this one. The elements
     * <em>inside</em> the nested overlay keep their own z-indexes and are ordered among
     * themselves, not against this one.
     * <p>
     * A cycle is refused and this returns {@code null}, as in the shorter form.
     * example:
     * <pre>
     * const outer = Hud.createDraw2D();
     * const back = Hud.createDraw2D();
     * back.addRect(0, 0, 100, 50, 0x80000000);
     * const front = Hud.createDraw2D();
     * front.addText("on top", 5, 5, 0xFFFFFFFF, true);
     * outer.addDraw2D(back, 20, 20, 100, 50, 0);
     * outer.addDraw2D(front, 20, 20, 100, 50, 1);
     * outer.register();
     * </pre>
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
     * @return {@code true} if adding the child to the parent would result in a cyclic dependency.
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
     * takes a nested overlay off this one.
     * <p>
     * Only the wrapper comes off. The overlay that was inside it is left as it is and can
     * be put back with {@link #addDraw2D(Draw2D, int, int, int, int)}, and whatever was
     * drawn inside it is still on it. {@link #removeElement(RenderElement)} does the same
     * thing without needing the wrapper type.
     * example:
     * <pre>
     * const outer = Hud.createDraw2D();
     * const panel = Hud.createDraw2D();
     * panel.addText("inside", 5, 5, 0xFFFFFFFF, true);
     * const wrapper = outer.addDraw2D(panel, 20, 20, 100, 50);
     * outer.register();
     * outer.removeDraw2D(wrapper);
     * Chat.log(`${outer.getDraw2Ds().size()} nested overlays left`);
     * </pre>
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
     * adds a piece of text at a position, with a colour and a shadow.
     * <p>
     * The position is in screen pixels from the top left, the colour is packed as the
     * game packs colours with the alpha in the top byte, and the shadow is the game's
     * own drop shadow behind the glyphs. This is the shortest form and puts the text at
     * z-index 0 with a scale of 1 and no rotation.
     * <p>
     * Coordinates are absolute, so text added at the same place twice sits on top of
     * itself rather than stacking.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
     * draw.register();
     * </pre>
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
     * adds a piece of text with a z-index.
     * <p>
     * The z-index orders this text against the other elements on this overlay, lower
     * drawn first, and the two texts in the example below are at the same place for
     * exactly that reason. It is not the overlay's own z-index, which orders whole
     * overlays; see {@link #getZIndex()} for that.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addText("behind", 10, 10, 0xFFFF0000, 0, true);
     * draw.addText("in front", 10, 10, 0xFF00FF00, 1, true);
     * draw.register();
     * </pre>
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
    public Text addText(String text, int x, int y, int color, int zIndex, boolean shadow) {
        return addText(text, x, y, color, 0, shadow, 1, 0);
    }

    /**
     * adds a piece of text with a scale and a rotation.
     * <p>
     * The scale multiplies the glyph size and the rotation turns the text in degrees,
     * both about the text's own top left corner. Neither affects the position, which is
     * still where the top left corner goes, so a scaled and rotated text reaches
     * further right and down from the same coordinates.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addText("tilted", 20, 20, 0xFFFFFFFF, true, 1.5, 15);
     * draw.register();
     * </pre>
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
     * adds a piece of text with a z-index, a scale and a rotation, which is the full
     * form of the other text helpers.
     * <p>
     * Every shorter form ends up here, so anything a script can express about a piece of
     * text it can express through this.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addText("everything", 20, 20, 0xFFFFFFFF, 1, true, 1.5, 15);
     * draw.register();
     * </pre>
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
     * adds a piece of already-built text, keeping whatever styling it has.
     * <p>
     * This takes a {@link TextHelper} rather than a plain string, so the text keeps its
     * own colours and formatting. The {@code color} argument is a tint over the top
     * rather than a replacement, so a helper with no styling of its own draws the same
     * as the string form.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addText(Chat.createTextHelperFromString("a green line"), 10, 10, 0xFFFFFFFF, true);
     * draw.register();
     * </pre>
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
     * adds a piece of already-built text with a z-index, keeping its styling.
     * <p>
     * The z-index orders this text against the other elements on this overlay, lower
     * drawn first, and has nothing to do with the styling the helper carries.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addText(Chat.createTextHelperFromString("a green line"), 10, 10, 0xFFFFFFFF, 1, true);
     * draw.register();
     * </pre>
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
     * adds a piece of already-built text with a scale and a rotation, keeping its
     * styling.
     * <p>
     * The scale and the rotation turn the whole text element, so they apply on top of
     * whatever the helper itself is styled with rather than changing it.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addText(Chat.createTextHelperFromString("tilted"), 20, 20, 0xFFFFFFFF, true, 1.5, 15);
     * draw.register();
     * </pre>
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
     * adds a piece of already-built text with everything, keeping its styling. This is
     * the full form of the other text helpers.
     * <p>
     * Every shorter form ends up here, so anything a script can express about a piece of
     * text it can express through this.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addText(Chat.createTextHelperFromString("everything"), 20, 20, 0xFFFFFFFF, 1, true, 1.5, 15);
     * draw.register();
     * </pre>
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
     * takes a piece of text off this overlay.
     * <p>
     * The text itself is left alone, so it can be put back with
     * {@link #reAddElement(RenderElement)} and comes back as it was.
     * {@link #removeElement(RenderElement)} does the same thing without needing the
     * type, and is not deprecated.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * const text = draw.addText("temporary", 10, 10, 0xFFFFFFFF, true);
     * draw.register();
     * draw.removeText(text);
     * Chat.log(`${draw.getTexts().size()} texts left`);
     * </pre>
     *
     * @param t the text to take off
     * @return self for chaining
     * @see IDraw2D#removeText(Text)
     * @since 1.0.5
     */
    @Override
    public Draw2D removeText(Text t) {
        synchronized (elements) {
            elements.remove(t);
        }
        return this;
    }

    /**
     * draws a region of a texture as an image.
     * <p>
     * The image is placed at {@code x}, {@code y} in screen pixels and drawn
     * {@code width} by {@code height} on the screen, from a region of a larger texture.
     * The two sets of coordinates are different things: the region is where in the
     * texture the pixels come from, and the width and height are how big that region
     * ends up on screen, so the same region can be drawn at any size.
     * <p>
     * A texture is named the way a texture pack names it, so
     * {@code assets/minecraft/textures/gui/recipe_book.png} is
     * {@code minecraft:textures/gui/recipe_book.png}. A texture made with
     * {@code Hud.createTexture} is named by whatever was passed for its name instead.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * // one item icon, drawn 16 by 16 at its natural size
     * draw.addImage(10, 10, 16, 16, "minecraft:item/diamond", 0, 0, 16, 16, 16, 16);
     * draw.register();
     * </pre>
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
     * draws a region of a texture as an image, with a z-index.
     * <p>
     * The same as the shorter form, with the z-index ordering this image against the
     * other elements on this overlay, lower drawn first.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addImage(10, 10, 16, 16, 1, "minecraft:item/diamond", 0, 0, 16, 16, 16, 16);
     * draw.register();
     * </pre>
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
     * draws a region of a texture as an image, turned by an angle.
     * <p>
     * The rotation is in degrees and turns the image about its own middle. The position
     * given is still where the image's top left corner goes, so a rotated image reaches
     * further left and up than an unrotated one at the same coordinates.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addImage(20, 20, 16, 16, "minecraft:item/diamond", 0, 0, 16, 16, 16, 16, 15);
     * draw.register();
     * </pre>
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
     * draws a region of a texture as an image, with a z-index and turned by an angle.
     * <p>
     * The z-index orders this image against the other elements on this overlay, and the
     * rotation turns it about its own middle in degrees.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addImage(20, 20, 16, 16, 1, "minecraft:item/diamond", 0, 0, 16, 16, 16, 16, 15);
     * draw.register();
     * </pre>
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
     * @see IDraw2D#addImage(int, int, int, int, String, int, int, int, int, int, int, double)
     * @since 1.2.6
     */
    @Override
    public Image addImage(int x, int y, int width, int height, int zIndex, String id, int imageX, int imageY, int regionWidth, int regionHeight, int textureWidth, int textureHeight, double rotation) {
        return addImage(x, y, width, height, zIndex, 0xFFFFFFFF, id, imageX, imageY, regionWidth, regionHeight, textureWidth, textureHeight, rotation);
    }

    /**
     * draws a region of a texture as a tinted image, with a z-index and a rotation.
     * <p>
     * The colour is a tint laid over the texture's own pixels rather than a replacement,
     * so a fully white tint leaves the image as it is in the texture and anything else
     * shifts it towards that colour. The shorter forms leave the tint fully white, which
     * is why they take no colour at all.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * // the same icon, tinted red
     * draw.addImage(20, 20, 16, 16, 0, 0xFFFF0000, "minecraft:item/diamond", 0, 0, 16, 16, 16, 16, 0);
     * draw.register();
     * </pre>
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
     * draws a tinted image with its transparency given separately, which is the full
     * form of the image helpers.
     * <p>
     * The alpha is the image's own transparency and the colour is the tint over it, so
     * the two do different jobs: the alpha decides how much of the background shows
     * through and the tint decides what colour the pixels are drawn. Every shorter form
     * ends up here, so anything a script can express about an image it can express
     * through this.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * // a red tinted icon at half transparency
     * draw.addImage(20, 20, 16, 16, 0, 128, 0xFF0000, "minecraft:item/diamond", 0, 0, 16, 16, 16, 16, 0);
     * draw.register();
     * </pre>
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
     * takes an image off this overlay.
     * <p>
     * The image itself is left alone, so it can be put back with
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
     * @param i the image to take off
     * @return self for chaining
     * @see IDraw2D#removeImage(Image)
     * @since 1.2.3
     */
    @Override
    public Draw2D removeImage(Image i) {
        synchronized (elements) {
            elements.remove(i);
        }
        return this;
    }

    /**
     * adds a filled rectangle between two corners.
     * <p>
     * The two coordinates are opposite corners rather than a minimum and a maximum, so
     * they can be given in any order and the rectangle is the same either way. It covers
     * the area between them, so {@code 0, 0, 10, 10} is ten by ten rather than eleven.
     * <p>
     * This is the shortest form and draws the rectangle fully opaque, with no z-index
     * of its own beyond 0 and no rotation.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addRect(10, 10, 110, 60, 0x80000000);
     * draw.register();
     * </pre>
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
     * adds a rectangle between two corners, with its transparency given separately.
     * <p>
     * The two coordinates are opposite corners rather than a minimum and a maximum, so
     * they can be given in any order. Nothing is filled in here, so an alpha of
     * {@code 0} really does make the rectangle invisible rather than being treated as
     * a missing one.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * // the same rectangle at half transparency
     * draw.addRect(10, 10, 110, 60, 0x000000, 128);
     * draw.register();
     * </pre>
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
     * adds a rectangle between two corners, with a rotation.
     * <p>
     * The rotation is in degrees and turns the rectangle about its own middle. The
     * corners given are still the unrotated ones, so the rectangle reaches further left
     * and up once it is turned.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addRect(60, 10, 110, 60, 0x80000000, 255, 15);
     * draw.register();
     * </pre>
     *
     * @param x1 the x coordinate of the first corner
     * @param y1 the y coordinate of the first corner
     * @param x2 the x coordinate of the second corner
     * @param y2 the y coordinate of the second corner
     * @param color    as hex
     * @param alpha    alpha channel 0-255
     * @param rotation as degrees
     * @return added rect
     * @see IDraw2D#addRect(int, int, int, int, int, int, double)
     * @since 1.2.6
     */
    @Override
    public Rect addRect(int x1, int y1, int x2, int y2, int color, int alpha, double rotation) {
        return addRect(x1, y1, x2, y2, color, alpha, 0, 0);
    }

    /**
     * adds a rectangle between two corners, with a rotation and a z-index, which is the
     * full form of the rectangle helpers.
     * <p>
     * The z-index orders this rectangle against the other elements on this overlay,
     * lower drawn first, and is what two rectangles drawn in the same place are ordered
     * by. Every shorter form ends up here.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addRect(10, 10, 110, 60, 0x80000000, 255, 0, 1);
     * draw.register();
     * </pre>
     *
     * @param x1 the x coordinate of the first corner
     * @param y1 the y coordinate of the first corner
     * @param x2 the x coordinate of the second corner
     * @param y2 the y coordinate of the second corner
     * @param color    as hex
     * @param alpha    alpha channel 0-255
     * @param rotation as degrees
     * @param zIndex   z-index
     * @return added rect
     * @see IDraw2D#addRect(int, int, int, int, int, int, double)
     * @since 1.2.6
     */
    @Override
    public Rect addRect(int x1, int y1, int x2, int y2, int color, int alpha, double rotation, int zIndex) {
        return reAddElement(new Rect(x1, y1, x2, y2, color, alpha, (float) rotation, zIndex).setParent(this));
    }

    /**
     * takes a rectangle off this overlay.
     * <p>
     * The rectangle itself is left alone, so it can be put back with
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
     * @param r the rectangle to take off
     * @return self for chaining
     * @see IDraw2D#removeRect(Rect)
     * @since 1.0.5
     */
    @Override
    public Draw2D removeRect(Rect r) {
        synchronized (elements) {
            elements.remove(r);
        }
        return this;
    }

    /**
     * adds a line between two points.
     * <p>
     * The two coordinates are the start and the end rather than a minimum and a maximum,
     * so a line has a direction and can be drawn either way round. The width is one
     * pixel and it is drawn at z-index 0.
     * <p>
     * A line whose two ends are the same point has no direction and nothing to draw.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addLine(0, 0, 50, 50, 0xFFFFFFFF);
     * draw.register();
     * </pre>
     *
     * @param x1 the x position of the start
     * @param y1 the y position of the start
     * @param x2 the x position of the end
     * @param y2 the y position of the end
     * @param color the color of the line, can include alpha value
     * @return the added line.
     * @see IDraw2D#addLine(int, int, int, int, int)
     * @since 1.8.4
     */
    @Override
    public Line addLine(int x1, int y1, int x2, int y2, int color) {
        return addLine(x1, y1, x2, y2, color, 0);
    }

    /**
     * adds a line between two points, with a z-index.
     * <p>
     * The z-index orders this line against the other elements on this overlay, lower
     * drawn first, and is what two lines crossing at the same place are ordered by. The
     * width is one pixel.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addLine(0, 0, 50, 50, 0xFFFF0000, 0);
     * draw.addLine(0, 50, 50, 0, 0xFF00FF00, 1);
     * draw.register();
     * </pre>
     *
     * @param x1 the x position of the start
     * @param y1 the y position of the start
     * @param x2 the x position of the end
     * @param y2 the y position of the end
     * @param color the color of the line, can include alpha value
     * @param zIndex the z-index of the line
     * @return the added line.
     * @see IDraw2D#addLine(int, int, int, int, int)
     * @since 1.8.4
     */
    @Override
    public Line addLine(int x1, int y1, int x2, int y2, int color, int zIndex) {
        return addLine(x1, y1, x2, y2, color, zIndex, 1);
    }

    /**
     * adds a line between two points, with a width.
     * <p>
     * The width is in screen pixels and straddles the line rather than being all on
     * one side of it, so a width of 4 makes a line two pixels either side of where the
     * two points are. It is drawn at z-index 0.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addLine(0, 0, 50, 50, 0xFFFFFFFF, 4);
     * draw.register();
     * </pre>
     *
     * @param x1 the x position of the start
     * @param y1 the y position of the start
     * @param x2 the x position of the end
     * @param y2 the y position of the end
     * @param color the color of the line, can include alpha value
     * @param width the width of the line
     * @return the added line.
     * @see IDraw2D#addLine(int, int, int, int, int)
     * @since 1.8.4
     */
    @Override
    public Line addLine(int x1, int y1, int x2, int y2, int color, double width) {
        return addLine(x1, y1, x2, y2, color, 0, width);
    }

    /**
     * adds a line between two points, with a z-index and a width.
     * <p>
     * The z-index orders this line against the other elements on this overlay and the
     * width is in screen pixels, straddling the line.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addLine(0, 0, 50, 50, 0xFFFFFFFF, 1, 4);
     * draw.register();
     * </pre>
     *
     * @param x1 the x position of the start
     * @param y1 the y position of the start
     * @param x2 the x position of the end
     * @param y2 the y position of the end
     * @param color the color of the line, can include alpha value
     * @param zIndex the z-index of the line
     * @param width the width of the line
     * @return the added line.
     * @see IDraw2D#addLine(int, int, int, int, int)
     * @since 1.8.4
     */
    @Override
    public Line addLine(int x1, int y1, int x2, int y2, int color, int zIndex, double width) {
        return addLine(x1, y1, x2, y2, color, zIndex, width, 0);
    }

    /**
     * adds a line between two points, with a width and a rotation.
     * <p>
     * The rotation is in degrees and turns the whole line about its middle, so the two
     * points given are where the line would be with no rotation. It is drawn at z-index
     * 0.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addLine(25, 25, 75, 25, 0xFFFFFFFF, 2, 30);
     * draw.register();
     * </pre>
     *
     * @param x1 the x position of the start
     * @param y1 the y position of the start
     * @param x2 the x position of the end
     * @param y2 the y position of the end
     * @param color the color of the line, can include alpha value
     * @param width the width of the line
     * @param rotation the rotation (clockwise) of the line (as degrees)
     * @return the added line.
     * @see IDraw2D#addLine(int, int, int, int, int)
     * @since 1.8.4
     */
    @Override
    public Line addLine(int x1, int y1, int x2, int y2, int color, double width, double rotation) {
        return addLine(x1, y1, x2, y2, color, 0, width, rotation);
    }

    /**
     * adds a line between two points, with a z-index, a width and a rotation, which is
     * the full form of the line helpers.
     * <p>
     * The z-index orders this line against the other elements on this overlay, the width
     * is in screen pixels and straddles the line, and the rotation turns it about its
     * middle in degrees. Every shorter form ends up here.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addLine(25, 25, 75, 25, 0xFFFFFFFF, 1, 2, 30);
     * draw.register();
     * </pre>
     *
     * @param x1 the x position of the start
     * @param y1 the y position of the start
     * @param x2 the x position of the end
     * @param y2 the y position of the end
     * @param color the color of the line, can include alpha value
     * @param zIndex the z-index of the line
     * @param width the width of the line
     * @param rotation the rotation (clockwise) of the line (as degrees)
     * @return the added line.
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
     * takes a line off this overlay.
     * <p>
     * The line itself is left alone, so it can be put back with
     * {@link #reAddElement(RenderElement)} and comes back as it was.
     * {@link #removeElement(RenderElement)} does the same thing without needing the type.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * const line = draw.addLine(0, 0, 50, 50, 0xFFFFFFFF);
     * draw.register();
     * draw.removeLine(line);
     * Chat.log(`${draw.getLines().size()} lines left`);
     * </pre>
     *
     * @param l the line to remove
     * @return self chaining.
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
     * draws an item icon at a position.
     * <p>
     * The id names the item the way the game does, and the namespace may be left off
     * for anything in {@code minecraft}. The position is in screen pixels from the top
     * left and is where the icon's top left corner goes. The icon is drawn at its
     * natural size with the durability bar and stack count showing.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addItem(10, 10, "minecraft:diamond");
     * draw.register();
     * </pre>
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
     * draws an item icon at a position, with a z-index.
     * <p>
     * <b>This overload is not implemented and returns {@code null}.</b> The body is an
     * unconditional {@code null}, so nothing is added to the overlay and the return
     * value is not an item. The other {@code addItem} forms all work, so the working
     * route is {@link #addItem(int, int, int, String, boolean)} with the overlay
     * argument set, or the builder.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * // this one adds nothing and hands back null
     * const nothing = draw.addItem(10, 10, 0, "minecraft:diamond");
     * // the working form for the same thing
     * draw.addItem(10, 10, 0, "minecraft:diamond", true);
     * draw.register();
     * </pre>
     *
     * @param x      left most corner
     * @param y      top most corner
     * @param zIndex z-index
     * @param id     item id
     * @return {@code null} always; nothing is added
     * @since 1.0.5
     */
    @Override
    @DocletReplaceParams("x: int, y: int, zIndex: int, id: CanOmitNamespace<ItemId>")
    public Item addItem(int x, int y, int zIndex, String id) {
        return null;
    }

    /**
     * draws an item icon at a position, with a choice of whether the stack overlay shows.
     * <p>
     * The overlay is the durability bar and the stack count drawn on top of the icon.
     * Turning it off gives a clean icon, which is usually what a script wants when it is
     * drawing one item rather than a whole inventory.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * // a clean icon, with no stack count or durability bar
     * draw.addItem(10, 10, "minecraft:diamond", false);
     * draw.register();
     * </pre>
     *
     * @param x       left most corner
     * @param y       top most corner
     * @param id      item id
     * @param overlay should include overlay health and count
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
     * draws an item icon at a position, with a z-index and a choice of whether the
     * stack overlay shows.
     * <p>
     * The z-index orders this icon against the other elements on this overlay, lower
     * drawn first, and the overlay is the durability bar and stack count. This is the
     * shortest form that actually adds anything, and the one to reach for when the
     * z-indexed {@link #addItem(int, int, int, String)} is wanted.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addItem(10, 10, 1, "minecraft:diamond", false);
     * draw.register();
     * </pre>
     *
     * @param x       left most corner
     * @param y       top most corner
     * @param zIndex  z-index
     * @param id      item id
     * @param overlay should include overlay health and count
     * @return added item
     * @since 1.2.0
     */
    @Override
    @DocletReplaceParams("x: int, y: int, zIndex: int, id: CanOmitNamespace<ItemId>, overlay: boolean")
    public Item addItem(int x, int y, int zIndex, String id, boolean overlay) {
        return addItem(x, y, zIndex, id, overlay, 1, 0);
    }

    /**
     * draws an item icon at a position, with a scale and a rotation.
     * <p>
     * The scale multiplies the icon's size and the rotation turns it in degrees, both
     * about the icon's own middle rather than its top left corner. The position given is
     * still where the top left corner goes, so a scaled and rotated icon reaches further
     * right and down from the same coordinates.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addItem(40, 40, "minecraft:diamond", false, 2, 15);
     * draw.register();
     * </pre>
     *
     * @param x        left most corner
     * @param y        top most corner
     * @param id       item id
     * @param overlay  should include overlay health and count
     * @param scale    scale of item
     * @param rotation rotation of item
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
     * draws an item icon at a position, with a z-index, a scale and a rotation, which is
     * the full form of the item helpers.
     * <p>
     * Every other {@code addItem} form that takes an item id ends up here, so anything a
     * script can express about an item icon it can express through this. The z-index
     * orders the icon against the other elements on this overlay, the scale multiplies
     * its size and the rotation turns it about its middle in degrees.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addItem(40, 40, 1, "minecraft:diamond", false, 2, 15);
     * draw.register();
     * </pre>
     *
     * @param x        left most corner
     * @param y        top most corner
     * @param zIndex   z-index
     * @param id       item id
     * @param overlay  should include overlay health and count
     * @param scale    scale of item
     * @param rotation rotation of item
     * @return added item
     * @since 1.2.0
     */
    @Override
    @DocletReplaceParams("x: int, y: int, zIndex: int, id: CanOmitNamespace<ItemId>, overlay: boolean, scale: double, rotation: double")
    public Item addItem(int x, int y, int zIndex, String id, boolean overlay, double scale, double rotation) {
        return reAddElement(new Item(x, y, zIndex, id, overlay, scale, (float) rotation).setParent(this));
    }

    /**
     * draws an item icon from an item stack.
     * <p>
     * This draws whatever the stack is rather than an item named by an id, so the count
     * and the damage on the stack are what the icon shows. The position is in screen
     * pixels and is where the icon's top left corner goes, and the stack overlay is on.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const draw = Hud.createDraw2D();
     *   draw.addItem(10, 10, player.getMainHand());
     *   draw.register();
     * }
     * </pre>
     *
     * @param x    left most corner
     * @param y    top most corner
     * @param item from inventory as helper
     * @return added item
     * @see IDraw2D#addItem(int, int, ItemStackHelper)
     * @since 1.0.5
     */
    @Override
    public Item addItem(int x, int y, ItemStackHelper Item) {
        return addItem(x, y, Item, true);
    }

    /**
     * draws an item icon from an item stack, with a z-index.
     * <p>
     * <b>This overload is not implemented and returns {@code null}.</b> The body is an
     * unconditional {@code null}, so nothing is added to the overlay and the return
     * value is not an item. {@link #addItem(int, int, int, ItemStackHelper, boolean)}
     * with the overlay argument set is the working form of the same thing.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const draw = Hud.createDraw2D();
     *   const stack = player.getMainHand();
     *   // this one adds nothing and hands back null
     *   draw.addItem(10, 10, 0, stack);
     *   // the working form for the same thing
     *   draw.addItem(10, 10, 0, stack, true);
     *   draw.register();
     * }
     * </pre>
     *
     * @param x      left most corner
     * @param y      top most corner
     * @param zIndex z-index
     * @param item   from inventory as helper
     * @return {@code null} always; nothing is added
     * @since 1.0.5
     */
    @Override
    public Item addItem(int x, int y, int zIndex, ItemStackHelper item) {
        return null;
    }

    /**
     * draws an item icon from an item stack, with a choice of whether the stack overlay
     * shows.
     * <p>
     * The overlay is the durability bar and stack count drawn on top of the icon, and
     * turning it off gives a clean icon.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const draw = Hud.createDraw2D();
     *   draw.addItem(10, 10, player.getMainHand(), false);
     *   draw.register();
     * }
     * </pre>
     *
     * @param x       left most corner
     * @param y       top most corner
     * @param item    from inventory as helper
     * @param overlay should include overlay health and count
     * @return added item
     * @see IDraw2D#addItem(int, int, ItemStackHelper, boolean)
     * @since 1.2.0
     */
    @Override
    public Item addItem(int x, int y, ItemStackHelper Item, boolean overlay) {
        return addItem(x, y, Item, overlay, 1, 0);
    }

    /**
     * draws an item icon from an item stack, with a z-index and a choice of whether the
     * stack overlay shows.
     * <p>
     * This is the shortest form of the item stack helpers that actually adds anything,
     * and the one to reach for when the z-indexed
     * {@link #addItem(int, int, int, ItemStackHelper)} is wanted.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const draw = Hud.createDraw2D();
     *   draw.addItem(10, 10, 1, player.getMainHand(), false);
     *   draw.register();
     * }
     * </pre>
     *
     * @param x       left most corner
     * @param y       top most corner
     * @param zIndex  z-index
     * @param item    from inventory as helper
     * @param overlay should include overlay health and count
     * @return added item
     * @since 1.2.0
     */
    @Override
    public Item addItem(int x, int y, int zIndex, ItemStackHelper item, boolean overlay) {
        return addItem(x, y, zIndex, item, overlay, 1, 0);
    }

    /**
     * draws an item icon from an item stack, with a scale and a rotation.
     * <p>
     * The scale multiplies the icon's size and the rotation turns it in degrees, both
     * about the icon's own middle. The position given is still where the top left corner
     * goes.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const draw = Hud.createDraw2D();
     *   draw.addItem(40, 40, player.getMainHand(), false, 2, 15);
     *   draw.register();
     * }
     * </pre>
     *
     * @param x        left most corner
     * @param y        top most corner
     * @param item     from inventory as helper
     * @param overlay  should include overlay health and count
     * @param scale    scale of item
     * @param rotation rotation of item
     * @return added item
     * @see IDraw2D#addItem(int, int, ItemStackHelper, boolean, double, double)
     * @since 1.2.6
     */
    @Override
    public Item addItem(int x, int y, ItemStackHelper item, boolean overlay, double scale, double rotation) {
        return addItem(x, y, 0, item, overlay, scale, rotation);
    }

    /**
     * draws an item icon from an item stack, with a z-index, a scale and a rotation,
     * which is the full form of the item stack helpers.
     * <p>
     * Every other {@code addItem} form that takes an item stack ends up here, so anything
     * a script can express about an item icon it can express through this.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const draw = Hud.createDraw2D();
     *   draw.addItem(40, 40, 1, player.getMainHand(), false, 2, 15);
     *   draw.register();
     * }
     * </pre>
     *
     * @param x        left most corner
     * @param y        top most corner
     * @param zIndex   z-index
     * @param item     from inventory as helper
     * @param overlay  should include overlay health and count
     * @param scale    scale of item
     * @param rotation rotation of item
     * @return added item
     * @since 1.2.6
     */
    @Override
    public Item addItem(int x, int y, int zIndex, ItemStackHelper item, boolean overlay, double scale, double rotation) {
        return reAddElement(new Item(x, y, zIndex, item, overlay, scale, (float) rotation).setParent(this));
    }

    /**
     * takes an item icon off this overlay.
     * <p>
     * The icon itself is left alone, so it can be put back with
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
     * @param i the item to remove
     * @return self for chaining
     * @see IDraw2D#removeItem(Item)
     * @since 1.0.5
     */
    @Override
    public Draw2D removeItem(Item i) {
        synchronized (elements) {
            elements.remove(i);
        }
        return this;
    }

    /**
     * empties this overlay and runs its init function.
     * <p>
     * Everything on the overlay goes first, so an init function that adds elements
     * rebuilds them rather than adding to what was there. It then calls the init
     * function if one is set, and after that initialises every nested overlay.
     * <p>
     * Registering calls this, and the window being resized calls it, which is why an
     * overlay built in an init function comes back after a resize. An init that throws
     * has the failure passed to the fail function if one is set, and is logged if it is
     * not, in either case without taking the rest of the game down.
     * <p>
     * A nested overlay is only initialised if this overlay has an init function of its
     * own, because the call is inside that branch. An overlay with no init function
     * leaves its nested overlays alone, so a panel put together by hand is not emptied
     * out from under itself.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addText("added by hand", 10, 10, 0xFFFFFFFF, true);
     * // empties the overlay, so the hand added text is gone
     * draw.init();
     * Chat.log(`${draw.getTexts().size} texts left after init`);
     * </pre>
     *
     * @since 1.0.5
     */
    public void init() {
        synchronized (elements) {
            elements.clear();
        }
        if (onInit != null) {
            try {
                onInit.accept(this);
                getDraw2Ds().forEach(e -> e.getDraw2D().init());
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
     * everything on this overlay, in the order it is drawn.
     * <p>
     * The elements are sorted by their own z-indexes, lowest first, and elements with
     * the same z-index keep the order they were added in. This is the order the renderer
     * walks, so two elements drawn in the same place are ordered by this rather than by
     * anything else.
     * <p>
     * A new iterator each call over a snapshot of the elements, so taking one and then
     * adding to the overlay does not disturb it. JavaScript cannot walk this directly; the
     * typed getters such as {@link #getTexts()} are the readable route and the z-indexes
     * are on the elements themselves.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addText("behind", 10, 10, 0xFFFF0000, 0, true);
     * draw.addText("in front", 10, 10, 0xFF00FF00, 1, true);
     * draw.register();
     * // the two texts come back in draw order, not the order they were added
     * const texts = draw.getTexts();
     * Chat.log(`first is the one with z-index ${texts.get(0).getZIndex()}`);
     * </pre>
     *
     * @return the elements on this overlay, in draw order
     * @since 1.8.4
     */
    public Iterator<RenderElement> getElementsByZIndex() {
        return elements.stream().sorted(Comparator.comparingInt(RenderElement::getZIndex)).iterator();
    }

    /**
     * init function, called when window is resized or screen/draw2d is registered.
     * clears all previous elements when called.
     * <p>
     * The overlay is emptied before this is called, so the function is what puts
     * anything back on it and is the place to build elements. That is the reason an
     * overlay should be registered rather than filled in by hand: a resize calls this
     * again, and an overlay that was filled in by hand would lose its elements to it.
     * <p>
     * The function is given this overlay, so it adds to it. If it throws, the failure
     * goes to the fail function if one is set and is logged if it is not, and neither
     * takes the game down.
     * <p>
     * An init function is what also makes the nested overlays get initialised; an
     * overlay with none of its own leaves theirs alone.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.setOnInit(JavaWrapper.methodToJava(function (self) {
     *   // self is this overlay, and it has just been emptied
     *   self.addRect(10, 10, 110, 60, 0x80000000);
     *   self.addText("rebuilt on every init", 16, 20, 0xFFFFFFFF, true);
     * }));
     * // registering runs it, so the elements are there before the first frame
     * draw.register();
     * </pre>
     *
     * @param onInit calls your method as a {@link java.util.function.Consumer Consumer}&lt;{@link Draw2D}&gt;
     * @return self for chaining
     * @see IDraw2D#setOnInit(MethodWrapper)
     * @since 1.2.7
     */
    @Override
    public Draw2D setOnInit(@Nullable MethodWrapper<Draw2D, Object, Object, ?> onInit) {
        this.onInit = onInit;
        return this;
    }

    /**
     * what to do when the init function throws.
     * <p>
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
     * @param catchInit calls your method as a {@link java.util.function.Consumer Consumer}&lt;{@link String String}&gt;
     * @return self for chaining
     * @see IDraw2D#setOnFailInit(MethodWrapper)
     * @since 1.2.7
     */
    @Override
    public Draw2D setOnFailInit(@Nullable MethodWrapper<String, Object, Object, ?> catchInit) {
        this.catchInit = catchInit;
        return this;
    }

    /**
     * register so the overlay actually renders
     * <p>
     * This runs the init function first and then adds the overlay to the list the HUD
     * renderer walks, so an overlay whose elements are built in an init function is ready
     * before its first frame. Building the elements is not registering the overlay, so
     * one that was filled in and never registered draws nothing.
     * <p>
     * Registering the same overlay twice does nothing different, since the list it goes
     * into holds each one once. The older {@code Hud.registerDraw2D} does the same thing
     * and is deprecated.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
     * // nothing has been drawn until this
     * draw.register();
     * </pre>
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
     * unregister so the overlay stops rendering
     * <p>
     * The elements are left on the overlay, so the same object can be registered again
     * later and comes back as it was. This only affects this overlay and leaves every
     * other registered one alone; {@code Hud.clearDraw2Ds()} is the one that takes down
     * all of them.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.addText("hello", 10, 10, 0xFFFFFFFF, true);
     * draw.register();
     * draw.unregister();
     * // the text is still on the overlay, just not being drawn
     * Chat.log(`${draw.getTexts().size()} texts, none of them visible`);
     * </pre>
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
     * where this overlay sits against the other registered overlays, lower drawn first.
     * <p>
     * This orders whole overlays rather than the elements within one, so it is what to
     * reach for when two overlays would otherwise fight over the same part of the
     * screen. It is not the z-index of any element, and it does not move this overlay
     * anywhere on screen.
     * <p>
     * A change takes effect on the next frame, since the list is sorted as it is
     * walked. It is the same thing as writing the {@code zIndex} field.
     * example:
     * <pre>
     * const back = Hud.createDraw2D();
     * back.addRect(0, 0, 50, 50, 0x80000000);
     * back.setZIndex(0);
     * back.register();
     * const front = Hud.createDraw2D();
     * front.addText("in front", 10, 10, 0xFFFFFFFF, true);
     * front.setZIndex(1);
     * front.register();
     * </pre>
     *
     * @param zIndex where this overlay sits against the other registered overlays
     * @since 1.8.4
     */
    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }

    /**
     * where this overlay sits against the other registered overlays, lower drawn first.
     * <p>
     * This is not the z-index of any element on it; those are set per element and are on
     * the elements themselves. It is also not a screen position: nothing moves, it only
     * changes which of two overlapping overlays is drawn over which.
     * <p>
     * The default is {@code 0}, and two overlays at the same value are ordered among
     * themselves by which was registered first.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * Chat.log(`overlay z-index: ${draw.getZIndex()}`);
     * </pre>
     *
     * @return where this overlay sits against the other registered overlays
     * @since 1.8.4
     */
    public int getZIndex() {
        return zIndex;
    }

}
