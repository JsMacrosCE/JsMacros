package com.jsmacrosce.jsmacros.client.api.classes.render.components3d;
import com.jsmacrosce.jsmacros.client.api.classes.render.RenderSink;

import com.jsmacrosce.jsmacros.client.util.RenderCompat;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.util.ColorMath;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.api.classes.render.Draw3D;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.util.CameraCompat;
import org.joml.Quaternionf;
import org.joml.Vector3f;

//? if >=1.21.10 {
/*import net.minecraft.client.entity.ClientAvatarState;
*///?}
//? if <=1.21.8 {
import net.minecraft.client.player.AbstractClientPlayer;
//?}

import java.util.Objects;

/**
 * A line from the camera toward a script-selected world-space target. The renderer
 * updates its camera-side endpoint each frame; {@link #getPos()} and {@link #setPos(double, double, double)}
 * refer to the target endpoint.
 * <p>
 * Constructors without an {@code alwaysOnTop} argument default to drawing over
 * terrain. Use an overload with that argument or {@link #setAlwaysOnTop(boolean)} to
 * control depth testing.
 * <pre>
 * const draw = Hud.createDraw3D();
 * // A red line from the camera toward this target.
 * draw.addTraceLine(0, 64, 0, Color.rgb(0xFF, 0x00, 0x00)).setAlwaysOnTop(false);
 * draw.register();
 * </pre>
 *
 * @author aMelonRind
 * @since 1.9.0
 */
@SuppressWarnings("unused")
@DocletCategory("Rendering/Graphics")
public class TraceLine implements RenderElement3D<TraceLine> {
    private final Line3D render;

    /**
     * Initializes a detached trace toward the target in world-space block coordinates.
     * Nonzero RGB with zero alpha becomes opaque, while zero-alpha black remains transparent.
     * Depth testing is disabled.
     *
     * @param x target x coordinate in blocks
     * @param y target y coordinate in blocks
     * @param z target z coordinate in blocks
     * @param color packed color; zero-alpha handling is described above
     */
    public TraceLine(double x, double y, double z, int color) {
        render = new Line3D(0, 0, 0, x, y, z, color, false);
    }

    /**
     * Initializes a detached trace toward the target in world-space block coordinates.
     * Alpha is shifted into the color's top byte without clamping or range validation;
     * depth testing is disabled.
     *
     * @param x target x coordinate in blocks
     * @param y target y coordinate in blocks
     * @param z target z coordinate in blocks
     * @param color RGB source; only its low 24 bits are used
     * @param alpha value shifted into the top byte
     */
    public TraceLine(double x, double y, double z, int color, int alpha) {
        render = new Line3D(0,0,0, x, y, z, color, alpha, false);
    }

    /**
     * Initializes a detached trace toward the target in world-space block coordinates.
     * Alpha is shifted into the top byte without clamping or range validation; {@code alwaysOnTop}
     * controls the inverse internal depth-test setting and does not ensure visibility.
     *
     * @param x target x coordinate in blocks
     * @param y target y coordinate in blocks
     * @param z target z coordinate in blocks
     * @param color RGB source; only its low 24 bits are used
     * @param alpha value shifted into the top byte
     * @param alwaysOnTop whether to disable depth testing
     */
    public TraceLine(double x, double y, double z, int color, int alpha, boolean alwaysOnTop) {
        render = new Line3D(0, 0, 0, x, y, z, color, alpha, !alwaysOnTop);
    }

    /**
     * Initializes a detached trace toward a copy of {@code pos}'s coordinates in world-space blocks.
     * A null position fails with {@link NullPointerException}; zero-alpha packed-color handling
     * follows the renderer's opaque-bare-RGB rule. Depth testing is disabled.
     *
     * @param pos target position; its coordinates are copied
     * @param color packed color; nonzero RGB with zero alpha becomes opaque, while zero-alpha black remains transparent
     */
    public TraceLine(Pos3D pos, int color) {
        render = new Line3D(0, 0, 0, pos.getX(), pos.getY(), pos.getZ(), color, false);
    }

    /**
     * Initializes a detached trace toward a copy of {@code pos}'s coordinates in world-space blocks.
     * A null position fails with {@link NullPointerException}. Alpha is shifted into the top byte
     * without clamping or range validation; depth testing is disabled.
     *
     * @param pos target position; its coordinates are copied
     * @param color RGB source; only its low 24 bits are used
     * @param alpha value shifted into the top byte
     */
    public TraceLine(Pos3D pos, int color, int alpha) {
        render = new Line3D(0, 0, 0, pos.getX(), pos.getY(), pos.getZ(), color, alpha, false);
    }

