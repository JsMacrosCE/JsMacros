package com.jsmacrosce.jsmacros.client.api.helper;

import net.minecraft.ChatFormatting;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * one of the game's text formattings: either a colour from the old sixteen, or a decoration
 * such as bold.
 * <p>
 * A formatting is the game's own enum, and it has twenty-two constants that fall into three
 * groups which do not quite line up. Sixteen are <b>colours</b>, {@code BLACK} through
 * {@code WHITE}. Five are <b>modifiers</b>: {@code OBFUSCATED}, {@code BOLD},
 * {@code STRIKETHROUGH}, {@code UNDERLINE} and {@code ITALIC}. The last is {@code RESET}, which
 * is neither, and no colour is also a modifier.
 * <p>
 * {@link #isColor()} is the one call that has to be made before the colour is read, because it
 * is true for exactly the sixteen colours and false for everything else. Those sixteen carry a
 * name, a legacy index running {@code 0} to {@code 15} and an rgb value, and
 * {@link #getColorValue()} answers that rgb value for them. The five modifiers and
 * {@code RESET} carry no colour of their own, so {@link #getColorIndex()} answers {@code -1}
 * for those and {@link #getColorValue()} throws instead of answering {@code 0}.
 * <p>
 * {@link #isModifier()} is not the opposite of {@link #isColor()}, and that is worth knowing
 * before either is used as a guard. It is true for the five modifiers and false for the
 * colours, but {@code RESET} answers {@code false} to both, so a test for the other case would
 * let {@code RESET} through into {@link #getColorValue()} and throw. The debug string is no
 * safer: it prints the colour value, so asking a modifier or a {@code RESET} for its string
 * form fails the same way. Log the name and the code instead.
 * <p>
 * A formatting comes from a colour that was written as one of the old names, which is what
 * {@link StyleHelper#getFormatting()} answers. An arbitrary rgb colour has no formatting, so a
 * run using one gives {@code null} there; a run with no colour at all is the case that throws
 * instead, so check {@link StyleHelper#hasColor()} first.
 * <br>
 * The class also exposes the legacy colour code character, which is what the old
 * {@code §}-prefixed string format is built from.
 * example:
 * <pre>
 * const line = Chat.createTextHelperFromString("a coloured run");
 * line.visit(JavaWrapper.methodToJava(function (style, run) {
 *   if (style.hasColor()) {
 *     const formatting = style.getFormatting();
 *     if (formatting !== null) {
 *       if (formatting.isColor()) {
 *         // one of the sixteen: a name, an index and an rgb value
 *         Chat.log(`${run} is ${formatting.getName()}, code ${formatting.getCode()}`);
 *         Chat.log(`rgb ${formatting.getColorValue()}, index ${formatting.getColorIndex()}`);
 *       } else {
 *         // a modifier, or RESET: a name and a code but no colour of its own, so
 *         // getColorValue() would throw here while getColorIndex() answers -1
 *         Chat.log(`${run} is the modifier ${formatting.getName()}, index ${formatting.getColorIndex()}`);
 *       }
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

    public FormattingHelper(ChatFormatting base) {
        super(base);
    }

    /**
     * The rgb colour this formatting paints with, and the reason {@link #isColor()} has to be
     * checked first. Only the sixteen colours carry one; a modifier and {@code RESET} do not, so
     * asking for it is not answered with a placeholder value but by throwing.
     *
     * @return the color value of this formatting.
     * @throws NullPointerException if this formatting is not a colour, since a modifier and
     *         {@code RESET} have no colour value to give back.
     * @since 1.8.4
     */
    public int getColorValue() {
        return base.getColor();
    }

    /**
     * @return the index of this formatting, {@code 0} to {@code 15} for the sixteen colours, or
     *         {@code -1} for a modifier and for {@code RESET}.
     * @since 1.8.4
     */
    public int getColorIndex() {
        return base.getId();
    }

    /**
     * @return the name of this formatting.
     * @since 1.8.4
     */
    public String getName() {
        return base.getName();
    }

    /**
     * The color code can be used with the paragraph to color text.
     * <p>
     * This is the single character the old string format uses, so the section symbol followed by
     * this character is what turns text on.
     * <p>
     * For a colour the character is the lowercase hexadecimal digit of that colour's own
     * {@link #getColorIndex()}, which is why the sixteen read {@code 0} to {@code 9} and then
     * {@code a} to {@code f}. The index is what the character is built from, not the name:
     * {@code dark_red} has index {@code 4} and so its character is {@code 4};
     * {@code red} has index {@code 12}, which is hexadecimal {@code c}, so its character is
     * {@code c}; and {@code d} is {@code light_purple} at index {@code 13} rather than a
     * reading of {@code dark_red}.
     * <p>
     * The five modifiers are the letters that follow the colours and are not hexadecimal, so they
     * are {@code k} for {@code obfuscated}, {@code l} for {@code bold}, {@code m} for
     * {@code strikethrough}, {@code n} for {@code underline} and {@code o} for {@code italic}.
     * {@code RESET} is the odd one out: it has index {@code -1} and no colour, and its character
     * is {@code r}.
     * <br>
     * The value comes back as a one character string, not as a number, so it can be pasted into a
     * plain message as it stands; only the section symbol in front of it has to be added.
     * example:
     * <pre>
     * const line = Chat.createTextHelperFromString("a coloured run");
     * line.visit(JavaWrapper.methodToJava(function (style, run) {
     *   if (style.hasColor()) {
     *     const formatting = style.getFormatting();
     *     if (formatting !== null) {
     *       // the old code is a section symbol followed by this character
     *       const code = "§" + formatting.getCode();
     *       Chat.log(`${run} is typed as ${code} in a plain message`);
     *     }
     *   }
     * }));
     * </pre>
     *
     * @return the color code of this formatting.
     * @since 1.8.4
     */
    public char getCode() {
        return base.getChar();
    }

    /**
     * @return {@code true} if this formatting is one of the sixteen colours, {@code false}
     *         otherwise. This is also {@code false} for the five modifiers and for
     *         {@code RESET}, so it is the check that guards {@link #getColorValue()}.
     * @since 1.8.4
     */
    public boolean isColor() {
        return base.isColor();
    }

    /**
     * @return {@code true} if this formatting is one of the five modifiers, {@code false}
     *         otherwise. Note that {@code RESET} answers {@code false} here as well as to
     *         {@link #isColor()}, so this is not the opposite of that call.
     * @since 1.8.4
     */
    public boolean isModifier() {
        return base.isFormat();
    }

    @Override
    public String toString() {
        return String.format("FormattingHelper:{\"index\": %d, \"color\": %d, \"name\": \"%s\", \"code\": \"%s\", \"isColor\": %b, \"isModifier\": %b}", getColorIndex(), getColorValue(), getName(), getCode(), isColor(), isModifier());
    }

}
