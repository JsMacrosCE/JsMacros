package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * the handle for an open stonecutter screen.
 * <p>
 * A stonecutter is one input and one result, plus a list of every recipe the current input can
 * be cut into. The input is slot 0 and the result is slot 1, which is the order the
 * stonecutter's menu builds them and the same as its own {@code INPUT_SLOT} and
 * {@code RESULT_SLOT} constants. There is no method for the input slot here, so it is read as
 * {@code getSlot(0)}, while the result has {@link #getOutput()}.
 * <p>
 * The recipe list is the rest of the class, and it only exists once an input is in the slot.
 * {@link #getAvailableRecipeCount()} is how many there are, {@link #getRecipes()} names what
 * they come out as, and {@link #selectRecipe(int)} picks one by the same number.
 * {@link #getSelectedRecipeIndex()} reports the pick, and {@link #canCraft()} is a quick way to
 * ask whether there is anything to cut at all.
 * example:
 * <pre>
 * const inv = Player.openInventory();
 * if (inv.is("Stonecutter")) {
 *   if (inv.canCraft()) {
 *     const count = inv.getAvailableRecipeCount();
 *     Chat.log(`${count} cuts from ${inv.getSlot(0).getItemId()}`);
 *     inv.selectRecipe(0);
 *     Chat.log(`now making ${inv.getOutput().getItemId()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Inventory")
@SuppressWarnings("unused")
public class StoneCutterInventory extends Inventory<StonecutterScreen> {

    public StoneCutterInventory(StonecutterScreen inventory) {
        super(inventory);
    }

    /**
     * which recipe is currently picked, or -1 when none is.
     * <p>
     * The index is into the list {@link #getRecipes()} returns, so the same number selects a
     * recipe with {@link #selectRecipe(int)}. It is -1 rather than 0 for "nothing picked",
     * and the stonecutter puts it back to -1 every time the input slot changes, so a fresh
     * input always reads -1 until something is selected.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Stonecutter")) {
     *   const picked = inv.getSelectedRecipeIndex();
     *   Chat.log(0 > picked ? "nothing selected" : `recipe ${picked} of ${inv.getAvailableRecipeCount()}`);
     * }
     * </pre>
     *
     * @return the index of the selected recipe, -1 when none is selected
     * @since 1.8.4
     */
    public int getSelectedRecipeIndex() {
        return inventory.getMenu().getSelectedRecipeIndex();
    }

    /**
     * the item the selected recipe comes out as, which is slot 1.
     * <p>
     * This is the result slot as the stonecutter currently has it, so it follows the pick
     * {@link #getSelectedRecipeIndex()} reports and is empty when nothing is selected. A
     * recipe can be selected and still leave this empty, so {@link #canCraft()} is the better
     * question to ask when a script only wants to know whether cutting is possible.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Stonecutter")) {
     *   const out = inv.getOutput();
     *   if (!out.isEmpty()) {
     *     Chat.log(`cutting into ${out.getItemId()}`);
     *   }
     * }
     * </pre>
     *
     * @return the result slot's item
     * @since 1.8.4
     */
    public ItemStackHelper getOutput() {
        return getSlot(1);
    }

    /**
     * picks one of the visible recipes by index, counting the same way
     * {@link #getAvailableRecipeCount()} counts them.
     * <p>
     * It returns itself whatever happens, so it is not a way to find out whether the pick was
     * accepted: an index outside the range 0 to one less than
     * {@link #getAvailableRecipeCount()} is dropped and the return value is still this. Read
     * {@link #getSelectedRecipeIndex()} afterwards to see what happened. Picking the recipe
     * that is already selected is also a no-op, since the stonecutter declines a click on the
     * current pick.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Stonecutter")) {
     *   if (inv.getAvailableRecipeCount() > 1) {
     *     inv.selectRecipe(1).selectRecipe(0);
     *     Chat.log(`selected ${inv.getSelectedRecipeIndex()}`);
     *   }
     * }
     * </pre>
     *
     * @param idx the index to select, 0 to {@code getAvailableRecipeCount() - 1}
     * @return self for chaining.
     * @since 1.8.4
     */
    public StoneCutterInventory selectRecipe(int idx) {
        if (idx >= 0 && idx < inventory.getMenu().getNumberOfVisibleRecipes()) {
            inventory.getMenu().clickMenuButton(mc.player, idx);
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(getCurrentSyncId(), idx);
        }
        return this;
    }

    /**
     * how many recipes the item in the input slot can be cut into, which is 0 without one.
     * <p>
     * The stonecutter works this out from the input rather than from a fixed table, so it moves
     * as the input slot changes and is 0 for an input nothing can be cut from. The indices
     * {@link #selectRecipe(int)} takes run from 0 to one less than this.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Stonecutter")) {
     *   Chat.log(`${inv.getAvailableRecipeCount()} cuts available`);
     * }
     * </pre>
     *
     * @return the amount of available recipes
     * @since 1.8.4
     */
    public int getAvailableRecipeCount() {
        return inventory.getMenu().getNumberOfVisibleRecipes();
    }

    /**
     * one item stack per visible recipe, in the same order the indices
     * {@link #selectRecipe(int)} takes.
     * <p>
     * Each entry is the recipe's own displayed result resolved against the current world, so
     * entry {@code i} is what selecting recipe {@code i} puts in {@link #getOutput()}. The
     * list is empty until an input is in the slot, and it holds an entry for every visible
     * recipe whether or not that one is selected.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Stonecutter")) {
     *   const recipes = inv.getRecipes();
     *   for (let i = 0; recipes.size() > i; i++) {
     *     Chat.log(`${i}: ${recipes.get(i).getItemId()}`);
     *   }
     * }
     * </pre>
     *
     * @return a list of all available recipe results in the form of item stacks.
     * @since 1.8.4
     */
    public List<ItemStackHelper> getRecipes() {
        var context = SlotDisplayContext.fromLevel(mc.level);
        return inventory.getMenu().getVisibleRecipes().entries().stream().map(recipe ->
                new ItemStackHelper(recipe.recipe().optionDisplay().resolveForFirstStack(context))
                ).collect(Collectors.toList());
    }

    /**
     * whether there is anything to cut right now, which takes both an input and a recipe for it.
     * <p>
     * It asks two questions rather than one: the input slot has to hold something, and the
     * stonecutter has to have found at least one recipe for it. So an input that is present but
     * cannot be cut is {@code false}, which is the case {@link #getAvailableRecipeCount()}
     * being 0 is the numeric form of.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Stonecutter")) {
     *   if (inv.canCraft()) {
     *     inv.selectRecipe(0);
     *   } else {
     *     Chat.log("nothing to cut");
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the input slot holds an item and the stonecutter found at least
     *         one recipe for it, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canCraft() {
        return inventory.getMenu().hasInputItem();
    }

    @Override
    public String toString() {
        return String.format("StoneCutterInventory:{}");
    }

}
