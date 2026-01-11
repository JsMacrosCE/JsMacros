package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.Animal;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class AnimalEntityHelper<T extends Animal> extends MobEntityHelper<T> {

    public AnimalEntityHelper(T base) {
        super(base);
    }

    /**
     * @param item the item to check
     * @return {@code true} if the item can be used to feed and breed this animal, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isFood(ItemHelper item) {
        return base.isFood(item.getRaw().getDefaultInstance());
    }

    /**
     * @param item the item to check
     * @return {@code true} if the item can be used to feed and breed this animal, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isFood(ItemStackHelper item) {
        return base.isFood(item.getRaw());
    }

    /**
     * @param other the other animal to check
     * @return {@code true} if this animal can be bred with the other animal, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean canBreedWith(AnimalEntityHelper<?> other) {
        return base.canMate(other.getRaw());
    }

}
