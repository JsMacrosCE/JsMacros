package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.item.trading.MerchantOffer;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.access.IMerchantScreen;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.TradeOfferHelper;

import java.util.LinkedList;
import java.util.List;

/**
 * Handles an open villager trading screen. Use {@link #getTrades()} to inspect its offers and
 * {@link #selectTrade(int)} to select one.
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
     * Selects a trade by its zero-based position in {@link #getTrades()}.
     *
     * @param index the position in {@link #getTrades()}, counting from 0
     * @return this inventory for chaining
     * @since 1.3.1
     */
    public VillagerInventory selectTrade(int index) {
        ((IMerchantScreen) inventory).jsmacros_selectIndex(index);
        return this;
    }

    /**
     * Returns the trader's current experience value from the menu.
     *
     * @return the trader's current experience
     * @since 1.3.1
     */
    public int getExperience() {
        return inventory.getMenu().getTraderXp();
    }

    /**
     * Returns the trader level stored in the menu.
     *
     * @return the trader's level
     * @since 1.3.1
     */
    public int getLevelProgress() {
        return inventory.getMenu().getTraderLevel();
    }

    /**
     * Returns the experience value calculated for the current trade inputs.
     *
     * @return the calculated future experience value
     * @since 1.3.1
     */
    public int getMerchantRewardedExperience() {
        return inventory.getMenu().getFutureTraderXp();
    }

    /**
     * Returns the menu's restock flag.
     *
     * @return {@code true} if a restock is available, {@code false} otherwise
     * @since 1.3.1
     */
    public boolean canRefreshTrades() {
        return inventory.getMenu().canRestock();
    }

    /**
     * Returns whether the menu displays a progress bar.
     *
     * @return {@code true} if the level bar is being shown, {@code false} otherwise
     * @since 1.3.1
     */
    public boolean isLeveled() {
        return inventory.getMenu().showProgressBar();
    }

    /**
     * Returns helpers for the current menu offers in offer order. Each helper's index matches
     * the position accepted by {@link #selectTrade(int)}.
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

    /**
     * Returns a diagnostic label containing the value from {@link #getLevelProgress()}.
     *
     * @return {@code VillagerInventory:{"level": level}}, with {@code level} replaced by
     *         the value returned by {@link #getLevelProgress()}
     */
    @Override
    public String toString() {
        return String.format("VillagerInventory:{\"level\": %d}", getLevelProgress());
    }

}
