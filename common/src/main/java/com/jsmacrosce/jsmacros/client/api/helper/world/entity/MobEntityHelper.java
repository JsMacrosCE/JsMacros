package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import net.minecraft.world.entity.Mob;
import com.jsmacrosce.doclet.DocletCategory;

/**
  * Wraps a Minecraft mob and exposes its mob-specific state.
 * <p>
 * <pre>
 * const MobEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper");
 * const mobs = World.getEntities("zombie", "skeleton", "creeper");
 * if (mobs !== null) {
 *   for (const entity of mobs) {
 *     const mob = MobEntityHelper.class.cast(entity);
 *     if (mob.isAiDisabled()) {
 *       Chat.log(`a ${mob.getType()} near ${mob.getPos()} has no AI and is not going anywhere`);
 *     }
 *   }
 * }
 * </pre>
 *
 * @param <T> the Minecraft mob type wrapped by this helper
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class MobEntityHelper<T extends Mob> extends LivingEntityHelper<T> {

    /**
     * Wraps the supplied mob and retains its reference for this helper.
     *
     * @param base mob to wrap
     */
    public MobEntityHelper(T base) {
        super(base);
    }

    /**
     * Returns whether the mob's aggressive-state flag is set.
     *
     * @return {@code true} if the entity's aggressive flag is set, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isAttacking() {
        return base.isAggressive();
    }

    /**
     * Returns whether the mob's no-AI flag is set.
     *
     * @return {@code true} if the entity's AI is disabled, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAiDisabled() {
        return base.isNoAi();
    }

}
