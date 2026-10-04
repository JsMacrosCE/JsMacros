package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration;

import net.minecraft.core.Rotations;
import net.minecraft.world.entity.decoration.ArmorStand;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.LivingEntityHelper;

/**
 * an armor stand, the thing used to hold armour and a head on display and to pose a mannequin.
 * <p>
 * The four flags here are the ones set through the item's own settings: whether the stand is
 * showing at all, whether it is the small one, whether it has arms, and whether it has the
 * plate under its feet. They are independent of each other, and a stand can be any combination
 * of all four, which is why a "marker" is not a sixth flag but a fifth thing entirely:
 * {@link #isMarker() isMarker()} is what makes a stand a non-entity for nearly every purpose.
 * <p>
 * A marker is the odd one out, because it is not about how the stand looks at all. A marker has
 * no gravity and no physics, cannot be pushed by a piston, cannot be hurt, does not trip
 * pressure plates and is not pickable, so a stand set as one is a pile of equipment that exists
 * only to hold what is on it. Nothing on this class sets any of the five: they are all reads,
 * and the settings come from whatever placed the stand.
 * example:
 * <pre>
 * const ArmorStandEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.ArmorStandEntityHelper");
 * const stands = World.getEntities(32, "armor_stand");
 * if (stands !== null) {
 *   for (const entity of stands) {
 *     const stand = ArmorStandEntityHelper.class.cast(entity);
 *     if (stand.isMarker()) {
 *       // a marker is the one that holds equipment without being a mob in the world
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

    public ArmorStandEntityHelper(ArmorStand base) {
        super(base);
    }

    /**
     * whether the stand is drawn at all, which is the ordinary invisible flag read the other
     * way round.
     * <p>
     * An armor stand is hidden through the same invisible flag every other entity uses, so
     * this answers {@code true} for a stand that is being shown and {@code false} for one the
     * game has been told not to draw. An empty stand is still drawn: this has nothing to do
     * with what is on it, and a stand with nothing in it and this {@code true} is a stand you
     * can see and that has nothing on it. A {@link #isMarker() marker()} can be either as
     * well, because the marker flag is about physics and picking rather than about drawing.
     * example:
     * <pre>
     * const ArmorStandEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.ArmorStandEntityHelper");
     * const stands = World.getEntities(32, "armor_stand");
     * if (stands !== null) {
     *   for (const entity of stands) {
     *     const stand = ArmorStandEntityHelper.class.cast(entity);
     *     if (!stand.isVisible()) {
     *       Chat.log(`hidden, but still wearing a ${stand.getHeadArmor().getItemID()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the armor stand is visible, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isVisible() {
        return !base.isInvisible();
    }

    /**
     * whether the stand is the half size one, rather than a full size stand.
     * <p>
     * This is the {@code Small} setting, and it is what makes the stand and everything on it
     * half scale rather than a separate kind of stand. It is the same flag the game reads as
     * the entity being a baby, so a small stand is a baby as far as anything checking that is
     * concerned.
     * example:
     * <pre>
     * const ArmorStandEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.ArmorStandEntityHelper");
     * const stands = World.getEntities(32, "armor_stand");
     * if (stands !== null) {
     *   for (const entity of stands) {
     *     const stand = ArmorStandEntityHelper.class.cast(entity);
     *     if (stand.isSmall()) {
     *       Chat.log(`a half size stand at ${entity.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the armor is small, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSmall() {
        return base.isSmall();
    }

    /**
     * whether the stand has arms, which is what lets it wear a helmet and hold an item in hand.
     * <p>
     * This is the {@code ShowArms} setting and it is separate from whether the arms are empty:
     * a stand with arms and nothing in them looks the same as a stand with no arms, so the only
     * way to tell is to read this. Turning it off also stops the stand using its hand slots at
     * all, so a script that has put an item there will find it cannot be interacted with.
     * example:
     * <pre>
     * const ArmorStandEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.ArmorStandEntityHelper");
     * const stands = World.getEntities(32, "armor_stand");
     * if (stands !== null) {
     *   for (const entity of stands) {
     *     const stand = ArmorStandEntityHelper.class.cast(entity);
     *     // arms off means the hand slots are not usable, whatever is in them
     *     if (!stand.hasArms()) {
     *       Chat.log(`no arms, so the main hand slot is not reachable`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the armor stand has arms, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasArms() {
        return base.showArms();
    }

    /**
     * whether the stand has the plate under its feet, which is the setting that decides
     * whether it stands on a visible slab.
     * <p>
     * The game stores this the other way round, as a {@code NoBasePlate} setting, and this
     * answers the question as asked rather than as stored. A stand with no plate is floating
     * with nothing under it, and a plate is shown by default.
     * example:
     * <pre>
     * const ArmorStandEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.ArmorStandEntityHelper");
     * const stands = World.getEntities(32, "armor_stand");
     * if (stands !== null) {
     *   for (const entity of stands) {
     *     const stand = ArmorStandEntityHelper.class.cast(entity);
     *     if (!stand.hasBasePlate()) {
     *       Chat.log(`floating, with nothing under its feet at ${entity.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the armor stand has a base plate, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasBasePlate() {
        return base.showBasePlate();
    }

    /**
     * whether the stand is a marker, which is the setting that turns it off as a thing in the
     * world while leaving what it holds in place.
     * <p>
     * A marker is the one combination here that is not really about how the stand looks. It has
     * no gravity and no physics, it cannot be pushed by a piston, it cannot be hit, it does not
     * trip pressure plates and it is not pickable, so a right click or a sword swing goes
     * straight through it. What is left is the equipment, which stays where it was put and can
     * still be read and changed through the shared entity helpers.
     * <p>
     * It is independent of all four other flags, so a marker can be small, can have arms, can
     * have a base plate and can be visible all at once.
     * example:
     * <pre>
     * const ArmorStandEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.ArmorStandEntityHelper");
     * const stands = World.getEntities(32, "armor_stand");
     * if (stands !== null) {
     *   for (const entity of stands) {
     *     const stand = ArmorStandEntityHelper.class.cast(entity);
     *     if (stand.isMarker()) {
     *       // the equipment is still there, and still reachable, even though the stand is not
     *       Chat.log(`wearing a ${stand.getHeadArmor().getItemID()}, and nothing can hit it`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the armor stand is a marker, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isMarker() {
        return base.isMarker();
    }

    /**
     * the head rotation of the armor stand.
     * <p>
     * The rotation is in the format of {@code [yaw, pitch, roll]}.
     * <p>
     * Each angle is wrapped into the range from minus 360 to 360 by the game, so nothing here
     * is ever a bigger number than that however many turns it was given. The three are applied
     * to the head model as a turn about y, a turn about x and a roll about z, which is why the
     * roll is the third number and not a second yaw.
     * example:
     * <pre>
     * const ArmorStandEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.ArmorStandEntityHelper");
     * const stands = World.getEntities(32, "armor_stand");
     * if (stands !== null) {
     *   for (const entity of stands) {
     *     const stand = ArmorStandEntityHelper.class.cast(entity);
     *     // a JavaArray, so index into it rather than spreading it
     *     const head = stand.getHeadRotation();
     *     if (head[1] !== 0) {
     *       Chat.log(`looking ${head[1]} degrees up or down`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the head rotation of the armor stand.
     * @since 1.8.4
     */
    public float[] getHeadRotation() {
        return toArray(base.getHeadPose());
    }

    /**
     * the body rotation of the armor stand.
     * <p>
     * The rotation is in the format of {@code [yaw, pitch, roll]}.
     * <p>
     * This is the torso only, so turning the head with
     * {@link #getHeadRotation() getHeadRotation()} does not move it. The body is the piece
     * that carries the chestplate, so this is the rotation a script wants when it is building
     * a mannequin rather than posing a head.
     * example:
     * <pre>
     * const ArmorStandEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.ArmorStandEntityHelper");
     * const stands = World.getEntities(32, "armor_stand");
     * if (stands !== null) {
     *   for (const entity of stands) {
     *     const stand = ArmorStandEntityHelper.class.cast(entity);
     *     const body = stand.getBodyRotation();
     *     Chat.log(`torso turned ${body[0]} degrees`);
     *   }
     * }
     * </pre>
     *
     * @return the body rotation of the armor stand.
     * @since 1.8.4
     */
    public float[] getBodyRotation() {
        return toArray(base.getBodyPose());
    }

    /**
     * the left arm rotation of the armor stand.
     * <p>
     * The rotation is in the format of {@code [yaw, pitch, roll]}.
     * <p>
     * This is the stand's own left rather than the viewer's, so it is the arm on the opposite
     * side from where a script looking at the stand would expect to find the item it is
     * holding. A stand that has never been posed reports {@code [-10.0, 0.0, -10.0]} here
     * against {@code [-15.0, 0.0, 10.0]} for
     * {@link #getRightArmRotation() getRightArmRotation()}, so the two differ even untouched.
     * example:
     * <pre>
     * const ArmorStandEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.ArmorStandEntityHelper");
     * const stands = World.getEntities(32, "armor_stand");
     * if (stands !== null) {
     *   for (const entity of stands) {
     *     const stand = ArmorStandEntityHelper.class.cast(entity);
     *     const arm = stand.getLeftArmRotation();
     *     // a raised arm: the pitch is well away from the default
     *     if (-90.0 > arm[1]) {
     *       Chat.log(`the left arm is up, at ${arm[1]} degrees`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the left arm rotation of the armor stand.
     * @since 1.8.4
     */
    public float[] getLeftArmRotation() {
        return toArray(base.getLeftArmPose());
    }

    /**
     * the right arm rotation of the armor stand.
     * <p>
     * The rotation is in the format of {@code [yaw, pitch, roll]}.
     * <p>
     * The counterpart to {@link #getLeftArmRotation() getLeftArmRotation()} on the other arm,
     * and the two defaults are not the same: an unposed stand reports
     * {@code [-15.0, 0.0, 10.0]} here against {@code [-10.0, 0.0, -10.0]} for the left, so a
     * stand nobody has posed still reads differently on each side.
     * <p>
     * Every angle is stored wrapped into the range from minus 360 to 360, so nothing here is
     * ever a bigger number than that however many turns it was given.
     * example:
     * <pre>
     * const ArmorStandEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.ArmorStandEntityHelper");
     * const stands = World.getEntities(32, "armor_stand");
     * if (stands !== null) {
     *   for (const entity of stands) {
     *     const stand = ArmorStandEntityHelper.class.cast(entity);
     *     const right = stand.getRightArmRotation();
     *     const left = stand.getLeftArmRotation();
     *     // the two arms are posed independently
     *     if (right[1] !== left[1]) {
     *       Chat.log(`arms posed differently: left ${left[1]}, right ${right[1]}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the right arm rotation of the armor stand.
     * @since 1.8.4
     */
    public float[] getRightArmRotation() {
        return toArray(base.getRightArmPose());
    }

    /**
     * the left leg rotation of the armor stand.
     * <p>
     * The rotation is in the format of {@code [yaw, pitch, roll]}.
     * <p>
     * The legs are the piece with by far the smallest default rotation of the six: an unposed
     * stand reports {@code [-1.0, 0.0, -1.0]} here, so all three numbers are a single degree
     * rather than the tens the arms start at.
     * example:
     * <pre>
     * const ArmorStandEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.ArmorStandEntityHelper");
     * const stands = World.getEntities(32, "armor_stand");
     * if (stands !== null) {
     *   for (const entity of stands) {
     *     const stand = ArmorStandEntityHelper.class.cast(entity);
     *     const leg = stand.getLeftLegRotation();
     *     // a planted leg: the pitch is a long way from the default
     *     if (-45.0 > leg[1]) {
     *       Chat.log(`the left leg is planted forward`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the left leg rotation of the armor stand.
     * @since 1.8.4
     */
    public float[] getLeftLegRotation() {
        return toArray(base.getLeftLegPose());
    }

    /**
     * the right leg rotation of the armor stand.
     * <p>
     * The rotation is in the format of {@code [yaw, pitch, roll]}.
     * <p>
     * The counterpart to {@link #getLeftLegRotation() getLeftLegRotation()} on the other leg.
     * The two defaults are exactly equal and opposite, {@code [-1.0, 0.0, -1.0]} against
     * {@code [1.0, 0.0, 1.0]}, so an unposed stand reads the same on both legs; anything else
     * is a pose a script or a command put there.
     * example:
     * <pre>
     * const ArmorStandEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.ArmorStandEntityHelper");
     * const stands = World.getEntities(32, "armor_stand");
     * if (stands !== null) {
     *   for (const entity of stands) {
     *     const stand = ArmorStandEntityHelper.class.cast(entity);
     *     const right = stand.getRightLegRotation();
     *     const left = stand.getLeftLegRotation();
     *     if (right[1] !== left[1]) {
     *       Chat.log(`walking pose: left ${left[1]}, right ${right[1]}`);
     *     }
     *   }
     * }
     * </pre>
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
