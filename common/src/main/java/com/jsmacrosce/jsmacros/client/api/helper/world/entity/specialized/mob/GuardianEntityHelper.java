package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * Provides access to guardian-specific state, including elder status, attack target, and movement state.
 * <pre>
 * const GuardianEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.GuardianEntityHelper");
 * const guardians = World.getEntities(32, "guardian", "elder_guardian");
 * if (guardians !== null) {
 *   for (const entity of guardians) {
 *     const guardian = GuardianEntityHelper.class.cast(entity);
 *     if (guardian.isElder()) {
 *       Chat.log(`an elder guardian at ${guardian.getPos()}`);
 *     }
 *     if (guardian.hasTarget()) {
 *       // the beam is on something
 *       const target = guardian.getTarget();
 *       if (target !== null) {
 *         Chat.log(`locked onto ${target.getType()}`);
 *       }
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
public class GuardianEntityHelper extends MobEntityHelper<Guardian> {

    /** Wraps the supplied guardian and retains its reference for this helper.
     * @param base guardian entity to wrap
     */
    public GuardianEntityHelper(Guardian base) {
        super(base);
    }

    /**
     * Whether this guardian is an elder guardian.
     *
     * @return {@code true} if this guardian is an elder guardian, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isElder() {
        return base instanceof ElderGuardian;
    }

    /**
     * Whether the guardian has an active attack target. The target may be unavailable through {@link #getTarget()}.
     *
     * @return {@code true} if this guardian has an active attack target, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasTarget() {
        return base.hasActiveAttackTarget();
    }

    /**
     * Resolves the guardian's active attack target, or {@code null} if no target is active or the target cannot be resolved.
     *
     * @return the active attack target, or {@code null} if there is no active target or it cannot be resolved.
     * @since 1.8.4
     */
    @Nullable
    public EntityHelper<?> getTarget() {
        var target = base.getActiveAttackTarget();
        return target == null ? null : EntityHelper.create(target);
    }

    /**
     * Whether the guardian's movement state indicates that its spikes are retracted.
     *
     * @return {@code true} if this guardian has its spikes retracted, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasSpikesRetracted() {
        return base.isMoving();
    }

}
