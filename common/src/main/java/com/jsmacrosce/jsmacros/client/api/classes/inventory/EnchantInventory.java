package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.EnchantmentHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

/**
 * the handle for an open enchanting table screen.
 * <p>
 * An enchanting table always has exactly three options to choose between, and nearly every
 * method here is about them. Each option has a cost in experience levels and a level of the
 * enchantment it would give, and the three are read as parallel arrays of length 3:
 * {@link #getRequiredLevels()} for the costs, {@link #getEnchantmentIds()} for what the
 * enchantments are, and {@link #getEnchantmentLevels()} for the levels they would be applied
 * at. {@link #getEnchantmentHelpers()} and {@link #getEnchantments()} are the same three
 * options wrapped up so a script can read a name without doing the lookup itself.
 * <p>
 * The index into those arrays is the same index {@link #doEnchant(int)} takes, so an option
 * can be read and then bought by number. It is 0 to 2 and nothing else.
 * <p>
 * The two slots are the item being enchanted at slot 0 and the lapis lazuli at slot 1,
 * which is the order the enchanting table's menu builds them. {@link #getMap()} names the same
 * two regions {@code item} and {@code lapis}.
 * example:
 * <pre>
 * const inv = Player.openInventory();
 * if (inv.is("Enchanting Table")) {
 *   if (!inv.getItemToEnchant().isEmpty()) {
 *     const enchants = inv.getEnchantmentHelpers();
 *     for (let i = 0; enchants.length > i; i++) {
 *       Chat.log(`${i}: ${enchants[i].getId()} ${enchants[i].getLevel()} for ${inv.getRequiredLevels()[i]} levels`);
 *     }
 *   }
 * }
 * </pre>
 *
 * @since 1.3.1
 */
@DocletCategory("Inventory")
@SuppressWarnings("unused")
public class EnchantInventory extends Inventory<EnchantmentScreen> {

    protected EnchantInventory(EnchantmentScreen inventory) {
        super(inventory);
    }

    /**
     * the experience level cost of each of the three options, as a three element array.
     * <p>
     * This is the menu's own array rather than a copy of it, and it is indexed the same way as
     * {@link #doEnchant(int)}. Every entry is 0 while the table has no item to enchant, so an
     * array of three zeroes means there is nothing on offer rather than that the options are
     * free. Unlike {@link #getEnchantments()} and {@link #getEnchantmentIds()} this cannot
     * fail on an empty table, so it is the safe thing to read first.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Enchanting Table")) {
     *   const costs = inv.getRequiredLevels();
     *   if (costs[0] > 0) {
     *     Chat.log(`the cheapest option costs ${costs[0]} levels`);
     *   }
     * }
     * </pre>
     *
     * @return the level cost of each option, in order, 0 for an option the table is not offering
     * @since 1.3.1
     */
    public int[] getRequiredLevels() {
        return inventory.getMenu().costs;
    }

    /**
     * the three options as display text, in order.
     * <p>
     * Each entry is the enchantment's full name at the level this table would apply it, which
     * includes the roman numeral for the level, so a script that needs the enchantment's
     * registry id or its level separately is better served by {@link #getEnchantmentIds()} and
     * {@link #getEnchantmentLevels()}.
     * <p>
     * This reads each option out of the registry by numeric id and insists on finding it, so
     * it raises {@link java.util.NoSuchElementException} whenever the table is offering fewer
     * than three enchantments, which is what happens with no item in the slot.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Enchanting Table")) {
     *   if (!inv.getItemToEnchant().isEmpty()) {
     *     for (const text of inv.getEnchantments()) {
     *       Chat.log(text.getString());
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the three options as text, in order
     * @throws java.util.NoSuchElementException if the table is not offering three enchantments
     * @since 1.3.1
     */
    public TextHelper[] getEnchantments() {
        TextHelper[] enchants = new TextHelper[3];
        var enchRegistry = mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (int j = 0; j < 3; ++j) {
            Holder<Enchantment> enchantment = enchRegistry.get(inventory.getMenu().enchantClue[j]).orElseThrow();
            if ((enchantment) != null) {
                enchants[j] = TextHelper.wrap(Enchantment.getFullname(enchantment, inventory.getMenu().levelClue[j]));
            }
        }
        return enchants;
    }

    /**
     * the three options as enchantment wrappers, in order, each carrying the level this table
     * would apply it at.
     * <p>
     * This is the richest of the three views: {@link EnchantmentHelper} answers for the name,
     * the registry id, the level and the minimum and maximum level the enchantment has in
     * vanilla, so a script does not have to split the display text. The level on each wrapper
     * is the one this table is offering, not the enchantment's maximum.
     * <p>
     * Like {@link #getEnchantments()} this insists on resolving all three enchantments and so
     * raises {@link java.util.NoSuchElementException} when the table is offering fewer than
     * three of them.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Enchanting Table")) {
     *   if (!inv.getItemToEnchant().isEmpty()) {
     *     const first = inv.getEnchantmentHelpers()[0];
     *     Chat.log(`${first.getId()} ${first.getLevel()}, max level ${first.getMaxLevel()}`);
     *   }
     * }
     * </pre>
     *
     * @return the three options as enchantment wrappers, in order
     * @throws java.util.NoSuchElementException if the table is not offering three enchantments
     * @since 1.8.4
     */
    public EnchantmentHelper[] getEnchantmentHelpers() {
        EnchantmentMenu handler = inventory.getMenu();
        EnchantmentHelper[] enchantments = new EnchantmentHelper[3];
        var enchRegistry = mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (int i = 0; i < 3; i++) {
            enchantments[i] = new EnchantmentHelper(enchRegistry.get(handler.enchantClue[i]).orElseThrow(), handler.levelClue[i]);
        }
        return enchantments;
    }

