package com.jsmacrosce.jsmacros.util;

import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringDecomposer;

import java.util.Optional;

public final class TextUtil {
    private TextUtil() {}

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
