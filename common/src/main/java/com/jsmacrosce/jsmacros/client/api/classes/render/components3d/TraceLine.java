package com.jsmacrosce.jsmacros.client.api.classes.render.components3d;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.util.ColorUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
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
 * a line from the crosshair to a point in the world.
 * <p>
 * The start of it is never set by a script. Every frame the line is drawn, the end
 * nearest the camera is moved to just in front of the camera, along the direction the
 * screen centre points in, and the other end stays where the script put it. That makes
 * this a reticle: it reads as a ray out of the player's view rather than as a fixed
 * segment, and it follows the camera as the player looks around without anything
 * having to update it.
 * <p>
 * Because the start is rewritten every frame, the line's own first position is a
 * derived value rather than something to set or read. {@link #getPos()} is the target,
 * which is the part that is the script's.
 * <p>
 * The crosshair direction is worked out from the camera's angles, and when view bob is
 * on the bobbing transform is undone as well, so the line stays on the crosshair while
 * the player walks. That is done in camera space, so the result is a world direction
 * and the line runs to whatever the crosshair is over rather than to a point a block
 * in front of the camera.
 * <p>
 * Lines made by the constructors that do not take an {@code alwaysOnTop} argument come
 * out drawn on top of everything, terrain included. The ones that do take the argument
 * are the only way to get a depth tested line from a constructor;
 * {@link #setAlwaysOnTop(boolean)} changes an existing line either way.
 * example:
 * <pre>
 * const draw = Hud.createDraw3D();
 * // a red line from the crosshair to a block, hidden by terrain in front of it
 * draw.addTraceLine(0, 64, 0, 0xFFFF0000).setAlwaysOnTop(false);
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

    public TraceLine(double x, double y, double z, int color) {
        render = new Line3D(0, 0, 0, x, y, z, color, false);
    }

    public TraceLine(double x, double y, double z, int color, int alpha) {
        render = new Line3D(0,0,0, x, y, z, color, alpha, false);
    }

    public TraceLine(double x, double y, double z, int color, int alpha, boolean alwaysOnTop) {
        render = new Line3D(0, 0, 0, x, y, z, color, alpha, !alwaysOnTop);
    }

    public TraceLine(Pos3D pos, int color) {
        render = new Line3D(0, 0, 0, pos.getX(), pos.getY(), pos.getZ(), color, false);
    }

    public TraceLine(Pos3D pos, int color, int alpha) {
        render = new Line3D(0, 0, 0, pos.getX(), pos.getY(), pos.getZ(), color, alpha, false);
    }

    public TraceLine(Pos3D pos, int color, int alpha, boolean alwaysOnTop) {
        render = new Line3D(0, 0, 0, pos.getX(), pos.getY(), pos.getZ(), color, alpha, !alwaysOnTop);
    }

    /**
     * moves the target this line points at.
     * <p>
     * Only the far end moves. The near end is put back in front of the camera on the
     * next frame whatever this is set to, so there is no way to pin a trace line to a
     * fixed start; use a {@link Line3D} for that.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addTraceLine(0, 64, 0, 0xFFFF0000);
     * // move where the crosshair ray lands, as the player moves
     * line.setPos(10, 70, -4);
     * draw.register();
     * </pre>
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
     * moves the target this line points at.
     * <p>
     * The same as the three number form. The position is read straight off, so a
     * position that is changed afterwards does not move the line with it.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addTraceLine(0, 64, 0, 0xFFFF0000);
     * line.setPos(PositionCommon.createPos(10, 70, -4));
     * draw.register();
     * </pre>
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
     * This is the end away from the camera, which is the end a script sets. It comes
     * back as a new position, so changing it does not move the line.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addTraceLine(0, 64, 0, 0xFFFF0000);
     * Chat.log(`line points at ${line.getPos()}`);
     * </pre>
     *
     * @return the position of the line's target.
     * @since 2.0.0
     */
    public Pos3D getPos() {
        return render.getPos2();
    }

    /**
     * the colour of this line, taken on its own.
     * <p>
     * A colour with no alpha of its own is given one: if the alpha byte is zero and
     * there is some red, green or blue, it becomes {@code 0xFF}. A colour that is
     * {@code 0x00000000} is left alone, since there is nothing to make visible, and
     * so is a colour that already carries a non-zero alpha.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addTraceLine(0, 64, 0, 0xFFFF0000);
     * // no alpha in this one, so it comes out fully opaque
     * line.setColor(0x00FF00);
     * draw.register();
     * </pre>
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
     * the colour of this line, with the alpha given separately.
     * <p>
     * Nothing is filled in here, so an alpha of {@code 0} really does make the line
     * invisible rather than being treated as a missing one.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addTraceLine(0, 64, 0, 0xFFFF0000);
     * line.setColor(0x00FF00, 128);
     * draw.register();
     * </pre>
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
     * The alpha is not part of this; it is masked out, so a line drawn fully opaque
     * and a line drawn at half alpha with the same colour read back the same. Use
     * {@link #getAlpha()} for the other half.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addTraceLine(0, 64, 0, 0xFFFF0000);
     * // no alpha byte in the result, whatever the line is drawn at
     * Chat.log(`colour 0x${line.getColor().toString(16)}, alpha ${line.getAlpha()}`);
     * </pre>
     *
     * @return the color of the line.
     * @since 2.0.0
     */
    public int getColor() {
        return render.getColor();
    }

    /**
     * how transparent this line is drawn, from 0 to 255.
     * <p>
     * This only replaces the alpha byte and leaves the colour alone, so it can be
     * used on its own to fade a line that already has a colour. A value of {@code 0}
     * makes the line invisible, and nothing fills it in the way {@link #setColor(int)}
     * fills in a missing alpha.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addTraceLine(0, 64, 0, 0xFFFF0000);
     * line.setAlpha(64);
     * draw.register();
     * </pre>
     *
     * @param alpha the alpha value for the line's color
     * @return self for chaining
     * @since 1.9.0
     */
    public TraceLine setAlpha(int alpha) {
        return setColor(render.color, alpha);
    }

    /**
     * From 0 to 255, where 255 is fully opaque. This is the half that
     * {@link #getColor()} leaves out, so the two together are the whole colour.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addTraceLine(0, 64, 0, 0xFFFF0000);
     * // whatever alpha the colour carried is what comes back here
     * Chat.log(`drawn at alpha ${line.getAlpha()}`);
     * </pre>
     *
     * @return the alpha value of the line's color.
     * @since 2.0.0
     */
    public int getAlpha() {
        return render.getAlpha();
    }

    /**
     * This is the opposite of the underlying line's {@code cull} flag, so turning this
     * on is the same as turning that off. A line that is on top draws over terrain as
     * well as over other lines, which is what a crosshair ray usually wants.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addTraceLine(0, 64, 0, 0xFFFF0000);
     * // on top by default from addTraceLine; make this one depth tested instead
     * line.setAlwaysOnTop(false);
     * draw.register();
     * </pre>
     *
     * @param alwaysOnTop whether the line should render on top of everything else.
     * @since 2.0.0
     */
    public void setAlwaysOnTop(boolean alwaysOnTop) {
        render.setAlwaysOnTop(alwaysOnTop);
    }

    /**
     * The inverse of the underlying line's {@code cull} flag, and the default a line
     * gets from the constructors that take no {@code alwaysOnTop} argument is
     * {@code true}.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const line = draw.addTraceLine(0, 64, 0, 0xFFFF0000);
     * Chat.log(`draws over terrain: ${line.isAlwaysOnTop()}`);
     * </pre>
     *
     * @return whether the line renders on top of everything else.
     * @since 2.0.0
     */
    public boolean isAlwaysOnTop() {
        return render.isAlwaysOnTop();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TraceLine traceLine = (TraceLine) o;
        return Objects.equals(render.pos.getEnd(), traceLine.render.pos.getEnd());
    }

    @Override
    public int hashCode() {
        return Objects.hash(render.pos.getEnd());
    }

    /**
     * orders this trace line against another one, on where they point.
     * <p>
     * Only ever reached for two trace lines, since the sort compares class names first
     * and only ties get here. What it looks at is the target, the end away from the
     * camera, and not the end that is recomputed from the view every frame. Two lines
     * pointing at the same place therefore compare equal whatever their colours are.
     * <p>
     * A line that has not been drawn yet has its target at the origin until its first
     * frame, so two freshly built lines compare equal until they have pointed somewhere.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * const a = draw.traceLineBuilder().pos(0, 64, 0).color(0xFFFF0000).build();
     * const b = draw.traceLineBuilder().pos(10, 64, 0).color(0xFF00FF00).build();
     * Chat.log(`a sorts before b: ${a.compareToSame(b) < 0}`);
     * </pre>
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
     * aims the line at where the crosshair is pointing and draws it.
     * <p>
     * The start is put one unit along the crosshair direction from the camera, so the
     * line does not begin inside the player's own head, and the end is left where the
     * script put it. The crosshair direction accounts for the camera's angles and, when
     * the player is walking with view bob on, for the bobbing transform as well, so
     * the line stays under the crosshair rather than drifting off it.
     * <p>
     * This also rewrites the line's own first position, which is why reading that back
     * after a frame gives a point in front of the camera rather than the origin.
     * <p>
     * This is called by the renderer once a frame. There is no reason for a script to
     * call it.
     *
     * @param matrixStack the world transform, already translated so the camera is at the origin
     * @param consumers the buffer source the geometry goes into
     * @param tickDelta how far into the current tick this frame is, from 0 to 1
     * @author aMelonRind
     * @since 1.9.0
     */
    @Override
    public void render(PoseStack matrixStack, MultiBufferSource consumers, float tickDelta) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = CameraCompat.position(camera);

        Vec3 lookDir = getCrosshairDirection(camera, tickDelta);
        Vec3 p1 = cameraPos.add(lookDir);

        render.setPos(p1.x, p1.y, p1.z, render.pos.x2, render.pos.y2, render.pos.z2);
        render.render(matrixStack, consumers, tickDelta);
    }

    /**
     * Returns the world-space direction from the camera that corresponds to the screen-centre
     * crosshair, accounting for the view-bob projection transform applied by GameRenderer.
     * <p>
     * bobView() bakes a translation, Z-roll, and X-pitch into the projection matrix
     * (in camera space). The full bob transform is:
     * <ol>
     *   <li>translate(sin(dist*pi)*bob*0.5, -|cos(dist*pi)*bob|, 0)</li>
     *   <li>Axis.ZP.rotationDegrees(sin(dist*pi)*bob*3)</li>
     *   <li>Axis.XP.rotationDegrees(|cos(dist*pi-0.2)*bob|*5)</li>
     * </ol>
     * Both the rotation and the translation must be compensated: the rotation changes
     * which camera-space direction maps to screen centre, and the translation shifts
     * the effective viewpoint so the perspective origin is offset.
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
     * collects the values for a {@link TraceLine}, then builds it.
     * <p>
     * The chain of setters, then {@link #build()} for a line that is not put anywhere
     * or {@link #buildAndAdd()} for one that is added to the overlay it was made
     * from. The defaults are a white line, fully opaque, always on top, pointing at
     * {@code 0, 0, 0}.
     * <p>
     * The colour setters keep the colour and the alpha apart internally, which is why
     * there is a separate {@link #alpha(int)}: {@link #color(int)} on its own picks
     * the alpha up out of the colour it is given.
     * example:
     * <pre>
     * const draw = Hud.createDraw3D();
     * draw.traceLineBuilder()
     *    .pos(0, 64, 0)
     *    .color(0xFF00FF00, 128)
     *    .alwaysOnTop(false)
     *    .buildAndAdd();
     * draw.register();
     * </pre>
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

        public Builder(Draw3D parent) {
            this.parent = parent;
        }

        /**
         * This is the end away from the camera. The end at the camera is worked out
         * every frame from where the player is looking, so it is not something this
         * builder sets.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.traceLineBuilder().pos(PositionCommon.createPos(0, 64, 0)).buildAndAdd();
         * draw.register();
         * </pre>
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
         * The block's own position, without its offset, so this points at the corner
         * of the block rather than at the middle of it.
         * example:
         * <pre>
         * const player = Player.getPlayer();
         * if (player !== null) {
         *   const draw = Hud.createDraw3D();
         *   draw.traceLineBuilder().pos(player.getBlockPos()).buildAndAdd();
         *   draw.register();
         * }
         * </pre>
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
         * A whole number here is a block corner, not the middle of the block, so a
         * line to {@code 0, 64, 0} stops at the base of the block rather than inside it.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.traceLineBuilder().pos(0, 64, 0).buildAndAdd();
         * draw.register();
         * </pre>
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
         * This is the builder's own value, taken straight off, so it is
         * {@code 0, 0, 0} until something is set and does not move when a position
         * handed to {@link #pos(Pos3D)} is changed afterwards.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().traceLineBuilder();
         * builder.pos(0, 64, 0);
         * Chat.log(`points at ${builder.getPos()}`);
         * </pre>
         *
         * @return the position of the target
         * @since 1.9.0
         */
        public Pos3D getPos() {
            return pos;
        }

        /**
         * The alpha is read out of this colour rather than being left alone, so a
         * colour written with a zero alpha byte comes out opaque unless something set
         * the alpha separately afterwards.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().traceLineBuilder();
         * // half a red line: the alpha comes out of the colour
         * builder.color(0x80FF0000);
         * Chat.log(`colour 0x${builder.getColor().toString(16)}, alpha ${builder.getAlpha()}`);
         * </pre>
         *
         * @param color the color of the line
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder color(int color) {
            this.color = color;
            this.alpha = ColorUtil.fixAlpha(color) >>> 24;
            return this;
        }

        /**
         * The two are kept apart on the builder, so the colour is stored exactly as
         * given here and whatever alpha it happened to carry is not what the built
         * line ends up drawn at.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().traceLineBuilder();
         * builder.color(0xFF0000, 128);
         * Chat.log(`colour 0x${builder.getColor().toString(16)}, alpha ${builder.getAlpha()}`);
         * </pre>
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
         * Nothing is clamped, so a component over 255 carries into the one above it
         * rather than being cut off, and the alpha is left as the last colour or
         * {@link #alpha(int)} call set it.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().traceLineBuilder();
         * builder.color(255, 128, 0);
         * Chat.log(`colour 0x${builder.getColor().toString(16)}, alpha ${builder.getAlpha()}`);
         * </pre>
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
         * The same packing as the three argument form with the alpha kept apart. The
         * defaults are a colour of {@code 0xFFFFFF} and an alpha of {@code 0xFF}, so a
         * builder with nothing set on it builds a fully opaque white line.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().traceLineBuilder();
         * builder.color(0, 128, 255, 255);
         * Chat.log(`colour 0x${builder.getColor().toString(16)}, alpha ${builder.getAlpha()}`);
         * </pre>
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
         * The builder's own field, not anything read back off a built line. The two
         * differ: a built line's {@code getColor()} masks the alpha out, while this
         * returns whatever was stored, alpha byte included.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().traceLineBuilder();
         * // the default is plain white with no alpha in the number
         * Chat.log(`default colour 0x${builder.getColor().toString(16)}`);
         * </pre>
         *
         * @return the color of the line
         * @since 1.9.0
         */
        public int getColor() {
            return color;
        }

        /**
         * From 0 to 255, and independent of the colour: setting this does not touch
         * the colour and setting the colour afterwards does not touch this.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().traceLineBuilder();
         * builder.alpha(64);
         * Chat.log(`drawn at alpha ${builder.getAlpha()}`);
         * </pre>
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
         * The default is {@code true}, which is the same as what a line gets from the
         * constructors that take no {@code alwaysOnTop} argument. Turning it off lets
         * terrain hide the line.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.traceLineBuilder().pos(0, 64, 0).alwaysOnTop(false).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param alwaysOnTop whether the line should render on top of everything else.
         * @return self for chaining.
         * @since 2.0.0
         */
        public Builder alwaysOnTop(boolean alwaysOnTop) {
            this.alwaysOnTop = alwaysOnTop;
            return this;
        }

        /**
         * From 0 to 255, and {@code 255} until a {@code color} or {@code alpha} call
         * changes it.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().traceLineBuilder();
         * builder.color(0x80FF0000);
         * // the alpha came out of the colour, so this is 128
         * Chat.log(`alpha ${builder.getAlpha()}`);
         * </pre>
         *
         * @return the alpha value of the line's color
         * @since 1.9.0
         */
        public int getAlpha() {
            return alpha;
        }

        /**
         * Creates the trace line for the given values and adds it to the draw3D
         * <p>
         * The line goes into the same list any other trace line does, so
         * {@code Draw3D.getTraceLines()} reports it and {@code removeTraceLine} takes
         * it off again. It draws nothing until the overlay itself is registered.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * draw.traceLineBuilder().pos(0, 64, 0).color(0xFFFF0000).buildAndAdd();
         * draw.register();
         * </pre>
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
         * Builds the line from the given values
         * <p>
         * The line is not put anywhere, so it draws nothing until it is added to an
         * overlay. Its near end is the origin until the first frame, which puts it in
         * front of the camera.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * const line = draw.traceLineBuilder().pos(0, 64, 0).build();
         * // built but not added, so it is not in the overlay yet
         * draw.addTraceLine(line);
         * draw.register();
         * </pre>
         *
         * @return the build line
         * @since 1.9.0
         */
        public TraceLine build() {
            return new TraceLine(pos, color, alpha, alwaysOnTop);
        }

    }

}
