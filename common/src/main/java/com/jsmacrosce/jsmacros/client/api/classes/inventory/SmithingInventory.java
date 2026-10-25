package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

/**
 * Wraps an open smithing table screen. Its input slots are 0 (template), 1 (base), and 2
 * (addition); the result is slot 3. The input accessors map to those first three slots, while
 * {@link #getOutput()} returns the addition input; read the result with {@code getSlot(3)}.
 * <pre>
 * const inv = Player.openInventory();
 * if (inv instanceof Inventories.Smithing) {
 *   Chat.log(`template ${inv.getLeftInput().getItemId()}`);
 *   Chat.log(`base ${inv.getRightInput().getItemId()}`);
 *   Chat.log(`addition ${inv.getOutput().getItemId()}`);
 *   const result = inv.getSlot(3);
 *   if (!result.isEmpty()) {
 *     Chat.log(`smiths into ${result.getItemId()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Inventory")
@SuppressWarnings("unused")
public class SmithingInventory extends Inventory<SmithingScreen> {

    /**
     * Wraps the supplied smithing screen and captures its menu and the current client player
     * through {@link Inventory}'s setup. This does not open or create a screen; scripts typically
     * obtain a wrapper for the current screen through {@link com.jsmacrosce.jsmacros.client.api.library.impl.FPlayer#openInventory()}.
     *
     * @param inventory the smithing screen to wrap
     */
    public SmithingInventory(SmithingScreen inventory) {
        super(inventory);
    }

    /**
     * Returns the template input in menu slot 0.
     *
     * @return the template item in the first slot.
     * @since 1.8.4
     */
    public ItemStackHelper getLeftInput() {
        return getSlot(0);
    }

    /**
     * Returns the base input in menu slot 1.
     *
     * @return the base item in the second slot.
     * @since 1.8.4
     */
    public ItemStackHelper getRightInput() {
        return getSlot(1);
    }

    /**
     * Returns the addition input in menu slot 2, not the result in slot 3.
     *
     * @return the addition item in the third slot.
     * @since 1.8.4
     */
    public ItemStackHelper getOutput() {
        return getSlot(2);
    }

    /**
     * Returns this wrapper's fixed diagnostic label.
     *
     * @return {@code SmithingInventory:{}}
     */
    @Override
    public String toString() {
        return String.format("SmithingInventory:{}");
    }

}
