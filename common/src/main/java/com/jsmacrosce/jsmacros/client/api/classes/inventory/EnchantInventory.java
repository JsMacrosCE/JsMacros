package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.EnchantmentHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

/**
 * @since 1.3.1
 */
@SuppressWarnings("unused")
public class EnchantInventory extends Inventory<EnchantmentScreen> {

    protected EnchantInventory(EnchantmentScreen inventory) {
        super(inventory);
    }

    /**
     * @return xp level required to do enchantments
     * @since 1.3.1
     */
    public int[] getRequiredLevels() {
        return inventory.getMenu().costs;
    }

    /**
     * @return list of enchantments text.
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
     * @return the visible enchantment for each level.
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
     * @return id for enchantments
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
     * @return level of enchantments
     * @since 1.3.1
     */
    public int[] getEnchantmentLevels() {
        return inventory.getMenu().levelClue;
    }

    /**
     * clicks the button to enchant.
     *
     * @param index
     * @return success
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
     * @return the item to be enchanted.
     * @since 1.8.4
     */
    public ItemStackHelper getItemToEnchant() {
        return getSlot(0);
    }

    /**
     * @return the slot containing the lapis lazuli.
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
