package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinAbstractFurnaceScreenHandler;

import java.util.Map;

/**
 * Inventory wrapper for a furnace screen, exposing its input, fuel, result, and smelting state.
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Inventory")
@SuppressWarnings("unused")
public class FurnaceInventory extends RecipeInventory<AbstractFurnaceScreen<?>> {

    /**
     * Wraps a furnace screen without opening or creating it; use {@link com.jsmacrosce.jsmacros.client.api.library.impl.FPlayer#openInventory()} to obtain the current inventory wrapper.
     *
     * @param inventory the furnace screen to wrap
     */
    public FurnaceInventory(AbstractFurnaceScreen<?> inventory) {
        super(inventory);
    }

    /**
     * Returns the item currently in the furnace menu's result slot.
     *
     * @return the item in the result slot
     * @since 1.8.4
     */
    @Override
    public ItemStackHelper getOutput() {
        return new ItemStackHelper(inventory.getMenu().getResultSlot().getItem());
    }

    /**
     * Returns the furnace's sole input item; the coordinates are ignored.
     *
     * @param x ignored
     * @param y ignored
     * @return the item in the input slot
     * @since 1.8.4
     */
    public ItemStackHelper getInput(int x, int y) {
        return getSmeltedItem();
    }

    /**
     * Returns the fixed width of the furnace input grid.
     * @return 1
     * @since 1.8.4
     */
    @Override
    public int getCraftingWidth() {
        return 1;
    }

    /**
     * Returns the fixed height of the furnace input grid.
     * @return 1
     * @since 1.8.4
     */
    @Override
    public int getCraftingHeight() {
        return 1;
    }

    /**
     * Returns the number of input slots, excluding the fuel and result slots.
     * @return 1
     * @since 1.8.4
     */
    @Override
    public int getCraftingSlotCount() {
        return 1;
    }

    /**
     * @return the currently smelting item.
     * @since 1.8.4
     */
    public ItemStackHelper getSmeltedItem() {
        return getSlot(0);
    }

    /**
     * @return the fuel item.
     * @since 1.8.4
     */
    public ItemStackHelper getFuel() {
        return getSlot(1);
    }

    /**
     * @param stack the item to check
     * @return {@code true} if the item is a valid fuel, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canUseAsFuel(ItemStackHelper stack) {
        return ((MixinAbstractFurnaceScreenHandler) inventory.getMenu()).invokeIsFuel(stack.getRaw());
    }

    /**
     * @param stack the item to check
     * @return {@code true} if the item can be smelted, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSmeltable(ItemStackHelper stack) {
        return ((MixinAbstractFurnaceScreenHandler) inventory.getMenu()).invokeIsSmeltable(stack.getRaw());
    }

    /**
     * Returns item registry IDs mapped to their burn times in ticks, using the current world's fuel values.
     * @return the fuel-value map
     * @since 1.8.4
     */
    public Map<String, Integer> getFuelValues() {
        Object2IntMap<String> fuelMap = new Object2IntOpenHashMap<>();
        for (Map.Entry<Item, Integer> entry : mc.level.fuelValues().values.entrySet()) {
            fuelMap.put(BuiltInRegistries.ITEM.getKey(entry.getKey()).toString(), entry.getValue().intValue());
        }
        return fuelMap;
    }

    /**
     * Returns the furnace menu's current smelting progress in ticks.
     * @return the current progress
     * @since 1.8.4
     */
    public int getSmeltingProgress() {
        return getPropertyDelegate().get(2);
    }

    /**
     * Returns the total smelting time reported by the furnace menu, in ticks.
     * @return the total smelting time
     * @since 1.8.4
     */
    public int getTotalSmeltingTime() {
        return getPropertyDelegate().get(3);
    }

    /**
     * Returns the total smelting time minus the current progress, in ticks.
     * @return the calculated remaining time
     * @since 1.8.4
     */
    public int getRemainingSmeltingTime() {
        return getTotalSmeltingTime() - getSmeltingProgress();
    }

    /**
     * Returns the furnace menu's remaining fuel time in ticks.
     * @return the remaining fuel time
     * @since 1.8.4
     */
    public int getRemainingFuelTime() {
        return getPropertyDelegate().get(0);
    }

    /**
     * Returns the furnace menu's total fuel time in ticks.
     * @return the total fuel time
     * @since 1.8.4
     */
    public int getTotalFuelTime() {
        return getPropertyDelegate().get(1);
    }

    private ContainerData getPropertyDelegate() {
        return ((MixinAbstractFurnaceScreenHandler) inventory.getMenu()).getPropertyDelegate();
    }

    /**
     * @return {@code true} if the furnace menu is lit, {@code false} otherwise
     * @since 1.8.4
     */
    public boolean isBurning() {
        return inventory.getMenu().isLit();
    }

    /**
     * Returns the fixed diagnostic label for this inventory wrapper.
     *
     * @return {@code "FurnaceInventory:{}"}
     */
    @Override
    public String toString() {
        return String.format("FurnaceInventory:{}");
    }

}
