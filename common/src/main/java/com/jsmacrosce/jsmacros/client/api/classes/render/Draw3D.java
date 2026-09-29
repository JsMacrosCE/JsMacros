package com.jsmacrosce.jsmacros.client.api.classes.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.api.math.Pos2D;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.api.classes.render.components3d.*;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.api.library.impl.FHud;
import com.jsmacrosce.jsmacros.client.util.CameraCompat;
import com.jsmacrosce.jsmacros.core.classes.Registrable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * a list of things to draw in the world, and the thing that draws them.
 * <p>
 * This is the 3D counterpart of a 2D overlay. Where a 2D overlay holds text, rectangles
 * and images in screen space, this holds cubes, lines and flat panels in the world, in
 * block coordinates in the dimension the player is in. The overlay is drawn relative to
 * the camera rather than absolutely, so everything in it moves with the player and
 * never has to be updated for that.
 * <p>
 * Nothing renders until {@link #register()} is called, and building the elements is
 * not registering it, so a script that fills one in and returns draws nothing. The
 * older {@code Hud.registerDraw3D} does the same thing and is deprecated. Taking it
 * off again is {@link #unregister()}, or {@link #clear()} to empty this one.
 * <p>
 * The contents are held in a list, not a set, so adding the same element twice gives
 * two copies. What the order is does not matter when adding, because before each frame
 * the list is sorted with the element's own comparison, which groups different kinds
 * of element together and orders each group by position. That sort happens in place, so
 * what {@link #getBoxes()} and its siblings return is in draw order rather than the
 * order things were added.
 * <p>
 * What the helpers leave out is worth knowing. A cube made with {@code addBox} and no
 * {@code cull} argument is drawn over the world rather than being hidden by terrain,
 * the same for {@code addLine} and the trace lines, and {@link #addPoint(Pos3D, double,
 * int)} makes a filled cube rather than an outline. Passing the argument, or setting
 * the field on the element afterwards, is how a depth tested one is made.
 * <p>
 * A flat 2D panel living in the world is a {@code Surface}, and the getters and
 * builders here are named after it. {@link #getDraw2Ds()} is the one name that does
 * not say what it returns: it gives the surfaces, not 2D overlays.
 * example:
 * <pre>
 * // a world overlay with a box and a line in it, registered so it shows
 * const draw = Hud.createDraw3D();
 * draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFF0000, 0x88000000, true);
 * draw.addLine(0, 64, 0, 8, 72, 0, 0xFF00FF00);
 * draw.register();
 *
 * // what went in, in the order it will be drawn
 * Chat.log(`${draw.getBoxes().size()} boxes, ${draw.getLines().size()} lines`);
 *
 * // and taken off again, which empties this overlay only
 * draw.unregister();
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.0.6
 */
@DocletCategory("Rendering/Graphics")
@SuppressWarnings("unused")
public class Draw3D implements Registrable<Draw3D> {
    /**
     * everything this overlay holds, in whatever order the last frame's sort left it.
     * <p>
     * The list is sorted in place before each frame, so the order here is the draw
     * order rather than the order things were added in. A script adding to it goes
     * through the helpers, and reading it through the typed getters.
     *
     * @since 1.0.6
     */
    private final List<RenderElement3D<?>> elements = new ArrayList<>();

    /**
     * every cube in this overlay, in the order they are drawn.
     * <p>
     * A new list each call, filtered by type, so changing it does not change what this
     * overlay holds. The order is the draw order rather than the order they were added
     * in, since the contents are sorted before each frame.
     * <p>
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFF0000, 0x00000000, false);
     * draw.register();
     * // the one box, in draw order
     * Chat.log(`${draw.getBoxes().size()} boxes`);
     * </pre>
     *
     * @return the cubes in this overlay
     * @since 1.0.6
     */
    public List<Box> getBoxes() {
        List<Box> list = new ArrayList<>();
        synchronized (elements) {
            for (RenderElement3D<?> element : elements) {
                if (element instanceof Box) {
                    list.add((Box) element);
                }
            }
        }
        return list;
    }

    /**
     * every straight 3D line in this overlay, in the order they are drawn.
     * <p>
     * A new list each call, filtered by type, so changing it does not change what this
     * overlay holds. This is the fixed lines only: a line that starts at the camera is
     * a {@code TraceLine} and is not in here, even though it is a kind of line.
     * <p>
     * The order is the draw order rather than the order they were added in, since the
     * contents are sorted before each frame.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.addLine(0, 64, 0, 8, 72, 0, 0xFF00FF00);
     * draw.addTraceLine(0, 64, 0, 0xFFFF0000);
     * draw.register();
     * // one fixed line; the trace line is counted separately
     * Chat.log(`${draw.getLines().size()} lines, ${draw.getTraceLines().size()} trace lines`);
     * </pre>
     *
     * @return the straight lines in this overlay
     * @since 1.0.6
     */
    public List<Line3D> getLines() {
        List<Line3D> list = new ArrayList<>();
        synchronized (elements) {
            for (RenderElement3D<?> element : elements) {
                if (element instanceof Line3D) {
                    list.add((Line3D) element);
                }
            }
        }
        return list;
    }

    /**
     * every camera trace line in this overlay, in the order they are drawn.
     * <p>
     * A new list each call, filtered by type, so changing it does not change what this
     * overlay holds. A trace line is the one that starts in front of the camera and
     * runs to a point, and it is a different kind of thing from the fixed lines in
     * {@link #getLines()}, so the two never overlap.
     * <p>
     * The order is the draw order rather than the order they were added in, since the
     * contents are sorted before each frame.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.addTraceLine(0, 64, 0, 0xFFFF0000);
     * draw.register();
     * Chat.log(`${draw.getTraceLines().size()} trace lines`);
     * </pre>
     *
     * @return the trace lines in this overlay
     * @since 1.9.0
     */
    public List<TraceLine> getTraceLines() {
        List<TraceLine> list = new ArrayList<>();
        synchronized (elements) {
            for (RenderElement3D<?> element : elements) {
                if (element instanceof TraceLine) {
                    list.add((TraceLine) element);
                }
            }
        }
        return list;
    }

    /**
     * every trace line that follows an entity, in the order they are drawn.
     * <p>
     * A new list each call, filtered by type, so changing it does not change what this
     * overlay holds. These are the ones whose target is an entity rather than a fixed
     * point, and they are counted separately from the plain trace lines even though
     * both are lines from the camera.
     * <p>
     * A line whose entity has gone is not in here from the frame after it notices, and
     * is not in the overlay at all after that.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   draw.addEntityTraceLine(player, 0xFFFF0000);
     * }
     * draw.register();
     * Chat.log(`${draw.getEntityTraceLines().size()} entity trace lines`);
     * </pre>
     *
     * @return the entity trace lines in this overlay
     * @since 1.9.0
     */
    public List<EntityTraceLine> getEntityTraceLines() {
        List<EntityTraceLine> list = new ArrayList<>();
        synchronized (elements) {
            for (RenderElement3D<?> element : elements) {
                if (element instanceof EntityTraceLine) {
                    list.add((EntityTraceLine) element);
                }
            }
        }
        return list;
    }

    /**
     * every flat panel in this overlay, in the order they are drawn.
     * <p>
     * A new list each call, filtered by type, so changing it does not change what this
     * overlay holds. The name is the leftover from when these were 2D overlays; what it
     * returns is surfaces, the flat 2D panels that live in the world rather than on the
     * screen.
     * <p>
     * The order is the draw order rather than the order they were added in. The sort
     * that produces it is a raw comparison on position rather than a distance, so two
     * panels that overlap still order against each other.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const surface = draw.surfaceBuilder().pos(0, 64, 0).size(1, 1).buildAndAdd();
     * surface.addText("hello", 10, 10, 0xFFFFFFFF, true);
     * draw.register();
     * Chat.log(`${draw.getDraw2Ds().size()} surfaces`);
     * </pre>
     *
     * @return the surfaces in this overlay
     * @since 1.6.5
     */
    public List<Surface> getDraw2Ds() {
        List<Surface> list = new ArrayList<>();
        synchronized (elements) {
            for (RenderElement3D<?> element : elements) {
                if (element instanceof Surface) {
                    list.add((Surface) element);
                }
            }
        }
        return list;
    }

    /**
     * takes every element out of this overlay.
     * <p>
     * This empties the list rather than unregistering the overlay, so the overlay is
     * still rendering and can be filled in again and will draw straight away. It also
     * leaves any element a script is holding a reference to alone; that reference just
     * points at something no longer being drawn.
     * <p>
     * It empties this overlay and no others, so it does not disturb what other scripts
     * have registered. {@code Hud.clearDraw3Ds()} is the one that empties all of them.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFF0000, 0x00000000, false);
     * draw.register();
     * // gone from the overlay, but the overlay is still registered
     * draw.clear();
     * Chat.log(`${draw.getBoxes().size()} boxes left`);
     * </pre>
     *
     * @since 1.8.4
     */
    public void clear() {
        synchronized (elements) {
            elements.clear();
        }
    }

    /**
     * puts an existing element back into this overlay.
     * <p>
     * For an element that was taken off with {@link #removeBox(Box)},
     * {@link #removeLine(Line3D)}, {@link #removeTraceLine(TraceLine)} or
     * {@link #removeDraw2D(Surface)}, and for anything built with {@code build()}
     * rather than {@code buildAndAdd()}. Nothing is checked, so the same element can be
     * put back more than once and would then be drawn more than once.
     * <p>
     * There is no check against adding a surface that already contains this overlay,
     * unlike the 2D overlay's equivalent.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const box = draw.boxBuilder().forBlock(0, 64, 0).color(0xFFFF0000).build();
     * draw.register();
     * // built but not added, so nothing is drawn yet
     * draw.reAddElement(box);
     * Chat.log(`${draw.getBoxes().size()} boxes`);
     * </pre>
     *
     * @param element the element to put back
     * @since 1.8.4
     */
    public void reAddElement(RenderElement3D<?> element) {
        synchronized (elements) {
            elements.add(element);
        }
    }

    /**
     * puts an existing cube into this overlay.
     * <p>
     * For a box built with {@code build()} rather than {@code buildAndAdd()}, or one
     * taken off again with {@link #removeBox(Box)}. Nothing is checked, so adding the
     * same box twice puts two copies in and it is drawn twice.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const box = draw.boxBuilder().forBlock(0, 64, 0).color(0xFFFF0000).build();
     * draw.addBox(box);
     * draw.register();
     * </pre>
     *
     * @param box the cube to add
     * @since 1.8.4
     */
    public void addBox(Box box) {
        synchronized (elements) {
            elements.add(box);
        }
    }

    /**
     * puts an existing straight 3D line into this overlay.
     * <p>
     * For a line built with {@code build()} rather than {@code buildAndAdd()}, or one
     * taken off again with {@link #removeLine(Line3D)}. Nothing is checked, so adding
     * the same line twice draws it twice.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.lineBuilder().pos(0, 64, 0, 8, 72, 0).color(0xFF00FF00).build();
     * draw.addLine(line);
     * draw.register();
     * </pre>
     *
     * @param line the line to add
     * @since 1.8.4
     */
    public void addLine(Line3D line) {
        synchronized (elements) {
            elements.add(line);
        }
    }

    /**
     * puts an existing trace line into this overlay.
     * <p>
     * For a line built with {@code build()} rather than {@code buildAndAdd()}, or one
     * taken off again with {@link #removeTraceLine(TraceLine)}. Nothing is checked, so
     * adding the same line twice draws it twice.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.traceLineBuilder().pos(0, 64, 0).color(0xFFFF0000).build();
     * draw.addTraceLine(line);
     * draw.register();
     * </pre>
     *
     * @param line the trace line to add
     * @since 1.9.0
     */
    public void addTraceLine(TraceLine line) {
        synchronized (elements) {
            elements.add(line);
        }
    }

    /**
     * puts an existing flat panel into this overlay.
     * <p>
     * For a surface built with {@code build()} rather than {@code buildAndAdd()}, or
     * one taken off again with {@link #removeDraw2D(Surface)}. Nothing is checked, so
     * adding the same surface twice puts two copies in and it is drawn twice.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const surface = draw.surfaceBuilder().pos(0, 64, 0).size(1, 1).build();
     * draw.addSurface(surface);
     * surface.addText("hello", 10, 10, 0xFFFFFFFF, true);
     * draw.register();
     * </pre>
     *
     * @param surface the surface to add
     * @since 1.8.4
     */
    public void addSurface(Surface surface) {
        synchronized (elements) {
            elements.add(surface);
        }
    }

    /**
     * @param x1 the x coordinate of the first corner
     * @param y1 the y coordinate of the first corner
     * @param z1 the z coordinate of the first corner
     * @param x2 the x coordinate of the second corner
     * @param y2 the y coordinate of the second corner
     * @param z2 the z coordinate of the second corner
     * @param color the colour of the outline, with or without an alpha of its own
     * @param fillColor the colour of the inside
     * @param fill whether to draw the inside as well as the outline
     * @return The {@link Box} you added.
     * @since 1.0.6
     */
    public Box addBox(double x1, double y1, double z1, double x2, double y2, double z2, int color, int fillColor, boolean fill) {
        return addBox(x1, y1, z1, x2, y2, z2, color, fillColor, fill, false);
    }

    /**
     *             is depth testing: {@code true} is the ordinary
     *             box terrain hides and {@code false} is the one drawn over everything,
     *             which is what the shorter form leaves
     * @param x1 the x coordinate of the first corner
     * @param y1 the y coordinate of the first corner
     * @param z1 the z coordinate of the first corner
     * @param x2 the x coordinate of the second corner
     * @param y2 the y coordinate of the second corner
     * @param z2 the z coordinate of the second corner
     * @param color the colour of the outline, with or without an alpha of its own
     * @param fillColor the colour of the inside
     * @param fill whether to draw the inside as well as the outline
     * @param cull whether the box is depth tested, so terrain draws over it. The name
     * @return The {@link Box} you added.
     * @since 1.3.1
     */
    public Box addBox(double x1, double y1, double z1, double x2, double y2, double z2, int color, int fillColor, boolean fill, boolean cull) {
        Box b = new Box(x1, y1, z1, x2, y2, z2, color, fillColor, fill, cull);
        synchronized (elements) {
            elements.add(b);
        }
        return b;
    }

    /**
     * @param x1 the x coordinate of the first corner
     * @param y1 the y coordinate of the first corner
     * @param z1 the z coordinate of the first corner
     * @param x2 the x coordinate of the second corner
     * @param y2 the y coordinate of the second corner
     * @param z2 the z coordinate of the second corner
     * @param color the colour of the outline
     * @param alpha the alpha of the outline, from 0 to 255
     * @param fillColor the colour of the inside
     * @param fillAlpha the alpha of the inside, from 0 to 255
     * @param fill whether to draw the inside as well as the outline
     * @return the {@link Box} you added.
     * @since 1.1.8
     */
    public Box addBox(double x1, double y1, double z1, double x2, double y2, double z2, int color, int alpha, int fillColor, int fillAlpha, boolean fill) {
        return addBox(x1, y1, z1, x2, y2, z2, color, alpha, fillColor, fillAlpha, fill, false);
    }

    /**
     * adds a cube with separate alphas for the outline and the inside, and a choice of
     * whether terrain hides it.
     * <p>
     * This is the full form. The two corners are opposite rather than a minimum and a
     * maximum, so they can be given in any order and the cube is the same either way.
     * A colour given without an alpha of its own here is made opaque rather than
     * transparent, since the alpha argument is what decides that.
     * <p>
     * The {@code cull} argument is depth testing: {@code true} is the ordinary cube that
     * terrain draws over and {@code false} is the one drawn over the world.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * // a half transparent cube that terrain can hide
     * draw.addBox(0, 64, 0, 1, 65, 1, 0xFF0000, 128, 0x0000FF, 64, true, true);
     * draw.register();
     * </pre>
     *
     * @param x1 the x coordinate of the first corner
     * @param y1 the y coordinate of the first corner
     * @param z1 the z coordinate of the first corner
     * @param x2 the x coordinate of the second corner
     * @param y2 the y coordinate of the second corner
     * @param z2 the z coordinate of the second corner
     * @param color the colour of the outline
     * @param alpha the alpha of the outline, from 0 to 255
     * @param fillColor the colour of the inside
     * @param fillAlpha the alpha of the inside, from 0 to 255
     * @param fill whether to draw the inside as well as the outline
     * @param cull whether the box is depth tested, so terrain draws over it
     * @return the {@link Box} you added.
     * @since 1.3.1
     */
    public Box addBox(double x1, double y1, double z1, double x2, double y2, double z2, int color, int alpha, int fillColor, int fillAlpha, boolean fill, boolean cull) {
        Box b = new Box(x1, y1, z1, x2, y2, z2, color, alpha, fillColor, fillAlpha, fill, cull);
        synchronized (elements) {
            elements.add(b);
        }
        return b;
    }

    /**
     * @param b the cube to take off this overlay
     * @return self for chaining
     * @since 1.0.6
     */
    public Draw3D removeBox(Box b) {
        synchronized (elements) {
            elements.remove(b);
        }
        return this;
    }

    /**
     *              colour with a zero alpha byte comes out fully opaque
     * @param x1 the x coordinate of the start
     * @param y1 the y coordinate of the start
     * @param z1 the z coordinate of the start
     * @param x2 the x coordinate of the end
     * @param y2 the y coordinate of the end
     * @param z2 the z coordinate of the end
     * @param color the colour of the line, with or without an alpha of its own. A
     * @return the {@link Line3D} you added.
     * @since 1.0.6
     */
    public Line3D addLine(double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        return addLine(x1, y1, z1, x2, y2, z2, color, false);
    }

    /**
     *             is depth testing: {@code true} is the ordinary
     *             line terrain hides and {@code false} is the one drawn over everything,
     *             which is what the shorter form leaves
     * @param x1 the x coordinate of the start
     * @param y1 the y coordinate of the start
     * @param z1 the z coordinate of the start
     * @param x2 the x coordinate of the end
     * @param y2 the y coordinate of the end
     * @param z2 the z coordinate of the end
     * @param color the colour of the line, with or without an alpha of its own
     * @param cull whether the line is depth tested, so terrain draws over it. The name
     * @return the {@link Line3D} you added.
     * @since 1.3.1
     */
    public Line3D addLine(double x1, double y1, double z1, double x2, double y2, double z2, int color, boolean cull) {
        Line3D l = new Line3D(x1, y1, z1, x2, y2, z2, color, cull);
        synchronized (elements) {
            elements.add(l);
        }
        return l;
    }

    /**
     * @param x1 the x coordinate of the start
     * @param y1 the y coordinate of the start
     * @param z1 the z coordinate of the start
     * @param x2 the x coordinate of the end
     * @param y2 the y coordinate of the end
     * @param z2 the z coordinate of the end
     * @param color the colour of the line
     * @param alpha the alpha of the line, from 0 to 255
     * @return the {@link Line3D} you added.
     * @since 1.1.8
     */

    public Line3D addLine(double x1, double y1, double z1, double x2, double y2, double z2, int color, int alpha) {
        return addLine(x1, y1, z1, x2, y2, z2, color, alpha, false);
    }

    /**
     * @param x1 the x coordinate of the start
     * @param y1 the y coordinate of the start
     * @param z1 the z coordinate of the start
     * @param x2 the x coordinate of the end
     * @param y2 the y coordinate of the end
     * @param z2 the z coordinate of the end
     * @param color the colour of the line
     * @param alpha the alpha of the line, from 0 to 255
     * @param cull whether the line is depth tested, so terrain draws over it
     * @return the {@link Line3D} you added.
     * @since 1.3.1
     */
    public Line3D addLine(double x1, double y1, double z1, double x2, double y2, double z2, int color, int alpha, boolean cull) {
        Line3D l = new Line3D(x1, y1, z1, x2, y2, z2, color, alpha, cull);
        synchronized (elements) {
            elements.add(l);
        }
        return l;
    }

    /**
     * @param l the straight line to take off this overlay
     * @return self for chaining
     * @since 1.0.6
     */
    public Draw3D removeLine(Line3D l) {
        synchronized (elements) {
            elements.remove(l);
        }
        return this;
    }

    /**
     * adds a line running from the crosshair to a point in the world.
     * <p>
     * The start is worked out from where the player is looking every frame, so only
     * the target is given. A colour with no alpha of its own comes out fully opaque,
     * and the line is drawn over the world rather than being hidden by terrain, which
     * is what the shorter form leaves. Use
     * {@link #traceLineBuilder()} to change either.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addTraceLine(0, 64, 0, 0xFFFF0000);
     * // terrain can hide it
     * line.setAlwaysOnTop(false);
     * draw.register();
     * </pre>
     *
     * @param x the x coordinate of the target
     * @param y the y coordinate of the target
     * @param z the z coordinate of the target
     * @param color the colour of the line
     * @return the {@link TraceLine} you added.
     * @since 1.9.0
     */
    public TraceLine addTraceLine(double x, double y, double z, int color) {
        TraceLine l = new TraceLine(x, y, z, color);
        synchronized (elements) {
            elements.add(l);
        }
        return l;
    }

    /**
     * adds a line running from the crosshair to a point, with the alpha given on its
     * own.
     * <p>
     * Nothing is filled in here, so an alpha of {@code 0} really does make the line
     * invisible. The line is drawn over the world rather than being hidden by terrain.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.addTraceLine(0, 64, 0, 0xFFFF0000, 128);
     * draw.register();
     * </pre>
     *
     * @param x the x coordinate of the target
     * @param y the y coordinate of the target
     * @param z the z coordinate of the target
     * @param color the colour of the line
     * @param alpha the alpha of the line, from 0 to 255
     * @return the {@link TraceLine} you added.
     * @since 1.9.0
     */
    public TraceLine addTraceLine(double x, double y, double z, int color, int alpha) {
        TraceLine l = new TraceLine(x, y, z, color, alpha);
        synchronized (elements) {
            elements.add(l);
        }
        return l;
    }

    /**
     * adds a line running from the crosshair to a position in the world.
     * <p>
     * The same as the three number form. The position is read straight off, so moving
     * it afterwards does not move the line with it. The line is drawn over the world
     * rather than being hidden by terrain.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.addTraceLine(PositionCommon.createPos(0, 64, 0), 0xFFFF0000);
     * draw.register();
     * </pre>
     *
     * @param pos the position of the target
     * @param color the colour of the line
     * @return the {@link TraceLine} you added.
     * @since 1.9.0
     */
    public TraceLine addTraceLine(Pos3D pos, int color) {
        TraceLine l = new TraceLine(pos, color);
        synchronized (elements) {
            elements.add(l);
        }
        return l;
    }

    /**
     * adds a line running from the crosshair to a position, with the alpha given on its
     * own.
     * <p>
     * Nothing is filled in here, so an alpha of {@code 0} really does make the line
     * invisible. The line is drawn over the world rather than being hidden by terrain.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.addTraceLine(PositionCommon.createPos(0, 64, 0), 0xFFFF0000, 128);
     * draw.register();
     * </pre>
     *
     * @param pos the position of the target
     * @param color the colour of the line
     * @param alpha the alpha of the line, from 0 to 255
     * @return the {@link TraceLine} you added.
     * @since 1.9.0
     */
    public TraceLine addTraceLine(Pos3D pos, int color, int alpha) {
        TraceLine l = new TraceLine(pos, color, alpha);
        synchronized (elements) {
            elements.add(l);
        }
        return l;
    }

    /**
     * adds a line running from the crosshair to an entity.
     * <p>
     * The target follows the entity: its interpolated position plus the offset is
     * worked out every frame, so there is nothing to update as the entity moves. The
     * offset here is half a block, which on a player-sized entity is a little under a
     * third of the way up.
     * <p>
     * A colour with no alpha of its own comes out fully opaque, and the line is drawn
     * over the world rather than being hidden by terrain. Both can be changed on the
     * returned line.
     * <p>
     * The line is dropped from the overlay by itself once the entity is gone, so there
     * is no need to take it off.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const draw = Hud.createDraw3D();
     *   draw.addEntityTraceLine(player, 0xFFFF0000);
     *   draw.register();
     * }
     * </pre>
     *
     * @param entity the entity to point at
     * @param color the colour of the line
     * @return the {@link EntityTraceLine} you added.
     * @since 1.9.0
     */
    public EntityTraceLine addEntityTraceLine(EntityHelper<?> entity, int color) {
        EntityTraceLine l = new EntityTraceLine(entity, color, 0.5);
        synchronized (elements) {
            elements.add(l);
        }
        return l;
    }

    /**
     * adds a line running from the crosshair to an entity, with the alpha given on its
     * own.
     * <p>
     * The offset is half a block, as in the shorter form, and the line is drawn over
     * the world rather than being hidden by terrain.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const draw = Hud.createDraw3D();
     *   draw.addEntityTraceLine(player, 0xFFFF0000, 128);
     *   draw.register();
     * }
     * </pre>
     *
     * @param entity the entity to point at
     * @param color the colour of the line
     * @param alpha the alpha of the line, from 0 to 255
     * @return the {@link EntityTraceLine} you added.
     * @since 1.9.0
     */
    public EntityTraceLine addEntityTraceLine(EntityHelper<?> entity, int color, int alpha) {
        return addEntityTraceLine(entity, color, alpha, 0.5);
    }

    /**
     * adds a line running from the crosshair to an entity, at a chosen height above
     * it.
     * <p>
     * The offset is in blocks from the entity's interpolated position and is worked
     * out every frame, so it follows the entity rather than being applied once. Zero
     * aims at the entity's own position, which for most entities is inside the ground.
     * <p>
     * The line is drawn over the world rather than being hidden by terrain, as in the
     * shorter forms.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const draw = Hud.createDraw3D();
     *   // aim at the player's eyes rather than their feet
     *   draw.addEntityTraceLine(player, 0xFFFF0000, 255, player.getEyeHeight());
     *   draw.register();
     * }
     * </pre>
     *
     * @param entity the entity to point at
     * @param color the colour of the line
     * @param alpha the alpha of the line, from 0 to 255
     * @param yOffset how far above the entity's feet to aim, in blocks
     * @return the {@link EntityTraceLine} you added.
     * @since 1.9.0
     */
    public EntityTraceLine addEntityTraceLine(EntityHelper<?> entity, int color, int alpha, double yOffset) {
        EntityTraceLine l = new EntityTraceLine(entity, color, alpha, yOffset);
        synchronized (elements) {
            elements.add(l);
        }
        return l;
    }

    /**
     * adds a line running from the crosshair to an entity, at a chosen height, with a
     * choice of whether terrain hides it.
     * <p>
     * This is the full form, and the only one of these that takes {@code alwaysOnTop}:
     * the shorter forms all leave the line drawn over the world. {@code false} is the
     * ordinary depth tested line that terrain draws over.
     * <p>
     * The line is dropped from the overlay by itself once the entity is gone, so there
     * is no need to take it off.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const draw = Hud.createDraw3D();
     *   // a line something in front of it can hide
     *   draw.addEntityTraceLine(player, 0xFFFF0000, 255, 0.5, false);
     *   draw.register();
     * }
     * </pre>
     *
     * @param entity the entity to point at
     * @param color the colour of the line
     * @param alpha the alpha of the line, from 0 to 255
     * @param yOffset how far above the entity's feet to aim, in blocks
     * @param alwaysOnTop whether the line is drawn over the world rather than being
     * @return the {@link EntityTraceLine} you added.
     *                    hidden by terrain
     * @since 1.9.0
     */
    public EntityTraceLine addEntityTraceLine(EntityHelper<?> entity, int color, int alpha, double yOffset, boolean alwaysOnTop) {
        EntityTraceLine l = new EntityTraceLine(entity, color, alpha, yOffset, alwaysOnTop);
        synchronized (elements) {
            elements.add(l);
        }
        return l;
    }

    /**
     * @param traceLine the trace line to take off this overlay
     * @return self for chaining
     * @since 1.9.0
     */
    public Draw3D removeTraceLine(TraceLine traceLine) {
        synchronized (elements) {
            elements.remove(traceLine);
        }
        return this;
    }

    /**
     * Draws a cube({@link Box}) with a specific radius({@code side length = 2*radius})
     *
     * <p>
     * The cube is filled, not just outlined, and its inside is the same colour as its
     * edges. It is drawn over the world rather than being hidden by terrain, since the
     * shorter forms leave the cull flag off. A colour with no alpha of its own comes
     * out fully opaque.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * // a solid dot half a block across
     * draw.addPoint(0.5, 64.5, 0.5, 0.5, 0xFFFF0000);
     * draw.register();
     * </pre>
     *
     * @param point  the center point
     * @param radius 1/2 of the side length of the cube
     * @param color  point color
     * @return the {@link Box} generated, and visualized
     * @see Box
     * @since 1.4.0
     */
    public Box addPoint(Pos3D point, double radius, int color) {
        return addPoint(point.getX(), point.getY(), point.getZ(), radius, color);
    }

    /**
     * Draws a cube({@link Box}) with a specific radius({@code side length = 2*radius})
     *
     * <p>
     * The cube is filled, not just outlined, and its inside is the same colour as its
     * edges. It is drawn over the world rather than being hidden by terrain, since the
     * cull flag is left off here. A colour with no alpha of its own comes out fully
     * opaque.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * // a solid dot half a block across
     * draw.addPoint(0.5, 64.5, 0.5, 0.5, 0xFFFF0000);
     * draw.register();
     * </pre>
     *
     * @param x      x value of the center point
     * @param y      y value of the center point
     * @param z      z value of the center point
     * @param radius 1/2 of the side length of the cube
     * @param color  point color
     * @return the {@link Box} generated, and visualized
     * @see Box
     * @since 1.4.0
     */
    public Box addPoint(double x, double y, double z, double radius, int color) {
        return addBox(
                x - radius,
                y - radius,
                z - radius,
                x + radius,
                y + radius,
                z + radius,
                color,
                color,
                true,
                false
        );
    }

    /**
     * Draws a cube({@link Box}) with a specific radius({@code side length = 2*radius})
     * <p>
     * This is the full form, and the only one of the three that takes a cull argument
     * and an alpha. The alpha is used for both the outline and the inside, and nothing
     * is filled in, so an alpha of {@code 0} really does make the dot invisible.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * // a half transparent dot that terrain can hide
     * draw.addPoint(0.5, 64.5, 0.5, 0.5, 0xFF0000, 128, true);
     * draw.register();
     * </pre>
     *
     * @param x      x value of the center point
     * @param y      y value of the center point
     * @param z      z value of the center point
     * @param radius 1/2 of the side length of the cube
     * @param color  point color
     * @param alpha  alpha of the point, used for both the outline and the inside
     * @param cull   whether to cull the point or not
     * @return the {@link Box} generated, and visualized
     * @see Box
     * @since 1.4.0
     */
    public Box addPoint(double x, double y, double z, double radius, int color, int alpha, boolean cull) {
        return addBox(
                x - radius,
                y - radius,
                z - radius,
                x + radius,
                y + radius,
                z + radius,
                color,
                color,
                alpha,
                alpha,
                true,
                cull
        );
    }

    /**
     * @param x top left
     * @param y
     * @param z
     * @return the {@link Surface} you added.
     * @since 1.6.5
     */
    public Surface addDraw2D(double x, double y, double z) {
        return addDraw2D(x, y, z, 0, 0, 0, 1, 1, 200, false, false);
    }

    /**
     * @param x the x coordinate of the panel's corner
     * @param y the y coordinate of the panel's corner
     * @param z the z coordinate of the panel's corner
     * @param width the width of the panel, in blocks
     * @param height the height of the panel, in blocks
     * @return the {@link Surface} you added.
     * @since 1.6.5
     */
    public Surface addDraw2D(double x, double y, double z, double width, double height) {
        return addDraw2D(x, y, z, 0, 0, 0, width, height, 200, false, false);
    }

    /**
     * @param x the x coordinate of the panel's corner
     * @param y the y coordinate of the panel's corner
     * @param z the z coordinate of the panel's corner
     * @param xRot the rotation about the x axis, in degrees
     * @param yRot the rotation about the y axis, in degrees
     * @param zRot the rotation about the z axis, in degrees
     * @return the {@link Surface} you added.
     * @since 1.6.5
     */
    public Surface addDraw2D(double x, double y, double z, double xRot, double yRot, double zRot) {
        return addDraw2D(x, y, z, xRot, yRot, zRot, 1, 1, 200, false, false);
    }

    /**
     * @param x the x coordinate of the panel's corner
     * @param y the y coordinate of the panel's corner
     * @param z the z coordinate of the panel's corner
     * @param xRot the rotation about the x axis, in degrees
     * @param yRot the rotation about the y axis, in degrees
     * @param zRot the rotation about the z axis, in degrees
     * @param width the width of the panel, in blocks
     * @param height the height of the panel, in blocks
     * @return the {@link Surface} you added.
     * @since 1.6.5
     */
    public Surface addDraw2D(double x, double y, double z, double xRot, double yRot, double zRot, double width, double height) {
        return addDraw2D(x, y, z, xRot, yRot, zRot, width, height, 200, false, false);
    }

    /**
     *                        Anything below 1 is raised to 1
     * @param x the x coordinate of the panel's corner
     * @param y the y coordinate of the panel's corner
     * @param z the z coordinate of the panel's corner
     * @param xRot the rotation about the x axis, in degrees
     * @param yRot the rotation about the y axis, in degrees
     * @param zRot the rotation about the z axis, in degrees
     * @param width the width of the panel, in blocks
     * @param height the height of the panel, in blocks
     * @param minSubdivisions how many surface pixels the smaller side is divided into.
     * @return the {@link Surface} you added.
     * @since 1.6.5
     */
    public Surface addDraw2D(double x, double y, double z, double xRot, double yRot, double zRot, double width, double height, int minSubdivisions) {
        return addDraw2D(x, y, z, xRot, yRot, zRot, width, height, minSubdivisions, false, false);
    }

    /**
     *                        Anything below 1 is raised to 1
     * @param x the x coordinate of the panel's corner
     * @param y the y coordinate of the panel's corner
     * @param z the z coordinate of the panel's corner
     * @param xRot the rotation about the x axis, in degrees
     * @param yRot the rotation about the y axis, in degrees
     * @param zRot the rotation about the z axis, in degrees
     * @param width the width of the panel, in blocks
     * @param height the height of the panel, in blocks
     * @param minSubdivisions how many surface pixels the smaller side is divided into.
     * @param renderBack whether the back of the panel is drawn as well as its front
     * @return the {@link Surface} you added.
     * @since 1.6.5
     */
    public Surface addDraw2D(double x, double y, double z, double xRot, double yRot, double zRot, double width, double height, int minSubdivisions, boolean renderBack) {
        return addDraw2D(x, y, z, xRot, yRot, zRot, width, height, minSubdivisions, renderBack, false);
    }

    /**
     *                        Anything below 1 is raised to 1
     *            is depth testing: {@code true} is the panel
     *            terrain hides and {@code false} is the one drawn over the world, which
     *            is what the shorter forms leave
     * @param x               top left, the x coordinate of the panel's corner
     * @param y
     * @param z
     * @param xRot the rotation about the x axis, in degrees
     * @param yRot the rotation about the y axis, in degrees
     * @param zRot the rotation about the z axis, in degrees
     * @param width the width of the panel, in blocks
     * @param height the height of the panel, in blocks
     * @param minSubdivisions how many surface pixels the smaller side is divided into.
     * @param renderBack whether the back of the panel is drawn as well as its front
     * @param cull whether the panel is depth tested, so terrain draws over it. The name
     * @return the {@link Surface} you added.
     * @since 1.6.5
     */
    public Surface addDraw2D(double x, double y, double z, double xRot, double yRot, double zRot, double width, double height, int minSubdivisions, boolean renderBack, boolean cull) {
        Surface surface = new Surface(
                new Pos3D(x, y, z),
                new Pos3D(xRot, yRot, zRot),
                new Pos2D(width, height),
                minSubdivisions,
                renderBack,
                cull
        );
        synchronized (elements) {
            this.elements.add(surface);
        }
        return surface;
    }

    /**
     * takes a flat panel off this overlay.
     * <p>
     * The panel itself is left alone, so it can be put back with
     * {@link #addSurface(Surface)} and comes back as it was, and its elements are still
     * on it. Only this overlay is touched, so it does not disturb other scripts.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const surface = draw.addDraw2D(0, 64, 0);
     * draw.register();
     * // taken off, but still holding its elements
     * draw.removeDraw2D(surface);
     * Chat.log(`${draw.getDraw2Ds().size()} surfaces left`);
     * </pre>
     *
     * @param surface the surface to take off this overlay
     * @since 1.6.5
     */
    public void removeDraw2D(Surface surface) {
        synchronized (elements) {
            this.elements.remove(surface);
        }
    }

    /**
     * The builder starts with a box of no size at the origin, white, not filled and
     * drawn over the world, so the size and the colour both have to be set. For a box
     * covering exactly one block, {@link #boxBuilder(int, int, int)} already has the
     * size and saves that step.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.boxBuilder().pos(0, 64, 0, 3, 67, 2).color(0xFFFF0000).buildAndAdd();
     * draw.register();
     * </pre>
     *
     * @return a new {@link Box.Builder} instance.
     * @since 1.8.4
     */
    public Box.Builder boxBuilder() {
        return new Box.Builder(this);
    }

    /**
     * The same as {@link #boxBuilder()} with the size already set to cover exactly
     * that one block. The block's own position is used, without its offset, so this is
     * a block corner rather than the middle of the block.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const draw = Hud.createDraw3D();
     *   draw.boxBuilder(player.getBlockPos()).color(0xFFFF0000).buildAndAdd();
     *   draw.register();
     * }
     * </pre>
     *
     * @param pos the block position of the box
     * @return a new {@link Box.Builder} instance.
     * @since 1.8.4
     */
    public Box.Builder boxBuilder(BlockPosHelper pos) {
        return new Box.Builder(this).forBlock(pos);
    }

    /**
     * The same as {@link #boxBuilder()} with the size already set to cover exactly the
     * one block named by those coordinates. This is the shortest way to say "highlight
     * this block".
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * // a wireframe around a single block
     * draw.boxBuilder(0, 64, 0).color(0xFFFF0000).buildAndAdd();
     * draw.register();
     * </pre>
     *
     * @param x the x coordinate of the box
     * @param y the y coordinate of the box
     * @param z the z coordinate of the box
     * @return a new {@link Box.Builder} instance.
     * @since 1.8.4
     */
    public Box.Builder boxBuilder(int x, int y, int z) {
        return new Box.Builder(this).forBlock(x, y, z);
    }

    /**
     * The builder starts with both ends at the origin, so a line built from it
     * without setting any position has no length and draws nothing. It is white and
     * drawn over the world by default.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.lineBuilder().pos(0, 64, 0, 8, 72, 0).color(0xFF00FF00).buildAndAdd();
     * draw.register();
     * </pre>
     *
     * @return a new {@link Line3D.Builder} instance.
     * @since 1.8.4
     */
    public Line3D.Builder lineBuilder() {
        return new Line3D.Builder(this);
    }

    /**
     * The builder starts with its target at the origin and is white, fully opaque and
     * drawn over the world. The start of the line is worked out from the camera every
     * frame, so there is nothing to set for it.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.traceLineBuilder().pos(0, 64, 0).color(0xFFFF0000).buildAndAdd();
     * draw.register();
     * </pre>
     *
     * @return a new {@link TraceLine.Builder} instance.
     * @since 1.9.0
     */
    public TraceLine.Builder traceLineBuilder() {
        return new TraceLine.Builder(this);
    }

    /**
     * The builder starts with <em>no entity</em>, a half block above where one would
     * be, white, fully opaque and drawn over the world. A line built from it without
     * naming an entity is removed on its first frame rather than pointing at the
     * origin.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const draw = Hud.createDraw3D();
     *   draw.entityTraceLineBuilder().entity(player).color(0xFFFF0000).buildAndAdd();
     *   draw.register();
     * }
     * </pre>
     *
     * @return a new {@link EntityTraceLine.Builder} instance.
     * @since 1.9.0
     */
    public EntityTraceLine.Builder entityTraceLineBuilder() {
        return new EntityTraceLine.Builder(this);
    }

    /**
     * The builder starts ten blocks on a side at the origin, rotated about its middle,
     * with its back drawn, drawn over the world and lit at full brightness. Ten blocks
     * is a large default for a panel whose elements are measured in pixels, so the size
     * is normally set explicitly.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const surface = draw.surfaceBuilder()
     *    .pos(0.5, 64, 0.5)
     *    .size(1, 1)
     *    .minSubdivisions(200)
     *    .buildAndAdd();
     * surface.addText("hello", 10, 10, 0xFFFFFFFF, true);
     * draw.register();
     * </pre>
     *
     * @return a new {@link Surface.Builder} instance.
     * @since 1.8.4
     */
    public Surface.Builder surfaceBuilder() {
        return new Surface.Builder(this);
    }

    /**
     * register so it actually shows up
     * <p>
     * Building the elements is not registering the overlay, so one that was filled in
     * and never registered draws nothing. This adds it to the list the world renderer
     * walks, from which point it draws every frame.
     * <p>
     * Registering the same overlay twice does nothing different, since the list it goes
     * into holds each one once.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFF0000, 0x00000000, false);
     * // nothing has been drawn until this
     * draw.register();
     * </pre>
     *
     * @return self for chaining
     * @since 1.6.5
     */
    @Override
    public Draw3D register() {
        FHud.renders.add(this);
        return this;
    }

    /**
     * takes this overlay off the list the world renderer walks, so it stops drawing.
     * <p>
     * The elements are left on the overlay, so the same object can be registered again
     * later and comes back as it was. This only affects this overlay and leaves every
     * other registered one alone; {@code Hud.clearDraw3Ds()} is the one that takes
     * down all of them.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFF0000, 0x00000000, false);
     * draw.register();
     * draw.unregister();
     * // the box is still on the overlay, just not being drawn
     * Chat.log(`${draw.getBoxes().size()} boxes, none of them visible`);
     * </pre>
     *
     * @return self for chaining
     * @since 1.6.5
     */
    @Override
    public Draw3D unregister() {
        FHud.renders.remove(this);
        return this;
    }

    @DocletIgnore
    public void render(PoseStack poseStack, MultiBufferSource consumers, float tickDelta) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = CameraCompat.position(camera);

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());

        EntityTraceLine.dirty = false;

        synchronized (elements) {
            Collections.sort(elements);

            for (RenderElement3D<?> element : elements) {
                element.render(poseStack, consumers, tickDelta);
            }
        }

        if (EntityTraceLine.dirty) {
            synchronized (elements) {
                elements.removeIf(e -> e instanceof EntityTraceLine etl && etl.shouldRemove);
            }
        }

        poseStack.popPose();
    }

    /**
     * Renders non-surface elements. Surfaces are drawn separately across every
     * registered Draw3D so their back-to-front order is global.
     */
    @DocletIgnore
    public void renderDepthPass(PoseStack poseStack, MultiBufferSource consumers, float tickDelta) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = CameraCompat.position(camera);

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());

        EntityTraceLine.dirty = false;

        synchronized (elements) {
            Collections.sort(elements);

            for (RenderElement3D<?> element : elements) {
                if (element instanceof Surface) {
                    continue;
                }
                element.render(poseStack, consumers, tickDelta);
            }
        }

        if (EntityTraceLine.dirty) {
            synchronized (elements) {
                elements.removeIf(e -> e instanceof EntityTraceLine etl && etl.shouldRemove);
            }
        }

        poseStack.popPose();
    }

    /**
     * Renders only the always-on-top (cull=false) surfaces.
     */
    @DocletIgnore
    public void renderAlwaysOnTopSurfaces(PoseStack poseStack, MultiBufferSource consumers, float tickDelta) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = CameraCompat.position(camera);

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());

        synchronized (elements) {
            Collections.sort(elements);

            for (RenderElement3D<?> element : elements) {
                if (element instanceof Surface surface && !surface.cull) {
                    element.render(poseStack, consumers, tickDelta);
                }
            }
        }

        poseStack.popPose();
    }

    /**
     * Renders the elements that draw themselves directly (surfaces and their children:
     * rect/line/text/image/item) into the buffer source. Must be called inside the render
     * pass's output override, after the gizmo passes, and flushed by the caller.
     * {@code alwaysOnTop} selects the group drawn after the always-on-top depth clear.
     */
    @DocletIgnore
    public void renderDirect(PoseStack poseStack, MultiBufferSource consumers, float tickDelta, boolean alwaysOnTop) {
        renderDirectSurfaces(Collections.singleton(this), poseStack, consumers, tickDelta, alwaysOnTop);
    }

    /**
     * Surface pipelines do not consistently write depth, so every registered
     * Draw3D must share the same back-to-front order within each depth group.
     */
    @DocletIgnore
    public static void renderDirectSurfaces(Iterable<Draw3D> draws, PoseStack poseStack, MultiBufferSource consumers, float tickDelta, boolean alwaysOnTop) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = CameraCompat.position(camera);

        List<Surface> surfaces = new ArrayList<>();
        for (Draw3D draw : draws) {
            synchronized (draw.elements) {
                for (RenderElement3D<?> element : draw.elements) {
                    if (element instanceof Surface surface && surface.cull != alwaysOnTop) {
                        surfaces.add(surface);
                    }
                }
            }
        }
        surfaces.sort((a, b) -> Double.compare(distanceSq(b.resolveRenderPos(tickDelta), cameraPos), distanceSq(a.resolveRenderPos(tickDelta), cameraPos)));

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());
        for (Surface surface : surfaces) {
            //? if >=1.21.11 {
            /*surface.renderDirect(poseStack, consumers, tickDelta, alwaysOnTop);
            *///? } else {
            surface.render(poseStack, consumers, tickDelta);
            //? }
        }
        poseStack.popPose();
    }

    private static double distanceSq(Pos3D pos, Vec3 cameraPos) {
        double dx = pos.x - cameraPos.x;
        double dy = pos.y - cameraPos.y;
        double dz = pos.z - cameraPos.z;
        return dx * dx + dy * dy + dz * dz;
    }
}
