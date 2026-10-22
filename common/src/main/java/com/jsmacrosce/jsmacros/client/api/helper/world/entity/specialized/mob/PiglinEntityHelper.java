package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinArmPose;
import com.jsmacrosce.doclet.DocletCategory;

/**
 * the piglin, and the six things it can be doing.
 * <p>
 * A piglin has one arm pose and the game works all six of these off that one value, so
 * they are six readings of the same number rather than six flags:
 * {@link #isWandering() isWandering} is the fallback the game returns when nothing else
 * applies, {@link #isDancing() isDancing} is the piglin dancing, {@link #isAdmiring()
 * isAdmiring} is it holding a loved item up to look at, and the other three are the
 * crossbow and the sword.
 * <p>
 * Exactly one of the six is true at a time, and they are read in the order the game works
 * them out in — dancing first, then admiring, then the melee swing, then the two crossbow
 * stages, with wandering as the fallback. So a piglin that is both admiring an item and
 * holding a loaded crossbow reports the admiring one. The zombification flag the piglin
 * family shares is on {@code AbstractPiglinEntityHelper}, one class up.
 * example:
 * <pre>
 * const PiglinEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.PiglinEntityHelper");
 * const piglins = World.getEntities(32, "piglin");
 * if (piglins !== null) {
 *   for (const entity of piglins) {
 *     const piglin = PiglinEntityHelper.class.cast(entity);
 *     // one pose and six readings of it, so the order has to be the game's own
 *     let doing = "wander";
 *     if (piglin.isDancing()) {
 *       doing = "dance";
 *     } else {
 *       if (piglin.isAdmiring()) {
 *         doing = "admire";
 *       } else {
 *         if (piglin.isMeleeAttacking()) {
 *           doing = "swing";
 *         } else {
 *           if (piglin.isChargingCrossbow()) {
 *             doing = "draw";
 *           } else {
 *             if (piglin.hasCrossbowReady()) {
 *               doing = "loaded";
 *             }
 *           }
 *         }
 *       }
 *     }
 *     if (piglin.isWandering()) {
 *       doing = "wander";
 *     }
 *     if (doing === "wander") {
 *       Chat.log(`a piglin at ${piglin.getPos()} is doing nothing in particular`);
 *     } else {
 *       Chat.log(`a piglin at ${piglin.getPos()} is ${doing}`);
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
public class PiglinEntityHelper extends AbstractPiglinEntityHelper<Piglin> {

    public PiglinEntityHelper(Piglin base) {
        super(base);
    }

    /**
     * Whether the piglin is doing nothing special, which is the game's fallback pose: the
     * game returns it once none of dancing, admiring, attacking, charging a crossbow or
     * holding a loaded one applies. So this is the "none of the other five" reading rather
     * than a flag of its own.
     * example:
     * <pre>
     * const PiglinEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.PiglinEntityHelper");
     * const piglins = World.getEntities(32, "piglin");
     * if (piglins !== null) {
     *   for (const entity of piglins) {
     *     const piglin = PiglinEntityHelper.class.cast(entity);
     *     if (piglin.isWandering()) {
     *       // nothing special: not dancing, not admiring, no crossbow out
     *       Chat.log(`a wandering piglin at ${piglin.getPos()}, health ${piglin.getHealth()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this piglin is doing nothing special, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isWandering() {
        return base.getArmPose() == PiglinArmPose.DEFAULT;
    }

    /**
     * Whether the piglin is dancing, which the game keeps as a flag of its own rather than
     * working out from what the piglin is holding. The game rewrites it every step of the
     * piglin's brain from that brain's own dancing memory, and clears it once the memory
     * of where the dancing started is gone.
     * example:
     * <pre>
     * const PiglinEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.PiglinEntityHelper");
     * const piglins = World.getEntities(32, "piglin");
     * if (piglins !== null) {
     *   for (const entity of piglins) {
     *     const piglin = PiglinEntityHelper.class.cast(entity);
     *     if (piglin.isDancing()) {
     *       // a dancing piglin is not also admiring or holding a crossbow out
     *       Chat.log(`a dancing piglin at ${piglin.getPos()}, wandering: ${piglin.isWandering()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this piglin is dancing to music, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isDancing() {
        return base.isDancing();
    }

    /**
     * Whether the piglin is admiring an item, which is the pose the game puts on a piglin
     * holding one of the items it loves — gold, for instance. It is a reading of what is in
     * the off hand rather than a flag, so it follows the item rather than an animation.
     * example:
     * <pre>
     * const PiglinEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.PiglinEntityHelper");
     * const piglins = World.getEntities(32, "piglin");
     * if (piglins !== null) {
     *   for (const entity of piglins) {
     *     const piglin = PiglinEntityHelper.class.cast(entity);
     *     if (piglin.isAdmiring()) {
     *       // whatever it is holding up is what the game calls a loved item
     *       Chat.log(`a piglin at ${piglin.getPos()} is holding up ${piglin.getOffHand().getName().getString()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this piglin is admiring an item, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAdmiring() {
        return base.getArmPose() == PiglinArmPose.ADMIRING_ITEM;
    }

    /**
     * Whether the piglin is swinging at something, which the game decides from two things
     * at once: that it is aggressive and that what it has in its main hand is a melee
     * weapon. A piglin holding a crossbow is never in this pose, however angry it is.
     * example:
     * <pre>
     * const PiglinEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.PiglinEntityHelper");
     * const piglins = World.getEntities(32, "piglin");
     * if (piglins !== null) {
     *   for (const entity of piglins) {
     *     const piglin = PiglinEntityHelper.class.cast(entity);
     *     if (piglin.isMeleeAttacking()) {
     *       // aggressive with a melee weapon, rather than with a crossbow
     *       Chat.log(`a piglin at ${piglin.getPos()} is swinging ${piglin.getMainHand().getName().getString()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this piglin is attacking another entity, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isMeleeAttacking() {
        return base.getArmPose() == PiglinArmPose.ATTACKING_WITH_MELEE_WEAPON;
    }

    /**
     * Whether the piglin is drawing a crossbow back. This is the charging stage, and the
     * pose it puts on is the one the game means by charged: the piglin has not finished
     * pulling the string yet, so {@link #hasCrossbowReady() hasCrossbowReady} is
     * {@code false} for the same piglin.
     * example:
     * <pre>
     * const PiglinEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.PiglinEntityHelper");
     * const piglins = World.getEntities(32, "piglin");
     * if (piglins !== null) {
     *   for (const entity of piglins) {
     *     const piglin = PiglinEntityHelper.class.cast(entity);
     *     if (piglin.isChargingCrossbow()) {
     *       // still pulling the string, so the crossbow is not ready yet
     *       Chat.log(`a piglin at ${piglin.getPos()} is mid-draw`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this piglin is currently charging its crossbow, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isChargingCrossbow() {
        return base.getArmPose() == PiglinArmPose.CROSSBOW_CHARGE;
    }

    /**
     * Whether the piglin is holding a crossbow that is already loaded, which is the stage
     * after {@link #isChargingCrossbow() isChargingCrossbow}. The game works this one out
     * from the crossbow itself rather than from a flag, so it tracks the weapon the piglin
     * actually has in its hand.
     * example:
     * <pre>
     * const PiglinEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.PiglinEntityHelper");
     * const piglins = World.getEntities(32, "piglin");
     * if (piglins !== null) {
     *   for (const entity of piglins) {
     *     const piglin = PiglinEntityHelper.class.cast(entity);
     *     if (piglin.hasCrossbowReady()) {
     *       // the other half of the crossbow pair: loaded rather than loading
     *       Chat.log(`a piglin at ${piglin.getPos()} has a loaded crossbow, charging: ${piglin.isChargingCrossbow()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this piglin has its crossbow fully charged, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean hasCrossbowReady() {
        return base.getArmPose() == PiglinArmPose.CROSSBOW_HOLD;
    }

}
