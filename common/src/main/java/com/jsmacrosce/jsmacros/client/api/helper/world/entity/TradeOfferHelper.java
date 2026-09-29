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
 * one trade on offer: what the merchant wants, what it gives, and how many times it can
 * still be done.
 * <p>
 * A trade has two inputs and one output, but only one of the two inputs is always
 * there. The first is required and the second is optional, which is why
 * {@link #getInput()} is a list rather than a pair and why
 * {@link #getRightInput()} can hand back an empty stack. A one-input trade gives
 * {@code 1} from {@link #getInput()} and a two-input one gives {@code 2}.
 * <p>
 * There are two price readings and they are not the same number.
 * {@link #getOriginalPrice()} is what the trade costs before anything is applied to it,
 * and {@link #getAdjustedPrice()} is what it actually costs now, once demand and the
 * player's standing with the merchant have had their say. Both of those are readings of
 * the <b>first</b> input, and the first is the only input the game ever adjusts: the
 * second is taken at the price the trade was set up at, and no amount of demand moves
 * it. So the input readers do not all agree on this and each is worth checking on its
 * own: {@link #getInput()} mixes the two, putting the adjusted first input and the
 * unadjusted second one in the same list, while {@link #getLeftInput()} and
 * {@link #getRightInput()} both hand back unadjusted stacks and
 * {@link #getOriginalFirstInput()} gives the same stack as {@link #getLeftInput()}.
 * <p>
 * A trade also comes with the screen it was read from, or without one. Only a trade
 * that came from an open trading screen can be selected with {@link #select()}, because
 * that is the only case where there is a screen to select it on.
 * example:
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

    public TradeOfferHelper(MerchantOffer base, int index, VillagerInventory inv) {
        super(base);
        this.inv = inv;
        this.index = index;
    }

    /**
     * the items the merchant wants, as a list built at the moment of the call.
     * <p>
     * The first input is always in the list and the second only when the trade has one,
     * so the length is one for a one-input trade and two for a two-input one, and there
     * is no empty result. The two entries come through different routes and do not
     * agree about adjustment: the first is the price as it stands now, so a trade whose
     * demand has gone up asks for more here than {@link #getOriginalFirstInput()}
     * reports, while the second is the price the trade was set up at and nothing moves
     * it, so for that one {@link #getOriginalFirstInput()} is the wrong thing to measure
     * against and {@link #getRightInput()} hands back the same stack.
     * <p>
     * A new list is built each call, so writing to it changes nothing.
     * example:
     * <pre>
     * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
     * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
     * const inv = Inventory.create(Hud.getOpenScreen());
     * if (inv !== null) {
     *   if (inv.is("Villager")) {
     *     for (const trade of VillagerInventory.class.cast(inv).getTrades()) {
     *       const inputs = trade.getInput();
     *       Chat.log(`#${trade.getIndex()} wants ${inputs.size()} input(s)`);
     *       for (let i = 0; i !== inputs.size(); i += 1) {
     *         Chat.log(`  ${inputs.get(i).getCount()} ${inputs.get(i).getItemId()}`);
     *       }
     *     }
     *   }
     * }
     * </pre>
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
     * the first input, which is the item the merchant always wants. It is the required
     * one, so there is always something here and it is never an empty stack.
     * <p>
     * This is the price as the trade was set up rather than as it stands now: it is the
     * unadjusted stack, so a trade whose demand has gone up asks for more through
     * {@link #getAdjustedPrice()} and through {@link #getInput()} than it does here. The
     * difference between the two is what {@link #getCurrentPriceAdjustment()} reports.
     * example:
     * <pre>
     * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
     * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
     * const inv = Inventory.create(Hud.getOpenScreen());
     * if (inv !== null) {
     *   if (inv.is("Villager")) {
     *     for (const trade of VillagerInventory.class.cast(inv).getTrades()) {
     *       // the set-up price and what it costs now
     *       const left = trade.getLeftInput();
     *       Chat.log(`#${trade.getIndex()} was ${left.getCount()} `
     *         + `and now wants ${trade.getAdjustedPrice()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the first input item.
     * @since 1.8.4
     */
    public ItemStackHelper getLeftInput() {
        return new ItemStackHelper(base.getItemCostA().itemStack());
    }

    /**
     * the second input, which is the optional one. A trade that only wants one thing has
     * no second input, and this is an empty stack when it does not, which
     * {@code isEmpty()} on the result answers.
     * <p>
     * Unlike the first input this is <b>not</b> the price as it stands now: it is the
     * price the trade was set up at, and nothing moves it, because the game applies
     * demand, the player's standing and the stack cap to the first input only. So
     * {@link #getLeftInput()} is an unadjusted stack for the same reason this one is,
     * and a trade whose demand has gone up asks for exactly as much here as the
     * merchant originally wanted: this is never the entry that says how much harder the
     * trade has got, and {@link #getAdjustedPrice()} never covers it.
     * example:
     * <pre>
     * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
     * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
     * const inv = Inventory.create(Hud.getOpenScreen());
     * if (inv !== null) {
     *   if (inv.is("Villager")) {
     *     for (const trade of VillagerInventory.class.cast(inv).getTrades()) {
     *       const right = trade.getRightInput();
     *       if (right.isEmpty()) {
     *         Chat.log(`#${trade.getIndex()} only wants one input`);
     *       } else {
     *         Chat.log(`#${trade.getIndex()} also wants ${right.getName()}`);
     *       }
     *     }
     *   }
     * }
     * </pre>
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
     * what the merchant hands over, as a stack with the count it gives. The count is the
     * one the trade was set up with, so a trade giving three of something reports three
     * rather than one.
     * <p>
     * This is the trade's stored result rather than an assembly of it, so it is what
     * the trade is worth and not what a particular payment would produce.
     * example:
     * <pre>
     * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
     * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
     * const inv = Inventory.create(Hud.getOpenScreen());
     * if (inv !== null) {
     *   if (inv.is("Villager")) {
     *     for (const trade of VillagerInventory.class.cast(inv).getTrades()) {
     *       const out = trade.getOutput();
     *       Chat.log(`#${trade.getIndex()} gives ${out.getCount()} ${out.getName()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return output item that will be received
     */
    public ItemStackHelper getOutput() {
        return new ItemStackHelper(base.getResult());
    }

    /**
     * where this trade sits in the list it was read from, counted from one. A trade read
     * off an open trading screen is numbered from one, matching the row it is drawn on.
     * <p>
     * The number is what {@link #select()} acts on, so it is the position rather than
     * anything about the trade itself. A trade that was read off the merchant entity
     * rather than off a screen is given the number zero by that reader, and selecting it
     * does nothing for the same reason selecting any of them would. On the client that
     * reader throws before it hands a list back at all, so the zero is a figure a server
     * sees and a client script only ever sees trades numbered off a screen.
     * example:
     * <pre>
     * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
     * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
     * const inv = Inventory.create(Hud.getOpenScreen());
     * if (inv !== null) {
     *   if (inv.is("Villager")) {
     *     for (const trade of VillagerInventory.class.cast(inv).getTrades()) {
     *       // counted from one, which is the row the trade is drawn on
     *       Chat.log(`row ${trade.getIndex()}: ${trade.getOutput().getName()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the index if this trade in the given villager inventory.
     * @since 1.8.4
     */
    public int getIndex() {
        return index;
    }

    /**
     * selects this trade on the trading screen, which is the game's own selection rather
     * than a script writing a flag: the screen redraws with the row highlighted and the
     * result slot ready.
     * <p>
     * It does nothing at all unless both of two things hold. The trade has to have come
     * from a screen, and that screen has to be the one that is open now. A trade read
     * off the merchant entity rather than off a trading screen has no screen behind it,
     * so a select on one is quietly refused; so is one whose screen has since been
     * closed. Nothing is thrown either way and the return value is the same trade, so a
     * caller cannot tell from this whether the selection happened.
     * example:
     * <pre>
     * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
     * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
     * const inv = Inventory.create(Hud.getOpenScreen());
     * if (inv !== null) {
     *   if (inv.is("Villager")) {
     *     const trades = VillagerInventory.class.cast(inv).getTrades();
     *     if (trades.size() > 0) {
     *       // the last row, which the screen now shows as selected
     *       trades.get(trades.size() - 1).select();
     *     }
     *   }
     * }
     * </pre>
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
     * whether the trade can still be done, which is {@code false} once it has been used
     * as many times as it allows. A trade that has run out of uses is the same thing the
     * game calls out of stock, and a merchant restocks it in its own time.
     * <p>
     * A trade that is out of stock is still on the list rather than gone from it, so this
     * is how a script tells the ones worth looking at from the ones that are not.
     * example:
     * <pre>
     * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
     * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
     * const inv = Inventory.create(Hud.getOpenScreen());
     * if (inv !== null) {
     *   if (inv.is("Villager")) {
     *     for (const trade of VillagerInventory.class.cast(inv).getTrades()) {
     *       if (trade.isAvailable()) {
     *         Chat.log(`#${trade.getIndex()} still on offer`);
     *       } else {
     *         Chat.log(`#${trade.getIndex()} is used up, `
     *           + `${trade.getUses()} of ${trade.getMaxUses()}`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the trade has uses left, {@code false} otherwise.
     */
    public boolean isAvailable() {
        return !base.isOutOfStock();
    }

    /**
     * the trade as a piece of NBT, which is the whole of it in the form the game saves
     * and sends: both costs, the result, the use count and the limit, whether the
     * merchant learns from it, the special price difference, the demand, the price
     * multiplier and the experience. It is a compound rather than a single value, so it
     * comes back as one of the compound helpers.
     * <p>
     * The trade is re-encoded on each call, so this is a fresh reading rather than a
     * view of the live trade. Nothing here changes the trade either way.
     * example:
     * <pre>
     * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
     * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
     * const inv = Inventory.create(Hud.getOpenScreen());
     * if (inv !== null) {
     *   if (inv.is("Villager")) {
     *     const trades = VillagerInventory.class.cast(inv).getTrades();
     *     if (trades.size() > 0) {
     *       // the whole trade, as the game would write it out
     *       Chat.log(trades.get(0).getNBT().toString());
     *     }
     *   }
     * }
     * </pre>
     *
     * @return trade offer as nbt tag
     */
    @DocletReplaceReturn("NBTElementHelper$NBTCompoundHelper")
    public NBTElementHelper<?> getNBT() {
        return NBTElementHelper.wrap(MerchantOffer.CODEC.encodeStart(RegistryHelper.getNbtOps(), base).getOrThrow());
    }

    /**
     * how many times this trade has been done, counted up towards
     * {@link #getMaxUses()}. It is at zero for a trade that has not been used and at the
     * maximum for one that is used up, which is the same point
     * {@link #isAvailable()} reports {@code false}.
     * <p>
     * The number only moves when a trade is actually made, so a trade that is on offer
     * but not taken stays where it is.
     * example:
     * <pre>
     * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
     * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
     * const inv = Inventory.create(Hud.getOpenScreen());
     * if (inv !== null) {
     *   if (inv.is("Villager")) {
     *     for (const trade of VillagerInventory.class.cast(inv).getTrades()) {
     *       const used = trade.getUses();
     *       const max = trade.getMaxUses();
     *       Chat.log(`#${trade.getIndex()} used ${used} of ${max}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return current number of uses
     */
    public int getUses() {
        return base.getUses();
    }

    /**
     * how many times this trade can be done before it locks, which is the number
     * {@link #getUses()} is counted towards. A trade that allows one use can be done
     * once and is out of stock after that, rather than being out of stock from the
     * start.
     * <p>
     * The figure comes from the trade rather than from the merchant, so two trades on
     * the same merchant can have different limits.
     * example:
     * <pre>
     * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
     * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
     * const inv = Inventory.create(Hud.getOpenScreen());
     * if (inv !== null) {
     *   if (inv.is("Villager")) {
     *     for (const trade of VillagerInventory.class.cast(inv).getTrades()) {
     *       // a trade that locks after one use is available once and not again
     *       Chat.log(`#${trade.getIndex()} allows ${trade.getMaxUses()} use(s)`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return max uses before it locks
     */
    public int getMaxUses() {
        return base.getMaxUses();
    }

    /**
     * {@code true} if after a successful trade xp will be summoned, {@code false}
     * otherwise.
     * <p>
     * This is a flag on the trade rather than a check of the merchant, and it is not the
     * same as whether the trade gives experience: {@link #getExperience()} is how much a
     * trade is worth and this says whether the merchant learns from it. Some trades give
     * experience without rewarding the player and some do the other way round, so the
     * two are read separately.
     * example:
     * <pre>
     * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
     * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
     * const inv = Inventory.create(Hud.getOpenScreen());
     * if (inv !== null) {
     *   if (inv.is("Villager")) {
     *     for (const trade of VillagerInventory.class.cast(inv).getTrades()) {
     *       if (trade.shouldRewardPlayerExperience()) {
     *         Chat.log(`#${trade.getIndex()} teaches the merchant, `
     *           + `worth ${trade.getExperience()} xp`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if after a successful trade xp will be summoned, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean shouldRewardPlayerExperience() {
        return base.shouldRewardExp();
    }

    /**
     * how much experience the trade is worth, which is what the merchant banks when it
     * is done and what eventually pushes the merchant up a level. It is a figure on the
     * trade rather than a running total, and it is what the merchant gains however much
     * the player pays.
     * <p>
     * Whether the merchant learns from the trade at all is a separate flag, which
     * {@link #shouldRewardPlayerExperience()} reports.
     * example:
     * <pre>
     * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
     * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
     * const inv = Inventory.create(Hud.getOpenScreen());
     * if (inv !== null) {
     *   if (inv.is("Villager")) {
     *     for (const trade of VillagerInventory.class.cast(inv).getTrades()) {
     *       Chat.log(`#${trade.getIndex()} is worth ${trade.getExperience()} xp`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return experience gained for trade
     */
    public int getExperience() {
        return base.getXp();
    }

    /**
     * how far the price has moved off the set-up price, as a number of items, and a
     * negative one is a discount. It is worked out by taking the current first input
     * count away from the original, so a positive number means the trade is asking for
     * more than it was set up at and a negative one means less.
     * <p>
     * It covers everything that has moved the price, not demand on its own. Demand,
     * the player's standing with the merchant and the cap on how large a stack can be
     * all end up in the current count, so this is the total movement rather than one
     * contribution to it. The demand on its own is {@link #getDemandBonus()} and the
     * player's standing is {@link #getSpecialPrice()}.
     * example:
     * <pre>
     * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
     * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
     * const inv = Inventory.create(Hud.getOpenScreen());
     * if (inv !== null) {
     *   if (inv.is("Villager")) {
     *     for (const trade of VillagerInventory.class.cast(inv).getTrades()) {
     *       const moved = trade.getCurrentPriceAdjustment();
     *       if (moved > 0) {
     *         Chat.log(`#${trade.getIndex()} costs ${moved} more than it started at`);
     *       } else {
     *         if (0 > moved) {
     *           Chat.log(`#${trade.getIndex()} is ${-moved} cheaper than it started at`);
     *         }
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return current price adjustment, negative is discount.
     */
    public int getCurrentPriceAdjustment() {
        return base.getCostA().getCount() - base.getBaseCostA().getCount();
    }

    /**
     * the first input at the price the trade was set up at, which is what the merchant
     * originally asked for before demand, the player's standing or the stack cap moved
     * it. It is the same figure {@link #getOriginalPrice()} gives as a bare number, and
     * the same stack {@link #getLeftInput()} hands back, since that is the unadjusted
     * one too. What this is not is {@link #getAdjustedPrice()}, which is this same
     * input read after the adjustments.
     * example:
     * <pre>
     * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
     * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
     * const inv = Inventory.create(Hud.getOpenScreen());
     * if (inv !== null) {
     *   if (inv.is("Villager")) {
     *     for (const trade of VillagerInventory.class.cast(inv).getTrades()) {
     *       const original = trade.getOriginalFirstInput();
     *       Chat.log(`#${trade.getIndex()} wanted ${original.getCount()} `
     *         + `${original.getItemId()} and now wants ${trade.getAdjustedPrice()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the original priced item without any adjustments due to rewards or demand.
     * @since 1.8.4
     */
    public ItemStackHelper getOriginalFirstInput() {
        return new ItemStackHelper(base.getBaseCostA());
    }

    /**
     * the original price of the item without any adjustments due to rewards or demand,
     * as a count rather than as a stack. It is the same number as the count on
     * {@link #getOriginalFirstInput()}, and the figure every adjustment is measured
     * against.
     * example:
     * <pre>
     * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
     * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
     * const inv = Inventory.create(Hud.getOpenScreen());
     * if (inv !== null) {
     *   if (inv.is("Villager")) {
     *     for (const trade of VillagerInventory.class.cast(inv).getTrades()) {
     *       Chat.log(`#${trade.getIndex()} started at ${trade.getOriginalPrice()} `
     *         + `and is now ${trade.getAdjustedPrice()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the original price of the item without any adjustments due to rewards or demand.
     * @since 1.8.4
     */
    public int getOriginalPrice() {
        return base.getBaseCostA().getCount();
    }

    /**
     * the adjusted price of the item, as a count rather than as a stack: what the first
     * input actually costs now, once demand, the player's standing with the merchant and
     * the cap on stack size have all been applied to it.
     * <p>
     * The game clamps it at both ends: never below one however large a discount is, and
     * never above the largest stack of the item, so a trade with an extreme price lands
     * on one of those two rather than going past it. This is the figure
     * {@link #getLeftInput()} is not, since that gives the unadjusted stack.
     * example:
     * <pre>
     * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
     * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
     * const inv = Inventory.create(Hud.getOpenScreen());
     * if (inv !== null) {
     *   if (inv.is("Villager")) {
     *     for (const trade of VillagerInventory.class.cast(inv).getTrades()) {
     *       // clamped at one below and at a full stack above
     *       Chat.log(`#${trade.getIndex()} now costs ${trade.getAdjustedPrice()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the adjusted price of the item.
     * @since 1.8.4
     */
    public int getAdjustedPrice() {
        return base.getCostA().getCount();
    }

    /**
     * the number of items the player's standing with this merchant has added to or taken
     * off the first input, and it is a difference rather than a price.
     * <p>
     * A negative value is a discount and means that the player has a good reputation with the
     * villager, while a positive value is a premium. Hero of the village will always affect and
     * reduce this value.
     * <p>
     * It is this figure and the demand bonus that together make up
     * {@link #getCurrentPriceAdjustment()}, and the price itself comes out through
     * {@link #getAdjustedPrice()}. A large negative value here does not drive the price
     * below one, because the price is floored there whatever this says.
     * example:
     * <pre>
     * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
     * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
     * const inv = Inventory.create(Hud.getOpenScreen());
     * if (inv !== null) {
     *   if (inv.is("Villager")) {
     *     for (const trade of VillagerInventory.class.cast(inv).getTrades()) {
     *       const special = trade.getSpecialPrice();
     *       if (0 > special) {
     *         Chat.log(`#${trade.getIndex()} is ${-special} cheaper for a good reputation`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the special price multiplier, which affects the price of the item depending on the
     * player's reputation with the villager.
     * @since 1.8.4
     */
    public int getSpecialPrice() {
        return base.getSpecialPriceDiff();
    }

    /**
     * how sharply the price of this trade responds to demand, as a fraction rather than
     * as a number of items. The higher it is the more the price moves, and it is
     * applied to the base price multiplied by the demand and by this number together,
     * then rounded down: a demand of ten adds a whole base price at a multiplier of
     * point one, and five times the base price at point five, while a demand of one at
     * point zero five adds a twentieth of it.
     * <p>
     * A higher price multiplier means that the price of these trades can vary much more than normal
     * ones. The default value is 0.05 and 0.2 for armor and tools.
     * <p>
     * It is a property of the trade rather than of the merchant, so two trades on the
     * same merchant can have different ones, and it is the figure the demand bonus is
     * applied through.
     * example:
     * <pre>
     * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
     * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
     * const inv = Inventory.create(Hud.getOpenScreen());
     * if (inv !== null) {
     *   if (inv.is("Villager")) {
     *     for (const trade of VillagerInventory.class.cast(inv).getTrades()) {
     *       Chat.log(`#${trade.getIndex()} multiplier ${trade.getPriceMultiplier()}, `
     *         + `demand ${trade.getDemandBonus()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the price multiplier, which is only depended on the type of trade.
     * @since 1.8.4
     */
    public float getPriceMultiplier() {
        return base.getPriceMultiplier();
    }

    /**
     * how much of the base price the demand is adding, as a count rather than as a
     * fraction. The demand is globally applied to all trades of this type for all villagers and
     * players, so one busy merchant moves the price for every merchant offering the same thing.
     * <p>
     * The demand is only calculated and updated on restock. A villager only restocks at
     * its job site once at least one of its offers has been used, so a villager nobody
     * has traded with keeps the demand it was built with. Updating the demand is done with
     * the following formula:
     * <pre> {@code demand = demand + 2 * uses - maxUses} </pre>
     * <p> Thus trading only half of the max uses will not increase the demand.
     * <p>
     * The value itself is not held at zero. What is held at zero is the effect it has:
     * the price works out the demand contribution and takes the larger of that and
     * nothing, so a negative demand never brings a price below its base. The figure
     * here can therefore go negative on a trade that has been used less than half its
     * limit, while the price stays where it started.
     * <p>
     * What it is applied through is {@link #getPriceMultiplier()}, and the total
     * movement in the price is {@link #getCurrentPriceAdjustment()}.
     * example:
     * <pre>
     * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
     * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
     * const inv = Inventory.create(Hud.getOpenScreen());
     * if (inv !== null) {
     *   if (inv.is("Villager")) {
     *     for (const trade of VillagerInventory.class.cast(inv).getTrades()) {
     *       const demand = trade.getDemandBonus();
     *       if (demand > 0) {
     *         Chat.log(`#${trade.getIndex()} is in demand, `
     *           + `${trade.getOriginalPrice()} up to ${trade.getAdjustedPrice()}`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the demand bonus for this trade.
     * @since 1.8.4
     */
    public int getDemandBonus() {
        return base.getDemand();
    }

    @Override
    public String toString() {
        return String.format("TradeOfferHelper:{\"inputs\": %s, \"output\": %s}", getInput().toString(), getOutput().toString());
    }

}
