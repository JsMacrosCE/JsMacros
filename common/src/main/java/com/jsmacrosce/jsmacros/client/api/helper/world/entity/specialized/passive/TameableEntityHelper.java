package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.TamableAnimal;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.LivingEntityHelper;

/**
 * the tameable animals: the ones a player can win over with food and then give commands to.
 * A {@link WolfEntityHelper wolf}, a {@link CatEntityHelper cat} and a {@link
 * ParrotEntityHelper parrot} all sit on this class, and everything here is about the three
 * things they share - the tame flag, the sit command, and the one entity the animal belongs
 * to.
 * <p>
 * The owner is a UUID carried in the synced data, so unlike a horse's owner it arrives with
 * the animal and is there even when the owner is not currently loaded in the world.
 * {@link #isOwner(LivingEntityHelper) isOwner} is the awkward one of the three: it compares
 * the owner entity itself, so it answers {@code false} for the right mob when that mob is
 * not loaded, and it is not a UUID comparison.
 * <p>
 * {@link #isSitting() isSitting} is the sit <em>order</em> rather than the current pose.
 * The game keeps the two apart: the order is what the player asked for and survives the
 * animal being distracted, while the pose is what the animal is actually doing and is
 * dropped the moment it stands up to follow somebody.
 * example:
 * <pre>
 * const TameableEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.TameableEntityHelper");
 * const player = Player.getPlayer();
 * const tameables = World.getEntities(32, "wolf", "cat", "parrot");
 * if (player !== null) {
 *   if (tameables !== null) {
 *     for (const entity of tameables) {
 *       const tame = TameableEntityHelper.class.cast(entity);
 *       if (tame.isTamed()) {
 *         Chat.log(`${tame.getType()} tamed by ${tame.getOwner()}, `
 *           + `yours ${tame.isOwner(player)}, told to sit ${tame.isSitting()}`);
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
public class TameableEntityHelper<T extends TamableAnimal> extends AnimalEntityHelper<T> {

    public TameableEntityHelper(T base) {
        super(base);
    }

    /**
     * The tame flag, which is set when a player wins the animal over and is what the game
     * uses to decide whether it will follow anybody, whether it can breed, and whether an
     * owner is worth looking up. It is separate from whether an owner is recorded: a tame
     * animal with no owner, and an owner with no tame flag, are both possible states.
     *
     * @return {@code true} if the entity is tamed, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isTamed() {
        return base.isTame();
    }

    /**
     * The sit order, so a player has told this animal to stay where it is. It is not the
     * sitting pose: the game clears the pose as soon as the animal steps off to do
     * something else, while the order is still on. A cat that has been called down off a bed
     * is a good example of the two coming apart.
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
     * This reads the reference rather than the entity, so it still answers while the owner
     * is somewhere the client has not loaded. {@link #isOwner(LivingEntityHelper) isOwner},
     * which needs the entity itself, answers {@code false} in that same situation.
     * example:
     * <pre>
     * const TameableEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.TameableEntityHelper");
     * const wolves = World.getEntities(32, "wolf");
     * if (wolves !== null) {
     *   for (const entity of wolves) {
     *     const wolf = TameableEntityHelper.class.cast(entity);
     *     if (wolf.getOwner() !== null) {
     *       Chat.log(`a tame wolf owned by ${wolf.getOwner()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the owner's uuid, or {@code null} if the entity is not tamed.
     * @since 1.8.4
     */
    @Nullable
    public String getOwner() {
        var owner = base.getOwnerReference();
        return owner != null ? owner.getUUID().toString() : null;
    }

    /**
     * Whether this exact entity is the one recorded as the owner.
     * <p>
     * The comparison is against the owner looked up in the world, not against the UUID, so
     * two things can go wrong for a script. A mob that is the owner but is not currently
     * loaded answers {@code false}, because there is no entity to match; and the answer is
     * about identity rather than about trust, so a mob the animal follows for another reason
     * still answers {@code false}.
     * example:
     * <pre>
     * const TameableEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.TameableEntityHelper");
     * const player = Player.getPlayer();
     * const cats = World.getEntities(32, "cat");
     * if (player !== null) {
     *   if (cats !== null) {
     *     for (const entity of cats) {
     *       const cat = TameableEntityHelper.class.cast(entity);
     *       if (cat.isOwner(player)) {
     *         Chat.log("this cat is yours");
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @param owner the possible owner
     * @return {@code true} if the entity is tamed by the given owner, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isOwner(LivingEntityHelper<?> owner) {
        return base.isOwnedBy(owner.getRaw());
    }

}
