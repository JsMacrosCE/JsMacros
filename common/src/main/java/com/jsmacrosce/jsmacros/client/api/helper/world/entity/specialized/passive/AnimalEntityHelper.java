package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.Animal;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * Provides food checks and breeding eligibility for animals. The {@link #isFood(ItemHelper)}
 * overload checks an item's default instance; {@link #isFood(ItemStackHelper)} checks the
 * supplied stack.
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
 * @param <T> the animal type represented by this helper
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class AnimalEntityHelper<T extends Animal> extends MobEntityHelper<T> {

    /**
     * Wraps the supplied animal entity and retains its reference for this helper.
     *
     * @param base animal entity to wrap
     */
    public AnimalEntityHelper(T base) {
        super(base);
    }

    /**
     * Checks whether the item's default instance is accepted as food; stack-specific data is not checked.
     *
     * @param item the item to check
     * @return whether the animal accepts the item as food
     * @since 1.8.4
     */
    public boolean isFood(ItemHelper item) {
        return base.isFood(item.getRaw().getDefaultInstance());
    }

    /**
     * Checks whether the supplied stack is accepted as food, including its stack-specific data.
     *
     * @param item the item to check
     * @return whether the animal accepts the stack as food
     * @since 1.8.4
     */
    public boolean isFood(ItemStackHelper item) {
        return base.isFood(item.getRaw());
    }

    /**
     * Checks this animal's type-specific mating rules against another animal. The base
     * check requires distinct animals of the same class, both in love; animal types
     * may override these conditions.
     *
     * @param other the other animal to check
     * @return whether this animal's mating check accepts the other animal
     * @since 1.8.4
     */
    public boolean canBreedWith(AnimalEntityHelper<?> other) {
        return base.canMate(other.getRaw());
    }

}
