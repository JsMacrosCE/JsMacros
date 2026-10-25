package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.doclet.DocletCategory;

/**
 * A handle for an open cartography table screen. The input slots are 0 and 1, and slot 2 is
 * the result; {@link #getMap()} groups the inputs as {@code input} and the result as {@code output}.
 * <p>
 * The second input accepts paper, maps, or glass panes. The menu supplies the result; this
 * wrapper does not calculate it.
 * <pre>
 * const inv = Player.openInventory();
 * if (inv instanceof Inventories.Cartography) {
 *   Chat.log(`${inv.getMapItem().getItemId()} with ${inv.getMaterial().getItemId()}`);
 *   if (!inv.getOutput().isEmpty()) {
 *     Chat.log(`table offers ${inv.getOutput().getCount()} item(s)`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Inventory")
@SuppressWarnings("unused")
public class CartographyInventory extends Inventory<CartographyTableScreen> {

    /**
     * Wraps the supplied cartography screen and captures its menu and the current client player
     * through {@link Inventory}'s setup. This does not open or create a screen; scripts can obtain
     * a wrapper for the current screen through {@link com.jsmacrosce.jsmacros.client.api.library.impl.FPlayer#openInventory()}.
     *
     * @param inventory the cartography screen to wrap
     */
    public CartographyInventory(CartographyTableScreen inventory) {
        super(inventory);
    }

    /**
     * Returns the item in the first input slot. The menu accepts items carrying a map ID here.
     *
     * @return the map item in the first slot
     * @since 1.8.4
     */
    public ItemStackHelper getMapItem() {
        return getSlot(0);
    }

    /**
     * Returns the second input item. It may be paper, a map, or a glass pane; its effect depends
     * on the item and the first input map's state.
     *
     * @return the item in the second slot
     * @since 1.8.4
     */
    public ItemStackHelper getMaterial() {
        return getSlot(1);
    }

    /**
     * Returns the result slot (slot 2) as currently exposed by the menu; this wrapper does not
     * calculate a result.
     *
     * @return the result the table is currently offering
     * @since 1.8.4
     */
    public ItemStackHelper getOutput() {
        return getSlot(2);
    }

    /**
     * Returns this wrapper's fixed diagnostic label.
     *
     * @return {@code CartographyInventory:{}}
     */
    @Override
    public String toString() {
        return String.format("CartographyInventory:{}");
    }

}
