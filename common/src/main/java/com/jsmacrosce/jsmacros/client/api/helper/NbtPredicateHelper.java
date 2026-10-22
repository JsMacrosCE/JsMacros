package com.jsmacrosce.jsmacros.client.api.helper;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import net.minecraft.advancements.critereon.NbtPredicate;

/**
 * a data condition: which block data, item data or entity data something is allowed to be.
 * <p>
 * This is the third part of a {@link BlockPredicateHelper} and the only one that looks at data
 * rather than at the block itself, so it is the one that can be applied to things that are not
 * blocks in the world: an item stack, an entity, or a piece of nbt on its own. What it checks
 * is a nested map of paths to expected values, so it is the condition to use for something like
 * "a chest containing exactly these items" or "an entity with this one piece of data set".
 * <p>
 * The condition is a filter rather than an equality: it matches when the data at every path it
 * names is equal to what it expects, and says nothing at all about anything it does not name.
 * A condition with nothing in it therefore matches everything, which is the same shape as a
 * block condition with no blocks.
 * <br>
 * It is reached through {@link BlockPredicateHelper#getNbtPredicate()}, which gives
 * {@code null} when the surrounding condition has no data part.
 * example:
 * <pre>
 * const player = Player.getPlayer();
 * if (player === null) { throw new Error("not in a world"); }
 * const elytra = player.getMainHand();
 *
 * for (const condition of elytra.getDestroyRestrictions()) {
 *   const data = condition.getNbtPredicate();
 *   if (data !== null) {
 *     // the data on the block below, tested as data
 *     const below = World.getBlock(player.getBlockPos().down());
 *     if (below !== null) {
 *       const nbt = below.getNBT();
 *       if (nbt !== null) {
 *         Chat.log(`the block below matches: ${data.test(nbt)}`);
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * @since 1.9.1
 */
@DocletCategory("Misc Helpers")
public class NbtPredicateHelper extends BaseHelper<NbtPredicate> {

    public NbtPredicateHelper(NbtPredicate base) {
        super(base);
    }

    /**
     * whether an entity's data satisfies this condition.
     * <p>
     * This is the form to use for anything the game hangs data off a living thing — a named
     * mob, an entity carrying equipment, an item frame. It reads the entity's own data, so an
     * entity in the world is all that is needed and nothing has to be built.
     * <br>
     * A condition with nothing in it matches every entity, and one that names a path the entity
     * does not have does not match it at all.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const elytra = player.getMainHand();
     *
     * for (const condition of elytra.getDestroyRestrictions()) {
     *   const data = condition.getNbtPredicate();
     *   if (data !== null) {
     *     const mobs = World.getEntities("minecraft:villager");
     *     if (mobs !== null) {
     *       for (const mob of mobs) {
     *         Chat.log(`${mob.getName()} matches: ${data.test(mob)}`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @since 1.9.1
     */
    public boolean test(EntityHelper<?> entity) {
        return base.matches(entity.getRaw());
    }

    /**
     * whether an item stack's data satisfies this condition.
     * <p>
     * The form to use for a stack rather than for something placed in the world. What it reads
     * is the stack's {@code CUSTOM_DATA} component and nothing else, so it sees the same thing a
     * recipe's ingredient test sees.
     * <br>
     * A stack with a custom name will fail a condition written for a plain one, and the reason
     * is the one that is easy to get backwards: the name does not go into that component at all,
     * it has a component of its own, so a renamed item has <i>less</i> for this test to look at
     * rather than more. A condition that names a key the stack's custom data does not carry
     * fails for the ordinary reason, which is that the key is missing.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const elytra = player.getMainHand();
     *
     * for (const condition of elytra.getDestroyRestrictions()) {
     *   const data = condition.getNbtPredicate();
     *   if (data !== null) {
     *     // the held stack, tested as a stack
     *     Chat.log(`the held stack matches: ${data.test(elytra)}`);
     *   }
     * }
     * </pre>
     *
     * @since 1.9.1
     */
    public boolean test(ItemStackHelper itemStack) {
        return base.matches(itemStack.getRaw());
    }

    /**
     * whether a piece of data on its own satisfies this condition.
     * <p>
     * The form to use when the data is already in hand rather than attached to a block or an
     * item — a compound read out of a command, or out of another piece of data. It is the most
     * direct of the three and the easiest to reason about, since nothing is looked up.
     * <br>
     * Because the test is over the data alone rather than over the thing carrying it, a
     * condition that a real item would fail can still pass here on a hand-built compound with
     * just the parts named.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const elytra = player.getMainHand();
     *
     * for (const condition of elytra.getDestroyRestrictions()) {
     *   const data = condition.getNbtPredicate();
     *   if (data !== null) {
     *     const nbt = elytra.getNBT();
     *     if (nbt !== null) {
     *       // the same test, against the stack's data rather than the stack
     *       Chat.log(`the data matches: ${data.test(nbt)}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @since 1.9.1
     */
    public boolean test(NBTElementHelper<?> nbtElement) {
        return base.matches(nbtElement.getRaw());
    }

}
