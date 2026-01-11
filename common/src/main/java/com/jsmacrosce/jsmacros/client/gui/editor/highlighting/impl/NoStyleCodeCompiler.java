package com.jsmacrosce.jsmacros.client.gui.editor.highlighting.impl;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import com.jsmacrosce.jsmacros.client.gui.editor.highlighting.AbstractRenderCodeCompiler;
import com.jsmacrosce.jsmacros.client.gui.editor.highlighting.AutoCompleteSuggestion;
import com.jsmacrosce.jsmacros.client.gui.screens.EditorScreen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NoStyleCodeCompiler extends AbstractRenderCodeCompiler {
    private Component[] compiledText = new Component[0];

    public NoStyleCodeCompiler(String language, EditorScreen screen) {
        super(language, screen);
    }

    @Override
    public void recompileRenderedText(@NotNull String text) {
        if (text.length() == 0) {
            compiledText = new Component[]{Component.literal("")};
        } else {
            String[] t2 = text.split("\n");
            compiledText = new Component[t2.length];
            for (int i = 0; i < t2.length; i++) {
                compiledText[i] = Component.literal(t2[i]).setStyle(EditorScreen.defaultStyle);
            }
        }
    }

    @Override
    public @NotNull Map<String, Runnable> getRightClickOptions(int index) {
        return new LinkedHashMap<>();
    }

    @Override
    public @NotNull Component[] getRenderedText() {
        return compiledText;
    }

    @Override
    public @NotNull List<AutoCompleteSuggestion> getSuggestions() {
        return new ArrayList<>();
    }

}
