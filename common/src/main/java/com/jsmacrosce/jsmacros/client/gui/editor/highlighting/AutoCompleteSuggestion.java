package com.jsmacrosce.jsmacros.client.gui.editor.highlighting;

import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.network.chat.Component;
import com.jsmacrosce.jsmacros.client.gui.screens.EditorScreen;

/**
 * Describes an editor autocomplete replacement and its dropdown label.
 * <pre>
 * const Suggestion = Java.type("com.jsmacrosce.jsmacros.client.gui.editor.highlighting.AutoCompleteSuggestion");
 * const item = new Suggestion(0, "Chat.log");
 * </pre>
 */
public class AutoCompleteSuggestion {
    /** Zero-based UTF-16 index where replacement begins; acceptance replaces through the cursor index. */
    public final int startIndex;
    /** String inserted in place of the text from {@code startIndex} to the cursor. */
    public final String suggestion;
    /** Component shown as the dropdown label; it need not match {@code suggestion}. */
    public final Component displayText;

    /** Creates a suggestion whose label is styled from its replacement text.
     * @param startIndex zero-based UTF-16 index where replacement begins
     * @param suggestion replacement string
     */
    public AutoCompleteSuggestion(int startIndex, String suggestion) {
        this.suggestion = suggestion;
        this.startIndex = startIndex;
        this.displayText = Component.literal(suggestion).setStyle(EditorScreen.defaultStyle);
    }

    /** Uses the supplied component as the dropdown label, separately from the replacement string.
     * @param startIndex zero-based UTF-16 index where replacement begins
     * @param suggestion replacement string
     * @param displayText component shown in the dropdown
     */
    public AutoCompleteSuggestion(int startIndex, String suggestion, Component displayText) {
        this.suggestion = suggestion;
        this.startIndex = startIndex;
        this.displayText = displayText;
    }

}
