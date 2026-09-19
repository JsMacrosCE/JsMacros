package com.jsmacrosce.jsmacros.client.api.classes.render.components3d;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
//? if >=26.1 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
*///?}
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.api.classes.render.Draw3D;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
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
 * @author aMelonRind
 * @since 1.9.0
 */
@SuppressWarnings("unused")
public class TraceLine implements RenderElement3D<TraceLine> {
    private final Line3D render;

    public TraceLine(double x, double y, double z, int color) {
        render = new Line3D(0, 0, 0, x, y, z, color, false);
    }

    public TraceLine(double x, double y, double z, int color, int alpha) {
        render = new Line3D(0,0,0, x, y, z, color, alpha, false);
    }

    public TraceLine(double x, double y, double z, int color, int alpha, boolean alwaysOnTop) {
        render = new Line3D(0,0,0, x, y, z, color, alpha, !alwaysOnTop);
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
     * @return self for chaining
     * @since 1.9.0
     */
    public TraceLine setPos(double x, double y, double z) {
        render.setPos(0, 0, 0, x, y, z);
        return this;
    }

    /**
     * @return self for chaining
     * @since 1.9.0
     */
    public TraceLine setPos(Pos3D pos) {
        render.setPos(0, 0, 0, pos.x, pos.y, pos.z);
        return this;
    }

    /**
     * @return the position of the line's target
     * @since 2.0.0
     */
    public Pos3D getPos() {
        return render.getPos2();
    }

    /**
     * @return self for chaining
     * @since 1.9.0
     */
    public TraceLine setColor(int color) {
        render.setColor(color);
        return this;
    }

    /**
     * @return self for chaining
     * @since 1.9.0
     */
    public TraceLine setColor(int color, int alpha) {
        render.setColor(color, alpha);
        return this;
    }

    /**
     * @return the color of the line
     * @since 2.0.0
     */
    public int getColor() {
        return render.getColor();
    }

    /**
     * @return self for chaining
     * @since 1.9.0
     */
    public TraceLine setAlpha(int alpha) {
        return setColor(render.color, alpha);
    }

    /**
     * @return the alpha value of the line's color
     * @since 2.0.0
     */
    public int getAlpha() {
        return render.getAlpha();
    }

    /**
     * @param alwaysOnTop whether the line should be rendered on top of everything else or not
     * @since 2.0.0
     */
    public void setAlwaysOnTop(boolean alwaysOnTop) {
        render.setAlwaysOnTop(alwaysOnTop);
    }

    /**
     * @return whether the line is rendered on top of everything else or not
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

    @Override
    public int compareToSame(TraceLine other) {
        return render.pos.getEnd().compareTo(other.render.pos.getEnd());
    }

    @Override
    //? if >=26.1 {
    /*public void render(PoseStack matrixStack, MultiBufferSource consumers, SubmitNodeCollector collector, float tickDelta) {
    *///?} else {
    public void render(PoseStack matrixStack, MultiBufferSource consumers, float tickDelta) {
    //?}
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        //? if >=1.21.11 {
        /*Vec3 cameraPos = camera.position();
        *///? } else {
        Vec3 cameraPos = camera.getPosition();
        //? }

        Vec3 lookDir = getCrosshairDirection(camera, tickDelta);
        Vec3 p1 = cameraPos.add(lookDir);

        render.setPos(p1.x, p1.y, p1.z, render.pos.x2, render.pos.y2, render.pos.z2);
        //? if >=26.1 {
        /*render.render(matrixStack, consumers, collector, tickDelta);
        *///?} else {
        render.render(matrixStack, consumers, tickDelta);
        //?}
    }

    /**
     * Returns the world-space direction from the camera that corresponds to the screen-centre
     * crosshair, accounting for the view-bob projection transform applied by GameRenderer.
     * <p>
     * bobView() bakes a translation, Z-roll, and X-pitch into the projection matrix
     * (in camera space). The full bob transform is:
     * <ol>
     *   <li>translate(sin(dist*π)*bob*0.5, -|cos(dist*π)*bob|, 0)</li>
     *   <li>Axis.ZP.rotationDegrees(sin(dist*π)*bob*3)</li>
     *   <li>Axis.XP.rotationDegrees(|cos(dist*π-0.2)*bob|*5)</li>
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
            // No bobbing active — the true camera forward is already screen-centre.
            //? if >=1.21.11 {
            /*return Vec3.directionFromRotation(camera.xRot(), camera.yRot());
             *///? } else {
            return Vec3.directionFromRotation(camera.getXRot(), camera.getYRot());
            //?}
        }

