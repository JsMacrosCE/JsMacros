package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.boss;

import net.minecraft.world.entity.boss.wither.WitherBoss;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * Provides access to the wither's invulnerability countdown and health-based phase state.
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

    /**
     * Wraps the supplied wither and retains its reference for this helper.
     *
     * @param base wither to wrap
     */
    public WitherEntityHelper(WitherBoss base) {
        super(base);
    }

    /**
     * Returns the wither's remaining invulnerability countdown in ticks.
     * @return remaining countdown ticks
     * @since 1.8.4
     */
    public int getRemainingInvulnerableTime() {
        return base.getInvulnerableTicks();
    }

    /**
     * Returns whether the wither's invulnerability countdown is positive.
     * @return {@code true} while the countdown is positive; otherwise {@code false}
     * @since 1.8.4
     */
    public boolean isInvulnerable() {
        return base.getInvulnerableTicks() > 0;
    }

    /**
     * Returns whether the wither's health is above half its maximum health.
     * @return {@code true} above half health; otherwise {@code false}
     * @since 1.8.4
     */
    public boolean isFirstPhase() {
        return !isSecondPhase();
    }

    /**
     * Returns whether the wither's health is at or below half its maximum health. Arrows and wind charges do not damage it in this state.
     * @return {@code true} at or below half health; otherwise {@code false}
     * @since 1.8.4
     */
    public boolean isSecondPhase() {
        return base.isPowered();
    }

}
