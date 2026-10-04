package com.jsmacrosce.jsmacros.client.api.classes.render.components3d;

import com.jsmacrosce.doclet.DocletCategory;

import com.mojang.blaze3d.vertex.PoseStack;
//? if <1.21.11 {
import com.mojang.blaze3d.vertex.VertexConsumer;
//? }

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;

import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.api.math.Vec3D;
import com.jsmacrosce.jsmacros.client.api.classes.render.Draw3D;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.util.ColorUtil;

import java.lang.reflect.Field;
import java.util.Objects;

//? if >=1.21.11 {
/*import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.LineGizmo;
*///? } else {
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.renderer.RenderType;
//?}

/**
 * a straight line between two points in the world, with a colour.
 * <p>
 * This is the fixed version: both ends are set by the script and neither moves on its
 * own, so it is what a script wants for anything it is drawing rather than for
 * something that should track the view. A line that starts at the camera and points
 * where the crosshair is over is a {@link TraceLine} instead.
 * <p>
 * The positions are block coordinates in the dimension the player is in, and the
 * overlay they belong to is drawn relative to the camera, so they move with the
 * player rather than staying put in the world.
 * <p>
 * The colour is packed as the game packs colours, with the alpha in the top byte.
 * Reading it back and writing it again is not quite symmetrical: {@link #getColor()}
 * masks the alpha out, so writing that value back with {@link #setColor(int)} gives
 * an opaque line of the same colour rather than the one that was read.
 * <p>
 * {@code cull} is depth testing: {@code true} means the line is depth tested, so terrain
 * hides it, and {@code false} means it is drawn over everything.
 * {@link #setAlwaysOnTop(boolean)} and {@link #isAlwaysOnTop()} say the same thing the
 * other way round, and are the easier pair to read.
 * example:
 * <pre>
 * const draw = Hud.createDraw3D();
 * // a green line across one block, hidden by anything in front of it
 * const line = draw.addLine(0, 64, 0, 1, 65, 0, 0xFF00FF00, true);
 * Chat.log(`runs from ${line.getPos1()} to ${line.getPos2()}`);
 * draw.register();
 * </pre>
 *
 * @author Wagyourtail
 */
@SuppressWarnings("unused")
@DocletCategory("Rendering/Graphics")
public class Line3D implements RenderElement3D<Line3D> {
    //? if <1.21.11 {
    private static final Field lineDepthTestFunction;
    private static final DepthTestFunction oldlineDepthTestFunction;

    static {
        try {
            lineDepthTestFunction = RenderPipelines.LINES.getClass().getDeclaredField("depthTestFunction");
            lineDepthTestFunction.setAccessible(true);
            oldlineDepthTestFunction = (DepthTestFunction) lineDepthTestFunction.get(RenderPipelines.LINES);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to reflect into RenderLayer for Line3D", e);
        }
    }
    //? }

    /**
     * the two ends of this line, as a segment.
     * <p>
     * Public and writable, so it can be moved by writing to its coordinates directly
     * as well as through {@link #setPos(double, double, double, double, double, double)}.
     * The segment runs from {@code x1, y1, z1} to {@code x2, y2, z2}, which are two
     * points rather than a minimum and a maximum, so the ends can be given in either
     * order.
     *
     * @since 1.0.6
     */
    public Vec3D pos;
    /**
     * the colour of this line, packed as the game packs colours, with the alpha in
     * the top byte. Public and writable; see {@link #setColor(int)} for what happens
     * to a colour with no alpha of its own.
     *
     * @since 1.0.6
     */
    public int color;
    /**
     * whether this line is depth tested, so that terrain draws over it.
     * <p>
     * {@code true} is the ordinary depth tested line and {@code false} is the one
     * drawn over everything. {@link #setAlwaysOnTop(boolean)} sets this the other way
     * round and is usually the easier thing to reach for.
     *
     * @since 1.0.6
     */
    public boolean cull;

    public Line3D(double x1, double y1, double z1, double x2, double y2, double z2, int color, boolean cull) {
        setPos(x1, y1, z1, x2, y2, z2);
        setColor(color);
        this.cull = cull;
    }