        // Replicate the bob rotation angles from bobView():
        float zDeg = Mth.sin(dist * (float) Math.PI) * bob * 3.0f;
        float xDeg = Math.abs(Mth.cos(dist * (float) Math.PI - 0.2f) * bob) * 5.0f;

        // Replicate the bob translation from bobView():
        float tx = Mth.sin(dist * (float) Math.PI) * bob * 0.5f;
        float ty = -Math.abs(Mth.cos(dist * (float) Math.PI) * bob);

        // The full bob matrix is M_bob = T(tx,ty,0) * R_Z(zDeg) * R_X(xDeg).
        // For a world-space point p1 = cameraPos + d to appear at screen centre,
        // we need:  M_bob * V_rot * d  to lie along (0, 0, -1).
        //
        // Expanding:  T * R_Z * R_X * V_rot * d = (0, 0, -z)
        //        =>   R_Z * R_X * V_rot * d = (-tx, -ty, -z)
        //        =>   V_rot * d = (R_Z * R_X)^-1 * (-tx, -ty, -z)
        //        =>   d = camera.rotation() * invBob * (-tx, -ty, -1)
        //
        // (We use z=1 since only the direction matters and we normalize implicitly
        //  by using the resulting vector as an offset from cameraPos.)

        // Inverse bob rotation: undo X then undo Z (reverse order, negated angles).
        Quaternionf invBob =
                new Quaternionf().rotateX((float) Math.toRadians(-xDeg)).rotateZ((float) Math.toRadians(-zDeg));

        // Include the bob translation offset so the line originates from the
        // effective viewpoint, not just the camera position.
        Vector3f camDir = invBob.transform(new Vector3f(-tx, -ty, -1.0f));

        // Rotate from camera space to world space using the camera's orientation.
        camera.rotation().transform(camDir);

        return new Vec3(camDir.x, camDir.y, camDir.z);
    }

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
         * @param pos the position of the target
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder pos(Pos3D pos) {
            this.pos = pos;
            return this;
        }

        /**
         * @param pos the position of the target
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder pos(BlockPosHelper pos) {
            this.pos = pos.toPos3D();
            return this;
        }

        /**
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
         * @return the position of the target
         * @since 1.9.0
         */
        public Pos3D getPos() {
            return pos;
        }

        /**
         * @param color the color of the line
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder color(int color) {
            this.color = color;
            return this;
        }

        /**
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
         * @return the color of the line
         * @since 1.9.0
         */
        public int getColor() {
            return color;
        }

        /**
         * @param alpha the alpha value for the line's color
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder alpha(int alpha) {
            this.alpha = alpha;
            return this;
        }

        /**
         * @param alwaysOnTop whether the line should be rendered on top of everything else or not
         * @return self for chaining
         * @since 2.0.0
         */
        public Builder alwaysOnTop(boolean alwaysOnTop) {
            this.alwaysOnTop = !alwaysOnTop;
            return this;
        }

        /**
         * @return the alpha value of the line's color
         * @since 1.9.0
         */
        public int getAlpha() {
            return alpha;
        }

        /**
         * Creates the trace line for the given values and adds it to the draw3D
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
         *
         * @return the build line
         * @since 1.9.0
         */
        public TraceLine build() {
            return new TraceLine(pos, color, alpha, alwaysOnTop);
        }

    }

}