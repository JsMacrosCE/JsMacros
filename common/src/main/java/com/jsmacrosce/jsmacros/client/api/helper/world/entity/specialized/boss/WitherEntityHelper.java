package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.boss;

import net.minecraft.world.entity.boss.wither.WitherBoss;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * the wither, the three-headed boss fought in a fortress.
 * <p>
 * The wither runs on a countdown rather than on health alone, and the countdown is what
 * {@link #getRemainingInvulnerableTime() getRemainingInvulnerableTime()} reports. While it is
 * running the wither cannot be hurt, cannot move and heals itself, and it explodes when it
 * reaches zero. The other three calls are the phase, which the game decides from the wither's
 * health rather than from a stored phase number: the second phase begins at half health.
 * example:
 * <pre>
 * const WitherEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.boss.WitherEntityHelper");
 * const withers = World.getEntities(64, "wither");
 * if (withers !== null) {
 *   for (const entity of withers) {
 *     const wither = WitherEntityHelper.class.cast(entity);
 *     if (wither.isInvulnerable()) {
 *       Chat.log(`untouchable for another ${wither.getRemainingInvulnerableTime()} ticks`);
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
public class WitherEntityHelper extends MobEntityHelper<WitherBoss> {

    public WitherEntityHelper(WitherBoss base) {
        super(base);
    }

    /**
     * how many ticks are left on the wither's invulnerability countdown, counting down to the
     * explosion that ends it.
     * <p>
     * This is the same number {@link #isInvulnerable() isInvulnerable()} tests, so it is
     * {@code 0} whenever that is {@code false} and positive whenever it is {@code true}. The
     * countdown runs for 220 ticks, which is eleven seconds at twenty ticks to the second, and
     * while it runs the wither heals itself by ten health every ten ticks. A script watching a
     * wither appear can time its arrival off this rather than polling for invulnerability.
     * example:
     * <pre>
     * const WitherEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.boss.WitherEntityHelper");
     * const withers = World.getEntities(64, "wither");
     * if (withers !== null) {
     *   for (const entity of withers) {
     *     const wither = WitherEntityHelper.class.cast(entity);
     *     const left = wither.getRemainingInvulnerableTime();
     *     if (left > 0) {
     *       // twenty ticks to the second, and it explodes at zero
     *       Chat.log(`${(left / 20).toFixed(1)} seconds before it can be hurt`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the time in ticks the wither will be invulnerable for.
     * @since 1.8.4
     */
    public int getRemainingInvulnerableTime() {
        return base.getInvulnerableTicks();
    }

    /**
     * The wither will only be invulnerable, by default for 220 ticks, when summoned.
     * <p>
     * This is the countdown being positive rather than a separate judgement about damage, so
     * it goes {@code true} the moment the wither is created by a completed skull and stays that
     * way for the whole 220 ticks. Damage that ignores invulnerability still lands in that
     * window, so this says the wither is on its countdown and not that it cannot be hurt at
     * all. Nothing on this class sets it.
     * example:
     * <pre>
     * const WitherEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.boss.WitherEntityHelper");
     * const withers = World.getEntities(64, "wither");
     * if (withers !== null) {
     *   for (const entity of withers) {
     *     const wither = WitherEntityHelper.class.cast(entity);
     *     // the same number isInvulnerable tests, so one is enough
     *     if (wither.getRemainingInvulnerableTime() > 0) {
     *       Chat.log("still on its arrival countdown");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the wither is invulnerable, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isInvulnerable() {
        return base.getInvulnerableTicks() > 0;
    }

    /**
     * whether the wither is still above half health, which is the same question as
     * {@link #isSecondPhase() isSecondPhase()} asked the other way round.
     * <p>
          * The phase is not stored: the game decides it from the wither's health, so this is
     * {@code true} whenever the health is strictly above half and {@code false} from the moment
     * it reaches half, with no gap in between. There is no gap in the other direction either,
     * and a wither that has just been summoned is already in the second phase: the game spawns
     * it at a third of its health so it can charge on arrival, which is below the halfway
     * mark. So a freshly summoned wither answers {@code false} here and {@code true} for
     * {@link #isSecondPhase() isSecondPhase()}, and the only way to see this answer {@code true}
     * is for it to have been healed back over half during the fight.
     * example:
     * <pre>
     * const WitherEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.boss.WitherEntityHelper");
     * const withers = World.getEntities(64, "wither");
     * if (withers !== null) {
     *   for (const entity of withers) {
     *     const wither = WitherEntityHelper.class.cast(entity);
     *     if (wither.isFirstPhase()) {
     *       Chat.log(`${wither.getHealth()} of ${wither.getMaxHealth()} health`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the wither is in its first phase, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isFirstPhase() {
        return !isSecondPhase();
    }

    /**
     * In the second phase the wither will be invulnerable to projectiles and starts going down
     * towards the player.
     * <p>
          * The phase is decided by health rather than stored, and it begins at half health or
     * below. A summoned wither is already below that, since the game gives it a third of its
     * health on arrival, so a wither that has just appeared answers {@code true} here.
     * <p>
     * The one concrete thing the game changes with it is damage: at or below half health the
     * wither stops taking damage from arrows and from wind charges, and other damage still
     * lands. The other differences in the fight are a matter of its goals rather than of this
     * flag.
     * example:
     * <pre>
     * const WitherEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.boss.WitherEntityHelper");
     * const withers = World.getEntities(64, "wither");
     * if (withers !== null) {
     *   for (const entity of withers) {
     *     const wither = WitherEntityHelper.class.cast(entity);
     *     // the phase is health decided, so it and isFirstPhase cannot both be true
     *     if (wither.isSecondPhase()) {
     *       if (wither.getMaxHealth() / 2 >= wither.getHealth()) {
     *         Chat.log("arrows will not hurt it from here on");
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the wither is in its second phase, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSecondPhase() {
        return base.isPowered();
    }

}
