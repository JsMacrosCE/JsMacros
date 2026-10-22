package com.jsmacrosce.jsmacros.client.api.helper.screen;

import com.google.common.collect.ImmutableList;
import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An objective holds scores for scoreboard owners and can be displayed in a scoreboard slot.
 * <p>
 * {@link #getPlayerScores()} maps raw owner names to scores; repeated names overwrite earlier
 * entries. {@link #getTexts()} omits hidden entries,
 * orders the rest by descending score then case-insensitive owner name, and limits the result
 * to fifteen. Both display-name methods apply team formatting.
 * <pre>
 * const boards = World.getScoreboards();
 * if (boards !== null) {
 *   const sidebar = boards.getCurrentScoreboard();
 *   if (sidebar !== null) {
 *     Chat.log(`${sidebar.getName()}: ${sidebar.getDisplayName().getString()}`);
 *     // the fifteen the sidebar would draw, highest first
 *     const texts = sidebar.getTexts();
 *     for (let i = 0; i !== texts.size(); i += 1) {
 *       Chat.log(`  ${texts.get(i).getString()}`);
 *     }
 *     // scores keyed by raw owner name (repeated names overwrite)
 *     for (const entry of sidebar.getPlayerScores().entrySet()) {
 *       Chat.log(`  ${entry.getKey()} on ${entry.getValue()}`);
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.2.9
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class ScoreboardObjectiveHelper extends BaseHelper<Objective> {
    private static final Comparator<PlayerScoreEntry> SCOREBOARD_ENTRY_COMPARATOR = Comparator.comparing(PlayerScoreEntry::value).reversed().thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);

    /** Wraps the supplied scoreboard objective.
     * @param o raw scoreboard objective to wrap
     */
    public ScoreboardObjectiveHelper(Objective o) {
        super(o);
    }

    /**
     * Returns a new map of raw scoreboard owner names to their scores, including hidden
     * entries. The map preserves the iteration order supplied by the scoreboard; modifying
     * it does not modify scoreboard state.
     *
     * @return player name to score map
     */
    public Map<String, Integer> getPlayerScores() {
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (PlayerScoreEntry pl : base.getScoreboard().listPlayerScores(base)) {
            scores.put(pl.owner(), pl.value());
        }
        return scores;
    }

    /**
     * Returns a new map from score to team-formatted owner text. Since the score is the key,
     * owners with equal scores overwrite one another; iteration is not sorted.
     *
     * @return a map from score to the name to display for it
     * @since 1.8.0
     */
    public Map<Integer, TextHelper> scoreToDisplayName() {
        Map<Integer, TextHelper> scores = new LinkedHashMap<>();
        for (PlayerScoreEntry pl : base.getScoreboard().listPlayerScores(base)) {
            PlayerTeam team = base.getScoreboard().getPlayersTeam(pl.owner());
            scores.put(pl.value(), TextHelper.wrap(PlayerTeam.formatNameForTeam(team, pl.ownerName())));
        }
        return scores;
    }

    /**
     * Returns team-formatted owner names for visible entries, ordered by descending score
     * then case-insensitive owner name and limited to fifteen. Scores are not included;
     * use {@link #getPlayerScores()} or {@link #scoreToDisplayName()} to retrieve them.
     *
     * @return the names the sidebar draws for this objective, highest score first
     * @since 2.0.0
     */
    public List<TextHelper> getTexts() {
        return base.getScoreboard().listPlayerScores(base).stream()
                .filter(ent -> !ent.isHidden())
                .sorted(SCOREBOARD_ENTRY_COMPARATOR)
                .limit(15L)
                .map(ent -> {
                    PlayerTeam team = base.getScoreboard().getPlayersTeam(ent.owner());
                    return TextHelper.wrap(PlayerTeam.formatNameForTeam(team, ent.ownerName()));
                })
                .toList();
    }

    /**
     * Returns the scoreboard's tracked owner names, whether or not each has a score on this
     * objective. Names are raw scoreboard names, and the returned list is newly built.
     *
     * @return the names the scoreboard is tracking
     * @since 1.7.0
     */
    public List<String> getKnownPlayers() {
        return base.getScoreboard().getTrackedPlayers().stream().map(ScoreHolder::getScoreboardName).toList();
    }

    /**
     * Returns display names for tracked owners in {@link #getKnownPlayers()} order. Owners
     * without a custom display name use their scoreboard name as literal text; team formatting
     * is not applied.
     *
     * @return the display name for each tracked player
     * @since 1.8.0
     */
    public List<TextHelper> getKnownPlayersDisplayNames() {
        return ImmutableList.copyOf(base.getScoreboard().getTrackedPlayers()).stream()
                .map(e -> e.getDisplayName() != null ? TextHelper.wrap(e.getDisplayName()) : TextHelper.wrap(Component.literal(e.getScoreboardName())))
                .collect(Collectors.toList());
    }

    /**
     * Returns the objective's identifier, distinct from its display name.
     *
     * @return name of scoreboard
     * @since 1.2.9
     */
    public String getName() {
        return base.getName();
    }

    /**
     * Returns the objective's display title as a text wrapper, preserving its styling.
     *
     * @return the text drawn above this objective's scores
     * @since 1.2.9
     */
    public TextHelper getDisplayName() {
        return TextHelper.wrap(base.getDisplayName());
    }

    /** Returns a diagnostic representation containing the objective identifier and display-name helper representation.
     * @return diagnostic representation of the scoreboard objective
     */
    @Override
    public String toString() {
        return String.format("ScoreboardObjectiveHelper:{\"name\": \"%s\", \"displayName\": \"%s\"}", getName(), getDisplayName());
    }

}
