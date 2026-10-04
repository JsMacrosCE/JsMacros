package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Creeper;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinCreeperEntity;

/**
 * the creeper, and the three separate things about one that read alike.
 * <p>
 * A creeper can be struck by lightning, can be lit, and can be running down a fuse, and
 * the game keeps those as three unrelated flags, so this class keeps them apart rather
 * than folding them into one "is it about to blow" answer.
 * {@link #isCharged() isCharged} is the lightning: a creeper struck by one explodes for
 * double when it finally goes off, lit or not.
 * {@link #isIgnited() isIgnited} is the lighting, and it is what makes the fuse run on its
 * own rather than only when something is standing near.
 * The other four are the fuse counters themselves, which a script usually wants as a
 * countdown rather than as a pair of numbers to subtract.
 * <p>
 * Nothing here sets any of them. These are reads of the state the client was last told, so
 * what they answer is what the server's creeper has, not a decision the client made.
 * example:
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

    public CreeperEntityHelper(Creeper base) {
        super(base);
    }

    /**
     * Whether the creeper was struck by lightning, which is what the game itself calls
     * powered. This has nothing to do with the fuse: a charged creeper is one that took a
     * lightning strike, and the only thing the game does with the flag is double the
     * explosion radius when the creeper eventually goes off, lit or not.
     * <p>
     * Nothing clears the flag once it is set, so a creeper that has been struck stays
     * charged for as long as it is loaded, including across a fuse that was talked out
     * before it went off. A creeper that has just spawned in a thunderstorm has not been
     * struck yet and answers {@code false}.
     * example:
     * <pre>
     * const CreeperEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.CreeperEntityHelper");
     * const creepers = World.getEntities(48, "creeper");
     * if (creepers !== null) {
     *   for (const entity of creepers) {
     *     const creeper = CreeperEntityHelper.class.cast(entity);
     *     if (creeper.isCharged()) {
     *       Chat.log(`the creeper at ${creeper.getPos()} was struck, and lit: ${creeper.isIgnited()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this creeper was struck by lightning, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isCharged() {
        return base.isPowered();
    }

    /**
     * Whether the creeper has been lit, which is the flag a flint and steel, a fire charge
     * or the game's own prime sets. Once it is set the game forces the swell direction to
     * one on every tick, so the fuse then runs to its end whether or not anything is still
     * standing near the creeper, and a lit creeper cannot be talked out of it.
     * <p>
     * This is the flag the fuse counters move on, and it is a different one from the
     * lightning behind {@link #isCharged() isCharged}: a creeper can be lit without having
     * been struck, and a lightning strike on its own does not light anything.
     * example:
     * <pre>
     * const CreeperEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.CreeperEntityHelper");
     * const creepers = World.getEntities(32, "creeper");
     * if (creepers !== null) {
     *   for (const entity of creepers) {
     *     const creeper = CreeperEntityHelper.class.cast(entity);
     *     // lit and still standing: the countdown is already the whole story
     *     if (creeper.isIgnited()) {
     *       if (creeper.getRemainingFuseTime() >= 0) {
     *         Chat.log(`${creeper.getFuseTime()} of ${creeper.getMaxFuseTime()} ticks gone`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the creeper has been ignited, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isIgnited() {
        return base.isIgnited();
    }

    /**
     * A negative value means the creeper is currently defusing, while a positive value means the
     * creeper is currently charging up.
     * <p>
     * This is a direction rather than a speed, and it is {@code -1} for as long as the
     * creeper has nothing to go off at: the game defines it that way and only sets it to one
     * once something has lit the fuse. It goes back to {@code -1} mid-fuse if the creeper's
     * target moves more than seven blocks away or the creeper loses sight of that target,
     * which is how a creeper is talked out of a fuse it has not finished.
     * example:
     * <pre>
     * const CreeperEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.CreeperEntityHelper");
     * const creepers = World.getEntities(16, "creeper");
     * if (creepers !== null) {
     *   for (const entity of creepers) {
     *     const creeper = CreeperEntityHelper.class.cast(entity);
     *     // a lit creeper walking away from its target is winding back down
     *     if (creeper.isIgnited()) {
     *       if (creeper.getFuseChange() >= 0) {
     *         Chat.log(`the creeper at ${creeper.getPos()} is charging, ${creeper.getRemainingFuseTime()} ticks to go`);
     *       } else {
     *         Chat.log(`the creeper at ${creeper.getPos()} is defusing, ${creeper.getFuseTime()} ticks still on the clock`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the change in fuse every tick.
     * @since 1.8.4
     */
    public int getFuseChange() {
        return base.getSwellDir();
    }

    /**
     * The fuse counter itself, which is how many ticks it has climbed. It climbs while the
     * swell direction is positive and falls back while it is negative, so on a creeper
     * being talked out of a fuse this is a number on the way down rather than a time
     * elapsed.
     * <p>
     * This is the raw counter rather than the smoothed one the client renders the creeper's
     * swelling from, so it moves in whole ticks.
     * example:
     * <pre>
     * const CreeperEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.CreeperEntityHelper");
     * const creepers = World.getEntities(16, "creeper");
     * if (creepers !== null) {
     *   for (const entity of creepers) {
     *     const creeper = CreeperEntityHelper.class.cast(entity);
     *     const ceiling = creeper.getMaxFuseTime();
     *     if (ceiling > 0) {
     *       const done = Math.round((creeper.getFuseTime() / ceiling) * 100);
     *       Chat.log(`fuse ${done}% through its ceiling of ${ceiling}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the time the creeper has been charging up.
     * @since 1.8.4
     */
    public int getFuseTime() {
        return ((MixinCreeperEntity) base).getFuseTime();
    }

    /**
     * The ceiling the fuse counts up to before the creeper goes off. It is 30 ticks on a
     * creeper that has never been touched, and the game stores a per-creeper one when a
     * saved creeper is read back, so this is worth reading rather than assuming.
     * example:
     * <pre>
     * const CreeperEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.CreeperEntityHelper");
     * const creepers = World.getEntities(16, "creeper");
     * if (creepers !== null) {
     *   for (const entity of creepers) {
     *     const creeper = CreeperEntityHelper.class.cast(entity);
     *     // anything but the default ceiling came off saved data
     *     if (creeper.getMaxFuseTime() !== 30) {
     *       Chat.log(`the creeper at ${creeper.getPos()} runs a ${creeper.getMaxFuseTime()} tick fuse`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the maximum time the creeper can be charged for before exploding.
     * @since 1.8.4
     */
    public int getMaxFuseTime() {
        return ((MixinCreeperEntity) base).getMaxFuseTime();
    }

    /**
     * The ticks left before the fuse runs out, or {@code -1} while the swell direction is
     * negative — which is both the idle state and the defusing one, so this answers
     * {@code -1} for a creeper that is not counting towards an explosion at all.
     * <p>
     * The subtraction is against the raw fuse counter rather than the smoothed one the
     * client renders, so this is a whole number of ticks and it does not slide down
     * smoothly between frames.
     * example:
     * <pre>
     * const CreeperEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.CreeperEntityHelper");
     * const creepers = World.getEntities(16, "creeper");
     * if (creepers !== null) {
     *   for (const entity of creepers) {
     *     const creeper = CreeperEntityHelper.class.cast(entity);
     *     const left = creeper.getRemainingFuseTime();
     *     if (left >= 0) {
     *       if (10 >= left) {
     *         Chat.log(`${left} ticks before the creeper at ${creeper.getPos()} goes off`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the remaining time until the creeper explodes with the current fuse time, or
     * {@code -1} if the creeper is not about to explode.
     * @since 1.8.4
     */
    public int getRemainingFuseTime() {
        return getFuseChange() < 0 ? -1 : getMaxFuseTime() - getFuseTime();
    }

}
