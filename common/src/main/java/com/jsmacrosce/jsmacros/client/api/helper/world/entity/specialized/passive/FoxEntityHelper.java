package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinFoxEntity;

import java.util.UUID;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.fox.Fox;
*///? } else {
import net.minecraft.world.entity.animal.Fox;
//?}

/**
 * Provides access to a fox's held item, trusted entities, variant, and behavior states.
 * <p>
 * Behavior methods expose distinct state flags, including crouching, target interest, pouncing,
 * jumping, and faceplanting.
 * <p>
 * A fox has up to two trusted-entity slots; {@link #getOwner()} and {@link #getSecondOwner()}
 * return their UUIDs.
 * <p>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class FoxEntityHelper extends AnimalEntityHelper<Fox> {

    /**
     * Wraps the supplied fox entity and retains its reference.
     *
     * @param base fox entity to wrap
     */
    public FoxEntityHelper(Fox base) {
        super(base);
    }

    /**
     * Returns the item held in the fox's main hand.
     *
     * @return the item in this fox's mouth.
     * @since 1.8.4
     */
    public ItemStackHelper getItemInMouth() {
        return getMainHand();
    }

    /**
     * Whether this fox's variant is snow.
     *
     * @return {@code true} if this fox is a snow fox, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSnowFox() {
        return base.getVariant() == Fox.Variant.SNOW;
    }

    /**
     * Whether this fox's variant is red.
     *
     * @return {@code true} if this fox is a red fox, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isRedFox() {
        return base.getVariant() == Fox.Variant.RED;
    }

    /**
     * Returns the UUID in the fox's first trusted-entity slot, or {@code null} if it is empty.
     * This is a trust slot, not a tamed-owner relationship; foxes can have two such slots.
     *
     * @return the first trusted entity's UUID, or {@code null} if the slot is empty.
     * @since 1.8.4
     */
    @Nullable
    public String getOwner() {
        return ((MixinFoxEntity) base).invokeGetTrustedEntities()
            .findFirst()
            .map(EntityReference::getUUID)
            .map(UUID::toString)
            .orElse(null);
    }

    /**
     * Returns the UUID in the fox's second trusted-entity slot, or {@code null} if it is empty.
     * An empty second slot does not indicate whether the first slot is populated; see
     * {@link #getOwner()}.
     *
     * @return the second trusted entity's UUID, or {@code null} if the slot is empty.
     * @since 1.8.4
     */
    @Nullable
    public String getSecondOwner() {
        return ((MixinFoxEntity) base).invokeGetTrustedEntities()
            .skip(1)
            .findFirst()
            .map(EntityReference::getUUID)
            .map(UUID::toString)
            .orElse(null);
    }

    /**
     * Checks whether the entity is in either trust slot. The comparison uses UUIDs; non-living
     * entities return {@code false}.
     *
     * @param entity the entity to check
     * @return {@code true} if this fox trusts the given entity, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canTrust(EntityHelper<?> entity) {
        var raw = entity.getRaw();
        return raw instanceof LivingEntity e && ((MixinFoxEntity) base).invokeCanTrust(e);
    }

    /**
     * Returns the fox's target-interest flag. This is distinct from {@link #isSneaking()}; it
     * does not guarantee that a pounce will follow.
     *
     * @return {@code true} if the fox is interested in its target, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasFoundTarget() {
        return base.isInterested();
    }

    /**
     * The sitting flag, which a player sets on a tamed-and-trusted fox the same way they set
     * it on a dog. It is not the same as {@link #isWandering() isWandering}, which is the
     * short flop after a missed pounce.
     *
     * @return {@code true} if this fox is sitting, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSitting() {
        return base.isSitting();
    }

    /**
     * Whether the fox is faceplanted after a failed pounce. The game clears this state when
     * the faceplant goal ends or the next stalking attempt starts.
     *
     * @return {@code true} if this fox has flopped over after a missed pounce, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isWandering() {
        return base.isFaceplanted();
    }

    /**
     * Whether the fox's sleeping state is set.
     *
     * @return {@code true} if this fox is sleeping, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSleeping() {
        return base.isSleeping();
    }

    /**
     * Returns whether the fox's defending flag is set. Its defense goal sets this when
     * targeting an attacker of a trusted entity; the flag alone does not identify its current target.
     *
     * @return {@code true} when the defending flag is set; {@code false} otherwise
     * @since 1.8.4
     */
    public boolean isDefending() {
        return ((MixinFoxEntity) base).invokeIsAggressive();
    }

    /**
     * Whether the fox's pounce state is set.
     *
     * @return {@code true} if this fox is just before its leap, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPouncing() {
        return base.isPouncing();
    }

    /**
     * Whether the fox's generic jump flag is set. This is distinct from {@link #isPouncing()},
     * which reports the fox-specific pounce state.
     *
     * @return {@code true} if this fox is jumping, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isJumping() {
        return base.isJumping();
    }

    /**
     * Whether the fox's crouching state is set. This is distinct from {@link #hasFoundTarget()},
     * which reports whether it is interested in a target.
     *
     * @return {@code true} if this fox is sneaking in preparation of an attack, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isSneaking() {
        return base.isCrouching();
    }

}
