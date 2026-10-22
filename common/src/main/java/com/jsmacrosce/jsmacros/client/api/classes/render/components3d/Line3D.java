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
 * A fixed line segment between two world-space points.
 * <p>
 * Coordinates are in blocks. {@link #getColor()} returns RGB without alpha; use
 * {@link #getAlpha()} to read the alpha separately. {@code cull} controls depth
 * testing: {@code true} lets geometry obscure the line, while {@code false} draws it
 * on top.
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
     * The line's endpoints as a segment, in the order supplied. Writable directly or
     * through {@link #setPos(double, double, double, double, double, double)}.
     *
     * @since 1.0.6
     */
    public Vec3D pos;
    /**
     * The line colour packed as ARGB, with alpha in the top byte. Writable; see
     * {@link #setColor(int)} for alpha handling.
     *
     * @since 1.0.6
     */
    public int color;
    /**
     * Whether the line is depth tested; {@code true} allows geometry to obscure it.
     *
     * @since 1.0.6
     */
    public boolean cull;

    /**
     * Initializes a detached line segment with ordered endpoints in block coordinates.
     * A packed color with zero alpha becomes opaque when its RGB bits are nonzero;
     * zero-alpha black remains transparent.
     *
     * @param x1 x coordinate of the first endpoint, in blocks
     * @param y1 y coordinate of the first endpoint, in blocks
     * @param z1 z coordinate of the first endpoint, in blocks
     * @param x2 x coordinate of the second endpoint, in blocks
     * @param y2 y coordinate of the second endpoint, in blocks
     * @param z2 z coordinate of the second endpoint, in blocks
     * @param color packed ARGB color; a zero alpha byte is fixed as described above
     * @param cull whether depth testing is enabled
     */
    public Line3D(double x1, double y1, double z1, double x2, double y2, double z2, int color, boolean cull) {
        setPos(x1, y1, z1, x2, y2, z2);
        setColor(color);
        this.cull = cull;
    }

    /**
     * Initializes a detached line segment with ordered endpoints in block coordinates.
     * Alpha is packed without clamping and replaces any alpha bits in {@code color}.
     *
     * @param x1 x coordinate of the first endpoint, in blocks
     * @param y1 y coordinate of the first endpoint, in blocks
     * @param z1 z coordinate of the first endpoint, in blocks
     * @param x2 x coordinate of the second endpoint, in blocks
     * @param y2 y coordinate of the second endpoint, in blocks
     * @param z2 z coordinate of the second endpoint, in blocks
     * @param color RGB source; only its low 24 bits are used
     * @param alpha value shifted into the top byte, without range validation
     * @param cull whether depth testing is enabled
     */
    public Line3D(double x1, double y1, double z1, double x2, double y2, double z2, int color, int alpha, boolean cull) {
        setPos(x1, y1, z1, x2, y2, z2);
        setColor(color, alpha);
        this.cull = cull;
    }

    /**
     * Sets the two endpoints of the segment, in block coordinates. The points are
     * ordered endpoints, not minimum and maximum bounds.
     *
     * @param x1 x coordinate of the first endpoint, in blocks
     * @param y1 y coordinate of the first endpoint, in blocks
     * @param z1 z coordinate of the first endpoint, in blocks
     * @param x2 x coordinate of the second endpoint, in blocks
     * @param y2 y coordinate of the second endpoint, in blocks
     * @param z2 z coordinate of the second endpoint, in blocks
     * @since 1.0.6
     */
    public void setPos(double x1, double y1, double z1, double x2, double y2, double z2) {
        pos = new Vec3D(x1, y1, z1, x2, y2, z2);
    }

    /**
     * Returns a copy of the line's endpoint coordinates.
     *
     * @return the positions of the line.
     * @since 2.0.0
     */
    public Vec3D getPos() {
        return new Vec3D(pos);
    }

    /**
     * Returns a new position for the first endpoint; modifying it does not move the line.
     *
     * @return the first position of the line.
     * @since 2.0.0
     */
    public Pos3D getPos1() {
        return new Pos3D(pos.x1, pos.y1, pos.z1);
    }

    /**
     * Returns a new position for the second endpoint; modifying it does not move the line.
     *
     * @return the second position of the line.
     * @since 2.0.0
     */
    public Pos3D getPos2() {
        return new Pos3D(pos.x2, pos.y2, pos.z2);
    }

    /**
     * Sets the packed colour, making a non-black RGB colour opaque when its alpha byte
     * is zero. Use {@link #setColor(int, int)} to explicitly set alpha to zero.
     *
     * @param color packed line colour
     * @since 1.0.6
     */
    public void setColor(int color) {
        this.color = ColorUtil.fixAlpha(color);
    }

    /**
     * Sets RGB from the low 24 bits of {@code color} and alpha in the top byte; alpha
     * is not clamped.
     *
     * @param color RGB colour; its alpha byte is ignored
     * @param alpha alpha value, packed into the top byte
     * @since 1.1.8
     */
    public void setColor(int color, int alpha) {
        this.color = (alpha << 24) | (color & 0xFFFFFF);
    }

    /**
     * Returns the line colour's low 24 bits; read alpha separately with
     * {@link #getAlpha()}.
     *
     * @return the RGB colour, without alpha.
     * @since 2.0.0
     */
    public int getColor() {
        return color & 0xFFFFFF;
    }

    /**
     * Replaces the top byte of the packed colour without changing its RGB bits.
     *
     * @param alpha value packed into the top byte; values are not clamped to 0–255
     * @since 1.1.8
     */
    public void setAlpha(int alpha) {
        this.color = (alpha << 24) | (color & 0xFFFFFF);
    }

    /**
     * Returns the packed colour's top byte.
     *
     * @return the alpha byte, from 0 to 255.
     * @since 2.0.0
     */
    public int getAlpha() {
        return (color >> 24) & 0xFF;
    }

    /**
     * Sets {@code cull} to the inverse of {@code alwaysOnTop}; this replaces its current value.
     *
     * @param alwaysOnTop whether to disable depth testing for the line
     * @since 2.0.0
     */
    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.cull = !alwaysOnTop;
    }

    /**
     * Returns the inverse of the line's {@code cull} setting.
     *
     * @return {@code true} when depth testing is disabled.
     * @since 2.0.0
     */
    public boolean isAlwaysOnTop() {
        return !cull;
    }

    /**
     * Tests equality by exact runtime class and endpoint value; color and depth testing
     * are ignored. Endpoint order matters, and a {@code null} position compares equal
     * only to another {@code null} position.
     *
     * @param o object to compare with
     * @return {@code true} for this object or an instance of the exact same runtime
     *         class with an equal position; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Line3D line3D = (Line3D) o;
        return Objects.equals(pos, line3D.pos);
    }

    /**
     * Computes a hash from the current endpoint value only; changing the position can
     * change this result.
     *
     * @return hash derived from the current position; it is not stable across position changes
     */
    @Override
    public int hashCode() {
        return Objects.hash(pos);
    }

    /**
     * Orders lines by their endpoint coordinates, in endpoint order; colour and culling do not affect the comparison. Reversing a segment's endpoints can change its order.
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
     * Configures a line before building it. Defaults are coincident endpoints at the origin, opaque white, and no depth testing. {@link #build()} returns a detached line; {@link #buildAndAdd()} also adds it to the parent overlay.
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

        /**
         * Creates a builder for a line with coincident origin endpoints, opaque white
         * color, and depth testing disabled. Building alone is detached; the parent is
         * used only by {@code buildAndAdd()}, which fails with {@link NullPointerException}
         * if it is null.
         *
         * @param parent overlay used by {@code buildAndAdd()}; may be null when only
         *               building detached lines
         */
        public Builder(Draw3D parent) {
            this.parent = parent;
        }

        /**
         * Sets the first endpoint by reference; the builder retains this position object.
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
         * Sets the first endpoint to the block coordinates, with no half-block offset.
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
         * Returns the first endpoint position held by this builder.
         *
         * @return the first endpoint position.
         * @since 1.8.4
         */
        public Pos3D getPos1() {
            return pos1;
        }

        /**
         * Sets the second endpoint by reference.
         *
         * @param pos2 the second endpoint
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos2(Pos3D pos2) {
            this.pos2 = pos2;
            return this;
        }

        /**
         * Sets the second endpoint from the block position's coordinates.
         *
         * @param pos2 the second endpoint block position
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos2(BlockPosHelper pos2) {
            this.pos2 = pos2.toPos3D();
            return this;
        }

        /**
         * Sets the second endpoint from integer block coordinates.
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
         * Returns the second endpoint position held by this builder.
         *
         * @return the second position of the line.
         * @since 1.8.4
         */
        public Pos3D getPos2() {
            return pos2;
        }

        /**
         * Sets the ordered endpoints from integer block coordinates; these coordinates
         * need not be in minimum-to-maximum order.
         *
         * @param x1 the x coordinate of the first position of the line
         * @param y1 the y coordinate of the first position of the line
         * @param z1 the z coordinate of the first position of the line
         * @param x2 the x coordinate of the second position of the line
         * @param y2 the y coordinate of the second position of the line
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
         * Sets each endpoint to the corresponding block position's coordinates (the
         * block corner, with no half-block offset).
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
         * Sets the ordered endpoints by reference; the builder and built line share
         * both position objects. The endpoints need not be in minimum-to-maximum order.
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
         * Sets the colour and derives the builder's alpha from its top byte, applying
         * {@link ColorUtil#fixAlpha(int)} first.
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
         * Sets the packed colour and alpha independently; the colour is stored as given.
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
         * Packs the components into RGB with shifts and bitwise OR; values are not
         * clamped or masked. This overload leaves the builder's alpha unchanged.
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
         * Packs RGB with shifts and bitwise OR and stores alpha separately; components
         * are not clamped or masked. An untouched builder defaults to opaque white.
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
         * Returns the packed value supplied to the builder; unlike the built line's
         * {@link Line3D#getColor()}, this value is not masked to RGB.
         *
         * @return the color of the line.
         * @since 1.8.4
         */
        public int getColor() {
            return color;
        }

        /**
         * Sets alpha independently of the stored color. Values are stored without
         * clamping; a later color-setting call may replace this value.
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
         * Returns the builder's alpha, initially {@code 255}; color-setting or
         * {@link #alpha(int)} calls can change it.
         *
         * @return the alpha value of the line's color.
         * @since 1.8.4
         */
        public int getAlpha() {
            return alpha;
        }

        /**
         * Sets whether the line is depth tested ({@code true}) or drawn over geometry
         * ({@code false}); the default is {@code false}.
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
         * Returns whether the line is depth tested; this is the inverse of
         * {@link Line3D#isAlwaysOnTop()} on the built line.
         *
         * @return {@code true} if the line should be culled, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isCulled() {
            return cull;
        }

        /**
         * Builds a line and adds it to the parent {@link Draw3D} overlay.
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
         * Builds a detached line from the current values; the builder remains reusable.
         *
         * @return the build line.
         */
        public Line3D build() {
            return new Line3D(pos1.x, pos1.y, pos1.z, pos2.x, pos2.y, pos2.z, color, alpha, cull);
        }

    }

}
