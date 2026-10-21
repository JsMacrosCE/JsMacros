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
 * Progress of a player's advancement, including completed criteria and requirement groups.
 * <p>
 * Each group requires at least one completed criterion; all groups must be satisfied. Percentages
 * count satisfied groups, and obtained-criterion timestamps are milliseconds since the epoch.
 * <pre>
 * const player = Player.getPlayer();
 * if (player !== null) {
 *   const manager = player.getAdvancementManager();
 *   const progress = manager.getAdvancementProgress("minecraft:story/mine_stone");
 *   if (progress.getRaw() === null) { throw new Error("no progress entry"); }
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

    /** Wraps the supplied advancement progress by storing it as this helper's base value.
     * @param base raw advancement progress to wrap
     */
    public AdvancementProgressHelper(AdvancementProgress base) {
        super(base);
    }

    /**
     * Whether all requirement groups are satisfied. An empty requirement list is not done.
     *
     * @return {@code true} if the advancement is finished, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isDone() {
        return base.isDone();
    }

    /**
     * Whether at least one criterion has been completed, including for a finished advancement.
     *
     * @return {@code true} if any criteria has already been met, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAnyObtained() {
        return base.hasProgress();
    }

    /**
     * Completed criteria and their completion times in milliseconds since the epoch.
     * The returned map is newly collected on each call; unmet criteria are omitted.
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
     * Requirement groups: each inner list contains alternative criteria, and every group must
     * have at least one completed criterion.
     *
     * @return all requirements of this advancement.
     * @since 1.8.4
     */
    public List<List<String>> getRequirements() {
        return ((MixinAdvancementProgress) base).getRequirements().requirements();
    }

    /**
     * Fraction of requirement groups satisfied, from {@code 0} to {@code 1}; an empty
     * requirement set yields {@code 0}.
     *
     * @return the fraction of requirement groups satisfied.
     * @since 1.8.4
     */
    public float getPercentage() {
        return base.getPercent();
    }

    /**
     * Localized progress text for the requirement groups, or {@code null} when there are no
     * criteria or at most one requirement group. See {@link #getPercentage()} for a numeric value.
     *
     * @return localized progress text, or {@code null} when unavailable.
     * @since 1.8.4
     */
    public TextHelper getFraction() {
        return TextHelper.wrap(base.getProgressText());
    }

    /**
     * Number of requirement groups for which at least one criterion is completed.
     *
     * @return the number of satisfied requirement groups.
     * @since 1.8.4
     */
    public int countObtainedRequirements() {
        return ((MixinAdvancementProgress) base).invokeCountObtainedRequirements();
    }

    /**
     * Names of criteria that have not been completed.
     *
     * @return the names of incomplete criteria.
     * @since 1.8.4
     */
    public String[] getUnobtainedCriteria() {
        return Iterables.toArray(base.getRemainingCriteria(), String.class);
    }

    /**
     * Names of completed criteria.
     *
     * @return the names of completed criteria.
     * @since 1.8.4
     */
    public String[] getObtainedCriteria() {
        return Iterables.toArray(base.getCompletedCriteria(), String.class);
    }

    /**
     * Earliest completion time among this advancement's criteria, in milliseconds since the
     * epoch. Throws {@link NullPointerException} if no criterion has been completed.
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const manager = player.getAdvancementManager();
     * const progress = manager.getAdvancementProgress("minecraft:story/mine_stone");
     * if (progress.getRaw() === null) { throw new Error("no progress entry"); }
     * if (progress.isAnyObtained()) {
     *   const started = new Date(progress.getEarliestProgressObtainDate());
     *   Chat.log(`first touched at ${started.toISOString()}`);
     * }
     * </pre>
     *
     * @return the earliest completion date of all criteria.
     * @since 1.8.4
     */
    public long getEarliestProgressObtainDate() {
        return base.getFirstProgressDate().toEpochMilli();
    }

    /**
     * Completion time for a named criterion, in milliseconds since the epoch. An unknown name
     * returns {@code -1}; a known but incomplete criterion throws {@link NullPointerException}.
     *
     * @param criteria criterion name
     * @return completion time in milliseconds since the epoch, or {@code -1} if the name is unknown
     * @since 1.8.4
     */
    public long getCriterionProgress(String criteria) {
        CriterionProgress progress = base.getCriterion(criteria);
        return progress == null ? -1 : progress.getObtained().toEpochMilli();
    }

    /**
     * Whether a named criterion is completed. An unknown criterion name throws
     * {@link NullPointerException}.
     *
     * @param criteria criterion name
     * @return {@code true} if completed; {@code false} if the criterion is known but incomplete
     * @since 1.8.4
     */
    public boolean isCriteriaObtained(String criteria) {
        return base.getCriterion(criteria).isDone();
    }

    /** Returns a diagnostic representation containing the advancement progress percentage.
     * @return diagnostic representation of advancement progress
     */
    @Override
    public String toString() {
        return String.format("AdvancementProgressHelper:{\"percent\": %f}", getPercentage());
    }

}