    /**
     * the registry id of the enchantment behind each of the three options, in order.
     * <p>
     * These are registry ids such as {@code "minecraft:sharpness"} rather than display names,
     * and they line up index for index with {@link #getRequiredLevels()},
     * {@link #getEnchantmentLevels()} and {@link #doEnchant(int)}. Because the lookup is
     * required to succeed, this raises {@link java.util.NoSuchElementException} whenever the
     * table is offering fewer than three enchantments.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Enchanting Table")) {
     *   if (!inv.getItemToEnchant().isEmpty()) {
     *     const ids = inv.getEnchantmentIds();
     *     for (let i = 0; ids.length > i; i++) {
     *       Chat.log(`${ids[i]} at level ${inv.getEnchantmentLevels()[i]}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the registry id of each option's enchantment, in order
     * @throws java.util.NoSuchElementException if the table is not offering three enchantments
     * @since 1.3.1
     */
    public String[] getEnchantmentIds() {
        String[] enchants = new String[3];
        var enchRegistry = mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (int j = 0; j < 3; ++j) {
            Holder<Enchantment> enchantment = enchRegistry.get(inventory.getMenu().enchantClue[j]).orElseThrow();
            enchants[j] = enchantment.getRegisteredName();
        }
        return enchants;
    }

    /**
     * the level each of the three options would apply its enchantment at, in order.
     * <p>
     * This is the menu's own array and an option the table is not offering reads back as -1
     * rather than as 0, so it is the one of the three parallel arrays that can hold a negative
     * and can still be read on an empty table without failing. Dropping the negative entries
     * is a reasonable way to ask how many options there actually are.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Enchanting Table")) {
     *   const levels = inv.getEnchantmentLevels();
     *   const offered = levels.filter((l) => l > 0).length;
     *   Chat.log(`${offered} of 3 options on offer`);
     * }
     * </pre>
     *
     * @return the level of each option, in order, -1 for an option the table is not offering
     * @since 1.3.1
     */
    public int[] getEnchantmentLevels() {
        return inventory.getMenu().levelClue;
    }

    /**
     * buys one of the three options, which spends the levels and the lapis and applies the
     * enchantment.
     * <p>
     * The index is the same one the three parallel arrays are indexed by, so it runs from 0 to
     * 2; anything outside that range is refused and logged rather than bought. The click is
     * checked locally before it is sent, so the table's own conditions decide: there has to be
     * an item in slot 0, enough lapis for the option (option {@code index} costs
     * {@code index + 1} lapis), and enough experience levels. A {@code false} return means one
     * of those was not met and nothing happened, so it is the way to find out whether an
     * option is affordable without attempting it.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Enchanting Table")) {
     *   if (!inv.getItemToEnchant().isEmpty()) {
     *     if (inv.doEnchant(0)) {
     *       Chat.log(`took option 0: ${inv.getEnchantmentIds()[0]}`);
     *     } else {
     *       Chat.log(`option 0 refused, it costs ${inv.getRequiredLevels()[0]} levels`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @param index which of the three options to buy, 0 to 2
     * @return {@code true} if the enchantment was applied, {@code false} if it was refused
     * @since 1.3.1
     */
    public boolean doEnchant(int index) {
        assert mc.gameMode != null;
        if (inventory.getMenu().clickMenuButton(mc.player, index)) {
            mc.gameMode.handleInventoryButtonClick(syncId, index);
            return true;
        }
        return false;
    }

    /**
     * the item on the table, which is slot 0.
     * <p>
     * Everything the table offers is worked out from this item, so it being empty is what
     * makes {@link #getRequiredLevels()} come back as three zeroes and makes the three
     * registry lookups fail. This reads the slot and nothing more, so an item the table would
     * refuse still comes back as present; {@link #getRequiredLevels()} is what says whether
     * there is anything on offer.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Enchanting Table")) {
     *   const item = inv.getItemToEnchant();
     *   if (!item.isEmpty()) {
     *     Chat.log(`enchanting ${item.getName()}`);
     *   }
     * }
     * </pre>
     *
     * @return the item being enchanted
     * @since 1.8.4
     */
    public ItemStackHelper getItemToEnchant() {
        return getSlot(0);
    }

    /**
     * the lapis lazuli the table charges for an enchantment, which is slot 1.
     * <p>
     * The number the table will actually take is the option index plus one, so the cheapest
     * option costs one lapis. Reading this does not reserve anything; taking the result is what
     * consumes it.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Enchanting Table")) {
     *   Chat.log(`${inv.getLapis().getCount()} lapis available`);
     * }
     * </pre>
     *
     * @return the lapis lazuli in the table
     * @since 1.8.4
     */
    public ItemStackHelper getLapis() {
        return getSlot(1);
    }

    @Override
    public String toString() {
        return String.format("EnchantInventory:{}");
    }

}