    /**
     * Initializes a detached trace toward a copy of {@code pos}'s coordinates in world-space blocks.
     * A null position fails with {@link NullPointerException}. Alpha is shifted into the top byte
     * without clamping or range validation; {@code alwaysOnTop} controls the inverse internal
     * depth-test setting and does not ensure visibility.
     *
     * @param pos target position; its coordinates are copied
     * @param color RGB source; only its low 24 bits are used
     * @param alpha value shifted into the top byte
     * @param alwaysOnTop whether to disable depth testing
     */
    public TraceLine(Pos3D pos, int color, int alpha, boolean alwaysOnTop) {
        render = new Line3D(0, 0, 0, pos.getX(), pos.getY(), pos.getZ(), color, alpha, !alwaysOnTop);
    }

    /**
     * Sets the target endpoint; the camera-side endpoint is recalculated during rendering.
     *
     * @param x the x coordinate of the target
     * @param y the y coordinate of the target
     * @param z the z coordinate of the target
     * @return self for chaining
     * @since 1.9.0
     */
    public TraceLine setPos(double x, double y, double z) {
        render.setPos(0, 0, 0, x, y, z);
        return this;
    }

    /**
     * Sets the target endpoint from the coordinates in {@code pos}; later changes to {@code pos} do not move the line.
     *
     * @param pos the position of the target
     * @return self for chaining
     * @since 1.9.0
     */
    public TraceLine setPos(Pos3D pos) {
        render.setPos(0, 0, 0, pos.x, pos.y, pos.z);
        return this;
    }

    /**
     * Returns a new position containing the target endpoint; modifying it does not move the line.
     *
     * @return the position of the line's target.
     * @since 2.0.0
     */
    public Pos3D getPos() {
        return render.getPos2();
    }

    /**
     * Sets the line colour, making non-black RGB opaque when its alpha byte is zero.
     * <p>
     * An all-zero colour stays transparent; use {@link #setColor(int, int)} to set alpha explicitly.
     *
     * @param color the color of the line
     * @return self for chaining
     * @since 1.9.0
     */
    public TraceLine setColor(int color) {
        render.setColor(color);
        return this;
    }

    /**
     * Sets the RGB colour and alpha separately; the alpha is packed into the top byte and is not clamped.
     *
     * @param color the color of the line
     * @param alpha the alpha value of the line's color
     * @return self for chaining
     * @since 1.9.0
     */
    public TraceLine setColor(int color, int alpha) {
        render.setColor(color, alpha);
        return this;
    }

    /**
     * Returns the RGB colour without its alpha byte; use {@link #getAlpha()} to read alpha.
     *
     * @return the color of the line.
     * @since 2.0.0
     */
    public int getColor() {
        return render.getColor();
    }

    /**
     * Replaces the alpha byte without changing RGB. Values are not clamped; zero makes the line transparent.
     *
     * @param alpha the alpha value for the line's color
     * @return self for chaining
     * @since 1.9.0
     */
    public TraceLine setAlpha(int alpha) {
        return setColor(render.color, alpha);
    }

    /**
     * Returns the alpha byte of the packed colour, from {@code 0} (transparent) to {@code 255} (opaque).
     *
     * @return the alpha value of the line's color.
     * @since 2.0.0
     */
    public int getAlpha() {
        return render.getAlpha();
    }

    /**
     * Sets whether depth testing is disabled ({@code true}, draws over terrain) or enabled ({@code false}).
     *
     * @param alwaysOnTop whether the line should render on top of everything else.
     * @since 2.0.0
     */
    public void setAlwaysOnTop(boolean alwaysOnTop) {
        render.setAlwaysOnTop(alwaysOnTop);
    }

    /**
     * Returns whether depth testing is disabled, so the line draws over terrain.
     * Constructors without an {@code alwaysOnTop} argument default to {@code true}.
     *
     * @return whether the line renders on top of everything else.
     * @since 2.0.0
     */
    public boolean isAlwaysOnTop() {
        return render.isAlwaysOnTop();
    }

    /**
     * Compares traces by target endpoint value only; the camera-side endpoint, color, and depth
     * setting are ignored. Only the same runtime class compares equal.
     *
     * @param o object to compare with
     * @return {@code true} for this object or a {@code TraceLine} of the exact same runtime class
     *         with an equal target endpoint; {@code false} for null or any other value
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TraceLine traceLine = (TraceLine) o;
        return Objects.equals(render.pos.getEnd(), traceLine.render.pos.getEnd());
    }

    /**
     * Computes a hash from the current target endpoint only; changing the target can change the result.
     *
     * @return hash derived from the current target endpoint
     */
    @Override
    public int hashCode() {
        return Objects.hash(render.pos.getEnd());
    }

