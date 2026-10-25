package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.TamableAnimal;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.LivingEntityHelper;

/**
 * Helpers for tameable animals, including wolves, cats, and parrots. Exposes their tame
 * state, sit order, and owner reference.
 * <p>
 * {@link #getOwner()} returns the recorded owner's UUID even if that entity is not loaded;
 * {@link #isOwner(LivingEntityHelper) isOwner} instead checks the resolved owner entity and
 * returns {@code false} if it cannot be resolved. {@link #isSitting()} reports the sit order,
 * not the animal's current sitting pose.
 * <pre>
 * const player = Player.getPlayer();
 * const tameables = World.getEntities(32, "wolf", "cat", "parrot");
 * if (player !== null) {
 *   if (tameables !== null) {
 *     for (const entity of tameables) {
 *       if (!(entity instanceof EntityHelpers.Tameable)) continue;
 *       if (entity.isTamed()) {
 *         Chat.log(`${entity.getType()} tamed by ${entity.getOwner()}, `
 *           + `yours ${entity.isOwner(player)}, told to sit ${entity.isSitting()}`);
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * @param <T> the tameable animal type wrapped by this helper
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class TameableEntityHelper<T extends TamableAnimal> extends AnimalEntityHelper<T> {

    /**
     * Wraps the supplied tameable animal and retains its reference for this helper.
     *
     * @param base tameable animal to wrap
     */
    public TameableEntityHelper(T base) {
        super(base);
    }

    /**
     * Returns the entity's tame flag. This is independent of whether an owner reference is
     * recorded.
     *
     * @return {@code true} if the entity is tamed, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isTamed() {
        return base.isTame();
    }

    /**
     * Returns the sit order, not the entity's current sitting pose.
     *
     * @return {@code true} if the entity is sitting, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSitting() {
        return base.isOrderedToSit();
    }

    /**
     * The owner's UUID as a string, or {@code null} when there is no owner.
     * <p>
     * The UUID is available independently of whether the owner entity is loaded; see
     * {@link #isOwner(LivingEntityHelper) isOwner} for entity comparison.
     *
     * @return the owner's UUID, or {@code null} if no owner is recorded.
     * @since 1.8.4
     */
    @Nullable
    public String getOwner() {
        var owner = base.getOwnerReference();
        return owner != null ? owner.getUUID().toString() : null;
    }

    /**
     * Checks whether the entity resolved as this animal's owner is the given entity. An
     * unresolved owner does not match.
     *
     * @param owner the possible owner
     * @return {@code true} if the given entity is the resolved owner; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isOwner(LivingEntityHelper<?> owner) {
        return base.isOwnedBy(owner.getRaw());
    }

}
