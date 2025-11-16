package xyz.wagyourtail.jsmacros.client.gui.editor.highlighting.scriptimpl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.client.gui.editor.highlighting.AbstractRenderCodeCompiler;
import xyz.wagyourtail.jsmacros.client.gui.editor.highlighting.AutoCompleteSuggestion;
import xyz.wagyourtail.jsmacros.client.gui.screens.EditorScreen;
import xyz.wagyourtail.jsmacros.core.MethodWrapper;
import xyz.wagyourtail.jsmacros.core.config.ScriptTrigger;
import xyz.wagyourtail.jsmacros.core.language.EventContainer;
import xyz.wagyourtail.wagyourgui.overlays.ConfirmOverlay;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * @author Wagyourtail
 */
public class ScriptCodeCompiler extends AbstractRenderCodeCompiler {
    private final ScriptTrigger scriptTrigger;
    private Component[] compiledText = new Component[]{Component.literal("")};
    private MethodWrapper<Integer, Object, Map<String, MethodWrapper<Object, Object, Object, ?>>, ?> getRClickActions = null;
    private List<AutoCompleteSuggestion> suggestions = new LinkedList<>();

    public ScriptCodeCompiler(String language, EditorScreen screen, File scriptFile) {
        super(language, screen);
        scriptTrigger = new ScriptTrigger(ScriptTrigger.TriggerType.EVENT, "CodeCompile", scriptFile.toPath(), true, true);
    }

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

    @NotNull
    @Override
    public Component[] getRenderedText() {
        return compiledText;
    }

    @NotNull
    @Override
    public List<AutoCompleteSuggestion> getSuggestions() {
        return suggestions;
    }

}
