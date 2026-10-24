package com.jsmacrosce.jsmacros.client.api.helper.world;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.ChatFormatting;
import com.jsmacrosce.jsmacros.client.util.FormattingCompat;
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
 * Provides access to a scoreboard's objectives, display slots, and teams. The
 * {@link #getObjectiveSlot(int)} index covers all 19 display slots (tab list, sidebar, below-name,
 * then team-colour sidebars); {@link #getObjectiveForTeamColorIndex(int)} indexes only the latter
 * 16, starting with black.
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

    /**
     * Wraps the supplied scoreboard reference.
     *
     * @param board the raw scoreboard to wrap.
     */
    public ScoreboardsHelper(Scoreboard board) {
        super(board);
    }

    /**
     * Returns the objective in a team-colour sidebar. Index {@code 0} is black and {@code 15} is
     * white; negative or larger indices return {@code null}, as does an empty slot.
     *
     * @param index the team colour index, from zero to fifteen.
     * @return the objective in that team's sidebar, or {@code null} if there is none.
     * @since 1.2.9
     */
    @Nullable
    public ScoreboardObjectiveHelper getObjectiveForTeamColorIndex(int index) {
        Objective obj = null;
        if (index >= 0 && index < DisplaySlot.values().length - 3) {
            obj = base.getDisplayObjective(DisplaySlot.values()[index + 3]);
        }
        return obj == null ? null : new ScoreboardObjectiveHelper(obj);
    }

    /**
     * Returns the objective in a display slot: {@code 0} is the tab list, {@code 1} the sidebar,
     * {@code 2} below-name, and {@code 3}–{@code 18} the team-colour sidebars (black to white).
     * Other indices and empty slots return {@code null}.
     *
     * @param slot the display slot, from zero to eighteen.
     * @return the objective in that slot, or {@code null} if there is none.
     * @since 1.2.9
     */
    @Nullable
    public ScoreboardObjectiveHelper getObjectiveSlot(int slot) {
        Objective obj = null;
        if (slot >= 0 && slot < DisplaySlot.values().length) {
            obj = base.getDisplayObjective(DisplaySlot.values()[slot]);
        }
        return obj == null ? null : new ScoreboardObjectiveHelper(obj);
    }

    /**
     * Returns the player's team-colour index: {@code 0} is black and {@code 15} is white. Returns
     * {@code -1} if the player has no team; this index can be passed to
     * {@link #getObjectiveForTeamColorIndex(int)} when non-negative.
     *
     * @param entity the player to find the team colour of.
     * @return the team colour index for that player, or {@code -1} if they are on no team.
     * @since 1.2.9
     */
    public int getPlayerTeamColorIndex(PlayerEntityHelper<Player> entity) {
        return getPlayerTeamColorIndex(entity.getRaw());
    }

    /**
     * Returns the local player's team-colour index: {@code 0} is black and {@code 15} is white, or
     * {@code -1} if the player has no team. Unlike the overload taking a player, this uses the
     * client player.
     *
     * @return the local player's team-colour index, or {@code -1} if they have no team.
     * @since 1.6.5
     */
    public int getPlayerTeamColorIndex() {
        return getPlayerTeamColorIndex(Minecraft.getInstance().player);
    }

    /**
     * Returns the local player's team colour as text formatting, or {@code null} if the player has
     * no team colour.
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
     * Returns a player's team colour as text formatting, or {@code null} if the player has no team
     * colour. The overload without a parameter uses the local player.
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
     * Returns a player's packed team colour value, or {@code -1} if they have no team colour. This
     * differs from {@link #getPlayerTeamColorIndex(PlayerEntityHelper)}, which returns the colour
     * index rather than the packed value.
     *
     * @param player the player to get the team color for
     * @return the packed team colour, or {@code -1} if no colour is assigned.
     * @since 1.8.4
     */
    public int getTeamColor(PlayerEntityHelper<Player> player) {
        ChatFormatting team = getPlayerTeamColor(player.getRaw());
        return team == null ? -1 : FormattingCompat.getColorValue(team);
    }

    /**
     * Returns the local player's packed team colour, or {@code -1} if they have no team colour.
     *
     * @return the packed team colour, or {@code -1} if none is assigned.
     * @since 1.8.4
     */
    public int getTeamColor() {
        ChatFormatting team = getPlayerTeamColor(Minecraft.getInstance().player);
        return team == null ? -1 : FormattingCompat.getColorValue(team);
    }

    /**
     * Returns the given player's team colour name, or {@code null} if they have no team colour.
     *
     * @param player the player to get the team color's name for
     * @return the name of the specified player's team color or {@code null} if the player is not in
     * a team.
     * @since 1.8.4
     */
    @Nullable
    public String getTeamColorName(PlayerEntityHelper<Player> player) {
        ChatFormatting team = getPlayerTeamColor(player.getRaw());
        return team == null ? null : FormattingCompat.getName(team);
    }

    /**
     * Returns the local player's team colour name, or {@code null} if they have no team colour.
     *
     * @return the team colour name, or {@code null} if none is assigned.
     * @since 1.8.4
     */
    @Nullable
    public String getTeamColorName() {
        ChatFormatting team = getPlayerTeamColor(Minecraft.getInstance().player);
        return team == null ? null : FormattingCompat.getName(team);
    }

    /**
     * Returns a new list containing the teams on this scoreboard; an empty scoreboard yields an
     * empty list.
     *
     * @return a list of all the teams on this scoreboard.
     * @since 1.3.0
     */
    public List<TeamHelper> getTeams() {
        return base.getPlayerTeams().stream().map(TeamHelper::new).collect(Collectors.toList());
    }

    /**
     * Returns the given player's team, or {@code null} if they are not on a team.
     *
     * @param p the player to look the team up for.
     * @return a helper over that player's team, or {@code null} if they have none.
     * @since 1.3.0
     */
    @Nullable
    public TeamHelper getPlayerTeam(PlayerEntityHelper<Player> p) {
        PlayerTeam team = getPlayerTeam(p.getRaw());
        return team == null ? null : new TeamHelper(team);
    }

    /**
     * Returns the local player's team, or {@code null} if there is no local player or they have no
     * team.
     *
     * @return the local player's team helper, or {@code null} if there is no player or team.
     * @since 1.6.5
     */
    @Nullable
    public TeamHelper getPlayerTeam() {
        PlayerTeam team = getPlayerTeam(Minecraft.getInstance().player);
        return team == null ? null : new TeamHelper(team);
    }

    /**
     * Looks up a player's team by their scoreboard name, or returns {@code null} if there is no
     * matching team. A {@code null} player also returns {@code null}.
     *
     * @param p the player to look the team up for.
     * @return the team, or {@code null} if the scoreboard has no team for that player.
     * @since 1.3.0
     */
    @Nullable
    protected PlayerTeam getPlayerTeam(Player p) {
        return p == null ? null : base.getPlayerTeam(p.getScoreboardName());
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
        return color == null ? -1 : FormattingCompat.getId(color);
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
        PlayerTeam t = getPlayerTeam(player);
        if (t == null) {
            return null;
        }
        return FormattingCompat.getTeamColor(t);
    }

    /**
     * Returns the local player's team-colour sidebar if present, otherwise the plain sidebar. If
     * neither slot has an objective, returns {@code null}.
     *
     * @return the team-colour sidebar objective, or the plain sidebar objective if that is absent;
     * {@code null} if neither exists.
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

    /**
     * Returns a diagnostic string containing the current sidebar objective, with literal
     * {@code null} when no objective is present. This uses the local player to select a
     * team-colour sidebar and requires a local player.
     *
     * @return a diagnostic string containing the current sidebar objective, with literal
     * {@code null} when none is present.
     */
    @Override
    public String toString() {
        return String.format("ScoreboardsHelper:{\"current\": %s}", getCurrentScoreboard());
    }

}
