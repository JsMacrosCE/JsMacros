package com.jsmacrosce.jsmacros.client.api.helper;

import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.client.api.library.impl.FChat;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Wraps a Minecraft text component, which can contain text with different styles and click or
 * hover behavior.
 * <p>
 * {@link #visit(com.jsmacrosce.jsmacros.core.MethodWrapper)} visits each text run with its
 * {@link StyleHelper}; {@link #getString()} returns the flattened text without styling.
 * <p>
 * Create helpers from strings, translation keys, or component JSON with the corresponding
 * {@code Chat.createTextHelperFrom...} methods. {@link #replaceFromString(String)} replaces this
 * helper's component; it does not update text already displayed elsewhere.
 * <p>
 * <pre>
 * // Read the component's flattened text
 * const line = Chat.createTextHelperFromString("some styled line");
 * Chat.log(`flattened: ${line.getString()}`);
 *
 * // Inspect each formatted run
 * line.visit(JavaWrapper.methodToJava(function (style, run) {
 *   if (style.bold()) {
 *     Chat.log(`bold run: ${run}`);
 *   }
 * }));
 *
 * // Build translated text for display
 * Chat.actionbar(Chat.createTextHelperFromTranslationKey("block.minecraft.stone"));
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.0.8
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class TextHelper extends BaseHelper<Component> {
    private static final Minecraft mc = Minecraft.getInstance();

    /**
     * Matches a section sign followed by a legacy formatting code ({@code 0-9}, {@code A-F},
     * {@code K-O}, or {@code R}), case-insensitively. Used by
     * {@link #getStringStripFormatting()} and {@link #withoutFormatting()}.
     * @since 1.0.8
     */
    public static final Pattern STRIP_FORMATTING_PATTERN = Pattern.compile("\u00a7[0-9A-FK-OR]", Pattern.CASE_INSENSITIVE);

    private TextHelper(Component t) {
        super(t);
    }

    /**
     * Wraps a component in a helper, retaining the supplied component as-is.
     *
     * @param t the component to wrap, or {@code null}
     * @return a new helper retaining {@code t}, or {@code null} when {@code t} is {@code null}
     */
    public static TextHelper wrap(Component t) {
        if (t != null) {
            return new TextHelper(t);
        } else {
            return null;
        }
    }

    /**
     * This deprecated method is unsupported and always throws; use
     * {@link FChat#createTextHelperFromJSON(String)} to create a helper from component JSON.
     *
     * @param json ignored
     * @return never returns
     * @since 1.0.8
     * @deprecated use {@link FChat#createTextHelperFromJSON(String)} instead.
     */
    @DocletIgnore
    @Deprecated
    public String replaceFromJson(String json) {
        throw new UnsupportedOperationException("replaceFromJson is deprecated, use FChat.createTextHelperFromJSON instead.");
    }

    /**
     * Replaces this helper's component with a literal string; this does not update text already
     * displayed elsewhere.
     *
     * @param content literal text for the replacement component
     * @return this helper
     * @since 1.0.8
     * @deprecated use {@link FChat#createTextHelperFromString(String)} instead.
     */
    @DocletIgnore
    @Deprecated
    public TextHelper replaceFromString(String content) {
        base = Component.literal(content);
        return this;
    }

    /**
     * @return the component serialized as JSON using the current player's registry access
     * @since 1.2.7
     */
    public String getJson() {
        var registryManager = Objects.requireNonNull(mc.player).registryAccess();
        return String.valueOf(ComponentSerialization.CODEC.encodeStart(registryManager.createSerializationContext(JsonOps.INSTANCE), base).getOrThrow());
    }

    /**
     * @return the component's flattened text, without style information
     * @since 1.2.7
     */
    public String getString() {
        return base.getString();
    }

    /**
     * @return the flattened text with legacy section-sign formatting codes removed
     * @since 1.6.5
     */
    public String getStringStripFormatting() {
        return STRIP_FORMATTING_PATTERN.matcher(base.getString()).replaceAll("");
    }

    /**
     * @return a new helper containing the flattened text with legacy section-sign formatting codes removed
     * @since 1.8.4
     */
    public TextHelper withoutFormatting() {
        return TextHelper.wrap(Component.literal(getStringStripFormatting()));
    }

    /**
     * Visits each text run with its style and text, then returns this helper.
     *
     * @param visitor callback receiving a {@link StyleHelper} and the run's text
     * @return self for chaining
     * @since 1.6.5
     */
    public TextHelper visit(MethodWrapper<StyleHelper, String, Object, ?> visitor) {
        base.visit((style, string) -> {
            visitor.accept(new StyleHelper(style), string);
            return Optional.empty();
        }, base.getStyle());
        return this;
    }

    /**
     * @return the rendered width of the component in client font units
     * @since 1.8.4
     */
    public int getWidth() {
        return Minecraft.getInstance().font.width(base);
    }

    /**
     * @return the component serialized as JSON using the current player's registry access
     * @since 1.0.8
     * @deprecated confusing name, use {@link #getJson()} instead.
     */
    @DocletIgnore
    @Deprecated
    public String toJson() {
        return getJson();
    }

    /**
     * @return a {@code TextHelper:{"text": "..."}} representation containing the flattened text
     * @since 1.0.8, this used to do the same as {@link #getString}
     */
    @Override
    public String toString() {
        return String.format("TextHelper:{\"text\": \"%s\"}", base.getString());
    }

}
