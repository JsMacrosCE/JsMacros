package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration;

import net.minecraft.core.Rotations;
import net.minecraft.world.entity.decoration.ArmorStand;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.LivingEntityHelper;

/**
 * Reads an armor stand's flags and six pose rotations.
 * <pre>
 * const ArmorStandEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.ArmorStandEntityHelper");
 * const stands = World.getEntities(32, "armor_stand");
 * if (stands !== null) {
 *   for (const entity of stands) {
 *     const stand = ArmorStandEntityHelper.class.cast(entity);
 *     if (stand.isMarker()) {
 *       // Marker stands have zero dimensions and cannot be picked.
 *       Chat.log(`marker at ${entity.getPos()}, wearing a ${stand.getHeadArmor().getItemID()}`);
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class ArmorStandEntityHelper extends LivingEntityHelper<ArmorStand> {

    /** Wraps the supplied armor stand, retaining that same object as the helper's underlying value.
     * @param base armor stand to wrap
     */
    public ArmorStandEntityHelper(ArmorStand base) {
        super(base);
    }

    /**
     * Returns whether the armor stand's invisible flag is unset.
     *
     * @return {@code true} if the armor stand is visible, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isVisible() {
        return !base.isInvisible();
    }

    /**
     * Returns whether this armor stand has its small flag set.
     *
     * @return {@code true} if the armor stand is small, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSmall() {
        return base.isSmall();
    }

    /**
     * Returns whether the armor stand displays arms. Without arms, hand slots cannot be equipped through player interaction.
     *
     * @return {@code true} if the armor stand has arms, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasArms() {
        return base.showArms();
    }

    /**
     * Returns whether the armor stand displays its base plate.
     *
     * @return {@code true} if the armor stand has a base plate, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasBasePlate() {
        return base.showBasePlate();
    }

    /**
     * Returns whether the armor stand has its marker flag set. Marker stands have zero dimensions and cannot be picked or interacted with.
     *
     * @return {@code true} if the armor stand is a marker, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isMarker() {
        return base.isMarker();
    }

    /**
     * Returns the head pose as {@code [yaw, pitch, roll]} in degrees.
     *
     * @return the head rotation of the armor stand.
     * @since 1.8.4
     */
    public float[] getHeadRotation() {
        return toArray(base.getHeadPose());
    }

    /**
     * Returns the body pose as {@code [yaw, pitch, roll]} in degrees.
     *
     * @return the body rotation of the armor stand.
     * @since 1.8.4
     */
    public float[] getBodyRotation() {
        return toArray(base.getBodyPose());
    }

    /**
     * Returns the left arm pose as {@code [yaw, pitch, roll]} in degrees.
     *
     * @return the left arm rotation of the armor stand.
     * @since 1.8.4
     */
    public float[] getLeftArmRotation() {
        return toArray(base.getLeftArmPose());
    }

    /**
     * Returns the right arm pose as {@code [yaw, pitch, roll]} in degrees.
     *
     * @return the right arm rotation of the armor stand.
     * @since 1.8.4
     */
    public float[] getRightArmRotation() {
        return toArray(base.getRightArmPose());
    }

    /**
     * Returns the left leg pose as {@code [y, x, z]} rotation components in degrees.
     *
     * @return the left leg pose components in {@code [y, x, z]} order.
     * @since 1.8.4
     */
    public float[] getLeftLegRotation() {
        return toArray(base.getLeftLegPose());
    }

    /**
     * Returns the right leg pose as {@code [y, x, z]} rotation components in degrees.
     *
     * @return the right leg pose components in {@code [y, x, z]} order.
     * @since 1.8.4
     */
    public float[] getRightLegRotation() {
        return toArray(base.getRightLegPose());
    }

    private float[] toArray(Rotations angle) {
        return new float[]{angle.y(), angle.x(), angle.z()};
    }

}
