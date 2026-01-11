package com.jsmacrosce.jsmacros.client.api.helper.screen;

import com.google.common.collect.ImmutableList;
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
 * @author Wagyourtail
 * @since 1.2.9
 */
@SuppressWarnings("unused")
public class ScoreboardObjectiveHelper extends BaseHelper<Objective> {
    private static final Comparator<PlayerScoreEntry> SCOREBOARD_ENTRY_COMPARATOR = Comparator.comparing(PlayerScoreEntry::value).reversed().thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);

    public ScoreboardObjectiveHelper(Objective o) {
        super(o);
    }

    /**
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
     * @return
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
     * @return
     * @since 1.7.0
     */
    public List<String> getKnownPlayers() {
        return base.getScoreboard().getTrackedPlayers().stream().map(ScoreHolder::getScoreboardName).toList();
    }

    /**
     * @return
     * @since 1.8.0
     */
    public List<TextHelper> getKnownPlayersDisplayNames() {
        return ImmutableList.copyOf(base.getScoreboard().getTrackedPlayers()).stream()
                .map(e -> e.getDisplayName() != null ? TextHelper.wrap(e.getDisplayName()) : TextHelper.wrap(Component.literal(e.getScoreboardName())))
                .collect(Collectors.toList());
    }

    /**
     * @return name of scoreboard
     * @since 1.2.9
     */
    public String getName() {
        return base.getName();
    }

    /**
     * @return name of scoreboard
     * @since 1.2.9
     */
    public TextHelper getDisplayName() {
        return TextHelper.wrap(base.getDisplayName());
    }

    @Override
    public String toString() {
        return String.format("ScoreboardObjectiveHelper:{\"name\": \"%s\", \"displayName\": \"%s\"}", getName(), getDisplayName());
    }

}
