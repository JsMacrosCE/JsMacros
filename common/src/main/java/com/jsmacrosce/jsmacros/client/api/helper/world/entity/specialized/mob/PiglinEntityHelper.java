package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinArmPose;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class PiglinEntityHelper extends AbstractPiglinEntityHelper<Piglin> {

    public PiglinEntityHelper(Piglin base) {
        super(base);
    }

    /**
     * @return {@code true} if this piglin is doing nothing special, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isWandering() {
        return base.getArmPose() == PiglinArmPose.DEFAULT;
    }

    /**
     * @return {@code true} if this piglin is dancing to music, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isDancing() {
        return base.isDancing();
    }

    /**
     * @return {@code true} if this piglin is admiring an item, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAdmiring() {
        return base.getArmPose() == PiglinArmPose.ADMIRING_ITEM;
    }

    /**
     * @return {@code true} if this piglin is attacking another entity, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isMeleeAttacking() {
        return base.getArmPose() == PiglinArmPose.ATTACKING_WITH_MELEE_WEAPON;
    }

    /**
     * @return {@code true} if this piglin is currently charging its crossbow, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isChargingCrossbow() {
        return base.getArmPose() == PiglinArmPose.CROSSBOW_CHARGE;
    }

    /**
     * @return {@code true} if this piglin has its crossbow fully charged, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasCrossbowReady() {
        return base.getArmPose() == PiglinArmPose.CROSSBOW_HOLD;
    }

}
