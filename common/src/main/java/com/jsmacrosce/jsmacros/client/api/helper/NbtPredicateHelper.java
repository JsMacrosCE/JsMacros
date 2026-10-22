package com.jsmacrosce.jsmacros.client.api.helper;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import net.minecraft.advancements.critereon.NbtPredicate;

/**
 * Tests an entity, item stack, or NBT element against a partial NBT condition.
 * <p>
 * Matching compares the expected paths and values without requiring unmentioned data to match.
 * Entity and item-stack overloads apply their respective Minecraft data extraction rules; the
 * NBT-element overload tests the supplied tag directly. {@link BlockPredicateHelper#getNbtPredicate()}
 * returns {@code null} when the block predicate has no NBT condition.
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

    /** Wraps the supplied predicate without copying it; a {@code null} value can be retrieved with
     * {@link #getRaw()} but cannot be used by this helper's test methods.
     * @param base raw NBT predicate to wrap; may be {@code null} and is stored as-is
     */
    public NbtPredicateHelper(NbtPredicate base) {
        super(base);
    }

    /**
     * Tests the entity's serialized data against this condition, including its selected item for players.
     *
     * @param entity entity whose serialized data is tested
     * @return whether the entity data matches this condition
     * @since 1.9.1
     */
    public boolean test(EntityHelper<?> entity) {
        return base.matches(entity.getRaw());
    }

    /**
     * Tests the stack's {@code CUSTOM_DATA} component against this condition; other components are not
     * considered.
     *
     * @param itemStack stack whose {@code CUSTOM_DATA} component is tested
     * @return whether the component data matches this condition
     * @since 1.9.1
     */
    public boolean test(ItemStackHelper itemStack) {
        return base.matches(itemStack.getRaw());
    }

    /**
     * Tests the supplied NBT element directly, without extracting data from an entity or item stack.
     *
     * @param nbtElement NBT element to test
     * @return whether the element matches this condition
     * @since 1.9.1
     */
    public boolean test(NBTElementHelper<?> nbtElement) {
        return base.matches(nbtElement.getRaw());
    }

}
