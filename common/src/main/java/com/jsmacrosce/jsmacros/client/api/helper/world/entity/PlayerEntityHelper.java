package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.FishingBobberEntityHelper;

/**
 * a player entity: the things a player has that an ordinary living entity does not,
 * which is experience, a score, sleep, and the equipment slots a player can actually
 * wear.
 * <p>
 * The equipment overrides here are the ones a player fills in differently from any other
 * mob. A {@link LivingEntityHelper} reads the same six slots for anything that has them,
 * and a player has all six, so these do not add anything a caller cannot already reach;
 * they are the names a script reaches for when it means the player rather than the mob.
 * example:
 * <pre>
 * // the local player, which is the only one a client has any business with
 * const player = Player.getPlayer();
 * if (player !== null) {
 *   Chat.log(`${player.getPlayerName()} on ${player.getXPLevel()} `
 *     + `(${player.getXP()} xp total), score ${player.getScore()}`);
 *   // the four armour slots, read as a player rather than as a living entity
 *   Chat.log(`helmet: ${player.getHeadArmor().getName()}`);
 *   Chat.log(`chest:  ${player.getChestArmor().getName()}`);
 *   Chat.log(`legs:   ${player.getLegArmor().getName()}`);
 *   Chat.log(`boots:  ${player.getFootArmor().getName()}`);
 * }
 * </pre>
 *
 * @author Wagyourtail
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class PlayerEntityHelper<T extends Player> extends LivingEntityHelper<T> {

    public PlayerEntityHelper(T e) {
        super(e);
    }

    /**
     * the player's account name, read out of the account data the server sent with the
     * entity. A server can set a display name over the top of it, and that is what
     * {@link EntityHelper#getName()} reads; this is the name underneath.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // the account name, next to whatever the world is showing
     *   Chat.log(`account ${player.getPlayerName()}, shown as ${player.getName()}`);
     * }
     * </pre>
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
     * the abilities block belonging to this player, which is where the flying, the
     * invulnerability, the creative flag and the two speed multipliers live. It is a
     * wrapper on the block the player actually uses rather than a copy, so writing
     * through one of its setters changes the player's own abilities.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const abilities = player.getAbilities();
     *   if (abilities.getCreativeMode()) {
     *     abilities.setFlySpeed(0.1);
     *     Chat.log(`flying at ${abilities.getFlySpeed()}`);
     *   }
     * }
     * </pre>
     *
     * @return a wrapper on this player's abilities
     * @since 1.0.3
     */
    public PlayerAbilitiesHelper getAbilities() {
        return new PlayerAbilitiesHelper(base.getAbilities());
    }

    /**
     * the item in the player's main hand. A player has this slot and an ordinary living
     * entity has one too, so this is the same read as the inherited one under the name a
     * script means by it.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const held = player.getMainHand();
     *   if (!held.isEmpty()) {
     *     Chat.log(`holding ${held.getCount()} ${held.getItemId()}`);
     *   }
     * }
     * </pre>
     *
     * @return the item in the player's main hand
     * @since 1.2.0
     */
    @Override
    public ItemStackHelper getMainHand() {
        return super.getMainHand();
    }

    /**
     * the item in the player's off hand, which is the second hand slot. A player can
     * hold a shield, a totem or a second tool there, and it is empty when nothing is
     * held.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const off = player.getOffHand();
     *   Chat.log(off.isEmpty() ? "off hand empty" : `off hand: ${off.getName()}`);
     * }
     * </pre>
     *
     * @return the item in the player's off hand
     * @since 1.2.0
     */
    @Override
    public ItemStackHelper getOffHand() {
        return super.getOffHand();
    }

    /**
     * the item in the player's helmet slot. The slot exists whether or not anything is
     * in it, so an empty result is the answer rather than a failure.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const head = player.getHeadArmor();
     *   if (!head.isEmpty()) {
     *     Chat.log(`wearing ${head.getName()}, ${head.getDurability()} damage`);
     *   }
     * }
     * </pre>
     *
     * @return the item in the player's helmet slot
     * @since 1.2.0
     */
    @Override
    public ItemStackHelper getHeadArmor() {
        return super.getHeadArmor();
    }

    /**
     * the item in the player's chestplate slot, which is the body armour piece.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const chest = player.getChestArmor();
     *   // the armour value and toughness are on the entity rather than on the piece
     *   if (!chest.isEmpty()) {
     *     Chat.log(`wearing ${chest.getName()}, `
     *       + `${player.getArmor()} armour and ${player.getArmorToughness()} toughness`);
     *   }
     * }
     * </pre>
     *
     * @return the item in the player's chestplate slot
     * @since 1.2.0
     */
    @Override
    public ItemStackHelper getChestArmor() {
        return super.getChestArmor();
    }

    /**
     * the item in the player's leggings slot.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const legs = player.getLegArmor();
     *   Chat.log(legs.isEmpty() ? "no leggings" : `wearing ${legs.getName()}`);
     * }
     * </pre>
     *
     * @return the item in the player's leggings slot
     * @since 1.2.0
     */
    @Override
    public ItemStackHelper getLegArmor() {
        return super.getLegArmor();
    }

    /**
     * the item in the player's boots slot.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const feet = player.getFootArmor();
     *   Chat.log(feet.isEmpty() ? "barefoot" : `wearing ${feet.getName()}`);
     * }
     * </pre>
     *
     * @return the item in the player's boots slot
     * @since 1.2.0
     */
    @Override
    public ItemStackHelper getFootArmor() {
        return super.getFootArmor();
    }

    /**
     * the total experience the player has collected, which is the lifetime figure and not
     * the progress through the current level. It is the number a death sends the player
     * back by, so it is the one to read for a total rather than
     * {@link #getXPProgress()}, which is the fraction within the level.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const level = player.getXPLevel();
     *   const into = player.getXPProgress() * player.getXPToLevelUp();
     *   Chat.log(`level ${level}, ${Math.floor(into)} of `
     *     + `${player.getXPToLevelUp()} through it, ${player.getXP()} collected`);
     * }
     * </pre>
     *
     * @return the total experience the player has collected
     * @since 1.2.5 [citation needed]
     */
    public int getXP() {
        return base.totalExperience;
    }

    /**
     * the player's experience level, which is the whole number the bar is measured
     * against. The game grows the cost of each level as they go, so
     * {@link #getXPToLevelUp()} is larger the further along the player is.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const level = player.getXPLevel();
     *   const needed = player.getXPToLevelUp();
     *   Chat.log(`level ${level}, next one costs ${needed} points`);
     * }
     * </pre>
     *
     * @return the player's experience level
     * @since 1.6.5
     */
    public int getXPLevel() {
        return base.experienceLevel;
    }

    /**
     * how far through the current level the player is, as a fraction from zero up to but
     * not including one. It is a fraction rather than a count of points, so it has to be
     * multiplied by {@link #getXPToLevelUp()} to get a number of experience, and the
     * product is the one that is usually wanted for drawing a bar.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const fraction = player.getXPProgress();
     *   if (fraction > 0) {
     *     Chat.log(`${Math.round(fraction * 100)}% through level ${player.getXPLevel()}`);
     *   }
     * }
     * </pre>
     *
     * @return the fraction of the current level the player has filled
     * @since 1.6.5
     */
    public float getXPProgress() {
        return base.experienceProgress;
    }

    /**
     * how many experience points the player needs to reach the next level from where they
     * are now. The game works this out from the current level rather than from what has
     * been banked, so the number goes up as the player does: seven points more than
     * twice the level below fifteen, then five points more per level up to thirty, then
     * nine a level after that.
     * <p>
     * It is the whole cost of the next level rather than what is left of it. The
     * remainder is this multiplied by {@link #getXPProgress()}.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const needed = player.getXPToLevelUp();
     *   const done = needed * player.getXPProgress();
     *   Chat.log(`${Math.floor(done)} of ${needed} through level ${player.getXPLevel()}`);
     * }
     * </pre>
     *
     * @return the experience needed to reach the next level
     * @since 1.6.5
     */
    public int getXPToLevelUp() {
        return base.getXpNeededForNextLevel();
    }

    /**
     * whether the player is asleep in a bed. The two halves of sleeping are read
     * separately: this says the player is in the bed at all, and
     * {@link #isSleepingLongEnough()} says whether they have been long enough for the
     * night to be skipped.
     * example:
     * <pre>
     * const players = World.getEntities("player");
     * if (players !== null) {
     *   for (const entity of players) {
     *     const player = entity.asPlayer();
     *     if (player.isSleeping()) {
     *       Chat.log(`${player.getPlayerName()} is in bed, `
     *         + `long enough: ${player.isSleepingLongEnough()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the player is asleep, {@code false} otherwise.
     * @since 1.2.5 [citation needed]
     */
    @Override
    public boolean isSleeping() {
        return super.isSleeping();
    }

    /**
     * whether the player has been asleep long enough for the night to be skipped, which
     * the game counts as a hundred ticks. The two halves go together: this is only
     * {@code true} while {@link #isSleeping()} is, so a player who has just got out of
     * bed answers {@code false} here as well.
     * <p>
     * A player who is being kept awake has the counter held back, so this can stay
     * {@code false} for as long as the player is in the bed.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   if (player.isSleepingLongEnough()) {
     *     // the night is being skipped and the player is on their way out of the bed
     *     Chat.log("the night is skipped");
     *   }
     * }
     * </pre>
     *
     * @return if the player has slept the minimum amount of time to pass the night.
     * @since 1.2.5 [citation needed]
     */
    public boolean isSleepingLongEnough() {
        return base.isSleepingLongEnough();
    }

    /**
     * the bobber the player has cast, or {@code null} when they are not fishing. The
     * bobber is a world entity of its own, so this hands back a wrapper on it rather
     * than a copy, and a bobber that has been reeled in comes back as {@code null} from
     * that moment on.
     * <p>
     * The check is worth making: an entity that happens to be the player but is not the
     * local one is still a player entity, and this answers for that player rather than
     * for the one the script is probably asking about.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const bobber = player.getFishingBobber();
     *   if (bobber !== null) {
     *     Chat.log(`the line is out at ${bobber.getPos()}`);
     *   } else {
     *     Chat.log("not fishing");
     *   }
     * }
     * </pre>
     *
     * @return the fishing bobber of the player, or {@code null} if the player is not fishing.
     * @since 1.8.4
     */
    @Nullable
    public FishingBobberEntityHelper getFishingBobber() {
        return base.fishing == null ? null : new FishingBobberEntityHelper(base.fishing);
    }

    /**
     * how charged the player's attack is, as a fraction from zero up to one. It reaches
     * one only when the attack has been fully wound up, and the game itself is what
     * resets it: the value climbs from zero as the player holds the attack and drops
     * back when an attack is made, so the number to read is the one at the moment the
     * attack lands rather than one saved earlier.
     * <p>
     * The full charge takes {@link #getAttackCooldownProgressPerTick()} ticks, so the
     * two are the same measurement in different units.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const charge = player.getAttackCooldownProgress();
     *   if (charge >= 1) {
     *     Chat.log("fully charged");
     *   } else {
     *     const ticksLeft = player.getAttackCooldownProgressPerTick() * (1 - charge);
     *     Chat.log(`${Math.round(charge * 100)}%, about ${Math.ceil(ticksLeft)} ticks left`);
     *   }
     * }
     * </pre>
     *
     * @return the attack charge as a fraction from zero to one
     * @since 1.8.4
     */
    public float getAttackCooldownProgress() {
        return base.getAttackStrengthScale(0);
    }

    /**
     * how many ticks a full attack charge takes, which is the attack speed attribute
     * turned into ticks: twenty divided by the attribute, so five seconds' worth of
     * ticks at the game's own attack speed of four. It is a length of time rather than
     * a rate of change, which the name does not make obvious, and a bigger number here
     * means a slower attack.
     * <p>
     * The player's attack speed attribute is what sets it, so a player in a slow weapon
     * or a modded gamemode reads a different number from the default.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const full = player.getAttackCooldownProgressPerTick();
     *   // a fraction of the way charged, and the ticks that leaves
     *   const charge = player.getAttackCooldownProgress();
     *   Chat.log(`${Math.round(charge * 100)}% of ${full} ticks`);
     * }
     * </pre>
     *
     * @return the number of ticks a full attack charge takes
     * @since 1.8.4
     */
    public float getAttackCooldownProgressPerTick() {
        return base.getCurrentItemAttackStrengthDelay();
    }

    /**
     * the score the player entity carries, which is the number the game shows on a
     * scoreboard and the one {@code /score} writes to. It is separate from experience:
     * it is a whole number the game keeps on the entity for display and bookkeeping, and
     * it is not read off the player's own progress through the game.
     * <p>
     * Nothing on this class sets it, so a script reads a score rather than giving one.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   Chat.log(`${player.getPlayerName()} has a score of ${player.getScore()}`);
     * }
     * </pre>
     *
     * @return the player's score.
     * @since 1.8.4
     */
    public int getScore() {
        return base.getScore();
    }

}
