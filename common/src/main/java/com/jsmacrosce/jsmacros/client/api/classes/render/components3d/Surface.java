package com.jsmacrosce.jsmacros.client.api.classes.render.components3d;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.api.math.Pos2D;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.api.classes.render.Draw2D;
import com.jsmacrosce.jsmacros.client.api.classes.render.Draw3D;
import com.jsmacrosce.jsmacros.client.api.classes.render.components.Draw2DElement;
import com.jsmacrosce.jsmacros.client.api.classes.render.components.RenderElement;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.util.CameraCompat;

import java.util.Iterator;
import java.util.Objects;

//? if >=26.1 {
/*import net.minecraft.util.LightCoordsUtil;
*///? } else {
import net.minecraft.client.renderer.LightTexture;
//?}

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * a flat panel in the world that 2D elements are drawn onto.
 * <p>
 * This is a 2D overlay living in three dimensions. It is a {@link Draw2D} in its own
 * right, so everything a 2D overlay can hold, {@code addText}, {@code addRect},
 * {@code addLine}, {@code addImage}, {@code addItem} and the rest, works on it
 * unchanged, and the same builders do too. The difference is where the result appears:
 * a surface is placed at a block position in the world, rotated to face a direction,
 * and drawn as a flat surface there rather than flat on the screen. The coordinates a
 * script gives its elements are the surface's own pixels, measured from its top left
 * with y growing downwards, and how big a pixel is in blocks is decided by the size
 * and the subdivision count rather than by the element.
 * <p>
 * A surface is reached through {@code Draw3D}, either with {@code addDraw2D} for the
 * quick form or through {@link Draw3D#surfaceBuilder()} for the full one. A surface on
 * its own draws nothing; it is the 3D overlay holding it that has to be registered.
 * <p>
 * Three settings decide whether it can be seen. {@code cull} makes it depth tested, so
 * terrain draws over it; leaving it off draws the surface over the world instead.
 * {@code renderBack} decides whether the back is drawn at all, and with it off the
 * surface disappears when the camera is behind it. And {@code rotateToPlayer} turns
 * the surface to face the camera every frame, which overwrites whatever rotations
 * were set.
 * <p>
 * Lighting is a fourth. By default everything on a surface is drawn at full
 * brightness, ignoring the world. {@link #setWorldLight()} samples the world instead,
 * once per frame at the surface's own position, and {@link #setLight(int, int)} fixes
 * a level. None of them model a cast shadow.
 * <p>
 * The size and the subdivision count work together rather than independently. The
 * size is in blocks and the subdivision count says how many of those the smaller side
 * is divided into, and one subdivision is the unit the elements are measured in. So a
 * surface {@code 1} block on a side with a subdivision count of {@code 200} is
 * {@code 200} by {@code 200} surface pixels, and the same surface at
 * {@code minSubdivisions} of {@code 1} is a single pixel. {@link #getWidth()} and
 * {@link #getHeight()} report those subdivision counts, not the block size.
 * <p>
 * The two ways of making a surface do not start from the same place.
 * {@link Draw3D#surfaceBuilder()} leaves the count at {@code 1}, so a builder with
 * nothing set on it gives a single pixel across its smaller side, while the short
 * {@code Draw3D.addDraw2D} forms that take no subdivision count pass {@code 200}.
 * example:
 * <pre>
 * const draw = Hud.createDraw3D();
 * // a one block panel with room for a label, drawn over the world
 * const surface = draw.surfaceBuilder()
 *    .pos(0.5, 64, 0.5)
 *    .size(1, 1)
 *    .minSubdivisions(200)
 *    .cull(false)
 *    .renderBack(true)
 *    .buildAndAdd();
 * surface.addText("hello", 10, 10, 0xFFFFFFFF, true);
 * draw.register();
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.6.5
 */
@DocletCategory("Rendering/Graphics")
@SuppressWarnings("unused")
public class Surface extends Draw2D implements RenderElement, RenderElement3D<Surface> {
    /**
     * whether this surface is turned to face the camera every frame.
     * <p>
     * On, the rotations are worked out from where the camera is each frame and
     * overwrite anything set through {@link #setRotations(double, double, double)},
     * so a surface set up this way cannot be given a fixed orientation. The pivot it
     * turns about depends on {@code rotateCenter}.
     *
     * @since 1.6.5
     */
    public boolean rotateToPlayer;
    /**
     * whether this surface turns about its middle rather than its top left corner.
     * <p>
     * This applies both to the rotations set on it and to the facing-the-camera
     * rotation, and to nested 2D overlays drawn onto it. It is {@code false} unless
     * something turns it on, which is not the same as the builder's default.
     *
     * @since 1.6.5
     */
    public boolean rotateCenter;
    /**
     * the entity this surface follows, or {@code null} if it is not bound to one.
     * <p>
     * While the entity is alive the surface is drawn at the entity's interpolated
     * position plus {@link #boundOffset}, and stops following the moment the entity
     * is not alive. Nothing is checked about the entity beyond that, so a surface
     * bound to a dead entity sits at its own position rather than disappearing.
     *
     * @since 1.6.5
     */
    @Nullable
    public EntityHelper<?> boundEntity;
    /**
     * where on the bound entity this surface is drawn, in blocks from the entity's
     * interpolated position. It is added every frame, so it follows the entity rather
     * than being applied once. It is ignored when there is no bound entity.
     * <p>
     * Public and writable, so it can be changed on a surface that is already in an
     * overlay and take effect on the next frame.
     *
     * @since 1.6.5
     */
    public Pos3D boundOffset = Pos3D.ZERO;
    /**
     * where this surface sits in the world, in blocks.
     * <p>
     * Public and writable, and the object itself is final, so the position is moved by
     * writing to its coordinates rather than by replacing it. A surface is anchored
     * here and moves with the player rather than staying put, because the overlay
     * holding it is drawn relative to the camera.
     * <p>
     * This is the panel's top left corner, not its middle, unless
     * {@link #rotateCenter} is on. With y running upwards in the world, the top left
     * corner is the one with the higher y of the two vertical extremes.
     *
     * @since 1.6.5
     */
    public final Pos3D pos;
    /**
     * how this surface is turned, in degrees on each axis.
     * <p>
     * Public and writable, and the object itself is final. The order the rotations are
     * applied in is y then x then z, so the y rotation is the one that swings the panel
     * around to face a compass direction and the other two tilt it. While
     * {@link #rotateToPlayer} is on these are overwritten every frame and reading them
     * back reports the facing the camera got rather than what was set.
     *
     * @since 1.6.5
     */
    public final Pos3D rotations;
    /**
     * how big this surface is, in blocks, as a width and a height.
     * <p>
     * The pixels the elements are measured in are subdivisions of this rather than
     * blocks, so this is the physical size of the panel and not how much room its
     * elements have. Read it with {@link #getSizes()}, which hands back a copy.
     *
     * @since 1.6.5
     */
    protected final Pos2D sizes;
    /**
     * how many subdivisions the smaller side of this surface is divided into.
     * <p>
     * This is the number of surface pixels across the smaller side, and it is never
     * below 1: a surface set to 0 or below divides into one pixel rather than none.
     * The larger side then gets however many that works out to.
     *
     * @since 1.6.5
     */
    protected int minSubdivisions;

    /**
     * how many blocks one surface pixel is, worked out from the size and the
     * subdivision count. This is not the panel's own scale in any other sense; it is
     * the conversion between the two, and it is what {@link #getWidth()} divides by.
     *
     * @since 1.6.5
     */
    protected double scale;
    /**
     * scale that zIndex is multiplied by to get the actual offset (in blocks) for rendering
     * default: {@code 1/1000} if there is still z-fighting, increase this value
     *
     * @since 1.6.5
     */
    public double zIndexScale = 0.001;
    /**
     * whether the back of this surface is drawn as well as its front.
     * <p>
     * Public and writable. With this off, the surface draws nothing at all while the
     * camera is on its back side, which is what a surface that should only be readable
     * from the front wants. The test is against the surface's own plane and the camera
     * position, so it flips as the player walks past the panel rather than at some
     * fixed distance.
     *
     * @since 1.6.5
     */
    public boolean renderBack;
    /**
     * whether this surface is depth tested, so that terrain draws over it.
     * <p>
     * {@code true} is the surface that terrain hides and {@code false} is the one
     * drawn over the world. This also decides which of the two passes the surface is
     * drawn in, and so how its own back-to-front order against other surfaces works
     * out.
     * <p>
     * Public and writable, so it can be changed on a surface that is already in an
     * overlay and take effect on the next frame.
     *
     * @since 1.6.5
     */
    public boolean cull;

    /**
     * How the surface's elements are lit.
     *
     * @since 2.0.0
     */
    private enum LightMode { FULL_BRIGHT, WORLD, CUSTOM }

    private LightMode lightMode = LightMode.FULL_BRIGHT;
    private int customLight = 0xF000F0;

    public Surface(Pos3D pos, Pos3D rotations, Pos2D sizes, int minSubdivisions, boolean renderBack, boolean cull) {
        this.pos = pos;
        this.rotations = rotations;
        this.sizes = sizes;
        this.minSubdivisions = Math.max(minSubdivisions, 1);
        this.renderBack = renderBack;
        this.cull = cull;
        init();
    }

    /**
     * @param pos the position of the surface
     * @return self for chaining.
     * @since 1.8.4
     */
    public Surface setPos(Pos3D pos) {
        this.pos.x = pos.x;
        this.pos.y = pos.y;
        this.pos.z = pos.z;
        return this;
    }

    /**
     * @param pos the position of the surface
     * @return self for chaining.
     * @since 1.8.4
     */
    public Surface setPos(BlockPosHelper pos) {
        this.pos.x = pos.getX();
        this.pos.y = pos.getY();
        this.pos.z = pos.getZ();
        return this;
    }

    /**
     * moves this surface to a point in the world.
     * <p>
     * The coordinates are in blocks and are the panel's top left corner, not its
     * middle, unless {@link #rotateCenter} is on. The position object is written into
     * rather than replaced, so a reference to {@link #pos} keeps following the surface.
     * <p>
     * This is ignored for rendering while the surface is bound to a live entity, since
     * then the entity's own position is used instead.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const surface = draw.surfaceBuilder().pos(0, 64, 0).buildAndAdd();
     * surface.setPos(10, 70, -4);
     * draw.register();
     * </pre>
     *
     * @param x the x coordinate of the surface's corner
     * @param y the y coordinate of the surface's corner
     * @param z the z coordinate of the surface's corner
     * @return self for chaining.
     * @since 1.6.5
     */
    public Surface setPos(double x, double y, double z) {
        this.pos.x = x;
        this.pos.y = y;
        this.pos.z = z;
        return this;
    }

    /**
     * The surface will move with the entity at the offset location.
     *
     * @param boundEntity the entity to bind the surface to
     * @return self for chaining.
     * @since 1.8.4
     */
    public Surface bindToEntity(@Nullable EntityHelper<?> boundEntity) {
        this.boundEntity = boundEntity;
        return this;
    }

    /**
     * entity.
     * @return the entity the surface is bound to, or {@code null} if it is not bound to an
     * @since 1.8.4
     */
    @Nullable
    public EntityHelper<?> getBoundEntity() {
        return boundEntity;
    }

    /**
     * @param boundOffset the offset from the entity's position to render the surface at
     * @return self for chaining.
     * @since 1.8.4
     */
    public Surface setBoundOffset(Pos3D boundOffset) {
        this.boundOffset = boundOffset;
        return this;
    }

    /**
     * @param x the x offset from the entity's position to render the surface at
     * @param y the y offset from the entity's position to render the surface at
     * @param z the z offset from the entity's position to render the surface at
     * @return self for chaining.
     * @since 1.8.4
     */
    public Surface setBoundOffset(double x, double y, double z) {
        this.boundOffset = new Pos3D(x, y, z);
        return this;
    }

    /**
     * @return the offset from the entity's position to render the surface at.
     * @since 1.8.4
     */
    public Pos3D getBoundOffset() {
        return boundOffset;
    }

    /**
     * @param rotateToPlayer whether to rotate the surface to face the player or not
     * @return self for chaining.
     * @since 1.8.4
     */
    public Surface setRotateToPlayer(boolean rotateToPlayer) {
        this.rotateToPlayer = rotateToPlayer;
        return this;
    }

    /**
     * otherwise.
     * @return {@code true} if the surface should be rotated to face the player, {@code false}
     * @since 1.8.4
     */
    public boolean doesRotateToPlayer() {
        return rotateToPlayer;
    }

    /**
     * turns this surface, in degrees on each axis.
     * <p>
     * The rotations are applied in the order y, then x, then z, so the y rotation is
     * the one that swings the panel around to face a compass direction and the other
     * two tilt it. They turn about the panel's corner unless
     * {@link #rotateCenter} is on, in which case they turn about its middle.
     * <p>
     * Nothing is wrapped into a range, so any number of degrees is accepted, and
     * anything beyond 360 is the same as the remainder. This is ignored while
     * {@link #rotateToPlayer} is on, since that overwrites the rotations every frame.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const surface = draw.surfaceBuilder().pos(0, 64, 0).buildAndAdd();
     * // face south, then tip it back
     * surface.setRotations(-30, 180, 0);
     * draw.register();
     * </pre>
     *
     * @param x the rotation about the x axis, in degrees
     * @param y the rotation about the y axis, in degrees
     * @param z the rotation about the z axis, in degrees
     * @since 1.6.5
     */
    public void setRotations(double x, double y, double z) {
        this.rotations.x = x;
        this.rotations.y = y;
        this.rotations.z = z;
    }

    /**
     * resizes this surface, in blocks.
     * <p>
     * This is the physical size of the panel in the world, not how much room its
     * elements have: the pixels they are measured in are subdivisions of this, so
     * making the panel bigger with the subdivision count fixed gives each pixel more
     * blocks. The size object is written into rather than replaced, so
     * {@link #getSizes()} reports the new one straight away.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const surface = draw.surfaceBuilder().size(1, 1).buildAndAdd();
     * // twice as wide in the world, same number of pixels across
     * surface.setSizes(2, 1);
     * draw.register();
     * </pre>
     *
     * @param x the width of the surface, in blocks
     * @param y the height of the surface, in blocks
     * @since 1.6.5
     */
    public void setSizes(double x, double y) {
        this.sizes.x = x;
        this.sizes.y = y;
        recomputeScale();
    }

    /**
     * how big this surface is, in blocks, as a width and a height.
     * <p>
     * A copy rather than the surface's own size object, so writing to what this
     * returns does not resize the surface. To change the size, use
     * {@link #setSizes(double, double)}.
     * <p>
     * These are blocks. The number of surface pixels the elements are measured in is
     * {@link #getWidth()} and {@link #getHeight()}, which are subdivisions of this.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const surface = draw.surfaceBuilder().size(1, 1).minSubdivisions(200).buildAndAdd();
     * // one block on a side, but 200 pixels of room to draw in
     * Chat.log(`size ${surface.getSizes()}, pixels ${surface.getWidth()} by ${surface.getHeight()}`);
     * </pre>
     *
     * @return the size of this surface, in blocks
     * @since 1.6.5
     */
    public Pos2D getSizes() {
        return sizes.add(0, 0);
    }

    /**
     * how finely this surface is divided, which is how many surface pixels it has.
     * <p>
     * The count is across the <em>smaller</em> side; the larger side gets however many
     * that works out to. A value of {@code 0} or below is raised to {@code 1}, so a
     * surface can be made to have a single pixel but never none.
     * <p>
     * This is what the elements' coordinates are measured in, so raising it gives them
     * more room without changing the panel's size in the world; lowering it does the
     * opposite. The physical scale is worked out again immediately, so a change takes
     * effect on the next frame.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const surface = draw.surfaceBuilder().size(1, 1).buildAndAdd();
     * // the default is a single pixel; make it 200 across instead
     * surface.setMinSubdivisions(200);
     * surface.addText("hello", 10, 10, 0xFFFFFFFF, true);
     * draw.register();
     * </pre>
     *
     * @param minSubdivisions how many subdivisions the smaller side is divided into
     * @since 1.6.5
     */
    public void setMinSubdivisions(int minSubdivisions) {
        this.minSubdivisions = Math.max(minSubdivisions, 1);
        recomputeScale();
    }

    private void recomputeScale() {
        scale = Math.min(sizes.x, sizes.y) / minSubdivisions;
    }

    /**
     * how many subdivisions the smaller side of this surface is divided into.
     * <p>
     * This is the number of surface pixels across the smaller side, so it is what the
     * element coordinates are measured in on that axis. It is never below {@code 1}.
     * <p>
     * The builder's own copy of this is not clamped, so a builder set to {@code 0} and
     * a surface built from it disagree until the surface is built.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const surface = draw.surfaceBuilder().size(1, 1).minSubdivisions(0).buildAndAdd();
     * // the builder was happy with 0, the surface raised it
     * Chat.log(`subdivisions: ${surface.getMinSubdivisions()}`);
     * </pre>
     *
     * @return the subdivision count across the smaller side
     * @since 1.6.5
     */
    public int getMinSubdivisions() {
        return minSubdivisions;
    }

    /**
     * Makes all elements on this surface render at full brightness, ignoring world lighting.
     *
     * @return self for chaining.
     * @since 2.0.0
     */
    public Surface setFullBrightLight() {
        this.lightMode = LightMode.FULL_BRIGHT;
        return this;
    }

    /**
     * Makes all elements on this surface sample block and sky light from the world each frame
     * at the surface's position (including the day/night sky darken). Cast shadows are not
     * modelled.
     *
     * @return self for chaining.
     * @since 2.0.0
     */
    public Surface setWorldLight() {
        this.lightMode = LightMode.WORLD;
        return this;
    }

    /**
     * Sets a fixed light level for all elements on this surface.
     *
     * @param blockLight block light level, 0-15 (e.g. 15 next to a torch)
     * @param skyLight   sky light level, 0-15 (e.g. 15 outdoors in daylight)
     * @return self for chaining.
     * @since 2.0.0
     */
    public Surface setLight(int blockLight, int skyLight) {
        this.lightMode = LightMode.CUSTOM;
        this.customLight = packLight(blockLight, skyLight);
        return this;
    }

    private static int packLight(int blockLight, int skyLight) {
        //? if >=26.1 {
        /*return LightCoordsUtil.pack(blockLight, skyLight);
        *///? } else {
        return LightTexture.pack(blockLight, skyLight);
        //?}
    }

    /**
     * how many surface pixels this surface is tall.
     * <p>
     * This is a subdivision count rather than a number of blocks: it is the height in
     * blocks divided by how many blocks one pixel is, which is worked out from the
     * smaller side's subdivision count. So a square surface reports exactly its
     * subdivision count, and a surface twice as tall as it is wide reports twice that.
     * <p>
     * This is the same pair of methods a 2D overlay has, and the meaning is quite
     * different there: on a 2D overlay they are the window's size in scaled pixels.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const surface = draw.surfaceBuilder().size(1, 1).minSubdivisions(200).buildAndAdd();
     * // 200 pixels tall, not 1
     * Chat.log(`${surface.getWidth()} by ${surface.getHeight()} pixels`);
     * draw.register();
     * </pre>
     *
     * @return the height in surface pixels
     * @since 1.6.5
     */
    @Override
    public int getHeight() {
        return (int) (sizes.y / scale);
    }

    /**
     * how many surface pixels this surface is wide.
     * <p>
     * This is a subdivision count rather than a number of blocks: it is the width in
     * blocks divided by how many blocks one pixel is, which is worked out from the
     * smaller side's subdivision count. So a square surface reports exactly its
     * subdivision count, and a surface twice as wide as it is tall reports twice that.
     * <p>
     * This is the same pair of methods a 2D overlay has, and the meaning is quite
     * different there: on a 2D overlay they are the window's size in scaled pixels.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const surface = draw.surfaceBuilder().size(2, 1).minSubdivisions(100).buildAndAdd();
     * // the smaller side is 1 block over 100 subdivisions, so 2 blocks is 200 pixels
     * Chat.log(`${surface.getWidth()} by ${surface.getHeight()} pixels`);
     * draw.register();
     * </pre>
     *
     * @return the width in surface pixels
     * @since 1.6.5
     */
    @Override
    public int getWidth() {
        return (int) (sizes.x / scale);
    }

    /**
     * @param rotateCenter whether to rotate the surface around its center or not
     * @return self for chaining.
     * @since 1.8.4
     */
    public Surface setRotateCenter(boolean rotateCenter) {
        this.rotateCenter = rotateCenter;
        return this;
    }

    /**
     * otherwise.
     * @return {@code true} if this surface is rotated around it's center, {@code false}
     * @since 1.8.4
     */
    public boolean isRotatingCenter() {
        return rotateCenter;
    }

    /**
     * rebuilds this surface and clears its elements.
     * <p>
     * The scale is worked out again from the current size and subdivision count before
     * anything else, so a surface whose size was written to directly rather than
     * through {@link #setSizes(double, double)} gets the right scale back. Then it does
     * what any 2D overlay does on init, which is empty the element list and run the
     * init function if one is set.
     * <p>
     * The 3D overlay calls this on a surface when it is registered, and again if the
     * window is resized, so a surface whose elements are built in an init function
     * come back after a resize. There is no reason for a script to call it directly.
     *
     * @since 1.6.5
     */
    @Override
    public void init() {
        recomputeScale();
        super.init();
    }

    /**
     * always {@code 0}.
     * <p>
     * A surface is sorted against other 3D elements by where it is in the world rather
     * than by a z-index, so it reports the same value whatever was set. This is the
     * 2D overlay z-index method with a different meaning, and setting it on a surface
     * has no effect on where it is drawn.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const surface = draw.surfaceBuilder().pos(0, 64, 0).buildAndAdd();
     * // a surface's own z-index is not what decides its draw order
     * Chat.log(`surface z-index: ${surface.getZIndex()}`);
     * draw.register();
     * </pre>
     *
     * @return always {@code 0}
     * @since 1.6.5
     */
    @Override
    public int getZIndex() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Surface surface = (Surface) o;
        return Objects.equals(pos, surface.pos) && Objects.equals(rotations, surface.rotations) && Objects.equals(sizes, surface.sizes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, rotations, sizes);
    }

    /**
     * orders this surface against another one.
     * <p>
     * By position first, then by rotations, then by size, and only when the two agree
     * on all three is the comparison a tie. It is a raw comparison on the coordinates
     * rather than a distance, so two surfaces that overlap still order against each
     * other rather than tying, and the result says which comes first rather than how
     * far apart they are.
     * <p>
     * This is only reached for two surfaces, since the sort compares class names first
     * and only ties get here.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const a = draw.surfaceBuilder().pos(0, 64, 0).build();
     * const b = draw.surfaceBuilder().pos(10, 64, 0).build();
     * Chat.log(`a sorts before b: ${a.compareToSame(b) < 0}`);
     * </pre>
     *
     * @param other another surface to order this one against
     * @return a negative number, zero, or a positive number as this surface is before,
     *         the same as, or after {@code other}
     * @since 1.6.5
     */
    @Override
    public int compareToSame(Surface other) {
        int i = pos.compareTo(other.pos);
        if (i == 0) {
            i = rotations.compareTo(other.rotations);
            if (i == 0) {
                i = sizes.compareTo(other.sizes);
            }
        }
        return i;
    }

    @Override
    @DocletIgnore
    public void render(PoseStack matrices, MultiBufferSource consumers, float tickDelta) {
        // On 1.21.11+ surfaces are drawn from renderDirect inside the Gizmos pass.
        //? if <1.21.11 {
        /*renderSurface(matrices, consumers, tickDelta);
        *///? }
    }

    @Override
    @DocletIgnore
    public void renderDirect(PoseStack matrices, MultiBufferSource consumers, float tickDelta, boolean alwaysOnTop) {
        //? if >=1.21.11 {
        /*// cull surfaces are depth-tested; non-cull surfaces draw after the depth clear.
        if ((!this.cull) != alwaysOnTop) {
            return;
        }
        renderSurface(matrices, consumers, tickDelta);
        *///? }
    }

    private void renderSurface(PoseStack matrices, MultiBufferSource consumers, float tickDelta) {
        boolean seeThrough = !this.cull;
        Pos3D renderPos = resolveRenderPos(tickDelta);
        updateRotateToPlayer(renderPos);
        Matrix4f transform = buildSurfaceTransform(renderPos);
        if (!renderBack && isCameraOnBackSide(transform)) {
            return;
        }
        int light = resolveLight(renderPos);
        matrices.pushPose();
        matrices.mulPose(transform);
        synchronized (elements) {
            renderDirectElements(matrices, consumers, light, seeThrough, tickDelta, getElementsByZIndex());
        }
        matrices.popPose();
    }

    /**
     * True when the camera is behind the surface's readable face. Surface content is
     * authored facing local +Z (see {@link #updateRotateToPlayer}, which points +Z at
     * the camera), so the camera is behind it when it lies on the -Z side of the plane.
     */
    private static boolean isCameraOnBackSide(Matrix4f transform) {
        Vec3 cameraPos = CameraCompat.position(Minecraft.getInstance().gameRenderer.getMainCamera());
        Vector3f origin = transform.transformPosition(new Vector3f(0, 0, 0));
        Vector3f facing = transform.transformPosition(new Vector3f(0, 0, 1)).sub(origin);
        return facing.x * (cameraPos.x - origin.x)
                + facing.y * (cameraPos.y - origin.y)
                + facing.z * (cameraPos.z - origin.z) < 0;
    }

    @DocletIgnore
    public Pos3D resolveRenderPos(float partialTicks) {
        boolean isTrackingEntity = boundEntity != null && boundEntity.isAlive();
        return isTrackingEntity ? boundEntity.getInterpolatedPos(partialTicks).add(boundOffset) : pos;
    }

    private void updateRotateToPlayer(Pos3D renderPos) {
        if (!rotateToPlayer) {
            return;
        }
        Vec3 cameraPos = CameraCompat.position(Minecraft.getInstance().gameRenderer.getMainCamera());
        double pivotX = rotateCenter ? renderPos.x + (sizes.x / 2.0) : renderPos.x;
        double pivotY = rotateCenter ? renderPos.y - (sizes.y / 2.0) : renderPos.y;
        double pivotZ = renderPos.z;
        double dx = cameraPos.x - pivotX;
        double dy = cameraPos.y - pivotY;
        double dz = cameraPos.z - pivotZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        rotations.x = -Math.toDegrees(Math.atan2(dy, horizontal));
        rotations.y = Math.toDegrees(Math.atan2(dx, dz));
        rotations.z = 0;
    }

    private void renderDirectElements(PoseStack matrices, MultiBufferSource consumers, int light, boolean seeThrough, float tickDelta, Iterator<RenderElement> iter) {
        while (iter.hasNext()) {
            RenderElement element = iter.next();
            if (element instanceof Draw2DElement draw2DElement) {
                // Give the nested panel its zIndex depth so it does not sit coplanar
                // with the parent's rects (older targets write depth in debugQuads).
                matrices.pushPose();
                if (scale != 0) {
                    matrices.translate(0, 0, (float) ((zIndexScale / scale) * element.getZIndex()));
                }
                renderNestedDirect(matrices, consumers, light, seeThrough, tickDelta, draw2DElement);
                matrices.popPose();
                continue;
            }
            matrices.pushPose();
            if (scale != 0) {
                matrices.translate(0, 0, (float) ((zIndexScale / scale) * element.getZIndex()));
            }
            element.render3D(matrices, consumers, light, seeThrough, tickDelta);
            // debugQuads sorts quads by camera distance on upload, which ignores
            // zIndex order, so flush each element as its own batch and rely on
            // painter's order.
            if (consumers instanceof MultiBufferSource.BufferSource bufferSource) {
                bufferSource.endBatch();
            }
            matrices.popPose();
        }
    }

    private void renderNestedDirect(PoseStack matrices, MultiBufferSource consumers, int light, boolean seeThrough, float tickDelta, Draw2DElement element) {
        matrices.pushPose();
        matrices.translate(element.x, element.y, 0);
        matrices.scale((float) element.scale, (float) element.scale, 1);
        float centerX = element.getWidth() / 2f;
        float centerY = element.getHeight() / 2f;
        if (element.rotateCenter) {
            matrices.translate(centerX, centerY, 0);
        }
        matrices.mulPose(new Quaternionf().rotateLocalZ((float) Math.toRadians(element.rotation)));
        if (element.rotateCenter) {
            matrices.translate(-centerX, -centerY, 0);
        }
        Draw2D draw2D = element.getDraw2D();
        synchronized (draw2D.getElements()) {
            renderDirectElements(matrices, consumers, light, seeThrough, tickDelta, draw2D.getElementsByZIndex());
        }
        matrices.popPose();
    }

    private Matrix4f buildSurfaceTransform(Pos3D renderPos) {
        Matrix4f m = new Matrix4f();
        m.translate((float) renderPos.x, (float) renderPos.y, (float) renderPos.z);
        float halfX = (float) (sizes.x / 2.0);
        float halfY = (float) (sizes.y / 2.0);
        if (rotateCenter) {
            // Rotate about the surface centre by giving each axis its own pivot.
            m.translate(halfX, 0, 0);
            m.rotateY((float) Math.toRadians(rotations.y));
            m.translate(-halfX, 0, 0);
            m.translate(0, -halfY, 0);
            m.rotateX((float) Math.toRadians(rotations.x));
            m.translate(0, halfY, 0);
            m.translate(halfX, -halfY, 0);
            m.rotateZ((float) Math.toRadians(rotations.z));
            m.translate(-halfX, halfY, 0);
        } else {
            m.rotateY((float) Math.toRadians(rotations.y));
            m.rotateX((float) Math.toRadians(rotations.x));
            m.rotateZ((float) Math.toRadians(rotations.z));
        }
        // Flip y (surface space grows downwards) and map surface pixels to blocks.
        m.scale((float) scale, (float) -scale, (float) scale);
        return m;
    }

    private int resolveLight(Pos3D renderPos) {
        return switch (lightMode) {
            case FULL_BRIGHT -> 0xF000F0;
            case CUSTOM -> customLight;
            case WORLD -> {
                var level = Minecraft.getInstance().level;
                if (level == null) {
                    yield 0xF000F0;
                }
                BlockPos blockPos = renderPos.toRawBlockPos();
                int block = level.getBrightness(LightLayer.BLOCK, blockPos);
                // Subtract the sky darken so the surface also dims at night, like
                // the vanilla lightmap. Cast shadows are not modelled.
                int sky = Math.max(0, level.getBrightness(LightLayer.SKY, blockPos) - level.getSkyDarken());
                yield packLight(block, sky);
            }
        };
    }

    private static Vector3f toEulerDegrees(Quaternionf quaternion) {
        // The old method
        float w = quaternion.w();
        float x = quaternion.x();
        float y = quaternion.y();
        float z = quaternion.z();

        float wSquared = w * w;
        float xSquared = x * x;
        float ySquared = y * y;
        float zSquared = z * z;
        float sumSquared = wSquared + xSquared + ySquared + zSquared;
        float k = 2.0F * w * x - 2.0F * y * z;

        double radianX = Math.asin(k / sumSquared);
        double radianY;
        double radianZ;
        if (Math.abs(k) > 0.999F * sumSquared) {
            radianY = 2.0F * Math.atan2(y, w);
            radianZ = 0.0F;
        } else {
            radianY = Math.atan2(2.0F * x * z + 2.0F * y * w, wSquared - xSquared - ySquared + zSquared);
            radianZ = Math.atan2(2.0F * x * y + 2.0F * w * z, wSquared - xSquared + ySquared - zSquared);
        }
        return new Vector3f((float) Math.toDegrees(radianX), (float) Math.toDegrees(radianY), (float) Math.toDegrees(radianZ));
    }

    private void renderElements3D(GuiGraphics drawContext, Iterator<RenderElement> iter) {
        while (iter.hasNext()) {
            RenderElement element = iter.next();
            // Render each draw2D element individually so that the cull and renderBack settings are used
            if (element instanceof Draw2DElement draw2DElement) {
                renderDraw2D3D(drawContext, draw2DElement);
            } else {
                renderElement3D(drawContext, element);
            }
        }
    }

    private void renderDraw2D3D(GuiGraphics drawContext, Draw2DElement element) {
        // TODO: Does setupMatrix operate the same here? Why does it have the final translation?
        //? if >1.21.5 {
        Matrix3x2fStack matrixStack = drawContext.pose();
        matrixStack.pushMatrix();
        setupMatrix(matrixStack, element.x, element.y, element.scale, element.rotation, element.getWidth(), element.getHeight(), element.rotateCenter);
        //?} else {
        /*PoseStack matrixStack = drawContext.pose();
        matrixStack.pushPose();
        matrixStack.translate(element.x, element.y, 0);
        matrixStack.scale(element.scale, element.scale, 1);
        if (rotateCenter) {
            matrixStack.translate(element.width.getAsInt() / 2d, element.height.getAsInt() / 2d, 0);
        }
        matrixStack.mulPose(new Quaternionf().rotateLocalZ((float) Math.toRadians(element.rotation)));
        if (rotateCenter) {
            matrixStack.translate(-element.width.getAsInt() / 2d, -element.height.getAsInt() / 2d, 0);
        }
        *///?}

        // Don't translate back!
        Draw2D draw2D = element.getDraw2D();
        synchronized (draw2D.getElements()) {
            renderElements3D(drawContext, draw2D.getElementsByZIndex());
        }
        //? if >1.21.5 {
        matrixStack.popMatrix();
        //?} else {
        /*matrixStack.popPose();
        *///?}
    }

    private void renderElement3D(GuiGraphics drawContext, RenderElement element) {
        //? if >1.21.5 {
        Matrix3x2fStack matrixStack = drawContext.pose();
        matrixStack.pushMatrix();
        // Z-index is no longer possible as this is a 3x2 matrix now.
        //matrixStack.translate(0, 0, zIndexScale * element.getZIndex());
        element.render3D(drawContext, 0, 0, 0);
        matrixStack.popMatrix();
        //?} else {
        /*PoseStack matrices = drawContext.pose();
        matrices.pushPose();
        matrices.translate(0, 0, zIndexScale * element.getZIndex());
        element.render3D(drawContext, 0, 0, 0);
        matrices.popPose();
        *///?}
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        // This does nothing I guess?
    }

    /**
     * collects the values for a {@link Surface}, then builds it.
     * <p>
     * A chain of setters, then {@link #build()} for a surface that is not put anywhere
     * or {@link #buildAndAdd()} for one added to the 3D overlay it was made from. A
     * surface on its own draws nothing either way; the overlay has to be registered.
     * <p>
     * The defaults are a surface ten blocks on a side at the origin, rotated about its
     * middle, with its back drawn, not depth tested, drawn over everything, lit at full
     * brightness and not following anybody. Ten blocks is a large default for a panel
     * whose elements are measured in pixels, so the size is normally set explicitly.
     * <p>
     * There is no init function here. A surface is a 2D overlay, so if its elements
     * are built in one, that goes through the surface itself once it has been built.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const surface = draw.surfaceBuilder()
     *    .pos(0.5, 64, 0.5)
     *    .size(1, 1)
     *    .minSubdivisions(200)
     *    .rotateToPlayer(true)
     *    .buildAndAdd();
     * surface.addText("a label", 10, 10, 0xFFFFFFFF, true);
     * draw.register();
     * </pre>
     *
     * @author Etheradon
     * @since 1.8.4
     */
    @DocletCategory("Rendering/Graphics")
    public static class Builder {
        private final Draw3D parent;

        private Pos3D pos = new Pos3D(0, 0, 0);
        @Nullable
        private EntityHelper<?> boundEntity;
        private Pos3D boundOffset = Pos3D.ZERO;
        private double xRot = 0;
        private double yRot = 0;
        private double zRot = 0;
        private boolean rotateCenter = true;
        private boolean rotateToPlayer = false;
        private double width = 10;
        private double height = 10;
        private int minSubdivisions = 1;
        private double zIndexScale = 0.001;
        private boolean renderBack = true;
        private boolean cull = false;
        private LightMode lightMode = LightMode.FULL_BRIGHT;
        private int customLight = 0xF000F0;

        public Builder(Draw3D parent) {
            this.parent = parent;
        }

        /**
         * The position is taken by reference, so the surface this builds and this
         * builder share it and moving one moves the other. The coordinates are the
         * panel's top left corner rather than its middle.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * const where = PositionCommon.createPos(0.5, 64, 0.5);
         * draw.surfaceBuilder().pos(where).size(1, 1).buildAndAdd();
         * // moving the position after the fact moves the surface
         * where.y = 70;
         * draw.register();
         * </pre>
         *
         * @param pos the position of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos(Pos3D pos) {
            this.pos = pos;
            return this;
        }

        /**
         * The block's own position, without its offset, so this is a block corner
         * rather than the middle of the block. A surface placed on a block this way has
         * its top left corner at that corner.
         * example:
         * <pre>
         * const player = Player.getPlayer();
         * if (player !== null) {
         *   const draw = Hud.createDraw3D();
         *   draw.surfaceBuilder().pos(player.getBlockPos()).size(1, 1).buildAndAdd();
         *   draw.register();
         * }
         * </pre>
         *
         * @param pos the position of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos(BlockPosHelper pos) {
            this.pos = pos.toPos3D();
            return this;
        }

        /**
         * The panel's top left corner, in blocks. A new position object is made here,
         * so unlike the {@link Pos3D} form nothing outside shares it with the surface
         * this builds.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.surfaceBuilder().pos(0.5, 64, 0.5).size(1, 1).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param x the x position of the surface
         * @param y the y position of the surface
         * @param z the z position of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder pos(double x, double y, double z) {
            this.pos = new Pos3D(x, y, z);
            return this;
        }

        /**
         * The builder's own position rather than a copy, and it is {@code 0, 0, 0}
         * until something is set. When a position object was handed to
         * {@link #pos(Pos3D)} this is that same object, so moving it moves the surface
         * the builder will build.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().surfaceBuilder();
         * builder.pos(0.5, 64, 0.5);
         * Chat.log(`surface is at ${builder.getPos()}`);
         * </pre>
         *
         * @return the position of the surface.
         * @since 1.8.4
         */
        public Pos3D getPos() {
            return pos;
        }

        /**
         * The surface will move with the entity at the offset location.
         * <p>
         * The surface is then drawn at the entity's interpolated position plus
         * {@link #boundOffset(double, double, double)}, worked out fresh every frame,
         * so it follows an entity that is moving. While the entity is alive the
         * surface's own position is not used at all; once it is not, the surface sits
         * where {@link #pos(Pos3D)} put it.
         * <p>
         * A {@code null} entity unbinds nothing in particular; it leaves the surface
         * unbound, which is the same as not calling this at all.
         * example:
         * <pre>
         * const player = Player.getPlayer();
         * if (player !== null) {
         *   const draw = Hud.createDraw3D();
         *   draw.surfaceBuilder()
         *      .bindToEntity(player)
         *      .boundOffset(0, 1.8, 0)
         *      .size(0.5, 0.5)
         *      .rotateToPlayer(true)
         *      .buildAndAdd();
         *   draw.register();
         * }
         * </pre>
         *
         * @param boundEntity the entity to bind the surface to
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder bindToEntity(@Nullable EntityHelper<?> boundEntity) {
            this.boundEntity = boundEntity;
            return this;
        }

        /**
         * entity.
         * @return the entity the surface is bound to, or {@code null} if it is not bound to an
         * @since 1.8.4
         */
        @Nullable
        public EntityHelper<?> getBoundEntity() {
            return boundEntity;
        }

        /**
         * In blocks, added to the entity's interpolated position every frame. The
         * offset is taken by reference, so the surface this builds and this builder
         * share it. It has no effect unless the surface is bound to an entity.
         * example:
         * <pre>
         * const player = Player.getPlayer();
         * if (player !== null) {
         *   const draw = Hud.createDraw3D();
         *   const lift = PositionCommon.createPos(0, 1.8, 0);
         *   draw.surfaceBuilder().bindToEntity(player).boundOffset(lift).size(0.5, 0.5).buildAndAdd();
         *   // moving the offset after the fact moves the surface with it
         *   lift.y = 2.4;
         *   draw.register();
         * }
         * </pre>
         *
         * @param entityOffset the offset from the entity's position to render the surface at
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder boundOffset(Pos3D entityOffset) {
            this.boundOffset = entityOffset;
            return this;
        }

        /**
         * In blocks, added to the entity's interpolated position every frame. This is
         * measured from the entity's feet, so {@code 1.8} puts the panel roughly at eye
         * level for a player. A new object is made here, so unlike the
         * {@link Pos3D} form nothing outside shares it.
         * example:
         * <pre>
         * const player = Player.getPlayer();
         * if (player !== null) {
         *   const draw = Hud.createDraw3D();
         *   draw.surfaceBuilder().bindToEntity(player).boundOffset(0, 1.8, 0).size(0.5, 0.5).buildAndAdd();
         *   draw.register();
         * }
         * </pre>
         *
         * @param x the x offset from the entity's position to render the surface at
         * @param y the y offset from the entity's position to render the surface at
         * @param z the z offset from the entity's position to render the surface at
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder boundOffset(double x, double y, double z) {
            this.boundOffset = new Pos3D(x, y, z);
            return this;
        }

        /**
         * The builder's own offset rather than a copy, and it starts at {@code 0, 0, 0}
         * until something is set. It says nothing about the surface once that has been
         * built, which carries its own copy of the reference.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().surfaceBuilder();
         * Chat.log(`offset starts at ${builder.getBoundOffset()}`);
         * </pre>
         *
         * @return the offset from the entity's position to render the surface at.
         * @since 1.8.4
         */
        public Pos3D getBoundOffset() {
            return boundOffset;
        }

        /**
         * In degrees, tipping the panel forwards or backwards. The three rotations are
         * applied in the order y, then x, then z, so the y one is the one that decides
         * which way the panel faces.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.surfaceBuilder().pos(0, 64, 0).size(1, 1).xRotation(-30).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param xRot the x rotation of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder xRotation(double xRot) {
            this.xRot = xRot;
            return this;
        }

        /**
         * The builder's own value, and {@code 0} until something is set. It is
         * unrelated to the rotations of a surface that has already been built, which
         * are overwritten every frame while it faces the player.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().surfaceBuilder();
         * Chat.log(`x rotation is ${builder.getXRotation()}`);
         * </pre>
         *
         * @return the x rotation of the surface.
         * @since 1.8.4
         */
        public double getXRotation() {
            return xRot;
        }

        /**
         * In degrees, swinging the panel around to face a compass direction. This is
         * the rotation that is applied first, so the other two tip a panel that is
         * already facing the direction it was pointed at.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.surfaceBuilder().pos(0, 64, 0).size(1, 1).yRotation(180).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param yRot the y rotation of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder yRotation(double yRot) {
            this.yRot = yRot;
            return this;
        }

        /**
         * The builder's own value, and {@code 0} until something is set.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().surfaceBuilder();
         * builder.yRotation(180);
         * Chat.log(`facing y rotation ${builder.getYRotation()}`);
         * </pre>
         *
         * @return the y rotation of the surface.
         * @since 1.8.4
         */
        public double getYRotation() {
            return yRot;
        }

        /**
         * In degrees, rolling the panel within its own plane. This is applied last, so
         * it rolls a panel that has already been pointed somewhere and tipped.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.surfaceBuilder().pos(0, 64, 0).size(1, 1).zRotation(45).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param zRot the z rotation of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder zRotation(double zRot) {
            this.zRot = zRot;
            return this;
        }

        /**
         * The builder's own value, and {@code 0} until something is set.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().surfaceBuilder();
         * Chat.log(`z rotation is ${builder.getZRotation()}`);
         * </pre>
         *
         * @return the z rotation of the surface.
         * @since 1.8.4
         */
        public double getZRotation() {
            return zRot;
        }

        /**
         * All three at once, in degrees, which is the three number form of the three
         * separate setters. They are applied in the order y, then x, then z.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.surfaceBuilder()
         *    .pos(0, 64, 0)
         *    .size(1, 1)
         *    .rotation(-30, 180, 0)
         *    .buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param xRot the x rotation of the surface
         * @param yRot the y rotation of the surface
         * @param zRot the z rotation of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder rotation(double xRot, double yRot, double zRot) {
            this.xRot = xRot;
            this.yRot = yRot;
            this.zRot = zRot;
            return this;
        }

        /**
         * This decides the pivot for the rotations and for the facing-the-camera
         * rotation, and for nested 2D overlays drawn onto the surface. The default
         * here is {@code true}, which is the opposite of what a surface gets when it
         * is made through {@code Draw3D.addDraw2D} rather than through this builder.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * // turn about the corner instead of the middle
         * draw.surfaceBuilder().pos(0, 64, 0).size(1, 1).rotateCenter(false).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param rotateCenter whether to rotate around the center of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder rotateCenter(boolean rotateCenter) {
            this.rotateCenter = rotateCenter;
            return this;
        }

        /**
         * {@code false} otherwise.
         * <p>
         * The default is {@code true} here, so a builder that has had nothing set on it
         * turns about the middle of the panel.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().surfaceBuilder();
         * Chat.log(`rotates about the center: ${builder.isRotatingCenter()}`);
         * </pre>
         *
         * @return {@code true} if this surface should be rotated around its center,
         * @since 1.8.4
         */
        public boolean isRotatingCenter() {
            return rotateCenter;
        }

        /**
         * On, the panel is turned to face the camera every frame and whatever
         * rotations were set are overwritten, so a surface set up this way cannot be
         * given a fixed orientation. The default here is {@code false}.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * // a nameplate that always faces the player
         * draw.surfaceBuilder()
         *    .pos(0.5, 64, 0.5)
         *    .size(1, 1)
         *    .rotateToPlayer(true)
         *    .buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param rotateToPlayer whether to rotate the surface to face the player or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder rotateToPlayer(boolean rotateToPlayer) {
            this.rotateToPlayer = rotateToPlayer;
            return this;
        }

        /**
         * {@code false} otherwise.
         * <p>
         * The default is {@code false}, so a builder that has had nothing set on it
         * keeps whatever rotations it was given.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().surfaceBuilder();
         * Chat.log(`faces the player: ${builder.doesRotateToPlayer()}`);
         * </pre>
         *
         * @return {@code true} if the surface should be rotated to face the player,
         * @since 1.8.4
         */
        public boolean doesRotateToPlayer() {
            return rotateToPlayer;
        }

        /**
         * In blocks, and the panel's total width rather than anything to do with the
         * pixels its elements are measured in. The default here is ten blocks, which is
         * a large panel, so this is normally set explicitly.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.surfaceBuilder().pos(0, 64, 0).width(1).minSubdivisions(200).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param width the width of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder width(double width) {
            this.width = width;
            return this;
        }

        /**
         * The builder's own value in blocks, and {@code 10} until something is set.
         * This is not the same thing as the width of a built surface, which is reported
         * in surface pixels by {@link Surface#getWidth()}.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().surfaceBuilder();
         * builder.width(1);
         * Chat.log(`builder width ${builder.getWidth()} blocks`);
         * </pre>
         *
         * @return the width of the surface.
         * @since 1.8.4
         */
        public double getWidth() {
            return width;
        }

        /**
         * In blocks, and the panel's total height rather than anything to do with the
         * pixels its elements are measured in. The default here is ten blocks.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.surfaceBuilder().pos(0, 64, 0).height(1).minSubdivisions(200).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param height the height of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder height(double height) {
            this.height = height;
            return this;
        }

        /**
         * The builder's own value in blocks, and {@code 10} until something is set.
         * This is not the same thing as the height of a built surface, which is reported
         * in surface pixels by {@link Surface#getHeight()}.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().surfaceBuilder();
         * builder.height(1);
         * Chat.log(`builder height ${builder.getHeight()} blocks`);
         * </pre>
         *
         * @return the height of the surface.
         * @since 1.8.4
         */
        public double getHeight() {
            return height;
        }

        /**
         * Both at once, in blocks, which is the two number form of the two separate
         * setters. This is the panel's physical size, not how many surface pixels its
         * elements are measured in; that comes from
         * {@link #minSubdivisions(int)}.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * // a one block panel with 200 pixels across its smaller side
         * draw.surfaceBuilder().pos(0, 64, 0).size(1, 1).minSubdivisions(200).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param width  the width of the surface
         * @param height the height of the surface
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder size(double width, double height) {
            this.width = width;
            this.height = height;
            return this;
        }

        /**
         * How many surface pixels the <em>smaller</em> side is divided into, which is
         * what the elements' coordinates are measured in. The default here is
         * {@code 1}, so a builder that has had nothing set on it makes a panel with a
         * single pixel across its smaller side.
         * <p>
         * Nothing is clamped here. The surface that comes out of {@link #build()}
         * raises anything below {@code 1} to {@code 1}, so a builder set to {@code 0}
         * and the surface built from it disagree until the surface exists.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.surfaceBuilder().pos(0, 64, 0).size(1, 1).minSubdivisions(200).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param minSubdivisions the minimum number of subdivisions
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder minSubdivisions(int minSubdivisions) {
            this.minSubdivisions = minSubdivisions;
            return this;
        }

        /**
         * The builder's own value, and {@code 1} until something is set. Unlike the
         * built surface's own count, this is not raised to {@code 1}, so a builder set
         * to {@code 0} still reports {@code 0} here even though the surface it builds
         * will report {@code 1}.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().surfaceBuilder();
         * builder.minSubdivisions(0);
         * Chat.log(`builder says ${builder.getMinSubdivisions()}`);
         * </pre>
         *
         * @return the minimum number of subdivisions.
         * @since 1.8.4
         */
        public int getMinSubdivisions() {
            return minSubdivisions;
        }

        /**
         * With this off the surface draws nothing at all while the camera is on its
         * back side, which is what a panel that should only be readable from the front
         * wants. The default here is {@code true}, so the back is drawn unless it is
         * turned off.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * // readable only from the front
         * draw.surfaceBuilder()
         *    .pos(0, 64, 0)
         *    .size(1, 1)
         *    .renderBack(false)
         *    .buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param renderBack whether the back of the surface should be rendered or not
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder renderBack(boolean renderBack) {
            this.renderBack = renderBack;
            return this;
        }

        /**
         * otherwise.
         * <p>
         * The default is {@code true} here, which is the opposite of what a surface
         * gets through {@code Draw3D.addDraw2D}.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().surfaceBuilder();
         * Chat.log(`renders its back: ${builder.shouldRenderBack()}`);
         * </pre>
         *
         * @return {@code true} if the back of the surface should be rendered, {@code false}
         * @since 1.8.4
         */
        public boolean shouldRenderBack() {
            return renderBack;
        }

        /**
         * {@code true} is the surface that terrain draws over and {@code false} is the
         * one drawn over the world. The default here is {@code false}, so a surface
         * from this builder is on top unless asked otherwise, which is what
         * {@code Draw3D.addDraw2D} gives too.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * // a panel something in front of it can hide
         * draw.surfaceBuilder()
         *    .pos(0, 64, 0)
         *    .size(1, 1)
         *    .minSubdivisions(200)
         *    .cull(true)
         *    .buildAndAdd();
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
         * That is, {@code true} when the surface is depth tested and terrain can hide
         * it. The word "box" in this is a leftover from when the builder was shared
         * with the box builder; the value is the surface's.
         * <p>
         * The default is {@code false}, so a surface from this builder is drawn over
         * the world.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().surfaceBuilder();
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
         * How far apart, in blocks, two neighbouring z-indexes on this surface's
         * elements are drawn. The default is a thousandth of a block, which is enough
         * to order flat elements on the same panel; a larger value is needed if they
         * are fighting with the world for the same depth.
         * <p>
         * This is the surface's own z-index scale. A surface's own z-index is always
         * {@code 0} and is not what positions it in the world.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * const surface = draw.surfaceBuilder().pos(0, 64, 0).size(1, 1).buildAndAdd();
         * // a bigger step between neighbouring elements, to stop z-fighting
         * surface.zIndexScale = 0.01;
         * draw.register();
         * </pre>
         *
         * @param zIndexScale the scale of the z-index
         * @return self for chaining.
         * @since 1.8.4
         */
        public Builder zIndex(double zIndexScale) {
            this.zIndexScale = zIndexScale;
            return this;
        }

        /**
         * The builder's own value, and a thousandth of a block until something is set.
         * This is the builder's copy rather than the surface's, and changing the
         * surface's own field afterwards does not move it.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().surfaceBuilder();
         * Chat.log(`z-index scale is ${builder.getZIndexScale()}`);
         * </pre>
         *
         * @return the scale of the z-index.
         * @since 1.8.4
         */
        public double getZIndexScale() {
            return zIndexScale;
        }

        /**
         * Renders all elements at full brightness, ignoring world lighting.
         *
         * @return self for chaining.
         * @since 2.0.0
         */
        public Builder fullBrightLight() {
            this.lightMode = LightMode.FULL_BRIGHT;
            return this;
        }

        /**
         * Samples block and sky light from the world each frame at the surface's position.
         *
         * @return self for chaining.
         * @since 2.0.0
         */
        public Builder worldLight() {
            this.lightMode = LightMode.WORLD;
            return this;
        }

        /**
         * Sets a fixed light level for all elements on this surface.
         *
         * @param blockLight block light level, 0-15 (e.g. 15 next to a torch)
         * @param skyLight   sky light level, 0-15 (e.g. 15 outdoors in daylight)
         * @return self for chaining.
         * @since 2.0.0
         */
        public Builder light(int blockLight, int skyLight) {
            this.lightMode = LightMode.CUSTOM;
            this.customLight = packLight(blockLight, skyLight);
            return this;
        }

        /**
         * Creates the surface for the given values and adds it to the draw3D.
         * <p>
         * The surface goes into the same list any other 3D element does, so
         * {@code Draw3D.getDraw2Ds()} reports it and {@code removeDraw2D} takes it off
         * again. It draws nothing until the overlay itself is registered.
         * <p>
         * This is the point at which anything below {@code 1} in the subdivision count
         * is raised, so the surface this returns may report a different subdivision
         * count than this builder did.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.surfaceBuilder().pos(0, 64, 0).size(1, 1).minSubdivisions(200).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @return the build surface.
         * @since 1.8.4
         */
        public Surface buildAndAdd() {
            Surface surface = build();
            parent.addSurface(surface);
            return surface;
        }

        /**
         * Builds the surface from the given values.
         * <p>
         * The surface is not put anywhere, so it draws nothing until it is added to a
         * 3D overlay. It is a separate object from the builder, so the builder can be
         * changed and used again afterwards.
         * <p>
         * The position is the one object the two do share: a position handed to
         * {@link #pos(Pos3D)} is the same object the surface gets, so moving it moves
         * the surface. The rotations and the size are made fresh here.
         * <p>
         * The builder's default of rotating about the middle and drawing the back is
         * carried over, along with the lighting, so a built surface is not the same as
         * one made through {@code Draw3D.addDraw2D} in those respects.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * const surface = draw.surfaceBuilder().pos(0, 64, 0).size(1, 1).build();
         * // built but not added, so it is not in the overlay yet
         * draw.addSurface(surface);
         * surface.addText("hello", 10, 10, 0xFFFFFFFF, true);
         * draw.register();
         * </pre>
         *
         * @return the build surface.
         */
        public Surface build() {
            Surface surface = new Surface(
                    pos,
                    new Pos3D(xRot, yRot, zRot),
                    new Pos2D(width, height),
                    minSubdivisions,
                    renderBack,
                    cull
            )
                    .setRotateCenter(rotateCenter)
                    .setRotateToPlayer(rotateToPlayer)
                    .bindToEntity(boundEntity)
                    .setBoundOffset(boundOffset);
            surface.lightMode = lightMode;
            surface.customLight = customLight;
            return surface;
        }

    }

}
