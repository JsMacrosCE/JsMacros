package com.jsmacrosce.jsmacros.client.gui.editor.highlighting;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import com.jsmacrosce.jsmacros.client.gui.screens.EditorScreen;

import java.util.List;
import java.util.Map;

public abstract class AbstractRenderCodeCompiler {
    protected final EditorScreen screen;
    protected final String language;

    public AbstractRenderCodeCompiler(String language, EditorScreen screen) {
        this.language = language;
        this.screen = screen;
    }

    public abstract void recompileRenderedText(@NotNull String text);

    @NotNull
    public abstract Map<String, Runnable> getRightClickOptions(int index);

    @NotNull
    public abstract Component[] getRenderedText();

    @NotNull
    public abstract List<AutoCompleteSuggestion> getSuggestions();

}
