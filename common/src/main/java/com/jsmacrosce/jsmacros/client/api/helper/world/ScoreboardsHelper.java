package com.jsmacrosce.jsmacros.client.api.helper.world;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.client.api.helper.FormattingHelper;
import com.jsmacrosce.jsmacros.client.api.helper.screen.ScoreboardObjectiveHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.PlayerEntityHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The scoreboard: the objectives a server publishes and the teams that go with them.<br>
 * A script gets one from {@code World.getScoreboards()}, which is {@code null} until a world is
 * joined. Most of what a script wants from a scoreboard is either the sidebar, meaning the list of
 * scores drawn on the right of the screen, or the team the local player is on, and this class has a
 * call for each of those plus the general ones underneath them.<br>
 * The two slot calls here differ in what they index. {@link #getObjectiveSlot(int)} counts from the
 * very first slot, so zero is the tab list and one is the plain sidebar.
 * {@link #getObjectiveForTeamColorIndex(int)} counts only the sixteen per-team sidebars, so zero is
 * the black one and there is no tab list or plain sidebar in that range.
 * example:
 * <pre>
 * const board = World.getScoreboards();
 * if (board !== null) {
 *   // the sidebar the player is actually looking at
 *   const shown = board.getCurrentScoreboard();
 *   if (shown !== null) {
 *     Chat.log(`sidebar: ${shown.getName()}`);
 *     for (const score of shown.getTexts()) {
 *       Chat.log(`  ${score.getString()}`);
 *     }
 *   }
 *
 *   // the local player's team, and its colour
 *   const colour = board.getTeamColorName();
 *   Chat.log(colour === null ? "no team" : `team colour: ${colour}`);
 * }
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.2.9
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class ScoreboardsHelper extends BaseHelper<Scoreboard> {

    public ScoreboardsHelper(Scoreboard board) {
        super(board);
    }

    /**
     * The objective in one of the sixteen sidebars that belong to a team colour, counted from the
     * black one at zero. Sixteen is the number of team colours the game has, so a number from zero
     * to fifteen names one of them and anything larger is out of range.
     * <p>
     * A negative number, which is what a player on no team gives for their colour index, gives
     * {@code null} rather than an error, and so does a slot with nothing published in it.
     * example:
     * <pre>
     * // which of the sixteen per team sidebars have something in them
     * const board = World.getScoreboards();
     * if (board !== null) {
     *   for (let i = 0; i !== 16; i += 1) {
     *     const objective = board.getObjectiveForTeamColorIndex(i);
     *     if (objective !== null) {
     *       Chat.log(`slot ${i} holds ${objective.getName()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @param index the team colour index, from zero to fifteen.
     * @return the objective in that team's sidebar, or {@code null} if there is none.
     * @since 1.2.9
     */
    @Nullable
    public ScoreboardObjectiveHelper getObjectiveForTeamColorIndex(int index) {
        Objective obj = null;
        if (index >= 0) {
            obj = base.getDisplayObjective(DisplaySlot.values()[index + 3]);
        }
        return obj == null ? null : new ScoreboardObjectiveHelper(obj);
    }

    /**
     * The objective in a display slot, counted from the first one. The order is the game's: zero is
     * the tab list, one is the plain sidebar, two is the slot below a player's name, and three to
     * eighteen are the sixteen per-team sidebars in colour order from black to white. Nineteen is
     * the first number with no slot behind it.
     * <p>
     * A negative number gives {@code null} rather than an error, and so does a slot with nothing
     * published in it.
     * example:
     * <pre>
     * // the tab list and the plain sidebar, which are the two that are always there
     * const board = World.getScoreboards();
     * if (board !== null) {
     *   for (const slot of [0, 1, 2]) {
     *     const objective = board.getObjectiveSlot(slot);
     *     Chat.log(`slot ${slot}: ${objective === null ? "empty" : objective.getName()}`);
     *   }
     *
     *   // and the whole range, which is nineteen slots
     *   for (let slot = 0; slot !== 19; slot += 1) {
     *     const objective = board.getObjectiveSlot(slot);
     *     if (objective !== null) {
     *       Chat.log(`  ${slot}: ${objective.getDisplayName().getString()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @param slot the display slot, from zero to eighteen.
     * @return the objective in that slot, or {@code null} if there is none.
     * @throws ArrayIndexOutOfBoundsException if {@code slot} is nineteen or more, since there is no
     *                                       such slot.
     * @since 1.2.9
     */
    @Nullable
    public ScoreboardObjectiveHelper getObjectiveSlot(int slot) {
        Objective obj = null;
        if (slot >= 0) {
            obj = base.getDisplayObjective(DisplaySlot.values()[slot]);
        }
        return obj == null ? null : new ScoreboardObjectiveHelper(obj);
    }

    /**
     * The colour index of the team a player is on, or {@code -1} if they are on no team. The index
     * is the small number the game uses for a colour, so it runs from zero for black to fifteen for
     * white, and it is the number {@link #getObjectiveForTeamColorIndex(int)} takes.
     * example:
     * <pre>
     * // the sidebar that matches a given player's team colour
     * const board = World.getScoreboards();
     * const players = World.getLoadedPlayers();
     * if (board !== null) {
     *   for (let i = 0; i !== players.size(); i += 1) {
     *     const index = board.getPlayerTeamColorIndex(players.get(i));
     *     if (index >= 0) {
     *       const objective = board.getObjectiveForTeamColorIndex(index);
     *       Chat.log(`colour ${index}: ${objective === null ? "no sidebar" : objective.getName()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @param entity the player to find the team colour of.
     * @return the team colour index for that player, or {@code -1} if they are on no team.
     * @since 1.2.9
     */
    public int getPlayerTeamColorIndex(PlayerEntityHelper<Player> entity) {
        return getPlayerTeamColorIndex(entity.getRaw());
    }

    /**
     * The colour index of the team the local player is on, or {@code -1} if they are on no team.
     * This is the same question as {@link #getPlayerTeamColorIndex(PlayerEntityHelper)} with the
     * local player filled in.
     * example:
     * <pre>
     * // the sidebar the local player's own team colour points at
     * const board = World.getScoreboards();
     * if (board !== null) {
     *   const index = board.getPlayerTeamColorIndex();
     *   const objective = board.getObjectiveForTeamColorIndex(index);
     *   if (index === -1) {
     *     Chat.log("no team");
     *   } else if (objective === null) {
     *     Chat.log("no sidebar");
     *   } else {
     *     Chat.log(objective.getName());
     *   }
     * }
     * </pre>
     *
     * @return team index for client player
     * @since 1.6.5
     */
    public int getPlayerTeamColorIndex() {
        return getPlayerTeamColorIndex(Minecraft.getInstance().player);
    }

    /**
     * The formatting of the local player's team colour, which is what a script needs to build text
     * in the same colour. A player on no team gives {@code null} here.
     * example:
     * <pre>
     * // say something in the local player's own colour
     * const board = World.getScoreboards();
     * if (board !== null) {
     *   const format = board.getTeamColorFormatting();
     *   if (format === null) {
     *     Chat.actionbar("no team colour");
     *   } else {
     *     Chat.actionbar(Chat.createTextBuilder()
     *       .append("in your team colour")
     *       .withFormatting(format)
     *       .build());
     *   }
     * }
     * </pre>
     *
     * @return the formatting for the client player's team, {@code null} if the player is not in a
     * team.
     * @since 1.8.4
     */
    @Nullable
    public FormattingHelper getTeamColorFormatting() {
        ChatFormatting team = getPlayerTeamColor(Minecraft.getInstance().player);
        return team == null ? null : new FormattingHelper(team);
    }

    /**
     * The formatting of a given player's team colour, for building text in the same colour as that
     * player. A player on no team gives {@code null} here.
     * example:
     * <pre>
     * // name each loaded player in their own team's colour
     * const board = World.getScoreboards();
     * const players = World.getLoadedPlayers();
     * if (board !== null) {
     *   for (let i = 0; i !== players.size(); i += 1) {
     *     const player = players.get(i);
     *     const format = board.getTeamColorFormatting(player);
     *     if (format === null) {
     *       Chat.log(player.getName());
     *     } else {
     *       Chat.log(Chat.createTextBuilder()
     *         .append(player.getName().getString())
     *         .withFormatting(format)
     *         .build());
     *     }
     *   }
     * }
     * </pre>
     *
     * @param player the player to get the team color's formatting for.
     * @return the formatting for the given player's team, {@code null} if the player is not in a
     * team.
     * @since 1.8.4
     */
    @Nullable
    public FormattingHelper getTeamColorFormatting(PlayerEntityHelper<Player> player) {
        ChatFormatting team = getPlayerTeamColor(player.getRaw());
        return team == null ? null : new FormattingHelper(team);
    }

    /**
     * The packed colour value of a given player's team, or {@code -1} if they are on no team or
     * their team has no colour of its own. This is the number to hand to a renderer;
     * {@link #getPlayerTeamColorIndex(PlayerEntityHelper)} is the small index for the same colour.
     * example:
     * <pre>
     * // the colour each loaded player is shown in
     * const board = World.getScoreboards();
     * const players = World.getLoadedPlayers();
     * if (board !== null) {
     *   for (let i = 0; i !== players.size(); i += 1) {
     *     const value = board.getTeamColor(players.get(i));
     *     if (value === -1) {
     *       Chat.log(`${players.get(i).getName()}: no team colour`);
     *     } else {
     *       Chat.log(`${players.get(i).getName()}: 0x${value.toString(16)}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @param player the player to get the team color for
     * @return the color of the specified player's team or {@code -1} if the player is not in a team.
     * @since 1.8.4
     */
    public int getTeamColor(PlayerEntityHelper<Player> player) {
        ChatFormatting team = getPlayerTeamColor(player.getRaw());
        return team == null || team.getColor() == null ? -1 : team.getColor();
    }

    /**
     * The packed colour value of the local player's team, or {@code -1} if they are on no team or
     * their team has no colour of its own. This is the same question as
     * {@link #getTeamColor(PlayerEntityHelper)} with the local player filled in.
     * example:
     * <pre>
     * const board = World.getScoreboards();
     * if (board !== null) {
     *   const value = board.getTeamColor();
     *   if (value === -1) {
     *     Chat.log("no team colour of your own");
     *   } else {
     *     Chat.log(`your team colour is 0x${value.toString(16)}`);
     *   }
     * }
     * </pre>
     *
     * @return the color of this player's team or {@code -1} if this player is not in a team.
     * @since 1.8.4
     */
    public int getTeamColor() {
        ChatFormatting team = getPlayerTeamColor(Minecraft.getInstance().player);
        return team == null || team.getColor() == null ? -1 : team.getColor();
    }

    /**
     * The name of a given player's team colour, which is the word a command would use for it, or
     * {@code null} if the player is on no team. The name and the number
     * {@link #getTeamColor(PlayerEntityHelper)} gives are two spellings of the same colour.
     * example:
     * <pre>
     * // the colour name beside the colour value
     * const board = World.getScoreboards();
     * const players = World.getLoadedPlayers();
     * if (board !== null) {
     *   for (let i = 0; i !== players.size(); i += 1) {
     *     const name = board.getTeamColorName(players.get(i));
     *     if (name !== null) {
     *       Chat.log(`${players.get(i).getName()}: ${name}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @param player the player to get the team color's name for
     * @return the name of the specified player's team color or {@code null} if the player is not in
     * a team.
     * @since 1.8.4
     */
    @Nullable
    public String getTeamColorName(PlayerEntityHelper<Player> player) {
        ChatFormatting team = getPlayerTeamColor(player.getRaw());
        return team == null ? null : team.getName();
    }

    /**
     * The name of the local player's team colour, or {@code null} if they are on no team.
     * example:
     * <pre>
     * const board = World.getScoreboards();
     * if (board !== null) {
     *   const name = board.getTeamColorName();
     *   Chat.log(name === null ? "not in a team" : `your colour is ${name}`);
     * }
     * </pre>
     *
     * @return the color of this player's team or {@code null} if this player is not in a team.
     * @since 1.8.4
     */
    @Nullable
    public String getTeamColorName() {
        ChatFormatting team = getPlayerTeamColor(Minecraft.getInstance().player);
        return team == null ? null : team.getName();
    }

    /**
     * Every team on this scoreboard, as a fresh list built from the scoreboard's own teams each
     * time. An empty scoreboard gives an empty list rather than {@code null}.
     * example:
     * <pre>
     * // every team, and how many are on each
     * const board = World.getScoreboards();
     * if (board !== null) {
     *   const teams = board.getTeams();
     *   for (let i = 0; i !== teams.size(); i += 1) {
     *     const team = teams.get(i);
     *     Chat.log(`${team.getName()}: ${team.getPlayerList().size()} members`);
     *   }
     * }
     * </pre>
     *
     * @return a list of all the teams on this scoreboard.
     * @since 1.3.0
     */
    public List<TeamHelper> getTeams() {
        return base.getPlayerTeams().stream().map(TeamHelper::new).collect(Collectors.toList());
    }

    /**
     * The team a given player is on. A scoreboard is not required to have a team for a player, so
     * this wraps whatever the scoreboard holds and that can be nothing at all; going through
     * {@link PlayerListEntryHelper#getTeam()} instead is the form that answers {@code null} rather
     * than wrapping a missing team.
     * example:
     * <pre>
     * // a player's team, checked before it is used
     * const board = World.getScoreboards();
     * const players = World.getLoadedPlayers();
     * if (board !== null) {
     *   for (let i = 0; i !== players.size(); i += 1) {
     *     const team = board.getPlayerTeam(players.get(i));
     *     // the tab list form is the one that can be asked whether it found anything
     *     const entry = World.getPlayerEntry(players.get(i).getName().getString());
     *     if (entry !== null) {
     *       if (entry.getTeam() === null) {
     *         continue;
     *       }
     *       Chat.log(`${players.get(i).getName()} is on ${entry.getTeam().getName()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @param p the player to look the team up for.
     * @return a helper over that player's team, which wraps nothing if the player is on none.
     * @since 1.3.0
     */
    public TeamHelper getPlayerTeam(PlayerEntityHelper<Player> p) {
        return new TeamHelper(getPlayerTeam(p.getRaw()));
    }

    /**
     * The team the local player is on, with the local player filled in. Like
     * {@link #getPlayerTeam(PlayerEntityHelper)} this wraps whatever the scoreboard holds rather
     * than answering {@code null}, and the tab list form
     * {@link PlayerListEntryHelper#getTeam()} is the one that can be asked.
     * example:
     * <pre>
     * // the local player's own team, the safe way round
     * const board = World.getScoreboards();
     * if (board !== null) {
     *   const teams = board.getTeams();
     *   for (let i = 0; i !== teams.size(); i += 1) {
     *     Chat.log(`${teams.get(i).getName()}: ${teams.get(i).getPlayerList().size()}`);
     *   }
     * }
     * </pre>
     *
     * @return a helper over the local player's team, which wraps nothing if they are on none.
     * @since 1.6.5
     */
    public TeamHelper getPlayerTeam() {
        return new TeamHelper(getPlayerTeam(Minecraft.getInstance().player));
    }

    /**
     * The team the scoreboard holds for a given player, or {@code null} if it holds none. The
     * lookup is by the player's scoreboard name rather than by their account, which for a real
     * player is the same string.
     *
     * @param p the player to look the team up for.
     * @return the team, or {@code null} if the scoreboard has no team for that player.
     * @since 1.3.0
     */
    @Nullable
    protected PlayerTeam getPlayerTeam(Player p) {
        return base.getPlayerTeam(p.getScoreboardName());
    }

    /**
     * The colour index of a given player's team, or {@code -1} if they are on no team.
     *
     * @param entity the player to find the team colour of.
     * @return the colour index, or {@code -1} if the player is on no team.
     * @since 1.2.9
     */
    protected int getPlayerTeamColorIndex(Player entity) {
        ChatFormatting color = getPlayerTeamColor(entity);
        return color == null ? -1 : color.getId();
    }

    /**
     * The team colour of a given player, or {@code null} if they are on no team. This is the
     * lookup the other team colour calls are all built on.
     *
     * @param player the player to get the team color for
     * @return the team color for the player or {@code null} if the player is not in a team.
     * @since 1.8.4
     */
    @Nullable
    protected ChatFormatting getPlayerTeamColor(Player player) {
        PlayerTeam t = base.getPlayerTeam(player.getScoreboardName());
        if (t == null) {
            return null;
        }
        return t.getColor();
    }

    /**
     * The sidebar the local player is actually looking at. This is the sidebar for the local
     * player's own team colour when there is one, and the plain sidebar otherwise, so a script that
     * wants what is on the screen should ask for this rather than reaching for a slot.
     * <p>
     * With no local player there is no team colour to go on, and with no sidebar published in
     * either place the answer is {@code null}.
     * example:
     * <pre>
     * // read the sidebar, whichever of the two it turns out to be
     * const board = World.getScoreboards();
     * if (board !== null) {
     *   const shown = board.getCurrentScoreboard();
     *   if (shown === null) {
     *     Chat.log("no sidebar on this server");
     *   } else {
     *     Chat.log(`${shown.getName()} scores ${shown.getKnownPlayers().size()} players`);
     *     for (let i = 0; i !== shown.getTexts().size(); i += 1) {
     *       Chat.log(`  ${shown.getTexts().get(i).getString()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the {@link ScoreboardObjectiveHelper} for the currently displayed sidebar scoreboard.
     * @since 1.2.9
     */
    @Nullable
    public ScoreboardObjectiveHelper getCurrentScoreboard() {
        Minecraft mc = Minecraft.getInstance();
        int color = getPlayerTeamColorIndex(mc.player);
        ScoreboardObjectiveHelper h = getObjectiveForTeamColorIndex(color);
        if (h == null) {
            h = getObjectiveSlot(1);
        }
        return h;
    }

    @Override
    public String toString() {
        return String.format("ScoreboardsHelper:{\"current\": %s}", getCurrentScoreboard().toString());
    }

}
