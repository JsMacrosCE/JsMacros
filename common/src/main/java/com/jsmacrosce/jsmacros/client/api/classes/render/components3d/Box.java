package com.jsmacrosce.jsmacros.client.api.classes.render.components3d;
import com.jsmacrosce.jsmacros.client.api.classes.render.RenderSink;

import com.jsmacrosce.doclet.DocletCategory;

import com.mojang.blaze3d.vertex.PoseStack;
//? if <1.21.11 {
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ShapeRenderer;
//? }
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.api.math.Vec3D;
import com.jsmacrosce.jsmacros.client.api.classes.render.Draw3D;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.util.ColorMath;

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
 * An axis-aligned cuboid with an outline and an optional filled interior. Its position
 * is defined by two opposite corners, and its outline and fill have independent colours.
 * Setters taking a packed colour make a zero alpha byte opaque unless the whole value
 * is zero; overloads taking a separate alpha preserve zero alpha.
 * <p>
 * {@code cull} enables depth testing, allowing terrain to hide the box; otherwise it is
 * drawn over the world. Builders and {@code Draw3D} helpers default it to false.
 * <pre>
 * const draw = Hud.createDraw3D();
 * // a filled red cube one block on a side, drawn over the world
 * draw.addBox(0, 64, 0, 1, 65, 1, Color.rgb(0xFF, 0x00, 0x00), Color.argb(0x88, 0, 0, 0), true);
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
     * The two opposite corners defining this box. Their supplied order is retained.
     *
     * @since 1.0.6
     */
    public Vec3D pos;
    /**
     * The outline colour, packed with alpha in the top byte. The outline is drawn
     * whether or not {@link #fill} is set.
     *
     * @since 1.0.6
     */
    public int color;
    /**
     * The fill colour, packed the same way as {@link #color}; used only when
     * {@link #fill} is {@code true}.
     *
     * @since 1.0.6
     */
    public int fillColor;
    /**
     * Whether to draw the inside as well as the outline.
     *
     * @since 1.0.6
     */
    public boolean fill;
    /**
     * Whether this box is depth tested, allowing terrain to hide it; false draws it
     * over the world.
     *
     * @since 1.0.6
     */
    public boolean cull;

    /**
     * Constructs a detached box from two ordered opposite corners in world-space block
     * coordinates and packed outline and fill colours. For each colour, a nonzero RGB value
     * with a zero alpha byte becomes opaque, while zero remains transparent.
     * This does not add or register the box with a {@link Draw3D} overlay.
     *
     * @param x1 x coordinate of the first corner
     * @param y1 y coordinate of the first corner
     * @param z1 z coordinate of the first corner
     * @param x2 x coordinate of the opposite corner
     * @param y2 y coordinate of the opposite corner
     * @param z2 z coordinate of the opposite corner
     * @param color packed outline colour
     * @param fillColor packed fill colour
     * @param fill whether to draw the interior
     * @param cull whether to enable the renderer's depth-testing setting
     */
    public Box(double x1, double y1, double z1, double x2, double y2, double z2, int color, int fillColor, boolean fill, boolean cull) {
        setPos(x1, y1, z1, x2, y2, z2);
        setColor(color);
        setFillColor(fillColor);
        this.fill = fill;
        this.cull = cull;
    }

    /**
     * Constructs a detached box from two ordered opposite corners in world-space block
     * coordinates, with outline and fill alpha supplied separately. Each colour's
     * existing alpha byte is ignored;
     * its low 24 bits are combined with the corresponding alpha using a left shift,
     * without range validation. This does not add or register the box with an overlay.
     *
     * @param x1 x coordinate of the first corner
     * @param y1 y coordinate of the first corner
     * @param z1 z coordinate of the first corner
     * @param x2 x coordinate of the opposite corner
     * @param y2 y coordinate of the opposite corner
     * @param z2 z coordinate of the opposite corner
     * @param color outline RGB value; its top byte is ignored
     * @param alpha outline alpha, shifted into the top byte without range validation
     * @param fillColor fill RGB value; its top byte is ignored
     * @param fillAlpha fill alpha, shifted into the top byte without range validation
     * @param fill whether to draw the interior
     * @param cull whether to enable the renderer's depth-testing setting
     */
    public Box(double x1, double y1, double z1, double x2, double y2, double z2, int color, int alpha, int fillColor, int fillAlpha, boolean fill, boolean cull) {
        setPos(x1, y1, z1, x2, y2, z2);
        setColor(color, alpha);
        setFillColor(fillColor, fillAlpha);
        this.fill = fill;
        this.cull = cull;
    }

    /**
     * Replaces {@link #pos} with the supplied opposite corners, retaining their order.
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
     * Sets this box to cover the block at {@code pos}, extending one block from its
     * coordinates along each axis.
     *
     * @param pos the block to cover
     * @since 1.9.0
     */
    public void setPosToBlock(BlockPosHelper pos) {
        setPosToBlock(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Sets this box to cover the block at the given coordinates, extending one block
     * from that corner along each axis.
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
     * Sets this box to a cube centred on {@code pos}, with side length twice
     * {@code radius}.
     *
     * @param pos the centre of the cube
     * @param radius half the side length, in blocks
     * @since 1.9.0
     */
    public void setPosToPoint(Pos3D pos, double radius) {
        setPosToPoint(pos.getX(), pos.getY(), pos.getZ(), radius);
    }

    /**
     * Sets this box to a cube centred on the given point, with side length twice
     * {@code radius}. A negative radius reverses the supplied corner order.
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
     * Replaces the outline colour. If its alpha byte is zero but its RGB value is
     * nonzero, the alpha is made opaque; transparent black remains transparent.
     * Use {@link #setColor(int, int)} to specify zero alpha explicitly.
     *
     * @param color the colour of the outline
     * @since 1.0.6
     */
    public void setColor(int color) {
        this.color = ColorMath.fixAlpha(color);
    }

    /**
     * Replaces the fill colour using the same zero-alpha handling as
     * {@link #setColor(int)}. The colour is stored even while filling is disabled.
     *
     * @param fillColor the colour of the inside
     * @since 1.0.6
     */
    public void setFillColor(int fillColor) {
        this.fillColor = ColorMath.fixAlpha(fillColor);
    }

    /**
     * Sets the outline RGB and alpha explicitly. The alpha replaces the top byte;
     * zero remains transparent.
     *
     * @param color the colour of the outline
     * @param alpha the alpha of the outline, from 0 to 255
     * @since 1.1.8
     */
    public void setColor(int color, int alpha) {
        this.color = (alpha << 24) | (color & 0xFFFFFF);
    }

    /**
     * Replaces the outline alpha without changing its RGB or the fill colour.
     *
     * @param alpha the alpha of the outline, from 0 to 255
     * @since 1.1.8
     */
    public void setAlpha(int alpha) {
        this.color = (alpha << 24) | (color & 0xFFFFFF);
    }

    /**
     * Sets the fill RGB and alpha explicitly, preserving zero alpha. It is visible
     * only when {@link #fill} is true.
     *
     * @param fillColor the colour of the inside
     * @param alpha the alpha of the inside, from 0 to 255
     * @since 1.1.8
     */
    public void setFillColor(int fillColor, int alpha) {
        this.fillColor = (fillColor & 0xFFFFFF) | (alpha << 24);
    }

    /**
     * Replaces the fill alpha without changing its RGB. It is visible only when
     * {@link #fill} is true.
     *
     * @param alpha the alpha of the inside, from 0 to 255
     * @since 1.1.8
     */
    public void setFillAlpha(int alpha) {
        this.fillColor = (fillColor & 0xFFFFFF) | (alpha << 24);
    }

    /**
     * Controls whether the interior is drawn; disabling it leaves the outline
     * unchanged.
     *
     * @param fill whether to draw the inside as well as the outline
     * @since 1.0.6
     */
    public void setFill(boolean fill) {
        this.fill = fill;
    }

    /**
     * Tests equality by exact runtime class and current position value; colour, fill,
     * and depth-testing settings are ignored. A null argument or a different class
     * does not match; null positions compare equal to null positions.
     * @param o object to compare
     * @return {@code true} if the objects match by this rule
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Box box = (Box) o;
        return Objects.equals(pos, box.pos);
    }

    /**
     * Computes a hash from the current position only. Since position is mutable, the
     * hash may change when it changes; colour, fill, and depth-testing settings are ignored.
     * @return hash of the current position
     */
    @Override
    public int hashCode() {
        return Objects.hash(pos);
    }

    /**
     * Compares this box's corner coordinates lexicographically with another box's.
     * The comparison is not based on distance or overlap; colour, fill, and culling
     * do not affect the result.
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
    public void render(PoseStack poseStack, RenderSink sink, float tickDelta) {
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

        try {
            if (seeThrough) {
                // Flush anything already queued first, otherwise this box's endBatch
                // would flush an earlier depth-tested box with NO_DEPTH_TEST too.
                sink.endBatch();
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
                ShapeRenderer.addChainedFilledBoxVertices(poseStack, sink.getBuffer(fillLayer), x1, y1, z1, x2, y2, z2, fr, fg, fb, fa);
            }

            float r = ((color >> 16) & 0xFF) / 255.0F;
            float g = ((color >> 8) & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            float a = ((color >> 24) & 0xFF) / 255.0F;
            ShapeRenderer.renderLineBox(
                    //? if >1.21.8 {
                    /*poseStack.last(),
                    *///?} else
                    poseStack,
                    sink.getBuffer(linesLayer),
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
                sink.endBatch();
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
     * Configures a {@link Box} before building it. {@link #build()} returns the box
     * without adding it; {@link #buildAndAdd()} adds it to the originating overlay.
     * The default is a zero-size box at the origin with opaque white outline and fill
     * colours, no fill, and no depth testing.
     * <p>
     * Use {@link #forBlock(int, int, int)} for a one-block box or the corner setters
     * to specify arbitrary bounds.
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.boxBuilder(0, 64, 0)
     *    .color(Color.rgb(0xFF, 0x00, 0x00))
     *    .fillColor(Color.rgb(0x00, 0xFF, 0x00), 128)
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

        /**
         * Creates a builder with coincident origin corners, opaque white outline and
         * fill, filling disabled, and depth testing disabled. Construction does not
         * build or add a box. A null parent is acceptable for {@link #build()}, but
         * {@link #buildAndAdd()} throws {@link NullPointerException} when it tries to
         * add to that parent.
         *
         * @param parent overlay used by {@link #buildAndAdd()}; may be null when only
         *               building detached boxes
         */
        public Builder(Draw3D parent) {
            this.parent = parent;
        }

        /**
         * Sets the first of the box's opposite corners. The position is stored by
         * reference, so mutating it affects boxes built later, not boxes already built.
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
         * Sets the first corner from the block's integer coordinates; a second corner
         * is still required to define the box.
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
         * Sets the first corner; use {@link #forBlock(int, int, int)} to set both
         * corners for a single block.
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
         * Returns the builder's first-corner object; mutating it affects boxes built later.
         *
         * @return the first position of the box.
         * @since 1.8.4
         */
        public Pos3D getPos1() {
            return pos1;
        }

        /**
         * Sets the corner opposite {@link #pos1(double, double, double)}. The position
         * is stored by reference, so mutating it affects boxes built later, not boxes already built.
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
         * Sets the opposite corner from the block's integer coordinates; unlike
         * {@link #pos2(double, double, double)}, this converts the helper to a position
         * object before storing it.
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
         * Sets the opposite corner; together with the first corner it determines the
         * box, and either corner may be first.
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
         * Returns the builder's second-corner object; mutating it affects boxes built later.
         *
         * @return the second position of the box.
         * @since 1.8.4
         */
        public Pos3D getPos2() {
            return pos2;
        }

        /**
         * Sets both opposite corners, creating new position objects rather than
         * retaining references supplied by the caller.
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
         * Sets both corners from the block positions' integer coordinates, converting
         * each helper to a position object.
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
         * Stores both position objects by reference in the builder. Mutating either
         * affects a later build; an already-built box copies their coordinate values.
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
         * Sets both corners to cover the block at these coordinates, from its corner
         * to one block further along each axis. This replaces any previously set corners.
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
         * Sets both corners to cover the block named by this position, from its integer
         * coordinates to one block further along each axis; the position's offset is
         * not used.
         * <pre>
         * const player = Player.getPlayer();
         * if (player !== null) {
         *   const draw = Hud.createDraw3D();
         *   draw.boxBuilder().forBlock(player.getBlockPos()).color(Color.rgb(0xFF, 0x00, 0x00)).buildAndAdd();
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
         * Sets the outline colour and derives its alpha from the packed value, making
         * a zero alpha byte opaque for non-black colours.
         *
         * @param color the color of the box
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder color(int color) {
            this.color = color;
            this.alpha = ColorMath.fixAlpha(color) >>> 24;
            return this;
        }

        /**
         * Sets the outline colour and alpha separately; the colour is stored unchanged,
         * including any alpha byte, which is ignored when building the box's outline.
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
         * Packs the components with shifts and bitwise OR, without clamping or masking;
         * leaves the separately stored outline alpha unchanged.
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
         * Packs RGB as in {@link #color(int, int, int)} and stores alpha separately.
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
         * Returns the stored outline colour, which may retain an alpha byte even though
         * the built box uses the separately stored alpha.
         *
         * @return the color of the box.
         * @since 1.8.4
         */
        public int getColor() {
            return color;
        }

        /**
         * Sets the outline alpha independently of its colour; a later colour setter
         * that accepts alpha may replace it. The fill alpha is separate.
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
         * Returns the separately stored outline alpha; it is independent of the fill alpha.
         *
         * @return the alpha value of the box's color.
         * @since 1.8.4
         */
        public int getAlpha() {
            return alpha;
        }

        /**
         * Stores the colour and sets the separate fill alpha from its alpha byte;
         * a non-black colour with zero alpha is made opaque.
         *
         * @param fillColor the fill color of the box
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder fillColor(int fillColor) {
            this.fillColor = fillColor;
            this.fillAlpha = ColorMath.fixAlpha(fillColor) >>> 24;
            return this;
        }

        /**
         * Stores the colour and alpha separately from the outline; the colour is kept
         * exactly as given, including any alpha byte in it.
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
         * Packs the RGB components without clamping or masking them; this does not
         * change the separately stored fill alpha or the outline.
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
         * Packs RGB without clamping or masking and stores alpha separately from the
         * outline.
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
         * Returns the stored fill colour, which may include an alpha byte; the built
         * box combines this value with the separately stored fill alpha.
         *
         * @return the fill color of the box.
         * @since 1.8.4
         */
        public int getFillColor() {
            return fillColor;
        }

        /**
         * Sets the separate fill alpha without changing the fill colour or outline.
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
         * Returns the separately stored fill alpha, initially {@code 255}; outline
         * alpha changes do not affect it.
         *
         * @return the alpha value of the box's fill color.
         * @since 1.8.4
         */
        public int getFillAlpha() {
            return fillAlpha;
        }

        /**
         * Controls whether the inside is drawn; it defaults to {@code false}.
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
         * Returns whether the builder enables drawing the box's inside.
         *
         * @return {@code true} if the box should be filled, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isFilled() {
            return fill;
        }

        /**
         * Enables depth testing, allowing terrain to hide the box; defaults to
         * {@code false}, which draws it over the world.
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
         * Returns whether this builder enables depth testing, allowing terrain to hide
         * the box. The default is {@code false}.
         *
         * @return {@code true} if culling is enabled for this box, {@code false} otherwise.
         * @since 1.8.4
         */
        public boolean isCulled() {
            return cull;
        }

        /**
         * Builds the box and adds it to the parent overlay.
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
         * Builds a new box from the current builder values without adding it to an
         * overlay. Later builder changes do not modify the returned box.
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
