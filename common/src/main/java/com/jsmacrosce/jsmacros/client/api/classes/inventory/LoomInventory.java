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
import com.jsmacrosce.jsmacros.client.access.ILoomScreen;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * the handle for an open loom screen.
 * <p>
 * A loom weaves a pattern into a banner, and it does so in one step: pick a pattern, and the
 * pattern is woven into whatever is already in the banner slot. That makes this class about
 * patterns rather than items. {@link #listAvailablePatterns()} names the patterns the loom can
 * offer right now, and {@link #selectPattern(int)} and {@link #selectPatternId(String)} pick
 * one.
 * <p>
 * What is on offer is decided by the item in the pattern slot and by nothing else. The loom
 * reads that slot to decide which patterns apply, so neither the banner nor the dye changes
 * the list, and a pattern slot holding nothing still offers the patterns that need no item at
 * all. A script that wants to know whether a pattern can be used should therefore compare
 * against this list rather than reading the slots.
 * <p>
 * The pattern list is an overlay the player opens on the loom, so choosing a pattern also has
 * to be sent to the server as a menu button click. {@link #selectPattern(int)} and
 * {@link #selectPatternId(String)} both refuse unless that overlay is open, which is why they
 * can return {@code false} with nothing wrong in the index.
 * example:
 * <pre>
 * const inv = Player.openInventory();
 * if (inv.is("Loom")) {
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
     * always throws, because the method it replaced was removed.
     * <p>
     * The body is a single unconditional {@link java.lang.NullPointerException} rather than a
     * {@code return}, so this does not merely return {@code false}: a script still calling it
     * fails loudly instead of quietly doing nothing. That is deliberate, so an old call site is
     * noticed rather than silently ignored. {@link #selectPatternId(String)} is what to use
     * instead, and it takes the registry id that {@link #listAvailablePatterns()} returns
     * rather than a display name, which is why the old name-based form could not be kept
     * working.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Loom")) {
     *   for (const id of inv.listAvailablePatterns()) {
     *     // the id form is the supported one; selectPatternName always throws
     *     inv.selectPatternId(id);
     *     break;
     *   }
     * }
     * </pre>
     *
     * @param name ignored; the pattern used to be named by its display name
     * @return never returns
     * @throws NullPointerException always, with a message pointing at
     *         {@link #selectPatternId(String)}
     * @since 1.5.1
     */
    @Deprecated
    public boolean selectPatternName(String name) {
        throw new NullPointerException("This method is deprecated. Use selectPatternId instead.");
    }

    /**
     * the registry ids of the patterns the loom can offer for the item in its pattern slot, in
     * the order they are offered.
     * <p>
     * The pattern slot, which is slot 2 of the loom's menu, is the only input this looks at: an
     * item there that provides patterns narrows the list to those, and an empty pattern slot
     * gives the patterns that need no item at all. The banner and the dye are not consulted, so
     * this can list patterns that the current banner cannot actually take.
     * <p>
     * The order matters, because it is the order {@link #selectPattern(int)} takes. The list is
     * empty when the pattern slot holds an item that provides no patterns, which is not the
     * same as an empty pattern slot.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Loom")) {
     *   for (const id of inv.listAvailablePatterns()) {
     *     Chat.log(`pattern ${id}`);
     *   }
     * }
     * </pre>
     *
     * @return the available pattern registry ids, in the order the loom offers them
     * @since 1.7.0
     */
    public List<String> listAvailablePatterns() {
        Iterable<Holder<BannerPattern>> patterns = getPatternsFor(inventory.getMenu().getSlot(2).getItem());
        return StreamSupport.stream(patterns.spliterator(), false).map(e -> Objects.requireNonNull(mc.getConnection()).registryAccess().lookupOrThrow(Registries.BANNER_PATTERN).getKey(e.value()).toString()).collect(Collectors.toList());
    }

    /**
     * picks a pattern by the registry id {@link #listAvailablePatterns()} returns, and asks the
     * loom to apply it.
     * <p>
     * This is the same choice {@link #selectPattern(int)} makes, looked up by id instead, so
     * the id has to be one the loom is currently offering: anything else finds no pattern and
     * returns {@code false}. It also requires the loom's pattern overlay to be open, so
     * {@code false} does not necessarily mean the id was wrong.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Loom")) {
     *   const applied = inv.selectPatternId("minecraft:base");
     *   Chat.log(applied ? "pattern applied" : "no such pattern, or the pattern list is closed");
     * }
     * </pre>
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
     * picks a pattern by its position in {@link #listAvailablePatterns()} and asks the loom to
     * apply it.
     * <p>
     * The valid range is 0 up to one less than the number of patterns on offer, so the last
     * usable index is {@code listAvailablePatterns().size() - 1}. An index below zero or at least
     * the number of patterns is rejected locally and returns {@code false} without sending a
     * selection. It also requires the loom's pattern overlay to be open, which is
     * the other reason a valid index can come back {@code false}.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Loom")) {
     *   const last = inv.listAvailablePatterns().size() - 1;
     *   if (last >= 0) {
     *     inv.selectPattern(last);
     *   }
     * }
     * </pre>
     *
     * @param index the position in the available pattern list, 0 to
     *              {@code listAvailablePatterns().size() - 1}
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

    @Override
    public String toString() {
        return String.format("LoomInventory:{}");
    }

}
