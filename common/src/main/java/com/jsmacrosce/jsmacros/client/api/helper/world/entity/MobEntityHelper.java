package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import net.minecraft.world.entity.Mob;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class MobEntityHelper<T extends Mob> extends LivingEntityHelper<T> {

    public MobEntityHelper(T base) {
        super(base);
    }

    /**
     * @return {@code true} if the entity is currently attacking something, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isAttacking() {
        return base.isAggressive();
    }

    /**
     * Mobs which have there AI disabled don't move, attack, or interact with the world by
     * themselves.
     *
     * @return {@code true} if the entity's AI is disabled, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAiDisabled() {
        return base.isNoAi();
    }

}
