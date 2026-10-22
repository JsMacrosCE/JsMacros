package com.jsmacrosce.jsmacros.client.gui.editor.highlighting.scriptimpl;

import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.gui.editor.highlighting.AbstractRenderCodeCompiler;
import com.jsmacrosce.jsmacros.client.gui.editor.highlighting.AutoCompleteSuggestion;
import com.jsmacrosce.jsmacros.client.gui.screens.EditorScreen;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.language.EventContainer;
import com.jsmacrosce.wagyourgui.overlays.ConfirmOverlay;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/** Compiles editor text and autocomplete/context-menu data by executing its configured code-compile script trigger. */
public class ScriptCodeCompiler extends AbstractRenderCodeCompiler {
    private final ScriptTrigger scriptTrigger;
    private Component[] compiledText = new Component[]{Component.literal("")};
    private MethodWrapper<Integer, Object, Map<String, MethodWrapper<Object, Object, Object, ?>>, ?> getRClickActions = null;
    private List<AutoCompleteSuggestion> suggestions = new LinkedList<>();

    /** Stores the syntax language and editor screen, and configures the {@code CodeCompile} event trigger for the script file. The trigger is not executed here.
     * @param language syntax-language identifier
     * @param screen editor that receives compilation outputs
     * @param scriptFile configured compiler script file
     */
    public ScriptCodeCompiler(String language, EditorScreen screen, File scriptFile) {
        super(language, screen);
        scriptTrigger = new ScriptTrigger(ScriptTrigger.TriggerType.EVENT, "CodeCompile", scriptFile.toPath(), true, true);
    }

    /** Runs the configured script with a {@code CodeCompileEvent} containing the source text. When an execution container is returned, attempts to await it before consuming the event's rendered lines, context-menu callback, and suggestions. An interrupted or spuriously returning wait can allow output consumption before the handler finishes. Rendered lines must wrap mutable components; their existing styles are replaced with the editor's default style.
     * @param text source text supplied to the compilation event
     */
    @Override
    public void recompileRenderedText(@NotNull String text) {
        CodeCompileEvent compileEvent = new CodeCompileEvent(text, language, screen);
        EventContainer<?> t = JsMacrosClient.clientCore.exec(scriptTrigger, compileEvent, null, (ex) -> {
            Font renderer = Minecraft.getInstance().font;
            StringWriter st = new StringWriter();
            ex.printStackTrace(new PrintWriter(st));
            Component error = Component.literal(st.toString().replaceAll("\r", "").replaceAll("\t", "    ")).setStyle(EditorScreen.defaultStyle);
            screen.openOverlay(new ConfirmOverlay(screen.width / 4, screen.height / 4, screen.width / 2, screen.height / 2, false, renderer, error, screen, (e) -> screen.openParent()));
        });
        if (t != null) {
            try {
                t.awaitLock(null);
            } catch (InterruptedException ignored) {
            }
        }
        getRClickActions = compileEvent.rightClickActions;
        compiledText = compileEvent.textLines.stream().map(e -> ((MutableComponent) e.getRaw()).setStyle(EditorScreen.defaultStyle)).toArray(Component[]::new);
        suggestions = compileEvent.autoCompleteSuggestions;
    }

    /** Returns the context-menu actions produced by the stored callback for the supplied character index. A missing callback or null callback result yields a new empty map; callback failures are passed to the profile error logger. A non-null callback map is returned by reference without validation.
     * @param index code-character index passed to the callback
     * @return callback labels mapped to actions, or a new empty map if no callback or result is available
     */
    @NotNull
    @Override
    public Map<String, Runnable> getRightClickOptions(int index) {
        if (getRClickActions == null) {
            return new HashMap<>();
        }
        Map<String, ? extends Runnable> results = null;
        try {
            results = getRClickActions.apply(index);
        } catch (Throwable e) {
            JsMacrosClient.clientCore.profile.logError(e);
        }
        if (results == null) {
            return new LinkedHashMap<>();
        }
        return (Map<String, Runnable>) results;
    }

    /** Returns the stored rendered lines. Before recompilation this is a one-element array containing an empty text component; after recompilation each line is cast to a mutable component and its style is replaced with the editor's default style. The returned array is the stored array.
     * @return current rendered-line array
     */
    @NotNull
    @Override
    public Component[] getRenderedText() {
        return compiledText;
    }

    /** Returns the stored autocomplete suggestion list, initially empty and replaced with the list produced by the latest compilation event. The returned list is shared, not copied.
     * @return current suggestion list
     */
    @NotNull
    @Override
    public List<AutoCompleteSuggestion> getSuggestions() {
        return suggestions;
    }

}
