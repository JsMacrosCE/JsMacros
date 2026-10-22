package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.Animal;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * the base of most of the passive mobs in this package, and the two questions a script has
 * about any of them: can this item be used on it, and will it breed with that other one.
 * <p>
 * Both of those are the game's own answers rather than anything decided here. What the item
 * counts as food is a tag on the item - wheat for a cow, cod for a cat, a bone or wolf food
 * for a wolf - and what counts as a mate is decided by the mob, which on several of them
 * asks for more than being in love. A foal is not a match for its parents because it is the
 * same kind of animal, and a parrot is never a match for anything.
 * <p>
 * A note on {@link #isFood(ItemHelper) isFood} against {@link #isFood(ItemStackHelper)
 * isFood}: they differ only in what they accept, not in what they answer. The first throws
 * the stack away and asks about a single default item, so a stack of two apples and a stack
 * of one answer the same; the second asks about the stack exactly as given, so it can
 * distinguish a stack that has been damaged or renamed. Neither one cares how many there
 * are.
 * example:
 * <pre>
 * const AnimalEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.AnimalEntityHelper");
 * const ItemStackHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper");
 * const carrots = new ItemStackHelper("carrot", 1);
 * const rabbits = World.getEntities(32, "rabbit");
 * if (rabbits !== null) {
 *   for (const entity of rabbits) {
 *     const rabbit = AnimalEntityHelper.class.cast(entity);
 *     Chat.log(`${rabbit.getType()} takes carrots: ${rabbit.isFood(carrots)}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class AnimalEntityHelper<T extends Animal> extends MobEntityHelper<T> {

    public AnimalEntityHelper(T base) {
        super(base);
    }

    /**
     * Whether this item is one of the things that feeds and breeds this kind of animal.
     * <p>
     * The item is asked about as a single default stack, so the count, any damage on the
     * stack and its name make no difference to the answer. Use the {@link
     * ItemStackHelper} overload when the stack itself is what matters.
     * example:
     * <pre>
     * const AnimalEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.AnimalEntityHelper");
     * const cows = World.getEntities(32, "cow");
     * if (cows !== null) {
     *   for (const entity of cows) {
     *     const cow = AnimalEntityHelper.class.cast(entity);
     *     if (cow.isBaby()) {
     *       Chat.log("a calf, so feeding it will also set it in love");
     *     }
     *   }
     * }
     * </pre>
     *
     * @param item the item to check
     * @return {@code true} if the item can be used to feed and breed this animal, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isFood(ItemHelper item) {
        return base.isFood(item.getRaw().getDefaultInstance());
    }

    /**
     * Whether this exact stack is one of the things that feeds and breeds this kind of
     * animal. The game asks about the stack rather than the item, so a stack that has been
     * renamed can answer differently from the same item unrenamed - which is how a named
     * carrot ends up not breeding rabbits.
     * example:
     * <pre>
     * const AnimalEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.AnimalEntityHelper");
     * const ItemStackHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper");
     * const carrots = new ItemStackHelper("carrot", 1);
     * const rabbits = World.getEntities(32, "rabbit");
     * if (rabbits !== null) {
     *   for (const entity of rabbits) {
     *     const rabbit = AnimalEntityHelper.class.cast(entity);
     *     if (rabbit.isFood(carrots)) {
     *       Chat.log("a rabbit will take that carrot");
     *     }
     *   }
     * }
     * </pre>
     *
     * @param item the item to check
     * @return {@code true} if the item can be used to feed and breed this animal, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isFood(ItemStackHelper item) {
        return base.isFood(item.getRaw());
    }

    /**
     * Whether the game would let these two breed, which is its answer rather than anything
     * decided here.
     * <p>
     * For a plain animal it wants both of them in love and both of them the very same class
     * of animal, and the pair has to be different individuals. Plenty of the mobs here ask
     * for more than that on top: a horse or a llama wants both parents tamed, grown up,
     * healthy and not riding or ridden; a wolf wants both parents tamed and neither sitting;
     * and a parrot, or a horse of the base kind, will not mate at all.
     * example:
     * <pre>
     * const AnimalEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.AnimalEntityHelper");
     * const sheep = World.getEntities(32, "sheep");
     * if (sheep !== null) {
     *   for (const entity of sheep) {
     *     const ewe = AnimalEntityHelper.class.cast(entity);
     *     for (const other of sheep) {
     *       const ram = AnimalEntityHelper.class.cast(other);
     *       if (ewe.isBaby()) {
     *         continue;
     *       }
     *       if (ewe.canBreedWith(ram)) {
     *         Chat.log("these two are a pair");
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @param other the other animal to check
     * @return {@code true} if this animal can be bred with the other animal, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean canBreedWith(AnimalEntityHelper<?> other) {
        return base.canMate(other.getRaw());
    }

}
