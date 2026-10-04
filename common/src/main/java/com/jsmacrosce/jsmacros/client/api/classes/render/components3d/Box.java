package com.jsmacrosce.jsmacros.client.api.classes.render.components3d;

import com.jsmacrosce.doclet.DocletCategory;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
//? if <1.21.11 {
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ShapeRenderer;
//? }
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.api.math.Vec3D;
import com.jsmacrosce.jsmacros.client.api.classes.render.Draw3D;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.util.ColorUtil;

//? if <1.21.11 {
import java.lang.reflect.Field;
//? }
import java.util.Objects;

//? if >=1.21.11 {
/*import net.minecraft.gizmos.CuboidGizmo;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
*///? } else {
import net.minecraft.client.renderer.RenderType;
//?}

//? if <1.21.11 {
import com.mojang.blaze3d.platform.DepthTestFunction;
//? }

/**
 * the outline of an axis-aligned cuboid, and optionally its solid inside.
 * <p>
 * The two corners are two opposite corners rather than a minimum and a maximum, so
 * they can be given in any order and the box is built between them either way. A box
 * that covers one block therefore runs from the block's own position to one block
 * further along each axis, which is what {@code boxBuilder().forBlock(int, int, int)}
 * does for you.
 * <p>
 * Coordinates are block coordinates in the dimension the player is in, and the
 * overlay that holds it is drawn relative to the camera, so a box moves with the
 * player rather than staying put in the world.
 * <p>
 * There are two colours, and they are independent. The outline is always drawn, in
 * {@code color}, and the inside is drawn as well only when {@code fill} is set, in
 * {@code fillColor}. A box made with {@code fill} off and no fill colour therefore
 * shows only its edges.
 * <p>
 * {@code cull} is {@code true} when the box is depth tested, so terrain draws over it,
 * and {@code false} when it is drawn over everything, which is what every
 * {@code Draw3D} helper gives you unless the argument is passed explicitly.
 * {@link Line3D#setAlwaysOnTop(boolean)} says the same thing the other way round.
 * <p>
 * A colour written without an alpha of its own is made opaque, so
 * {@code 0x00FF0000} comes out as {@code 0xFFFF0000}. A colour of {@code 0x00000000}
 * is left alone, since there is nothing there to make visible; use the setters that
 * take a separate alpha when {@code 0} is what is wanted.
 * example:
 * <pre>
 * const draw = Hud.createDraw3D();
 * // a filled red cube one block on a side, drawn over the world
 * draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFF0000, 0x88000000, true);
 * draw.register();
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.0.6
 */
@SuppressWarnings("unused")
@DocletCategory("Rendering/Graphics")
public class Box implements RenderElement3D<Box> {
    //? if <1.21.11 {
    private static final Field lineDepthTestFunction;
    private static final DepthTestFunction oldlineDepthTestFunction;
    private static final Field boxDepthTestFunction;
    private static final DepthTestFunction oldboxDepthTestFunction;

