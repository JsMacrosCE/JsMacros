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
 * the handle for an open grindstone screen.
 * <p>
 * A grindstone strips enchantments off an item and grinds it down, and it works from two input
 * slots into a single result slot: the top slot, the bottom slot, and the result. Those three
 * are {@link #getTopInput()}, {@link #getBottomInput()} and {@link #getOutput()}, numbered 0,
 * 1 and 2 in the order the grindstone's menu builds them, which is the same numbering as the
 * vanilla {@code GrindstoneMenu} uses for its {@code INPUT_SLOT}, {@code ADDITIONAL_SLOT} and
 * {@code RESULT_SLOT}. {@link #getMap()} names the same two regions {@code input} and
 * {@code output}.
 * <p>
 * The other half of this class is {@link #simulateXp()}, which works out what the grindstone
 * would pay in experience for the enchantments currently on the two inputs. The grindstone
 * pays that out when the result is taken, not when this is called.
 * example:
 * <pre>
 * const inv = Player.openInventory();
 * if (inv.is("Grindstone")) {
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

    public GrindStoneInventory(GrindstoneScreen inventory) {
        super(inventory);
    }

    /**
     * the item in the top input slot, which is slot 0.
     * <p>
     * Putting something here strips its non-curse enchantments and grinds it down; the result
     * appears in {@link #getOutput()} as soon as the slot changes. This reads the slot rather
     * than contributing to anything, so reading it twice gives the same answer unless the slot
     * changed in between.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Grindstone")) {
     *   if (!inv.getTopInput().isEmpty()) {
     *     Chat.log(`top input ${inv.getTopInput().getName()}`);
     *   }
     * }
     * </pre>
     *
     * @return the top input item
     * @since 1.8.4
     */
    public ItemStackHelper getTopInput() {
        return getSlot(0);
    }

    /**
     * the item in the bottom input slot, which is slot 1.
     * <p>
     * The bottom slot is a second, equivalent input rather than a material: a grindstone takes
     * up to two items and treats them the same way, so this can be empty while
     * {@link #getTopInput()} is not.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Grindstone")) {
     *   if (!inv.getBottomInput().isEmpty()) {
     *     Chat.log(`bottom input ${inv.getBottomInput().getName()}`);
     *   }
     * }
     * </pre>
     *
     * @return the bottom input item
     * @since 1.8.4
     */
    public ItemStackHelper getBottomInput() {
        return getSlot(1);
    }

    /**
     * the item the grindstone is working towards, which is slot 2.
     * <p>
     * This is the result slot as the client currently has it, not a calculation done here, and
     * it stays empty when the two inputs produce nothing. Taking it is what the grindstone pays
     * out the experience {@link #simulateXp()} estimates.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Grindstone")) {
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
     * the smallest amount of experience the grindstone would pay for the enchantments on the
     * two input slots, or 0 when there are none to pay for.
     * <p>
     * This is a simulation of the grindstone's own arithmetic and it works the same way the
     * grindstone does: for each of the two inputs it adds up the minimum cost of every
     * enchantment on the item, skipping curses because the grindstone keeps those and so does
     * not pay for removing them, then halves the total and rounds up. A curse enchantment is
     * therefore worth nothing here even though it is on the item.
     * <p>
     * The grindstone picks a random amount at that lower bound when it pays out, so this is
     * the floor and not the amount. The largest it can pay is {@code 2 * simulateXp() - 1},
     * because the roll it adds is drawn from a range of the halved total, so doubling the
     * return value overshoots by one whenever the halved total is 1 or more.
     * <p>
     * Nothing here changes the grindstone, and the estimate follows the slots rather than the
     * result: an input that has been put in but whose result has not been taken still counts.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Grindstone")) {
     *   const floor = inv.simulateXp();
     *   if (floor > 0) {
     *     Chat.log(`between ${floor} and ${2 * floor - 1} xp when the result is taken`);
     *   }
     * }
     * </pre>
     *
     * @return the least experience the grindstone would pay, 0 when there is nothing to pay for
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

    @Override
    public String toString() {
        return String.format("GrindStoneInventory:{}");
    }

}
