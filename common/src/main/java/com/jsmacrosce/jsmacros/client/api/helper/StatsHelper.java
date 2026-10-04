package com.jsmacrosce.jsmacros.client.api.helper;

import com.google.common.collect.ImmutableSet;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinStatHandler;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * the player's statistics counters, which are the same numbers the statistics screen shows.
 * <p>
 * This is a view onto the game's own counters rather than a copy, so a value read here is the
 * count the game holds for that statistic. The game is what fills them in, and it only does so
 * when the server sends the statistics it is keeping, so a value read straight after an action
 * may not have caught up yet; {@link #updateStatistics()} asks the server for them.
 * <p>
 * There are two ways to ask this class for a value, and the difference matters. The typed calls
 * such as {@link #getBlockMined(String)} and {@link #getCustomStat(String)} name one exact
 * statistic and return it, and they are the ones to use whenever a script knows what it wants.
 * The keyed calls {@link #getRawStatValue(String)}, {@link #getFormattedStatValue(String)} and
 * {@link #getStatText(String)} instead take a key from {@link #getStatList()}.
 * <br>
 * Each key identifies an individual statistic by its type and registry value, for example
 * {@code minecraft.mined:minecraft.stone}. The maps preserve separate entries for statistics
 * in the same category. {@link #getStatText(String)} still returns the category's heading,
 * rather than the individual statistic's display name.
 * <br>
 * A statistic the client was never sent has no entry at all, so a key that is not in
 * {@link #getStatList()} makes the three keyed calls throw rather than return zero.
 * example:
 * <pre>
 * if (World.isWorldLoaded()) {
 *   const stats = Player.getStatistics();
 *
 *   // the typed calls name one exact statistic, so this is unambiguous
 *   Chat.log(`${stats.getBlockMined("minecraft:stone")} stone mined`);
 *   Chat.log(`${stats.getItemUsed("minecraft:diamond_pickaxe")} pickaxe uses`);
 *
 *   // ask the server to send everything it is holding before reading
 *   stats.updateStatistics();
 * }
 * </pre>
 *
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class StatsHelper extends BaseHelper<StatsCounter> {
    public StatsHelper(StatsCounter base) {
        super(base);
    }

    /**
     * the keys of the statistics this helper currently holds.
     * <p>
     * This is where the keys the keyed calls take come from, and it is the list to walk when a
     * script wants to see everything at once. A key that is not in this list makes
     * {@link #getRawStatValue(String)}, {@link #getFormattedStatValue(String)} and
     * {@link #getStatText(String)} throw, so this doubles as the list of valid keys.
     * <br>
     * Each key names one recorded statistic. Statistics in the same category have distinct keys.
     * example:
     * <pre>
     * if (World.isWorldLoaded()) {
     *   const stats = Player.getStatistics();
     *   for (const key of stats.getStatList()) {
     *     Chat.log(`${key}: ${stats.getFormattedStatValue(key)}`);
     *   }
     * }
     * </pre>
     *
     * @return a list of individual statistic keys, one per recorded statistic
     * @since 1.8.4
     */
    public List<String> getStatList() {
        return ((MixinStatHandler) base).getStatMap().keySet().stream().map(Stat::getName).collect(Collectors.toList());
    }

    /**
     * the heading of the category the given key belongs to.
     * <p>
     * Although a key identifies an individual statistic, this returns the category's own name —
     * the "Mined" or "Used" heading — shared by statistics of the same type. It is
     * the text the statistics screen prints at the top of each group, which is what makes it
     * useful as a label when walking {@link #getStatList()}, and misleading as a way of naming
     * one statistic.
     * <br>
     * This returns the game's own text component, not a {@link TextHelper}, so it carries the
     * client's language; wrap it if a helper is wanted.
     * example:
     * <pre>
     * if (World.isWorldLoaded()) {
     *   const stats = Player.getStatistics();
     *   // getStatText hands back the game's own text component, so wrap it to read it
     *   const TextHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.TextHelper");
     *   for (const key of stats.getStatList()) {
     *     // the heading for that whole category, not for one statistic
     *     const heading = TextHelper.wrap(stats.getStatText(key));
     *     Chat.log(`${heading.getString()} - ${key}: ${stats.getFormattedStatValue(key)}`);
     *   }
     * }
     * </pre>
     *
     * @param statKey one of the keys from {@link #getStatList()}
     * @return the text component naming that statistic's category
     * @throws IllegalArgumentException if the key is not one this helper holds
     * @since 1.8.4
     */
    public Component getStatText(String statKey) {
        for (Stat<?> stat : ImmutableSet.copyOf(((MixinStatHandler) base).getStatMap().keySet())) {
            if (stat.getName().equals(statKey)) {
                return stat.getType().getDisplayName();
            }
        }
        throw new IllegalArgumentException("Stat not found: " + statKey);
    }

    /**
     * the raw counter for the individual statistic the given key names, as a plain number.
     * <p>
     * The value is the game's own count, unformatted: distance statistics use centimetres and
     * plain counters have no thousands separator. The key selects the exact recorded statistic.
     * example:
     * <pre>
     * if (World.isWorldLoaded()) {
     *   const stats = Player.getStatistics();
     *   for (const key of stats.getStatList()) {
     *     Chat.log(`${key} = ${stats.getRawStatValue(key)}`);
     *   }
     * }
     * </pre>
     *
     * @param statKey one of the keys from {@link #getStatList()}
     * @return the raw value of the matching statistic
     * @throws IllegalArgumentException if the key is not one this helper holds
     * @since 1.8.4
     */
    public int getRawStatValue(String statKey) {
        for (Stat<?> stat : ImmutableSet.copyOf(((MixinStatHandler) base).getStatMap().keySet())) {
            if (stat.getName().equals(statKey)) {
                return base.getValue(stat);
            }
        }
        throw new IllegalArgumentException("Stat not found: " + statKey);
    }

    /**
     * the counter for the statistic the given key names, the way the statistics screen prints it.
     * <p>
     * This is the same number {@link #getRawStatValue(String)} returns, run through the
     * statistic's own formatter, so a distance can read as "1.2 km", a time uses the formatter's
     * selected unit, and a plain count stays a plain count.
     * example:
     * <pre>
     * if (World.isWorldLoaded()) {
     *   const stats = Player.getStatistics();
     *   for (const key of stats.getStatList()) {
     *     Chat.log(`${stats.getFormattedStatValue(key)} (raw ${stats.getRawStatValue(key)})`);
     *   }
     * }
     * </pre>
     *
     * @param statKey one of the keys from {@link #getStatList()}
     * @return the formatted value of the matching statistic
     * @throws IllegalArgumentException if the key is not one this helper holds
     * @since 1.8.4
     */
    public String getFormattedStatValue(String statKey) {
        for (Stat<?> stat : ImmutableSet.copyOf(((MixinStatHandler) base).getStatMap().keySet())) {
            if (stat.getName().equals(statKey)) {
                return stat.format(base.getValue(stat));
            }
        }
        throw new IllegalArgumentException("Stat not found: " + statKey);
    }

    /**
     * Every statistic this helper holds, keyed by its statistic name, with each value already
     * formatted. Statistics in the same category retain their individual entries.
     * example:
     * <pre>
     * if (World.isWorldLoaded()) {
     *   const stats = Player.getStatistics();
     *   for (const entry of stats.getFormattedStatMap().entrySet()) {
     *     Chat.log(`${entry.getKey()}: ${entry.getValue()}`);
     *   }
     * }
     * </pre>
     *
     * @return a map of statistic name to its formatted value
     * @since 1.8.4
     */
    public Map<String, String> getFormattedStatMap() {
        Map<String, String> map = new HashMap<>();
        for (Stat<?> stat : ImmutableSet.copyOf(((MixinStatHandler) base).getStatMap().keySet())) {
            map.put(stat.getName(), stat.format(base.getValue(stat)));
        }
        return map;
    }

    /**
     * every statistic this helper holds, keyed by its individual statistic name, with each value unformatted.
     * <p>
     * The raw counterpart to {@link #getFormattedStatMap()}; statistics in the same category
     * retain separate entries.
     * example:
     * <pre>
     * if (World.isWorldLoaded()) {
     *   const stats = Player.getStatistics();
     *   for (const entry of stats.getRawStatMap().entrySet()) {
     *     Chat.log(`${entry.getKey()}: ${entry.getValue()}`);
     *   }
     * }
     * </pre>
     *
     * @return a map of individual statistic name to its raw value
     * @since 1.8.4
     */
    public Map<String, Integer> getRawStatMap() {
        Map<String, Integer> map = new HashMap<>();
        for (Stat<?> stat : ImmutableSet.copyOf(((MixinStatHandler) base).getStatMap().keySet())) {
            map.put(stat.getName(), base.getValue(stat));
        }
        return map;
    }

    /**
     * @param id the identifier of the entity
     * @return how many times the player has killed the entity.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: EntityId")
    public int getEntityKilled(String id) {
        return getStat(Stats.ENTITY_KILLED, BuiltInRegistries.ENTITY_TYPE, id);
    }

    /**
     * @param id the identifier of the entity
     * @return how many times the player has killed the specified entity.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: EntityId")
    public int getKilledByEntity(String id) {
        return getStat(Stats.ENTITY_KILLED_BY, BuiltInRegistries.ENTITY_TYPE, id);
    }

    /**
     * @param id the identifier of the block
     * @return how many times the player has mined the block.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: BlockId")
    public int getBlockMined(String id) {
        return getStat(Stats.BLOCK_MINED, BuiltInRegistries.BLOCK, id);
    }

    /**
     * @param id the identifier of the item
     * @return how many times the player has broken the item.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: ItemId")
    public int getItemBroken(String id) {
        return getStat(Stats.ITEM_BROKEN, BuiltInRegistries.ITEM, id);
    }

    /**
     * @param id the identifier of the item
     * @return how many times the player has crafted the item.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: ItemId")
    public int getItemCrafted(String id) {
        return getStat(Stats.ITEM_CRAFTED, BuiltInRegistries.ITEM, id);
    }

    /**
     * @param id the identifier of the item
     * @return how many times the player has used the item.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: ItemId")
    public int getItemUsed(String id) {
        return getStat(Stats.ITEM_USED, BuiltInRegistries.ITEM, id);
    }

    /**
     * @param id the identifier of the item
     * @return how many times the player has picked up the item.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: ItemId")
    public int getItemPickedUp(String id) {
        return getStat(Stats.ITEM_PICKED_UP, BuiltInRegistries.ITEM, id);
    }

    /**
     * @param id the identifier of the item
     * @return how many times the player has dropped the item.
     * @since 1.8.4
     */
    @DocletReplaceParams("id: ItemId")
    public int getItemDropped(String id) {
        return getStat(Stats.ITEM_DROPPED, BuiltInRegistries.ITEM, id);
    }

    /**
     * @param id the identifier of the custom stat
     * @return the value of the custom stat.
     * @since 1.8.4
     */
    public int getCustomStat(String id) {
        return base.getValue(Stats.CUSTOM.get(RegistryHelper.parseIdentifier(id)));
    }

    private <T> int getStat(StatType<T> type, Registry<T> registry, String id) {
        return base.getValue(type.get(registry.getValue(RegistryHelper.parseIdentifier(id))));
    }

    /**
     * @param id the identifier of the custom stat
     * @return the formatted value of the custom stat.
     * @since 1.8.4
     */
    public String getCustomFormattedStat(String id) {
        Stat<ResourceLocation> stat = Stats.CUSTOM.get(RegistryHelper.parseIdentifier(id));
        return stat.format(base.getValue(stat));
    }

    /**
     * Used to request an update of the statistics from the server.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public StatsHelper updateStatistics() {
        Minecraft mc = Minecraft.getInstance();
        assert mc.getConnection() != null;
        mc.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
        return this;
    }

    @Override
    public String toString() {
        return String.format("StatsHelper:{%s}", getFormattedStatMap());
    }

}
