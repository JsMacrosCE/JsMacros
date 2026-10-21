package com.jsmacrosce.jsmacros.client.api.helper.inventory;

import net.minecraft.world.food.FoodProperties;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * Exposes an item's nutrition, saturation, and always-edible flag.
 * <pre>
 * const apple = Client.getRegistryManager().getItem("minecraft:golden_apple");
 * const food = apple.getFood();
 * if (food !== null) {
 *   Chat.log(`${food.getHunger()} hunger points, ${food.getSaturation()} saturation`);
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Items/Enchantments")
@SuppressWarnings("unused")
public class FoodComponentHelper extends BaseHelper<FoodProperties> {

    /** Wraps the supplied food properties for access through this helper.
     * @param base raw food properties to wrap
     */
    public FoodComponentHelper(FoodProperties base) {
        super(base);
    }

    /**
     * Returns this food's nutrition value in hunger points (two points per bar segment).
     *
     * @return the amount of hunger this food restores.
     * @since 1.8.4
     */
    public int getHunger() {
        return base.nutrition();
    }

    /**
     * Returns the saturation value stored for this food.
     *
     * @return the amount of saturation this food restores.
     * @since 1.8.4
     */
    public float getSaturation() {
        return base.saturation();
    }

    /**
     * Returns whether this food's always-edible flag is set.
     *
     * @return {@code true} if the always-edible flag is set; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAlwaysEdible() {
        return base.canAlwaysEat();
    }

    /** Returns a diagnostic representation of this food's nutrition, saturation, and always-edible flag.
     * @return diagnostic representation of the food properties
     */
    @Override
    public String toString() {
        return String.format("FoodComponentHelper:{\"hunger\": %d, \"saturation\": %f, \"alwaysEdible\": %b}", getHunger(), getSaturation(), isAlwaysEdible());
    }

}
