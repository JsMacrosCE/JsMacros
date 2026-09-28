package com.jsmacrosce.jsmacros.client.api.helper;

import com.google.common.collect.Iterables;

import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinAdvancementProgress;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * how far the player has got on one advancement.
 * <p>
 * This is the player's half of an advancement, split off from the {@link AdvancementHelper} that
 * describes the advancement itself, and it is where the two questions of "what does this
 * advancement need" and "which of those have happened" are answered. What it needs is on
 * {@link AdvancementHelper#getRequirements()}; what has happened is here.
 * <p>
 * The unit of progress is a <b>criterion</b>, one condition such as having mined a block. An
 * advancement has several criteria, and the game groups them. <b>All</b> of the groups have to
 * be satisfied for the advancement to be done, and <b>any one</b> criterion inside a group
 * satisfies that group. An advancement that puts every criterion in a group of its own
 * collapses the two rules into "every criterion is done", but that is the rarer shape in the
 * vanilla advancements: of the 1520 files shipped with 1.21.8, 1409 have at least one group
 * holding more than one criterion.
 * That is why there is a {@link #getPercentage()} and a {@link #getFraction()} as well as a
 * plain count — the fraction is the game's own "2/3" style text, which it works out from the
 * groups, and it is what the advancements tab prints.
 * <p>
 * A criterion is only "obtained" once, at the moment it was first met, and that moment is kept:
 * {@link #getCriteria()} and {@link #getCriterionProgress(String)} report when, as
 * milliseconds since the epoch. The lists of obtained and unobtained criteria are what a script
 * wants when it is working out what is left to do.
 * example:
 * <pre>
 * const player = Player.getPlayer();
 * if (player !== null) {
 *   const manager = player.getAdvancementManager();
 *   const progress = manager.getAdvancementProgress("minecraft:story/mine_stone");
 *
 *   if (progress.isDone()) {
 *     Chat.log("already earned");
 *   } else {
 *     // the game's own "1/2" style text, straight from the tab, and null
 *     // when the advancement has a single group so guard it
 *     const fraction = progress.getFraction();
 *     Chat.log(`progress: ${fraction === null ? "part way" : fraction.getString()}`);
 *
 *     // and what is actually left
 *     for (const criterion of progress.getUnobtainedCriteria()) {
 *       Chat.log(`still to do: ${criterion}`);
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
public class AdvancementProgressHelper extends BaseHelper<AdvancementProgress> {

    public AdvancementProgressHelper(AdvancementProgress base) {
        super(base);
    }

    /**
     * whether every group of criteria in this advancement has been satisfied.
     * <p>
     * This is the whole test, groups included: <b>every</b> group has to have at least one of
     * its criteria done before the game marks the advancement done. Where each criterion has a
     * group of its own that is the same as all criteria being done; where several criteria
     * share a group the advancement is done as soon as one of them in each group is, which is
     * the more common shape in the vanilla advancements.
     * <p>
     * An advancement with no criteria at all is never done by this test. That is not a
     * vacuous pass: the game checks the requirement list first and an empty one fails
     * outright, so there is no case where an advancement with nothing to do counts as done.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const progress = manager.getAdvancementProgress("minecraft:story/root");
     * if (progress.isDone()) {
     *   Chat.log("root is done");
     * }
     * </pre>
     *
     * @return {@code true} if the advancement is finished, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isDone() {
        return base.isDone();
    }

    /**
     * whether any criterion has been met at all, whether or not the advancement is finished.
     * <p>
     * This separates "partly done" from "not started", which {@link #isDone()} cannot: an
     * advancement that is not done has either been started or not touched, and this is what
     * tells the two apart. An advancement that is done always also answers {@code true} here.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * // the advancements actually part way through, as opposed to untouched
     * for (const adv of manager.getMissingAdvancements()) {
     *   const progress = adv.getProgress();
     *   if (progress.isAnyObtained()) {
     *     Chat.log(`partly done: ${adv.getId()}`);
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if any criteria has already been met, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAnyObtained() {
        return base.hasProgress();
    }

    /**
     * every criterion that has been met, with the moment each was met.
     * <p>
     * The value is milliseconds since the epoch, and it is the moment the criterion was
     * <i>first</i> met, so re-completing a criterion does not move it. Criteria that have not
     * been met are not in the map at all, which is what makes this the complement of
     * {@link #getUnobtainedCriteria()}.
     * <p>
     * The map is built fresh on every call, so a script that wants to watch progress over time
     * should read this again rather than hold on to it.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const progress = manager.getAdvancementProgress("minecraft:story/mine_stone");
     * for (const entry of progress.getCriteria().entrySet()) {
     *   const when = new Date(entry.getValue());
     *   Chat.log(`${entry.getKey()} was met at ${when.toISOString()}`);
     * }
     * </pre>
     *
     * @return a map of every criterion that has been met, and when it was met.
     * @since 1.8.4
     */
    public Map<String, Long> getCriteria() {
        return ((MixinAdvancementProgress) base).getCriteriaProgresses().entrySet().stream().filter(e -> e.getValue().getObtained() != null).collect(Collectors.toMap(
                Map.Entry::getKey,
                criterionProgressEntry -> criterionProgressEntry.getValue().getObtained().toEpochMilli()
        ));
    }

    /**
     * all requirements of this advancement.
     * <p>
     * The same grouping {@link AdvancementHelper#getRequirements()} gives for the advancement
     * itself, read from the progress's own copy of it. The outer list is the groups and
     * <b>every</b> group has to be satisfied; the entries inside a group are alternatives, so
     * any one of them being done satisfies that group. An advancement that puts each criterion
     * in a group of its own has a single name per group and so no alternatives inside it,
     * which is the rarer shape in the vanilla advancements.
     * <br>
     * The inner lists are the game's own objects, so a script should read them rather than write
     * to them.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const progress = manager.getAdvancementProgress("minecraft:story/mine_stone");
     * for (const group of progress.getRequirements()) {
     *   // any one of these being done is enough for this group
     *   const left = group.filter(function (c) { return !progress.isCriteriaObtained(c); });
     *   if (left.length === 0) {
     *     Chat.log("this group is already satisfied");
     *   }
     * }
     * </pre>
     *
     * @return all requirements of this advancement.
     * @since 1.8.4
     */
    public List<List<String>> getRequirements() {
        return ((MixinAdvancementProgress) base).getRequirements().requirements();
    }

    /**
     * how much of this advancement is done, as a fraction from 0 to 1.
     * <p>
     * This is the number behind the bar the advancements tab draws, and it is the count of
     * satisfied groups divided by the number of groups, not the count of satisfied criteria
     * divided by the number of criteria. Where each criterion has a group of its own the two
     * are the same number; where a group holds several alternatives, a single satisfied
     * criterion moves this by a whole group's worth, and that is the common case in the
     * vanilla advancements.
     * <br>
     * For text to show a player rather than a number, {@link #getFraction()} is the same value
     * already formatted — except that it is {@code null} for an advancement with a single
     * group, so it cannot stand in for this one everywhere.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const progress = manager.getAdvancementProgress("minecraft:story/mine_stone");
     * const bar = "=".repeat(Math.round(progress.getPercentage() * 20));
     * Chat.log(`[${bar}] ${Math.round(progress.getPercentage() * 100)} percent`);
     * </pre>
     *
     * @return the percentage of finished requirements.
     * @since 1.8.4
     */
    public float getPercentage() {
        return base.getPercent();
    }

    /**
     * the same progress as {@link #getPercentage()}, already written the way the tab writes it.
     * <p>
     * This is the game's own text, so it is the exact wording the advancements tab shows, in
     * the client's language, and it is what a script should show a player rather than a bare
     * number.
     * <br>
     * <b>It is {@code null} for almost every advancement.</b> The game only produces this text
     * when there are at least two groups to count, and it also declines when there are no
     * criteria at all. A single group is overwhelmingly the usual case: of the 1520 advancement
     * files shipped with 1.21.8, 1510 have exactly one group and only 10 have two or more, so
     * this answers {@code null} for about 99 percent of the vanilla advancements. That single
     * group is usually an explicit group of two alternatives rather than one criterion on its
     * own. A script must check for the {@code null} before using it;
     * {@link #getPercentage()} always has a number.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const progress = manager.getAdvancementProgress("minecraft:story/mine_stone");
     * const fraction = progress.getFraction();
     * if (fraction !== null) {
     *   Chat.actionbar(fraction);
     * } else {
     *   // the game has no "1/1" text, so use the number instead
     *   Chat.actionbar(`${Math.round(progress.getPercentage() * 100)} percent`);
     * }
     * </pre>
     *
     * @return the fraction of finished requirements to total requirements.
     * @since 1.8.4
     */
    public TextHelper getFraction() {
        return TextHelper.wrap(base.getProgressText());
    }

    /**
     * how many groups of criteria are satisfied.
     * <p>
     * This counts <b>groups</b>, not criteria: a group counts once any of its criteria are done,
     * however many of them are. It is the numerator of {@link #getPercentage()}, so together
     * with {@link AdvancementHelper#getRequirementCount()} on the advancement it accounts for
     * the whole fraction.
     * <br>
     * A count of criteria themselves is not available here; {@link #getObtainedCriteria()} and
     * {@link #getUnobtainedCriteria()} are the two lists, and their lengths add up to the total.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const adv = manager.getAdvancement("minecraft:story/mine_stone");
     * const progress = adv.getProgress();
     * const total = adv.getRequirementCount();
     * Chat.log(`${progress.countObtainedRequirements()} of ${total} groups satisfied`);
     * </pre>
     *
     * @return the amount of requirements criteria.
     * @since 1.8.4
     */
    public int countObtainedRequirements() {
        return ((MixinAdvancementProgress) base).invokeCountObtainedRequirements();
    }

    /**
     * the criteria that have not been met yet.
     * <p>
     * This is the complement of {@link #getObtainedCriteria()} across all of the advancement's
     * criteria, and it is what a script wants when it is working out what is left to do. It is
     * empty for a finished advancement, and it is not empty for one that has not been started
     * at all, where every criterion is listed.
     * <p>
     * The entries are the same names {@link #getRequirements()} uses, but note that one of these
     * being listed does not mean a group is unreachable: on an advancement that groups several
     * criteria together, only one of them in each group has to be done.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const progress = manager.getAdvancementProgress("minecraft:story/mine_stone");
     * const left = progress.getUnobtainedCriteria();
     * if (left.length === 0) {
     *   Chat.log("nothing left");
     * } else {
     *   Chat.log(`left: ${left.join(", ")}`);
     * }
     * </pre>
     *
     * @return the amount/values of missing criteria.
     * @since 1.8.4
     */
    public String[] getUnobtainedCriteria() {
        return Iterables.toArray(base.getRemainingCriteria(), String.class);
    }

    /**
     * the criteria that have been met, as names only.
     * <p>
     * The names half of {@link #getCriteria()}; use that one instead when the moment each was
     * met matters as well. A criterion appears here once it has been met, and the game keeps it
     * that way even if the condition stops being true afterwards.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const progress = manager.getAdvancementProgress("minecraft:story/mine_stone");
     * for (const criterion of progress.getObtainedCriteria()) {
     *   Chat.log(`done: ${criterion}`);
     * }
     * </pre>
     *
     * @return the ids of the finished requirements.
     * @since 1.8.4
     */
    public String[] getObtainedCriteria() {
        return Iterables.toArray(base.getCompletedCriteria(), String.class);
    }

    /**
     * when the first of this advancement's criteria was met.
     * <p>
     * This is the earliest of the timestamps {@link #getCriteria()} reports, which is when the
     * player first touched this advancement rather than when they finished it. The time is
     * milliseconds since the epoch.
     * <br>
     * An advancement with nothing met yet has no first moment, and the call throws a
     * {@link java.lang.NullPointerException} rather than giving
     * {@code -1}, so {@link #isAnyObtained()} is not optional before it.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const progress = manager.getAdvancementProgress("minecraft:story/mine_stone");
     * if (progress.isAnyObtained()) {
     *   const started = new Date(progress.getEarliestProgressObtainDate());
     *   Chat.log(`first touched at ${started.toISOString()}`);
     * }
     * </pre>
     *
     * @return the earliest completion date of all criteria.
     * @throws NullPointerException if no criterion of this advancement has been met
     * @since 1.8.4
     */
    public long getEarliestProgressObtainDate() {
        return base.getFirstProgressDate().toEpochMilli();
    }

    /**
     * when one named criterion was met.
     * <p>
     * The moment is the first time the criterion was satisfied and is not moved afterwards.
     * <br>
     * The {@code -1} is for a name this advancement does not have as a criterion at all. A
     * criterion that <i>does</i> exist but has not been met yet is a different case and throws
     * a {@link java.lang.NullPointerException} rather than giving
     * {@code -1}, because there is no moment to take. To ask that question without risking the
     * throw, use {@link #isCriteriaObtained(String)} on a name already known to be one of this
     * advancement's, or check {@link #getCriteria()}.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const progress = manager.getAdvancementProgress("minecraft:story/mine_stone");
     * // only ask about a criterion this advancement actually has
     * for (const criterion of progress.getObtainedCriteria()) {
     *   const when = progress.getCriterionProgress(criterion);
     *   Chat.log(`${criterion} was met at ${new Date(when).toISOString()}`);
     * }
     * </pre>
     *
     * @param criteria the criteria
     * @return the completion date of the given criteria or {@code -1} if the criteria is not met
     * yet.
     * @throws NullPointerException if the criterion exists but has not been met
     * @since 1.8.4
     */
    public long getCriterionProgress(String criteria) {
        CriterionProgress progress = base.getCriterion(criteria);
        return progress == null ? -1 : progress.getObtained().toEpochMilli();
    }

    /**
     * whether one named criterion has been met, and when it was first met.
     * <p>
     * This has no "not met" answer: a name this advancement does not have, and a criterion
     * that exists but has not been satisfied, both throw rather than giving {@code false}. It
     * is the call to use on a criterion name that is already known to be one of this
     * advancement's, and {@link #getUnobtainedCriteria()} or {@link #getCriteria()} is how to
     * find out what those names are.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const progress = manager.getAdvancementProgress("minecraft:story/mine_stone");
     * // only ask about a criterion this advancement actually has
     * for (const group of progress.getRequirements()) {
     *   for (const criterion of group) {
     *     if (!progress.isCriteriaObtained(criterion)) {
     *       Chat.log(`not done: ${criterion}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @param criteria the criteria
     * @return {@code true} if the given criteria is met, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCriteriaObtained(String criteria) {
        return base.getCriterion(criteria).isDone();
    }

    @Override
    public String toString() {
        return String.format("AdvancementProgressHelper:{\"percent\": %f}", getPercentage());
    }

}
