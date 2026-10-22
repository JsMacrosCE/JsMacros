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
 * the shared half of every container that has an input, an output and a recipe book.
 * <p>
 * This class is abstract and a script never gets one of these directly. What it gets depends
 * on what is open: a crafting table, a furnace and the player's own inventory are all recipe
 * inventories, and each of them fills in the same handful of abstract methods to describe its
 * own grid. So the grid shape is read through {@link #getCraftingWidth()} and
 * {@link #getCraftingHeight()}, the individual cells through {@link #getInput(int, int)} or
 * the whole grid through {@link #getInput()}, and the thing being made through
 * {@link #getOutput()}.
 * <p>
 * How much those three answer depends on the container, and the differences are worth knowing
 * before a script reads them as one interface. A crafting table has a real 3 by 3 grid and
 * reports 3 for both measurements. The player's own inventory has a 2 by 2 grid but reports 0
 * for both, so {@link #getInputSize()} and {@link #getInput()} come back empty there rather
 * than describing the four cells. A furnace has a single input and reports 1 for both.
 * <p>
 * The other half is the recipe book, which {@link #isRecipeBookOpened()},
 * {@link #toggleRecipeBook()} and {@link #setRecipeBook(boolean)} drive, and
 * {@link #getRecipes(boolean)} reads.
 * example:
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
     * the item the container is working towards, filled in by whichever container this is.
     * <p>
     * There is no shared behaviour behind it: each subclass reads its own result slot, so what
     * this returns, and whether it is a fixed slot number or something read from the menu,
     * depends on the container. It is worth checking what arrived rather than assuming.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Crafting Table")) {
     *   if (!inv.getOutput().isEmpty()) {
     *     Chat.log(`crafting into ${inv.getOutput().getName()}`);
     *   }
     * }
     * </pre>
     *
     * @return the item in the container's result slot
     * @since 1.8.4
     */
    public abstract ItemStackHelper getOutput();

    /**
     * the size of the input grid, worked out as the height times the width.
     * <p>
     * It counts input cells only, so it leaves out the result slot and the player's inventory
     * around the container. Because it is the product of {@link #getCraftingWidth()} and
     * {@link #getCraftingHeight()}, it follows whichever of those a container reports rather
     * than its real grid: 9 for a crafting table, 1 for a furnace, and 0 for the player's own
     * inventory even though that one does have four crafting cells.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Crafting Table")) {
     *   Chat.log(`${inv.getInputSize()} grid cells`);
     * }
     * </pre>
     *
     * @return the width times the height, as reported by this container
     * @since 1.8.4
     */
    public int getInputSize() {
        return getCraftingHeight() * getCraftingWidth();
    }

    /**
     * one cell of the input grid, filled in by whichever container this is.
     * <p>
     * The two numbers are a position in the grid rather than a slot number: the first counts
     * from the left, the second from the top, both from 0. They have to be inside the grid this
     * container reports, so check {@link #getCraftingWidth()} and
     * {@link #getCraftingHeight()} before walking it, and note that the player's own inventory
     * reports 0 for both while still having cells to read.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Crafting Table")) {
     *   const topLeft = inv.getInput(0, 0);
     *   if (!topLeft.isEmpty()) {
     *     Chat.log(`top left cell holds ${topLeft.getItemId()}`);
     *   }
     * }
     * </pre>
     *
     * @param x the x position of the input slot, starting at 0, left to right. Must be less than
     *          {@link #getCraftingWidth()}
     * @param z the z position of the input slot, starting at 0, top to bottom. Must be less than
     *          {@link #getCraftingHeight()}
     * @return the input item at the given position.
     * @since 1.8.4
     */
    public abstract ItemStackHelper getInput(int x, int z);

    /**
     * the whole input grid as a two dimensional array, indexed by column and then by row.
     * <p>
     * The outer array has one entry per column and the inner one per row, and {@code get(x, z)}
     * is the same cell as {@code getInput(x, z)}. The array is sized from
     * {@link #getCraftingWidth()} and {@link #getCraftingHeight()} rather than from anything
     * on screen, so it comes back with no rows at all for a container that reports either as 0,
     * which includes the player's own inventory.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Crafting Table")) {
     *   const grid = inv.getInput();
     *   Chat.log(`${grid.length} columns of ${grid.length > 0 ? grid[0].length : 0} rows`);
     * }
     * </pre>
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
     * how many columns the grid has, filled in by whichever container this is.
     * <p>
     * Not every container measures its real grid here. A crafting table reports 3 and a furnace
     * reports 1, but the player's own inventory reports 0 despite having a 2 by 2 grid, so
     * this is not always a count of something a script can walk.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Crafting Table")) {
     *   Chat.log(`${inv.getCraftingWidth()} columns`);
     * }
     * </pre>
     *
     * @return the width this container reports
     * @since 1.8.4
     */
    public abstract int getCraftingWidth();

    /**
     * how many rows the grid has, filled in by whichever container this is.
     * <p>
     * The same caveat as the width applies: a crafting table reports 3 and a furnace 1, but
     * the player's own inventory reports 0 despite having a 2 by 2 grid. {@link #getInputSize()}
     * is the product of the two, so it is 0 for that container too.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Crafting Table")) {
     *   Chat.log(`${inv.getCraftingHeight()} rows`);
     * }
     * </pre>
     *
     * @return the height this container reports
     * @since 1.8.4
     */
    public abstract int getCraftingHeight();

    /**
     * how many slots the grid takes up, filled in by whichever container this is.
     * <p>
     * This is the container's own count of input slots rather than the product
     * {@link #getInputSize()} works out, so the two agree for a crafting table and a furnace and
     * both happen to be 0 for the player's own inventory. It excludes the result slot.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Crafting Table")) {
     *   Chat.log(`${inv.getCraftingSlotCount()} input slots`);
     * }
     * </pre>
     *
     * @return the number of input slots this container reports
     * @since 1.8.4
     */
    public abstract int getCraftingSlotCount();

    /**
     * which kind of recipe book this container uses, as a name rather than an id.
     * <p>
     * It is the name of the vanilla recipe book type, so one of {@code CRAFTING},
     * {@code FURNACE}, {@code BLAST_FURNACE} or {@code SMOKER}. A crafting table and the
     * player's own inventory both come back as {@code CRAFTING}, and it is the container rather
     * than its contents that decides.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Crafting Table")) {
     *   Chat.log(`recipe book type ${inv.getCategory()}`);
     * }
     * </pre>
     *
     * @return the recipe book type name for this container
     * @since 1.8.4
     */
    @DocletReplaceReturn("RecipeBookCategory")
    public String getCategory() {
        return handler.getRecipeBookType().name();
    }

    /**
     * the recipes this container's recipe book lists that the player could make right now.
     * <p>
     * This is {@link #getRecipes(boolean)} with {@code true}, so it reads the same list and
     * carries the same notes: the declared {@link java.lang.InterruptedException} is not raised anywhere
     * on this path, and what comes back is a list that may simply be empty.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Crafting Table")) {
     *   const recipes = inv.getCraftableRecipes();
     *   Chat.log(`${recipes.size()} recipes available`);
     *   for (let i = 0; recipes.size() > i; i++) {
     *     Chat.log(`  ${recipes.get(i).getOutput().getItemId()}`);
     *   }
     * }
     * </pre>
     *
     * @return the recipes this container can make with the player holding the ingredients
     * @throws InterruptedException declared but never raised by this implementation
     * @since 1.3.1
     */
    public List<RecipeHelper> getCraftableRecipes() throws InterruptedException {
        return getRecipes(true);
    }

    /**
     * the recipes this container's recipe book lists, optionally filtered to what the player
     * can make at the moment.
     * <p>
     * The list comes from the recipe book rather than from the grid: it walks the tabs the
     * screen's recipe book offers and collects the entries in each. What
     * {@code craftable} changes is the filter, and the filter is built from the player's own
     * inventory and from the container's crafting slots, so a recipe with no ingredients in
     * reach is left out when it is {@code true} and included when it is {@code false}.
     * <p>
     * Two things about the signature are worth knowing. It is declared
     * {@link java.lang.InterruptedException} and returns {@code null}, but neither happens here: no
     * statement in the body can throw, and the only exit is the one that builds the list, so a
     * call either gives a list, possibly empty, or has already failed.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Crafting Table")) {
     *   const all = inv.getRecipes(false);
     *   Chat.log(`${all?.size()} recipes listed, ${inv.getCraftableRecipes().size()} craftable`);
     * }
     * </pre>
     *
     * @param craftable whether to leave out the recipes the player cannot make right now
     * @return the recipes this container's recipe book lists
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
     * whether the recipe book overlay is showing on this container's screen.
     * <p>
     * It reads the screen's own recipe book widget, so it is the overlay's visibility rather
     * than whether a recipe book exists: a container without the widget at all reads back
     * {@code false}. It is safe to call whatever is open, and it does nothing.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Crafting Table")) {
     *   if (inv.isRecipeBookOpened()) {
     *     Chat.log("the recipe book is open");
     *   }
     * }
     * </pre>
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
     * flips the recipe book overlay open or closed, whichever it currently is.
     * <p>
     * This only acts when this container's screen is the one currently open, so a handle kept
     * from an earlier screen does nothing rather than reaching across to whatever is open now.
     * Either way it changes nothing that lasts: it sends no click to the server, and the
     * overlay's visibility is a display choice rather than a saved setting, so it should be
     * set again when the screen is opened.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Crafting Table")) {
     *   inv.toggleRecipeBook();
     *   Chat.log(`recipe book now ${inv.isRecipeBookOpened() ? "open" : "closed"}`);
     * }
     * </pre>
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
     * opens the recipe book overlay, or closes it, and leaves it there only if it moved.
     * <p>
     * This is the explicit form of {@link #toggleRecipeBook()}: it checks the current
     * visibility first and does nothing when it already matches what was asked for, so calling
     * it twice with the same value is not a way to end up back where it started. It also only
     * acts while this container's screen is the one open, and it sends nothing to the server.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Crafting Table")) {
     *   inv.setRecipeBook(true);
     *   Chat.log(`recipe book forced ${inv.isRecipeBookOpened() ? "open" : "closed"}`);
     * }
     * </pre>
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
