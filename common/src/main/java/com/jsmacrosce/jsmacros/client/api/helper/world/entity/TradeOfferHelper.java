package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory;
import com.jsmacrosce.jsmacros.client.api.helper.NBTElementHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a merchant trade, including its inputs, output, and use state.
 * <p>
 * The first input is required and may be price-adjusted; the second is optional and
 * remains at its original count. {@link #getInput()} includes the adjusted first
 * input and any non-empty second input, while the individual input accessors return
 * unadjusted stacks. Price adjustments affect only the first input.
 * <p>
 * Trades read from an inventory screen can be selected with {@link #select()} while
 * that screen remains open; trades read from an entity cannot be selected.
 * <pre>
 * <pre>
 * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
 * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
 * const inv = Inventory.create(Hud.getOpenScreen());
 * if (inv !== null) {
 *   if (inv.is("Villager")) {
 *     for (const trade of VillagerInventory.class.cast(inv).getTrades()) {
 *       // the first input is what it costs now, the second may be nothing
 *       const inputs = trade.getInput();
 *       const parts = [];
 *       for (let i = 0; i !== inputs.size(); i += 1) {
 *         parts.push(inputs.get(i).getName().getString());
 *       }
 *       Chat.log(`#${trade.getIndex()} ${parts.join(" and ")} `
 *         + `for ${trade.getOutput().getName().getString()}`);
 *     }
 *   }
 * }
 * </pre>
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class TradeOfferHelper extends BaseHelper<MerchantOffer> {
    private final VillagerInventory inv;
    private final int index;

    /**
     * Wraps a merchant offer and stores the inventory context used by {@link #select()}.
     * This does not select or look up the offer, or copy, validate, register, or execute a trade.
     *
     * @param base raw merchant offer to wrap
     * @param index zero-based slot index used when selecting from the supplied inventory; not validated
     * @param inv inventory used by {@link #select()}, or {@code null} when there is no inventory selection context
     */
    public TradeOfferHelper(MerchantOffer base, int index, VillagerInventory inv) {
        super(base);
        this.inv = inv;
        this.index = index;
    }

    /**
     * Returns a new list containing the adjusted first input and, when present, the
     * non-empty second input. The second input is not price-adjusted.
     *
     * @return list of input items required
     */
    public List<ItemStackHelper> getInput() {
        List<ItemStackHelper> items = new ArrayList<>();
        ItemStack first = base.getCostA();
        if (!first.isEmpty()) {
            items.add(new ItemStackHelper(first));
        }
        ItemStack second = base.getCostB();
        if (second != null && !second.isEmpty()) {
            items.add(new ItemStackHelper(second));
        }
        return items;
    }

    /**
     * Returns the first input stack at its original, unadjusted count.
     *
     * @return the first input item.
     * @since 1.8.4
     */
    public ItemStackHelper getLeftInput() {
        return new ItemStackHelper(base.getItemCostA().itemStack());
    }

    /**
     * Returns the optional second input at its original count, or an empty stack if
     * the trade has no second input.
     *
     * @return the second input item.
     * @since 1.8.4
     */
    public ItemStackHelper getRightInput() {
        if (base.getItemCostB().isEmpty()) {
            return new ItemStackHelper(ItemStack.EMPTY);
        }
        return new ItemStackHelper(base.getItemCostB().get().itemStack());
    }

    /**
     * Returns the trade's output stack, with its configured count.
     *
     * @return output item that will be received
     */
    public ItemStackHelper getOutput() {
        return new ItemStackHelper(base.getResult());
    }

    /**
     * The screen inventory assigns indices from zero, matching the position accepted by
     * {@link VillagerInventory#selectTrade(int)}. Trades read from a merchant entity
     * instead have index zero; they have no screen to select on.
     *
     * @return the zero-based index in the screen's offer list, or zero for an entity-read trade.
     * @since 1.8.4
     */
    public int getIndex() {
        return index;
    }

    /**
     * Selects this trade in the screen it was read from, but only while that screen is
     * still open. Entity-read trades, or trades from a closed screen, are unchanged.
     *
     * @return self for chaining.
     */
    public TradeOfferHelper select() {
        if (inv != null && Minecraft.getInstance().screen == inv.getRawContainer()) {
            inv.selectTrade(index);
        }
        return this;
    }

    /**
     * Returns whether the offer is available under its use limit.
     *
     * @return {@code true} if the trade has uses left, {@code false} otherwise.
     */
    public boolean isAvailable() {
        return !base.isOutOfStock();
    }

    /**
     * Encodes the offer with Minecraft's merchant-offer codec and wraps the resulting
     * compound. Each call encodes the current offer state.
     *
     * @return trade offer as nbt tag
     */
    @DocletReplaceReturn("NBTElementHelper$NBTCompoundHelper")
    public NBTElementHelper<?> getNBT() {
        return NBTElementHelper.wrap(MerchantOffer.CODEC.encodeStart(RegistryHelper.getNbtOps(), base).getOrThrow());
    }

    /**
     * Returns the number of times this offer has been used.
     *
     * @return current number of uses
     */
    public int getUses() {
        return base.getUses();
    }

    /**
     * Returns the use limit for this offer; {@link #getUses()} counts uses against it.
     *
     * @return max uses before it locks
     */
    public int getMaxUses() {
        return base.getMaxUses();
    }

    /**
     * Returns the offer's flag for rewarding experience after a successful trade.
     *
     * @return {@code true} if the offer is flagged to reward experience; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean shouldRewardPlayerExperience() {
        return base.shouldRewardExp();
    }

    /**
     * Returns the experience amount stored on the offer. Whether the offer is flagged
     * to reward experience is reported separately by {@link #shouldRewardPlayerExperience()}.
     *
     * @return experience gained for trade
     */
    public int getExperience() {
        return base.getXp();
    }

    /**
     * Returns the difference between the adjusted and original counts of the first
     * input. Positive values increase the count; negative values decrease it.
     *
     * @return current price adjustment, negative is discount.
     */
    public int getCurrentPriceAdjustment() {
        return base.getCostA().getCount() - base.getBaseCostA().getCount();
    }

    /**
     * Returns the first input stack at its original, unadjusted count.
     *
     * @return the original first input stack.
     * @since 1.8.4
     */
    public ItemStackHelper getOriginalFirstInput() {
        return new ItemStackHelper(base.getBaseCostA());
    }

    /**
     * Returns the original count of the first input, before price adjustment.
     *
     * @return the original first-input count.
     * @since 1.8.4
     */
    public int getOriginalPrice() {
        return base.getBaseCostA().getCount();
    }

    /**
     * Returns the count of the first input after demand and special-price adjustments,
     * clamped to the range from one to that item's maximum stack size. {@link #getLeftInput()}
     * returns the original count.
     *
     * @return the adjusted first-input count.
     * @since 1.8.4
     */
    public int getAdjustedPrice() {
        return base.getCostA().getCount();
    }

    /**
     * Returns the price difference attributed to special pricing, in items added to or
     * subtracted from the first input. Negative values reduce the price; positive values
     * increase it. The final price also includes the non-negative demand contribution and
     * is clamped to the item's stack-size range.
     *
     * @return the special price difference in items.
     * @since 1.8.4
     */
    public int getSpecialPrice() {
        return base.getSpecialPriceDiff();
    }

    /**
     * Returns the per-offer factor used to calculate the demand contribution to its first
     * input's price. The contribution is the greater of zero and the floored product of
     * the original count, demand, and this multiplier.
     *
     * @return the demand price multiplier for this offer.
     * @since 1.8.4
     */
    public float getPriceMultiplier() {
        return base.getPriceMultiplier();
    }

    /**
     * Returns this offer's demand value. Its contribution to the adjusted first-input
     * count is {@code max(0, floor(originalCount * demand * priceMultiplier))}; a negative
     * demand therefore contributes zero rather than reducing the base count. The total
     * price change also includes the special price difference and the final stack-size clamp.
     *
     * @return the demand value used to calculate this offer's price.
     * @since 1.8.4
     */
    public int getDemandBonus() {
        return base.getDemand();
    }

    /**
     * Returns a diagnostic representation built from the offer's input and output helpers.
     * This is not a full serialization of the offer, its pricing algorithm, or its complete state.
     *
     * @return diagnostic representation of the trade inputs and output
     */
    @Override
    public String toString() {
        return String.format("TradeOfferHelper:{\"inputs\": %s, \"output\": %s}", getInput().toString(), getOutput().toString());
    }

}
