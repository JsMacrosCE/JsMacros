package com.jsmacrosce.jsmacros.client.util;

import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.ChatFormatting;
import net.minecraft.world.scores.PlayerTeam;

import java.util.Locale;

//? if >=26.2 {
/*import net.minecraft.network.chat.TextColor;
import net.minecraft.world.scores.TeamColor;
*///?}

/**
 * Keeps the legacy formatting metadata that 26.2 removed from {@link ChatFormatting}.
 * <p>
 * Up to 26.1 the enum carried its colour value, id, name, code character and colour/modifier
 * flags. 26.2 reduced it to the colour names and a code lookup, moving colour values to
 * {@link TextColor} and team colours to {@link TeamColor}. These helpers derive the old values
 * from the enum's ordinal, name and code so the script API keeps reporting the same data.
 */
@DocletIgnore
public final class FormattingCompat {
    private FormattingCompat() {
    }

    public static int getColorValue(ChatFormatting formatting) {
        //? if >=26.2 {
        /*TextColor color = TextColor.fromLegacyFormat(formatting);
        return color == null ? -1 : color.getValue();
        *///?} else {
        Integer color = formatting.getColor();
        return color == null ? -1 : color;
        //?}
    }

    public static int getId(ChatFormatting formatting) {
        return formatting.ordinal();
    }

    public static String getName(ChatFormatting formatting) {
        return formatting.name().toLowerCase(Locale.ROOT);
    }

    public static char getCode(ChatFormatting formatting) {
        return formatting.toString().charAt(1);
    }

    public static boolean isColor(ChatFormatting formatting) {
        return formatting.ordinal() < 16;
    }

    public static boolean isModifier(ChatFormatting formatting) {
        return formatting.ordinal() >= 16 && formatting.ordinal() <= 20;
    }

    public static ChatFormatting getByName(String name) {
        //? if >=26.2 {
        /*for (ChatFormatting formatting : ChatFormatting.values()) {
            if (formatting.name().equalsIgnoreCase(name)) {
                return formatting;
            }
        }
        return null;
        *///?} else {
        return ChatFormatting.getByName(name);
        //?}
    }

    public static ChatFormatting getById(int id) {
        //? if >=26.2 {
        /*ChatFormatting[] values = ChatFormatting.values();
        return id >= 0 && id < values.length ? values[id] : null;
        *///?} else {
        return ChatFormatting.getById(id);
        //?}
    }

    //? if >=26.2 {
    /*public static ChatFormatting fromTeamColor(TeamColor color) {
        return color == null ? ChatFormatting.RESET : ChatFormatting.valueOf(color.name());
    }
    *///?} else {
    public static ChatFormatting fromTeamColor(ChatFormatting color) {
        return color == null ? ChatFormatting.RESET : color;
    }
    //?}

    /**
     * Reads a team's colour in the legacy {@link ChatFormatting} form. Up to 26.1 the team
     * returns a nullable {@code ChatFormatting}; 26.2 returns an {@code Optional<TeamColor>}.
     */
    public static ChatFormatting getTeamColor(PlayerTeam team) {
        //? if >=26.2 {
        /*return team.getColor().map(FormattingCompat::fromTeamColor).orElse(ChatFormatting.RESET);
        *///?} else {
        return fromTeamColor(team.getColor());
        //?}
    }
}
