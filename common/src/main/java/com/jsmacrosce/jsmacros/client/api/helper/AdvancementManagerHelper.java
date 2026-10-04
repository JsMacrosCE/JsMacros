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
 * the whole advancement tree as the server sent it, and the player's place in it.
 * <p>
 * This is the top of the advancement hierarchy: it can list every advancement, look one up by
 * identifier, and divide them up by how far the player has got on each. The individual
 * advancements it hands back are {@link AdvancementHelper}, and the state of one of them is an
 * {@link AdvancementProgressHelper}.
 * <p>
 * The split by progress is worth being precise about, because the three lists are not the same
 * size and do not partition the tree. {@link #getCompletedAdvancements()} and
 * {@link #getMissingAdvancements()} do add up to everything, since one asks whether the
 * progress is done and the other asks whether it is not. {@link #getStartedAdvancements()} is
 * the smaller middle: an advancement that has been started is one with at least one criterion
 * met but not all of them, so it is a subset of the missing ones rather than a third group.
 * <p>
 * The progress half of this class needs a player, since it is the player's progress, so the
 * lists that come from it are only meaningful in a world.
 * example:
 * <pre>
 * const player = Player.getPlayer();
 * if (player !== null) {
 *   const manager = player.getAdvancementManager();
 *
 *   // the three ways of dividing the tree up
 *   Chat.log(`${manager.getCompletedAdvancements().length} done`);
 *   Chat.log(`${manager.getStartedAdvancements().length} partly done`);
 *   Chat.log(`${manager.getMissingAdvancements().length} not done`);
 *
 *   // one advancement, looked up by identifier
 *   const root = manager.getAdvancement("minecraft:story/root");
 *   Chat.log(`root is ${root.getProgress().isDone() ? "done" : "not done"}`);
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class AdvancementManagerHelper extends BaseHelper<AdvancementTree> {

    public AdvancementManagerHelper(AdvancementTree advancementManager) {
        super(advancementManager);
    }

    /**
     * every advancement, keyed by its identifier.
     * <p>
     * This is the same set {@link #getAdvancements()} gives as a list, in a form that is easier
     * to look things up in when the identifiers are what a script already has. Every identifier
     * is unique, so unlike some of the game's own maps nothing is lost here.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const byId = manager.getAdvancementsForIdentifiers();
     * const root = byId.get("minecraft:story/root");
     * if (root !== null) {
     *   Chat.log(`root is worth ${root.getExperience()} xp`);
     * }
     * </pre>
     *
     * @return a map of all advancement ids and their advancement.
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
     * every advancement the server sent, in no particular order.
     * <p>
     * This is the whole tree flattened into a list, so it is the starting point for a script
     * that wants to go through everything once. It includes advancements the player has not
     * started, and it is not the same as {@link #getRootAdvancements()}, which is only the top
     * level of the tree.
     * <p>
     * The order comes out of the tree, so a script that wants a stable order should sort or key
     * off {@link AdvancementHelper#getId()} rather than relying on the order here.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * let count = 0;
     * for (const adv of manager.getAdvancements()) {
     *   if (adv.getProgress().isDone()) {
     *     count++;
     *   }
     * }
     * Chat.log(`${count} of the advancements are done`);
     * </pre>
     *
     * @return a list of all advancements.
     * @since 1.8.4
     */
    public List<AdvancementHelper> getAdvancements() {
        return base.nodes().stream().map(AdvancementHelper::new).collect(Collectors.toList());
    }

    /**
     * Started advancements are advancements that have been started, so at least one task has been
     * completed so far, but not fully completed.
     * <p>
     * This is the "in progress" middle of the three, and it is a subset of
     * {@link #getMissingAdvancements()}: an advancement is here only if at least one of its
     * criteria has been met, whereas missing includes everything the player has not finished
     * including the ones they have not started at all.
     * <p>
     * This needs a player, since it is the player's progress.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * // what the player is part way through
     * for (const adv of manager.getStartedAdvancements()) {
     *   const progress = adv.getProgress();
     *   Chat.log(`${adv.getId()} at ${progress.getPercentage()} percent`);
     * }
     * </pre>
     *
     * @return a list of all started advancements.
     * @since 1.8.4
     */
    public List<AdvancementHelper> getStartedAdvancements() {
        return getProgressStream().filter(progress -> !progress.getValue().isDone() && progress.getValue().hasProgress()).map(advancementProgressEntry -> new AdvancementHelper(base.get(advancementProgressEntry.getKey()))).collect(Collectors.toList());
    }

    /**
     * every advancement the player has not finished, whether started or not.
     * <p>
     * This is the complement of {@link #getCompletedAdvancements()}, so the two together cover
     * the whole tree with nothing in common. On a fresh save, where nothing has been finished,
     * it is by far the bigger of the two.
     * <p>
     * This needs a player, since it is the player's progress.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * // the ones with nothing done on them yet
     * const untouched = manager.getMissingAdvancements()
     *   .filter(function (adv) { return !adv.getProgress().isAnyObtained(); });
     * Chat.log(`${untouched.length} advancements untouched`);
     * </pre>
     *
     * @return a list of all missing advancements.
     * @since 1.8.4
     */
    public List<AdvancementHelper> getMissingAdvancements() {
        return getProgressStream().filter(advancementProgressEntry -> !advancementProgressEntry.getValue().isDone()).map(advancementProgressEntry -> new AdvancementHelper(base.get(advancementProgressEntry.getKey()))).collect(Collectors.toList());
    }

    /**
     * every advancement the player has finished.
     * <p>
     * This is the complement of {@link #getMissingAdvancements()}, and it covers an advancement
     * being done even where the player did not do it in one go: the game only marks it done
     * when every criterion is met, however that happened.
     * <p>
     * This needs a player, since it is the player's progress.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const done = manager.getCompletedAdvancements();
     * Chat.log(`${done.length} done`);
     * // the experience they add up to
     * let xp = 0;
     * for (const adv of done) {
     *   xp += adv.getExperience();
     * }
     * Chat.log(`worth ${xp} xp`);
     * </pre>
     *
     * @return a list of all completed advancements.
     * @since 1.8.4
     */
    public List<AdvancementHelper> getCompletedAdvancements() {
        return getProgressStream().filter(advancementProgressEntry -> advancementProgressEntry.getValue().isDone()).map(advancementProgressEntry -> new AdvancementHelper(base.get(advancementProgressEntry.getKey()))).collect(Collectors.toList());
    }

    /**
     * the advancements at the top of the tree, the ones with no parent.
     * <p>
     * This is one level of the hierarchy rather than the whole thing, and it is the natural
     * starting point for walking downwards: each of these has its own
     * {@link AdvancementHelper#getChildren()}. It is not a list of what the player has done;
     * the completed and missing lists are for that.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * // walk the whole tree from the top
     * const seen = [];
     * const queue = Array.from(manager.getRootAdvancements());
     * while (queue.length > 0) {
     *   const adv = queue.shift();
     *   if (adv === undefined) {
     *     break;
     *   }
     *   seen.push(adv.getId());
     *   for (const child of adv.getChildren()) {
     *     queue.push(child);
     *   }
     * }
     * Chat.log(`the tree has ${seen.length} advancements`);
     * </pre>
     *
     * @return a list of all the root advancements.
     * @since 1.8.4
     */
    public List<AdvancementHelper> getRootAdvancements() {
        return StreamSupport.stream(base.roots().spliterator(), false).map(AdvancementHelper::new).collect(Collectors.toList());
    }

    /**
     * every advancement that is not a root.
     * <p>
     * This is the other end of {@link #getRootAdvancements()}: the whole tree minus the top
     * level. It is a flat list rather than a hierarchy, so a script wanting the structure walks
     * the roots with {@link AdvancementHelper#getChildren()} instead, and this is for the
     * simpler question of which advancements are not at the top.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const total = manager.getAdvancements().length;
     * const roots = manager.getRootAdvancements().length;
     * Chat.log(`${manager.getSubAdvancements().length} below the top, ${roots} at the top, ${total} in all`);
     * </pre>
     *
     * @return a list of all advancements that are not a root.
     * @since 1.8.4
     */
    public List<AdvancementHelper> getSubAdvancements() {
        return ((MixinAdvancementManager) base).getDependents().stream().map(AdvancementHelper::new).collect(Collectors.toList());
    }

    /**
     * one advancement, by identifier.
     * <p>
     * The identifier is the one the advancement is written as in the data files, which is what
     * {@link AdvancementHelper#getId()} gives back, so the two round-trip. The namespace may be
     * left off, in which case {@code minecraft:} is assumed.
     * <p>
     * This is the tree's own lookup, so an identifier the server did not send gives
     * {@code null} rather than a helper with nothing in it.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * // both spellings work, the namespace is optional
     * const a = manager.getAdvancement("minecraft:story/root");
     * const b = manager.getAdvancement("story/root");
     * Chat.log(`${a.getId()} is ${b.getId()}`);
     * </pre>
     *
     * @param identifier the identifier of the advancement
     * @return the advancement for the given identifier.
     * @since 1.8.4
     */
    @DocletReplaceParams("identifier: CanOmitNamespace<AdvancementId>")
    public AdvancementHelper getAdvancement(String identifier) {
        return new AdvancementHelper(base.get(RegistryHelper.parseIdentifier(identifier)));
    }

    /**
     * every advancement together with the player's progress on it, in one map.
     * <p>
     * Convenient when a script wants to go over the whole tree once and needs both halves of
     * each advancement, since it saves looking the progress up again through
     * {@link AdvancementHelper#getProgress()}.
     * <p>
     * The keys are helper objects rather than identifiers, so a script wanting to look
     * something up by name wants {@link #getAdvancementsForIdentifiers()} instead.
     * <p>
     * This needs a player, since it is the player's progress.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * for (const entry of manager.getAdvancementsProgress().entrySet()) {
     *   const progress = entry.getValue();
     *   if (progress.isAnyObtained()) {
     *     Chat.log(`${entry.getKey().getId()}: ${Math.round(progress.getPercentage() * 100)} percent`);
     *   }
     * }
     * </pre>
     *
     * @return a map of all advancements and their progress.
     * @since 1.8.4
     */
    public Map<AdvancementHelper, AdvancementProgressHelper> getAdvancementsProgress() {
        return getProgressStream().collect(Collectors.toMap(
                advancementProgressEntry -> new AdvancementHelper(base.get(advancementProgressEntry.getKey())),
                advancementProgressEntry -> new AdvancementProgressHelper(advancementProgressEntry.getValue())
        ));
    }

    /**
     * how far the player has got on one named advancement.
     * <p>
     * The same object as {@link AdvancementHelper#getProgress()}, reached by identifier rather
     * than by having the advancement in hand, which saves a lookup when the identifier is all
     * the script has. The identifier may leave the namespace off.
     * <br>
     * The wrapper this hands back is never {@code null}, but what is inside it can be: an
     * identifier the server did not send, or one it sent without any progress for, gives a
     * progress object with nothing behind it, and the first call that reads it throws. A script
     * that cannot be sure the identifier is good wants {@link #getAdvancement(String)} and a
     * {@code null} check on that instead, which distinguishes the two cases.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     *
     * // check the identifier is real before asking about its progress
     * const adv = manager.getAdvancement("minecraft:story/mine_stone");
     * if (adv !== null) {
     *   const progress = adv.getProgress();
     *   if (progress.isDone()) {
     *     Chat.log("done");
     *   } else {
     *     Chat.log(`not done, ${progress.getUnobtainedCriteria().length} criteria left`);
     *   }
     * }
     * </pre>
     *
     * @return the progress of the given advancement.
     * @since 1.8.4
     */
    @DocletReplaceParams("identifier: CanOmitNamespace<AdvancementId>")
    public AdvancementProgressHelper getAdvancementProgress(String identifier) {
        assert Minecraft.getInstance().player != null;
        return new AdvancementProgressHelper(((MixinClientAdvancementManager) Minecraft.getInstance().player.connection.getAdvancements()).getAdvancementProgresses().get(base.get(RegistryHelper.parseIdentifier(identifier)).holder()));
    }

    /**
     * the connection's own progress store, as a stream of advancement to progress.
     * <p>
     * Every list that needs the player's progress goes through here, which is why they all want
     * a player: the store lives on the connection rather than on the tree.
     */
    private Stream<Map.Entry<AdvancementHolder, AdvancementProgress>> getProgressStream() {
        LocalPlayer player = Minecraft.getInstance().player;
        assert player != null;
        return ((MixinClientAdvancementManager) player.connection.getAdvancements()).getAdvancementProgresses().entrySet().stream();
    }

    @Override
    public String toString() {
        return String.format("AdvancementManagerHelper:{\"started\": %d, \"missing\": %d, \"completed\": %d}", getStartedAdvancements().size(), getMissingAdvancements().size(), getCompletedAdvancements().size());
    }

}
