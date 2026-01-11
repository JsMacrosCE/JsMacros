package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.classes.render.IScreen;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.RecipeHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public abstract class RecipeInventory<T extends AbstractRecipeBookScreen<? extends RecipeBookMenu>> extends Inventory<T> {

    private final RecipeBookMenu handler;

    protected RecipeInventory(T inventory) {
        super(inventory);
        this.handler = inventory.getMenu();
    }

    /**
     * @return the output item.
     * @since 1.8.4
     */
    public abstract ItemStackHelper getOutput();

    /**
     * @return the maximum input size for all recipes in this inventory.
     * @since 1.8.4
     */
    public int getInputSize() {
        return getCraftingHeight() * getCraftingWidth();
    }

    /**
     * @param x the x position of the input slot, starting at 0, left to right. Must be less than
     *          {@link #getCraftingWidth()}
     * @param z the z position of the input slot, starting at 0, top to bottom. Must be less than
     *          {@link #getCraftingHeight()}
     * @return the input item at the given position.
     * @since 1.8.4
     */
    public abstract ItemStackHelper getInput(int x, int z);

    /**
     * @return the input items of the crafting grid, in a 2d array.
     * @since 1.8.4
     */
    public ItemStackHelper[][] getInput() {
        ItemStackHelper[][] input = new ItemStackHelper[getCraftingWidth()][getCraftingHeight()];
        for (int x = 0; x < getCraftingWidth(); x++) {
            for (int z = 0; z < getCraftingHeight(); z++) {
                input[x][z] = getInput(x, z);
            }
        }
        return input;
    }

    /**
     * @return the width of the crafting grid.
     * @since 1.8.4
     */
    public abstract int getCraftingWidth();

    /**
     * @return the height of the crafting grid.
     * @since 1.8.4
     */
    public abstract int getCraftingHeight();

    /**
     * @return the amount of slots used for crafting.
     * @since 1.8.4
     */
    public abstract int getCraftingSlotCount();

    /**
     * @return the recipe category of recipes that can be crafted in this inventory.
     * @since 1.8.4
     */
    @DocletReplaceReturn("RecipeBookCategory")
    public String getCategory() {
        return handler.getRecipeBookType().name();
    }

    /**
     * @return
     * @throws InterruptedException
     * @since 1.3.1
     */
    public List<RecipeHelper> getCraftableRecipes() throws InterruptedException {
        return getRecipes(true);
    }

    /**
     * @param craftable whether only to list craftable recipes
     * @return a list of recipes that can be crafted in this inventory.
     * @throws InterruptedException
     * @since 1.8.4
     */
    @Nullable
    public List<RecipeHelper> getRecipes(boolean craftable) throws InterruptedException {
        StackedItemContents recipeFinder = new StackedItemContents();

        if (craftable) {
            mc.player.getInventory().fillStackedContents(recipeFinder);
            handler.fillCraftSlotsStackedContents(recipeFinder);
        }

        List<RecipeDisplayEntry> recipeIds = new ArrayList<>();
        ClientRecipeBook recipeBook = mc.player.getRecipeBook();
        for (RecipeBookComponent.TabInfo t : inventory.recipeBookComponent.tabInfos) {
            for (RecipeCollection res : recipeBook.getCollection(t.category())) {
                for (RecipeDisplayEntry displayEntry : res.getRecipes()) {
                    if (!craftable || displayEntry.canCraft(recipeFinder)) {
                        recipeIds.add(displayEntry);
                    }
                }
            }
        }

        return recipeIds.stream().map(e -> new RecipeHelper(e, syncId)).collect(Collectors.toList());
    }

    @Nullable
    private RecipeBookComponent<?> getRecipeBookWidget() {
        return inventory.recipeBookComponent;
    }

    /**
     * @return {@code true} if the recipe book is visible, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isRecipeBookOpened() {
        RecipeBookComponent<?> recipeBookWidget = getRecipeBookWidget();
        if (recipeBookWidget == null) {
            return false;
        }
        return recipeBookWidget.isVisible();
    }

    /**
     * @since 1.8.4
     */
    public void toggleRecipeBook() {
        if (mc.screen != inventory) {
            return;
        }
        RecipeBookComponent<?> recipeBookWidget = getRecipeBookWidget();
        if (recipeBookWidget == null) {
            return;
        }
        recipeBookWidget.toggleVisibility();
        ((IScreen) inventory).reloadScreen();
    }

    /**
     * @param open whether to open or close the recipe book
     * @since 1.8.4
     */
    public void setRecipeBook(boolean open) {
        if (mc.screen != inventory) {
            return;
        }
        RecipeBookComponent<?> recipeBookWidget = getRecipeBookWidget();
        if (recipeBookWidget != null) {
            if (recipeBookWidget.isVisible() != open) {
                recipeBookWidget.toggleVisibility();
                ((IScreen) inventory).reloadScreen();
            }
        }
    }

}
