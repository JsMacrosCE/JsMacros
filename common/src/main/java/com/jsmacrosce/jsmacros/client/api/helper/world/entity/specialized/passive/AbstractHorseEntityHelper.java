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
 * Provides shared APIs for horses, donkeys, mules, and llamas.
 * <p>
 * It also exposes shared equipment and stat queries. {@link #isAngry()} reports the standing
 * flag, not anger. Saddle-slot and body-slot eligibility are separate checks, exposed by
 * {@link #canBeSaddled()} and {@link #canWearArmor()}.
 * <pre>
 * const horses = World.getEntities(32, "horse", "donkey", "llama");
 * if (horses !== null) {
 *   for (const entity of horses) {
 *     if (!(entity instanceof EntityHelpers.AbstractHorse)) continue;
 *     Chat.log(`${entity.getType()}: jump ${entity.getJumpStrengthStat()}, `
 *       + `speed ${entity.getSpeedStat()}, health ${entity.getHealthStat()}`);
 *     if (entity.canBeSaddled()) {
 *       Chat.log(`  saddleable, estimated speed ${entity.getHorseSpeed()} blocks per second`);
 *     }
 *     // the flag is the rear, not anger
 *     if (entity.isAngry()) {
 *       Chat.log("  rearing up");
 *     }
 *   }
 * }
 * </pre>
 * @param <T> the {@link AbstractHorse} subtype wrapped by this helper
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class AbstractHorseEntityHelper<T extends AbstractHorse> extends AnimalEntityHelper<T> {

    /**
     * Wraps the supplied horse entity and retains its reference for this helper.
     *
     * @param base horse entity to wrap
     */
    public AbstractHorseEntityHelper(T base) {
        super(base);
    }

    /**
     * Returns the owner's UUID string, or {@code null} when no owner reference is present.
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
     * Returns whether this horse is marked as tamed.
     *
     * @return {@code true} if this horse is already tamed, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isTame() {
        return base.isTamed();
    }

    /**
     * Returns whether the horse has a saddle equipped. This differs from
     * {@link #canBeSaddled()}, which reports whether the saddle slot is usable.
     *
     * @return {@code true} if this horse is saddled, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSaddled() {
        return base.isSaddled();
    }

    /**
     * Returns the horse's standing/rearing flag; despite the method name, this does not
     * indicate anger.
     *
     * @return {@code true} if this horse is rearing up on its hind legs, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isAngry() {
        return base.isStanding();
    }

    /**
     * Returns the horse's bred flag.
     *
     * @return {@code true} when the horse's bred flag is set; {@code false} otherwise
     * @since 1.8.4
     */
    public boolean isBred() {
        return base.isBred();
    }

    /**
     * Returns whether the horse is in its eating state.
     *
     * @return {@code true} if this horse is currently eating, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isEating() {
        return base.isEating();
    }

    /**
     * Returns whether the horse can use its body equipment slot.
     *
     * @return {@code true} if this horse can wear armor, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canWearArmor() {
        return base.canUseSlot(EquipmentSlot.BODY);
    }

    /**
     * Returns whether the horse can use its saddle slot.
     *
     * @return {@code true} if this horse can be saddled, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canBeSaddled() {
        return base.canUseSlot(EquipmentSlot.SADDLE);
    }

    /**
     * Returns the number of slots in the horse's inventory.
     *
     * @return this horse's inventory size.
     * @since 1.8.4
     */
    public int getInventorySize() {
        return ((MixinAbstractHorseEntity) base).invokeGetInventorySize();
    }

    /**
     * Returns this horse's jump-strength attribute value.
     *
     * @return this horse's jump strength.
     * @since 1.8.4
     */
    public double getJumpStrengthStat() {
        return base.getAttributeValue(Attributes.JUMP_STRENGTH);
    }

    /**
     * Estimates jump height from the jump-strength attribute using a fitted approximation;
     * the game does not provide this height value.
     *
     * @return the estimated jump height for this horse's current jump strength.
     * @since 1.8.4
     */
    public double getHorseJumpHeight() {
        double jumpStrength = base.getAttributeValue(Attributes.JUMP_STRENGTH);
        return -0.1817584952 * Math.pow(jumpStrength, 3) + 3.689713992 * Math.pow(jumpStrength, 2) + 2.128599134 * jumpStrength - 0.343930367;
    }

    /**
     * Returns the upper bound used when generating wild horse jump strength and breeding foals.
     * It is not a limit on the attribute itself.
     *
     * @return the maximum possible value of a horse's jump strength.
     * @since 1.8.4
     */
    public int getMaxJumpStrengthStat() {
        return 1;
    }

    /**
     * Returns the lower bound used when generating wild horse jump strength and breeding foals.
     *
     * @return the minimum possible value of a horse's jump strength.
     * @since 1.8.4
     */
    public double getMinJumpStrengthStat() {
        return 0.4;
    }

    /**
     * Returns this horse's movement-speed attribute value.
     *
     * @return this horse's speed stat.
     * @since 1.8.4
     */
    public double getSpeedStat() {
        return base.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    /**
     * Converts the movement-speed attribute to an estimated speed in blocks per second by
     * multiplying it by {@code 42.16}.
     *
     * @return this horse's speed in blocks per second.
     * @since 1.8.4
     */
    public double getHorseSpeed() {
        return getSpeedStat() * 42.16;
    }

    /**
     * Returns the upper bound used when generating wild horse movement speed and breeding foals.
     * It is not a limit on the attribute itself.
     *
     * @return the maximum possible value of a horse's speed stat.
     * @since 1.8.4
     */
    public double getMaxSpeedStat() {
        return 0.3375;
    }

    /**
     * Returns the lower bound used when generating wild horse movement speed and breeding foals.
     *
     * @return the minimum possible value of a horse's speed stat.
     * @since 1.8.4
     */
    public double getMinSpeedStat() {
        return 0.1125;
    }

    /**
     * Returns this horse's maximum-health attribute value, not its current health.
     *
     * @return this horse's health stat.
     * @since 1.8.4
     */
    public double getHealthStat() {
        return base.getMaxHealth();
    }

    /**
     * Returns the upper bound used when generating wild horse maximum health and breeding foals.
     * It is not a limit on the attribute itself.
     *
     * @return the maximum possible value of a horse's health stat.
     * @since 1.8.4
     */
    public int getMaxHealthStat() {
        return 30;
    }

    /**
     * Returns the lower bound used to generate wild horse maximum health and calculate foal
     * maximum health; it is not a floor on the attribute.
     *
     * @return the minimum possible value of a horse's health stat.
     * @since 1.8.4
     */
    public int getMinHealthStat() {
        return 15;
    }

}
