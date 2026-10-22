package com.jsmacrosce.jsmacros.client.api.helper.world;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.world.scores.PlayerTeam;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.FormattingHelper;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Exposes a scoreboard team and its display, membership, color, and team-rule settings.
 * Obtain a team from a player entry or from {@link ScoreboardsHelper#getTeams()}.
 * <p>
 * Team colors have distinct representations: {@link #getColorFormat()} returns formatting,
 * {@link #getColorIndex()} its index, {@link #getColorValue()} its packed value (or {@code -1}
 * when unset), and {@link #getColorName()} its name.
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
    /** Wraps the supplied raw scoreboard team without changing it.
     * @param t raw scoreboard team to wrap
     */
    public TeamHelper(PlayerTeam t) {
        super(t);
    }

    /**
     * Returns the team's registered name.
     *
     * @return the team's registered name.
     * @since 1.3.0
     */
    public String getName() {
        return base.getName();
    }

    /**
     * Returns the team's display text, which may differ from {@link #getName()}.
     *
     * @return the display name of this team.
     * @since 1.3.0
     */
    public TextHelper getDisplayName() {
        return TextHelper.wrap(base.getDisplayName());
    }

    /**
     * Returns a copy of the team's scoreboard member names; changing the returned list does not
     * change team membership.
     *
     * @return a copy of the list of member names.
     * @since 1.3.0
     */
    public List<String> getPlayerList() {
        return new ArrayList<>(base.getPlayers());
    }

    /**
     * Returns the team's color formatting. An unset team color is represented by reset formatting;
     * use {@link #getColorValue()} to distinguish it from a set color.
     *
     * @return the formatting of this team's color.
     * @since 1.8.4
     */
    public FormattingHelper getColorFormat() {
        return new FormattingHelper(base.getColor());
    }

    /**
     * Returns the team's color index.
     *
     * @return the team's color index.
     * @since 1.3.0
     * @deprecated use {@link #getColorIndex()} instead.
     */
    @DocletIgnore
    @Deprecated
    public int getColor() {
        return getColorIndex();
    }

    /**
     * Returns the team's color index, distinct from the packed value returned by
     * {@link #getColorValue()}.
     *
     * @return the color index of this team.
     * @since 1.8.4
     */
    public int getColorIndex() {
        return base.getColor().getId();
    }

    /**
     * Returns the packed color value, or {@code -1} when the team color has no packed value.
     *
     * @return the color value for this team or {@code -1} if it has no color.
     * @since 1.8.4
     */
    public int getColorValue() {
        return base.getColor().getColor() == null ? -1 : base.getColor().getColor();
    }

    /**
     * Returns the team's color name.
     *
     * @return the name of this team's color.
     * @since 1.8.4
     */
    @DocletReplaceReturn("FormattingColorName")
    public String getColorName() {
        return base.getColor().getName();
    }

    /**
     * Returns the scoreboard this team belongs to.
     *
     * @return the scoreboard including this team.
     * @since 1.8.4
     */
    public ScoreboardsHelper getScoreboard() {
        return new ScoreboardsHelper(base.getScoreboard());
    }

    /**
     * Returns the text prepended to member names; it may be empty and may include formatting.
     *
     * @return the prefix set on this team.
     * @since 1.3.0
     */
    public TextHelper getPrefix() {
        return TextHelper.wrap(base.getPlayerPrefix());
    }

    /**
     * Returns the text appended to member names; it may be empty and may include formatting.
     *
     * @return the suffix set on this team.
     * @since 1.3.0
     */
    public TextHelper getSuffix() {
        return TextHelper.wrap(base.getPlayerSuffix());
    }

    /**
     * Returns the collision rule name: {@code always}, {@code never}, {@code pushOtherTeams}, or
     * {@code pushOwnTeam}.
     *
     * @return the team's collision rule.
     * @since 1.3.0
     */
    @DocletReplaceReturn("TeamCollisionRule")
    public String getCollisionRule() {
        return base.getCollisionRule().name;
    }

    /**
     * Returns the team's friendly-fire setting.
     *
     * @return {@code true} if members of this team can hurt each other.
     * @since 1.3.0
     */
    public boolean isFriendlyFire() {
        return base.isAllowFriendlyFire();
    }

    /**
     * Returns whether team members can see other members while they are invisible.
     *
     * @return {@code true} if members of this team can see each other through invisibility.
     * @since 1.3.0
     */
    public boolean showFriendlyInvisibles() {
        return base.canSeeFriendlyInvisibles();
    }

    /**
     * Returns the name-tag visibility rule: {@code always}, {@code never},
     * {@code hideForOtherTeams}, or {@code hideForOwnTeam}.
     *
     * @return this team's nametag visibility rule.
     * @since 1.3.0
     */
    @DocletReplaceReturn("TeamVisibilityRule")
    public String nametagVisibility() {
        return base.getNameTagVisibility().name;
    }

    /**
     * Returns the death-message visibility rule, configured separately from
     * {@link #nametagVisibility()}.
     *
     * @return this team's death message visibility rule.
     * @since 1.3.0
     */
    @DocletReplaceReturn("TeamVisibilityRule")
    public String deathMessageVisibility() {
        return base.getDeathMessageVisibility().name;
    }

    /** Returns a diagnostic representation containing the team's display-name helper representation.
     * @return a diagnostic representation of the wrapped team
     */
    @Override
    public String toString() {
        return String.format("TeamHelper:{\"name\": \"%s\"}", getDisplayName().toString());
    }

}
