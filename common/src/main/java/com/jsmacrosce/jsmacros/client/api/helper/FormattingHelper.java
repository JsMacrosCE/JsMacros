package com.jsmacrosce.jsmacros.client.api.helper;

import net.minecraft.ChatFormatting;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class FormattingHelper extends BaseHelper<ChatFormatting> {

    public FormattingHelper(ChatFormatting base) {
        super(base);
    }

    /**
     * @return the color value of this formatting.
     * @since 1.8.4
     */
    public int getColorValue() {
        return base.getColor();
    }

    /**
     * @return the index of this formatting or {@code -1} if this formatting is a modifier.
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
     *
     * @return the color code of this formatting.
     * @since 1.8.4
     */
    public char getCode() {
        return base.getChar();
    }

    /**
     * @return {@code true} if this formatting is a color, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isColor() {
        return base.isColor();
    }

    /**
     * @return {@code true} if this formatting is a modifier, {@code false} otherwise.
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