    /**
     * Compares trace lines by their target endpoint's coordinates. Colour is not
     * considered; lines with the same target compare equal.
     *
     * @param other another trace line to order this one against
     * @return a negative number, zero, or a positive number as this line is before, the
     *         same as, or after {@code other}
     * @author aMelonRind
     * @since 1.9.0
     */
    @Override
    public int compareToSame(TraceLine other) {
        return render.pos.getEnd().compareTo(other.render.pos.getEnd());
    }

    /**
     * Updates the camera-side endpoint to the screen-centre crosshair direction, then
     * renders the line. Called by the renderer, not by scripts.
     *
     * @param poseStack the world transform, already translated so the camera is at the origin
     * @param sink the buffer source the geometry goes into
     * @param tickDelta how far into the current tick this frame is, from 0 to 1
     * @author aMelonRind
     * @since 1.9.0
     */
    @Override
    public void render(PoseStack poseStack, RenderSink sink, float tickDelta) {
        Camera camera = RenderCompat.mainCamera(Minecraft.getInstance());
        Vec3 cameraPos = CameraCompat.position(camera);

        Vec3 lookDir = getCrosshairDirection(camera, tickDelta);
        Vec3 p1 = cameraPos.add(lookDir);

        render.setPos(p1.x, p1.y, p1.z, render.pos.x2, render.pos.y2, render.pos.z2);
        render.render(poseStack, sink, tickDelta);
    }

    /**
     * Returns the world-space direction from the camera that maps to the screen-centre
     * crosshair, compensating for the view-bob translation and rotations used by
     * {@code GameRenderer.bobView()} on Minecraft 1.21.8.
     */
    private static Vec3 getCrosshairDirection(Camera camera, float tickDelta) {
        // Replicate the bob parameters from GameRenderer.bobView()
        float dist = 0.0f;
        float bob = 0.0f;
        if (Minecraft.getInstance().options.bobView().get() && Minecraft.getInstance().getCameraEntity() instanceof
                //? if >=1.21.10 {
                /*net.minecraft.client.player.AbstractClientPlayer player) {
            ClientAvatarState avatarState = player.avatarState();
            dist = avatarState.getBackwardsInterpolatedWalkDistance(tickDelta);
            bob  = avatarState.getInterpolatedBob(tickDelta);
            *///? } else {
                AbstractClientPlayer player)
        {
            float f3 = player.walkDist - player.walkDistO;
            dist = -(player.walkDist + f3 * tickDelta);
            bob = Mth.lerp(tickDelta, player.oBob, player.bob);
            //?}
        }

        if (bob == 0.0f) {
            // No bobbing active: the true camera forward is already screen-centre.
            return Vec3.directionFromRotation(CameraCompat.xRot(camera), CameraCompat.yRot(camera));
        }

        // Replicate the bob rotation angles from bobView():
        float zDeg = Mth.sin(dist * (float) Math.PI) * bob * 3.0f;
        float xDeg = Math.abs(Mth.cos(dist * (float) Math.PI - 0.2f) * bob) * 5.0f;

        // Replicate the bob translation from bobView():
        float tx = Mth.sin(dist * (float) Math.PI) * bob * 0.5f;
        float ty = -Math.abs(Mth.cos(dist * (float) Math.PI) * bob);

        // The full bob matrix is M_bob = T(tx,ty,0) * R_Z(zDeg) * R_X(xDeg).
        // For a world-space point p1 = cameraPos + d to appear at screen centre,
        // we need M_bob * V_rot * d to lie along (0, 0, -1), so
        // d = camera.rotation() * invBob * (-tx, -ty, -1).
        // (z=1 is arbitrary: only the direction matters.)
        Quaternionf invBob =
                new Quaternionf().rotateX((float) Math.toRadians(-xDeg)).rotateZ((float) Math.toRadians(-zDeg));
        Vector3f camDir = invBob.transform(new Vector3f(-tx, -ty, -1.0f));

        // Rotate from camera space to world space using the camera's orientation.
        camera.rotation().transform(camDir);

        return new Vec3(camDir.x, camDir.y, camDir.z);
    }

    /**
     * Configures a trace line before building it. Defaults are a white, fully opaque
     * line targeting the origin and drawn over terrain. {@link #build()} returns a
     * detached line; {@link #buildAndAdd()} adds it to this builder's overlay.
     *
     * @author aMelonRind
     * @since 1.9.0
     */
    @DocletCategory("Rendering/Graphics")
    public static class Builder {
        private final Draw3D parent;

