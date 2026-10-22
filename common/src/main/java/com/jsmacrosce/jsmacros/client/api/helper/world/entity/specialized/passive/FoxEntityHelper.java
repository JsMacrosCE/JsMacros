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
 * the fox, and above all the run of states a fox goes through while it hunts.
 * <p>
 * A hunting fox runs through four flags in a fixed order, and they are worth keeping apart
 * because a script that reads the wrong one will call a fox idle while it is lining up a
 * kill. {@link #isSneaking() isSneaking} and {@link #hasFoundTarget() hasFoundTarget} are
 * the wind-up: the fox has its prey, is crouched and has stopped moving. {@link
 * #isPouncing() isPouncing} is the leap itself, and {@link #isJumping() isJumping} is the
 * jump flag the leap sets at the same moment. If the leap misses, {@link
 * #isWandering() isWandering} is up for about two seconds while the fox lies on its face.
 * <p>
 * The trust methods are a separate matter. A fox remembers up to two entities, and the
 * player who tamed it is the first of them; {@link #getOwner() getOwner} and {@link
 * #getSecondOwner() getSecondOwner} are those two slots and are both {@code null} when the
 * slot is empty, which is a different thing from the fox having no owner at all.
 * <p>
 * Two of the names here are older than the state they report and read oddly: {@link
 * #isDefending() isDefending} is about defending somebody the fox trusts rather than
 * another fox, and {@link #isWandering() isWandering} is about the flop after a missed
 * pounce rather than about ambling about. Both say what they really answer below.
 * example:
 * <pre>
 * const FoxEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.FoxEntityHelper");
 * const player = Player.getPlayer();
 * const foxes = World.getEntities(32, "fox");
 * if (player !== null) {
 *   if (foxes !== null) {
 *     for (const entity of foxes) {
 *       const fox = FoxEntityHelper.class.cast(entity);
 *       Chat.log(`${fox.getType()} at ${fox.getPos()}, `
 *         + `found ${fox.hasFoundTarget()}, `
 *         + `pouncing ${fox.isPouncing()}, `
 *         + `flopped ${fox.isWandering()}, `
 *         + `trusts you ${fox.canTrust(player)}`);
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
public class FoxEntityHelper extends AnimalEntityHelper<Fox> {

    public FoxEntityHelper(Fox base) {
        super(base);
    }

    /**
     * The fox carries whatever it has picked up in its main hand, so this is the same figure
     * {@code getMainHand()} gives and the name is the game's rather than this class's. A fox
     * that has just found something has a non-empty stack here; one that has eaten it or
     * dropped it does not.
     * example:
     * <pre>
     * const FoxEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.FoxEntityHelper");
     * const foxes = World.getEntities(32, "fox");
     * if (foxes !== null) {
     *   for (const entity of foxes) {
     *     const fox = FoxEntityHelper.class.cast(entity);
     *     const held = fox.getItemInMouth();
     *     if (held.isEmpty()) {
     *       continue;
     *     }
     *     Chat.log(`carrying ${held.getCount()} ${held.getItemId()}`);
     *   }
     * }
     * </pre>
     *
     * @return the item in this fox's mouth.
     * @since 1.8.4
     */
    public ItemStackHelper getItemInMouth() {
        return getMainHand();
    }

    /**
     * There are only two fox variants, this one and {@link #isRedFox() isRedFox}, so a fox
     * that is neither is a fox whose variant data has not arrived yet rather than a third
     * kind of fox. The game picks the variant from the biome the fox spawns in: a fox in one
     * of the biomes tagged for snow foxes gets this one, and everywhere else it is red.
     *
     * @return {@code true} if this fox is a snow fox, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSnowFox() {
        return base.getVariant() == Fox.Variant.SNOW;
    }

    /**
     * There are only two fox variants, this one and {@link #isSnowFox() isSnowFox}, so a fox
     * that is neither is a fox whose variant data has not arrived yet rather than a third
     * kind of fox. This is the commoner of the two.
     *
     * @return {@code true} if this fox is a red fox, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isRedFox() {
        return base.getVariant() == Fox.Variant.RED;
    }

    /**
     * The UUID of the first entity the fox trusts, as a string, or {@code null} when that
     * slot is empty.
     * <p>
     * Trust is not the same as being tamed. A wild fox trusts whoever it was first hand-fed
     * or spawned next to, and it remembers two of them; the fox does not follow the first
     * one, and nothing stops a second player taking that slot.
     * example:
     * <pre>
     * const FoxEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.FoxEntityHelper");
     * const foxes = World.getEntities(32, "fox");
     * if (foxes !== null) {
     *   for (const entity of foxes) {
     *     const fox = FoxEntityHelper.class.cast(entity);
     *     Chat.log(`trusts ${fox.getOwner()} then ${fox.getSecondOwner()}`);
     *   }
     * }
     * </pre>
     *
     * @return the owner's UUID, or {@code null} if this fox has no owner.
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
     * The UUID of the second entity the fox trusts, as a string, or {@code null} when that
     * slot is empty - which is the usual case, since almost every fox in the world has one
     * trusted entity rather than two. A {@code null} here says nothing about whether the
     * fox has an owner at all; {@link #getOwner() getOwner} is the one that answers that.
     * example:
     * <pre>
     * const FoxEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.FoxEntityHelper");
     * const foxes = World.getEntities(32, "fox");
     * if (foxes !== null) {
     *   for (const entity of foxes) {
     *     const fox = FoxEntityHelper.class.cast(entity);
     *     if (fox.getSecondOwner() !== null) {
     *       Chat.log(`a fox that trusts two: ${fox.getOwner()} and ${fox.getSecondOwner()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the second owner's UUID, or {@code null} if this fox has no second owner.
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
     * Whether the fox has this entity in either of its two trust slots. The comparison is by
     * UUID, so it holds for the same mob even if the fox has never met this particular copy
     * of it.
     * <p>
     * An entity that is not alive at all - a dropped item, a boat, a piece of falling sand -
     * can never be trusted, because the fox only ever records living entities and this
     * answers {@code false} for anything else rather than throwing.
     * example:
     * <pre>
     * const FoxEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.FoxEntityHelper");
     * const player = Player.getPlayer();
     * const foxes = World.getEntities(32, "fox");
     * if (player !== null) {
     *   if (foxes !== null) {
     *     for (const entity of foxes) {
     *       const fox = FoxEntityHelper.class.cast(entity);
     *       if (fox.canTrust(player)) {
     *         Chat.log("this one knows you");
     *       }
     *     }
     *   }
     * }
     * </pre>
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
     * The wind-up, so the name is about the fox having picked its prey rather than about the
     * leap itself. The fox has a target, has stopped moving, and is crouched and staring at
     * it; the flag goes up as soon as the fox closes to within six blocks and is cleared the
     * instant the pounce starts, so it is {@code false} for the whole of the leap.
     * <p>
     * A fox that is crouched and has found its prey is therefore answering {@code true} to
     * both this and {@link #isSneaking() isSneaking}, which is what distinguishes the wind-up
     * from a fox that is merely crouching.
     * example:
     * <pre>
     * const FoxEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.FoxEntityHelper");
     * const foxes = World.getEntities(24, "fox");
     * if (foxes !== null) {
     *   for (const entity of foxes) {
     *     const fox = FoxEntityHelper.class.cast(entity);
     *     if (fox.hasFoundTarget()) {
     *       Chat.log("locked on, and about to go for it");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this fox has locked onto its prey and is preparing its jump,
     * {@code false} otherwise.
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
     * The name is misleading: this is not the fox ambling about, it is the fox lying on its
     * face after a pounce that fell short.
     * <p>
     * A fox that missed its leap sets this, and the game then keeps it there for roughly two
     * seconds, stops the fox from moving at all, and has it chip at the block underneath
     * every so often. It is cleared when the flop is over and again by the next stalk, so it
     * is the one fox state that comes and goes on its own.
     * example:
     * <pre>
     * const FoxEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.FoxEntityHelper");
     * const foxes = World.getEntities(24, "fox");
     * if (foxes !== null) {
     *   for (const entity of foxes) {
     *     const fox = FoxEntityHelper.class.cast(entity);
     *     if (fox.isWandering()) {
     *       Chat.log("that pounce missed, and it is face down in the dirt");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this fox has flopped over after a missed pounce, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isWandering() {
        return base.isFaceplanted();
    }

    /**
     * The sleeping flag, which the game raises on its own once the fox has found somewhere
     * to sleep and clears when anything wakes it. It is the fox's own flag rather than the
     * generic pose flag a wolf or a cat uses.
     *
     * @return {@code true} if this fox is sleeping, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSleeping() {
        return base.isSleeping();
    }

    /**
     * Whether the fox has gone after something for having hurt one of the entities it
     * trusts. The name reads as though this were about another fox, and it is not: the
     * usual case is a fox that has taken on whatever attacked the player who tamed it.
     * <p>
     * The flag goes up when that goal starts and comes down again when the fox loses its
     * target, so it lasts about as long as the fox is actually going after somebody. The
     * game also consults it when deciding whether a nearby wolf or polar bear is a threat
     * worth fleeing from, and a fox that is defending somebody does not run.
     *
     * @return {@code true} if this fox is defending an entity it trusts, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isDefending() {
        return ((MixinFoxEntity) base).invokeIsAggressive();
    }

    /**
     * The leap itself, which runs from the tick the fox springs until the pounce goal stops.
     * A pounce that ends in a flop ends the flag too, so {@link #isWandering() isWandering}
     * and this do not overlap.
     *
     * @return {@code true} if this fox is just before its leap, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPouncing() {
        return base.isPouncing();
    }

    /**
     * The generic jump flag rather than a fox-specific one, and the game raises it on the
     * same tick the pounce starts. It is a per-tick input flag that is cleared when the fox
     * cannot act, so it is close to {@link #isPouncing() isPouncing} for a fox without being
     * the same figure: another goal setting the fox jumping would move this one alone.
     *
     * @return {@code true} if this fox is jumping, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isJumping() {
        return base.isJumping();
    }

    /**
     * The fox's own crouch flag, which the hunt is what sets: the fox crouches as it closes
     * on its prey, and the game stops it moving while it is crouched and staring. The flag
     * goes down again when the pounce starts, so a crouched fox with {@link
     * #hasFoundTarget() hasFoundTarget} up is one about to leap.
     *
     * @return {@code true} if this fox is sneaking in preparation of an attack, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isSneaking() {
        return base.isCrouching();
    }

}
