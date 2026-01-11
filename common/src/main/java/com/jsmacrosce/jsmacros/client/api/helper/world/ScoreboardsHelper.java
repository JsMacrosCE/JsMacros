package com.jsmacrosce.jsmacros.client.api.helper.world;

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
 * @author Wagyourtail
 * @since 1.2.9
 */
@SuppressWarnings("unused")
public class ScoreboardsHelper extends BaseHelper<Scoreboard> {

    public ScoreboardsHelper(Scoreboard board) {
        super(board);
    }

    /**
     * @param index
     * @return
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
     * {@code 0} is tab list, {@code 1} or {@code 3 + getPlayerTeamColorIndex()} is sidebar, {@code 2} should be below name.
     * therefore max slot number is 18.
     *
     * @param slot
     * @return
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
     * @param entity
     * @return
     * @since 1.2.9
     */
    public int getPlayerTeamColorIndex(PlayerEntityHelper<Player> entity) {
        return getPlayerTeamColorIndex(entity.getRaw());
    }

    /**
     * @return team index for client player
     * @since 1.6.5
     */
    public int getPlayerTeamColorIndex() {
        return getPlayerTeamColorIndex(Minecraft.getInstance().player);
    }

    /**
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
     * @param player the player to get the team color's formatting for.
     * @return the formatting for the client player's team, {@code null} if the player is not in a
     * team.
     * @since 1.8.4
     */
    @Nullable
    public FormattingHelper getTeamColorFormatting(PlayerEntityHelper<Player> player) {
        ChatFormatting team = getPlayerTeamColor(player.getRaw());
        return team == null ? null : new FormattingHelper(team);
    }

    /**
     * @param player the player to get the team color for
     * @return the color of the specified player's team or {@code -1} if the player is not in a team.
     * @since 1.8.4
     */
    public int getTeamColor(PlayerEntityHelper<Player> player) {
        ChatFormatting team = getPlayerTeamColor(player.getRaw());
        return team == null || team.getColor() == null ? -1 : team.getColor();
    }

    /**
     * @return the color of this player's team or {@code -1} if this player is not in a team.
     * @since 1.8.4
     */
    public int getTeamColor() {
        ChatFormatting team = getPlayerTeamColor(Minecraft.getInstance().player);
        return team == null || team.getColor() == null ? -1 : team.getColor();
    }

    /**
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
     * @return the color of this player's team or {@code null} if this player is not in a team.
     * @since 1.8.4
     */
    @Nullable
    public String getTeamColorName() {
        ChatFormatting team = getPlayerTeamColor(Minecraft.getInstance().player);
        return team == null ? null : team.getName();
    }

    /**
     * @return
     * @since 1.3.0
     */
    public List<TeamHelper> getTeams() {
        return base.getPlayerTeams().stream().map(TeamHelper::new).collect(Collectors.toList());
    }

    /**
     * @param p
     * @return
     * @since 1.3.0
     */
    public TeamHelper getPlayerTeam(PlayerEntityHelper<Player> p) {
        return new TeamHelper(getPlayerTeam(p.getRaw()));
    }

    /**
     * @return team for client player
     * @since 1.6.5
     */
    public TeamHelper getPlayerTeam() {
        return new TeamHelper(getPlayerTeam(Minecraft.getInstance().player));
    }

    /**
     * @param p
     * @return
     * @since 1.3.0
     */
    @Nullable
    protected PlayerTeam getPlayerTeam(Player p) {
        return base.getPlayerTeam(p.getScoreboardName());
    }

    /**
     * @param entity
     * @return
     * @since 1.2.9
     */
    protected int getPlayerTeamColorIndex(Player entity) {
        ChatFormatting color = getPlayerTeamColor(entity);
        return color == null ? -1 : color.getId();
    }

    /**
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