    public Line3D(double x1, double y1, double z1, double x2, double y2, double z2, int color, int alpha, boolean cull) {
        setPos(x1, y1, z1, x2, y2, z2);
        setColor(color, alpha);
        this.cull = cull;
    }

    /**
     * The two triples are the start and the end of the line rather than a minimum and
     * a maximum, so they can be given in either order and swapping them gives the
     * same line running the other way. The coordinates are blocks in the dimension the
     * player is in.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addLine(0, 64, 0, 8, 72, 0, 0xFFFFFFFF);
     * line.setPos(0, 64, 0, 0, 72, 8);
     * draw.register();
     * </pre>
     *
     * @param x1
     * @param y1
     * @param z1
     * @param x2
     * @param y2
     * @param z2
     * @since 1.0.6
     */
    public void setPos(double x1, double y1, double z1, double x2, double y2, double z2) {
        pos = new Vec3D(x1, y1, z1, x2, y2, z2);
    }

    /**
     * A copy, so moving the line after reading this does not change what this returns.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addLine(0, 64, 0, 8, 72, 0, 0xFFFFFFFF);
     * Chat.log(`line is ${line.getPos()}`);
     * </pre>
     *
     * @return the positions of the line.
     * @since 2.0.0
     */
    public Vec3D getPos() {
        return new Vec3D(pos);
    }

    /**
     * A new position rather than the line's own, so writing to it does not move the
     * line. The two ends are set in the order they were given to
     * {@link #setPos(double, double, double, double, double, double)}, which is not
     * necessarily near to far.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addLine(0, 64, 0, 8, 72, 0, 0xFFFFFFFF);
     * Chat.log(`starts at ${line.getPos1()}`);
     * </pre>
     *
     * @return the first position of the line.
     * @since 2.0.0
     */
    public Pos3D getPos1() {
        return new Pos3D(pos.x1, pos.y1, pos.z1);
    }

    /**
     * A new position rather than the line's own, so writing to it does not move the
     * line. The two ends are set in the order they were given to
     * {@link #setPos(double, double, double, double, double, double)}, which is not
     * necessarily near to far.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addLine(0, 64, 0, 8, 72, 0, 0xFFFFFFFF);
     * Chat.log(`ends at ${line.getPos2()}`);
     * </pre>
     *
     * @return the second position of the line.
     * @since 2.0.0
     */
    public Pos3D getPos2() {
        return new Pos3D(pos.x2, pos.y2, pos.z2);
    }

    /**
     * A colour with no alpha of its own is given one: if the alpha byte is zero and
     * there is some red, green or blue, it becomes {@code 0xFF}. A colour of
     * {@code 0x00000000} is left alone, since there is nothing to make visible, and
     * so is one that already carries a non-zero alpha. Use
     * {@link #setColor(int, int)} when an alpha of {@code 0} is what is wanted.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addLine(0, 64, 0, 8, 72, 0, 0xFFFFFFFF);
     * // no alpha in this one, so it comes out fully opaque
     * line.setColor(0x00FF00);
     * draw.register();
     * </pre>
     *
     * @param color
     * @since 1.0.6
     */
    public void setColor(int color) {
        this.color = ColorUtil.fixAlpha(color);
    }

    /**
     * The alpha is put in the top byte and the rest of the colour kept as it is, with
     * nothing filled in, so an alpha of {@code 0} really does make the line invisible.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addLine(0, 64, 0, 8, 72, 0, 0xFFFFFFFF);
     * line.setColor(0x00FF00, 128);
     * draw.register();
     * </pre>
     *
     * @param color
     * @param alpha
     * @since 1.1.8
     */
    public void setColor(int color, int alpha) {
        this.color = (alpha << 24) | (color & 0xFFFFFF);
    }

    /**
     * The alpha is masked out, so a line drawn fully opaque and one drawn at half alpha
     * with the same colour read back the same. {@link #getAlpha()} is the other half.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addLine(0, 64, 0, 8, 72, 0, 0xFFFF0000);
     * Chat.log(`colour 0x${line.getColor().toString(16)}, alpha ${line.getAlpha()}`);
     * </pre>
     *
     * @return the color of the line.
     * @since 2.0.0
     */
    public int getColor() {
        return color & 0xFFFFFF;
    }

