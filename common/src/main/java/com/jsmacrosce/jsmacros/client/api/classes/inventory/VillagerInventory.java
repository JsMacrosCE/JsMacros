package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.item.trading.MerchantOffer;
import com.jsmacrosce.jsmacros.client.access.IMerchantScreen;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.TradeOfferHelper;

import java.util.LinkedList;
import java.util.List;

/**
 * @since 1.3.1
 */
@SuppressWarnings("unused")
public class VillagerInventory extends Inventory<MerchantScreen> {

    protected VillagerInventory(MerchantScreen inventory) {
        super(inventory);
    }

    /**
     * select the trade by its index
     *
     * @param index
     * @return self for chaining
     * @since 1.3.1
     */
    public VillagerInventory selectTrade(int index) {
        ((IMerchantScreen) inventory).jsmacros_selectIndex(index);
        return this;
    }

    /**
     * @return
     * @since 1.3.1
     */
    public int getExperience() {
        return inventory.getMenu().getTraderXp();
    }

    /**
     * @return
     * @since 1.3.1
     */
    public int getLevelProgress() {
        return inventory.getMenu().getTraderLevel();
    }

    /**
     * @return
     * @since 1.3.1
     */
    public int getMerchantRewardedExperience() {
        return inventory.getMenu().getFutureTraderXp();
    }

    /**
     * @return
     * @since 1.3.1
     */
    public boolean canRefreshTrades() {
        return inventory.getMenu().canRestock();
    }

    /**
     * @return
     * @since 1.3.1
     */
    public boolean isLeveled() {
        return inventory.getMenu().showProgressBar();
    }

    /**
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
