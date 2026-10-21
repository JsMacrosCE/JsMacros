package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.warden.Warden;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * Provides access to a warden's anger, poses, and sonic-boom animation state.
 * <p>
 * The pose methods report distinct entity poses; anger is a synced level, and
 * {@link #isChargingSonicBoom() isChargingSonicBoom} reports client animation state.
 * <p>
 * These methods report the state currently available on the client.
 * <pre>
 * const WardenEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.WardenEntityHelper");
 * const wardens = World.getEntities(32, "warden");
 * if (wardens !== null) {
 *   for (const entity of wardens) {
 *     const warden = WardenEntityHelper.class.cast(entity);
 *     if (warden.isDigging()) Chat.log(`digging at ${warden.getPos()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class WardenEntityHelper extends MobEntityHelper<Warden> {

    /**
     * Wraps the supplied warden and retains its reference for this helper.
     *
     * @param base warden entity to wrap
     */
    public WardenEntityHelper(Warden base) {
        super(base);
    }

    /**
     * The warden's current client-synced anger level.
     * <pre>
     * const WardenEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.WardenEntityHelper");
     * const wardens = World.getEntities(32, "warden");
     * if (wardens !== null) {
     *   for (const entity of wardens) {
     *     const warden = WardenEntityHelper.class.cast(entity);
     *     // 80 is where the game calls a warden angry
     *     if (warden.getAnger() >= 80) {
     *       Chat.log(`a warden at ${warden.getPos()} is at anger ${warden.getAnger()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the client-synced anger level.
     * @since 1.8.4
     */
    public int getAnger() {
        return base.getClientAngerLevel();
    }

    /**
     * Whether the warden is in the pose for going into the ground.
     * <pre>
     * const WardenEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.WardenEntityHelper");
     * const wardens = World.getEntities(32, "warden");
     * if (wardens !== null) {
     *   for (const entity of wardens) {
     *     const warden = WardenEntityHelper.class.cast(entity);
     *     if (warden.isDigging()) {
     *       // going in, not coming out: the other pose is the one to ask for that
     *       Chat.log(`a warden at ${warden.getPos()} is digging, emerging: ${warden.isEmerging()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this warden is digging into the ground, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isDigging() {
        return base.hasPose(Pose.DIGGING);
    }

    /**
     * Whether the warden is in the pose for emerging from the ground.
     * <pre>
     * const WardenEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.WardenEntityHelper");
     * const wardens = World.getEntities(32, "warden");
     * if (wardens !== null) {
     *   for (const entity of wardens) {
     *     const warden = WardenEntityHelper.class.cast(entity);
     *     if (warden.isEmerging()) {
     *       Chat.log(`a warden at ${warden.getPos()} is coming up out of the ground`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this warden is emerging from the ground, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isEmerging() {
        return base.hasPose(Pose.EMERGING);
    }

    /**
     * Whether the warden is in its roaring pose.
     * <pre>
     * const WardenEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.WardenEntityHelper");
     * const wardens = World.getEntities(32, "warden");
     * if (wardens !== null) {
     *   for (const entity of wardens) {
     *     const warden = WardenEntityHelper.class.cast(entity);
     *     if (warden.isRoaring()) {
     *       // a pose, so it can be true at a low anger number
     *       Chat.log(`roaring at anger ${warden.getAnger()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this warden is roaring, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isRoaring() {
        return base.hasPose(Pose.ROARING);
    }

    /**
     * Whether the warden is in its sniffing pose.
     * <pre>
     * const WardenEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.WardenEntityHelper");
     * const wardens = World.getEntities(32, "warden");
     * if (wardens !== null) {
     *   for (const entity of wardens) {
     *     const warden = WardenEntityHelper.class.cast(entity);
     *     if (warden.isSniffing()) {
     *       Chat.log(`a warden at ${warden.getPos()} is following a sound`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this warden is sniffing, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSniffing() {
        return base.hasPose(Pose.SNIFFING);
    }

    /**
     * Whether the client-side sonic-boom animation state has started; this does not indicate
     * whether the warden is currently charging an attack.
     *
     * @return whether the client-side animation state is started.
     * @since 1.8.4
     */
    public boolean isChargingSonicBoom() {
        return base.sonicBoomAnimationState.isStarted();
    }

}
