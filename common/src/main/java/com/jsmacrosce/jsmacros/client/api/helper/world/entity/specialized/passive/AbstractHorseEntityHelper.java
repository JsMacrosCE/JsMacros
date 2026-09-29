package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinAbstractHorseEntity;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.equine.AbstractHorse;
*///? } else {
import net.minecraft.world.entity.animal.horse.AbstractHorse;
//?}

/**
 * the horse family. A plain horse is covered by this class, and so are the donkeys, mules,
 * llamas and trader llamas, which all sit on it in the game: {@code DonkeyEntityHelper} and
 * {@code LlamaEntityHelper} add the chest and the variant on top of everything here.
 * <p>
 * What a horse has that most other animals do not is a saddle slot, a body armour slot, the
 * rearing animation, and three separate stat attributes a script usually wants side by side:
 * the jump strength from {@link #getJumpStrengthStat()}, the movement speed from {@link
 * #getSpeedStat()}, and the maximum health from {@link #getHealthStat()}.
 * <p>
 * {@link #isAngry() isAngry} is the one to read carefully: it is not anger, it is the rearing
 * flag. The game sets it when a horse is hurt about one time in three, when the rear-up goal
 * fires, when a rider asks for a jump, and when a player makes the horse mad, so it answers
 * {@code true} for a horse that is merely showing off.
 * <p>
 * The two saddle questions are not the same either.
 * {@link #canBeSaddled() canBeSaddled} asks the real one - alive, grown up and tamed - while
 * {@link #canWearArmor() canWearArmor} asks the body slot, which the game answers yes for
 * every horse and which is therefore not worth asking.
 * example:
 * <pre>
 * const AbstractHorseEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.AbstractHorseEntityHelper");
 * const horses = World.getEntities(32, "horse", "donkey", "llama");
 * if (horses !== null) {
 *   for (const entity of horses) {
 *     const horse = AbstractHorseEntityHelper.class.cast(entity);
 *     // three stats and the range the game keeps them in
 *     Chat.log(`${horse.getType()}: jump ${horse.getJumpStrengthStat()}, `
 *       + `speed ${horse.getSpeedStat()}, health ${horse.getHealthStat()}`);
 *     if (horse.canBeSaddled()) {
 *       Chat.log(`  saddleable, and riding at ${horse.getHorseSpeed()} blocks a second`);
 *     }
 *     // the flag is the rear, not anger
 *     if (horse.isAngry()) {
 *       Chat.log("  rearing up");
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
public class AbstractHorseEntityHelper<T extends AbstractHorse> extends AnimalEntityHelper<T> {

    public AbstractHorseEntityHelper(T base) {
        super(base);
    }

    /**
     * The owner of a tamed horse, as a UUID string in the usual
     * {@code 8-4-4-4-12} form, or {@code null} when there is none.
     * <p>
     * A horse keeps its owner in a plain field on the entity rather than in the synced data
     * the server sends with it, which is unlike a tameable animal such as a wolf or a cat:
     * those carry their owner in synced data, so their answer arrives with the animal, while
     * a horse's is whatever this client-side horse itself is holding.
     * example:
     * <pre>
     * const AbstractHorseEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.AbstractHorseEntityHelper");
     * const horses = World.getEntities(32, "horse");
     * if (horses !== null) {
     *   for (const entity of horses) {
     *     const horse = AbstractHorseEntityHelper.class.cast(entity);
     *     if (horse.isTame()) {
     *       Chat.log(`tamed, owner ${horse.getOwner()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the UUID of this horse's owner, or {@code null} if it has no owner.
     * @since 1.8.4
     */
    @Nullable
    public String getOwner() {
        var owner = base.getOwnerReference();
        return owner != null ? owner.getUUID().toString() : null;
    }

    /**
     * The tamed flag, which is set when a player tames the horse and is what the game also
     * uses to decide whether a saddle or a piece of body armour may be put on it. A horse
     * spawned into the world, or one that came out of a wild parent pair, is not tamed.
     * example:
     * <pre>
     * const AbstractHorseEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.AbstractHorseEntityHelper");
     * const horses = World.getEntities(32, "horse");
     * if (horses !== null) {
     *   for (const entity of horses) {
     *     const horse = AbstractHorseEntityHelper.class.cast(entity);
     *     if (horse.isTame()) {
     *       Chat.log(`${horse.getType()} is tamed, `
     *         + `${horse.getHealth()} of ${horse.getMaxHealth()} health`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this horse is already tamed, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isTame() {
        return base.isTamed();
    }

    /**
     * Whether there is a saddle in the saddle slot. This is what the game itself asks before
     * letting a rider jump, and it has nothing to do with whether the horse <em>may</em> be
     * saddled - that is {@link #canBeSaddled() canBeSaddled}, which asks whether the horse is
     * alive, grown up and tamed.
     *
     * @return {@code true} if this horse is saddled, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSaddled() {
        return base.isSaddled();
    }

    /**
     * The rearing flag, so the name is misleading: this is not anger.
     * <p>
     * The game raises it in several unrelated places. A horse that has just been hurt raises
     * it about one time in three, the rear-up goal raises it on a tamed horse now and then,
     * a rider double-tapping jump raises it, and the game's own make-mad call raises it along
     * with the cross noise. None of those is cleared when the reason passes, so the flag
     * stays up for as long as the horse is loaded.
     * <p>
     * It is not the same as the horse having somebody to be cross about either: a horse
     * raises it without picking a target at all, so a script watching for a cross horse
     * should watch this rather than a target.
     * example:
     * <pre>
     * const AbstractHorseEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.AbstractHorseEntityHelper");
     * const horses = World.getEntities(24, "horse");
     * if (horses !== null) {
     *   for (const entity of horses) {
     *     const horse = AbstractHorseEntityHelper.class.cast(entity);
     *     if (horse.isAngry()) {
     *       Chat.log(`rearing at ${horse.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this horse is rearing up on its hind legs, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isAngry() {
        return base.isStanding();
    }

    /**
     * The bred flag the game stores with the horse. It is used in only two places: choosing
     * which of the two parents a foal takes its inherited stats from, and drawing a baby
     * horse at adult size rather than as a foal.
     * <p>
     * Nothing in this version of the game ever raises it. It is written to the horse's own
     * save data and read back from it, and there is no code path that turns it on, so a
     * horse met in the world rather than read back from a save answers {@code false}.
     *
     * @return {@code true} if this horse was bred and not naturally spawned, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isBred() {
        return base.isBred();
    }

    /**
     * Whether the horse has its head in a haystack. The game raises this when the horse is
     * eating and clears it when it is done or when something pulls the horse away, so it is
     * a short-lived state rather than something that persists.
     *
     * @return {@code true} if this horse is currently eating, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isEating() {
        return base.isEating();
    }

    /**
     * This asks whether the body armour slot is one the game lets anything be put into, and
     * for the body slot the horse hands the question straight up to the generic answer, which
     * is yes for everything but the saddle. It therefore answers {@code true} for every
     * horse, tamed or not, and is not worth asking.
     * <p>
     * What is actually worth asking is whether the horse is tamed, which is the condition
     * the game puts on <em>this</em> slot elsewhere, and whether it already has something on.
     *
     * @return {@code true} if this horse can wear armor, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canWearArmor() {
        return base.canUseSlot(EquipmentSlot.BODY);
    }

    /**
     * The real saddle question, and unlike {@link #canWearArmor() canWearArmor} it has three
     * conditions: the horse has to be alive, it has to be grown up rather than a foal, and it
     * has to be tamed. That is the whole of the game's rule for the saddle slot on a horse.
     *
     * @return {@code true} if this horse can be saddled, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canBeSaddled() {
        return base.canUseSlot(EquipmentSlot.SADDLE);
    }

    /**
     * The number of slots in the horse's own inventory, which is three times its number of
     * columns rather than a fixed figure.
     * <p>
     * A plain horse has no columns and so answers {@code 0}. A donkey or a mule with a chest
     * has five columns and answers {@code 15}, and answers {@code 0} again once the chest is
     * taken off. A llama with a chest has as many columns as its strength, so it answers
     * {@code 3}, {@code 6}, {@code 9}, {@code 12} or {@code 15}.
     * example:
     * <pre>
     * const AbstractHorseEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.AbstractHorseEntityHelper");
     * const horses = World.getEntities(32, "horse", "donkey", "mule", "llama");
     * if (horses !== null) {
     *   for (const entity of horses) {
     *     const horse = AbstractHorseEntityHelper.class.cast(entity);
     *     Chat.log(`${horse.getType()} carries ${horse.getInventorySize()} slots`);
     *   }
     * }
     * </pre>
     *
     * @return this horse's inventory size.
     * @since 1.8.4
     */
    public int getInventorySize() {
        return ((MixinAbstractHorseEntity) base).invokeGetInventorySize();
    }

    /**
     * The horse's own jump strength attribute, which is the number the game actually jumps
     * with: it multiplies the attribute straight into the upward impulse. A horse spawned by
     * the game gets a value drawn from {@code 0.4} to {@code 1}, and an attribute command
     * can move it outside that.
     *
     * @return this horse's jump strength.
     * @since 1.8.4
     */
    public double getJumpStrengthStat() {
        return base.getAttributeValue(Attributes.JUMP_STRENGTH);
    }

    /**
     * The result of this method is only an approximation, but it's really close.
     * <p>
     * It is a fit rather than a figure read off the horse: the game itself jumps with the
     * raw strength attribute multiplied by the block jump factor, and has no formula that
     * turns that into blocks. This is the familiar curve fitted to observed jumps, so it is
     * the right number to print next to a horse and the wrong one to drive a movement
     * prediction with.
     * example:
     * <pre>
     * const AbstractHorseEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.AbstractHorseEntityHelper");
     * const horses = World.getEntities(32, "horse");
     * if (horses !== null) {
     *   for (const entity of horses) {
     *     const horse = AbstractHorseEntityHelper.class.cast(entity);
     *     Chat.log(`about ${horse.getHorseJumpHeight().toFixed(2)} blocks up`);
     *   }
     * }
     * </pre>
     *
     * @return this horse's maximum jump height for its current jump strength.
     * @since 1.8.4
     */
    public double getHorseJumpHeight() {
        double jumpStrength = base.getAttributeValue(Attributes.JUMP_STRENGTH);
        return -0.1817584952 * Math.pow(jumpStrength, 3) + 3.689713992 * Math.pow(jumpStrength, 2) + 2.128599134 * jumpStrength - 0.343930367;
    }

    /**
     * The upper end of the range the game works in for a horse's jump strength, and a whole
     * number while {@link #getMinJumpStrengthStat() getMinJumpStrengthStat} is not. The game
     * draws a wild horse's value from this range and clamps both parents into it before
     * averaging a foal's, but the attribute itself is not limited to it.
     *
     * @return the maximum possible value of a horse's jump strength.
     * @since 1.8.4
     */
    public int getMaxJumpStrengthStat() {
        return 1;
    }

    /**
     * The lower end of the range the game works in for a horse's jump strength, and what a
     * horse spawned at the very bottom of it is given.
     *
     * @return the minimum possible value of a horse's jump strength.
     * @since 1.8.4
     */
    public double getMinJumpStrengthStat() {
        return 0.4;
    }

    /**
     * The horse's own movement speed attribute, which is the number the game multiplies into
     * how far it moves each tick. The horse family draws its value from {@link
     * #getMinSpeedStat() getMinSpeedStat} to {@link #getMaxSpeedStat() getMaxSpeedStat}.
     *
     * @return this horse's speed stat.
     * @since 1.8.4
     */
    public double getSpeedStat() {
        return base.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    /**
     * The speed attribute put into blocks per second, by the same factor of {@code 42.16}
     * the game itself uses when it turns a zombified horse's speed stat back into blocks a
     * second. The figure is as approximate as any other stat read off a mob: it says
     * nothing about the terrain, and it is not the speed the horse is actually travelling
     * at.
     *
     * @return this horse's speed in blocks per second.
     * @since 1.8.4
     */
    public double getHorseSpeed() {
        return getSpeedStat() * 42.16;
    }

    /**
     * The upper end of the range the game works in for a horse's movement speed. This is the
     * figure a fastest wild horse is drawn at, not a ceiling the attribute is clamped to.
     *
     * @return the maximum possible value of a horse's speed stat.
     * @since 1.8.4
     */
    public double getMaxSpeedStat() {
        return 0.3375;
    }

    /**
     * The lower end of the range the game works in for a horse's movement speed, which is the
     * figure a slowest wild horse is drawn at.
     *
     * @return the minimum possible value of a horse's speed stat.
     * @since 1.8.4
     */
    public double getMinSpeedStat() {
        return 0.1125;
    }

    /**
     * The returned value is equal to {@link #getMaxHealth()}.
     * <p>
     * This is the horse's maximum health attribute rather than the health it currently has,
     * and unlike every other stat on this class it is not tied to the range the min and max
     * methods report. A llama is the case in point: its maximum health is left at the base
     * horse figure, which is well above {@link #getMaxHealthStat() getMaxHealthStat}, while
     * a horse or a donkey is drawn from inside the range. An attribute command can move it
     * anywhere.
     *
     * @return this horse's health stat.
     * @since 1.8.4
     */
    public double getHealthStat() {
        return base.getMaxHealth();
    }

    /**
     * The upper end of the range the game draws a wild horse's maximum health from, and the
     * range a foal's is averaged inside. It is not a ceiling on the attribute.
     *
     * @return the maximum possible value of a horse's health stat.
     * @since 1.8.4
     */
    public int getMaxHealthStat() {
        return 30;
    }

    /**
     * The lower end of the range the game draws a wild horse's maximum health from, and the
     * range a foal's is averaged inside. It is not a floor on the attribute.
     *
     * @return the minimum possible value of a horse's health stat.
     * @since 1.8.4
     */
    public int getMinHealthStat() {
        return 15;
    }

}