        private Pos3D pos = new Pos3D(0.0, 0.0, 0.0);
        private int color = 0xFFFFFF;
        private int alpha = 0xFF;
        private boolean alwaysOnTop = true;

        /**
         * Creates a builder with an origin target, opaque white color, and {@code alwaysOnTop} enabled.
         * The parent is stored without validation and is used only by {@link #buildAndAdd()}; it may
         * be null for detached {@link #build()} calls, but {@code buildAndAdd()} then fails with
         * {@link NullPointerException}.
         *
         * @param parent overlay for {@code buildAndAdd()}, or null when only building detached lines
         */
        public Builder(Draw3D parent) {
            this.parent = parent;
        }

        /**
         * Sets the world-space target endpoint; the camera-side endpoint is computed
         * each render frame.
         *
         * @param pos the position of the target
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder pos(Pos3D pos) {
            this.pos = pos;
            return this;
        }

        /**
         * Sets the target to the block position's integer coordinates (the block
         * corner, with no half-block offset).
         *
         * @param pos the position of the target
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder pos(BlockPosHelper pos) {
            this.pos = pos.toPos3D();
            return this;
        }

        /**
         * Sets the target using block coordinates; integer coordinates identify block
         * corners, not centers.
         *
         * @param x the x coordinate of the target
         * @param y the y coordinate of the target
         * @param z the z coordinate of the target
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder pos(int x, int y, int z) {
            this.pos = new Pos3D(x, y, z);
            return this;
        }

        /**
         * Returns the target position held by the builder. It defaults to the origin;
         * a position passed to {@link #pos(Pos3D)} is retained by reference.
         *
         * @return the position of the target
         * @since 1.9.0
         */
        public Pos3D getPos() {
            return pos;
        }

        /**
         * Sets the packed colour and derives alpha from its top byte after applying the
         * renderer's zero-alpha rule; non-black RGB with a zero alpha becomes opaque.
         *
         * @param color the color of the line
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder color(int color) {
            this.color = color;
            this.alpha = ColorMath.fixAlpha(color) >>> 24;
            return this;
        }

        /**
         * Stores the colour integer unchanged and sets alpha separately; neither value is clamped.
         *
         * @param color the color of the line
         * @param alpha the alpha value of the line's color
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder color(int color, int alpha) {
            this.color = color;
            this.alpha = alpha;
            return this;
        }

        /**
         * Sets packed RGB using {@code (r << 16) | (g << 8) | b}; values are not clamped and alpha is unchanged.
         *
         * @param r the red component of the color
         * @param g the green component of the color
         * @param b the blue component of the color
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder color(int r, int g, int b) {
            this.color = (r << 16) | (g << 8) | b;
            return this;
        }

        /**
         * Sets packed RGB as in {@link #color(int, int, int)} and sets alpha separately; values are not clamped.
         *
         * @param r the red component of the color
         * @param g the green component of the color
         * @param b the blue component of the color
         * @param a the alpha value of the color
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder color(int r, int g, int b, int a) {
            this.color = (r << 16) | (g << 8) | b;
            this.alpha = a;
            return this;
        }

        /**
         * Returns the colour integer stored by the builder unchanged.
         *
         * @return the color of the line
         * @since 1.9.0
         */
        public int getColor() {
            return color;
        }

        /**
         * Sets alpha independently of the stored colour integer; the value is not clamped.
         *
         * @param alpha the alpha value for the line's color
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder alpha(int alpha) {
            this.alpha = alpha;
            return this;
        }

        /**
         * Sets whether the line is drawn over terrain; the default is {@code true}.
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.traceLineBuilder().pos(0, 64, 0).alwaysOnTop(false).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param alwaysOnTop whether terrain depth should be ignored when drawing the line
         * @return self for chaining
         * @since 2.0.0
         */
        public Builder alwaysOnTop(boolean alwaysOnTop) {
            this.alwaysOnTop = alwaysOnTop;
            return this;
        }

        /**
         * Returns the alpha value stored by the builder, initially {@code 255}. {@link #color(int)} derives it from the supplied packed colour.
         *
         * @return the alpha value of the line's color
         * @since 1.9.0
         */
        public int getAlpha() {
            return alpha;
        }

        /**
         * Builds the line and adds it to this builder's overlay. It renders only while the overlay is registered.
         *
         * @return the build line
         * @since 1.9.0
         */
        public TraceLine buildAndAdd() {
            TraceLine line = build();
            parent.addTraceLine(line);
            return line;
        }

        /**
         * Builds a detached line; add it to an overlay before it can render.
         *
         * @return the build line
         * @since 1.9.0
         */
        public TraceLine build() {
            return new TraceLine(pos, color, alpha, alwaysOnTop);
        }

    }

}
