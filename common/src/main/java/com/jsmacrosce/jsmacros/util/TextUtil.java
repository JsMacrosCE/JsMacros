package com.jsmacrosce.jsmacros.util;

import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringDecomposer;

import java.util.Optional;

/**
 * Version-compatible raw formatted-text style lookup using font pixel width; this is not a global library.
 * <pre>
 * const TextUtil = Java.type("com.jsmacrosce.jsmacros.util.TextUtil");
 * const font = Client.getMinecraft().font;
 * if (font) {
 *   const text = Chat.createTextHelperFromString("Hello").getRaw();
 *   const style = TextUtil.componentStyleAtWidth(font, text, 0);
 *   Chat.log(style !== null);
 * }
 * </pre>
 */
public final class TextUtil {
    private TextUtil() {}

    /**
     * Returns the raw style of the first formatted character whose accumulated font width exceeds the pixel budget, or {@code null} if traversal finishes without crossing it. The budget is measured from the start of the text; an exact boundary does not select the character ending at that boundary. This visits formatted text runs, not wrapped lines or screen hit regions. From Minecraft 1.21.11, a negative budget returns {@code null} immediately; earlier targets delegate it to Minecraft's splitter.
     * @param font non-null font used to measure character widths
     * @param text non-null formatted text to visit
     * @param pixelX width budget in pixels from the start of the text
     * @return raw style at the first width overflow, or {@code null} if text traversal finishes
     */
    public static Style componentStyleAtWidth(Font font, FormattedText text, int pixelX) {
        //? if >=1.21.11 {
        /*if (pixelX < 0) return null;

        StringSplitter splitter = font.getSplitter();
        // Inner, non-static class, so we need the `splitter.new` syntax.
        StringSplitter.WidthLimitedCharSink sink = splitter.new WidthLimitedCharSink((float) pixelX);

        return text
                .visit((style, string) -> {
                    // If iterateFormatted returns false, sink hit the width limit
                    boolean finished = StringDecomposer.iterateFormatted(string, style, sink);
                    return finished ? Optional.empty() : Optional.of(style);
                }, Style.EMPTY)
                .orElse(null);
        *///? } else {
        return font.getSplitter().componentStyleAtWidth(text, pixelX);
        //? }
    }
}
