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
 * one objective on a scoreboard, which is the thing that holds a column of scores: the
 * {@code kills} on a server's sidebar is an objective, and the players down it with
 * their numbers are that objective's scores.
 * <p>
 * The two ways of reading the scores here differ in what they leave out.
 * {@link #getPlayerScores()} and {@link #scoreToDisplayName()} go through every entry
 * the server has sent, whereas {@link #getTexts()} leaves out the entries whose owner
 * name starts with a hash, sorts what is left by score, and keeps only the top fifteen.
 * A hash-prefixed name is how the game marks a score that belongs to something that is
 * not a player, such as a fake player, and {@code getTexts()} drops those.
 * <p>
 * An objective is reached through {@code World.getScoreboards()}, which is the way to
 * the sidebar and to each slot on it. It is a read: nothing here changes a score, and
 * the display names come back with whatever the owning team has done to them, such as
 * colouring the name or replacing it outright.
 * example:
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
 *     // and the whole set, keyed by player name
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

    public ScoreboardObjectiveHelper(Objective o) {
        super(o);
    }

    /**
     * every score on this objective, as a map from the owner name to the score. The
     * names are the raw scoreboard names rather than display names, so they are what a
     * team has not had anything done to them.
     * <p>
     * The map is built in the order the scoreboard handed the scores over rather than
     * sorted, and it is a new map each call, so writing to it does not change the
     * scoreboard. Entries whose owner is not a player are in here as well, which is the
     * difference from {@link #getTexts()}.
     * example:
     * <pre>
     * const boards = World.getScoreboards();
     * if (boards !== null) {
     *   const sidebar = boards.getCurrentScoreboard();
     *   if (sidebar !== null) {
     *     const scores = sidebar.getPlayerScores();
     *     Chat.log(`${scores.size()} entries`);
     *     for (const entry of scores.entrySet()) {
     *       Chat.log(`  ${entry.getKey()} on ${entry.getValue()}`);
     *     }
     *   }
     * }
     * </pre>
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
     * every score on this objective, as a map from the score to the name to show for it.
     * The name is the one the owning team produces, so a team that has coloured a name
     * or replaced it outright changes what comes back here, and the text is a wrapper
     * rather than a plain string so that colouring survives.
     * <p>
     * The map is keyed by the score rather than by the player, which is the opposite of
     * {@link #getPlayerScores()}, so two players on the same score land on one key and
     * only one of them is in the result. Nothing here sorts, and the map is a new one
     * each call.
     * example:
     * <pre>
     * const boards = World.getScoreboards();
     * if (boards !== null) {
     *   const sidebar = boards.getCurrentScoreboard();
     *   if (sidebar !== null) {
     *     for (const entry of sidebar.scoreToDisplayName().entrySet()) {
     *       // keyed by score, so two players on one score share a key
     *       Chat.log(`${entry.getKey()}: ${entry.getValue().getString()}`);
     *     }
     *   }
     * }
     * </pre>
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
     * the names the sidebar draws for this objective, highest score first. This is the
     * game's own pipeline for that list rather than a likeness of it: the same filter on
     * hidden entries, the same ordering and the same cut, so what comes back is the
     * sidebar's list of names.
     * <p>
     * Three things are left out by that pipeline, and they are what makes this the
     * sidebar's list rather than the scoreboard's. An entry whose owner name starts with
     * a hash is dropped, which is how the game marks a score that is not a player's.
     * Ties on the score are broken by owner name, ignoring case, so the order is the
     * same every time rather than depending on what arrived first. The list is then cut
     * to fifteen entries, which is the same cut the sidebar's own drawing makes.
     * <p>
     * The scores themselves are not in here. Each name goes through its owning team's
     * formatting, the same as on the sidebar, but the number beside it is not, so a
     * score has to be read from {@link #getPlayerScores()} or
     * {@link #scoreToDisplayName()}. A list is built and handed back each call, so it
     * is a snapshot rather than a view.
     * example:
     * <pre>
     * const boards = World.getScoreboards();
     * if (boards !== null) {
     *   const sidebar = boards.getCurrentScoreboard();
     *   if (sidebar !== null) {
     *     // at most fifteen names, highest score first
     *     const texts = sidebar.getTexts();
     *     for (let i = 0; i !== texts.size(); i += 1) {
     *       Chat.log(`  ${texts.get(i).getString()}`);
     *     }
     *     // the whole set is on the other reader, uncut and unsorted
     *     Chat.log(`the objective holds ${sidebar.getPlayerScores().size()} entries`);
     *   }
     * }
     * </pre>
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
     * every name the scoreboard is keeping a score for, whether or not this objective
     * has one for that name. This is the scoreboard's own list of who it is tracking
     * rather than anything about this objective, so a name can be in here with no entry
     * in {@link #getPlayerScores()}.
     * <p>
     * The names are the raw scoreboard names, with no team formatting applied, and the
     * list is a new one each call.
     * example:
     * <pre>
     * const boards = World.getScoreboards();
     * if (boards !== null) {
     *   const sidebar = boards.getCurrentScoreboard();
     *   if (sidebar !== null) {
     *     const tracked = sidebar.getKnownPlayers();
     *     const scored = sidebar.getPlayerScores();
     *     for (const name of tracked) {
     *       // tracked by the scoreboard, which is not the same as having a score here
     *       Chat.log(`${name}: ${scored.containsKey(name) ? "on the board" : "no score"}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the names the scoreboard is tracking
     * @since 1.7.0
     */
    public List<String> getKnownPlayers() {
        return base.getScoreboard().getTrackedPlayers().stream().map(ScoreHolder::getScoreboardName).toList();
    }

    /**
     * the display name for each name the scoreboard is tracking, in the same order as
     * {@link #getKnownPlayers()} and covering the same names. A name that has a display
     * name of its own gets that, and one that has not gets its scoreboard name as
     * literal text instead, so every entry has something rather than some being empty.
     * <p>
     * A display name set here is one the player chose for themselves rather than
     * anything a team has done, which is the difference from
     * {@link #scoreToDisplayName()}.
     * example:
     * <pre>
     * const boards = World.getScoreboards();
     * if (boards !== null) {
     *   const sidebar = boards.getCurrentScoreboard();
     *   if (sidebar !== null) {
     *     const names = sidebar.getKnownPlayersDisplayNames();
     *     for (let i = 0; i !== names.size(); i += 1) {
     *       Chat.log(`${sidebar.getKnownPlayers().get(i)} shows as `
     *         + `${names.get(i).getString()}`);
     *     }
     *   }
     * }
     * </pre>
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
     * the objective's name, which is the identifier the server and the commands use
     * rather than anything a player sees. The sidebar's title is
     * {@link #getDisplayName()} and the two are often different, the name being a short
     * key and the display name being the text drawn above the column.
     * example:
     * <pre>
     * const boards = World.getScoreboards();
     * if (boards !== null) {
     *   // nineteen display slots, zero to eighteen
     *   for (let slot = 0; slot !== 19; slot += 1) {
     *     const objective = boards.getObjectiveSlot(slot);
     *     if (objective !== null) {
     *       // the name is a key, the display name is what is drawn
     *       Chat.log(`slot ${slot}: ${objective.getName()} shown as `
     *         + `"${objective.getDisplayName().getString()}"`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return name of scoreboard
     * @since 1.2.9
     */
    public String getName() {
        return base.getName();
    }

    /**
     * the text drawn above the column of scores, as a text wrapper rather than a plain
     * string, so the colouring survives. This is the title the objective's owner set and
     * not the same thing as {@link #getName()}, which is the key the commands use.
     * <p>
     * A list is built and handed back each call, and the same text can be read again
     * from {@link #getTexts()}'s neighbours rather than from here, so this is the one to
     * read when a script wants the sidebar's own heading.
     * example:
     * <pre>
     * const boards = World.getScoreboards();
     * if (boards !== null) {
     *   const sidebar = boards.getCurrentScoreboard();
     *   if (sidebar !== null) {
     *     // the heading as it is drawn, and the same text stripped of its styling
     *     Chat.log(sidebar.getDisplayName());
     *     Chat.log(sidebar.getDisplayName().getStringStripFormatting());
     *   }
     * }
     * </pre>
     *
     * @return the text drawn above this objective's scores
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
