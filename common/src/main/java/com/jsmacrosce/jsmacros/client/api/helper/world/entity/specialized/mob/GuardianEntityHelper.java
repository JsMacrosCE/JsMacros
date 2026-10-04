package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * the guardian, which is the same mob as the elder guardian with a flag on it.
 * <p>
 * {@link #isElder() isElder} is the only difference between the two: the game gives the
 * elder guardian a larger body, 80 health where the ordinary guardian has 30, and the
 * mining fatigue it radiates around itself. It is one of the same class, so a script has
 * to ask rather than narrow by type.
 * <p>
 * The other three are about what a guardian is doing right now rather than what it is: the
 * thing its beam is locked onto, and whether it is holding station. None of them are set
 * by anything on this class.
 * example:
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

    public GuardianEntityHelper(Guardian base) {
        super(base);
    }

    /**
     * Whether this is the elder guardian rather than the ordinary one. The two are the same
     * class in the game, so this is a check of which mob it is rather than a separate kind
     * of entity, and the world's own type id already tells the same thing.
     * example:
     * <pre>
     * const GuardianEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.GuardianEntityHelper");
     * const guardians = World.getEntities(32, "guardian", "elder_guardian");
     * if (guardians !== null) {
     *   for (const entity of guardians) {
     *     const guardian = GuardianEntityHelper.class.cast(entity);
     *     if (guardian.isElder()) {
     *       // the elder's health pool is the visible difference: 80 against 30
     *       Chat.log(`elder: ${guardian.getHealth()} of ${guardian.getMaxHealth()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this guardian is an elder guardian, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isElder() {
        return base instanceof ElderGuardian;
    }

    /**
     * Whether the guardian's beam has something to point at. This is the flag the target is
     * synced through, and the game keeps it set for as long as the guardian is locked on
     * rather than only for the frames the beam is actually drawn.
     * <p>
     * This is the cheap way to ask; {@link #getTarget() getTarget} then resolves the entity
     * the flag names, which is a second step and can come back empty-handed.
     * example:
     * <pre>
     * const GuardianEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.GuardianEntityHelper");
     * const guardians = World.getEntities(32, "guardian");
     * if (guardians !== null) {
     *   for (const entity of guardians) {
     *     const guardian = GuardianEntityHelper.class.cast(entity);
     *     if (guardian.hasTarget()) {
     *       const target = guardian.getTarget();
     *       if (target !== null) {
     *         Chat.log(`a guardian at ${guardian.getPos()} is on ${target.getType()}`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this guardian is targeting a mob, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasTarget() {
        return base.hasActiveAttackTarget();
    }

    /**
     * The entity the guardian's beam is locked onto, or {@code null} when
     * {@link #hasTarget() hasTarget} is {@code false}.
     * <p>
     * The flag {@code hasTarget} reads only says that the guardian was last told about a
     * target, and this is the step that looks that entity up in the world the client has.
     * A target that has since left the loaded chunks leaves the flag set and this unable to
     * find anything to wrap, which returns {@code null}. A {@code hasTarget} check alone does
     * not guarantee that the target can be resolved.
     * example:
     * <pre>
     * const GuardianEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.GuardianEntityHelper");
     * const guardians = World.getEntities(32, "guardian", "elder_guardian");
     * if (guardians !== null) {
     *   for (const entity of guardians) {
     *     const guardian = GuardianEntityHelper.class.cast(entity);
     *     if (guardian.hasTarget()) {
     *       const target = guardian.getTarget();
     *       if (target !== null) {
     *         Chat.log(`${guardian.distanceTo(target)} blocks from ${target.getType()}`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the target of this guardian's beam, or {@code null} if it has no target.
     * @since 1.8.4
     */
    @Nullable
    public EntityHelper<?> getTarget() {
        var target = base.getActiveAttackTarget();
        return target == null ? null : EntityHelper.create(target);
    }

    /**
     * Whether the guardian has its spikes retracted. The game folds them in while the guardian
     * is moving and holds them out when it stops.
     * <p>
     * The flag behind it is the one the guardian's own movement goal sets, so this tracks
     * whether the guardian is off on its way somewhere rather than whether anything is
     * happening to it.
     * example:
     * <pre>
     * const GuardianEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.GuardianEntityHelper");
     * const guardians = World.getEntities(32, "guardian", "elder_guardian");
     * if (guardians !== null) {
     *   for (const entity of guardians) {
     *     const guardian = GuardianEntityHelper.class.cast(entity);
     *     // retracted spikes indicate that the guardian is moving
     *     if (guardian.hasSpikesRetracted()) {
     *       Chat.log(`a guardian at ${guardian.getPos()} is moving, health ${guardian.getHealth()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this guardian has its spikes retracted, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasSpikesRetracted() {
        return base.isMoving();
    }

}
