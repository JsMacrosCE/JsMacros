package com.jsmacrosce.jsmacros.client.api.helper.inventory;

import net.minecraft.world.food.FoodProperties;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * What eating an item does: how much it restores and whether it can be eaten at all when the player
 * is not hungry.<br>
 * A script gets one from {@link ItemHelper#getFood()}, which answers {@code null} for an item that
 * is not food, so this is the food data for an item rather than the default data for a block. It is
 * three numbers and a flag, and all of them come off the item's own food component rather than off
 * anything in the world.
 * example:
 * <pre>
 * // what a food item does, for one item
 * const apple = Client.getRegistryManager().getItem("minecraft:golden_apple");
 * const food = apple.getFood();
 * if (food !== null) {
 *   Chat.log(`${apple.getId()}: hunger ${food.getHunger()}, saturation ${food.getSaturation()}`);
 *   Chat.log(`edible when not hungry: ${food.isAlwaysEdible()}`);
 * }
 *
 * // and across a set of items
 * for (const id of ["minecraft:bread", "minecraft:cooked_beef", "minecraft:golden_apple", "minecraft:stone"]) {
 *   const item = Client.getRegistryManager().getItem(id);
 *   const food = item.getFood();
 *   if (food === null) {
 *     Chat.log(`${id} is not food`);
 *   } else {
 *     Chat.log(`${id}: ${food.getHunger()} hunger, ${food.getSaturation()} saturation`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Items/Enchantments")
@SuppressWarnings("unused")
public class FoodComponentHelper extends BaseHelper<FoodProperties> {

    public FoodComponentHelper(FoodProperties base) {
        super(base);
    }

    /**
     * How much hunger this food restores, on the game's scale of half a bar per point and twenty
     * for a full bar. A cooked steak gives more than raw beef and a golden apple gives more than
     * either.
     * example:
     * <pre>
     * // the two hunger numbers for an item
     * const item = Client.getRegistryManager().getItem("minecraft:cooked_beef");
     * const food = item.getFood();
     * if (food !== null) {
     *   Chat.log(`${food.getHunger()} hunger is ${food.getHunger() / 2} bars`);
     * }
     *
     * // the best food in the registry, by this number
     * const reg = Client.getRegistryManager();
     * const items = reg.getItems();
     * let bestId = "";
     * let best = -1;
     * for (let i = 0; i !== items.size(); i += 1) {
     *   const food = items.get(i).getFood();
     *   if (food !== null) {
     *     if (food.getHunger() > best) {
     *       best = food.getHunger();
     *       bestId = items.get(i).getId();
     *     }
     *   }
     * }
     * Chat.log(`${bestId} restores the most at ${best}`);
     * </pre>
     *
     * @return the amount of hunger this food restores.
     * @since 1.8.4
     */
    public int getHunger() {
        return base.nutrition();
    }

    /**
     * The saturation this food leaves behind, as the game's own number rather than as a bar. It is
     * what keeps the hunger bar from emptying again straight away, and the game works it out from
     * the food's own data rather than from a separate setting.
     * example:
     * <pre>
     * // hunger against saturation, for a few foods
     * for (const id of ["minecraft:bread", "minecraft:cooked_beef", "minecraft:golden_apple"]) {
     *   const food = Client.getRegistryManager().getItem(id).getFood();
     *   if (food !== null) {
     *     Chat.log(`${id}: ${food.getHunger()} hunger, ${food.getSaturation()} saturation`);
     *   }
     * }
     * </pre>
     *
     * @return the amount of saturation this food restores.
     * @since 1.8.4
     */
    public float getSaturation() {
        return base.saturation();
    }

    /**
     * Whether this food can be eaten at all when the hunger bar is still full. The flag is set on
     * the food itself, and it is what lets a player eat a golden carrot on a full bar, which is
     * something effects are built on.
     * example:
     * <pre>
     * // which foods are worth eating on a full bar
     * for (const id of ["minecraft:bread", "minecraft:golden_carrot", "minecraft:rotten_flesh"]) {
     *   const food = Client.getRegistryManager().getItem(id).getFood();
     *   if (food !== null) {
     *     Chat.log(`${id}: always edible ${food.isAlwaysEdible()}`);
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this food can be eaten even when the player is not hungry,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAlwaysEdible() {
        return base.canAlwaysEat();
    }

    @Override
    public String toString() {
        return String.format("FoodComponentHelper:{\"hunger\": %d, \"saturation\": %f, \"alwaysEdible\": %b}", getHunger(), getSaturation(), isAlwaysEdible());
    }

}
