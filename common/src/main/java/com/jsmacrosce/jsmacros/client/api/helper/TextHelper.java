package com.jsmacrosce.jsmacros.client.api.helper;

import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.library.impl.FChat;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * a piece of rich text: some text, plus the colours, decorations and click or hover behaviour
 * that go with it.
 * <p>
 * This is the game's own text object wrapped, which means it is not a flat string. One helper
 * can hold several runs with different formatting inside it, and
 * {@link #visit(com.jsmacrosce.jsmacros.core.MethodWrapper)} is how a script gets at them one at
 * a time, handing each run's {@link StyleHelper} alongside the run's own text;
 * {@link #getString()} flattens the whole thing to text and throws the formatting away.
 * <p>
 * A helper is not something a script builds from nothing. It arrives as the return value of
 * something — a name tag, an item name, a menu label, a chat line — and the three
 * {@code Chat} calls that do take text build one from a plain string, a translation key or JSON:
 * {@code Chat.createTextHelperFromString}, {@code Chat.createTextHelperFromTranslationKey} and
 * {@code Chat.createTextHelperFromJSON}. The display calls, such as {@code Chat.log} and
 * {@code Chat.actionbar}, take one of those directly, or any other value and use its
 * {@code toString()}.
 * <br>
 * Two things follow from wrapping the game's object rather than copying it. Reading
 * {@link #getWidth()} asks the client's font, so it is only meaningful once that font is
 * loaded, and writing through {@link #replaceFromString(String)} replaces the text this helper
 * was built around rather than changing anything the game is showing.
 * example:
 * <pre>
 * // build one, then read it back as flat text
 * const line = Chat.createTextHelperFromString("some styled line");
 * Chat.log(`flattened: ${line.getString()}`);
 *
 * // the formatting is per run, so visit walks the text
 * line.visit(JavaWrapper.methodToJava(function (style, run) {
 *   if (style.bold()) {
 *     Chat.log(`bold run: ${run}`);
 *   }
 * }));
 *
 * // a translation key is built the same way and handed straight to a display call
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
     * the pattern matching the old section-symbol formatting codes in a plain string.
     * <p>
     * This is what {@link #getStringStripFormatting()} and {@link #withoutFormatting()} use to
     * strip the formatting out of a message some server sent as plain text with the codes typed
     * into it, which is still how a few of them do it. It matches a section symbol followed by
     * one of the code characters and nothing else, so a lone section symbol, or a section
     * symbol followed by something that is not a code, is left alone. The characters it matches
     * are twenty-two: the sixteen colour characters, {@code 0} to {@code 9} and {@code A} to
     * {@code F}, and six modifier characters, {@code K} to {@code O} plus {@code R}. Note that
     * {@code P} and {@code Q} are not codes and are not matched, so the modifiers are not a
     * single unbroken run up to {@code R}. The pattern is case-insensitive, so a lower case
     * code is matched too.
     * <p>
     * A script that wants the same thing on a string of its own can use this directly, which is
     * the easy way to ask whether a message carries any formatting at all. Note that the
     * generated TypeScript types {@code CharSequence} as a Java interface a JavaScript string
     * does not satisfy, so calling {@code matcher} on a plain string from a typed script is
     * reported as a type error even though it works at runtime; a script that only needs
     * {@link #withoutFormatting()} should use that instead.
     * @since 1.0.8
     */
    public static final Pattern STRIP_FORMATTING_PATTERN = Pattern.compile("\u00a7[0-9A-FK-OR]", Pattern.CASE_INSENSITIVE);

    private TextHelper(Component t) {
        super(t);
    }

    public static TextHelper wrap(Component t) {
        if (t != null) {
            return new TextHelper(t);
        } else {
            return null;
        }
    }

    /**
     * replace the text in this class with JSON data.
     *
     * @param json
     * @return
     * @since 1.0.8
     * @deprecated use {@link FChat#createTextHelperFromJSON(String)} instead.
     */
    @Deprecated
    public String replaceFromJson(String json) {
        throw new UnsupportedOperationException("replaceFromJson is deprecated, use FChat.createTextHelperFromJSON instead.");
    }

    /**
     * replace the text in this class with {@link String} data.
     *
     * @param content
     * @return
     * @since 1.0.8
     * @deprecated use {@link FChat#createTextHelperFromString(String)} instead.
     */
    @Deprecated
    public TextHelper replaceFromString(String content) {
        base = Component.literal(content);
        return this;
    }

    /**
     * @return JSON data representation.
     * @since 1.2.7
     */
    public String getJson() {
        var registryManager = Objects.requireNonNull(mc.player).registryAccess();
        return String.valueOf(ComponentSerialization.CODEC.encodeStart(registryManager.createSerializationContext(JsonOps.INSTANCE), base).getOrThrow());
    }

    /**
     * @return the text content.
     * @since 1.2.7
     */
    public String getString() {
        return base.getString();
    }

    /**
     * @return the text content. stripped formatting when servers send it the (super) old way due to shitty coders.
     * @since 1.6.5
     */
    public String getStringStripFormatting() {
        return STRIP_FORMATTING_PATTERN.matcher(base.getString()).replaceAll("");
    }

    /**
     * @return the text helper without the formatting applied.
     * @since 1.8.4
     */
    public TextHelper withoutFormatting() {
        return TextHelper.wrap(Component.literal(getStringStripFormatting()));
    }

    /**
     * @param visitor function with 2 args, no return.
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
     * @return the width of this text.
     * @since 1.8.4
     */
    public int getWidth() {
        return Minecraft.getInstance().font.width(base);
    }

    /**
     * @return
     * @since 1.0.8
     * @deprecated confusing name, use {@link #getJson()} instead.
     */
    @Deprecated
    public String toJson() {
        return getJson();
    }

    /**
     * @return String representation of text helper.
     * @since 1.0.8, this used to do the same as {@link #getString}
     */
    @Override
    public String toString() {
        return String.format("TextHelper:{\"text\": \"%s\"}", base.getString());
    }

}
