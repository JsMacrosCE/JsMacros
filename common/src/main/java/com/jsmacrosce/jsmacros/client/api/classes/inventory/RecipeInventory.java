package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.classes.render.IScreen;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.RecipeHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base API for recipe-book screens, exposing their input dimensions, cells, output, and recipe-book entries.
 * <p>
 * Reported dimensions vary by container. The player's inventory screen reports zero width and
 * height despite having a 2-by-2 crafting grid; consequently {@link #getInput()} returns an
 * empty array there. Read those cells individually with {@link #getInput(int, int)}. Other
 * recipe-book screens provide their own grid dimensions and cell implementations.
 * <p>
 * The recipe-book visibility and entries are accessed through {@link #isRecipeBookOpened()},
 * {@link #toggleRecipeBook()}, {@link #setRecipeBook(boolean)}, and {@link #getRecipes(boolean)}.
 * <pre>
 * const inv = Player.openInventory();
 * if (inv.is("Crafting Table")) {
 *   const grid = inv.getInput();
 *   for (let x = 0; inv.getCraftingWidth() > x; x++) {
 *     for (let z = 0; inv.getCraftingHeight() > z; z++) {
 *       if (!grid[x][z].isEmpty()) {
 *         Chat.log(`cell ${x},${z} holds ${grid[x][z].getItemId()}`);
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * @param <T> the recipe-book screen type wrapped by this inventory
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Inventory")
@SuppressWarnings("unused")
public abstract class RecipeInventory<T extends AbstractRecipeBookScreen<? extends RecipeBookMenu>> extends Inventory<T> {

    private final RecipeBookMenu handler;

    protected RecipeInventory(T inventory) {
        super(inventory);
        this.handler = inventory.getMenu();
    }

    /**
     * Returns the item in this container's result slot.
     *
     * @return the item in the container's result slot
     * @since 1.8.4
     */
    public abstract ItemStackHelper getOutput();

    /**
     * Returns the product of the container-reported input width and height.
     *
     * @return the product of {@link #getCraftingWidth()} and {@link #getCraftingHeight()}
     * @since 1.8.4
     */
    public int getInputSize() {
        return getCraftingHeight() * getCraftingWidth();
    }

    /**
     * Returns an input cell. Coordinates are zero-based, with {@code x} increasing left to right
     * and {@code z} top to bottom. The player's inventory screen accepts its 2-by-2 cell
     * coordinates even though its reported dimensions are zero.
     *
     * @param x zero-based column, increasing left to right
     * @param z zero-based row, increasing top to bottom
     * @return the item in the selected input cell
     * @since 1.8.4
     */
    public abstract ItemStackHelper getInput(int x, int z);

    /**
     * Returns input cells in an array indexed by column, then row. Its dimensions are the
     * reported width and height, so it is empty when either dimension is zero.
     *
     * @return the grid as {@code [column][row]}
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
     * Returns the input-grid width reported by this container. The player's inventory screen
     * reports zero despite having a 2-by-2 crafting grid.
     *
     * @return the reported input-grid width
     * @since 1.8.4
     */
    public abstract int getCraftingWidth();

    /**
     * Returns the input-grid height reported by this container. The player's inventory screen
     * reports zero despite having a 2-by-2 crafting grid.
     *
     * @return the reported input-grid height
     * @since 1.8.4
     */
    public abstract int getCraftingHeight();

    /**
     * Returns the number of input slots reported by this container, which need not match the
     * product of the reported grid dimensions.
     *
     * @return the reported input-slot count
     * @since 1.8.4
     */
    public abstract int getCraftingSlotCount();

    /**
     * Returns the name of the recipe-book category associated with this container.
     *
     * @return the category name
     * @since 1.8.4
     */
    @DocletReplaceReturn("RecipeBookCategory")
    public String getCategory() {
        return handler.getRecipeBookType().name();
    }

    /**
     * Returns recipes reported craftable using the player's inventory and this container's input
     * slots. The list may be empty.
     *
     * @return recipes reported craftable from the available items; may be empty
     * @since 1.3.1
     */
    public List<RecipeHelper> getCraftableRecipes() throws InterruptedException {
        return getRecipes(true);
    }

    /**
     * Returns entries in this screen's recipe-book tabs, optionally limited to entries craftable
     * from the player's inventory and this container's crafting slots.
     *
     * @param craftable whether to include only entries reported craftable from the player's
     *                  inventory and this container's crafting slots
     * @return entries in the screen's recipe-book tabs, filtered by craftability when requested;
     *         the list is non-null and may be empty
     * @throws InterruptedException declared but never raised by this implementation
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
     * Returns whether this container's recipe-book widget is visible; if it has no widget,
     * returns {@code false}.
     *
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
     * Toggles this container's recipe-book widget when its screen is currently open. Does
     * nothing if the screen is not open or has no recipe-book widget.
     *
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
     * Sets this container's recipe-book widget visibility when its screen is currently open.
     * Does nothing if the screen is not open, has no widget, or already has the requested state.
     *
     * @param open whether the recipe book should be showing when this returns
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
