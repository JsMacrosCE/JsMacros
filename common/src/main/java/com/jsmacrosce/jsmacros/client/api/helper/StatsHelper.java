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
 * Provides access to the player's client-side statistics counters.
 * <p>
 * Typed methods query a named registry statistic, while custom-stat methods query the named custom
 * statistic directly. Keyed methods accept names from {@link #getStatList()}; unknown keys throw
 * {@link IllegalArgumentException}. {@link #getStatText(String)} returns the statistic type's
 * display name, not an individual statistic name. {@link #updateStatistics()} requests updated
 * statistics from the server.
 * <pre>
 * if (World.isWorldLoaded()) {
 *   const stats = Player.getStatistics();
 *
 *   Chat.log(`${stats.getBlockMined("minecraft:stone")} stone mined`);
 *   Chat.log(`${stats.getItemUsed("minecraft:diamond_pickaxe")} pickaxe uses`);
 *
 *   stats.updateStatistics();
 * }
 * </pre>
 *
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class StatsHelper extends BaseHelper<StatsCounter> {
    /**
     * Wraps the supplied statistics counter.
     *
     * @param base raw statistics counter to wrap
     */
    public StatsHelper(StatsCounter base) {
        super(base);
    }

    /**
     * Returns the names of the statistics currently held by this helper; each name identifies
     * one statistic, including when multiple statistics share a type.
     *
     * @return a list of individual statistic keys, one per recorded statistic
     * @since 1.8.4
     */
    public List<String> getStatList() {
        return ((MixinStatHandler) base).getStatMap().keySet().stream().map(Stat::getName).collect(Collectors.toList());
    }

    /**
     * Returns the display name of the statistic type identified by {@code statKey}, rather than
     * a name specific to that statistic.
     *
     * @param statKey one of the keys from {@link #getStatList()}
     * @return the text component naming that statistic's category
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
     * Returns the unformatted counter for the statistic identified by {@code statKey}.
     *
     * @param statKey one of the keys from {@link #getStatList()}
     * @return the raw value of the matching statistic
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
     * Returns the counter for the statistic identified by {@code statKey}, formatted by that
     * statistic's formatter.
     *
     * @param statKey one of the keys from {@link #getStatList()}
     * @return the formatted value of the matching statistic
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
     * Returns the formatted counters currently held by this helper, keyed by individual
     * statistic name.
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
     * Returns the unformatted counters currently held by this helper, keyed by individual
     * statistic name.
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
     * The number of times the player has killed the entity with this identifier.
     *
     * @param id the identifier of the entity
     * @return the kill count
     * @since 1.8.4
     */
    @DocletReplaceParams("id: EntityId")
    public int getEntityKilled(String id) {
        return getStat(Stats.ENTITY_KILLED, BuiltInRegistries.ENTITY_TYPE, id);
    }

    /**
     * The number of times the player has been killed by the entity with this identifier.
     *
     * @param id the identifier of the entity
     * @return the death count
     * @since 1.8.4
     */
    @DocletReplaceParams("id: EntityId")
    public int getKilledByEntity(String id) {
        return getStat(Stats.ENTITY_KILLED_BY, BuiltInRegistries.ENTITY_TYPE, id);
    }

    /**
     * The number of times the player has mined the block with this identifier.
     *
     * @param id the identifier of the block
     * @return the mined count
     * @since 1.8.4
     */
    @DocletReplaceParams("id: BlockId")
    public int getBlockMined(String id) {
        return getStat(Stats.BLOCK_MINED, BuiltInRegistries.BLOCK, id);
    }

    /**
     * Returns the broken-item count for the item identified by {@code id}.
     *
     * @param id the identifier of the item
     * @return the broken count
     * @since 1.8.4
     */
    @DocletReplaceParams("id: ItemId")
    public int getItemBroken(String id) {
        return getStat(Stats.ITEM_BROKEN, BuiltInRegistries.ITEM, id);
    }

    /**
     * Returns the crafted-item count for the item identified by {@code id}.
     *
     * @param id the identifier of the item
     * @return the crafted count
     * @since 1.8.4
     */
    @DocletReplaceParams("id: ItemId")
    public int getItemCrafted(String id) {
        return getStat(Stats.ITEM_CRAFTED, BuiltInRegistries.ITEM, id);
    }

    /**
     * Returns the item-use count for the item identified by {@code id}.
     *
     * @param id the identifier of the item
     * @return the used count
     * @since 1.8.4
     */
    @DocletReplaceParams("id: ItemId")
    public int getItemUsed(String id) {
        return getStat(Stats.ITEM_USED, BuiltInRegistries.ITEM, id);
    }

    /**
     * Returns the item pickup count for the item identified by {@code id}.
     *
     * @param id the identifier of the item
     * @return the picked-up count
     * @since 1.8.4
     */
    @DocletReplaceParams("id: ItemId")
    public int getItemPickedUp(String id) {
        return getStat(Stats.ITEM_PICKED_UP, BuiltInRegistries.ITEM, id);
    }

    /**
     * Returns the item-drop count for the item identified by {@code id}.
     *
     * @param id the identifier of the item
     * @return the dropped count
     * @since 1.8.4
     */
    @DocletReplaceParams("id: ItemId")
    public int getItemDropped(String id) {
        return getStat(Stats.ITEM_DROPPED, BuiltInRegistries.ITEM, id);
    }

    /**
     * Returns the counter for the custom statistic identified by {@code id}.
     *
     * @param id the custom statistic identifier
     * @return the raw counter value
     * @since 1.8.4
     */
    public int getCustomStat(String id) {
        return base.getValue(Stats.CUSTOM.get(RegistryHelper.parseIdentifier(id)));
    }

    private <T> int getStat(StatType<T> type, Registry<T> registry, String id) {
        return base.getValue(type.get(registry.getValue(RegistryHelper.parseIdentifier(id))));
    }

    /**
     * Returns the formatted counter for the custom statistic identified by {@code id}.
     *
     * @param id the custom statistic identifier
     * @return the formatted counter value
     * @since 1.8.4
     */
    public String getCustomFormattedStat(String id) {
        Stat<ResourceLocation> stat = Stats.CUSTOM.get(RegistryHelper.parseIdentifier(id));
        return stat.format(base.getValue(stat));
    }

    /**
     * Requests that the server send the current statistics over the active connection.
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

    /**
     * Returns a diagnostic string containing the formatted statistics map, in the form
     * {@code StatsHelper:{...}}.
     *
     * @return diagnostic string containing the formatted statistics map
     */
    @Override
    public String toString() {
        return String.format("StatsHelper:{%s}", getFormattedStatMap());
    }

}
