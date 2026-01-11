package com.jsmacrosce.jsmacros.client.api.helper;

import com.google.common.collect.ImmutableSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinStatHandler;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class StatsHelper extends BaseHelper<StatsCounter> {
    public StatsHelper(StatsCounter base) {
        super(base);
    }

    public List<String> getStatList() {
        return ((MixinStatHandler) base).getStatMap().keySet().stream().map(this::getTranslationKey).collect(Collectors.toList());
    }

    public Component getStatText(String statKey) {
        for (Stat<?> stat : ImmutableSet.copyOf(((MixinStatHandler) base).getStatMap().keySet())) {
            if (getTranslationKey(stat).equals(statKey)) {
                return stat.getType().getDisplayName();
            }
        }
        throw new IllegalArgumentException("Stat not found: " + statKey);
    }

    public int getRawStatValue(String statKey) {
        for (Stat<?> stat : ImmutableSet.copyOf(((MixinStatHandler) base).getStatMap().keySet())) {
            if (getTranslationKey(stat).equals(statKey)) {
                return base.getValue(stat);
            }
        }
        throw new IllegalArgumentException("Stat not found: " + statKey);
    }

    public String getFormattedStatValue(String statKey) {
        for (Stat<?> stat : ImmutableSet.copyOf(((MixinStatHandler) base).getStatMap().keySet())) {
            if (getTranslationKey(stat).equals(statKey)) {
                return stat.format(base.getValue(stat));
            }
        }
        throw new IllegalArgumentException("Stat not found: " + statKey);
    }

    private String getTranslationKey(Stat<?> stat) {
        if (stat.getType().getDisplayName() instanceof TranslatableContents t) {
            return t.getKey();
        } else {
            return stat.getType().getDisplayName().getString();
        }
    }

    public Map<String, String> getFormattedStatMap() {
        Map<String, String> map = new HashMap<>();
        for (Stat<?> stat : ImmutableSet.copyOf(((MixinStatHandler) base).getStatMap().keySet())) {
            map.put(getTranslationKey(stat), stat.format(base.getValue(stat)));
        }
        return map;
    }

    public Map<String, Integer> getRawStatMap() {
        Map<String, Integer> map = new HashMap<>();
        for (Stat<?> stat : ImmutableSet.copyOf(((MixinStatHandler) base).getStatMap().keySet())) {
            map.put(getTranslationKey(stat), base.getValue(stat));
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
