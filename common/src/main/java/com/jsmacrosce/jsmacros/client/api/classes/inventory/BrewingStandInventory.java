package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Exposes a brewing stand's potion, ingredient, and fuel slots by name, with helpers to check
 * recipes and preview brewing results without changing the stand.
 * <pre>
 * const inv = Player.openInventory();
 * if (inv.is("Brewing Stand")) {
 *   // what would the three bottles become?
 *   for (const potion of inv.previewPotions()) {
 *     Chat.log(potion.getName());
 *   }
 *   // and is it worth doing at all
 *   if (inv.canBrewCurrentInput()) {
 *     Chat.log(`brewing for another ${inv.getRemainingTicks()} ticks`);
 *   }
 * }
 * </pre>
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Inventory")
@SuppressWarnings("unused")
public class BrewingStandInventory extends Inventory<BrewingStandScreen> {

    /**
     * Wraps the supplied brewing-stand screen and captures its menu and current client-player state.
     * This constructor does not open or create a screen; scripts typically obtain the wrapper from
     * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FPlayer#openInventory()}.
     *
     * @param inventory the brewing-stand screen to wrap
     */
    public BrewingStandInventory(BrewingStandScreen inventory) {
        super(inventory);
    }

    /**
     * @param potion the potion to check
     * @return {@code true} if the stack contains a potion type used as the result of a brewing
     * recipe; {@code false} if it has no potion type or is not brewable.
     * @since 1.8.4
     */
    public boolean isBrewablePotion(ItemStackHelper potion) {
        PotionContents pot = potion.getRaw().get(DataComponents.POTION_CONTENTS);
        if (pot != null && pot.potion().isPresent()) {
            return Objects.requireNonNull(mc.getConnection()).potionBrewing().isBrewablePotion(pot.potion().get());
        }
        return false;
    }

    /**
     * @param ingredient the item to check
     * @return {@code true} if the given item is a valid ingredient, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isValidIngredient(ItemStackHelper ingredient) {
        return Objects.requireNonNull(mc.getConnection()).potionBrewing().isIngredient(ingredient.getRaw());
    }

    /**
     * @param potion     the potion to check
     * @param ingredient the ingredient to check
     * @return {@code true} if the given potion and ingredient can be brewed together, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isValidRecipe(ItemStackHelper potion, ItemStackHelper ingredient) {
        return Objects.requireNonNull(mc.getConnection()).potionBrewing().hasMix(potion.getRaw(), ingredient.getRaw());
    }

    /**
     * @return the current fuel-use count.
     * @since 1.8.4
     */
    public int getFuelCount() {
        return inventory.getMenu().getFuel();
    }

    /**
     * @return the maximum fuel-use count (20).
     * @since 1.8.4
     */
    public int getMaxFuelUses() {
        return BrewingStandBlockEntity.FUEL_USES;
    }

    /**
     * @return {@code true} if the current ingredient is valid and at least one nonempty potion slot
     * has a matching recipe; {@code false} otherwise. This does not check fuel or brewing progress.
     * @since 1.8.4
     */
    public boolean canBrewCurrentInput() {
        ItemStackHelper ingredient = getIngredient();
        if (ingredient.isEmpty()) {
            return false;
        } else if (!isValidIngredient(ingredient)) {
            return false;
        } else {
            for (ItemStackHelper stack : getPotions()) {
                if (!stack.isEmpty() && isValidRecipe(stack, ingredient)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return elapsed brewing ticks, calculated as the standard brewing duration minus the menu's
     * remaining ticks.
     * @since 1.8.4
     */
    public int getBrewTime() {
        return PotionBrewing.BREWING_TIME_SECONDS * 20 - inventory.getMenu().getBrewingTicks();
    }

    /**
     * @return the menu's brewing-tick value, which counts down while brewing.
     * @since 1.8.4
     */
    public int getRemainingTicks() {
        return inventory.getMenu().getBrewingTicks();
    }

    /**
     * @param potion     the potion
     * @param ingredient the ingredient
     * @return the result of mixing the ingredient into the potion stack, or the original potion
     * stack if no mix applies.
     * @since 1.8.4
     */
    public ItemStackHelper previewPotion(ItemStackHelper potion, ItemStackHelper ingredient) {
        return new ItemStackHelper(Objects.requireNonNull(mc.getConnection()).potionBrewing().mix(ingredient.getRaw(), potion.getRaw()));
    }

    /**
     * Returns the results of mixing the current ingredient with each of the three potion slots.
     * Empty or incompatible slots are passed through unchanged by the brewing mix operation.
     * @return the three resulting item stacks, in potion-slot order.
     * @since 1.8.4
     */
    public List<ItemStackHelper> previewPotions() {
        ItemStack ingredient = getIngredient().getRaw();
        return getPotions().stream().map(stack -> new ItemStackHelper(Objects.requireNonNull(mc.getConnection()).potionBrewing().mix(ingredient, stack.getRaw()))).collect(Collectors.toList());
    }

    /**
     * @return the item in slot 3, the ingredient slot.
     * @since 1.8.4
     */
    public ItemStackHelper getIngredient() {
        return getSlot(3);
    }

    /**
     * @return the item in slot 4, the fuel slot.
     * @since 1.8.4
     */
    public ItemStackHelper getFuel() {
        return getSlot(4);
    }

    /**
     * @return the item in potion slot 0.
     * @since 1.8.4
     */
    public ItemStackHelper getFirstPotion() {
        return getSlot(0);
    }

    /**
     * @return the item in potion slot 1.
     * @since 1.8.4
     */
    public ItemStackHelper getSecondPotion() {
        return getSlot(1);
    }

    /**
     * @return the item in potion slot 2.
     * @since 1.8.4
     */
    public ItemStackHelper getThirdPotion() {
        return getSlot(2);
    }

    /**
     * @return the items in potion slots 0, 1, and 2, in slot order.
     * @since 1.8.4
     */
    public List<ItemStackHelper> getPotions() {
        return Arrays.asList(getSlot(0), getSlot(1), getSlot(2));
    }

    /**
     * Returns the fixed diagnostic string {@code "BrewingStandInventory:{}"}; it does not include
     * inventory contents.
     *
     * @return the literal {@code "BrewingStandInventory:{}"}
     */
    @Override
    public String toString() {
        return String.format("BrewingStandInventory:{}");
    }

}
