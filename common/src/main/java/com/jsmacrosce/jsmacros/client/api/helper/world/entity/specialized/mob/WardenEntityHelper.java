package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.warden.Warden;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class WardenEntityHelper extends MobEntityHelper<Warden> {

    public WardenEntityHelper(Warden base) {
        super(base);
    }

    /**
     * @return this warden's anger towards its active target.
     * @since 1.8.4
     */
    public int getAnger() {
        return base.getClientAngerLevel();
    }

    /**
     * @return {@code true} if this warden is digging into the ground, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isDigging() {
        return base.hasPose(Pose.DIGGING);
    }

    /**
     * @return {@code true} if this warden is emerging from the ground, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isEmerging() {
        return base.hasPose(Pose.EMERGING);
    }

    /**
     * @return {@code true} if this warden is roaring, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isRoaring() {
        return base.hasPose(Pose.ROARING);
    }

    /**
     * @return {@code true} if this warden is sniffing, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSniffing() {
        return base.hasPose(Pose.SNIFFING);
    }

    /**
     * @return {@code true} if this warden is charging its sonic boom attack, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isChargingSonicBoom() {
        return base.sonicBoomAnimationState.isStarted();
    }

}
