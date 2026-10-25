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
 * Handles an open enchanting table screen. Its three options use indices 0–2 in the option
 * getters and {@link #doEnchant(int)}; {@link #getRequiredLevels()} reports costs,
 * {@link #getEnchantmentIds()} and {@link #getEnchantmentLevels()} report clue details, and
 * {@link #getEnchantments()} and {@link #getEnchantmentHelpers()} provide resolved names or
 * wrappers. The item and lapis slots are 0 and 1, respectively.
 * <pre>
 * const inv = Player.openInventory();
 * if (inv instanceof Inventories.Enchant) {
 *   if (!inv.getItemToEnchant().isEmpty()) {
 *     const enchants = inv.getEnchantmentHelpers();
 *     for (let i = 0; enchants.length > i; i++) {
 *       if (enchants[i] === null) continue;
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
     * Returns the menu's three required-level thresholds, indexed 0–2. A zero marks an option
     * the menu is not offering. This is the menu's live array; on a successful ordinary-player
     * enchant, the menu deducts the option index plus one levels, not this threshold. Players
     * with infinite materials bypass the level-sufficiency check.
     *
     * @return the required-level threshold of each option, in order, 0 for an option the table is not offering
     * @since 1.3.1
     */
    public int[] getRequiredLevels() {
        return inventory.getMenu().costs;
    }

    /**
     * Returns the three option names as full-level display text; unresolved clues are
     * {@code null}. Entries retain the option indices 0–2.
     *
     * @return the three options as text, in order, with {@code null} for unresolved clues
     * @since 1.3.1
     */
    public TextHelper[] getEnchantments() {
        TextHelper[] enchants = new TextHelper[3];
        var enchRegistry = mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (int j = 0; j < 3; ++j) {
            int index = j;
            enchRegistry.get(inventory.getMenu().enchantClue[j]).ifPresent(enchantment ->
                    enchants[index] = TextHelper.wrap(Enchantment.getFullname(enchantment, inventory.getMenu().levelClue[index])));
        }
        return enchants;
    }

    /**
     * Returns wrappers for the three option clues; each wrapper's level is the offered level,
     * and unresolved clues are {@code null}.
     *
     * @return the three options as enchantment wrappers, in order, with {@code null} for unresolved clues
     * @since 1.8.4
     */
    public EnchantmentHelper[] getEnchantmentHelpers() {
        EnchantmentMenu handler = inventory.getMenu();
        EnchantmentHelper[] enchantments = new EnchantmentHelper[3];
        var enchRegistry = mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (int i = 0; i < 3; i++) {
            int level = i;
            enchRegistry.get(handler.enchantClue[i]).ifPresent(enchantment ->
                    enchantments[level] = new EnchantmentHelper(enchantment, handler.levelClue[level]));
        }
        return enchantments;
    }

    /**
     * Returns the registry id for each option clue, indexed 0–2; unresolved clues are
     * {@code null}.
     *
     * @return the registry id of each option's enchantment, in order, with {@code null} for unresolved clues
     * @since 1.3.1
     */
    public String[] getEnchantmentIds() {
        String[] enchants = new String[3];
        var enchRegistry = mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (int j = 0; j < 3; ++j) {
            enchants[j] = enchRegistry.get(inventory.getMenu().enchantClue[j]).map(Holder::getRegisteredName).orElse(null);
        }
        return enchants;
    }

    /**
     * Returns the menu's three offered enchantment levels, indexed 0–2. An unavailable option
     * is represented by {@code -1}; this is the menu's live array.
     *
     * @return the level of each option, in order, -1 for an option the table is not offering
     * @since 1.3.1
     */
    public int[] getEnchantmentLevels() {
        return inventory.getMenu().levelClue;
    }

    /**
     * Requests the menu to select an option at index 0–2. The local check refuses an invalid
     * index, a missing item, or an unavailable option; ordinary players also need at least
     * {@code index + 1} lapis and experience levels at least both that amount and the threshold
     * from {@link #getRequiredLevels()}. On successful ordinary-player handling, that amount of
     * lapis and levels is consumed; players with infinite materials bypass sufficiency checks
     * and lapis consumption. {@code true} means the local check passed and the request was sent,
     * not that the server confirmed the enchantment; {@code false} means the local check refused it.
     *
     * @param index which of the three options to buy, 0 to 2
     * @return {@code true} if the local menu check passes and the request is sent; {@code false} if the check refuses it
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
     * Returns the stack in the enchanting menu's item slot (slot 0).
     *
     * @return the item being enchanted
     * @since 1.8.4
     */
    public ItemStackHelper getItemToEnchant() {
        return getSlot(0);
    }

    /**
     * Returns the lapis stack in slot 1. A successful ordinary-player enchant consumes
     * {@code index + 1} lapis for the selected option; players with infinite materials do not
     * consume lapis.
     *
     * @return the lapis lazuli stack in slot 1
     * @since 1.8.4
     */
    public ItemStackHelper getLapis() {
        return getSlot(1);
    }

    /**
     * Returns the fixed diagnostic label {@code EnchantInventory:{}}.
     *
     * @return the fixed diagnostic label {@code EnchantInventory:{}}
     */
    @Override
    public String toString() {
        return String.format("EnchantInventory:{}");
    }

}
