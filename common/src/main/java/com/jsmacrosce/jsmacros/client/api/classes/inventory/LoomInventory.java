package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BannerPatternTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import com.jsmacrosce.jsmacros.client.access.ILoomScreen;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @since 1.5.1
 */
@SuppressWarnings("unused")
public class LoomInventory extends Inventory<LoomScreen> {

    protected LoomInventory(LoomScreen inventory) {
        super(inventory);
    }

    private List<Holder<BannerPattern>> getPatternsFor(ItemStack stack) {
        var bannerPatternLookup = mc.getConnection().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
        // Taken from LoomScreenHandler#getPatternsFor
        if (stack.isEmpty()) {
            return bannerPatternLookup.get(BannerPatternTags.NO_ITEM_REQUIRED).map(ImmutableList::copyOf).orElse(ImmutableList.of());
        } else {
            TagKey<BannerPattern> tagKey = stack.get(DataComponents.PROVIDES_BANNER_PATTERNS);
            return tagKey != null ? bannerPatternLookup.get(tagKey).map(ImmutableList::copyOf).orElse(ImmutableList.of()) : List.of();
        }
    }

    /**
     * @param name
     * @return success
     * @since 1.5.1
     */
    @Deprecated
    public boolean selectPatternName(String name) {
        throw new NullPointerException("This method is deprecated. Use selectPatternId instead.");
    }

    /**
     * @return available pattern ids
     * @since 1.7.0
     */
    public List<String> listAvailablePatterns() {
        Iterable<Holder<BannerPattern>> patterns = getPatternsFor(inventory.getMenu().getSlot(2).getItem());
        return StreamSupport.stream(patterns.spliterator(), false).map(e -> Objects.requireNonNull(mc.getConnection()).registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).getKey(e.value()).toString()).collect(Collectors.toList());
    }

    /**
     * @param id
     * @return success
     * @since 1.5.1
     */
    public boolean selectPatternId(String id) {
        List<Holder<BannerPattern>> patterns = getPatternsFor(inventory.getMenu().getSlot(2).getItem());
        Holder<BannerPattern> pattern = StreamSupport.stream(patterns.spliterator(), false).filter(e -> Objects.requireNonNull(mc.getConnection()).registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).getKey(e.value()).toString().equals(id)).findFirst().orElse(null);

        int iid = patterns.indexOf(pattern);
        if (pattern != null && ((ILoomScreen) inventory).jsmacros_canApplyDyePattern() &&
                inventory.getMenu().clickMenuButton(player, iid)) {
            assert mc.gameMode != null;
            mc.gameMode.handleInventoryButtonClick(syncId, iid);
            return true;
        }
        return false;
    }

    /**
     * @param index
     * @return success
     * @since 1.5.1
     */
    public boolean selectPattern(int index) {
        List<Holder<BannerPattern>> patterns = getPatternsFor(inventory.getMenu().getSlot(2).getItem());

        if (index >= 0 && index <= patterns.size() && ((ILoomScreen) inventory).jsmacros_canApplyDyePattern() &&
                inventory.getMenu().clickMenuButton(player, index)) {
            assert mc.gameMode != null;
            mc.gameMode.handleInventoryButtonClick(syncId, index);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("LoomInventory:{}");
    }

}
