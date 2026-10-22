package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

/**
 * the handle for an open smithing table screen.
 * <p>
 * A smithing table has three inputs rather than two, and the order matters more here than on
 * most containers because none of these three slots is the result. The menu builds them as the
 * template, the base item and then the addition material, with the smithed result after all
 * three, so the slots are 0 template, 1 base item, 2 addition and 3 result. That is the same
 * numbering as the vanilla {@code SmithingMenu} uses for its own {@code TEMPLATE_SLOT},
 * {@code BASE_SLOT}, {@code ADDITIONAL_SLOT} and {@code RESULT_SLOT} constants.
 * <p>
 * The consequence is that the two methods named after inputs and the one named after the output
 * do not line up with those roles: {@link #getLeftInput()} is the template,
 * {@link #getRightInput()} is the base item and {@link #getOutput()} is the addition. The
 * smithed result is slot 3, and it is read with {@code getSlot(3)} rather than through a
 * method here, because {@link #getMap()} places it under {@code output}.
 * example:
 * <pre>
 * const inv = Player.openInventory();
 * if (inv.is("Smithing Table")) {
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

    public SmithingInventory(SmithingScreen inventory) {
        super(inventory);
    }

    /**
     * the template item, which is slot 0.
     * <p>
     * The template says what kind of smithing is being done and is consumed with the rest, so
     * it is a required input rather than a choice. It is the first slot the menu builds and so
     * the lowest slot number on the screen.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Smithing Table")) {
     *   if (!inv.getLeftInput().isEmpty()) {
     *     Chat.log(`template ${inv.getLeftInput().getItemId()}`);
     *   }
     * }
     * </pre>
     *
     * @return the template item in the first slot.
     * @since 1.8.4
     */
    public ItemStackHelper getLeftInput() {
        return getSlot(0);
    }

    /**
     * the base item being smithed, which is slot 1.
     * <p>
     * This is the item the other two work on: the template says how to change it and the
     * addition says what to change it with, so it is the one that ends up in the result. It is
     * a separate slot rather than the template's own stack because a smithing table always has
     * all three, even while only one of them is filled.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Smithing Table")) {
     *   if (!inv.getRightInput().isEmpty()) {
     *     Chat.log(`smithing ${inv.getRightInput().getName()}`);
     *   }
     * }
     * </pre>
     *
     * @return the base item in the second slot.
     * @since 1.8.4
     */
    public ItemStackHelper getRightInput() {
        return getSlot(1);
    }

    /**
     * the addition material, which is slot 2, and not the smithed result.
     * <p>
     * This is the third input, the material the base item is combined with, so it goes in rather
     * than coming out. Despite the name it is not the output: the smithed result is slot 3,
     * which is what {@link #getMap()} calls {@code output}, and which a script has to read as
     * {@code getSlot(3)}.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Smithing Table")) {
     *   if (!inv.getOutput().isEmpty()) {
     *     Chat.log(`adding ${inv.getOutput().getItemId()} to the base item`);
     *   }
     * }
     * </pre>
     *
     * @return the addition item in the third slot.
     * @since 1.8.4
     */
    public ItemStackHelper getOutput() {
        return getSlot(2);
    }

    @Override
    public String toString() {
        return String.format("SmithingInventory:{}");
    }

}
