package com.jsmacrosce.jsmacros.client.api.helper.world;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.world.scores.PlayerTeam;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.FormattingHelper;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * A scoreboard team: the group the game uses for colouring names, deciding who can hurt whom, and
 * deciding whose nametags are hidden.<br>
 * A team is reached either from a player, through {@link PlayerListEntryHelper#getTeam()} for
 * somebody in the tab list, or from the scoreboard, through {@link ScoreboardsHelper#getTeams()}.
 * The team carries a name the server chose, a display name that may be anything at all, a colour
 * that may be none, and a member list that the server owns.
 * <p>
 * The colour is spelled four ways here and they answer different questions:
 * {@link #getColorFormat()} is the formatting itself, {@link #getColorIndex()} is the small number
 * a command uses, {@link #getColorValue()} is the packed colour for rendering and answers
 * {@code -1} when there is no colour, and {@link #getColorName()} is the word.
 * example:
 * <pre>
 * const board = World.getScoreboards();
 * if (board !== null) {
 *   const teams = board.getTeams();
 *   for (let i = 0; i !== teams.size(); i += 1) {
 *     const team = teams.get(i);
 *     Chat.log(`${team.getName()} is shown as ${team.getDisplayName().getString()}`);
 *     Chat.log(`  colour ${team.getColorName()}, value ${team.getColorValue()}`);
 *     Chat.log(`  ${team.getPlayerList().size()} members, prefix ${team.getPrefix()}`);
 *   }
 * }
 *
 * // and the team a particular player is on, which is null when they are on none
 * const entry = World.getPlayerEntry("Notch");
 * if (entry !== null) {
 *   const team = entry.getTeam();
 *   Chat.log(team === null ? "not in a team" : `on ${team.getName()}`);
 * }
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.3.0
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class TeamHelper extends BaseHelper<PlayerTeam> {
    public TeamHelper(PlayerTeam t) {
        super(t);
    }

    /**
     * The team's name as the server registered it, which is the plain name a command uses and not
     * what players see. It has nothing to do with {@link #getDisplayName()}, which the server is
     * free to set to anything.
     * example:
     * <pre>
     * const board = World.getScoreboards();
     * if (board !== null) {
     *   const teams = board.getTeams();
     *   for (let i = 0; i !== teams.size(); i += 1) {
     *     Chat.log(teams.get(i).getName());
     *   }
     * }
     * </pre>
     *
     * @return the team's registered name.
     * @since 1.3.0
     */
    public String getName() {
        return base.getName();
    }

    /**
     * The name the game shows for the team in a list of teams, which the server chooses and which
     * is often nothing like {@link #getName()}. A team can be registered under one name and shown
     * under another.
     * example:
     * <pre>
     * // the name a player would see, against the one a command would use
     * const board = World.getScoreboards();
     * if (board !== null) {
     *   const teams = board.getTeams();
     *   for (let i = 0; i !== teams.size(); i += 1) {
     *     const team = teams.get(i);
     *     Chat.log(`${team.getName()} shows as ${team.getDisplayName().getString()}`);
     *   }
     * }
     * </pre>
     *
     * @return the display name of this team.
     * @since 1.3.0
     */
    public TextHelper getDisplayName() {
        return TextHelper.wrap(base.getDisplayName());
    }

    /**
     * The names of the players on this team, as a copy rather than the team's own set, so changing
     * the list that comes back does not change the team. The names are the scoreboard names the
     * server holds, which for a real player is the account name.
     * example:
     * <pre>
     * // who is on a team, and how many
     * const board = World.getScoreboards();
     * if (board !== null) {
     *   const teams = board.getTeams();
     *   for (let i = 0; i !== teams.size(); i += 1) {
     *     const team = teams.get(i);
     *     const members = team.getPlayerList();
     *     if (members.size() > 0) {
     *       Chat.log(`${team.getName()}: ${members.size()}`);
     *       for (let j = 0; j !== members.size(); j += 1) {
     *         Chat.log(`  ${members.get(j)}`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return a copy of the list of member names.
     * @since 1.3.0
     */
    public List<String> getPlayerList() {
        return new ArrayList<>(base.getPlayers());
    }

    /**
     * The team's colour as a formatting, which is what to use for building a text in the same
     * colour. A team with no colour set still has a formatting here, and it is the one the game
     * falls back to rather than nothing at all, so {@link #getColorValue()} is the call that
     * actually says whether a colour was set.
     * example:
     * <pre>
     * // build a label in a team's own colour
     * const board = World.getScoreboards();
     * if (board !== null) {
     *   const teams = board.getTeams();
     *   for (let i = 0; i !== teams.size(); i += 1) {
     *     const team = teams.get(i);
     *     Chat.actionbar(Chat.createTextBuilder()
     *       .append(team.getName())
     *       .withFormatting(team.getColorFormat())
     *       .build());
     *   }
     * }
     * </pre>
     *
     * @return the formatting of this team's color.
     * @since 1.8.4
     */
    public FormattingHelper getColorFormat() {
        return new FormattingHelper(base.getColor());
    }

    /**
     * The team's colour index, the small number a command takes.
     *
     * @return the same number as {@link #getColorIndex()}.
     * @since 1.3.0
     * @deprecated use {@link #getColorIndex()} instead.
     */
    @Deprecated
    public int getColor() {
        return getColorIndex();
    }

    /**
     * The team's colour as the small number the game uses for it, which is what a command takes.
     * This is the index rather than the packed colour {@link #getColorValue()} gives, and the two
     * are not the same number.
     * example:
     * <pre>
     * // the index, and the word that goes with it
     * const board = World.getScoreboards();
     * if (board !== null) {
     *   const teams = board.getTeams();
     *   for (let i = 0; i !== teams.size(); i += 1) {
     *     const team = teams.get(i);
     *     Chat.log(`${team.getName()}: index ${team.getColorIndex()}, name ${team.getColorName()}`);
     *   }
     * }
     * </pre>
     *
     * @return the color index of this team.
     * @since 1.8.4
     */
    public int getColorIndex() {
        return base.getColor().getId();
    }

    /**
     * The packed colour value for rendering, or {@code -1} when the team has no colour of its own.
     * This is the number to hand to a renderer, and the {@code -1} is what tells a team apart from
     * a team whose colour happens to be white.
     * example:
     * <pre>
     * // which teams actually have a colour of their own
     * const board = World.getScoreboards();
     * if (board !== null) {
     *   const teams = board.getTeams();
     *   for (let i = 0; i !== teams.size(); i += 1) {
     *     const team = teams.get(i);
     *     const value = team.getColorValue();
     *     if (value === -1) {
     *       Chat.log(`${team.getName()} has no colour`);
     *     } else {
     *       Chat.log(`${team.getName()} is 0x${value.toString(16)}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the color value for this team or {@code -1} if it has no color.
     * @since 1.8.4
     */
    public int getColorValue() {
        return base.getColor().getColor() == null ? -1 : base.getColor().getColor();
    }

    /**
     * The name of the team's colour, which is the word a command would use for it.
     * example:
     * <pre>
     * const entry = World.getPlayerEntry("Notch");
     * if (entry !== null) {
     *   const team = entry.getTeam();
     *   if (team !== null) {
     *     Chat.log(`that player's team colour is ${team.getColorName()}`);
     *   }
     * }
     * </pre>
     *
     * @return the name of this team's color.
     * @since 1.8.4
     */
    @DocletReplaceReturn("FormattingColorName")
    public String getColorName() {
        return base.getColor().getName();
    }

    /**
     * The scoreboard this team belongs to, which is the way back out to the objectives and to the
     * other teams.
     * example:
     * <pre>
     * // from a player to their team and on to the scoreboard
     * const entry = World.getPlayerEntry("Notch");
     * if (entry !== null) {
     *   const team = entry.getTeam();
     *   if (team !== null) {
     *     const board = team.getScoreboard();
     *     Chat.log(`${team.getName()} is one of ${board.getTeams().size()} teams`);
     *   }
     * }
     * </pre>
     *
     * @return the scoreboard including this team.
     * @since 1.8.4
     */
    public ScoreboardsHelper getScoreboard() {
        return new ScoreboardsHelper(base.getScoreboard());
    }

    /**
     * The text the game puts in front of every name on this team, as the server set it. It can be
     * empty, and it can carry formatting, so it is a text rather than a string.
     * example:
     * <pre>
     * // the prefix and suffix that go on a player's name
     * const entry = World.getPlayerEntry("Notch");
     * if (entry !== null) {
     *   const team = entry.getTeam();
     *   if (team !== null) {
     *     Chat.log(`[${team.getPrefix()}]Notch[${team.getSuffix()}]`);
     *   }
     * }
     * </pre>
     *
     * @return the prefix set on this team.
     * @since 1.3.0
     */
    public TextHelper getPrefix() {
        return TextHelper.wrap(base.getPlayerPrefix());
    }

    /**
     * The text the game puts after every name on this team, as the server set it. Like the prefix it
     * can be empty and can carry formatting.
     * example:
     * <pre>
     * // the full decorated name the game would draw
     * const board = World.getScoreboards();
     * if (board !== null) {
     *   const teams = board.getTeams();
     *   for (let i = 0; i !== teams.size(); i += 1) {
     *     const team = teams.get(i);
     *     Chat.log(`${team.getPrefix()}${team.getName()}${team.getSuffix()}`);
     *   }
     * }
     * </pre>
     *
     * @return the suffix set on this team.
     * @since 1.3.0
     */
    public TextHelper getSuffix() {
        return TextHelper.wrap(base.getPlayerSuffix());
    }

    /**
     * Whether players on this team push through each other, and which players, as one of four
     * words: {@code always}, {@code never}, {@code pushOtherTeams} or {@code pushOwnTeam}. The last
     * two are the middle ground, where members of the same team pass through one another but still
     * collide with everybody else.
     * example:
     * <pre>
     * // the collision rule on every team
     * const board = World.getScoreboards();
     * if (board !== null) {
     *   const teams = board.getTeams();
     *   for (let i = 0; i !== teams.size(); i += 1) {
     *     Chat.log(`${teams.get(i).getName()}: ${teams.get(i).getCollisionRule()}`);
     *   }
     * }
     * </pre>
     *
     * @return the team's collision rule.
     * @since 1.3.0
     */
    @DocletReplaceReturn("TeamCollisionRule")
    public String getCollisionRule() {
        return base.getCollisionRule().name;
    }

    /**
     * Whether members of this team can hurt each other. It is a team setting, and it applies
     * whatever the game mode is.
     * example:
     * <pre>
     * // a pvp server's team rules
     * const entry = World.getPlayerEntry("Notch");
     * if (entry !== null) {
     *   const team = entry.getTeam();
     *   if (team !== null) {
     *     Chat.log(`friendly fire is ${team.isFriendlyFire() ? "on" : "off"}`);
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if members of this team can hurt each other.
     * @since 1.3.0
     */
    public boolean isFriendlyFire() {
        return base.isAllowFriendlyFire();
    }

    /**
     * Whether members of this team can see each other while one of them is invisible. It is a
     * setting the server turns on, and a client only knows what the server told it.
     * example:
     * <pre>
     * // which teams can see through invisibility
     * const board = World.getScoreboards();
     * if (board !== null) {
     *   const teams = board.getTeams();
     *   for (let i = 0; i !== teams.size(); i += 1) {
     *     const team = teams.get(i);
     *     if (team.showFriendlyInvisibles()) {
     *       Chat.log(`${team.getName()} can see its own`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if members of this team can see each other through invisibility.
     * @since 1.3.0
     */
    public boolean showFriendlyInvisibles() {
        return base.canSeeFriendlyInvisibles();
    }

    /**
     * Whose nametags this team can see, as one of four words: {@code always}, {@code never},
     * {@code hideForOtherTeams} or {@code hideForOwnTeam}. The two middle answers are the ones
     * worth knowing apart, since one of them hides the names of everybody else and the other hides
     * the names of the team's own members.
     * example:
     * <pre>
     * // the nametag rules, per team
     * const board = World.getScoreboards();
     * if (board !== null) {
     *   const teams = board.getTeams();
     *   for (let i = 0; i !== teams.size(); i += 1) {
     *     const team = teams.get(i);
     *     Chat.log(`${team.getName()}: names ${team.nametagVisibility()}`);
     *   }
     * }
     * </pre>
     *
     * @return this team's nametag visibility rule.
     * @since 1.3.0
     */
    @DocletReplaceReturn("TeamVisibilityRule")
    public String nametagVisibility() {
        return base.getNameTagVisibility().name;
    }

    /**
     * Whose death messages this team can see, as the same four words
     * {@link #nametagVisibility()} gives. It is a separate setting from the nametag one, and a team
     * can have the two set differently.
     * example:
     * <pre>
     * // both visibility settings side by side
     * const entry = World.getPlayerEntry("Notch");
     * if (entry !== null) {
     *   const team = entry.getTeam();
     *   if (team !== null) {
     *     Chat.log(`nametags ${team.nametagVisibility()}, deaths ${team.deathMessageVisibility()}`);
     *   }
     * }
     * </pre>
     *
     * @return this team's death message visibility rule.
     * @since 1.3.0
     */
    @DocletReplaceReturn("TeamVisibilityRule")
    public String deathMessageVisibility() {
        return base.getDeathMessageVisibility().name;
    }

    @Override
    public String toString() {
        return String.format("TeamHelper:{\"name\": \"%s\"}", getDisplayName().toString());
    }

}
