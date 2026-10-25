package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinAnvilScreen;

/**
 * Handle for an open anvil screen. Slots 0, 1, and 2 are the first input, second input, and
 * result, respectively; {@link #getOutput()} reads the current result rather than calculating it.
 * <pre>
 * const inv = Player.openInventory();
 * if (inv instanceof Inventories.Anvil) {
 *   Chat.log(`${inv.getLeftInput().getItemId()} + ${inv.getRightInput().getItemId()}`);
 *   if (!inv.getOutput().isEmpty()) {
 *     Chat.log(`result: ${inv.getOutput().getName()} for ${inv.getLevelCost()} levels`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Inventory")
@SuppressWarnings("unused")
public class AnvilInventory extends Inventory<AnvilScreen> {

    /**
     * Wraps the supplied anvil screen and captures its menu and the current client player through
     * {@link Inventory}'s setup. This does not open or create a screen; scripts typically obtain
     * a wrapper for the current screen through {@link com.jsmacrosce.jsmacros.client.api.library.impl.FPlayer#openInventory()}.
     *
     * @param inventory the anvil screen to wrap
     */
    public AnvilInventory(AnvilScreen inventory) {
        super(inventory);
    }

    /**
     * Returns the current text in the anvil rename box. This is box text, not a guarantee
     * that the anvil will produce a result; it may remain readable after the first input is removed.
     *
     * @return the current text of the anvil's rename box
     * @since 1.8.4
     */
    public String getName() {
        return ((MixinAnvilScreen) inventory).getNameField().getValue();
    }

    /**
     * Sets the anvil rename-box text. Changing the box updates the anvil's cost and result;
     * the renamed item is obtained by taking the result. The box accepts at most 50 characters.
     *
     * @param name the text to put in the rename box
     * @return self for chaining.
     * @since 1.8.4
     */
    public AnvilInventory setName(String name) {
        ((MixinAnvilScreen) inventory).getNameField().setValue(name);
        return this;
    }

    /**
     * Returns the current operation cost in experience levels. It can be nonzero with no
     * result when the cost is at least 40; players with infinite materials may still take
     * such a result. Rename-only costs are capped at 39.
     *
     * @return the level cost the anvil is asking for, 0 when there is nothing to take
     * @since 1.8.4
     */
    public int getLevelCost() {
        return inventory.getMenu().getCost();
    }

    /**
     * Returns the repair-cost data value on the first input item, or 0 if it has none. This
     * value contributes to anvil level cost; it is not an item count.
     *
     * @return the first input item's repair cost, 0 when it carries none
     * @since 1.8.4
     */
    public int getItemRepairCost() {
        return getSlot(0).getRepairCost();
    }

    /**
     * Returns the fixed threshold of 40 used by anvil cost rules. Rename-only costs are
     * capped at 39; other costs of 40 or more normally suppress the result.
     *
     * @return 40
     * @since 1.8.4
     */
    public int getMaximumLevelCost() {
        return 40;
    }

    /**
     * Returns the first input item (slot 0), which is the item repaired or renamed.
     *
     * @return the first input item
     * @since 1.8.4
     */
    public ItemStackHelper getLeftInput() {
        return getSlot(0);
    }

    /**
     * Returns the second input item (slot 1), used as repair material or an enchantment source.
     * It may be empty, including when the anvil is used only for renaming.
     *
     * @return the second input item
     * @since 1.8.4
     */
    public ItemStackHelper getRightInput() {
        return getSlot(1);
    }

    /**
     * Returns the current client-side result in slot 2; taking it applies the rename and
     * charges the anvil cost.
     *
     * @return the result the anvil is currently offering
     * @since 1.8.4
     */
    public ItemStackHelper getOutput() {
        return getSlot(2);
    }

    /**
     * Returns this wrapper's fixed diagnostic label.
     *
     * @return {@code AnvilInventory:{}}
     */
    @Override
    public String toString() {
        return String.format("AnvilInventory:{}");
    }

}
