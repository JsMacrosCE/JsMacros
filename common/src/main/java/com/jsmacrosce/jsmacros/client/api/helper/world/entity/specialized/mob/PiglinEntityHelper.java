package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinArmPose;
import com.jsmacrosce.doclet.DocletCategory;

/**
 * Provides access to the piglin's current arm-pose states. The arm-pose predicates are mutually exclusive; {@link #isDancing()} reads a separate state, so check it separately from the arm-pose predicates.
 * <pre>
 * const PiglinEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.PiglinEntityHelper");
 * const piglins = World.getEntities(32, "piglin");
 * if (piglins !== null) {
 *   for (const entity of piglins) {
 *     const piglin = PiglinEntityHelper.class.cast(entity);
 *     // Dancing is a separate state; the arm-pose predicates below are mutually exclusive.
 *     let doing = "wander";
 *     if (piglin.isDancing()) {
 *       doing = "dance";
 *     } else {
 *       if (piglin.isAdmiring()) {
 *         doing = "admire";
 *       } else {
 *         if (piglin.isMeleeAttacking()) {
 *           doing = "swing";
 *         } else {
 *           if (piglin.isChargingCrossbow()) {
 *             doing = "draw";
 *           } else {
 *             if (piglin.hasCrossbowReady()) {
 *               doing = "loaded";
 *             }
 *           }
 *         }
 *       }
 *     }
 *     if (piglin.isWandering()) {
 *       doing = "wander";
 *     }
 *     if (doing === "wander") {
 *       Chat.log(`a piglin at ${piglin.getPos()} is doing nothing in particular`);
 *     } else {
 *       Chat.log(`a piglin at ${piglin.getPos()} is ${doing}`);
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
public class PiglinEntityHelper extends AbstractPiglinEntityHelper<Piglin> {

    /** Wraps the supplied piglin entity and retains its reference for this helper.
     * @param base piglin entity to wrap
     */
    public PiglinEntityHelper(Piglin base) {
        super(base);
    }

    /**
     * Whether the piglin's arm pose is {@code DEFAULT}.
     *
     * @return {@code true} if this piglin's arm pose is {@code DEFAULT}; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isWandering() {
        return base.getArmPose() == PiglinArmPose.DEFAULT;
    }

    /**
      * Whether the piglin is dancing.
     *
     * @return {@code true} if the piglin's dancing state is set, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isDancing() {
        return base.isDancing();
    }

    /**
      * Whether the piglin's arm pose is admiring an item.
     *
     * @return {@code true} if this piglin is admiring an item, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAdmiring() {
        return base.getArmPose() == PiglinArmPose.ADMIRING_ITEM;
    }

    /**
      * Whether the piglin's arm pose is the melee-attack pose.
     *
     * @return {@code true} if the piglin's arm pose is {@code ATTACKING_WITH_MELEE_WEAPON}, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isMeleeAttacking() {
        return base.getArmPose() == PiglinArmPose.ATTACKING_WITH_MELEE_WEAPON;
    }

    /**
      * Whether the piglin's arm pose is the crossbow-charging pose; this is distinct from {@link #hasCrossbowReady()}.
     *
     * @return {@code true} if the piglin's arm pose is {@code CROSSBOW_CHARGE}, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isChargingCrossbow() {
        return base.getArmPose() == PiglinArmPose.CROSSBOW_CHARGE;
    }

    /**
      * Whether the piglin's arm pose is the crossbow-hold pose.
     *
     * @return {@code true} if the piglin's arm pose is {@code CROSSBOW_HOLD}, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasCrossbowReady() {
        return base.getArmPose() == PiglinArmPose.CROSSBOW_HOLD;
    }

}
