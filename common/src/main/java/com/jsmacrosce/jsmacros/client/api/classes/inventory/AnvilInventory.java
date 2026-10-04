package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinAnvilScreen;

/**
 * the handle for an open anvil screen.
 * <p>
 * An anvil is where an item is repaired, renamed or traded off, and it works from two input
 * slots into a single result slot, so the three methods {@link #getLeftInput()},
 * {@link #getRightInput()} and {@link #getOutput()} are most of this class. The rename box is
 * the rest of it: {@link #getName()} and {@link #setName(String)} read and write the text in
 * that box, and {@link #getLevelCost()} is what the anvil is asking to be paid for the
 * operation as it currently stands.
 * <p>
 * The slots are the menu's own, in the order the anvil builds them, so they are 0 for the
 * first input, 1 for the second input and 2 for the result, which is the same numbering as
 * the vanilla {@code AnvilMenu} uses for its own {@code INPUT_SLOT}, {@code ADDITIONAL_SLOT}
 * and {@code RESULT_SLOT} constants. {@link #getMap()} names the same two regions {@code input}
 * and {@code output}.
 * <p>
 * Nothing here recomputes the anvil. {@link #getOutput()} reads the result slot as the client
 * currently has it, which the anvil itself refreshes whenever an input or the name changes,
 * and renaming is applied by taking the result rather than by setting the name.
 * example:
 * <pre>
 * const inv = Player.openInventory();
 * if (inv.is("Anvil")) {
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

    public AnvilInventory(AnvilScreen inventory) {
        super(inventory);
    }

    /**
     * the text sitting in the anvil's rename box right now, which is the name the result item
     * would be given.
     * <p>
     * This reads the box rather than the result, so it says nothing about whether the name is
     * one the anvil will accept. An empty string means nothing has been typed. The box is made
     * non-editable while the first input slot is empty, but reading it still returns whatever
     * text is left in it, so a script that empties the slot does not necessarily read back an
     * empty string.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Anvil")) {
     *   if (inv.getLeftInput().getItemId() === "minecraft:diamond_sword") {
     *     Chat.log(`renaming to "${inv.getName()}"`);
     *   }
     * }
     * </pre>
     *
     * @return the current text of the anvil's rename box
     * @since 1.8.4
     */
    public String getName() {
        return ((MixinAnvilScreen) inventory).getNameField().getValue();
    }

    /**
     * types into the anvil's rename box, which is what the anvil reacts to.
     * <p>
     * The change is not applied here: the anvil recomputes its cost and its result as a side
     * effect of the box changing, and the renamed item only exists once the result is taken
     * out of the anvil. The box caps what it will hold at 50 characters, which is the same
     * limit the vanilla anvil validates a name against, so a longer string is cut rather than
     * rejected.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Anvil")) {
     *   if (!inv.getLeftInput().isEmpty()) {
     *     inv.setName("Sword of Testing");
     *     // the anvil has already recalculated; the name lands on the item when it is taken
     *     Chat.log(`${inv.getName()} costs ${inv.getLevelCost()} levels`);
     *   }
     * }
     * </pre>
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
     * the price of the operation the anvil is currently offering, in experience levels.
     * <p>
     * This is what taking the result would cost, so it is 0 when there is no result to take,
     * and it moves as the inputs and the rename box change rather than only when a script
     * calls something. Vanilla treats 40 as the ceiling: a rename-only cost that reaches 40 is
     * clamped to 39, and a cost of 40 or more empties the result slot, so a large number here
     * means the anvil has stopped offering a result rather than that it wants 40 levels. A
     * player with infinite materials is exempt from that second rule, so in creative mode a
     * cost of 40 or more still comes back with a result to take.
     * {@link #getMaximumLevelCost()} is the number to compare against.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Anvil")) {
     *   if (!inv.getOutput().isEmpty()) {
     *     const cost = inv.getLevelCost();
     *     if (inv.getMaximumLevelCost() > cost) {
     *       Chat.log(`affordable at ${cost} levels`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the level cost the anvil is asking for, 0 when there is nothing to take
     * @since 1.8.4
     */
    public int getLevelCost() {
        return inventory.getMenu().getCost();
    }

    /**
     * the first input item's repair cost, which is a number on the item rather than a count
     * of anything.
     * <p>
     * It is read off the item's own repair-cost data component, and is 0 when the item does not
     * carry one. The anvil uses it as an input to its level cost rather than as a quantity, so
     * a high value means repairing the item is expensive in levels, not that there are many of
     * them. Only the first input slot is looked at; the second item is not considered.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Anvil")) {
     *   Chat.log(`${inv.getLeftInput().getItemId()} has repair cost ${inv.getItemRepairCost()}`);
     * }
     * </pre>
     *
     * @return the first input item's repair cost, 0 when it carries none
     * @since 1.8.4
     */
    public int getItemRepairCost() {
        return getSlot(0).getRepairCost();
    }

    /**
     * always 40, a literal in this class rather than a reading of the anvil.
     * <p>
     * It does not change with the item, the second input or the levels the player holds, so it
     * is a ceiling to compare {@link #getLevelCost()} against rather than a number the open
     * anvil will ever report. Vanilla's own threshold is one lower in the case that matters
     * most: a rename-only cost is clamped to 39, so an anvil asking for 40 is not offering a
     * result the player can pay for.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Anvil")) {
     *   Chat.log(`${inv.getLevelCost()} of at most ${inv.getMaximumLevelCost()}`);
     * }
     * </pre>
     *
     * @return 40
     * @since 1.8.4
     */
    public int getMaximumLevelCost() {
        return 40;
    }

    /**
     * the item being repaired or renamed, which is slot 0 of the anvil.
     * <p>
     * This is the item the whole anvil is working on: it is the one the rename box follows,
     * the one {@link #getItemRepairCost()} reads, and the one whose presence decides whether
     * the box accepts typing.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Anvil")) {
     *   if (!inv.getLeftInput().isEmpty()) {
     *     Chat.log(`working on ${inv.getLeftInput().getName()}`);
     *   }
     * }
     * </pre>
     *
     * @return the first input item
     * @since 1.8.4
     */
    public ItemStackHelper getLeftInput() {
        return getSlot(0);
    }

    /**
     * the second input item, which is slot 1 of the anvil.
     * <p>
     * An anvil treats its second item as a cost rather than as a target: it is the material
     * the first item is repaired or upgraded with, and taking the result takes the anvil's
     * repair cost worth of items out of it, emptying the slot when there are not that many.
     * It is optional, so this is empty whenever the anvil is only being used to rename the
     * first item.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Anvil")) {
     *   if (!inv.getRightInput().isEmpty()) {
     *     Chat.log(`sacrificing ${inv.getRightInput().getName()}`);
     *   }
     * }
     * </pre>
     *
     * @return the second input item
     * @since 1.8.4
     */
    public ItemStackHelper getRightInput() {
        return getSlot(1);
    }

    /**
     * the item the anvil is working towards, which is slot 2 of the anvil.
     * <p>
     * This is the result slot as the client currently has it, not a recipe worked out here, so
     * it is empty whenever the two inputs do not combine. Taking it is what applies the rename
     * and spends the {@link #getLevelCost()}.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Anvil")) {
     *   const out = inv.getOutput();
     *   if (!out.isEmpty()) {
     *     Chat.log(`the anvil offers ${out.getName()}`);
     *   }
     * }
     * </pre>
     *
     * @return the result the anvil is currently offering
     * @since 1.8.4
     */
    public ItemStackHelper getOutput() {
        return getSlot(2);
    }

    @Override
    public String toString() {
        return String.format("AnvilInventory:{}");
    }

}