    /**
     * Only the top byte is replaced and the colour is left alone, so this fades a line
     * without touching what colour it is. A value of {@code 0} makes the line
     * invisible.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addLine(0, 64, 0, 8, 72, 0, 0xFFFF0000);
     * line.setAlpha(64);
     * draw.register();
     * </pre>
     *
     * @param alpha
     * @since 1.1.8
     */
    public void setAlpha(int alpha) {
        this.color = (alpha << 24) | (color & 0xFFFFFF);
    }

    /**
     * From 0 to 255, where 255 is fully opaque. Together with {@link #getColor()} this
     * is the whole of the packed colour.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addLine(0, 64, 0, 8, 72, 0, 0xFFFF0000);
     * Chat.log(`drawn at alpha ${line.getAlpha()}`);
     * </pre>
     *
     * @return the alpha value of the line's color.
     * @since 2.0.0
     */
    public int getAlpha() {
        return (color >> 24) & 0xFF;
    }

    /**
     * This sets the {@code cull} field the other way round, so a line made with
     * {@code addLine} and no {@code cull} argument comes out on top of everything and
     * this is what turns that off. The two are the same setting under opposite names,
     * and neither is checked against the other.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addLine(0, 64, 0, 8, 72, 0, 0xFFFF0000);
     * // on top by default; make this one something terrain can hide
     * line.setAlwaysOnTop(false);
     * draw.register();
     * </pre>
     *
     * @param alwaysOnTop whether the line should render on top of everything else.
     * @since 2.0.0
     */
    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.cull = !alwaysOnTop;
    }

    /**
     * The inverse of the {@code cull} field, so a line is on top when {@code cull} is
     * {@code false}.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addLine(0, 64, 0, 8, 72, 0, 0xFFFF0000);
     * Chat.log(`draws over terrain: ${line.isAlwaysOnTop()}`);
     * </pre>
     *
     * @return whether the line renders on top of everything else.
     * @since 2.0.0
     */
    public boolean isAlwaysOnTop() {
        return !cull;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Line3D line3D = (Line3D) o;
        return Objects.equals(pos, line3D.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos);
    }

    /**
     * orders this line against another one, on their positions.
     * <p>
     * Only ever reached for two lines, since the sort compares class names first and
     * only ties get here. What it looks at is the two ends, and the comparison is a raw
     * one on the coordinates rather than a distance, so two lines that cross still order
     * against each other rather than tying. The colour and the cull flag are not part
     * of it.
     * <p>
     * The ends are compared in the order they were given, so a line drawn from A to B
     * can come out before one drawn from B to A even though they are the same segment.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const a = draw.lineBuilder().pos(0, 64, 0, 8, 72, 0).color(0xFF00FF00).build();
     * const b = draw.lineBuilder().pos(10, 64, 0, 18, 72, 0).color(0xFFFF0000).build();
     * Chat.log(`a sorts before b: ${a.compareToSame(b) < 0}`);
     * </pre>
     *
     * @param o another line to order this one against
     * @return a negative number, zero, or a positive number as this line is before, the
     *         same as, or after {@code o}
     * @author Etheradon
     * @since 1.8.4
     */
    @Override
    public int compareToSame(Line3D o) {
        return pos.compareTo(o.pos);
    }

    //? if <1.21.11 {
    private void addLine(VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            int color,
            float normalX,
            float normalY,
            float normalZ)
    {
        consumer.addVertex(pose, x, y, z).setColor(color).setNormal(pose, normalX, normalY, normalZ);
    }
    //? }

    @Override
    @DocletIgnore
    public void render(PoseStack matrixStack, MultiBufferSource consumers, float tickDelta) {
        boolean alwaysOnTop = !this.cull;
        //? if >=1.21.11 {
        /*GizmoProperties gizmo = Gizmos.addGizmo(new LineGizmo(
                pos.getStart().toMojangDoubleVector(),
                pos.getEnd().toMojangDoubleVector(),
                color,
                2.5F));
        if (alwaysOnTop) {
            gizmo.setAlwaysOnTop();
        }
        *///? } else {
        try {
            if (alwaysOnTop) {
                // Flush anything already queued with the default depth state first.
                if (consumers instanceof MultiBufferSource.BufferSource immediate) {
                    immediate.endBatch();
                }
                lineDepthTestFunction.set(RenderPipelines.LINES, DepthTestFunction.NO_DEPTH_TEST);
            }
            VertexConsumer consumer = consumers.getBuffer(RenderType.lines());
            PoseStack.Pose entry = matrixStack.last();

            // Draw 3 lines in each of the normals for consistency
            addLine(consumer, entry, (float) pos.x1, (float) pos.y1, (float) pos.z1, color, 1, 0, 0);
            addLine(consumer, entry, (float) pos.x2, (float) pos.y2, (float) pos.z2, color, 1, 0, 0);
            addLine(consumer, entry, (float) pos.x1, (float) pos.y1, (float) pos.z1, color, 0, 1, 0);
            addLine(consumer, entry, (float) pos.x2, (float) pos.y2, (float) pos.z2, color, 0, 1, 0);
            addLine(consumer, entry, (float) pos.x1, (float) pos.y1, (float) pos.z1, color, 0, 0, 1);
            addLine(consumer, entry, (float) pos.x2, (float) pos.y2, (float) pos.z2, color, 0, 0, 1);

            if (alwaysOnTop && consumers instanceof MultiBufferSource.BufferSource immediate) {
                immediate.endBatch();
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            if (alwaysOnTop) {
                try {
                    lineDepthTestFunction.set(RenderPipelines.LINES, oldlineDepthTestFunction);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        //? }
    }

    /**
     * collects the values for a {@link Line3D}, then builds it.
     * <p>
     * A chain of setters, then {@link #build()} for a line that is not put anywhere or
     * {@link #buildAndAdd()} for one added to the overlay it was made from. The
     * defaults are a line from {@code 0, 0, 0} to {@code 0, 0, 0}, white, fully
     * opaque, and drawn over everything.
     * <p>
     * The colour setters keep the colour and the alpha apart internally, which is why
     * there is a separate {@link #alpha(int)}: {@link #color(int)} on its own picks
     * the alpha up out of the colour it is given.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.lineBuilder()
     *    .pos(0, 64, 0, 8, 72, 0)
     *    .color(0xFF00FF00, 128)
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
        private int alpha = 255;
        private boolean cull = false;

        public Builder(Draw3D parent) {
            this.parent = parent;
        }

        /**
         * The position is taken by reference, so the built line and this builder share
         * it and moving one moves the other.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * const start = PositionCommon.createPos(0, 64, 0);
         * draw.lineBuilder().pos1(start).pos2(0, 72, 0).buildAndAdd();
         * // moving the position after the fact moves the line too
         * start.y = 70;
         * draw.register();
         * </pre>
         *
         * @param pos1 the first position of the line
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos1(Pos3D pos1) {
            this.pos1 = pos1;
            return this;
        }

        /**
         * The block's own position, without its offset.
         * example:
         * <pre>
         * const player = Player.getPlayer();
         * if (player !== null) {
         *   const draw = Hud.createDraw3D();
         *   draw.lineBuilder().pos1(player.getBlockPos()).pos2(0, 72, 0).buildAndAdd();
         *   draw.register();
         * }
         * </pre>
         *
         * @param pos1 the first position of the line
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos1(BlockPosHelper pos1) {
            this.pos1 = pos1.toPos3D();
            return this;
        }

        /**
         * @param x1 the x coordinate of the first position of the line
         * @param y1 the y coordinate of the first position of the line
         * @param z1 the z coordinate of the first position of the line
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos1(double x1, double y1, double z1) {
            this.pos1 = new Pos3D(x1, y1, z1);
            return this;
        }

        /**
         * @return the first position of the line.
         * @since 1.8.4
         */
        public Pos3D getPos1() {
            return pos1;
        }

        /**
         * @param pos2 the second position of the line
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos2(Pos3D pos2) {
            this.pos2 = pos2;
            return this;
        }

        /**
         * @param pos2 the second position of the line
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos2(BlockPosHelper pos2) {
            this.pos2 = pos2.toPos3D();
            return this;
        }

        /**
         * This one takes whole numbers, where the matching {@link #pos1(double, double,
         * double)} takes doubles. Both are truncated the same way, so there is no
         * difference beyond what the arguments allow.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.lineBuilder().pos1(0, 64, 0).pos2(8, 72, 0).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param x2 the x coordinate of the second position of the line
         * @param y2 the y coordinate of the second position of the line
         * @param z2 the z coordinate of the second position of the line
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos2(int x2, int y2, int z2) {
            this.pos2 = new Pos3D(x2, y2, z2);
            return this;
        }

        /**
         * This is the builder's own position rather than a copy, so changing it changes
         * what a line built from this builder afterwards will be.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().lineBuilder();
         * builder.pos2(8, 72, 0);
         * Chat.log(`second end is ${builder.getPos2()}`);
         * </pre>
         *
         * @return the second position of the line.
         * @since 1.8.4
         */
        public Pos3D getPos2() {
            return pos2;
        }

        /**
         * The two triples are the start and the end rather than a minimum and a
         * maximum, so they can be given in either order. Both positions are new objects
         * here, so unlike {@link #pos(Pos3D, Pos3D)} nothing outside shares them.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.lineBuilder().pos(0, 64, 0, 8, 72, 0).color(0xFFFFFFFF).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param x1 the x coordinate of the first position of the line
         * @param y1 the y coordinate of the first position of the line
         * @param z1 the z coordinate of the first position of the line
         * @param x2 the x coordinate of the second position of the line
         * @param y2 the x coordinate of the second position of the line
         * @param z2 the z coordinate of the second position of the line
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos(int x1, int y1, int z1, int x2, int y2, int z2) {
            this.pos1 = new Pos3D(x1, y1, z1);
            this.pos2 = new Pos3D(x2, y2, z2);
            return this;
        }

        /**
         * Each block's own position, without its offset, so these are block corners
         * rather than block centres.
         * example:
         * <pre>
         * const player = Player.getPlayer();
         * if (player !== null) {
         *   const draw = Hud.createDraw3D();
         *   draw.lineBuilder().pos(player.getBlockPos(), player.getBlockPos()).buildAndAdd();
         *   draw.register();
         * }
         * </pre>
         *
         * @param pos1 the first position of the line
         * @param pos2 the second position of the line
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos(BlockPosHelper pos1, BlockPosHelper pos2) {
            this.pos1 = pos1.toPos3D();
            this.pos2 = pos2.toPos3D();
            return this;
        }

        /**
         * Both positions are taken by reference, so the built line and this builder
         * share them and moving one moves the other. The two are the start and the end
         * of the line rather than a minimum and a maximum, so they can be given in
         * either order.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * const start = PositionCommon.createPos(0, 64, 0);
         * const end = PositionCommon.createPos(8, 72, 0);
         * draw.lineBuilder().pos(start, end).buildAndAdd();
         * // moving either position after the fact moves the line
         * end.y = 80;
         * draw.register();
         * </pre>
         *
         * @param pos1 the first position of the line
         * @param pos2 the second position of the line
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos(Pos3D pos1, Pos3D pos2) {
            this.pos1 = pos1;
            this.pos2 = pos2;
            return this;
        }

        /**
         * The alpha is read out of this colour rather than being left alone, so a
         * colour written with a zero alpha byte comes out opaque unless the alpha was
         * set separately afterwards.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().lineBuilder();
         * // half a red line, with the alpha coming out of the colour
         * builder.color(0x80FF0000);
         * Chat.log(`colour 0x${builder.getColor().toString(16)}, alpha ${builder.getAlpha()}`);
         * </pre>
         *
         * @param color the color of the line
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
         * given and whatever alpha it carried is not what the built line is drawn at.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().lineBuilder();
         * builder.color(0xFF0000, 128);
         * Chat.log(`colour 0x${builder.getColor().toString(16)}, alpha ${builder.getAlpha()}`);
         * </pre>
         *
         * @param color the color of the line
         * @param alpha the alpha value of the line's color
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
         * const builder = Hud.createDraw3D().lineBuilder();
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
         * a builder with nothing set builds a fully opaque white line.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().lineBuilder();
         * builder.color(0, 128, 255, 255);
         * Chat.log(`colour 0x${builder.getColor().toString(16)}, alpha ${builder.getAlpha()}`);
         * </pre>
         *
         * @param r the red component of the color
         * @param g the green component of the color
         * @param b the blue component of the color
         * @param a the alpha value of the color
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder color(int r, int g, int b, int a) {
            this.color = (r << 16) | (g << 8) | b;
            this.alpha = a;
            return this;
        }

        /**
         * The builder's own field rather than a copy of the built line's colour, and
         * unlike a built line's {@code getColor()} this does not mask the alpha out.
         * The two therefore read back differently for the same line.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().lineBuilder();
         * builder.color(0x80FF0000);
         * const line = builder.build();
         * // the builder keeps the alpha byte, the built line does not
         * Chat.log(`builder 0x${builder.getColor().toString(16)}, line 0x${line.getColor().toString(16)}`);
         * </pre>
         *
         * @return the color of the line.
         * @since 1.8.4
         */
        public int getColor() {
            return color;
        }

        /**
         * From 0 to 255, and independent of the colour: setting this does not touch
         * the colour and setting the colour afterwards does not touch this.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().lineBuilder();
         * builder.alpha(64);
         * Chat.log(`drawn at alpha ${builder.getAlpha()}`);
         * </pre>
         *
         * @param alpha the alpha value for the line's color
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder alpha(int alpha) {
            this.alpha = alpha;
            return this;
        }

        /**
         * From 0 to 255, and {@code 255} until a colour or alpha call changes it. This
         * is the builder's value; a built line reports its own through
         * {@link Line3D#getAlpha()}.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().lineBuilder();
         * builder.color(0x80FF0000);
         * // the alpha came out of the colour, so this is 128
         * Chat.log(`alpha ${builder.getAlpha()}`);
         * </pre>
         *
         * @return the alpha value of the line's color.
         * @since 1.8.4
         */
        public int getAlpha() {
            return alpha;
        }

        /**
         * {@code true} is the depth tested line that terrain hides, and
         * {@code false} is the one drawn over everything. The default here is
         * {@code false}, so a line from this builder is on top unless asked otherwise.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * // a line something in front of it can hide
         * draw.lineBuilder().pos(0, 64, 0, 8, 72, 0).cull(true).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param cull whether to cull the line or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder cull(boolean cull) {
            this.cull = cull;
            return this;
        }

        /**
         * That is, {@code true} when the line is depth tested and terrain can hide it.
         * It is the inverse of what a built line calls
         * {@link Line3D#isAlwaysOnTop()}.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().lineBuilder();
         * // the default is not culled, so the line is drawn over everything
         * Chat.log(`culled: ${builder.isCulled()}`);
         * </pre>
         *
         * @return {@code true} if the line should be culled, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isCulled() {
            return cull;
        }

        /**
         * Creates the line for the given values and adds it to the draw3D.
         * <p>
         * The line goes into the same list any other 3D line does, so
         * {@code Draw3D.getLines()} reports it and {@code removeLine} takes it off
         * again. It draws nothing until the overlay itself is registered.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.lineBuilder().pos(0, 64, 0, 8, 72, 0).color(0xFFFFFFFF).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @return the build line.
         * @since 1.8.4
         */
        public Line3D buildAndAdd() {
            Line3D line = build();
            parent.addLine(line);
            return line;
        }

        /**
         * Builds the line from the given values.
         * <p>
         * The line is not put anywhere, so it draws nothing until it is added to an
         * overlay. It is a copy of everything collected so far, so the builder can be
         * changed and used again afterwards.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * const builder = draw.lineBuilder().color(0xFFFF0000);
         * builder.pos(0, 64, 0, 8, 72, 0);
         * const first = builder.build();
         * // the same builder, a second line somewhere else
         * builder.pos(8, 72, 0, 16, 80, 0);
         * const second = builder.build();
         * draw.addLine(first);
         * draw.addLine(second);
         * draw.register();
         * </pre>
         *
         * @return the build line.
         */
        public Line3D build() {
            return new Line3D(pos1.x, pos1.y, pos1.z, pos2.x, pos2.y, pos2.z, color, alpha, cull);
        }

    }

}
