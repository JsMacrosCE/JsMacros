package com.jsmacrosce.jsmacros.client.gui.editor.highlighting.scriptimpl;

import com.jsmacrosce.doclet.DocletIgnore;
import io.noties.prism4j.Prism4j;
import com.jsmacrosce.StringHashTrie;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.TextBuilder;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.config.ClientConfigV2;
import com.jsmacrosce.jsmacros.client.gui.editor.SelectCursor;
import com.jsmacrosce.jsmacros.client.gui.editor.highlighting.AutoCompleteSuggestion;
import com.jsmacrosce.jsmacros.client.gui.editor.highlighting.Prism;
import com.jsmacrosce.jsmacros.client.gui.editor.highlighting.impl.TextStyleCompiler;
import com.jsmacrosce.jsmacros.client.gui.screens.EditorScreen;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Event passed to the editor's script-based code compiler. Populate its rendered lines, autocomplete suggestions, and
 * optional context-menu callback while handling it; the editor reads these outputs after the handler completes.
 *
 * @author Wagyourtail
 * @since 1.3.1
 */
@DocletCategory("Render/UI")
@Event("CodeRender")
@SuppressWarnings("unused")
public class CodeCompileEvent extends BaseEvent {
    /** Cursor object captured from the supplied screen; its state is shared, not copied. */
    public final SelectCursor cursor;
    /** Source text supplied for this compilation, rather than a live editor-content accessor. */
    public final String code;
    /** Syntax-language identifier supplied for this compilation; it is not necessarily a file extension or supported grammar. */
    public final String language;
    /** Editor screen supplied for this compilation, retained by reference. */
    public final EditorScreen screen;

    /** Rendered lines to display after handling; each line is reset to the editor's default style. */
    public final List<TextHelper> textLines = new LinkedList<>();

    /** Autocomplete suggestions exposed for the current code after handling. */
    public final List<AutoCompleteSuggestion> autoCompleteSuggestions = new LinkedList<>();

    /** Callback supplying context-menu actions for a character index; {@code null} means there are no actions. */
    public MethodWrapper<Integer, Object, Map<String, MethodWrapper<Object, Object, Object, ?>>, ?> rightClickActions;

    /** Stores compilation inputs and captures the screen's cursor; does not run the compiler or dispatch the handler.
     * @param code source text to render
     * @param language syntax-language identifier
     * @param screen editor screen supplying the cursor
     */
    public CodeCompileEvent(String code, String language, EditorScreen screen) {
        super(JsMacrosClient.clientCore);
        this.code = code;
        this.language = language;
        this.screen = screen;
        this.cursor = screen.cursor;
    }

    /** Returns Prism syntax nodes for this code and language; an unsupported language causes a {@link NullPointerException}.
     * @return the generated Prism nodes
     */
    public List<Prism4j.Node> genPrismNodes() {
        return Prism.getNodes(code, language);
    }

    /** Creates an insertion-ordered map suitable as the result of {@link #rightClickActions}.
     * @return an empty insertion-ordered map
     */
    public Map<?, ?> createMap() {
        return new LinkedHashMap<>();
    }

    /** Creates a text builder for constructing a rendered line in {@link #textLines}.
     * @return a new text builder
     */
    public TextBuilder createTextBuilder() {
        return new TextBuilder();
    }

    /** Creates an autocomplete suggestion using its suggestion text as the display text.
     * @param startIndex index in the code at which the replaced portion begins
     * @param suggestion replacement text
     * @return the created suggestion
     */
    public AutoCompleteSuggestion createSuggestion(int startIndex, String suggestion) {
        return createSuggestion(startIndex, suggestion, null);
    }

    /** Creates an autocomplete suggestion, optionally with separate dropdown display text.
     * @param startIndex index in the code at which the replaced portion begins
     * @param suggestion replacement text
     * @param displayText dropdown text; {@code null} uses {@code suggestion}
     * @return the created suggestion
     */
    public AutoCompleteSuggestion createSuggestion(int startIndex, String suggestion, TextHelper displayText) {
        if (displayText == null) {
            return new AutoCompleteSuggestion(startIndex, suggestion);
        }
        return new AutoCompleteSuggestion(startIndex, suggestion, displayText.getRaw());
    }

    /** Creates an empty prefix tree for autocomplete data.
     * @return an empty prefix tree
     */
    public StringHashTrie createPrefixTree() {
        return new StringHashTrie();
    }

    /** Returns syntax-theme RGB components keyed by token name; the first call initializes and saves default colors if none are configured.
     * @return theme colors keyed by token name, with each color represented by three RGB components
     */
    public Map<String, short[]> getThemeData() {
        return JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).getThemeData();
    }

}
