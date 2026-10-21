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
 * A {@link Draw2D} panel rendered in the world. Its element coordinates are surface
 * pixels measured from the top left, with y increasing downwards; surface size is in
 * blocks, and {@link #getWidth()} and {@link #getHeight()} report pixel dimensions.
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
     * Whether the surface faces the camera during rendering. When enabled, the
     * camera-derived angles overwrite {@link #rotations} each frame; the pivot is
     * controlled by {@link #rotateCenter}.
     *
     * @since 1.6.5
     */
    public boolean rotateToPlayer;
    /**
     * Whether surface rotations pivot around the center rather than the corner.
     * This also selects the pivot for camera-facing rotation. The field defaults to
     * {@code false}; the builder defaults to {@code true}.
     *
     * @since 1.6.5
     */
    public boolean rotateCenter;
    /**
     * Entity followed by this surface, or {@code null} if unbound. While alive, the
     * surface uses the entity's interpolated position plus {@link #boundOffset}; if
     * the entity is no longer alive, rendering uses {@link #pos} instead.
     *
     * @since 1.6.5
     */
    @Nullable
    public EntityHelper<?> boundEntity;
    /**
     * Offset in blocks added to the bound entity's interpolated position at render
     * time. It has no effect when the surface is unbound.
     *
     * @since 1.6.5
     */
    public Pos3D boundOffset = Pos3D.ZERO;
    /**
     * World position in blocks used when this surface is not following a live entity.
     * It is the panel corner; {@link #rotateCenter} changes the rotation pivot, not
     * the position's meaning.
     *
     * @since 1.6.5
     */
    public final Pos3D pos;
    /**
     * Surface rotations in degrees, applied in y, x, then z order. When
     * {@link #rotateToPlayer} is enabled, rendering overwrites these values each frame.
     *
     * @since 1.6.5
     */
    public final Pos3D rotations;
    /**
     * Physical width and height in blocks; element dimensions are measured in surface
     * pixels. Read through {@link #getSizes()}.
     *
     * @since 1.6.5
     */
    protected final Pos2D sizes;
    /**
     * Number of surface pixels across the smaller side. Values are clamped to at least
     * {@code 1}; the larger side scales with the aspect ratio.
     *
     * @since 1.6.5
     */
    protected int minSubdivisions;

    /**
     * Pixel size in blocks: the smaller physical dimension divided by
     * {@code minSubdivisions}. Used to convert block dimensions to pixel dimensions.
     *
     * @since 1.6.5
     */
    protected double scale;
    /**
     * Scales each element's z-index into a rendering offset in blocks. Defaults to
     * {@code 0.001} blocks per z-index unit.
     *
     * @since 1.6.5
     */
    public double zIndexScale = 0.001;
    /**
     * Whether the surface is drawn when the camera is on its back side. When false,
     * rendering is skipped from that side.
     *
     * @since 1.6.5
     */
    public boolean renderBack;
    /**
     * Whether the surface is depth tested against the world. When false, it is drawn
     * over the world instead.
     *
     * @since 1.6.5
     */
    public boolean cull;

    /**
     * Selects full-bright, world-sampled, or custom lighting for the surface's elements.
     *
     * @since 2.0.0
     */
    private enum LightMode { FULL_BRIGHT, WORLD, CUSTOM }

    private LightMode lightMode = LightMode.FULL_BRIGHT;
    private int customLight = 0xF000F0;

    /**
     * Initializes a surface with world position, rotations, block dimensions, and rendering options.
     * The position and rotation are in world coordinates; the position is the panel's top-left corner,
     * and rotation components are degrees. The three position objects are retained by reference and
     * must be non-null. Subdivisions are clamped to at least {@code 1}; initialization computes the
     * pixel scale and clears/runs the inherited initialization behavior, but does not add this surface
     * to a {@link Draw3D} overlay.
     *
     * @param pos panel's top-left world position, in blocks
     * @param rotations rotations about x, y, and z, in degrees
     * @param sizes physical width and height, in blocks
     * @param minSubdivisions subdivisions across the smaller side, clamped to at least {@code 1}
     * @param renderBack whether to draw the surface when viewed from its back side
     * @param cull whether depth testing against the world is enabled
     */
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
     * Updates this surface's position coordinates from {@code pos}.
     *
     * @param pos position in world blocks
     * @return this surface for chaining
     * @since 1.8.4
     */
    public Surface setPos(Pos3D pos) {
        this.pos.x = pos.x;
        this.pos.y = pos.y;
        this.pos.z = pos.z;
        return this;
    }

    /**
     * Updates this surface's position coordinates from {@code pos}.
     *
     * @param pos block position in the world
     * @return this surface for chaining
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
     * Binds this surface to an entity; while that entity is alive, its interpolated
     * position plus {@link #boundOffset} determines the render position. Passing
     * {@code null} unbinds it.
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
     * Returns the entity this surface follows, if any.
     * @return the bound entity, or {@code null} if none is set
     * @since 1.8.4
     */
    @Nullable
    public EntityHelper<?> getBoundEntity() {
        return boundEntity;
    }

    /**
     * Sets the entity-following offset by reference, in blocks.
     *
     * @param boundOffset offset from the entity's interpolated position
     * @return this surface for chaining
     * @since 1.8.4
     */
    public Surface setBoundOffset(Pos3D boundOffset) {
        this.boundOffset = boundOffset;
        return this;
    }

    /**
     * Sets the offset added to the bound entity's interpolated position, in blocks.
     * Has no effect while the surface is unbound. Replaces the offset object.
     *
     * @param x the x offset in blocks
     * @param y the y offset in blocks
     * @param z the z offset in blocks
     * @return this surface
     * @since 1.8.4
     */
    public Surface setBoundOffset(double x, double y, double z) {
        this.boundOffset = new Pos3D(x, y, z);
        return this;
    }

    /**
     * Returns the offset added to the bound entity's interpolated position, in blocks.
     * The returned object is the surface's live offset; mutating it changes the offset.
     *
     * @return the bound-entity offset
     * @since 1.8.4
     */
    public Pos3D getBoundOffset() {
        return boundOffset;
    }

    /**
     * Sets whether the surface is rotated to face the camera during rendering.
     * When enabled, this overwrites the configured rotations each frame.
     *
     * @param rotateToPlayer whether to face the camera
     * @return this surface
     * @since 1.8.4
     */
    public Surface setRotateToPlayer(boolean rotateToPlayer) {
        this.rotateToPlayer = rotateToPlayer;
        return this;
    }

    /**
     * @return {@code true} if the surface is configured to face the camera
     * @since 1.8.4
     */
    public boolean doesRotateToPlayer() {
        return rotateToPlayer;
    }

    /**
     * Sets the surface's rotations in degrees.
     * <p>
     * The rotations are applied in the order y, then x, then z, so the y rotation is
     * the one that swings the panel around to face a compass direction and the other
     * two tilt it. They turn about the panel's corner unless
     * {@link #rotateCenter} is on, in which case they turn about its middle.
     * <p>
     * Angles are not range-clamped. These rotations are overwritten during rendering
     * while {@link #rotateToPlayer} is enabled.
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
     * Sets the surface's physical width and height in blocks.
     * <p>
     * This is the physical size of the panel in the world, not how much room its
     * elements have: the pixels they are measured in are subdivisions of this, so
     * making the panel bigger with the subdivision count fixed gives each pixel more
     * physical space.
     * The size object is updated in place, and the pixel-to-block scale is recomputed.
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
     * Returns this surface's physical width and height in blocks.
     * <p>
     * A copy rather than the surface's own size object, so writing to what this
     * returns does not resize the surface. To change the size, use
     * {@link #setSizes(double, double)}.
     * <p>
     * These are blocks. The number of surface pixels the elements are measured in is
     * {@link #getWidth()} and {@link #getHeight()}, which are subdivisions of this.
     *
     * @return the size of this surface, in blocks
     * @since 1.6.5
     */
    public Pos2D getSizes() {
        return sizes.add(0, 0);
    }

    /**
     * Sets the number of subdivisions across the smaller side of this surface.
     * <p>
     * The count is across the <em>smaller</em> side; the larger side gets however many
     * that works out to. A value of {@code 0} or below is raised to {@code 1}, so a
     * surface can be made to have a single pixel but never none.
     * The pixel scale is recomputed after the count is set.
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
     * Returns the subdivision count across this surface's smaller side.
     * <p>
     * This is the number of surface pixels across the smaller side, so it is what the
     * element coordinates are measured in on that axis. It is never below {@code 1}.
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
     * Makes elements use the block and sky light at the surface's render position, sampled
     * each frame. Sky light is reduced by the world's sky darkening; cast shadows are not
     * modelled. If no world is loaded, this uses full-bright lighting.
     *
     * @return self for chaining.
     * @since 2.0.0
     */
    public Surface setWorldLight() {
        this.lightMode = LightMode.WORLD;
        return this;
    }

    /**
     * Sets fixed block and sky light levels for all elements on this surface.
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
     * Returns the surface height in pixels. The smaller side has
     * {@link #getMinSubdivisions()} pixels; the other side scales with the surface's
     * aspect ratio. The result is truncated to an integer.
     *
     * @return the height in surface pixels
     * @since 1.6.5
     */
    @Override
    public int getHeight() {
        return (int) (sizes.y / scale);
    }

    /**
     * Returns the surface width in pixels. The smaller side has
     * {@link #getMinSubdivisions()} pixels; the other side scales with the surface's
     * aspect ratio. The result is truncated to an integer.
     *
     * @return the width in surface pixels
     * @since 1.6.5
     */
    @Override
    public int getWidth() {
        return (int) (sizes.x / scale);
    }

    /**
     * @param rotateCenter whether rotations use the surface center rather than its corner as pivot
     * @return self for chaining.
     * @since 1.8.4
     */
    public Surface setRotateCenter(boolean rotateCenter) {
        this.rotateCenter = rotateCenter;
        return this;
    }

    /**
     * @return whether rotations use the surface center as their pivot
     * @since 1.8.4
     */
    public boolean isRotatingCenter() {
        return rotateCenter;
    }

    /**
     * Recomputes the pixel scale, clears the elements, and runs the init function if set.
     *
     * @since 1.6.5
     */
    @Override
    public void init() {
        recomputeScale();
        super.init();
    }

    /**
     * Surfaces do not use a z-index for ordering; this method always returns {@code 0}.
     *
     * @return always {@code 0}
     * @since 1.6.5
     */
    @Override
    public int getZIndex() {
        return 0;
    }

    /**
     * Tests equality using the exact runtime class and the current position, rotations, and size only.
     * These mutable values determine the result; subdivision and rendering settings are ignored.
     *
     * @param o object to compare
     * @return {@code true} if {@code o} is this surface or has the same runtime class and equal position, rotations, and size; {@code false} for {@code null} or a different class
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Surface surface = (Surface) o;
        return Objects.equals(pos, surface.pos) && Objects.equals(rotations, surface.rotations) && Objects.equals(sizes, surface.sizes);
    }

    /**
     * Computes a hash from the surface's current position, rotations, and size; changes to those mutable values can change the result.
     *
     * @return hash code derived from the current position, rotations, and size
     */
    @Override
    public int hashCode() {
        return Objects.hash(pos, rotations, sizes);
    }

    /**
     * Compares surfaces by position, then rotations, then size. Surfaces matching on
     * all three compare equal; this compares those values, not spatial distance.
     *
     * @param other another surface to order this one against
     * @return a negative number, zero, or a positive number as this surface sorts before,
     *         equal to, or after {@code other}
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
     * Returns whether the camera is on the negative local-Z side of the surface plane.
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

    /**
     * This override does nothing; it does not render the surface or use its arguments.
     * @param drawContext graphics context, unused
     * @param mouseX mouse x-coordinate, unused
     * @param mouseY mouse y-coordinate, unused
     * @param delta frame delta, unused
     */
    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        // This does nothing I guess?
    }

    /**
     * Builder for configuring and creating a {@link Surface}.
     * <p>
     * A chain of setters, then {@link #build()} for a surface that is not put anywhere
     * or {@link #buildAndAdd()} for one added to the 3D overlay it was made from. A
     * surface on its own draws nothing either way; the overlay has to be registered
     * before it is drawn.
     * <p>
     * The defaults are a surface ten blocks on a side at the origin, rotated about its
     * middle, with its back drawn, not depth tested, drawn over everything, lit at full
     * brightness and not following anybody. Ten blocks is a large default for a panel
     * whose elements are measured in pixels, so the size is normally set explicitly.
     * <p>
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

        /**
         * Creates a builder with origin position, zero rotations, a {@code 10} by {@code 10} block size,
         * and one subdivision across the smaller side. The parent is retained for {@link #buildAndAdd()};
         * a {@code null} parent allows detached {@link #build()} calls, but {@code buildAndAdd()} then
         * throws {@link NullPointerException} when it attempts to add the surface.
         *
         * @param parent overlay used by {@code buildAndAdd()}, or {@code null} for detached builds
         */
        public Builder(Draw3D parent) {
            this.parent = parent;
        }

        /**
         * Sets the panel's top-left corner in world blocks. The builder retains {@code pos}
         * by reference, but each built surface receives a coordinate copy.
         * <pre>
         * const draw = Hud.createDraw3D();
         * const where = PositionCommon.createPos(0.5, 64, 0.5);
         * draw.surfaceBuilder().pos(where).size(1, 1).buildAndAdd();
         * // changing this position affects future builds, not the surface already built
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
         * Sets the panel's top-left corner to the block position's coordinates.
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
         * Sets the panel's top-left corner in world blocks. Each built surface receives
         * its own copy of these coordinates.
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
         * Returns the builder's position object, initially {@code 0, 0, 0}. If set with
         * {@link #pos(Pos3D)}, it is the same object; edits affect future builds only.
         * @return the builder's position
         * @since 1.8.4
         */
        public Pos3D getPos() {
            return pos;
        }

        /**
         * Sets the entity to follow. A live entity's interpolated position plus the
         * configured offset determines the render position; otherwise the configured
         * builder position is used. Passing {@code null} leaves the surface unbound.
         * <pre>
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
         * Returns the entity configured for the surface, if any.
         * @return the bound entity, or {@code null} if none is set
         * @since 1.8.4
         */
        @Nullable
        public EntityHelper<?> getBoundEntity() {
            return boundEntity;
        }

        /**
         * Sets the entity-following offset in blocks, retained by reference and shared
         * with surfaces built by this builder. It has no effect while unbound.
         * <pre>
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
         * Sets the entity-following offset in blocks, creating a new position object
         * rather than sharing one supplied by the caller.
         * <pre>
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
         * Returns the builder's offset object, initially {@code 0, 0, 0}. Built surfaces
         * share this object, so mutating it changes their bound offset.
         * @return the entity-following offset in blocks
         * @since 1.8.4
         */
        public Pos3D getBoundOffset() {
            return boundOffset;
        }

        /**
         * Sets the x-axis rotation in degrees. Rotations are applied in y, x, then z order.
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
         * Returns this builder's x-axis rotation; it defaults to {@code 0} and is independent of built surfaces.
         *
         * @return the x rotation of the surface.
         * @since 1.8.4
         */
        public double getXRotation() {
            return xRot;
        }

        /**
         * Sets the y-axis rotation in degrees; it is applied before the x- and z-axis rotations.
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
         * Returns this builder's y-axis rotation; it defaults to {@code 0}.
         *
         * @return the y rotation of the surface.
         * @since 1.8.4
         */
        public double getYRotation() {
            return yRot;
        }

        /**
         * Sets the z-axis rotation in degrees; it is applied after the y- and x-axis rotations.
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
         * Returns this builder's z-axis rotation; it defaults to {@code 0}.
         *
         * @return the z rotation of the surface.
         * @since 1.8.4
         */
        public double getZRotation() {
            return zRot;
        }

        /**
         * Sets all three rotations in degrees, in x, y, z argument order. They are applied in y, x, z order.
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
         * Sets whether rotations, camera-facing orientation, and nested 2D overlays pivot around the center. Defaults to {@code true} for this builder.
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
         * Returns whether this builder uses the panel center as the rotation pivot; defaults to {@code true}.
         *
         * @return {@code true} if this surface should be rotated around its center,
         * @since 1.8.4
         */
        public boolean isRotatingCenter() {
            return rotateCenter;
        }

        /**
         * Enables camera-facing rotation, which replaces the configured rotations each frame. Defaults to {@code false}.
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
         * Returns whether the builder enables camera-facing rotation. It defaults to
         * {@code false}; when enabled, configured rotations are replaced during rendering.
         *
         * @return {@code true} if camera-facing rotation is enabled
         * @since 1.8.4
         */
        public boolean doesRotateToPlayer() {
            return rotateToPlayer;
        }

        /**
         * Sets the panel's physical width in blocks; this is independent of its element
         * coordinate width. The default is {@code 10} blocks.
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
         * Returns the builder's physical width in blocks, initially {@code 10}; this is
         * distinct from a built surface's width in pixels.
         *
         * @return the width of the surface.
         * @since 1.8.4
         */
        public double getWidth() {
            return width;
        }

        /**
         * Sets the panel's physical height in blocks, independent of its element
         * coordinate height. The default is {@code 10} blocks.
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
         * Returns the builder's physical height in blocks, initially {@code 10}; this is
         * distinct from a built surface's height in pixels.
         *
         * @return the height of the surface.
         * @since 1.8.4
         */
        public double getHeight() {
            return height;
        }

        /**
         * Sets the panel's physical width and height in blocks. Element coordinate
         * dimensions are determined by {@link #minSubdivisions(int)}.
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
         * Sets the number of surface pixels across the smaller side, which determines
         * the element coordinate scale. Defaults to {@code 1}; values below {@code 1}
         * are retained by the builder but clamped to {@code 1} on the built surface.
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
         * Returns the builder's subdivision count, initially {@code 1}. Values below
         * {@code 1} are reported unchanged here, though the built surface clamps them.
         *
         * @return the minimum number of subdivisions.
         * @since 1.8.4
         */
        public int getMinSubdivisions() {
            return minSubdivisions;
        }

        /**
         * Sets whether the surface is drawn when the camera is on its back side. The
         * default is {@code true}.
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
         * Returns whether this builder enables rendering from the back side. It defaults
         * to {@code true}.
         *
         * @return {@code true} if the back should be rendered
         * @since 1.8.4
         */
        public boolean shouldRenderBack() {
            return renderBack;
        }

        /**
         * Enables depth testing, allowing terrain to obscure the surface. The default
         * is {@code false}, which draws it over the world.
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
         * Returns whether depth testing is enabled for surfaces built by this builder.
         * The default is {@code false}.
         *
         * @return {@code true} if depth testing is enabled
         * @since 1.8.4
         */
        public boolean isCulled() {
            return cull;
        }

        /**
         * Sets the depth offset per z-index step for elements on the surface, in blocks.
         * The default is {@code 0.001}; it does not position the surface itself.
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
         * Returns this builder's z-index scale, initially {@code 0.001}.
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
         * Samples block and sky light each frame at the surface's render position. If no
         * world is loaded, lighting is full bright; sky light accounts for sky darkening.
         *
         * @return self for chaining.
         * @since 2.0.0
         */
        public Builder worldLight() {
            this.lightMode = LightMode.WORLD;
            return this;
        }

        /**
         * Sets fixed block and sky light levels for elements on the built surface.
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
         * Builds the surface and adds it to the originating overlay. It draws when that
         * overlay is registered. Subdivision counts below {@code 1} are clamped on the
         * built surface, not on this builder.
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
         * Builds an unattached surface. Position, rotation, and size are copied;
         * the surface shares the builder's current bound-offset object, so mutating
         * that object affects both, but later replacing the builder's offset does
         * not change the built surface. Other builder settings are copied. Add the
         * result to a 3D overlay before it can draw.
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
                    new Pos3D(pos.x, pos.y, pos.z),
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
