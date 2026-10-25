package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

/**
 * Handle for an open grindstone screen.
 * <p>
 * {@link #getTopInput()}, {@link #getBottomInput()} and {@link #getOutput()} read menu slots 0,
 * 1 and 2 respectively; {@link #getMap()} groups the input and output slots as {@code input}
 * and {@code output}.
 * <p>
 * {@link #simulateXp()} returns the lower bound of the experience awarded when the result is
 * taken; it does not take the result or change the inventory.
 * <pre>
 * const inv = Player.openInventory();
 * if (inv instanceof Inventories.GrindStone) {
 *   if (!inv.getOutput().isEmpty()) {
 *     const floor = inv.simulateXp();
 *     Chat.log(`grinding to ${inv.getOutput().getName()} for ${floor} to ${2 * floor - 1} xp`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Inventory")
@SuppressWarnings("unused")
public class GrindStoneInventory extends Inventory<GrindstoneScreen> {

    /**
     * Wraps the supplied grindstone screen; it does not open or create a screen.
     * Use {@link com.jsmacrosce.jsmacros.client.api.library.impl.FPlayer#openInventory()} to obtain
     * a wrapper for the currently open inventory.
     *
     * @param inventory grindstone screen to wrap
     */
    public GrindStoneInventory(GrindstoneScreen inventory) {
        super(inventory);
    }

    /**
     * Returns the item in the top input slot (slot 0).
     *
     * @return the top input item
     * @since 1.8.4
     */
    public ItemStackHelper getTopInput() {
        return getSlot(0);
    }

    /**
     * Returns the item in the bottom input slot (slot 1).
     *
     * @return the bottom input item
     * @since 1.8.4
     */
    public ItemStackHelper getBottomInput() {
        return getSlot(1);
    }

    /**
     * Returns the current item in the result slot (slot 2), or an empty item if there is no result.
     * Taking the result awards the experience estimated by {@link #simulateXp()}.
     * <p>
     * <pre>
     * const inv = Player.openInventory();
     * if (inv instanceof Inventories.GrindStone) {
     *   const out = inv.getOutput();
     *   if (!out.isEmpty()) {
     *     Chat.log(`grinding to ${out.getName()}`);
     *   }
     * }
     * </pre>
     *
     * @return the result the grindstone is currently offering
     * @since 1.8.4
     */
    public ItemStackHelper getOutput() {
        return getSlot(2);
    }

    /**
     * Estimates the minimum experience awarded for enchantments in both input slots, or 0 if
     * there are no non-curse enchantments to remove.
     * <p>
     * It sums each non-curse enchantment's minimum cost at its current level across both inputs,
     * then rounds half the sum up. Curses are retained and contribute nothing. When the result
     * is taken, the actual award is randomized from this value (inclusive) to
     * {@code 2 * simulateXp() - 1}. This method only reads the inputs and does not change the
     * inventory.
     * <p>
     * <pre>
     * const inv = Player.openInventory();
     * if (inv instanceof Inventories.GrindStone) {
     *   const floor = inv.simulateXp();
     *   if (floor > 0) {
     *     Chat.log(`between ${floor} and ${2 * floor - 1} xp when the result is taken`);
     *   }
     * }
     * </pre>
     *
     * @return the minimum experience award, or 0 when no non-curse enchantment contributes
     * @since 1.8.4
     */
    public int simulateXp() {
        int xp = 0;
        xp += this.getExperience(getTopInput().getRaw());
        xp += this.getExperience(getBottomInput().getRaw());
        return xp > 0 ? (int) Math.ceil((double) xp / 2.0) : 0;
    }

    private int getExperience(ItemStack stack) {
        int i = 0;
        ItemEnchantments lv = EnchantmentHelper.getEnchantmentsForCrafting(stack);

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : lv.entrySet()) {
            Holder<Enchantment> lv2 = entry.getKey();
            int j = entry.getIntValue();
            if (!lv2.is(EnchantmentTags.CURSE)) {
                i += lv2.value().getMinCost(j);
            }
        }

        return i;
    }

    /**
     * Returns the fixed diagnostic string {@code GrindStoneInventory:{}}.
     *
     * @return {@code GrindStoneInventory:{}}
     */
    @Override
    public String toString() {
        return String.format("GrindStoneInventory:{}");
    }

}
