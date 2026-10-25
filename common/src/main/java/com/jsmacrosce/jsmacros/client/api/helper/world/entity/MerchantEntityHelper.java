package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import com.jsmacrosce.doclet.DocletCategory;
//? if >=1.21.11 {
/*import net.minecraft.world.entity.npc.villager.AbstractVillager;
*///? } else {
import net.minecraft.world.entity.npc.AbstractVillager;
//? }
import net.minecraft.world.item.trading.MerchantOffer;
import com.jsmacrosce.jsmacros.client.access.IMerchantEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers for villagers and wandering traders, which share the game's merchant base class.
 * Use {@code EntityHelpers.Merchant} to match either kind, or {@code EntityHelpers.Villager}
 * for villagers alone.
 * <p>
 * {@link #getTrades()} and {@link #refreshTrades()} throw {@code IllegalStateException}
 * on the client: the underlying merchant offer getter is server-only.
 * <p>
 * Read displayed offers from the open trading screen's {@code VillagerInventory} and
 * trading experience through {@link #getExperience()}. {@link #hasCustomer()} reports the
 * entity's local trading-player field, which is not a reliable indication of remote occupancy on a client.
 * <pre>
 * // the trade list has to come from the open screen, not from the entity
 * const inv = Inventories.Inventory.create(Hud.getOpenScreen());
 * if (inv !== null) {
 *   if (inv instanceof Inventories.Villager) {
 *     for (const trade of inv.getTrades()) {
 *       Chat.log(`#${trade.getIndex()}: ${trade.getOutput().getName()} `
 *         + `for ${trade.getLeftInput().getName()}`);
 *     }
 *   }
 * }
 * </pre>
 * @param <T> the merchant entity type wrapped by this helper
 */
@SuppressWarnings("unused")
@DocletCategory("Entity Helpers")
public class MerchantEntityHelper<T extends AbstractVillager> extends LivingEntityHelper<T> {

    /** Wraps the supplied merchant entity, retaining the same reference.
     * @param e merchant entity to wrap
     */
    public MerchantEntityHelper(T e) {
        super(e);
    }

    /**
     * Returns this merchant's offers. The underlying offer getter throws
     * {@code IllegalStateException} on the client; read displayed offers from the open
     * trading screen's {@code VillagerInventory} instead.
     *
     * @return the merchant's offers
     */
    public List<TradeOfferHelper> getTrades() {
        List<TradeOfferHelper> offers = new ArrayList<>();
        for (MerchantOffer offer : base.getOffers()) {
            offers.add(new TradeOfferHelper(offer, 0, null));
        }
        return offers;
    }

    /**
     * Clears the cached offers and returns the regenerated list. On the client, the
     * cache is cleared before {@link #getTrades()} throws.
     *
     * @return the merchant's regenerated offers
     */
    public List<TradeOfferHelper> refreshTrades() {
        ((IMerchantEntity) base).jsmacros_refreshOffers();
        return getTrades();
    }

    /**
     * Returns the merchant's accumulated trading experience; the base merchant
     * implementation returns zero. For a villager, its level is available from
     * {@code VillagerEntityHelper}.
     *
     * @return the experience the merchant has banked
     */
    public int getExperience() {
        return base.getVillagerXp();
    }

    /**
     * Returns whether this entity's local trading-player field is set. On a client this
     * field is not a reliable indication that a remote player is trading with the merchant.
     *
     * @return whether the local entity has a trading player
     */
    public boolean hasCustomer() {
        return base.isTrading();
    }

}
