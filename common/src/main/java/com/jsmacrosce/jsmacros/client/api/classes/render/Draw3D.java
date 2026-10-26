package com.jsmacrosce.jsmacros.client.api.classes.render;

import com.jsmacrosce.jsmacros.client.util.RenderCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
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
 * A collection of 3D elements rendered as a world overlay.
 * <p>
 * Elements use world-space block coordinates and are rendered relative to the camera.
 * <p>
 * Elements render only while this overlay is registered with {@link #register()};
 * {@link #unregister()} stops rendering without removing them, while {@link #clear()}
 * removes its elements.
 * <p>
 * Duplicate entries are allowed. Elements are sorted in place before rendering, so typed
 * getters return matching entries in the current sorted order, not insertion order.
 * <p>
 * <p>
 * <pre>
 * // a world overlay with a box and a line in it, registered so it shows
 * const draw = Hud.createDraw3D();
 * draw.addBox(0, 64, 0, 1, 65, 1, Color.rgb(0xFF, 0x00, 0x00), Color.argb(0x88, 0, 0, 0), true);
 * draw.addLine(0, 64, 0, 8, 72, 0, Color.rgb(0x00, 0xFF, 0x00));
 * draw.register();
 *
 * // what went in, in the order it will be drawn
 * Chat.log(`${draw.getBoxes().size()} boxes, ${draw.getLines().size()} lines`);
 *
 * // stop rendering this overlay without removing its elements
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
     * Elements held by this overlay. Rendering sorts this list in place before drawing.
     *
     * @since 1.0.6
     */
    private final List<RenderElement3D<?>> elements = new ArrayList<>();

    /**
     * Returns a new list of this overlay's boxes in their current element order.
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
     * Returns a new list of fixed-endpoint lines, excluding camera trace lines.
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
     * Returns a new list of fixed-target camera trace lines.
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
     * Returns a new list of trace lines targeting entities.
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
     * Returns a new list of this overlay's world-space surfaces. The legacy method name
     * refers to surfaces, not screen overlays.
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
     * Removes all elements from this overlay without unregistering it; other overlays
     * and references to the removed elements are unaffected.
     *
     * @since 1.8.4
     */
    public void clear() {
        synchronized (elements) {
            elements.clear();
        }
    }

    /**
     * Adds an existing render element to this overlay. Elements built with {@code build()}
     * are not added automatically; adding the same instance more than once creates
     * duplicate list entries and draws it more than once.
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
     * Adds an existing box to this overlay. The same box can be added more than once.
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
     * Adds an existing straight 3D line to this overlay. The same line can be added more than once.
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
     * Adds an existing trace line to this overlay. The same line can be added more than once.
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
     * Adds an existing surface to this overlay. The same surface can be added more than once.
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
     * @return the added box
     * @since 1.0.6
     */
    public Box addBox(double x1, double y1, double z1, double x2, double y2, double z2, int color, int fillColor, boolean fill) {
        return addBox(x1, y1, z1, x2, y2, z2, color, fillColor, fill, false);
    }

    /**
     * Adds a box between two corners, optionally depth-tested against world geometry.
     *
     * @param x1 the x coordinate of the first corner
     * @param y1 the y coordinate of the first corner
     * @param z1 the z coordinate of the first corner
     * @param x2 the x coordinate of the second corner
     * @param y2 the y coordinate of the second corner
     * @param z2 the z coordinate of the second corner
     * @param color the colour of the outline, with or without an alpha of its own
     * @param fillColor the colour of the inside
     * @param fill whether to draw the inside as well as the outline
     * @param cull whether to depth-test the box against world geometry; {@code false} draws over it
     * @return the added box
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
     * @return the added box
     * @since 1.1.8
     */
    public Box addBox(double x1, double y1, double z1, double x2, double y2, double z2, int color, int alpha, int fillColor, int fillAlpha, boolean fill) {
        return addBox(x1, y1, z1, x2, y2, z2, color, alpha, fillColor, fillAlpha, fill, false);
    }

    /**
     * Adds a box between opposite corners with separate outline and fill alpha values.
     * The alpha arguments determine opacity; {@code cull} enables depth testing so
     * terrain can occlude the box. Without this argument, depth testing is disabled.
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
     * @return the added box
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
     * @return this overlay
     * @since 1.0.6
     */
    public Draw3D removeBox(Box b) {
        synchronized (elements) {
            elements.remove(b);
        }
        return this;
    }

    /**
     * Adds a line between two world-space endpoints. A zero alpha byte is treated as opaque
     * when the RGB value is nonzero; {@code 0x00000000} remains transparent.
     *
     * @param x1 the x coordinate of the start
     * @param y1 the y coordinate of the start
     * @param z1 the z coordinate of the start
     * @param x2 the x coordinate of the end
     * @param y2 the y coordinate of the end
     * @param z2 the z coordinate of the end
     * @param color the line color, with or without an alpha byte
     * @return the {@link Line3D} you added.
     * @since 1.0.6
     */
    public Line3D addLine(double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        return addLine(x1, y1, z1, x2, y2, z2, color, false);
    }

    /**
     * Adds a line between two world-space endpoints, optionally depth-tested against world
     * geometry. With {@code cull} false, it draws over the world.
     *
     * @param x1 the x coordinate of the start
     * @param y1 the y coordinate of the start
     * @param z1 the z coordinate of the start
     * @param x2 the x coordinate of the end
     * @param y2 the y coordinate of the end
     * @param z2 the z coordinate of the end
     * @param color the colour of the line, with or without an alpha of its own
     * @param cull whether to depth-test the line against world geometry
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
     * Adds a line between two world-space endpoints with an explicit alpha from 0 to 255.
     * The alpha is used as given, so zero makes the line transparent.
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
     * Adds a line between two world-space endpoints with an explicit alpha, optionally depth-tested
     * against world geometry.
     * @param x1 the x coordinate of the start
     * @param y1 the y coordinate of the start
     * @param z1 the z coordinate of the start
     * @param x2 the x coordinate of the end
     * @param y2 the y coordinate of the end
     * @param z2 the z coordinate of the end
     * @param color the colour of the line
     * @param alpha the alpha of the line, from 0 to 255
     * @param cull whether to depth-test the line against world geometry
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
     * Removes one matching straight line entry from this overlay, if present.
     *
     * @param l the straight line to remove
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
     * Adds a line from the camera toward a fixed world-space target. Its camera endpoint
     * is updated each frame; it draws over terrain by default. Use {@link #traceLineBuilder()}
     * to configure depth testing or alpha.
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
     * Adds a camera-to-target line with an explicit alpha from 0 to 255. Unlike the
     * single-color form, alpha zero remains transparent. It draws over terrain by default.
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
     * Adds a camera-to-target line using the position's coordinates at call time. Later
     * changes to {@code pos} do not move the target; the line draws over terrain by default.
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
     * Adds a camera-to-target line using the position's coordinates at call time, with
     * an explicit alpha from 0 to 255. Alpha zero is transparent; the line draws over
     * terrain by default.
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
     * Adds a camera-to-entity line. The target follows the entity's interpolated position
     * plus a 0.5-block vertical offset, recalculated each frame. The line draws over
     * terrain by default and is removed from the overlay after the entity is gone.
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
     * Adds a line from the crosshair to an entity, with an explicit alpha. It uses the
     * default 0.5-block vertical offset and is drawn over the world.
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
     * Adds a line from the crosshair to an entity at a chosen vertical offset above
     * its feet. The offset is in blocks and follows the entity each frame; the line
     * is drawn over the world.
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const draw = Hud.createDraw3D();
     *   // aim at the player's eyes rather than their feet
     *   draw.addEntityTraceLine(player, Color.rgb(0xFF, 0x00, 0x00), 255, player.getEyeHeight());
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
     * Adds a line from the crosshair to an entity at a chosen vertical offset above
     * its feet. The offset is in blocks and follows the entity each frame. Set
     * {@code alwaysOnTop} to {@code true} to draw it over the world; {@code false}
     * allows terrain to hide it.
     *
     * @param entity the entity to point at
     * @param color the colour of the line
     * @param alpha the alpha of the line, from 0 to 255
     * @param yOffset how far above the entity's feet to aim, in blocks
     * @param alwaysOnTop whether to draw over the world instead of allowing terrain to hide the line
     * @return the added line
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
     * Removes the trace line from this overlay if present.
     *
     * @param traceLine the line to remove
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
     * Adds a filled cube centered at {@code point}, with side length twice the radius.
     *
     * @param point  the center point
     * @param radius 1/2 of the side length of the cube
     * @param color  point color
     * @return the added box
     * @see Box
     * @since 1.4.0
     */
    public Box addPoint(Pos3D point, double radius, int color) {
        return addPoint(point.getX(), point.getY(), point.getZ(), radius, color);
    }

    /**
     * Adds a filled cube centered at the given coordinates, with side length twice the
     * radius. Its outline and fill use the same color.
     *
     * @param x      x value of the center point
     * @param y      y value of the center point
     * @param z      z value of the center point
     * @param radius 1/2 of the side length of the cube
     * @param color  point color
     * @return the added box
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
     * Adds a filled cube centered at the given coordinates, with side length twice the
     * radius. The supplied alpha applies to both its outline and fill; {@code cull}
     * controls whether terrain can hide it.
     *
     * @param x      x value of the center point
     * @param y      y value of the center point
     * @param z      z value of the center point
     * @param radius 1/2 of the side length of the cube
     * @param color  point color
     * @param alpha  alpha of the point, used for both the outline and the inside
     * @param cull   whether to cull the point or not
     * @return the added box
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
     * Adds a 1-by-1 panel with no rotation, 200 minimum subdivisions, and no back face
     * rendering or depth culling.
     *
     * @param x the x coordinate of the panel corner
     * @param y the y coordinate of the panel corner
     * @param z the z coordinate of the panel corner
     * @return the {@link Surface} you added.
     * @since 1.6.5
     */
    public Surface addDraw2D(double x, double y, double z) {
        return addDraw2D(x, y, z, 0, 0, 0, 1, 1, 200, false, false);
    }

    /**
     * Adds a panel of the given size with no rotation, 200 minimum subdivisions, and
     * no back face rendering or depth culling.
     *
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
     * Adds a 1-by-1 panel with the given rotation, 200 minimum subdivisions, and no
     * back face rendering or depth culling.
     *
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
     * Adds a panel with the given position, rotation, and size, using 200 minimum subdivisions
     * with back-face rendering and depth culling disabled.
     *
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
     * Adds a panel with the requested minimum subdivision count, clamped to at least 1; back-face
     * rendering and depth culling are disabled.
     *
     * @param x the x coordinate of the panel's corner
     * @param y the y coordinate of the panel's corner
     * @param z the z coordinate of the panel's corner
     * @param xRot the rotation about the x axis, in degrees
     * @param yRot the rotation about the y axis, in degrees
     * @param zRot the rotation about the z axis, in degrees
     * @param width the width of the panel, in blocks
     * @param height the height of the panel, in blocks
     * @param minSubdivisions the minimum number of subdivisions along the shorter side
     * @return the {@link Surface} you added.
     * @since 1.6.5
     */
    public Surface addDraw2D(double x, double y, double z, double xRot, double yRot, double zRot, double width, double height, int minSubdivisions) {
        return addDraw2D(x, y, z, xRot, yRot, zRot, width, height, minSubdivisions, false, false);
    }

    /**
     * Adds a panel with optional back-face rendering. The subdivision count is clamped to at least
     * 1, and depth culling is disabled.
     *
     * @param x the x coordinate of the panel's corner
     * @param y the y coordinate of the panel's corner
     * @param z the z coordinate of the panel's corner
     * @param xRot the rotation about the x axis, in degrees
     * @param yRot the rotation about the y axis, in degrees
     * @param zRot the rotation about the z axis, in degrees
     * @param width the width of the panel, in blocks
     * @param height the height of the panel, in blocks
     * @param minSubdivisions the minimum number of subdivisions along the shorter side
     * @param renderBack whether the back of the panel is drawn as well as its front
     * @return the {@link Surface} you added.
     * @since 1.6.5
     */
    public Surface addDraw2D(double x, double y, double z, double xRot, double yRot, double zRot, double width, double height, int minSubdivisions, boolean renderBack) {
        return addDraw2D(x, y, z, xRot, yRot, zRot, width, height, minSubdivisions, renderBack, false);
    }

    /**
     * Adds a panel with the specified geometry and rendering options. The subdivision count is
     * clamped to at least 1.
     *
     * @param x               the x coordinate of the panel corner
     * @param y               the y coordinate of the panel corner
     * @param z               the z coordinate of the panel corner
     * @param xRot the rotation about the x axis, in degrees
     * @param yRot the rotation about the y axis, in degrees
     * @param zRot the rotation about the z axis, in degrees
     * @param width the width of the panel, in blocks
     * @param height the height of the panel, in blocks
     * @param minSubdivisions the minimum number of subdivisions along the shorter side
     * @param renderBack whether the back of the panel is drawn as well as its front
     * @param cull whether to depth-test the panel against world geometry; {@code false} draws over it
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
     * Removes a panel from this overlay. The surface object and its elements are not modified.
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
     * Creates a box builder whose corners default to the origin and whose color is white; set
     * both corners and the color before building. Boxes are unfilled and drawn over the world by
     * default. For a one-block box, use {@link #boxBuilder(int, int, int)}.
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.boxBuilder().pos(0, 64, 0, 3, 67, 2).color(Color.rgb(0xFF, 0x00, 0x00)).buildAndAdd();
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
     * Creates a box builder with its corners set to the block's position and the opposite corner
     * one block farther along each axis.
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const draw = Hud.createDraw3D();
     *   draw.boxBuilder(player.getBlockPos()).color(Color.rgb(0xFF, 0x00, 0x00)).buildAndAdd();
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
     * Creates a box builder spanning the block at {@code (x, y, z)}.
     * <pre>
     * const draw = Hud.createDraw3D();
     * // a wireframe around a single block
     * draw.boxBuilder(0, 64, 0).color(Color.rgb(0xFF, 0x00, 0x00)).buildAndAdd();
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
     * Creates a line builder with both endpoints at the origin, white, and drawn over the world.
     * Set distinct endpoints for a visible line.
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.lineBuilder().pos(0, 64, 0, 8, 72, 0).color(Color.rgb(0x00, 0xFF, 0x00)).buildAndAdd();
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
     * Creates a trace-line builder targeting the origin, white and fully opaque, drawn over the
     * world. Its start point is calculated from the camera each frame.
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.traceLineBuilder().pos(0, 64, 0).color(Color.rgb(0xFF, 0x00, 0x00)).buildAndAdd();
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
     * Creates a builder for a white, fully opaque, always-on-top line that follows an
     * entity, with a {@code 0.5}-block vertical offset. No entity is set by default;
     * a line built without one is removed on its first frame.
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const draw = Hud.createDraw3D();
     *   draw.entityTraceLineBuilder().entity(player).color(Color.rgb(0xFF, 0x00, 0x00)).buildAndAdd();
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
     * Creates a surface builder with a {@code 10 × 10}-block size, centered rotation,
     * a rendered back, no depth culling, and full-bright lighting. Its default
     * subdivision count is {@code 1}, so set the size and subdivisions for the desired
     * panel dimensions and element resolution.
     * <pre>
     * const draw = Hud.createDraw3D();
     * const surface = draw.surfaceBuilder()
     *    .pos(0.5, 64, 0.5)
     *    .size(1, 1)
     *    .minSubdivisions(200)
     *    .buildAndAdd();
     * surface.addText("hello", 10, 10, Color.rgb(0xFF, 0xFF, 0xFF), true);
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
     * Registers this overlay for rendering. Adding elements alone does not make them
     * render; registration adds this instance to the renderer's set. Re-registering an
     * already registered instance has no additional effect.
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.addBox(0, 64, 0, 1, 65, 1, Color.rgb(0xFF, 0x00, 0x00), Color.argb(0, 0, 0, 0), false);
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
     * Unregisters this overlay without removing its elements. It can be registered
     * again later; other overlays are unaffected.
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.addBox(0, 64, 0, 1, 65, 1, Color.rgb(0xFF, 0x00, 0x00), Color.argb(0, 0, 0, 0), false);
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
    public void render(PoseStack poseStack, RenderSink sink, float tickDelta) {
        Camera camera = RenderCompat.mainCamera(Minecraft.getInstance());
        Vec3 cameraPos = CameraCompat.position(camera);

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());

        EntityTraceLine.dirty = false;

        synchronized (elements) {
            Collections.sort(elements);

            for (RenderElement3D<?> element : elements) {
                element.render(poseStack, sink, tickDelta);
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
     * Renders non-surface elements; surfaces are rendered separately to sort them
     * back-to-front across overlays.
     */
    @DocletIgnore
    public void renderDepthPass(PoseStack poseStack, RenderSink sink, float tickDelta) {
        Camera camera = RenderCompat.mainCamera(Minecraft.getInstance());
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
                element.render(poseStack, sink, tickDelta);
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
     * Renders only surfaces with depth culling disabled ({@code cull == false}).
     */
    @DocletIgnore
    public void renderAlwaysOnTopSurfaces(PoseStack poseStack, RenderSink sink, float tickDelta) {
        Camera camera = RenderCompat.mainCamera(Minecraft.getInstance());
        Vec3 cameraPos = CameraCompat.position(camera);

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());

        synchronized (elements) {
            Collections.sort(elements);

            for (RenderElement3D<?> element : elements) {
                if (element instanceof Surface surface && !surface.cull) {
                    element.render(poseStack, sink, tickDelta);
                }
            }
        }

        poseStack.popPose();
    }

    /**
     * Renders this overlay's surfaces and their 2D elements into the buffer source for
     * the selected depth group. The caller must invoke this inside the render pass's
     * output override, after gizmo passes, and flush the buffer source.
     */
    @DocletIgnore
    public void renderDirect(PoseStack poseStack, RenderSink sink, float tickDelta, boolean alwaysOnTop) {
        renderDirectSurfaces(Collections.singleton(this), poseStack, sink, tickDelta, alwaysOnTop);
    }

    /**
     * Renders surfaces from the supplied overlays in back-to-front order within the
     * selected depth group. To order surfaces across multiple overlays, supply them
     * together in one call; the caller manages the render pass and buffer flushing.
     */
    @DocletIgnore
    public static void renderDirectSurfaces(Iterable<Draw3D> draws, PoseStack poseStack, RenderSink sink, float tickDelta, boolean alwaysOnTop) {
        Camera camera = RenderCompat.mainCamera(Minecraft.getInstance());
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
            /*surface.renderDirect(poseStack, sink, tickDelta, alwaysOnTop);
            *///? } else {
            surface.render(poseStack, sink, tickDelta);
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
