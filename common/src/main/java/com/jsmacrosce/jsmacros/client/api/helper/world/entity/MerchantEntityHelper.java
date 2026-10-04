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
 * anything that trades: a villager, a wandering trader, or anything else built on the
 * game's merchant base class.
 * <p>
 * <b>Reading the trades off the entity throws on the client.</b> The game's own
 * {@code getOffers()} refuses to build a trade list outside a server world, and
 * {@link #getTrades()} goes straight through to it, so calling either it or
 * {@link #refreshTrades()} raises an {@code IllegalStateException} rather than handing
 * back an empty list. The trade list is a server thing that the client is sent, not one
 * it works out, and a script cannot ask the entity for it.
 * <p>
 * The way to read a villager's trades is through the open trading screen instead, which
 * holds the list the server sent. That is {@code getTrades()} on the
 * {@code VillagerInventory} that {@code Inventory.create()} hands back for a merchant
 * screen, and the offers it gives are the ones actually on offer to this player.
 * <p>
 * What is left on this class is the state around the trading rather than the trades
 * themselves, and that does read on the client: the experience a villager has banked and
 * whether somebody is currently being served by it.
 * example:
 * <pre>
 * // the trade list has to come from the open screen, not from the entity
 * const Inventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory");
 * const VillagerInventory = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.VillagerInventory");
 * const inv = Inventory.create(Hud.getOpenScreen());
 * if (inv !== null) {
 *   if (inv.is("Villager")) {
 *     for (const trade of VillagerInventory.class.cast(inv).getTrades()) {
 *       Chat.log(`#${trade.getIndex()}: ${trade.getOutput().getName()} `
 *         + `for ${trade.getLeftInput().getName()}`);
 *     }
 *   }
 * }
 *
 * // the entity itself still answers the questions that are not about the trades
 * const villagers = World.getEntities("villager");
 * if (villagers !== null) {
 *   for (const entity of villagers) {
 *     const villager = entity.asVillager();
 *     if (villager.hasCustomer()) {
 *       Chat.log(`a level ${villager.getLevel()} ${villager.getProfession()} is busy`);
 *     }
 *   }
 * }
 * </pre>
 */
@SuppressWarnings("unused")
@DocletCategory("Entity Helpers")
public class MerchantEntityHelper<T extends AbstractVillager> extends LivingEntityHelper<T> {

    public MerchantEntityHelper(T e) {
        super(e);
    }

    /**
     * the trade list this merchant is offering.
     * <p>
     * This throws rather than returning anything on the client, which is the only place
     * a script runs. The game's own offer getter builds the list from the merchant's
     * level and its profession and refuses to do that outside a server world, and this
     * calls it directly.
     * <p>
     * Use {@code getTrades()} on the {@code VillagerInventory} of the open trading
     * screen instead: that reads the list the server sent to the client, which is the
     * one this player is actually being offered.
     *
     * @return the trade offers read off the merchant entity itself, which on the
     * client is never reached
     * @throws IllegalStateException always on the client, because the game refuses to
     * build a merchant's offers outside a server world
     */
    public List<TradeOfferHelper> getTrades() {
        List<TradeOfferHelper> offers = new ArrayList<>();
        for (MerchantOffer offer : base.getOffers()) {
            offers.add(new TradeOfferHelper(offer, 0, null));
        }
        return offers;
    }

    /**
     * drops the cached trade list so that the next read builds it again, and then reads
     * it back.
     * <p>
     * Because the read at the end goes through {@link #getTrades()}, this throws on the
     * client for the same reason that one does, and the cache being dropped is the only
     * part that runs. The list itself is a server thing, so there is nothing a client
     * can usefully refresh here.
     * <p>
     * A merchant does pick new offers up on its own, on the game's own restock timer,
     * and a script that wants to see them can read them off the open screen.
     *
     * @return the trade offers, which on the client is never reached
     * @throws IllegalStateException always on the client, because the read at the end
     * goes through {@link #getTrades()}
     */
    public List<TradeOfferHelper> refreshTrades() {
        ((IMerchantEntity) base).jsmacros_refreshOffers();
        return getTrades();
    }

    /**
     * the experience the merchant has banked, which is what pushes it towards its next
     * trading level. The value is a running total rather than a fraction of a bar, and
     * the level it belongs to is on {@code VillagerEntityHelper}, for a villager.
     * <p>
     * The base class of every merchant answers this with zero, so a merchant that is not
     * a villager reads as having none rather than as having an amount.
     * example:
     * <pre>
     * const villagers = World.getEntities("villager");
     * if (villagers !== null) {
     *   for (const entity of villagers) {
     *     const villager = entity.asVillager();
     *     Chat.log(`${villager.getProfession()} level ${villager.getLevel()} `
     *       + `on ${villager.getExperience()} xp`);
     *   }
     * }
     * </pre>
     *
     * @return the experience the merchant has banked
     */
    public int getExperience() {
        return base.getVillagerXp();
    }

    /**
     * whether somebody is trading with this merchant right now, which is the game
     * holding on to a player as its current customer. A villager stops pathing and
     * starts facing them while this is on, and one villager serves one player at a time.
     * <p>
     * This is about the merchant being busy rather than about it having anything to sell,
     * and it is readable on the client: the client is told who is being served.
     * example:
     * <pre>
     * const villagers = World.getEntities("villager");
     * if (villagers !== null) {
     *   for (const entity of villagers) {
     *         const villager = entity.asVillager();
     *         if (villager.hasCustomer()) {
     *           Chat.log(`busy, so it is standing still at ${villager.getPos()}`);
     *         }
     *       }
     *     }
     * </pre>
     *
     * @return {@code true} if a player is currently trading with this merchant,
     * {@code false} otherwise.
     */
    public boolean hasCustomer() {
        return base.isTrading();
    }

}
