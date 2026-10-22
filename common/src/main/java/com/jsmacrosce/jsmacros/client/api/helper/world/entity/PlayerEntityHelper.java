package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.FishingBobberEntityHelper;

/**
 * Provides player-specific entity data, including experience, score, and sleep state.
 * Equipment accessors are inherited in behavior from {@link LivingEntityHelper}; the
 * player overrides return those same six hand and armor slots, not additional slots.
 * <pre>
 * // the local player, which is the only one a client has any business with
 * const player = Player.getPlayer();
 * if (player !== null) {
 *   Chat.log(`${player.getPlayerName()} on ${player.getXPLevel()} `
 *     + `(${player.getXP()} xp total), score ${player.getScore()}`);
 * }
 * </pre>
 *
 * @param <T> the player entity type wrapped by this helper
 * @author Wagyourtail
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class PlayerEntityHelper<T extends Player> extends LivingEntityHelper<T> {

    /** Wraps the supplied player entity and retains that reference.
     * @param e the player entity to wrap
     */
    public PlayerEntityHelper(T e) {
        super(e);
    }

    /**
     * Returns the account name from the player's game profile, rather than the entity's display name.
     *
     * @return the player's account name, not its display name
     * @since 1.8.4
     */
    public String getPlayerName() {
        //? if >1.21.8 {
        /*return base.getGameProfile().name();
        *///?} else {
        return base.getGameProfile().getName();
        //?}
    }

    /**
     * Returns a helper wrapping this player's abilities object; setter calls modify that object.
     *
     * @return a wrapper on this player's abilities
     * @since 1.0.3
     */
    public PlayerAbilitiesHelper getAbilities() {
        return new PlayerAbilitiesHelper(base.getAbilities());
    }

    /**
     * Returns the player's main-hand stack.
     * @return the player's main-hand item stack, empty when the hand is empty
     * @since 1.2.0
     */
    @Override
    public ItemStackHelper getMainHand() {
        return super.getMainHand();
    }

    /**
     * Returns the player's off-hand stack.
     * @return the player's off-hand item stack, empty when the hand is empty
     * @since 1.2.0
     */
    @Override
    public ItemStackHelper getOffHand() {
        return super.getOffHand();
    }

    /**
     * Returns the player's helmet-slot stack.
     * @return the player's helmet-slot item stack, empty when the slot is empty
     * @since 1.2.0
     */
    @Override
    public ItemStackHelper getHeadArmor() {
        return super.getHeadArmor();
    }

    /**
     * Returns the player's chest-armor-slot stack.
     * @return the player's chest-armor-slot item stack, empty when the slot is empty
     * @since 1.2.0
     */
    @Override
    public ItemStackHelper getChestArmor() {
        return super.getChestArmor();
    }

    /**
     * Returns the player's leggings-slot stack.
     * @return the player's leggings-slot item stack, empty when the slot is empty
     * @since 1.2.0
     */
    @Override
    public ItemStackHelper getLegArmor() {
        return super.getLegArmor();
    }

    /**
     * Returns the player's boots-slot stack.
     * @return the player's boots-slot item stack, empty when the slot is empty
     * @since 1.2.0
     */
    @Override
    public ItemStackHelper getFootArmor() {
        return super.getFootArmor();
    }

    /**
     * Returns the player's accumulated total experience points, distinct from current-level progress.
     *
     * @return the total experience the player has collected
     * @since 1.2.5
     */
    public int getXP() {
        return base.totalExperience;
    }

    /**
     * the player's current experience level.
     *
     * @return the player's experience level
     * @since 1.6.5
     */
    public int getXPLevel() {
        return base.experienceLevel;
    }

    /**
     * the player's progress through the current experience level, as a fraction of the
     * level's total cost.
     *
     * @return the fraction of the current level the player has filled
     * @since 1.6.5
     */
    public float getXPProgress() {
        return base.experienceProgress;
    }

    /**
     * the total experience-point cost of the next level, determined by the player's
     * current level, not the points remaining in it.
     *
     * @return the experience needed to reach the next level
     * @since 1.6.5
     */
    public int getXPToLevelUp() {
        return base.getXpNeededForNextLevel();
    }

    /**
     * whether the player is currently sleeping.
     *
     * @return {@code true} if the player is asleep, {@code false} otherwise.
     * @since 1.2.6
     */
    @Override
    public boolean isSleeping() {
        return super.isSleeping();
    }

    /**
     * whether the player is sleeping and has met the game's 100-tick sleep threshold.
     *
     * @return if the player has slept the minimum amount of time to pass the night.
     * @since 1.2.6
     */
    public boolean isSleepingLongEnough() {
        return base.isSleepingLongEnough();
    }

    /**
     * this player's fishing bobber, or {@code null} when none is attached. The returned
     * wrapper refers to that bobber entity; this works for local and other players.
     *
     * @return the fishing bobber of the player, or {@code null} if the player is not fishing.
     * @since 1.8.4
     */
    @Nullable
    public FishingBobberEntityHelper getFishingBobber() {
        return base.fishing == null ? null : new FishingBobberEntityHelper(base.fishing);
    }

    /**
     * the player's attack cooldown progress, clamped to the range {@code 0.0} to
     * {@code 1.0}.
     *
     * @return the attack charge as a fraction from zero to one
     * @since 1.8.4
     */
    public float getAttackCooldownProgress() {
        return base.getAttackStrengthScale(0);
    }

    /**
     * the number of ticks in a full attack cooldown, calculated from the player's attack
     * speed attribute.
     *
     * @return the number of ticks a full attack charge takes
     * @since 1.8.4
     */
    public float getAttackCooldownProgressPerTick() {
        return base.getCurrentItemAttackStrengthDelay();
    }

    /**
     * the integer score stored on the player entity.
     *
     * @return the player's score
     * @since 1.8.4
     */
    public int getScore() {
        return base.getScore();
    }

}