    static {
        try {
            lineDepthTestFunction = RenderPipelines.LINES.getClass().getDeclaredField("depthTestFunction");
            lineDepthTestFunction.setAccessible(true);
            oldlineDepthTestFunction = (DepthTestFunction) lineDepthTestFunction.get(RenderPipelines.LINES);
            boxDepthTestFunction = RenderPipelines.DEBUG_FILLED_BOX.getClass().getDeclaredField("depthTestFunction");
            boxDepthTestFunction.setAccessible(true);
            oldboxDepthTestFunction = (DepthTestFunction) boxDepthTestFunction.get(RenderPipelines.DEBUG_FILLED_BOX);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    //? }

    /**
     * the two opposite corners of this box, as a segment.
     * <p>
     * Public and writable, so the box can be moved by writing to the coordinates
     * directly as well as through
     * {@link #setPos(double, double, double, double, double, double)}. The two ends
     * are the corners as they were given and are not put in any order, so
     * {@code pos.getStart()} is whichever corner was passed first.
     *
     * @since 1.0.6
     */
    public Vec3D pos;
    /**
     * the colour of the outline, packed as the game packs colours, with the alpha in
     * the top byte. Public and writable; the outline is drawn whatever {@link #fill}
     * is set to.
     *
     * @since 1.0.6
     */
    public int color;
    /**
     * the colour of the inside, packed the same way as {@link #color}. Public and
     * writable. This is only used when {@link #fill} is {@code true}, and is a
     * separate colour from the outline rather than a shade of it.
     *
     * @since 1.0.6
     */
    public int fillColor;
    /**
     * whether the inside of this box is drawn as well as its outline.
     * <p>
     * Public and writable, so it can be changed on a box that is already in an
     * overlay and take effect on the next frame.
     *
     * @since 1.0.6
     */
    public boolean fill;
    /**
     * whether this box is depth tested, so that terrain draws over it.
     * <p>
     * {@code true} is the ordinary box that terrain hides and {@code false} is the one
     * drawn over everything. Every {@code Draw3D} helper that takes no {@code cull}
     * argument leaves this {@code false}.
     *
     * @since 1.0.6
     */
    public boolean cull;

    public Box(double x1, double y1, double z1, double x2, double y2, double z2, int color, int fillColor, boolean fill, boolean cull) {
        setPos(x1, y1, z1, x2, y2, z2);
        setColor(color);
        setFillColor(fillColor);
        this.fill = fill;
        this.cull = cull;
    }

    public Box(double x1, double y1, double z1, double x2, double y2, double z2, int color, int alpha, int fillColor, int fillAlpha, boolean fill, boolean cull) {
        setPos(x1, y1, z1, x2, y2, z2);
        setColor(color, alpha);
        setFillColor(fillColor, fillAlpha);
        this.fill = fill;
        this.cull = cull;
    }

    /**
     * moves this box to run between two corners.
     * <p>
     * The two triples are opposite corners rather than a minimum and a maximum, so
     * they can be given in either order and the box is the same either way. This
     * replaces the box's position object rather than writing into the old one, so a
     * reference taken to it beforehand stops following the box.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const box = draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFFFFFF, 0x00000000, false);
     * // move it two blocks along x without changing its size
     * box.setPos(2, 64, 0, 3, 65, 1);
     * draw.register();
     * </pre>
     *
     * @param x1 the x coordinate of the first corner
     * @param y1 the y coordinate of the first corner
     * @param z1 the z coordinate of the first corner
     * @param x2 the x coordinate of the second corner
     * @param y2 the y coordinate of the second corner
     * @param z2 the z coordinate of the second corner
     * @since 1.0.6
     */
    public void setPos(double x1, double y1, double z1, double x2, double y2, double z2) {
        pos = new Vec3D(x1, y1, z1, x2, y2, z2);
    }

    /**
     * set this component's pos to a block
     * <p>
     * The box is made to cover exactly that one block, running from the block's own
     * position to one block further along each axis. A block position is a corner,
     * not a centre, so the block this names is the one whose base corner is at those
     * coordinates rather than the one the player is standing in.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const box = draw.boxBuilder(0, 64, 0).color(0xFFFF0000).buildAndAdd();
     * // point it at a different block instead of rebuilding it
     * box.setPosToBlock(10, 64, 10);
     * draw.register();
     * </pre>
     *
     * @param pos the block to cover
     * @since 1.9.0
     */
    public void setPosToBlock(BlockPosHelper pos) {
        setPosToBlock(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * set this component's pos to a block
     * <p>
     * The box is made to cover exactly that one block, from the coordinates given to
     * one block further along each axis. The numbers are a block corner rather than
     * its middle, so {@code 0, 64, 0} is the block above {@code 0, 63, 0}.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const box = draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFFFFFF, 0x00000000, false);
     * box.setPosToBlock(10, 70, -3);
     * draw.register();
     * </pre>
     *
     * @param x the x coordinate of the block's corner
     * @param y the y coordinate of the block's corner
     * @param z the z coordinate of the block's corner
     * @since 1.9.0
     */
    public void setPosToBlock(int x, int y, int z) {
        setPos(x, y, z, x + 1, y + 1, z + 1);
    }

    /**
     * set this component's pos to a point
     * <p>
     * The box becomes a cube centred on the point, running from one radius below it to
     * one radius above it on each axis, so its side length is twice the radius. A
     * radius of {@code 0.5} is therefore a cube exactly one block on a side.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const box = draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFFFFFF, 0x00000000, false);
     * // a one block cube centred on a point
     * box.setPosToPoint(PositionCommon.createPos(0.5, 64.5, 0.5), 0.5);
     * draw.register();
     * </pre>
     *
     * @param pos the centre of the cube
     * @param radius half the side length, in blocks
     * @since 1.9.0
     */
    public void setPosToPoint(Pos3D pos, double radius) {
        setPosToPoint(pos.getX(), pos.getY(), pos.getZ(), radius);
    }

    /**
     * set this component's pos to a point
     * <p>
     * The cube is centred on the point given, running from one radius below it to one
     * radius above it on each axis, so the side length is twice the radius. A
     * negative radius is not checked and simply flips the two corners, which leaves
     * the same cube.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const box = draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFFFFFF, 0x00000000, false);
     * // a two block cube centred on the middle of that block
     * box.setPosToPoint(0.5, 64.5, 0.5, 1);
     * draw.register();
     * </pre>
     *
     * @param x the x coordinate of the centre
     * @param y the y coordinate of the centre
     * @param z the z coordinate of the centre
     * @param radius half the side length, in blocks
     * @since 1.9.0
     */
    public void setPosToPoint(double x, double y, double z, double radius) {
        setPos(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
    }

    /**
     * A colour with no alpha of its own is made opaque: if the alpha byte is zero and
     * there is some red, green or blue, it becomes {@code 0xFF}. A colour of
     * {@code 0x00000000} is left alone, since there is nothing to make visible, and so
     * is one that already carries a non-zero alpha. Use {@link #setColor(int, int)}
     * when an alpha of {@code 0} is what is wanted.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const box = draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFFFFFF, 0x00000000, false);
     * // no alpha in this one, so the outline comes out fully opaque
     * box.setColor(0x00FF00);
     * draw.register();
     * </pre>
     *
     * @param color the colour of the outline
     * @since 1.0.6
     */
    public void setColor(int color) {
        this.color = ColorUtil.fixAlpha(color);
    }

    /**
     * The same alpha handling as {@link #setColor(int)}, and it is applied whether or
     * not {@link #fill} is on, so a fill colour set ahead of turning the fill on is
     * already opaque by the time it is used.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const box = draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFFFFFF, 0x00000000, false);
     * box.setFill(true);
     * // no alpha in this one, so the inside comes out fully opaque
     * box.setFillColor(0x0000FF);
     * draw.register();
     * </pre>
     *
     * @param fillColor the colour of the inside
     * @since 1.0.6
     */
    public void setFillColor(int fillColor) {
        this.fillColor = ColorUtil.fixAlpha(fillColor);
    }

    /**
     * The alpha is put in the top byte and the rest of the colour kept as it is, with
     * nothing filled in, so an alpha of {@code 0} really does make the outline
     * invisible rather than being treated as a missing one.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const box = draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFFFFFF, 0x00000000, false);
     * box.setColor(0xFF0000, 128);
     * draw.register();
     * </pre>
     *
     * @param color the colour of the outline
     * @param alpha the alpha of the outline, from 0 to 255
     * @since 1.1.8
     */
    public void setColor(int color, int alpha) {
        this.color = (alpha << 24) | (color & 0xFFFFFF);
    }

    /**
     * Only the top byte is replaced and the colour is left alone, so this fades a
     * box's edges without touching what colour they are. A value of {@code 0} makes
     * the outline invisible while leaving the inside alone.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const box = draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFFFFFF, 0x00000000, true);
     * // fade the edges, leave the inside as it is
     * box.setAlpha(64);
     * draw.register();
     * </pre>
     *
     * @param alpha the alpha of the outline, from 0 to 255
     * @since 1.1.8
     */
    public void setAlpha(int alpha) {
        this.color = (alpha << 24) | (color & 0xFFFFFF);
    }

    /**
     * The two are packed the same way as {@link #setColor(int, int)}, and an alpha of
     * {@code 0} is kept rather than being filled in. This only has a visible effect
     * while {@link #fill} is on.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const box = draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFFFFFF, 0x00000000, true);
     * // a half transparent blue inside behind an opaque red outline
     * box.setFillColor(0x0000FF, 128);
     * draw.register();
     * </pre>
     *
     * @param fillColor the colour of the inside
     * @param alpha the alpha of the inside, from 0 to 255
     * @since 1.1.8
     */
    public void setFillColor(int fillColor, int alpha) {
        this.fillColor = (fillColor & 0xFFFFFF) | (alpha << 24);
    }

    /**
     * Only the top byte of the fill colour is replaced and its colour is left alone.
     * This has no visible effect unless {@link #fill} is on, since the inside is not
     * drawn at all otherwise.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const box = draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFFFFFF, 0x00000000, true);
     * box.setFillAlpha(64);
     * draw.register();
     * </pre>
     *
     * @param alpha the alpha of the inside, from 0 to 255
     * @since 1.1.8
     */
    public void setFillAlpha(int alpha) {
        this.fillColor = (fillColor & 0xFFFFFF) | (alpha << 24);
    }

    /**
     * This can be changed on a box that is already in an overlay and takes effect on
     * the next frame, which is the cheap way to make a box appear when something
     * happens. Turning it off leaves the outline and its colour untouched.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const box = draw.addBox(0, 64, 0, 1, 65, 1, 0xFFFF0000, 0x88000000, false);
     * draw.register();
     * // solid once something is true
     * box.setFill(true);
     * </pre>
     *
     * @param fill whether to draw the inside as well as the outline
     * @since 1.0.6
     */
    public void setFill(boolean fill) {
        this.fill = fill;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Box box = (Box) o;
        return Objects.equals(pos, box.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos);
    }

    /**
     * orders this box against another one, on their positions.
     * <p>
     * Only ever reached for two boxes, since the sort compares class names first and
     * only ties get here. What it looks at is the two corners, and the comparison is a
     * raw one on the coordinates rather than a distance, so two boxes that overlap
     * still order against each other rather than tying. The colour, the fill and the
     * cull flag are not part of it.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const a = draw.boxBuilder().forBlock(0, 64, 0).color(0xFFFF0000).build();
     * const b = draw.boxBuilder().forBlock(10, 64, 0).color(0xFF00FF00).build();
     * // the red one is nearer the origin, so it sorts first
     * Chat.log(`a sorts before b: ${a.compareToSame(b) < 0}`);
     * </pre>
     *
     * @param other another box to order this one against
     * @return a negative number, zero, or a positive number as this box is before, the
     *         same as, or after {@code other}
     * @author Etheradon
     * @since 1.8.4
     */
    @Override
    public int compareToSame(Box other) {
        if (other instanceof Box) {
            return pos.compareTo(((Box) other).pos);
        }
        return 0;
    }

    @Override
    @DocletIgnore
    public void render(PoseStack matrixStack, MultiBufferSource consumers, float tickDelta) {
        boolean seeThrough = !this.cull;
        //? if >=1.21.11 {
        /*AABB box = new AABB(pos.getStart().toMojangDoubleVector(), pos.getEnd().toMojangDoubleVector());
        int renderFillColor = fill ? fillColor : 0;
        GizmoStyle style = new GizmoStyle(color, 2.5F, renderFillColor);
        GizmoProperties gizmo = Gizmos.addGizmo(new CuboidGizmo(box, style, false));
        if (seeThrough) {
            gizmo.setAlwaysOnTop();
        }
        *///? } else {
        float x1 = (float) pos.x1;
        float y1 = (float) pos.y1;
        float z1 = (float) pos.z1;
        float x2 = (float) pos.x2;
        float y2 = (float) pos.y2;
        float z2 = (float) pos.z2;

        MultiBufferSource.BufferSource immediate = (MultiBufferSource.BufferSource) consumers;
        try {
            if (seeThrough) {
                // Flush anything already queued first, otherwise this box's endBatch
                // would flush an earlier depth-tested box with NO_DEPTH_TEST too.
                immediate.endBatch();
                lineDepthTestFunction.set(RenderPipelines.LINES, DepthTestFunction.NO_DEPTH_TEST);
                boxDepthTestFunction.set(RenderPipelines.DEBUG_FILLED_BOX, DepthTestFunction.NO_DEPTH_TEST);
            }

            RenderType linesLayer = RenderType.lines();
            RenderType fillLayer = RenderType.debugFilledBox();

            if (this.fill) {
                float fa = ((fillColor >> 24) & 0xFF) / 255.0F;
                float fr = ((fillColor >> 16) & 0xFF) / 255.0F;
                float fg = ((fillColor >> 8) & 0xFF) / 255.0F;
                float fb = (fillColor & 0xFF) / 255.0F;
                ShapeRenderer.addChainedFilledBoxVertices(matrixStack, consumers.getBuffer(fillLayer), x1, y1, z1, x2, y2, z2, fr, fg, fb, fa);
            }

            float r = ((color >> 16) & 0xFF) / 255.0F;
            float g = ((color >> 8) & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            float a = ((color >> 24) & 0xFF) / 255.0F;
            ShapeRenderer.renderLineBox(
                    //? if >1.21.8 {
                    /*matrixStack.last(),
                    *///?} else
                    matrixStack,
                    consumers.getBuffer(linesLayer),
                    x1,
                    y1,
                    z1,
                    x2,
                    y2,
                    z2,
                    r,
                    g,
                    b,
                    a);

            if (seeThrough) {
                immediate.endBatch();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (seeThrough) {
                try {
                    lineDepthTestFunction.set(RenderPipelines.LINES, oldlineDepthTestFunction);
                    boxDepthTestFunction.set(RenderPipelines.DEBUG_FILLED_BOX, oldboxDepthTestFunction);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        //? }
    }

    /**
     * collects the values for a {@link Box}, then builds it.
     * <p>
     * A chain of setters, then {@link #build()} for a box that is not put anywhere or
     * {@link #buildAndAdd()} for one added to the overlay it was made from. The
     * defaults are a box with no size, at the origin, white outline, white inside,
     * fully opaque, not filled, and drawn over everything.
     * <p>
     * There are two ways to give the same box. {@link #forBlock(int, int, int)} takes
     * one block and makes a box covering exactly it, which is the usual case, while
     * the {@code pos} setters take two corners and so can make a box of any size at
     * all.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.boxBuilder(0, 64, 0)
     *    .color(0xFFFF0000)
     *    .fillColor(0x00FF00, 128)
     *    .fill(true)
     *    .cull(true)
     *    .buildAndAdd();
     * draw.register();
     * </pre>
     *
     * @author Etheradon
     * @since 1.8.4
     */
    @DocletCategory("Rendering/Graphics")
    public static class Builder {

        private final Draw3D parent;

        private Pos3D pos1 = new Pos3D(0, 0, 0);
        private Pos3D pos2 = new Pos3D(0, 0, 0);
        private int color = 0xFFFFFF;
        private int fillColor = 0xFFFFFF;
        private int alpha = 0xFF;
        private int fillAlpha = 0xFF;
        private boolean fill = false;
        private boolean cull = false;

        public Builder(Draw3D parent) {
            this.parent = parent;
        }

        /**
         * Taken by reference, so the built box and this builder share the position and
         * moving one moves the other. The two corners are the box's opposite corners
         * rather than a minimum and a maximum, so which one is the first is arbitrary.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * const corner = PositionCommon.createPos(0, 64, 0);
         * draw.boxBuilder().pos1(corner).pos2(1, 65, 1).buildAndAdd();
         * // moving the corner after the fact moves the box with it
         * corner.y = 70;
         * draw.register();
         * </pre>
         *
         * @param pos1 the first position of the box
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos1(Pos3D pos1) {
            this.pos1 = pos1;
            return this;
        }

        /**
         * The block's own position, without its offset, so this is a block corner
         * rather than the middle of the block. The other corner still has to be given
         * separately; this does not by itself make a box one block on a side.
         * example:
         * <pre>
         * const player = Player.getPlayer();
         * if (player !== null) {
         *   const draw = Hud.createDraw3D();
         *   draw.boxBuilder().pos1(player.getBlockPos()).pos2(1, 65, 1).buildAndAdd();
         *   draw.register();
         * }
         * </pre>
         *
         * @param pos1 the first position of the box
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos1(BlockPosHelper pos1) {
            this.pos1 = pos1.toPos3D();
            return this;
        }

        /**
         * One corner on its own. Nothing is sized from it, so this only makes a box
         * once the second corner is given as well; {@link #forBlock(int, int, int)}
         * is the one call that does both.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.boxBuilder().pos1(0, 64, 0).pos2(3, 67, 2).color(0xFFFF0000).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param x1 the x coordinate of the first position of the box
         * @param y1 the y coordinate of the first position of the box
         * @param z1 the z coordinate of the first position of the box
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos1(double x1, double y1, double z1) {
            this.pos1 = new Pos3D(x1, y1, z1);
            return this;
        }

        /**
         * The builder's own position rather than a copy, so changing it changes what a
         * box built afterwards will be.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().boxBuilder();
         * builder.pos1(0, 64, 0);
         * Chat.log(`first corner is ${builder.getPos1()}`);
         * </pre>
         *
         * @return the first position of the box.
         * @since 1.8.4
         */
        public Pos3D getPos1() {
            return pos1;
        }

        /**
         * Taken by reference, so the built box and this builder share the position and
         * moving one moves the other. This is the corner opposite
         * {@link #pos1(double, double, double)}, not a far corner in any particular
         * direction.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * const corner = PositionCommon.createPos(1, 65, 1);
         * draw.boxBuilder().pos1(0, 64, 0).pos2(corner).buildAndAdd();
         * corner.x = 4;
         * draw.register();
         * </pre>
         *
         * @param pos2 the second position of the box
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos2(Pos3D pos2) {
            this.pos2 = pos2;
            return this;
        }

        /**
         * The block's own position, without its offset, so this is a block corner
         * rather than the middle of the block. Two neighbouring block positions given
         * to the two {@code pos} setters make a box one block on a side.
         * example:
         * <pre>
         * const player = Player.getPlayer();
         * if (player !== null) {
         *   const draw = Hud.createDraw3D();
         *   const base = player.getBlockPos();
         *   draw.boxBuilder().pos1(base).pos2(base.offset(1, 1, 1)).buildAndAdd();
         *   draw.register();
         * }
         * </pre>
         *
         * @param pos2 the second position of the box
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos2(BlockPosHelper pos2) {
            this.pos2 = pos2.toPos3D();
            return this;
        }

        /**
         * The corner opposite the first, and together the two decide the box's size
         * and where it sits. Which is the start and which is the end is arbitrary, so
         * they can be given in either order.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.boxBuilder().pos1(0, 64, 0).pos2(3, 67, 2).color(0xFFFF0000).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param x2 the x coordinate of the second position of the box
         * @param y2 the y coordinate of the second position of the box
         * @param z2 the z coordinate of the second position of the box
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos2(double x2, double y2, double z2) {
            this.pos2 = new Pos3D(x2, y2, z2);
            return this;
        }

        /**
         * The builder's own position rather than a copy, so changing it changes what a
         * box built afterwards will be.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().boxBuilder();
         * builder.pos2(1, 65, 1);
         * Chat.log(`second corner is ${builder.getPos2()}`);
         * </pre>
         *
         * @return the second position of the box.
         * @since 1.8.4
         */
        public Pos3D getPos2() {
            return pos2;
        }

        /**
         * Both corners at once, which is the two number form of the two separate
         * {@code pos} setters. The corners are opposite rather than a minimum and a
         * maximum, so they can be given in either order. Both positions are new
         * objects here, so unlike the {@link #pos(Pos3D, Pos3D)} form nothing outside
         * shares them with the built box.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * // a 3 by 3 by 2 box
         * draw.boxBuilder().pos(0, 64, 0, 3, 67, 2).color(0xFFFF0000).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param x1 the x coordinate of the first position of the box
         * @param y1 the y coordinate of the first position of the box
         * @param z1 the z coordinate of the first position of the box
         * @param x2 the x coordinate of the second position of the box
         * @param y2 the y coordinate of the second position of the box
         * @param z2 the z coordinate of the second position of the box
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos(double x1, double y1, double z1, double x2, double y2, double z2) {
            this.pos1 = new Pos3D(x1, y1, z1);
            this.pos2 = new Pos3D(x2, y2, z2);
            return this;
        }

        /**
         * Each block's own position, without its offset. Two blocks a diagonal apart
         * make a box filling the space between them, so two blocks that are one apart
         * on each axis make a box exactly one block on a side.
         * example:
         * <pre>
         * const player = Player.getPlayer();
         * if (player !== null) {
         *   const draw = Hud.createDraw3D();
         *   const base = player.getBlockPos();
         *   draw.boxBuilder().pos(base, base.offset(1, 1, 1)).color(0xFFFF0000).buildAndAdd();
         *   draw.register();
         * }
         * </pre>
         *
         * @param pos1 the first position of the box
         * @param pos2 the second position of the box
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos(BlockPosHelper pos1, BlockPosHelper pos2) {
            this.pos1 = pos1.toPos3D();
            this.pos2 = pos2.toPos3D();
            return this;
        }

        /**
         * Both positions are taken by reference, so the built box and this builder
         * share them and moving either one moves the box.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * const corner = PositionCommon.createPos(0, 64, 0);
         * draw.boxBuilder().pos(corner, PositionCommon.createPos(1, 65, 1)).buildAndAdd();
         * // moving the corner after the fact moves the box with it
         * corner.z = 8;
         * draw.register();
         * </pre>
         *
         * @param pos1 the first position of the box
         * @param pos2 the second position of the box
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos(Pos3D pos1, Pos3D pos2) {
            this.pos1 = pos1;
            this.pos2 = pos2;
            return this;
        }

        /**
         * Highlights the given block position.
         * <p>
         * Both corners at once, running from the coordinates given to one block further
         * along each axis, which is the block those coordinates name. This is the
         * shortest way to say "one block" and overwrites any corners set before it.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.boxBuilder().forBlock(0, 64, 0).color(0xFFFF0000).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param x the x coordinate of the block
         * @param y the y coordinate of the block
         * @param z the z coordinate of the block
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder forBlock(int x, int y, int z) {
            this.pos1 = new Pos3D(x, y, z);
            this.pos2 = new Pos3D(x + 1, y + 1, z + 1);
            return this;
        }

        /**
         * Highlights the given block position.
         * <p>
         * Both corners at once, running from the block's own position to one block
         * further along each axis, which is the block that position names rather than
         * the one the player is in. The block's offset is not included, so a block at
         * {@code 0.5, 64, 0.5} still makes the box covering the whole block.
         * example:
         * <pre>
         * const player = Player.getPlayer();
         * if (player !== null) {
         *   const draw = Hud.createDraw3D();
         *   draw.boxBuilder().forBlock(player.getBlockPos()).color(0xFFFF0000).buildAndAdd();
         *   draw.register();
         * }
         * </pre>
         *
         * @param pos the block position
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder forBlock(BlockPosHelper pos) {
            this.pos1 = pos.toPos3D();
            this.pos2 = pos.offset(1, 1, 1).toPos3D();
            return this;
        }

        /**
         * The alpha is read out of this colour rather than being left alone, so a
         * colour written with a zero alpha byte comes out opaque unless the alpha was
         * set separately afterwards. This is the outline colour, not the fill.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().boxBuilder();
         * // half a red outline, with the alpha coming out of the colour
         * builder.color(0x80FF0000);
         * Chat.log(`colour 0x${builder.getColor().toString(16)}, alpha ${builder.getAlpha()}`);
         * </pre>
         *
         * @param color the color of the box
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder color(int color) {
            this.color = color;
            this.alpha = ColorUtil.fixAlpha(color) >>> 24;
            return this;
        }

        /**
         * The two are kept apart on the builder, so the colour is stored exactly as
         * given and whatever alpha it carried is not what the built box is drawn at.
         * This is the outline colour; the inside is {@link #fillColor(int, int)}.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().boxBuilder();
         * builder.color(0xFF0000, 128);
         * Chat.log(`colour 0x${builder.getColor().toString(16)}, alpha ${builder.getAlpha()}`);
         * </pre>
         *
         * @param color the color of the box
         * @param alpha the alpha value for the box's color
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder color(int color, int alpha) {
            this.color = color;
            this.alpha = alpha;
            return this;
        }

        /**
         * Nothing is clamped, so a component over 255 carries into the one above it
         * rather than being cut off, and the alpha is left as the last colour or
         * {@link #alpha(int)} call set it.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().boxBuilder();
         * builder.color(255, 128, 0);
         * Chat.log(`colour 0x${builder.getColor().toString(16)}, alpha ${builder.getAlpha()}`);
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
         * The same packing as the three argument form, with the alpha kept apart. The
         * defaults are a colour of {@code 0xFFFFFF} and an alpha of {@code 0xFF}, so
         * a builder with nothing set builds an opaque white box.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().boxBuilder();
         * builder.color(0, 128, 255, 255);
         * Chat.log(`colour 0x${builder.getColor().toString(16)}, alpha ${builder.getAlpha()}`);
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
         * The builder's own field rather than a copy, and unlike a built box it does
         * not mask the alpha out, so a colour set with an alpha in it reads back with
         * that alpha still in the number.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().boxBuilder();
         * builder.color(0x80FF0000);
         * Chat.log(`colour 0x${builder.getColor().toString(16)}, alpha ${builder.getAlpha()}`);
         * </pre>
         *
         * @return the color of the box.
         * @since 1.8.4
         */
        public int getColor() {
            return color;
        }

        /**
         * From 0 to 255, and independent of the colour: setting this does not touch
         * the colour and setting the colour afterwards does not touch this. This is
         * the outline's alpha; the inside has {@link #fillAlpha(int)}.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().boxBuilder();
         * builder.color(0xFF0000).alpha(64);
         * Chat.log(`outline alpha ${builder.getAlpha()}`);
         * </pre>
         *
         * @param alpha the alpha value for the box's color
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder alpha(int alpha) {
            this.alpha = alpha;
            return this;
        }

        /**
         * From 0 to 255, and {@code 255} until a colour or alpha call changes it. This
         * is the outline's alpha and is independent of the fill's.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().boxBuilder();
         * builder.fillColor(0x0000FF, 64);
         * // the fill's alpha changed; the outline is untouched
         * Chat.log(`outline alpha ${builder.getAlpha()}, fill alpha ${builder.getFillAlpha()}`);
         * </pre>
         *
         * @return the alpha value of the box's color.
         * @since 1.8.4
         */
        public int getAlpha() {
            return alpha;
        }

        /**
         * The alpha is read out of this colour rather than being left alone, the same
         * as the outline's. This only has a visible effect once
         * {@link #fill(boolean)} is on, since the inside is not drawn at all
         * otherwise.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().boxBuilder();
         * // half a blue inside, with the alpha coming out of the colour
         * builder.fill(true).fillColor(0x800000FF);
         * Chat.log(`fill 0x${builder.getFillColor().toString(16)}, alpha ${builder.getFillAlpha()}`);
         * </pre>
         *
         * @param fillColor the fill color of the box
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder fillColor(int fillColor) {
            this.fillColor = fillColor;
            this.fillAlpha = ColorUtil.fixAlpha(fillColor) >>> 24;
            return this;
        }

        /**
         * The two are kept apart on the builder, so the colour is stored exactly as
         * given. This is independent of the outline's colour and alpha, and both can
         * be set at once for a box with a different colour inside and out.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().boxBuilder();
         * // a half transparent blue inside, whatever the outline is
         * builder.color(0xFFFF0000).fill(true).fillColor(0x0000FF, 128);
         * Chat.log(`fill 0x${builder.getFillColor().toString(16)}, alpha ${builder.getFillAlpha()}`);
         * </pre>
         *
         * @param fillColor the fill color of the box
         * @param alpha     the alpha value for the box's fill color
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder fillColor(int fillColor, int alpha) {
            this.fillColor = fillColor;
            this.fillAlpha = alpha;
            return this;
        }

        /**
         * Nothing is clamped, so a component over 255 carries into the one above it,
         * and the fill's alpha is left as the last fill colour or
         * {@link #fillAlpha(int)} call set it. The outline is not touched.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().boxBuilder();
         * builder.fill(true).fillColor(0, 0, 255);
         * Chat.log(`fill 0x${builder.getFillColor().toString(16)}, alpha ${builder.getFillAlpha()}`);
         * </pre>
         *
         * @param r the red component of the fill color
         * @param g the green component of the fill color
         * @param b the blue component of the fill color
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder fillColor(int r, int g, int b) {
            this.fillColor = (r << 16) | (g << 8) | b;
            return this;
        }

        /**
         * The same packing as the three argument form, with the fill's alpha kept
         * apart from the outline's, so a box can have a fully transparent inside and a
         * fully opaque outline.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().boxBuilder();
         * builder.color(0xFFFF0000).fill(true).fillColor(255, 0, 0, 0);
         * // opaque red outline, invisible inside
         * Chat.log(`outline alpha ${builder.getAlpha()}, fill alpha ${builder.getFillAlpha()}`);
         * </pre>
         *
         * @param r the red component of the fill color
         * @param g the green component of the fill color
         * @param b the blue component of the fill color
         * @param a the alpha component of the fill color
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder fillColor(int r, int g, int b, int a) {
            this.fillColor = (r << 16) | (g << 8) | b;
            this.fillAlpha = a;
            return this;
        }

        /**
         * The builder's own field rather than a copy, and unlike a built box it does
         * not mask the alpha out.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().boxBuilder();
         * builder.fill(true).fillColor(0x0000FF);
         * Chat.log(`fill 0x${builder.getFillColor().toString(16)}`);
         * </pre>
         *
         * @return the fill color of the box.
         * @since 1.8.4
         */
        public int getFillColor() {
            return fillColor;
        }

        /**
         * From 0 to 255, and independent of the fill's colour and of the outline's
         * alpha. It only matters once {@link #fill(boolean)} is on.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().boxBuilder();
         * builder.fill(true).fillColor(0x0000FF).fillAlpha(64);
         * Chat.log(`fill alpha ${builder.getFillAlpha()}`);
         * </pre>
         *
         * @param fillAlpha the alpha value for the box's fill color
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder fillAlpha(int fillAlpha) {
            this.fillAlpha = fillAlpha;
            return this;
        }

        /**
         * From 0 to 255, and {@code 255} until a fill colour or fill alpha call
         * changes it. Setting the outline's alpha does not move this one.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().boxBuilder();
         * builder.color(0xFF0000, 64);
         * // the outline faded; the fill is untouched
         * Chat.log(`outline alpha ${builder.getAlpha()}, fill alpha ${builder.getFillAlpha()}`);
         * </pre>
         *
         * @return the alpha value of the box's fill color.
         * @since 1.8.4
         */
        public int getFillAlpha() {
            return fillAlpha;
        }

        /**
         * This is the flag the fill colour hangs on, so setting a fill colour without
         * turning the fill on draws nothing different. The default is off.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.boxBuilder().forBlock(0, 64, 0)
         *    .color(0xFFFF0000)
         *    .fillColor(0x00FF00)
         *    .fill(true)
         *    .buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param fill {@code true} if the box should be filled, {@code false} otherwise
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder fill(boolean fill) {
            this.fill = fill;
            return this;
        }

        /**
         * The default is {@code false}, so a box from this builder draws only its
         * outline unless the fill is turned on.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().boxBuilder();
         * Chat.log(`filled by default: ${builder.isFilled()}`);
         * </pre>
         *
         * @return {@code true} if the box should be filled, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isFilled() {
            return fill;
        }

        /**
         * {@code true} is the depth tested box that terrain hides and {@code false} is
         * the one drawn over everything. The default here is {@code false}, so a box
         * from this builder is on top unless asked otherwise.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * // a box something in front of it can hide
         * draw.boxBuilder().forBlock(0, 64, 0).cull(true).color(0xFFFF0000).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param cull whether to enable culling or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder cull(boolean cull) {
            this.cull = cull;
            return this;
        }

        /**
         * That is, {@code true} when the box is depth tested and terrain can hide it.
         * It is the inverse of what {@link Line3D#isAlwaysOnTop()} reports for a
         * built line.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().boxBuilder();
         * // the default is not culled, so the box is drawn over everything
         * Chat.log(`culled: ${builder.isCulled()}`);
         * </pre>
         *
         * @return {@code true} if culling is enabled for this box, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isCulled() {
            return cull;
        }

        /**
         * Creates the box for the given values and adds it to the draw3D.
         * <p>
         * The box goes into the same list any other 3D box does, so
         * {@code Draw3D.getBoxes()} reports it and {@code removeBox} takes it off
         * again. It draws nothing until the overlay itself is registered.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.boxBuilder().forBlock(0, 64, 0).color(0xFFFF0000).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @return the build box.
         * @since 1.8.4
         */
        public Box buildAndAdd() {
            Box box = build();
            parent.addBox(box);
            return box;
        }

        /**
         * Builds the box from the given values.
         * <p>
         * The box is not put anywhere, so it draws nothing until it is added to an
         * overlay. It is a separate object from the builder, so the builder can be
         * changed and used again afterwards.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * const builder = draw.boxBuilder().color(0xFFFF0000);
         * builder.forBlock(0, 64, 0);
         * const first = builder.build();
         * // the same builder, a second box somewhere else
         * builder.forBlock(10, 64, 10);
         * const second = builder.build();
         * draw.addBox(first);
         * draw.addBox(second);
         * draw.register();
         * </pre>
         *
         * @return the build box.
         */
        public Box build() {
            return new Box(
                    pos1.x,
                    pos1.y,
                    pos1.z,
                    pos2.x,
                    pos2.y,
                    pos2.z,
                    color,
                    alpha,
                    fillColor,
                    fillAlpha,
                    fill,
                    cull
            );
        }

    }
}
