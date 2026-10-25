package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BannerPatternTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.client.access.ILoomScreen;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Provides access to pattern selection in an open loom screen. Available patterns are
 * determined by the pattern slot; selecting one requires the pattern list to be displayed.
 * <pre>
 * const inv = Player.openInventory();
 * if (inv instanceof Inventories.Loom) {
 *   const patterns = inv.listAvailablePatterns();
 *   Chat.log(`${patterns.size()} patterns on offer`);
 *   if (patterns.size() > 0) {
 *     Chat.log(`picking ${patterns.get(0)}`);
 *     inv.selectPattern(0);
 *   }
 * }
 * </pre>
 *
 * @since 1.5.1
 */
@DocletCategory("Inventory")
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
            var patterns = stack.get(DataComponents.PROVIDES_BANNER_PATTERNS);
            //? if >=26.1 {
            /*return patterns != null ? StreamSupport.stream(patterns.spliterator(), false).toList() : List.of();
            *///? } else {
            return patterns != null ? bannerPatternLookup.get(patterns).map(ImmutableList::copyOf).orElse(ImmutableList.of()) : List.of();
            //? }
        }
    }

    /**
     * This deprecated name-based method always throws; use {@link #selectPatternId(String)}.
     *
     * @param name ignored
     * @return never returns
     * @since 1.5.1
     */
    @Deprecated
    @DocletIgnore
    public boolean selectPatternName(String name) {
        throw new NullPointerException("This method is deprecated. Use selectPatternId instead.");
    }

    /**
     * Returns the registry ids of patterns available for the current pattern-slot item, in
     * selection order. An empty pattern slot yields patterns that need no item; an item with no
     * provided patterns yields an empty list. The banner and dye do not affect this list.
     *
     * @return the available pattern registry ids, in the order the loom offers them
     * @since 1.7.0
     */
    public List<String> listAvailablePatterns() {
        Iterable<Holder<BannerPattern>> patterns = getPatternsFor(inventory.getMenu().getSlot(2).getItem());
        return StreamSupport.stream(patterns.spliterator(), false).map(e -> Objects.requireNonNull(mc.getConnection()).registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).getKey(e.value()).toString()).collect(Collectors.toList());
    }

    /**
     * Selects a currently available pattern by its registry id and sends the selection to the
     * server. Returns {@code false} if the id is unavailable or the pattern list is not
     * displayed.
     *
     * @param id the registry id of the pattern to apply, such as {@code "minecraft:base"}
     * @return {@code true} if the pattern was selected and the loom was asked to apply it,
     *         {@code false} otherwise
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
     * Selects a currently available pattern by its zero-based position and sends the selection
     * to the server. Returns {@code false} for an out-of-range index or when the pattern list is
     * not displayed.
     *
     * @param index zero-based position in the available pattern list
     * @return {@code true} if the pattern was selected and the loom was asked to apply it,
     *         {@code false} otherwise
     * @since 1.5.1
     */
    public boolean selectPattern(int index) {
        List<Holder<BannerPattern>> patterns = getPatternsFor(inventory.getMenu().getSlot(2).getItem());

        if (index >= 0 && index < patterns.size() && ((ILoomScreen) inventory).jsmacros_canApplyDyePattern() &&
                inventory.getMenu().clickMenuButton(player, index)) {
            assert mc.gameMode != null;
            mc.gameMode.handleInventoryButtonClick(syncId, index);
            return true;
        }
        return false;
    }

    /**
     * Returns the fixed diagnostic label {@code LoomInventory:{}}.
     *
     * @return {@code LoomInventory:{}}
     */
    @Override
    public String toString() {
        return String.format("LoomInventory:{}");
    }

}
