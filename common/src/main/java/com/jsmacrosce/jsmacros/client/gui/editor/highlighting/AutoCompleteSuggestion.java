package com.jsmacrosce.jsmacros.client.gui.editor.highlighting;

import net.minecraft.network.chat.Component;
import com.jsmacrosce.jsmacros.client.gui.screens.EditorScreen;

public class AutoCompleteSuggestion {
    public final int startIndex;
    public final String suggestion;
    public final Component displayText;

    public AutoCompleteSuggestion(int startIndex, String suggestion) {
        this.suggestion = suggestion;
        this.startIndex = startIndex;
        this.displayText = Component.literal(suggestion).setStyle(EditorScreen.defaultStyle);
    }

    public AutoCompleteSuggestion(int startIndex, String suggestion, Component displayText) {
        this.suggestion = suggestion;
        this.startIndex = startIndex;
        this.displayText = displayText;
    }

}
