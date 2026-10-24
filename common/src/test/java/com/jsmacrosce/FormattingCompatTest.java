package com.jsmacrosce;

import com.jsmacrosce.jsmacros.client.api.helper.FormattingHelper;
import com.jsmacrosce.jsmacros.client.util.FormattingCompat;
import net.minecraft.ChatFormatting;

// The script-facing numeric color contract is the same on every supported target.
public final class FormattingCompatTest {
    private static final ChatFormatting[] COLORS = {
            ChatFormatting.BLACK, ChatFormatting.DARK_BLUE, ChatFormatting.DARK_GREEN,
            ChatFormatting.DARK_AQUA, ChatFormatting.DARK_RED, ChatFormatting.DARK_PURPLE,
            ChatFormatting.GOLD, ChatFormatting.GRAY, ChatFormatting.DARK_GRAY,
            ChatFormatting.BLUE, ChatFormatting.GREEN, ChatFormatting.AQUA,
            ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE, ChatFormatting.YELLOW,
            ChatFormatting.WHITE
    };
    private static final ChatFormatting[] NON_COLORS = {
            ChatFormatting.OBFUSCATED, ChatFormatting.BOLD, ChatFormatting.STRIKETHROUGH,
            ChatFormatting.UNDERLINE, ChatFormatting.ITALIC, ChatFormatting.RESET
    };
    private static int checks;

    public static void main(String[] args) {
        colorIds();
        lookupBoundaries();
        nonColorIds();
        check(FormattingCompat.fromTeamColor(null) == ChatFormatting.RESET, "unset team color uses RESET");
        expectId(FormattingCompat.fromTeamColor(null), -1);
        System.out.println("FormattingCompat: 22 formats, " + checks + " checks passed");
    }

    private static void colorIds() {
        for (int id = 0; id < COLORS.length; id++) {
            ChatFormatting color = COLORS[id];
            expectId(color, id);
            check(FormattingCompat.getById(id) == color, "color lookup for ID " + id);
        }
    }

    private static void lookupBoundaries() {
        for (int id : new int[]{Integer.MIN_VALUE, -256, -16, -2, -1}) {
            check(FormattingCompat.getById(id) == ChatFormatting.RESET, "negative ID selects RESET: " + id);
        }
        for (int id : new int[]{16, 17, 18, 19, 20, 21, 22, 255, Integer.MAX_VALUE}) {
            check(FormattingCompat.getById(id) == null, "unmapped non-negative ID supplies no color: " + id);
        }
    }

    private static void nonColorIds() {
        for (ChatFormatting formatting : NON_COLORS) {
            expectId(formatting, -1);
        }
    }

    private static void expectId(ChatFormatting formatting, int expected) {
        int actual = FormattingCompat.getId(formatting);
        check(actual == expected, formatting.name() + " color ID: expected " + expected + ", got " + actual);
        check(new FormattingHelper(formatting).getColorIndex() == expected,
                formatting.name() + " script-facing color index");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
