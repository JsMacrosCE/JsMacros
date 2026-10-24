package com.jsmacrosce.jsmacros.client.api.helper;

import net.minecraft.ChatFormatting;
import com.jsmacrosce.jsmacros.client.util.FormattingCompat;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.access.CustomClickEvent;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.stream.Collectors;

/**
 * Exposes the formatting and click/hover events stored on a text style.
 * <p>
 * {@link TextHelper#visit(com.jsmacrosce.jsmacros.core.MethodWrapper)} supplies a style for each
 * visited text run. These accessors report properties stored on that style, not formatting
 * inherited from surrounding text.
 * <p>
 * Decoration accessors return booleans. For colours, {@link #getColorIndex()} and
 * {@link #getColorValue()} return {@code -1} when there is no legacy named colour; use
 * {@link #getCustomColor()} for the packed RGB of either colour kind, or {@code -1} when unset.
 * {@link #hasColor()} and {@link #hasCustomColor()} distinguish unset, legacy, and custom colours.
 * <p>
 * Click and hover accessors expose the action separately from its value. JsMacros-registered
 * clicks report {@code "custom"}; retrieve their runnable with {@link #getCustomClickValue()}.
 * <pre>
 * // a style is per run, so visit walks the text and gives one to each run
 * const text = Chat.createTextHelperFromString("some styled line");
 * text.visit(JavaWrapper.methodToJava(function (style, run) {
 *   if (style.hasColor()) {
 *     if (style.hasCustomColor()) {
 *       // an arbitrary rgb colour, packed as 0xRRGGBB
 *       Chat.log(`${run} is colour #${style.getCustomColor().toString(16)}`);
 *     } else {
 *       // one of the sixteen legacy named colours
 *       Chat.log(`${run} is ${style.getColorName()}, index ${style.getColorIndex()}`);
 *     }
 *   }
 *   if (style.underlined()) {
 *     Chat.log(`${run} is underlined`);
 *   }
 *   const click = style.getClickAction();
 *   if (click !== null) {
 *     Chat.log(`${run} click does ${click} ${style.getClickValue()}`);
 *   }
 * }));
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.6.5
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class StyleHelper extends BaseHelper<Style> {
    /** Wraps the supplied raw text style reference without copying or parsing it.
     * @param base raw text style to wrap
     */
    public StyleHelper(Style base) {
        super(base);
    }

    /**
     * Whether this style has a colour set.
     *
     * @return {@code true} if a colour is set, {@code false} otherwise
     * @since 1.6.5
     */
    public boolean hasColor() {
        return base.getColor() != null;
    }

    /**
     * @return the legacy colour index, or {@code -1} if unset or custom
     * @deprecated use {@link #getColorIndex()} instead.
     */
    @DocletIgnore
    @Deprecated
    public int getColor() {
        return getColorIndex();
    }

    /**
     * Returns the legacy formatting for this style's colour. An arbitrary RGB colour returns
     * {@code null}; when no colour is set, this method throws {@link java.lang.NullPointerException}.
     *
     * @return the legacy formatting, or {@code null} for an arbitrary rgb colour
     * @since 1.8.4
     */
    @Nullable
    public FormattingHelper getFormatting() {
        ChatFormatting f = FormattingCompat.getByName(base.getColor().serialize());
        return f == null ? null : new FormattingHelper(f);
    }

    /**
     * Returns this style's legacy colour index. Custom RGB colours are not assigned a legacy
     * index.
     *
     * @return the legacy colour index, or {@code -1} if no colour is set or it is an arbitrary rgb colour
     * @since 1.8.4
     */
    public int getColorIndex() {
        if (base.getColor() == null) {
            return -1;
        }
        ChatFormatting f = FormattingCompat.getByName(base.getColor().serialize());
        return f == null ? -1 : FormattingCompat.getId(f);
    }

    /**
     * Returns the RGB value associated with a legacy named colour; custom RGB colours and
     * unset colours return {@code -1}.
     *
     * @return the color value of this style or {@code -1} if it doesn't have one.
     * @since 1.8.4
     */
    public int getColorValue() {
        if (base.getColor() == null) {
            return -1;
        }
        ChatFormatting f = FormattingCompat.getByName(base.getColor().serialize());
        return f == null ? -1 : FormattingCompat.getColorValue(f);
    }

    /**
     * Returns the serialized colour of this style, or {@code null} if unset. Legacy colours
     * serialize by name and custom colours as hash-prefixed hexadecimal RGB.
     *
     * @return the color name of this style or {@code null} if it has no color.
     * @since 1.8.4
     */
    @Nullable
    public String getColorName() {
        return base.getColor() == null ? null : base.getColor().serialize();
    }

    /**
     * Returns whether this style has a custom RGB colour. Returns {@code false} when unset; use
     * {@link #hasColor()} to distinguish unset from a legacy colour.
     *
     * @return {@code true} if this run has a colour that is not one of the legacy named ones
     * @since 1.6.5
     */
    public boolean hasCustomColor() {
        return base.getColor() != null && base.getColor().serialize().startsWith("#");
    }

    /**
     * Returns this style's colour as packed {@code 0xRRGGBB}, whether custom or legacy, or
     * {@code -1} if unset. Use {@link #hasCustomColor()} to distinguish custom from legacy.
     *
     * @return the packed rgb value of this run's colour, or {@code -1} if it has no colour
     * @since 1.6.5
     */
    public int getCustomColor() {
        return base.getColor() == null ? -1 : base.getColor().getValue();
    }

    /**
     * Returns whether this style has its bold flag set; inherited formatting is not included.
     *
     * @return {@code true} if this run is bold, {@code false} otherwise
     * @since 1.6.5
     */
    public boolean bold() {
        return base.isBold();
    }

    /**
     * Returns whether this style has its italic flag set; inherited formatting is not included.
     *
     * @return {@code true} if this run is italic, {@code false} otherwise
     * @since 1.6.5
     */
    public boolean italic() {
        return base.isItalic();
    }

    /**
     * Returns whether this style has its underline flag set; inherited formatting is not included.
     *
     * @return {@code true} if this run is underlined, {@code false} otherwise
     * @since 1.6.5
     */
    public boolean underlined() {
        return base.isUnderlined();
    }

    /**
     * Returns whether this style has its strikethrough flag set; inherited formatting is not included.
     *
     * @return {@code true} if this run is struck through, {@code false} otherwise
     * @since 1.6.5
     */
    public boolean strikethrough() {
        return base.isStrikethrough();
    }

    /**
     * Returns whether this style has its obfuscated flag set; inherited formatting is not included.
     *
     * @return {@code true} if this run is obfuscated, {@code false} otherwise
     * @since 1.6.5
     */
    public boolean obfuscated() {
        return base.isObfuscated();
    }

    /**
     * Returns the click action's enum name, or {@code null} when no click event is set. Built-in
     * action names are uppercase; JsMacros-registered clicks return {@code "custom"} and their
     * runnable is available from {@link #getCustomClickValue()}. Other action values are available
     * from {@link #getClickValue()}.
     *
     * @return the click action's enum constant name in upper case, {@code "custom"} for a
     * JsMacros-registered command, or {@code null} if this run has no click action
     * @since 1.6.5
     */
    @DocletReplaceReturn("TextClickAction | 'custom' | null")
    @Nullable
    public String getClickAction() {
        if (base.getClickEvent() == null) {
            return null;
        }
        if (base.getClickEvent() instanceof CustomClickEvent) {
            return "custom";
        }
        return base.getClickEvent().action().name();
    }

    /**
     * the text argument for a click action, or {@code null} when the action has no text value.
     * {@link #getClickAction()} identifies the action. A page number is returned as a decimal
     * string; the {@code "custom"} and {@code SHOW_DIALOG} actions have no text value.
     *
     * @return the click action's argument as a string, or {@code null} if there is no click
     * action or the action is {@code "custom"} or {@code SHOW_DIALOG}
     * @since 1.6.5
     */
    @Nullable
    public String getClickValue() {
        return switch (base.getClickEvent()) {
            case ClickEvent.OpenUrl ce -> ce.uri().toString();
            case ClickEvent.OpenFile ce -> ce.path();
            case ClickEvent.RunCommand ce -> ce.command();
            case ClickEvent.SuggestCommand ce -> ce.command();
            case ClickEvent.ChangePage ce -> Integer.toString(ce.page());
            case ClickEvent.CopyToClipboard ce -> ce.value();
            case null, default -> null;
        };
    }

    /**
     * the runnable stored by a JsMacros-registered click, or {@code null} for other click events.
     * {@link #getClickAction()} reports {@code "custom"} for that event.
     *
     * @return the runnable to call for a {@code "custom"} click, or {@code null} if this run's
     * click action is not {@code "custom"}
     * @since 1.6.5
     */
    @Nullable
    public Runnable getCustomClickValue() {
        if (base.getClickEvent() instanceof CustomClickEvent ce) {
            return ce.event();
        }
        return null;
    }

    /**
     * the serialized name of this run's hover action, or {@code null} if it has no hover event.
     * Unlike {@link #getClickAction()}, this name is lowercase (for example,
     * {@code show_item}). The action's value is returned by {@link #getHoverValue()}.
     *
     * @return the hover action's name, or {@code null} if this run has no hover action
     * @since 1.6.5
     */
    @DocletReplaceReturn("TextHoverAction | null")
    @Nullable
    public String getHoverAction() {
        return base.getHoverEvent() == null ? null : base.getHoverEvent().action().getSerializedName();
    }

    /**
     * the value shown by this run's hover event, or {@code null} when there is no recognized event.
     * <p>
     * The type of the answer depends on what {@link #getHoverAction()} said, and there are
     * three shapes: a {@code show_text} hover gives a {@link TextHelper} for the text shown;
     * a {@code show_item} hover gives an {@link ItemStackHelper} for the item shown; and a
     * {@code show_entity} hover gives a <i>list</i> of {@link TextHelper}, one per line of the
     * entity tooltip. A run with no hover gives {@code null}, as does any hover action the game
     * does not recognise.
     * <p>
     * The result has different shapes, so use {@link #getHoverAction()} to distinguish them.
     * For {@code show_item}, it is an {@link ItemStackHelper} wrapping the item value.
     * <p>
     * <pre>
     * const text = Chat.createTextHelperFromString("hover me");
     * text.visit(JavaWrapper.methodToJava(function (style, run) {
     *   const action = style.getHoverAction();
     *   if (action === null) {
     *     return;
     *   }
     *   const value = style.getHoverValue();
     *   if (value === null) {
     *     return;
     *   }
     *   if (action === "show_text") {
     *     // a single text helper
     *     Chat.log(`hover text: ${value.getString()}`);
     *   } else if (action === "show_entity") {
     *     // a list of text helpers, one per tooltip line
     *     for (const line of value) {
     *       Chat.log(`tooltip: ${line.getString()}`);
     *     }
     *   } else {
     *     // a single item helper
     *     Chat.log(`hover item: ${value.getItemId()}`);
     *   }
     * }));
     * </pre>
     *
     * @return a {@link TextHelper} for a {@code show_text} hover, an {@link ItemStackHelper} for
     * a {@code show_item} hover, a list of {@link TextHelper} for a {@code show_entity} hover,
     * or {@code null} if there is no recognized hover action
     * @since 1.6.5
     */
    @Nullable
    public Object getHoverValue() {
        return switch (base.getHoverEvent()) {
            case HoverEvent.ShowText s -> TextHelper.wrap(s.value());
            //? if >=26.1 {
            /*case HoverEvent.ShowItem i -> new ItemStackHelper(i.item().create());
            *///?} else {
            case HoverEvent.ShowItem i -> new ItemStackHelper(i.item());
            //?}
            case HoverEvent.ShowEntity e -> e.entity().getTooltipLines().stream().map(TextHelper::wrap).collect(Collectors.toList());
            case null, default -> null;
        };
    }

    /**
     * the text inserted for this run, or {@code null} if it has no insertion.
     *
     * @return the insertion text, or {@code null} if this run has none
     * @since 1.6.5
     */
    public String getInsertion() {
        return base.getInsertion();
    }

    /** Returns a diagnostic summary of the wrapped style's stored properties.
     * @return a diagnostic representation of the wrapped style
     */
    @Override
    public String toString() {
        return "StyleHelper:{\"color\": \"" + (hasColor() ? hasCustomColor() ? getCustomColor() : String.format("%x", getColorIndex()) : "none") + "\"" +
                ", \"bold\": " + bold() +
                ", \"italic\": " + italic() +
                ", \"underlined\": " + underlined() +
                ", \"strikethrough\": " + strikethrough() +
                ", \"obfuscated\": " + obfuscated() +
                ", \"clickAction\": \"" + getClickAction() + "\"" +
                ", \"clickValue\": \"" + getClickValue() + "\"" +
                ", \"customClickValue\": " + getCustomClickValue() +
                ", \"hoverAction\": \"" + getHoverAction() + "\"" +
                ", \"hoverValue\": " + getHoverValue() +
                ", \"insertion\": \"" + getInsertion() + "\"" +
                "}";
    }

}
