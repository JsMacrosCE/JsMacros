package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.boss;

import net.minecraft.world.entity.boss.wither.WitherBoss;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class WitherEntityHelper extends MobEntityHelper<WitherBoss> {

    public WitherEntityHelper(WitherBoss base) {
        super(base);
    }

    /**
     * @return the time in ticks the wither will be invulnerable for.
     * @since 1.8.4
     */
    public int getRemainingInvulnerableTime() {
        return base.getInvulnerableTicks();
    }

    /**
     * The wither will only be invulnerable, by default for 220 ticks, when summoned.
     *
     * @return {@code true} if the wither is invulnerable, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isInvulnerable() {
        return base.getInvulnerableTicks() > 0;
    }

    /**
     * @return {@code true} if the wither is in its first phase, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isFirstPhase() {
        return !isSecondPhase();
    }

    /**
     * In the second phase the wither will be invulnerable to projectiles and starts going down
     * towards the player.
     *
     * @return {@code true} if the wither is in its second phase, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSecondPhase() {
        return base.isPowered();
    }

}
