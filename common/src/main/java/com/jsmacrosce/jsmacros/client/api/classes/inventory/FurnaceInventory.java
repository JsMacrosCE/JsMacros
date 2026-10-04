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
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Inventory")
@SuppressWarnings("unused")
public class FurnaceInventory extends RecipeInventory<AbstractFurnaceScreen<?>> {

    public FurnaceInventory(AbstractFurnaceScreen<?> inventory) {
        super(inventory);
    }

    /**
     * the result slot, which is what the furnace is working towards.
     * <p>
     * This reads the slot rather than working out the recipe, so it is whatever the client
     * currently has there. The input it is working from is {@link #getSmeltedItem()}, and
     * whether the furnace is lit at all is {@link #isBurning()}, so those three together say
     * more about the state of the furnace than this does on its own.
     * <p>
     * It is slot 2 of the menu, the same as {@code getSlot(2)}; the furnace's three slots are
     * the input, the fuel and then this one.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Furnace")) {
     *   const out = inv.getOutput();
     *   if (!out.isEmpty()) {
     *     Chat.log(`smelting into ${out.getName()}`);
     *   }
     * }
     * </pre>
     *
     * @return the item in the result slot
     * @since 1.8.4
     */
    @Override
    public ItemStackHelper getOutput() {
        return new ItemStackHelper(inventory.getMenu().getResultSlot().getItem());
    }

    /**
     * @param x the x position of the input, will always be 0
     * @param y the y position of the input, will always be 0
     * @return the currently smelting item.
     * @since 1.8.4
     */
    public ItemStackHelper getInput(int x, int y) {
        return getSmeltedItem();
    }

    /**
     * always 1, because a furnace has a single input slot rather than a grid.
     * <p>
     * The furnace is a recipe inventory only in the sense that it has an input and an output, so
     * the grid measurements are fixed rather than read from anything. Both of them being 1 is
     * what makes {@link #getInputSize()} come back as 1 here.
     * @return 1
     * @since 1.8.4
     */
    @Override
    public int getCraftingWidth() {
        return 1;
    }

    /**
     * always 1, because a furnace has a single input slot rather than a grid.
     * <p>
     * The furnace is a recipe inventory only in the sense that it has an input and an output, so
     * the grid measurements are fixed rather than read from anything.
     * @return 1
     * @since 1.8.4
     */
    @Override
    public int getCraftingHeight() {
        return 1;
    }

    /**
     * always 1, the number of input slots a furnace has.
     * <p>
     * This is the count of input slots, so the fuel slot is not part of it. It agrees with
     * {@link #getCraftingWidth()} times {@link #getCraftingHeight()} here, which is what
     * {@link #getInputSize()} computes.
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
     * @return a map of all valid fuels and their burn times in ticks.
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
     * If the returned value equals {@link #getTotalSmeltingTime()} then the item is done smelting.
     *
     * @return the current Smelting progress in ticks.
     * @since 1.8.4
     */
    public int getSmeltingProgress() {
        return getPropertyDelegate().get(2);
    }

    /**
     * @return the total smelting time of a single input item in ticks.
     * @since 1.8.4
     */
    public int getTotalSmeltingTime() {
        return getPropertyDelegate().get(3);
    }

    /**
     * @return the remaining time of the smelting progress in ticks.
     * @since 1.8.4
     */
    public int getRemainingSmeltingTime() {
        return getTotalSmeltingTime() - getSmeltingProgress();
    }

    /**
     * @return the remaining fuel time in ticks.
     * @since 1.8.4
     */
    public int getRemainingFuelTime() {
        return getPropertyDelegate().get(0);
    }

    /**
     * @return the total fuel time of the current fuel item in ticks.
     * @since 1.8.4
     */
    public int getTotalFuelTime() {
        return getPropertyDelegate().get(1);
    }

    private ContainerData getPropertyDelegate() {
        return ((MixinAbstractFurnaceScreenHandler) inventory.getMenu()).getPropertyDelegate();
    }

    /**
     * @return {@code true} if the furnace is currently smelting an item, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isBurning() {
        return inventory.getMenu().isLit();
    }

    @Override
    public String toString() {
        return String.format("FurnaceInventory:{}");
    }

}
