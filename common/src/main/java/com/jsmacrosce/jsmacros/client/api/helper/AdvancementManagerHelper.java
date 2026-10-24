package com.jsmacrosce.jsmacros.client.api.helper;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinAdvancementManager;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinClientAdvancementManager;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Provides the client's advancement tree and, when a player is available, that player's progress.
 * <p>
 * Progress queries use the local player's advancement progress; the unfiltered tree queries do not.
 * <p>
 * <pre>
 * const player = Player.getPlayer();
 * if (player !== null) {
 *   const manager = player.getAdvancementManager();
 *
 *   // the progress views
 *   Chat.log(`${manager.getCompletedAdvancements().length} done`);
 *   Chat.log(`${manager.getStartedAdvancements().length} partly done`);
 *   Chat.log(`${manager.getMissingAdvancements().length} not done`);
 *
 *   // one advancement, looked up by identifier
 *   const root = manager.getAdvancement("minecraft:story/root");
 *   if (root.getRaw() !== null) {
 *     const progress = root.getProgress();
 *     if (progress.getRaw() !== null) {
 *       Chat.log(`root is ${progress.isDone() ? "done" : "not done"}`);
 *     } else {
 *       Chat.log("root has no progress entry");
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class AdvancementManagerHelper extends BaseHelper<AdvancementTree> {

    /**
     * Wraps the supplied advancement tree.
     *
     * @param advancementManager raw advancement tree to wrap
     */
    public AdvancementManagerHelper(AdvancementTree advancementManager) {
        super(advancementManager);
    }

    /**
     * Returns the advancement tree keyed by each identifier's {@code namespace:path} string.
     *
     * @return a map containing every advancement; identifiers use the {@code namespace:path} form
     * @since 1.8.4
     */
    @DocletReplaceReturn("JavaMap<AdvancementId, AdvancementHelper>")
    public Map<String, AdvancementHelper> getAdvancementsForIdentifiers() {
        return ((MixinAdvancementManager) base).getAdvancements().entrySet().stream().collect(Collectors.toMap(
                identifierAdvancementEntry -> identifierAdvancementEntry.getKey().toString(),
                identifierAdvancementEntry -> new AdvancementHelper(identifierAdvancementEntry.getValue())
        ));
    }

    /**
     * Returns every advancement node in the tree; the list order is unspecified.
     *
     * @return the advancement nodes
     * @since 1.8.4
     */
    public List<AdvancementHelper> getAdvancements() {
        return base.nodes().stream().map(AdvancementHelper::new).collect(Collectors.toList());
    }

    /**
     * Returns local-player progress entries that are incomplete and have at least one criterion obtained.
     *
     * @return the matching advancement nodes
     * @since 1.8.4
     */
    public List<AdvancementHelper> getStartedAdvancements() {
        return getProgressStream().filter(progress -> !progress.getValue().isDone() && progress.getValue().hasProgress()).map(advancementProgressEntry -> new AdvancementHelper(base.get(advancementProgressEntry.getKey()))).collect(Collectors.toList());
    }

    /**
     * Returns local-player progress entries that are not complete, including entries with no criteria obtained.
     *
     * @return the matching advancement nodes
     * @since 1.8.4
     */
    public List<AdvancementHelper> getMissingAdvancements() {
        return getProgressStream().filter(advancementProgressEntry -> !advancementProgressEntry.getValue().isDone()).map(advancementProgressEntry -> new AdvancementHelper(base.get(advancementProgressEntry.getKey()))).collect(Collectors.toList());
    }

    /**
     * Returns local-player progress entries that are complete.
     *
     * @return the matching advancement nodes
     * @since 1.8.4
     */
    public List<AdvancementHelper> getCompletedAdvancements() {
        return getProgressStream().filter(advancementProgressEntry -> advancementProgressEntry.getValue().isDone()).map(advancementProgressEntry -> new AdvancementHelper(base.get(advancementProgressEntry.getKey()))).collect(Collectors.toList());
    }

    /**
     * Returns root advancements, which have no parent.
     *
     * @return the root advancement nodes
     * @since 1.8.4
     */
    public List<AdvancementHelper> getRootAdvancements() {
        return StreamSupport.stream(base.roots().spliterator(), false).map(AdvancementHelper::new).collect(Collectors.toList());
    }

    /**
     * Returns a flat list of non-root advancements.
     *
     * @return the non-root advancement nodes
     * @since 1.8.4
     */
    public List<AdvancementHelper> getSubAdvancements() {
        //? if >=26.3 {
        /*return StreamSupport.stream(base.tasks().spliterator(), false).map(AdvancementHelper::new).collect(Collectors.toList());
        *///?} else {
        return ((MixinAdvancementManager) base).getDependents().stream().map(AdvancementHelper::new).collect(Collectors.toList());
        //?}
    }

    /**
     * Looks up an advancement by identifier; an omitted namespace defaults to {@code minecraft}.
     * The wrapper has no underlying node if the identifier is absent from the tree; check its raw
     * value before using the wrapper.
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * // both spellings work, the namespace is optional
     * const a = manager.getAdvancement("minecraft:story/root");
     * const b = manager.getAdvancement("story/root");
     * if (a.getRaw() !== null) {
     *   if (b.getRaw() !== null) {
     *     Chat.log(`${a.getId()} is ${b.getId()}`);
     *   }
     * }
     * </pre>
     *
     * @param identifier the advancement identifier; the {@code minecraft} namespace may be omitted
     * @return a wrapper for the matching advancement, with no underlying node if it is absent
     * @since 1.8.4
     */
    @DocletReplaceParams("identifier: CanOmitNamespace<AdvancementId>")
    public AdvancementHelper getAdvancement(String identifier) {
        return new AdvancementHelper(base.get(RegistryHelper.parseIdentifier(identifier)));
    }

    /**
     * Returns the local player's progress entries, keyed by their advancement nodes.
     *
     * @return progress entries present in the local player's progress store
     * @since 1.8.4
     */
    public Map<AdvancementHelper, AdvancementProgressHelper> getAdvancementsProgress() {
        return getProgressStream().collect(Collectors.toMap(
                advancementProgressEntry -> new AdvancementHelper(base.get(advancementProgressEntry.getKey())),
                advancementProgressEntry -> new AdvancementProgressHelper(advancementProgressEntry.getValue())
        ));
    }

    /**
     * Returns local-player progress for an advancement identifier; an omitted namespace defaults
     * to {@code minecraft}. The advancement must exist in the tree. If it has no progress entry,
     * the returned wrapper has no underlying progress; check its raw value before using it.
     *
     * @param identifier the advancement identifier
     * @return a wrapper for the advancement's progress entry, which may have no underlying progress
     * @since 1.8.4
     */
    @DocletReplaceParams("identifier: CanOmitNamespace<AdvancementId>")
    public AdvancementProgressHelper getAdvancementProgress(String identifier) {
        assert Minecraft.getInstance().player != null;
        return new AdvancementProgressHelper(((MixinClientAdvancementManager) Minecraft.getInstance().player.connection.getAdvancements()).getAdvancementProgresses().get(base.get(RegistryHelper.parseIdentifier(identifier)).holder()));
    }

    /**
     * Returns entries from the local player's connection-owned advancement progress store.
     */
    private Stream<Map.Entry<AdvancementHolder, AdvancementProgress>> getProgressStream() {
        LocalPlayer player = Minecraft.getInstance().player;
        assert player != null;
        return ((MixinClientAdvancementManager) player.connection.getAdvancements()).getAdvancementProgresses().entrySet().stream();
    }

    /**
     * Returns a diagnostic representation with started, missing, and completed advancement counts.
     * Counts are obtained from the local player's progress lists; started entries are also included
     * in the missing count. A local player is required.
     *
     * @return diagnostic representation containing started, missing, and completed counts
     */
    @Override
    public String toString() {
        return String.format("AdvancementManagerHelper:{\"started\": %d, \"missing\": %d, \"completed\": %d}", getStartedAdvancements().size(), getMissingAdvancements().size(), getCompletedAdvancements().size());
    }

}
