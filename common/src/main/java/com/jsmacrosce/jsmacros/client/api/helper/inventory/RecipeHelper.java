package com.jsmacrosce.jsmacros.client.api.helper.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Wagyourtail
 * @since 1.3.1
 */
@SuppressWarnings("unused")
public class RecipeHelper extends BaseHelper<RecipeDisplayEntry> {
    private static final Minecraft mc = Minecraft.getInstance();
    protected int syncId;

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
     * get ingredients list
     *
     * @return
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
     * @return
     * @since 1.3.1
     */
    public ItemStackHelper getOutput() {
        assert mc.level != null;
        return new ItemStackHelper(base.resultItems(SlotDisplayContext.fromLevel(mc.level)).getFirst());
    }

    /**
     * @param craftAll
     * @since 1.3.1
     */
    public RecipeHelper craft(boolean craftAll) {
        Minecraft mc = Minecraft.getInstance();
        assert mc.player != null;
        if ((mc.screen instanceof AbstractContainerScreen && ((AbstractContainerScreen<?>) mc.screen).getMenu().containerId == syncId) ||
                (mc.screen == null && syncId == mc.player.inventoryMenu.containerId)) {
            assert mc.gameMode != null;
            mc.gameMode.handlePlaceRecipe(syncId, base.id(), craftAll);
            return this;
        }
        throw new AssertionError("Crafting Screen no longer open!");
    }

    /**
     * @return the type of this recipe.
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

//    /**
//     * @return the type of this recipe.
//     * @since 1.8.4
//     */
//    @DocletReplaceReturn("RecipeTypeId")
//    public String getType() {
//        return Registries.RECIPE_TYPE.getId(base.value().getType()).toString();
//    }

    /**
     * @return {@code true} if the recipe can be crafted with the current inventory, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean canCraft() {
        StackedItemContents recipeFinder = new StackedItemContents();
        mc.player.getInventory().fillStackedContents(recipeFinder);
        if (mc.screen instanceof AbstractRecipeBookScreen<?> screen) {
            screen.getMenu().fillCraftSlotsStackedContents(recipeFinder);
        }
        return base.canCraft(recipeFinder);
    }

    /**
     * @param amount the amount of items to craft
     * @return {@code true} if the given amount of items can be crafted with the current inventory,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canCraft(int amount) {
        return getCraftableAmount() >= amount;
    }

    /**
     * @return how often the recipe can be crafted with the current player inventory.
     * @since 1.8.4
     */
    public int getCraftableAmount() {
        StackedItemContents recipeFinder = new StackedItemContents();
        mc.player.getInventory().fillStackedContents(recipeFinder);
        if (mc.screen instanceof AbstractRecipeBookScreen<?> screen) {
            screen.getMenu().fillCraftSlotsStackedContents(recipeFinder);
        }
        return recipeFinder.raw.tryPickAll(base.craftingRequirements().get(), Integer.MAX_VALUE, null);
    }

    @Override
    public String toString() {
        return String.format("RecipeHelper:{\"id\": \"%s\"}", base.id().toString());
    }

}
