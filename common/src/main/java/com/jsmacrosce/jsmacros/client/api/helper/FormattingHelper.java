package com.jsmacrosce.jsmacros.client.api.helper;

import net.minecraft.ChatFormatting;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * Wraps one of Minecraft's text color or formatting codes.
 * <p>
 * {@code RESET} is neither a color nor a modifier, so {@link #isColor()} and
 * {@link #isModifier()} are both false for it.
 * <p>
 * The color index and RGB value are {@code -1} for modifiers and {@code RESET}.
 * <p>
 * A {@link StyleHelper} returns this only for legacy formatting values; arbitrary RGB colors do
 * not have a formatting code.
 * <pre>
 * const line = Chat.createTextBuilder().append("a coloured run").withColor(0xc).build();
 * line.visit(JavaWrapper.methodToJava(function (style, run) {
 *   if (style.hasColor()) {
 *     const formatting = style.getFormatting();
 *     if (formatting !== null) {
 *       Chat.log(`${run}: ${formatting.getName()}, index ${formatting.getColorIndex()}, rgb ${formatting.getColorValue()}`);
 *     }
 *   }
 * }));
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class FormattingHelper extends BaseHelper<ChatFormatting> {

    /**
     * Wraps the supplied formatting value without copying or parsing it.
     *
     * @param base raw text color or formatting code to wrap
     */
    public FormattingHelper(ChatFormatting base) {
        super(base);
    }

    /**
     * Returns the RGB value for one of the sixteen colors; modifiers and {@code RESET} return
     * {@code -1}.
     *
     * @return the color value, or {@code -1} for non-color formats.
     * @since 1.8.4
     */
    public int getColorValue() {
        Integer color = base.getColor();
        return color == null ? -1 : color;
    }

    /**
     * @return the legacy color index ({@code 0}–{@code 15}), or {@code -1} for a modifier or
     *         {@code RESET}.
     * @since 1.8.4
     */
    public int getColorIndex() {
        return base.getId();
    }

    /**
     * @return the lowercase name of this formatting.
     * @since 1.8.4
     */
    public String getName() {
        return base.getName();
    }

    /**
     * Returns the legacy formatting code character. Prefix it with the section symbol ({@code §})
     * to form a legacy formatting sequence; color codes are {@code 0}–{@code 9} and {@code a}–
     * {@code f}, modifiers are {@code k}–{@code o}, and {@code RESET} is {@code r}.
     *
     * @return the formatting code character.
     * @since 1.8.4
     */
    public char getCode() {
        return base.getChar();
    }

    /**
     * @return {@code true} for one of the sixteen colors; {@code false} for modifiers and
     *         {@code RESET}.
     * @since 1.8.4
     */
    public boolean isColor() {
        return base.isColor();
    }

    /**
     * @return {@code true} for one of the five modifiers; {@code false} for colors and
     *         {@code RESET}.
     * @since 1.8.4
     */
    public boolean isModifier() {
        return base.isFormat();
    }

    /**
     * Returns a diagnostic string containing this formatting's index, color value, name, code,
     * and color and modifier flags.
     *
     * @return diagnostic representation of the wrapped formatting
     */
    @Override
    public String toString() {
        return String.format("FormattingHelper:{\"index\": %d, \"color\": %d, \"name\": \"%s\", \"code\": \"%s\", \"isColor\": %b, \"isModifier\": %b}", getColorIndex(), getColorValue(), getName(), getCode(), isColor(), isModifier());
    }

}
