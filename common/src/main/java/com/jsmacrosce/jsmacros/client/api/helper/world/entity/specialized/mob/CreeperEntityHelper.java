package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Creeper;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinCreeperEntity;

/**
 * Reads a creeper's powered and ignited states and fuse counters. Powered and ignited are
 * independent; the fuse direction and counter expose its progress, while
 * {@link #getRemainingFuseTime()} provides a countdown when applicable.
 *
 * <pre>
 * const CreeperEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.CreeperEntityHelper");
 * const creepers = World.getEntities(32, "creeper");
 * if (creepers !== null) {
 *   for (const entity of creepers) {
 *     const creeper = CreeperEntityHelper.class.cast(entity);
 *     // three different flags, so read the one that is actually the question
 *     if (creeper.isIgnited()) {
 *       Chat.log(`a lit creeper at ${creeper.getPos()}, ${creeper.getRemainingFuseTime()} ticks left`);
 *     }
 *     if (creeper.isCharged()) {
 *       Chat.log("that one was struck by lightning, so its explosion is doubled");
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
public class CreeperEntityHelper extends MobEntityHelper<Creeper> {

    /**
     * Wraps the supplied creeper and retains its reference for this helper.
     *
     * @param base creeper entity to wrap
     */
    public CreeperEntityHelper(Creeper base) {
        super(base);
    }

    /**
     * Returns whether the creeper is powered by a lightning strike. A powered creeper's
     * explosion radius is doubled; this state is independent of ignition.
     *
     * @return {@code true} if this creeper was struck by lightning, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isCharged() {
        return base.isPowered();
    }

    /**
     * Returns whether the creeper is ignited. While ignited, the game sets its swell
     * direction to {@code 1} each tick; this state is independent of being powered.
     *
     * @return {@code true} if the creeper has been ignited, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isIgnited() {
        return base.isIgnited();
    }

    /**
     * Returns the creeper's swell direction: negative, positive, or zero.
     *
     * @return the swell direction.
     * @since 1.8.4
     */
    public int getFuseChange() {
        return base.getSwellDir();
    }

    /**
     * Returns the whole-tick fuse counter. It rises while swell direction is positive and falls
     * while negative, so during defusing it is not elapsed time. Use {@link #getRemainingFuseTime()}
     * for the counter difference when the direction is nonnegative.
     *
     * @return the time the creeper has been charging up.
     * @since 1.8.4
     */
    public int getFuseTime() {
        return ((MixinCreeperEntity) base).getFuseTime();
    }

    /**
     * Returns the per-creeper fuse limit, in ticks. The game initializes it to 30 and stores it
     * with the creeper; read this value rather than assuming the default.
     *
     * @return the maximum time the creeper can be charged for before exploding.
     * @since 1.8.4
     */
    public int getMaxFuseTime() {
        return ((MixinCreeperEntity) base).getMaxFuseTime();
    }

    /**
     * Returns the whole-tick difference between the fuse limit and current fuse counter
     * while the swell direction is nonnegative. Returns {@code -1} while it is negative,
     * including both idle and defusing states.
     *
     * @return the fuse limit minus the current fuse counter, or {@code -1} when the swell
     * direction is negative.
     * @since 1.8.4
     */
    public int getRemainingFuseTime() {
        return getFuseChange() < 0 ? -1 : getMaxFuseTime() - getFuseTime();
    }

}
