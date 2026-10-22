package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.item.trading.MerchantOffer;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.access.IMerchantScreen;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.TradeOfferHelper;

import java.util.LinkedList;
import java.util.List;

/**
 * the handle for an open villager trading screen.
 * <p>
 * A trading screen offers a list of trades, picks one of them, and then hands over items for it.
 * {@link #getTrades()} is the list, {@link #selectTrade(int)} is the pick, and the trade itself
 * still has to be carried out by hand: selecting only highlights it and tells the server which
 * one is on screen.
 * <p>
 * The numbers around the villager are four separate things and are easy to confuse.
 * {@link #getExperience()} is the trading experience the villager has accumulated,
 * {@link #getLevelProgress()} is its trade level, and
 * {@link #getMerchantRewardedExperience()} is what the trade currently on screen would pay out,
 * which is a reward rather than a total. {@link #isLeveled()} says whether the level is
 * currently being displayed at all, and {@link #canRefreshTrades()} whether the villager has a
 * restock coming.
 * example:
 * <pre>
 * const inv = Player.openInventory();
 * if (inv.is("Villager")) {
 *   Chat.log(`level ${inv.getLevelProgress()}, ${inv.getExperience()} xp so far`);
 *   const trades = inv.getTrades();
 *   for (let i = 0; trades.size() > i; i++) {
 *     Chat.log(`${i}: ${trades.get(i).getOutput().getItemId()}`);
 *   }
 * }
 * </pre>
 *
 * @since 1.3.1
 */
@DocletCategory("Inventory")
@SuppressWarnings("unused")
public class VillagerInventory extends Inventory<MerchantScreen> {

    protected VillagerInventory(MerchantScreen inventory) {
        super(inventory);
    }

    /**
     * highlights one of the trades by index, counting from 0 in the order
     * {@link #getTrades()} lists them.
     * <p>
     * This is a selection rather than a purchase: it sets which trade the screen is on and
     * tells the server about it, and the items still have to be put in the payment slots by
     * hand for anything to happen. An index outside the list does not fail here in any
     * particular way, it simply selects nothing, so a script that picked wrongly should check
     * {@code getTrades().size()} first.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Villager")) {
     *   const trades = inv.getTrades();
     *   if (trades.size() > 1) {
     *     inv.selectTrade(1);
     *     Chat.log(`offer ${trades.get(1).getOutput().getItemId()} selected`);
     *   }
     * }
     * </pre>
     *
     * @param index the position in {@link #getTrades()}, counting from 0
     * @return self for chaining
     * @since 1.3.1
     */
    public VillagerInventory selectTrade(int index) {
        ((IMerchantScreen) inventory).jsmacros_selectIndex(index);
        return this;
    }

    /**
     * the experience the villager has accumulated from trading, as the server last reported it.
     * <p>
     * This is a running total rather than a progress figure: it keeps climbing as trades are
     * made, and the trade level it implies is {@link #getLevelProgress()}. It arrives with the
     * offers themselves, so it describes the villager as of the last time it offered them
     * rather than continuously. It is not what the trade on screen would pay: that is
     * {@link #getMerchantRewardedExperience()}.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Villager")) {
     *   Chat.log(`${inv.getExperience()} xp, level ${inv.getLevelProgress()}`);
     * }
     * </pre>
     *
     * @return the villager's accumulated experience
     * @since 1.3.1
     */
    public int getExperience() {
        return inventory.getMenu().getTraderXp();
    }

    /**
     * the villager's trade level, which is where it is in the ladder of discount tiers.
     * <p>
     * The name is the misleading part: this is the level itself and not a progress bar towards
     * the next one. It is the trade level the server sent with the offers rather than something
     * worked out here, and it moves as the villager is levelled up.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Villager")) {
     *   Chat.log(`this villager is a level ${inv.getLevelProgress()} trader`);
     * }
     * </pre>
     *
     * @return the villager's trade level
     * @since 1.3.1
     */
    public int getLevelProgress() {
        return inventory.getMenu().getTraderLevel();
    }

    /**
     * the experience reward of the trade currently selected, not a total.
     * <p>
     * Despite the name this is what the selected trade is worth on its own, and not what the
     * villager's experience will add up to afterwards. It is 0 when there is no usable trade
     * selected, and it follows the selection, so calling it after
     * {@link #selectTrade(int)} is what reads a different trade's reward. Compare it with
     * {@link #getExperience()} to see how far along the villager is.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Villager")) {
     *   const trades = inv.getTrades();
     *   inv.selectTrade(0);
     *   Chat.log(`trade 0 pays ${inv.getMerchantRewardedExperience()} xp`);
     * }
     * </pre>
     *
     * @return the experience the selected trade rewards
     * @since 1.3.1
     */
    public int getMerchantRewardedExperience() {
        return inventory.getMenu().getFutureTraderXp();
    }

    /**
     * whether the villager has a restock of trades waiting.
     * <p>
     * This is the flag the server sent with the offers rather than something worked out
     * here, so it says whether the villager is due a new batch of trades. It says nothing
     * about whether the trades currently on offer can be used, and it does not change the
     * list; the list moves on its own when a restock arrives.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Villager")) {
     *   if (inv.canRefreshTrades()) {
     *     Chat.log("this villager is due a restock");
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if a restock is available, {@code false} otherwise
     * @since 1.3.1
     */
    public boolean canRefreshTrades() {
        return inventory.getMenu().canRestock();
    }

    /**
     * whether the trading screen is currently showing a level bar.
     * <p>
     * It is about the display rather than about the villager: a villager always has a level, and
     * this is whether the screen has chosen to draw the bar for it. So {@code false} does not
     * mean an unlevelled villager, and {@link #getLevelProgress()} is the number to read for
     * the level itself.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Villager")) {
     *   if (!inv.isLeveled()) {
     *     Chat.log(`no level bar, but the villager is level ${inv.getLevelProgress()}`);
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the level bar is being shown, {@code false} otherwise
     * @since 1.3.1
     */
    public boolean isLeveled() {
        return inventory.getMenu().showProgressBar();
    }

    /**
     * the trades on offer, in the order the screen lists them.
     * <p>
     * Each entry carries the position {@link #selectTrade(int)} expects, so a script can walk
     * the list once, keep the interesting offers and select them by the same number without
     * having to match the two up afterwards. The list is rebuilt on every call from the
     * screen's current offers, so it follows a restock.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Villager")) {
     *   const trades = inv.getTrades();
     *   for (let i = 0; trades.size() > i; i++) {
     *     const inputs = trades.get(i).getInput();
     *     Chat.log(`trade ${trades.get(i).getIndex()}: ${inputs.size()} input(s) for ${trades.get(i).getOutput().getItemId()}`);
     *   }
     * }
     * </pre>
     *
     * @return list of trade offers
     * @since 1.3.1
     */
    public List<TradeOfferHelper> getTrades() {
        List<TradeOfferHelper> offers = new LinkedList<>();
        int i = -1;
        for (MerchantOffer offer : inventory.getMenu().getOffers()) {
            offers.add(new TradeOfferHelper(offer, ++i, this));
        }
        return offers;
    }

    @Override
    public String toString() {
        return String.format("VillagerInventory:{\"level\": %d}", getLevelProgress());
    }

}
