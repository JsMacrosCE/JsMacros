package com.jsmacrosce.jsmacros.client.api.helper;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.FluidStateHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;

/**
 * a block-state condition: which states of a block something is allowed to be.
 * <p>
 * A block state is the set of properties a block has right now — a log's axis, a stair's facing,
 * a slab's half — and this is a test over those rather than over the block itself. It is the
 * narrow half of a {@link BlockPredicateHelper}: that one picks which blocks are acceptable and
 * this picks which of their states are.
 * <p>
 * The condition is written as a map from property name to a set of acceptable values, and it
 * passes when the state under test agrees with every entry. A property the condition does not
 * mention is not checked, so a condition naming only the axis accepts a log whichever way it
 * points.
 * <br>
 * It is reached through {@link BlockPredicateHelper#getStatePredicate()}, which gives
 * {@code null} when the surrounding condition has no state part, and it needs the actual state
 * to run against — which is what the two {@code test} calls take.
 * example:
 * <pre>
 * const player = Player.getPlayer();
 * if (player === null) { throw new Error("not in a world"); }
 * const elytra = player.getMainHand();
 *
 * for (const condition of elytra.getDestroyRestrictions()) {
 *   const states = condition.getStatePredicate();
 *   if (states !== null) {
 *     // ask the world what is actually there, then test the state
 *     const below = World.getBlock(player.getBlockPos().down());
 *     if (below !== null) {
 *       Chat.log(`the block below matches: ${states.test(below.getBlockStateHelper())}`);
 *     }
 *   }
 * }
 * </pre>
 *
 * @since 1.9.1
 */
@DocletCategory("Misc Helpers")
public class StatePredicateHelper extends BaseHelper<StatePropertiesPredicate> {

    public StatePredicateHelper(StatePropertiesPredicate base) {
        super(base);
    }

    /**
     * whether a block state satisfies this condition.
     * <p>
     * This checks every property the condition names and ignores the ones it does not, so a
     * condition naming a single property accepts a block whose other properties are anything.
     * That makes it narrower than a bare block check and wider than nothing.
     * <br>
     * The state is taken as given rather than read from the world, so a script can test a state
     * it built itself against a condition that came from somewhere else.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const elytra = player.getMainHand();
     *
     * for (const condition of elytra.getDestroyRestrictions()) {
     *   const states = condition.getStatePredicate();
     *   if (states !== null) {
     *     const below = World.getBlock(player.getBlockPos().down());
     *     if (below !== null) {
     *       Chat.log(`the block below matches: ${states.test(below.getBlockStateHelper())}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @since 1.9.1
     */
    public boolean test(BlockStateHelper state) {
        return base.matches(state.getRaw());
    }

    /**
     * whether a fluid state satisfies this condition.
     * <p>
     * The same test as the block form, for the case where what is under test is a fluid rather
     * than a block — water or lava in a world, or a bucket's contents. The condition's
     * properties are matched against the fluid's own, so a condition written for blocks will
     * not match a fluid unless its properties happen to line up.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const elytra = player.getMainHand();
     *
     * for (const condition of elytra.getDestroyRestrictions()) {
     *   const states = condition.getStatePredicate();
     *   if (states !== null) {
     *     const below = World.getBlock(player.getBlockPos().down());
     *     if (below !== null) {
     *       // the fluid the block holds, which a waterlogged block has
     *       const fluid = below.getBlockStateHelper().getFluidState();
     *       Chat.log(`the fluid matches: ${states.test(fluid)}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @since 1.9.1
     */
    public boolean test(FluidStateHelper state) {
        return base.matches(state.getRaw());
    }

}
