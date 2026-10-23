package com.jsmacrosce.jsmacros.client.api.helper.inventory;

import com.jsmacrosce.jsmacros.client.util.GuiCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Describes a recipe's inputs and output, and checks or requests crafting for the current player.
 * Recipe helpers are returned by recipe inventories such as {@code RecipeInventory.getRecipes()}.
 * <pre>
 * function affordable(recipes) {
 *   for (let i = 0; i !== recipes.size(); i += 1) {
 *     const recipe = recipes.get(i);
 *     const times = recipe.getCraftableAmount();
 *     if (times > 0) {
 *       Chat.log(`${recipe.getOutput().getItemId()} can be made ${times} times`);
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.3.1
 */
@DocletCategory("Items/Enchantments")
@SuppressWarnings("unused")
public class RecipeHelper extends BaseHelper<RecipeDisplayEntry> {
    private static final Minecraft mc = Minecraft.getInstance();
    protected int syncId;

    /**
     * Wraps a recipe display entry and records the menu synchronization ID used by craft requests.
     *
     * @param base the raw recipe display entry to wrap
     * @param syncId the menu synchronization ID used by craft requests
     */
    public RecipeHelper(RecipeDisplayEntry base, int syncId) {
        super(base);
        this.syncId = syncId;
    }

//    /**
//     * @return
//     * @since 1.3.1
//     */
//    @DocletReplaceReturn("RecipeId")
//    public String getId() {
//        return base.
//    }

    /**
     * Each outer-list entry is a recipe slot; its inner list contains the alternative item stacks
     * accepted for that slot. A recipe with no declared ingredients returns an empty list.
     *
     * @return the ingredient slots and their accepted item stacks; empty when none are declared.
     * @since 1.8.3
     */
    public List<List<ItemStackHelper>> getIngredients() {
        List<List<ItemStackHelper>> ingredients = new ArrayList<>();

        for (Ingredient in : base.craftingRequirements().orElseGet(List::of)) {
            ingredients.add(in.items().map(ItemStack::new).map(ItemStackHelper::new).collect(Collectors.toList()));
        }

        return ingredients;
    }

    /**
     * Returns the first result as a stack, resolving display data using the loaded world. Its count
     * is the output count for one craft.
     *
     * @return the first result stack.
     * @since 1.3.1
     */
    public ItemStackHelper getOutput() {
        assert mc.level != null;
        return new ItemStackHelper(base.resultItems(SlotDisplayContext.fromLevel(mc.level)).getFirst());
    }

    /**
     * Requests crafting through the matching open container screen, or the player inventory when
     * no screen is open. Returns this helper. If its originating menu is not open, throws an
     * {@code AssertionError}.
     *
     * @param craftAll whether to craft as many times as the ingredients allow rather than once.
     * @return this recipe, for chaining.
     * @since 1.3.1
     */
    public RecipeHelper craft(boolean craftAll) {
        Minecraft mc = Minecraft.getInstance();
        assert mc.player != null;
        if ((GuiCompat.screen(mc) instanceof AbstractContainerScreen && ((AbstractContainerScreen<?>) GuiCompat.screen(mc)).getMenu().containerId == syncId) ||
                (GuiCompat.screen(mc) == null && syncId == mc.player.inventoryMenu.containerId)) {
            assert mc.gameMode != null;
            mc.gameMode.handlePlaceRecipe(syncId, base.id(), craftAll);
            return this;
        }
        throw new AssertionError("Crafting Screen no longer open!");
    }

    /**
     * Returns this recipe's recipe-book category identifier.
     *
     * @return the category identifier.
     * @since 1.8.4
     */
    public String getGroup() {
        return BuiltInRegistries.RECIPE_BOOK_CATEGORY.getKey(base.category()).toString();
    }

//    /**
//     * This will not account for the actual items used in the recipe, but only the default recipe
//     * itself. Items with durability or with a lot of tags will probably not work correctly.
//     *
//     * @return will return {@code true} if any of the default ingredients have a recipe remainder.
//     * @since 1.8.4
//     */
//    public boolean hasRecipeRemainders() {
//        base.isCraftable()
//        return base.value().getIngredients().stream().anyMatch(ingredient -> ingredient.getMatchingStacks()[0].getItem().hasRecipeRemainder());
//    }
//
//    /**
//     * @return a list of all possible recipe remainders.
//     * @since 1.8.4
//     */
//    public List<List<ItemStackHelper>> getRecipeRemainders() {
//        return base.value().getIngredients().stream()
//                .filter(ingredient -> ingredient.getMatchingStacks().length > 0 && ingredient.getMatchingStacks()[0].getItem().hasRecipeRemainder())
//                .map(ingredient -> Arrays.stream(ingredient.getMatchingStacks()).map(ItemStackHelper::new).collect(Collectors.toList()))
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * @return the type of this recipe.
//     * @since 1.8.4
//     */
//    @DocletReplaceReturn("RecipeTypeId")
//    public String getType() {
//        return Registries.RECIPE_TYPE.getId(base.value().getType()).toString();
//    }

    /**
     * Tests whether one craft is possible using the player's inventory, plus the crafting slots
     * when a recipe-book screen is open.
     *
     * @return {@code true} if one craft is possible, {@code false} otherwise.
     * @since 1.8.4
     */

    public boolean canCraft() {
        StackedItemContents recipeFinder = new StackedItemContents();
        mc.player.getInventory().fillStackedContents(recipeFinder);
        if (GuiCompat.screen(mc) instanceof AbstractRecipeBookScreen<?> screen) {
            screen.getMenu().fillCraftSlotsStackedContents(recipeFinder);
        }
        return base.canCraft(recipeFinder);
    }

    /**
     * Tests whether {@link #getCraftableAmount()} is at least {@code amount}; zero and negative
     * amounts therefore return {@code true}.
     *
     * @param amount the minimum number of crafts required
     * @return {@code true} when the computed craftable amount is at least {@code amount}.
     * @since 1.8.4
     */
    public boolean canCraft(int amount) {
        return getCraftableAmount() >= amount;
    }

    /**
     * Counts how many times the recipe's requirements can be met from the player's inventory,
     * including crafting slots when a recipe-book screen is open. This calculation does not craft
     * the recipe.
     *
     * @return the number of crafts allowed by the available ingredients.
     * @since 1.8.4
     */
    public int getCraftableAmount() {
        StackedItemContents recipeFinder = new StackedItemContents();
        mc.player.getInventory().fillStackedContents(recipeFinder);
        if (GuiCompat.screen(mc) instanceof AbstractRecipeBookScreen<?> screen) {
            screen.getMenu().fillCraftSlotsStackedContents(recipeFinder);
        }
        return recipeFinder.raw.tryPickAll(base.craftingRequirements().get(), Integer.MAX_VALUE, null);
    }

    /**
     * Returns a diagnostic representation containing the display entry's identifier.
     *
     * @return a diagnostic representation of the recipe display entry.
     */
    @Override
    public String toString() {
        return String.format("RecipeHelper:{\"id\": \"%s\"}", base.id().toString());
    }

}
