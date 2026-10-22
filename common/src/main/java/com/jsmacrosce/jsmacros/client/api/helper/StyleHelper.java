package com.jsmacrosce.jsmacros.client.api.helper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.access.CustomClickEvent;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.stream.Collectors;

/**
 * the formatting on a run of text: its colour, its four text decorations, and what happens when
 * it is clicked or hovered.
 * <p>
 * This is a read-only view of one Brigadier style, and the whole class is reached through
 * {@link TextHelper#visit(com.jsmacrosce.jsmacros.core.MethodWrapper)}: a piece of text is
 * split into runs by whatever the server put in it, and the visitor hands a style for each run
 * along with the text of that run. A run's style is only its <i>own</i> formatting, so a run
 * that inherits its colour from the text around it reports no colour of its own; the game works
 * that out later and the style object here does not.
 * <p>
 * The four decorations — {@link #bold()}, {@link #italic()}, {@link #underlined()} and
 * {@link #strikethrough()}, with {@link #obfuscated()} for the scrambled one — are plain
 * booleans. Note that {@code false} means "not set here" rather than "explicitly off", since a
 * style only records the decorations that were turned on.
 * <br>
 * The colour is the awkward part, because the game has two colour systems and this class exposes
 * both. A colour can be one of the sixteen legacy named ones, in which case it has an index and
 * an rgb value, or it can be an arbitrary rgb value, in which case it has neither. Three of the
 * accessors — {@link #getColor()}, {@link #getColorIndex()} and {@link #getColorValue()} — answer
 * "which legacy colour" and give {@code -1} when the colour is not a legacy one or is not set at
 * all. {@link #getCustomColor()} is the odd one out and the one to reach for: it gives
 * {@code -1} only when there is no colour at all, and the rgb of whichever colour there is, so it
 * covers both systems. {@link #hasColor()} and {@link #hasCustomColor()} are what tell the two
 * systems apart.
 * <p>
 * Clicking and hovering are read in two steps: {@link #getClickAction()} and
 * {@link #getHoverAction()} name what happens, and {@link #getClickValue()} and
 * {@link #getHoverValue()} carry the argument. A JsMacros-registered command reports the action
 * {@code "custom"} and hands back a runnable from {@link #getCustomClickValue()} instead of a
 * string, so that is the one case where the value is not text.
 * example:
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
    public StyleHelper(Style base) {
        super(base);
    }

    /**
     * whether this run has a colour of its own.
     * <p>
     * This is the check the colour accessors need first, since all of them answer {@code -1} or
     * {@code null} when there is no colour rather than throwing. It says nothing about which
     * kind of colour it is: pair it with {@link #hasCustomColor()} for that, or read
     * {@link #getColorName()} and look at whether it starts with a hash.
     * example:
     * <pre>
     * const text = Chat.createTextHelperFromString("a coloured run");
     * text.visit(JavaWrapper.methodToJava(function (style, run) {
     *   if (style.hasColor()) {
     *     Chat.log(`${run} has a colour`);
     *   }
     * }));
     * </pre>
     *
     * @return {@code true} if a colour is set, {@code false} otherwise
     * @since 1.6.5
     */
    public boolean hasColor() {
        return base.getColor() != null;
    }

    /**
     * @return the color index of this style or {@code -1} if no color is set.
     * @deprecated use {@link #getColorIndex()} instead.
     */
    @Deprecated
    public int getColor() {
        return getColorIndex();
    }

    /**
     * the legacy formatting named by this run's colour, if there is one.
     * <p>
     * Only the sixteen legacy named colours have a formatting; an arbitrary rgb colour does not
     * and gives {@code null}. The name is misleading in one respect worth knowing: a run with
     * <b>no</b> colour at all does not give {@code null} here, it throws a
     * {@link java.lang.NullPointerException}, because the lookup reads the
     * colour without checking it is there first. {@link #hasColor()} is therefore not optional
     * before this call.
     * example:
     * <pre>
     * const text = Chat.createTextHelperFromString("a coloured run");
     * text.visit(JavaWrapper.methodToJava(function (style, run) {
     *   // without this check an uncoloured run throws here
     *   if (style.hasColor()) {
     *     const formatting = style.getFormatting();
     *     if (formatting !== null) {
     *       Chat.log(`${run} uses ${formatting.getName()}`);
     *     } else {
     *       Chat.log(`${run} uses an rgb colour, not a legacy one`);
     *     }
     *   }
     * }));
     * </pre>
     *
     * @return the formatting of this style, or {@code null} if no formatting was found.
     * @throws NullPointerException if this run has no colour of its own, which
     * {@link #hasColor()} rules out
     * @since 1.8.4
     */
    @Nullable
    public FormattingHelper getFormatting() {
        ChatFormatting f = ChatFormatting.getByName(base.getColor().serialize());
        return f == null ? null : new FormattingHelper(f);
    }

    /**
     * the position of this run's colour in the sixteen legacy colours, or {@code -1}.
     * <p>
     * For a colour the index runs {@code 0} to {@code 9} for the first ten and then {@code 10} to
     * {@code 15} for the rest, and the character the old formatting code uses is the lowercase
     * hexadecimal digit of that index, so the sixteen read {@code 0} to {@code 9} and then
     * {@code a} to {@code f}. {@code 9} is {@code blue} and {@code 15} is {@code white}, which is
     * code {@code f}; the modifiers come after {@code f} and are not hexadecimal. The index is
     * what the deprecated {@link #getColor()} returns. A run with no colour, and a run coloured
     * with an arbitrary rgb value, both give {@code -1}; {@link #hasColor()} separates the first
     * case and {@link #hasCustomColor()} the second.
     * example:
     * <pre>
     * const text = Chat.createTextHelperFromString("a coloured run");
     * text.visit(JavaWrapper.methodToJava(function (style, run) {
     *   if (style.hasColor()) {
     *     if (!style.hasCustomColor()) {
     *       // the legacy index, which is also the old formatting code's digit
     *       Chat.log(`${run} is legacy colour ${style.getColorIndex()}`);
     *     }
     *   }
     * }));
     * </pre>
     *
     * @return the color index of this style or {@code -1} if no color is set.
     * @since 1.8.4
     */
    public int getColorIndex() {
        if (base.getColor() == null) {
            return -1;
        }
        ChatFormatting f = ChatFormatting.getByName(base.getColor().serialize());
        return f == null ? -1 : f.getId();
    }

    /**
     * the rgb value of this run's colour, or {@code -1}.
     * <p>
     * This is the legacy colour's own rgb value, so the answer depends on which of the sixteen
     * it is and is the same number for every run of that colour. It is the same value the game
     * draws that colour with. A run with no colour, and one coloured with an arbitrary rgb value
     * rather than a legacy name, both give {@code -1}; for the arbitrary value use
     * {@link #getCustomColor()} after {@link #hasCustomColor()}.
     * example:
     * <pre>
     * const text = Chat.createTextHelperFromString("a coloured run");
     * text.visit(JavaWrapper.methodToJava(function (style, run) {
     *   if (style.hasColor()) {
     *     const rgb = style.getColorValue();
     *     if (rgb !== -1) {
     *       Chat.log(`${run} is drawn as #${rgb.toString(16)}`);
     *     }
     *   }
     * }));
     * </pre>
     *
     * @return the color value of this style or {@code -1} if it doesn't have one.
     * @since 1.8.4
     */
    public int getColorValue() {
        if (base.getColor() == null) {
            return -1;
        }
        ChatFormatting f = ChatFormatting.getByName(base.getColor().serialize());
        return f == null || f.getColor() == null ? -1 : f.getColor();
    }

    /**
     * this run's colour written as a string, or {@code null}.
     * <p>
     * The two colour systems come out as two different shapes of string: a legacy colour gives
     * its name, such as {@code "dark_red"}, and an arbitrary rgb colour gives a hash-prefixed
     * six-digit hex value such as {@code "#8B0000"}. So a single check tells the two apart —
     * a leading hash means custom — which is the same test {@link #hasCustomColor()} makes.
     * example:
     * <pre>
     * const text = Chat.createTextHelperFromString("a coloured run");
     * text.visit(JavaWrapper.methodToJava(function (style, run) {
     *   const name = style.getColorName();
     *   if (name !== null) {
     *     // "dark_red" for a legacy colour, "#8B0000" for an arbitrary one
     *     Chat.log(`${run} is ${name}`);
     *   }
     * }));
     * </pre>
     *
     * @return the color name of this style or {@code null} if it has no color.
     * @since 1.8.4
     */
    @Nullable
    public String getColorName() {
        return base.getColor() == null ? null : base.getColor().serialize();
    }

    /**
     * whether this run's colour is an arbitrary rgb value rather than a legacy name.
     * <p>
     * This is the discriminator between the game's two colour systems, and it needs
     * {@link #hasColor()} first: a run with no colour at all is not a custom colour either, so
     * this alone answers {@code false} for both "uncoloured" and "legacy". Where a run is custom
     * the value itself comes from {@link #getCustomColor()}.
     * example:
     * <pre>
     * const text = Chat.createTextHelperFromString("a coloured run");
     * text.visit(JavaWrapper.methodToJava(function (style, run) {
     *   if (style.hasColor()) {
     *     if (style.hasCustomColor()) {
     *       // an rgb value packed as one number, 0xRRGGBB
     *       const rgb = style.getCustomColor();
     *       Chat.log(`${run} is #${rgb.toString(16).padStart(6, "0")}`);
     *     }
     *   }
     * }));
     * </pre>
     *
     * @return {@code true} if this run has a colour that is not one of the legacy named ones
     * @since 1.6.5
     */
    public boolean hasCustomColor() {
        return base.getColor() != null && base.getColor().serialize().startsWith("#");
    }

    /**
     * this run's colour as a packed rgb number.
     * <p>
     * The value is the colour's three channels in one integer, {@code 0xRRGGBB}, with the alpha
     * byte left out, and it is the number to hand to anything wanting a raw colour. A run with
     * no colour gives {@code -1}.
     * <br>
     * The name is a little misleading: this is the rgb value of <i>any</i> colour, so a legacy
     * named colour answers with its rgb value too and not only {@code -1}. Pair it with
     * {@link #hasCustomColor()} when the distinction matters.
     * example:
     * <pre>
     * const text = Chat.createTextHelperFromString("a coloured run");
     * text.visit(JavaWrapper.methodToJava(function (style, run) {
     *   if (style.hasColor()) {
     *     const rgb = style.getCustomColor();
     *     // unpack it into the three channels
     *     const r = Math.floor(rgb / 65536);
     *     const g = Math.floor(rgb / 256) % 256;
     *     const b = rgb % 256;
     *     Chat.log(`${run} is rgb(${r}, ${g}, ${b})`);
     *   }
     * }));
     * </pre>
     *
     * @return the packed rgb value of this run's colour, or {@code -1} if it has no colour
     * @since 1.6.5
     */
    public int getCustomColor() {
        return base.getColor() == null ? -1 : base.getColor().getValue();
    }

    /**
     * whether this run is bold.
     * <p>
     * A run is bold when it carries the bold flag itself. A run inside bold text with no flag of
     * its own is not bold here, because the style records only what was set on it and the
     * inherited bold is worked out by the renderer rather than stored.
     * example:
     * <pre>
     * const text = Chat.createTextHelperFromString("plain and bold");
     * text.visit(JavaWrapper.methodToJava(function (style, run) {
     *   if (style.bold()) {
     *     Chat.log(`${run} is bold`);
     *   }
     * }));
     * </pre>
     *
     * @return {@code true} if this run is bold, {@code false} otherwise
     * @since 1.6.5
     */
    public boolean bold() {
        return base.isBold();
    }

    /**
     * whether this run is italic.
     * <p>
     * The same rule as {@link #bold()}: it answers for this run's own flag, not for what the
     * run inherits from the text around it.
     * example:
     * <pre>
     * const text = Chat.createTextHelperFromString("plain and slanted");
     * text.visit(JavaWrapper.methodToJava(function (style, run) {
     *   if (style.italic()) {
     *     Chat.log(`${run} is italic`);
     *   }
     * }));
     * </pre>
     *
     * @return {@code true} if this run is italic, {@code false} otherwise
     * @since 1.6.5
     */
    public boolean italic() {
        return base.isItalic();
    }

    /**
     * whether this run is underlined.
     * <p>
     * The same rule as {@link #bold()}: it answers for this run's own flag, not for what the
     * run inherits from the text around it.
     * example:
     * <pre>
     * const text = Chat.createTextHelperFromString("plain and underlined");
     * text.visit(JavaWrapper.methodToJava(function (style, run) {
     *   if (style.underlined()) {
     *     Chat.log(`${run} is underlined`);
     *   }
     * }));
     * </pre>
     *
     * @return {@code true} if this run is underlined, {@code false} otherwise
     * @since 1.6.5
     */
    public boolean underlined() {
        return base.isUnderlined();
    }

    /**
     * whether this run is struck through.
     * <p>
     * The same rule as {@link #bold()}: it answers for this run's own flag, not for what the
     * run inherits from the text around it.
     * example:
     * <pre>
     * const text = Chat.createTextHelperFromString("plain and struck through");
     * text.visit(JavaWrapper.methodToJava(function (style, run) {
     *   if (style.strikethrough()) {
     *     Chat.log(`${run} is struck through`);
     *   }
     * }));
     * </pre>
     *
     * @return {@code true} if this run is struck through, {@code false} otherwise
     * @since 1.6.5
     */
    public boolean strikethrough() {
        return base.isStrikethrough();
    }

    /**
     * whether this run is obfuscated, which is the scrambled-text effect.
     * <p>
     * Obfuscated runs are drawn with their characters constantly replaced, and they are the
     * mechanism a server uses to hide part of a message such as the name in a whitelist message.
     * Like the other decorations this answers for this run's own flag rather than for what it
     * inherits.
     * example:
     * <pre>
     * const text = Chat.createTextHelperFromString("plain and scrambled");
     * text.visit(JavaWrapper.methodToJava(function (style, run) {
     *   if (style.obfuscated()) {
     *     // the run's own characters are still readable to a script
     *     Chat.log(`a scrambled run holding ${run}`);
     *   }
     * }));
     * </pre>
     *
     * @return {@code true} if this run is obfuscated, {@code false} otherwise
     * @since 1.6.5
     */
    public boolean obfuscated() {
        return base.isObfuscated();
    }

    /**
     * what clicking this run does, or {@code null} if it does nothing.
     * <p>
     * This is the action's own name, and it is a string rather than a helper so a script can
     * branch on it with a comparison. The value the action needs comes from
     * {@link #getClickValue()}, except for the {@code "custom"} action, which is JsMacros-registered
     * and hands back a runnable from {@link #getCustomClickValue()} instead. A {@code SHOW_DIALOG}
     * action has no value either, and {@link #getClickValue()} gives it {@code null}.
     * <p>
     * The value is the action's enum constant name, so it is <i>uppercase</i>, and it is not the
     * lowercase id the game puts on the wire. The eight constants are {@code OPEN_URL} for
     * {@code open_url}, {@code OPEN_FILE} for {@code open_file}, {@code RUN_COMMAND} for
     * {@code run_command}, {@code SUGGEST_COMMAND} for {@code suggest_command},
     * {@code SHOW_DIALOG} for {@code show_dialog}, {@code CHANGE_PAGE} for {@code change_page},
     * {@code COPY_TO_CLIPBOARD} for {@code copy_to_clipboard}, and {@code CUSTOM} for
     * {@code custom}.
     * <br>
     * That last pair is the one place the case changes, and the reason is that
     * {@code "custom"} in lowercase is hard-coded here. A JsMacros-registered click is
     * recognised by its own event type and reported as that literal, while the game's own
     * {@code custom} click — which carries server-supplied data rather than a runnable — is not
     * recognised and falls through to the constant name.
     * example:
     * <pre>
     * const text = Chat.createTextHelperFromString("click me");
     * text.visit(JavaWrapper.methodToJava(function (style, run) {
     *   const action = style.getClickAction();
     *   if (action === null) {
     *     return;
     *   }
     *   if (action === "custom") {
     *     // a command a script registered, and the runnable is what runs it
     *     const runnable = style.getCustomClickValue();
     *     if (runnable !== null) {
     *       runnable.run();
     *     }
     *   } else {
     *     Chat.log(`clicking ${run} would ${action} ${style.getClickValue()}`);
     *   }
     * }));
     * </pre>
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
     * the argument the click action needs, as text, or {@code null}.
     * <p>
     * Which action this is depends on what {@link #getClickAction()} said: a url, a file path, a
     * command to run, a command to suggest, a page number as a decimal string, or the text to
     * copy. A run with no click action gives {@code null}, and so does the {@code "custom"}
     * action, whose value is a runnable rather than text and comes from
     * {@link #getCustomClickValue()} instead, and so does {@code SHOW_DIALOG}, which this does
     * not read a value for at all.
     * example:
     * <pre>
     * const text = Chat.createTextHelperFromString("a clickable run");
     * text.visit(JavaWrapper.methodToJava(function (style, run) {
     *   if (style.getClickAction() !== null) {
     *     const value = style.getClickValue();
     *     if (value !== null) {
     *       Chat.log(`${run} would act on ${value}`);
     *     }
     *   }
     * }));
     * </pre>
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
     * the runnable behind a JsMacros-registered click, or {@code null}.
     * <p>
     * This is the one click whose value is code rather than text: a command a script registered
     * is stored as a runnable, and this hands it back so the script that put the text together
     * can recognise its own click and act on it. It is {@code null} for every other kind of
     * click and for a run with none, and the way to tell the case is
     * {@link #getClickAction()} returning {@code "custom"}.
     * example:
     * <pre>
     * const text = Chat.createTextHelperFromString("a script command");
     * text.visit(JavaWrapper.methodToJava(function (style, run) {
     *   if (style.getClickAction() === "custom") {
     *     const runnable = style.getCustomClickValue();
     *     if (runnable !== null) {
     *       // this is the command a script registered, so running it is safe
     *       runnable.run();
     *     }
     *   }
     * }));
     * </pre>
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
     * what hovering this run does, or {@code null} if it does nothing.
     * <p>
     * The name of the hover action, and the content of the hover comes from
     * {@link #getHoverValue()}. The three names are the game's own: {@code show_text},
     * {@code show_item} and {@code show_entity}.
     * <p>
     * <b>This is not the same string shape as {@link #getClickAction()},</b> and comparing the
     * two is the trap. That one is the enum constant name and so is upper case, where this is
     * the lowercase id the game puts on the wire; {@code SHOW_ITEM} against {@code show_item}
     * is the whole difference. {@link #getClickAction()} spells out its own eight constants and
     * why, and it has one case change of its own that this does not: a JsMacros-registered
     * command reports as the literal {@code "custom"}.
     * example:
     * <pre>
     * const text = Chat.createTextHelperFromString("hover me");
     * text.visit(JavaWrapper.methodToJava(function (style, run) {
     *   const action = style.getHoverAction();
     *   if (action !== null) {
     *     Chat.log(`hovering ${run} would ${action}`);
     *   }
     * }));
     * </pre>
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
     * what this run's hover shows, or {@code null}.
     * <p>
     * The type of the answer depends on what {@link #getHoverAction()} said, and there are
     * three shapes: a {@code show_text} hover gives a {@link TextHelper} for the text shown;
     * a {@code show_item} hover gives an {@link ItemStackHelper} for the item shown; and a
     * {@code show_entity} hover gives a <i>list</i> of {@link TextHelper}, one per line of the
     * entity tooltip. A run with no hover gives {@code null}, as does any hover action the game
     * does not recognise.
     * <p>
     * Because the three shapes do not share a type, the answer is untyped and a script that
     * wants to use it should switch on the action first. Only the item shape differs across
     * game versions in what it wraps, so a script reading it should go through the helper rather
     * than the raw object.
     * example:
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
     * or {@code null} if there is no hover action
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
     * the text this run inserts when shift-clicked, or {@code null}.
     * <p>
     * An insertion is what a chat box puts on the input line when a shift-clicked run is
     * clicked, and most runs have none. A run with no insertion gives {@code null}.
     * example:
     * <pre>
     * const text = Chat.createTextHelperFromString("a shift-clickable run");
     * text.visit(JavaWrapper.methodToJava(function (style, run) {
     *   const insertion = style.getInsertion();
     *   if (insertion !== null) {
     *     Chat.log(`shift-clicking ${run} would type ${insertion}`);
     *   }
     * }));
     * </pre>
     *
     * @return the insertion text, or {@code null} if this run has none
     * @since 1.6.5
     */
    public String getInsertion() {
        return base.getInsertion();
    }

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
