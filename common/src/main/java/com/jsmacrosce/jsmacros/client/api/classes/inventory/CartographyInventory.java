package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.doclet.DocletCategory;

/**
 * the handle for an open cartography table screen.
 * <p>
 * A cartography table is a two-in, one-out block: a map or an item that carries one goes in
 * the first slot, paper goes in the second, and the result is a map. The three methods here are
 * those three slots, in the order the table builds them, so 0 is the map,
 * {@code CartographyTableMenu.MAP_SLOT}, 1 is the material, {@code ADDITIONAL_SLOT}, and 2 is
 * the result, {@code RESULT_SLOT}. {@link #getMap()} names the same two regions
 * {@code input} and {@code output}.
 * <p>
 * Nothing here works out what a map would become; {@link #getOutput()} reads the result slot
 * as the client currently has it.
 * example:
 * <pre>
 * const inv = Player.openInventory();
 * if (inv.is("Cartography Table")) {
 *   Chat.log(`${inv.getMapItem().getItemId()} with ${inv.getMaterial().getItemId()}`);
 *   if (!inv.getOutput().isEmpty()) {
 *     Chat.log(`clones to ${inv.getOutput().getCount()} map(s)`);
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

    public CartographyInventory(CartographyTableScreen inventory) {
        super(inventory);
    }

    /**
     * the map or map item the table is working from, which is slot 0.
     * <p>
     * An empty map counts here as well as a filled one, since cloning a map is one of the
     * things the table does. Reading this never changes the result; the table reacts to the
     * slot changing.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Cartography Table")) {
     *   if (!inv.getMapItem().isEmpty()) {
     *     Chat.log(`cartographing ${inv.getMapItem().getName()}`);
     *   }
     * }
     * </pre>
     *
     * @return the map item in the first slot
     * @since 1.8.4
     */
    public ItemStackHelper getMapItem() {
        return getSlot(0);
    }

    /**
     * the material added to the map, which is slot 1.
     * <p>
     * The slot only accepts paper, another map or a glass pane, and what it means depends on
     * which: paper clones the map, a glass pane locks it, and a map in this slot recrafts the
     * first one into something else. So "the paper item" is the common case rather than the
     * only one.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Cartography Table")) {
     *   if (!inv.getMaterial().isEmpty()) {
     *     Chat.log(`adding ${inv.getMaterial().getItemId()}`);
     *   }
     * }
     * </pre>
     *
     * @return the item in the second slot
     * @since 1.8.4
     */
    public ItemStackHelper getMaterial() {
        return getSlot(1);
    }

    /**
     * the map the table is working towards, which is slot 2.
     * <p>
     * This is the result slot as the client currently has it rather than a calculation done
     * here, so it is empty unless the two inputs actually combine into something.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Cartography Table")) {
     *   const out = inv.getOutput();
     *   if (!out.isEmpty()) {
     *     Chat.log(`the table offers ${out.getName()}`);
     *   }
     * }
     * </pre>
     *
     * @return the result the table is currently offering
     * @since 1.8.4
     */
    public ItemStackHelper getOutput() {
        return getSlot(2);
    }

    @Override
    public String toString() {
        return String.format("CartographyInventory:{}");
    }

}
