package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration;

import net.minecraft.core.Rotations;
import net.minecraft.world.entity.decoration.ArmorStand;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.LivingEntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class ArmorStandEntityHelper extends LivingEntityHelper<ArmorStand> {

    public ArmorStandEntityHelper(ArmorStand base) {
        super(base);
    }

    /**
     * @return {@code true} if the armor stand is visible, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isVisible() {
        return !base.isInvisible();
    }

    /**
     * @return {@code true} if the armor is small, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSmall() {
        return base.isSmall();
    }

    /**
     * @return {@code true} if the armor stand has arms, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasArms() {
        return base.showArms();
    }

    /**
     * @return {@code true} if the armor stand has a base plate, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasBasePlate() {
        return base.showBasePlate();
    }

    /**
     * @return {@code true} if the armor stand is a marker, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isMarker() {
        return base.isMarker();
    }

    /**
     * The rotation is in the format of {@code [yaw, pitch, roll]}.
     *
     * @return the head rotation of the armor stand.
     * @since 1.8.4
     */
    public float[] getHeadRotation() {
        return toArray(base.getHeadPose());
    }

    /**
     * The rotation is in the format of {@code [yaw, pitch, roll]}.
     *
     * @return the body rotation of the armor stand.
     * @since 1.8.4
     */
    public float[] getBodyRotation() {
        return toArray(base.getBodyPose());
    }

    /**
     * The rotation is in the format of {@code [yaw, pitch, roll]}.
     *
     * @return the left arm rotation of the armor stand.
     * @since 1.8.4
     */
    public float[] getLeftArmRotation() {
        return toArray(base.getLeftArmPose());
    }

    /**
     * The rotation is in the format of {@code [yaw, pitch, roll]}.
     *
     * @return the right arm rotation of the armor stand.
     * @since 1.8.4
     */
    public float[] getRightArmRotation() {
        return toArray(base.getRightArmPose());
    }

    /**
     * The rotation is in the format of {@code [yaw, pitch, roll]}.
     *
     * @return the left leg rotation of the armor stand.
     * @since 1.8.4
     */
    public float[] getLeftLegRotation() {
        return toArray(base.getLeftLegPose());
    }

    /**
     * The rotation is in the format of {@code [yaw, pitch, roll]}.
     *
     * @return the right leg rotation of the armor stand.
     * @since 1.8.4
     */
    public float[] getRightLegRotation() {
        return toArray(base.getRightLegPose());
    }

    private float[] toArray(Rotations angle) {
        return new float[]{angle.y(), angle.x(), angle.z()};
    }

}
