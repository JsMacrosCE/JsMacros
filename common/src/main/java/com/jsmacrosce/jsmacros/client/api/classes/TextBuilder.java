package com.jsmacrosce.jsmacros.client.api.classes;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
//? if >=26.1 {
/*import net.minecraft.world.item.ItemStackTemplate;
*///?}
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.jsmacros.access.CustomClickEvent;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.FormattingHelper;
import com.jsmacrosce.jsmacros.client.api.helper.StyleHelper;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

import java.net.URI;
import java.util.*;

/**
 * usage: {@code builder.append("hello,").withColor(0xc).append(" World!").withColor(0x6)}
 * <p>
 * The builder is a run of text with a style on the last thing appended to it, so the pattern is
 * always the same: append some text, style it, append some more, style that. Every {@code with}
 * method here changes only the section that was appended last, and appending moves on to a new
 * one, so a style set before the first append has nothing to attach to. The chain ends with
 * {@link #build()}, which is what the display calls take.
 * <p>
 * This is reached through {@code Chat.createTextBuilder()}, which is the only way to get one.
 * example:
 * <pre>
 * // a line with two colours in it: the styling applies to what comes
 * // after it, so the second append needs its own withColor
 * const text = Chat.createTextBuilder()
 *   .append("hello, ").withColor(0xc).append(" World!").withColor(0x6)
 *   .build();
 * Chat.actionbar(text);
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.3.0
 */
@SuppressWarnings("unused")
public class TextBuilder {
    private final MutableComponent head = Component.literal("");
    private MutableComponent self = head;

    /**
     * makes an empty builder.
     * <p>
     * A new builder holds one empty literal, so the first {@link #append(Object)} is what gives it
     * text, and a style applied before that has nothing to attach to. A builder is normally
     * obtained from {@code Chat.createTextBuilder()} rather than constructed here.
     * example:
     * <pre>
     * // an empty builder measures as nothing, since it has no text yet
     * const builder = Chat.createTextBuilder();
     * Chat.log(`width before anything is appended: ${builder.getWidth()}`);
     * builder.append("now it has some").withFormatting(false, true, false, false, false);
     * Chat.log(`width after: ${builder.getWidth()}`);
     * </pre>
     *
     * @since 1.3.0
     */
    public TextBuilder() {

    }

    /**
     * move on to next section and set it's text.
     * <p>
     * Whatever kind of text it is, this also moves the builder on: the styling methods here change
     * whatever was appended last, and this is what makes that be this text. A plain object that is
     * neither a {@link TextHelper} nor a {@link TextBuilder} is turned into text with its
     * {@code toString}, so a number appends as its digits rather than failing.
     * <p>
     * A {@link TextHelper} and a {@link TextBuilder} are both added by reference rather than
     * copied, and the nested builder is not finished by being appended. What a nested builder
     * contributes is its own live root component, so text appended to it afterwards and styling
     * applied to it afterwards both turn up in the outer builder, and in anything the outer
     * builder has already built and handed over. Nesting therefore takes no snapshot at all: a
     * nested builder that is still being appended to is a live part of the outer one rather than a
     * frozen section of it, and a parent built before the nested builder finished still changes
     * afterwards.
     * example:
     * <pre>
     * // three sections in one line, each with its own colour
     * const text = Chat.createTextBuilder()
     *   .append("first ")
     *   .withColor(0x6)
     *   .append("second ")
     *   .withColor(0xc)
     *   .append("third")
     *   .build();
     * Chat.actionbar(text);
     * </pre>
     *
     * @param text a {@link String}, {@link TextHelper} or {@link TextBuilder}
     * @return this builder, for chaining
     * @throws NullPointerException if {@code text} is {@code null}, since its {@code toString} is
     *         then asked for
     * @throws ClassCastException if {@code text} is a {@link TextHelper} whose raw component is
     *         not a mutable one. There is an assertion for that in the code, so it is only a
     *         failure when assertions are enabled, and it is not checked otherwise
     * @since 1.3.0
     */
    public TextBuilder append(Object text) {
        if (text instanceof TextHelper) {
            appendInternal((TextHelper) text);
        } else if (text instanceof TextBuilder) {
            appendInternal(((TextBuilder) text).build());
        } else {
            appendInternal(text.toString());
        }
        return this;
    }

    private void appendInternal(String text) {
        head.append(self = Component.literal(text));
    }

    private void appendInternal(TextHelper helper) {
        assert helper.getRaw() instanceof MutableComponent;
        head.append(self = (MutableComponent) helper.getRaw());
    }

    /**
     * set current section's color by color code as hex, like {@code 0x6} for gold
     * and {@code 0xc} for red.
     * <p>
     * This is the game's own sixteen colour codes rather than an rgb value, and the argument is
     * the numeric value of the code character: {@code 0x6} is the character {@code 6} and
     * {@code 0xc} is the character {@code c}. The five decoration codes are here too, so
     * {@code 0x9} is bold and {@code 0xe} is italic, which is a different thing from the boolean
     * form of {@link #withFormatting(boolean, boolean, boolean, boolean, boolean)}.
     * <p>
     * A code that is not one of the game's clears the colour rather than failing. A negative
     * number is the game's own reset, so it removes the colour from the section.
     * example:
     * <pre>
     * // gold then red, which are the codes 6 and c. A code that is not
     * // one of the game's would clear the colour rather than raise
     * const text = Chat.createTextBuilder()
     *   .append("gold ").withColor(0x6)
     *   .append("red").withColor(0xc)
     *   .build();
     * Chat.actionbar(text);
     * </pre>
     *
     * @param color the numeric value of a chat colour code, {@code 0} to {@code 0xf} for the
     *              sixteen colours and the codes of the five decorations
     * @return this builder, for chaining
     * @since 1.3.0
     */
    public TextBuilder withColor(int color) {
        self.withStyle(style -> style.withColor(ChatFormatting.getById(color)));
        return this;
    }

    /**
     * Add text with custom colors.
     * <p>
     * This is a true rgb value rather than one of the sixteen codes, so it is the form to reach
     * for when the colour is not one the game has a code for. Each channel is masked to a single
     * byte, so a value outside {@code 0-255} is wrapped into range rather than refused, and the
     * colour is opaque: there is no alpha to set here.
     * <p>
     * A colour set this way replaces one set by {@link #withColor(int)} on the same section
     * rather than adding to it, since the section has one colour.
     * example:
     * <pre>
     * // an rgb colour, which is the form for a shade the game's own
     * // sixteen codes do not cover
     * const text = Chat.createTextBuilder()
     *   .append("custom ")
     *   .withColor(255, 85, 85)
     *   .append("colour")
     *   .build();
     * Chat.actionbar(text);
     * </pre>
     *
     * @param r red {@code 0-255}
     * @param g green {@code 0-255}
     * @param b blue {@code 0-255}
     * @return this builder, for chaining
     * @since 1.3.1
     */
    public TextBuilder withColor(int r, int g, int b) {
        self.withStyle(style -> style.withColor(TextColor.fromRgb((r & 255) << 16 | (g & 255) << 8 | (b & 255))));
        return this;
    }

    /**
     * set other formatting options for the current section
     * <p>
     * The five flags are the game's five decorations and they are independent, so any combination
     * of them can be set at once. The order of the parameters is underline, bold, italic,
     * strikethrough and the obscuring one, which is not the order the chat codes are usually
     * written in, so a positional call is worth reading twice.
     * <p>
     * This is a separate call from {@link #withColor(int)} rather than part of it, so a section
     * can have both a colour and decorations. Setting decorations on a section that already has
     * some replaces them rather than adding to them, since the same call also drops the ones set
     * to false.
     * example:
     * <pre>
     * // underline, bold, italic, strikethrough and obscured, in that order.
     * // Only the first two are asked for here
     * const text = Chat.createTextBuilder()
     *   .append("underlined and bold")
     *   .withColor(0xe)
     *   .withFormatting(true, true, false, false, false)
     *   .build();
     * Chat.actionbar(text);
     * </pre>
     *
     * @param underline whether to underline the current section
     * @param bold whether to embolden the current section
     * @param italic whether to italicise the current section
     * @param strikethrough whether to strike the current section through
     * @param magic whether to obscure the current section
     * @return this builder, for chaining
     * @since 1.3.0
     */
    public TextBuilder withFormatting(boolean underline, boolean bold, boolean italic, boolean strikethrough, boolean magic) {
        List<ChatFormatting> formattings = new LinkedList<>();
        if (underline) {
            formattings.add(ChatFormatting.UNDERLINE);
        }
        if (bold) {
            formattings.add(ChatFormatting.BOLD);
        }
        if (italic) {
            formattings.add(ChatFormatting.ITALIC);
        }
        if (strikethrough) {
            formattings.add(ChatFormatting.STRIKETHROUGH);
        }
        if (magic) {
            formattings.add(ChatFormatting.OBFUSCATED);
        }
        self.withStyle(style -> style.applyFormats(formattings.toArray(new ChatFormatting[0])));
        return this;
    }

    /**
     * applies the given formattings to the current section.
     * <p>
     * This is the same set of five decorations the boolean form sets, taken as helpers rather than
     * as flags, so it is the form to use when the formattings came from somewhere else rather than
     * being chosen here. As many may be given and a section may take more than one. A section that
     * already had decorations has them replaced, not added to, exactly as the boolean form does.
     * example:
     * <pre class="language-typescript">
     * // a text helper can hand over the formattings its own style carries,
     * // which is how a decoration is copied from one piece of text to
     * // another rather than chosen
     * const bold = Chat.createTextBuilder()
     *   .append("x")
     *   .withFormatting(false, true, false, false, false)
     *   .build();
     * const builder = Chat.createTextBuilder()
     *   .append("copied ");
     * bold.visit(JavaWrapper.methodToJava(function (style: StyleHelper, part) {
     *   const f = style.getFormatting();
     *   if (f !== null) {
     *     builder.withFormatting(f);
     *   }
     *   return part;
     * }));
     * Chat.actionbar(builder.build());
     * </pre>
     *
     * @param formattings the formattings to apply
     * @return self for chaining.
     * @since 1.8.4
     */
    public TextBuilder withFormatting(FormattingHelper... formattings) {
        self.withStyle(style -> style.applyFormats(Arrays.stream(formattings).map(FormattingHelper::getRaw).toArray(ChatFormatting[]::new)));
        return this;
    }

    /**
     * set current section's hover event to show text
     * <p>
     * This is the hover the game shows when the cursor is over the section, and the text it shows
     * is a {@link TextHelper} rather than a plain string, so it can be styled in turn. The hover
     * belongs to the section that was appended last and it replaces any hover that section already
     * had.
     * example:
     * <pre>
     * // a section that shows a second, styled line when hovered
     * const tip = Chat.createTextBuilder()
     *   .append("this is the tooltip")
     *   .withColor(0x6)
     *   .build();
     * const text = Chat.createTextBuilder()
     *   .append("hover me")
     *   .withShowTextHover(tip)
     *   .build();
     * Chat.actionbar(text);
     * </pre>
     *
     * @param text the text to show on hover, which may itself be styled
     * @return this builder, for chaining
     * @since 1.3.0
     */
    public TextBuilder withShowTextHover(TextHelper text) {
        self.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(text.getRaw())));
        return this;
    }

    /**
     * set current section's hover event to show an item
     * <p>
     * This is the hover that shows an item's own tooltip, name, enchants and all, so what appears
     * is whatever that item would show on its own rather than text of the script's choosing. The
     * stack is taken as it is, so its count, damage and custom name are all part of what is shown.
     * example:
     * <pre>
     * // a section whose hover is an item's own tooltip. A creative stack
     * // from the registry has no damage or count of its own
     * const diamond = Client.getRegistryManager().getItemStack("minecraft:diamond");
     * const text = Chat.createTextBuilder()
     *   .append("a diamond")
     *   .withShowItemHover(diamond)
     *   .build();
     * Chat.actionbar(text);
     * </pre>
     *
     * @param item the item to show on hover
     * @return this builder, for chaining
     * @since 1.3.0
     */
    public TextBuilder withShowItemHover(ItemStackHelper item) {
        //? if >=26.1 {
        /*self.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowItem(ItemStackTemplate.fromNonEmptyStack(item.getRaw()))));
        *///?} else {
        self.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowItem(item.getRaw())));
        //?}
        return this;
    }

    /**
     * set current section's hover event to show an entity
     * <p>
     * The hover shows the entity's name and type, and it is built from the entity at the moment
     * this is called rather than looked up later, so an entity that has gone away by the time the
     * text is shown still has the name it had.
     * example:
     * <pre>
     * // a section that names an entity on hover. The entity is taken now,
     * // so the hover does not depend on it still being there later
     * const near = World.getEntities(10);
     * if (near !== null) {
     *   const first = near.get(0);
     *   const text = Chat.createTextBuilder()
     *     .append(first.getName())
     *     .withShowEntityHover(first)
     *     .build();
     *   Chat.actionbar(text);
     * }
     * </pre>
     *
     * @param entity the entity to show on hover
     * @return this builder, for chaining
     * @since 1.3.0
     */
    public TextBuilder withShowEntityHover(EntityHelper<Entity> entity) {
        Entity raw = entity.getRaw();
        self.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowEntity(new HoverEvent.EntityTooltipInfo(raw.getType(), raw.getUUID(), raw.getName()))));
        return this;
    }

    /**
     * custom click event.
     * <p>
     * This is the click action that runs a script function rather than one of the game's own, so
     * clicking the text calls back into the script. The function takes nothing and its return
     * value is not used, and it is handed over as a java wrapper because that is what the click
     * has to be able to call.
     * <p>
     * Anything the function throws is caught and written to the script log rather than reaching
     * the game's error handling, so a click that fails is quiet in chat and shows up in the log.
     * example:
     * <pre>
     * // a section that runs a script function when it is clicked
     * const text = Chat.createTextBuilder()
     *   .append("click me")
     *   .withCustomClickEvent(JavaWrapper.methodToJava(function () {
     *     Chat.log("clicked");
     *   }))
     *   .build();
     * Chat.actionbar(text);
     * </pre>
     *
     * @param action the function to run on click, which takes nothing
     * @return this builder, for chaining
     * @since 1.3.0
     */
    public TextBuilder withCustomClickEvent(MethodWrapper<Object, Object, Object, ?> action) {
        self.withStyle(style -> style.withClickEvent(new CustomClickEvent(() -> {
            try {
                action.run();
            } catch (Throwable ex) {
                JsMacrosClient.clientCore.profile.logError(ex);
            }
        })));

        return this;
    }

    /**
     * normal click events like: {@code open_url}, {@code open_file}, {@code run_command}, {@code suggest_command}, {@code change_page}, and {@code copy_to_clipboard}
     * <p>
     * The action is named in lower case here whatever case it is written in, because it is upper
     * cased before being looked up, and a name the game does not know is refused at that lookup
     * rather than ignored. The value means something different for each: a url for
     * {@code open_url}, a path for {@code open_file}, a command line for {@code run_command}, a
     * command line to put in the chat box for {@code suggest_command}, a page number for
     * {@code change_page} and a piece of text for {@code copy_to_clipboard}.
     * <p>
     * A dialog can be named instead, and then the value is looked up in the dialog registry rather
     * than used directly, so a dialog the game does not know is refused there.
     * example:
     * <pre>
     * // a section that opens a url when clicked. open_url takes the url
     * // itself as the value, and the action name is case insensitive
     * const text = Chat.createTextBuilder()
     *   .append("the docs")
     *   .withClickEvent("open_url", "https://jsmacrosce.github.io/")
     *   .build();
     * Chat.actionbar(text);
     * </pre>
     *
     * @param action the click action, named in any case: {@code open_url}, {@code open_file},
     *               {@code run_command}, {@code suggest_command}, {@code change_page},
     *               {@code copy_to_clipboard}, {@code show_dialog} or {@code custom}
     * @param value the argument for that action, whose meaning is decided by which one it is
     * @return this builder, for chaining
     * @throws IllegalArgumentException if {@code action} is not one the game knows, or, for
     *         {@code open_url}, if the value is not a url the game can parse
     * @throws NumberFormatException if {@code action} is {@code change_page} and the value is not
     *         a number
     * @throws java.util.NoSuchElementException if {@code action} is {@code show_dialog} and no
     *         dialog is registered under that name
     * @throws NullPointerException if there is no connection, since the dialog lookup needs the
     *         client's registry access
     * @since 1.3.0
     */
    @DocletReplaceParams("action: TextClickAction, value: string")
    public TextBuilder withClickEvent(String action, String value) {
        ClickEvent.Action clickAction = ClickEvent.Action.valueOf(action.toUpperCase(Locale.ROOT));
        HolderLookup.Provider lookup = Objects
                .requireNonNull(Minecraft.getInstance().getConnection())
                .registryAccess();
        self.withStyle(style -> style.withClickEvent(switch (clickAction) {
            case OPEN_URL -> new ClickEvent.OpenUrl(URI.create(value));
            case OPEN_FILE -> new ClickEvent.OpenFile(value);
            case RUN_COMMAND -> new ClickEvent.RunCommand(value);
            case SUGGEST_COMMAND -> new ClickEvent.SuggestCommand(value);
            case CHANGE_PAGE -> new ClickEvent.ChangePage(Integer.parseInt(value));
            case COPY_TO_CLIPBOARD -> new ClickEvent.CopyToClipboard(value);
            //? if >1.21.5 {
            case SHOW_DIALOG -> {
                var registryWrapper = lookup.lookupOrThrow(Registries.DIALOG);
                var dialogKey = ResourceKey.create(Registries.DIALOG, ResourceLocation.parse(value));
                var entry = registryWrapper.get(dialogKey).orElseThrow(() -> new IllegalArgumentException("Unknown dialog type: " + value));
                yield new ClickEvent.ShowDialog(entry);
            }
            case CUSTOM -> new ClickEvent.Custom(ResourceLocation.parse(value),null);
            //?}
        }));
        return this;
    }

    /**
     * replaces the current section's whole style with the given one.
     * <p>
     * This is the one styling method here that replaces rather than adds, so a section styled by
     * this loses the colour and the hovers it already had, and the style given is used as it
     * stands. Every other {@code with} method changes one thing and leaves the rest of the
     * section's style alone, which is usually what a script wants and why this one is worth
     * knowing about. A style is not a colour or a formatting on its own but all of them together,
     * so the way to keep part of a section's styling is to read it, change what is needed and
     * write it back rather than to call this after the other methods.
     * example:
     * <pre class="language-typescript">
     * // this replaces the whole style, so a colour set earlier on the
     * // same section is gone afterwards. The visitor below hands over the
     * // style of a section that has both a colour and a decoration
     * const source = Chat.createTextBuilder()
     *   .append("x")
     *   .withColor(0xc)
     *   .withFormatting(false, true, false, false, false)
     *   .build();
     * const text = Chat.createTextBuilder()
     *   .append("copied style");
     * source.visit(JavaWrapper.methodToJava(function (style: StyleHelper, part) {
     *   text.withStyle(style);
     *   return part;
     * }));
     * Chat.actionbar(text);
     * </pre>
     *
     * @param style the style to put on the current section, replacing whatever it had
     * @return this builder, for chaining
     * @since 1.8.4
     */
    public TextBuilder withStyle(StyleHelper style) {
        self.setStyle(style.getRaw());
        return this;
    }

    /**
     * Measures the text as it is at this moment, in pixels.
     * <p>
     * This is the game's own font measurement of everything appended so far, so the answer changes
     * as the chain grows and the last call is the one that matters. It needs the client's font,
     * which is not there before a world has been loaded, so measuring before then is a problem
     * rather than a number.
     * example:
     * <pre>
     * // the width grows as sections are appended, and the styling can
     * // change it too because a decoration is drawn as well as the text
     * const builder = Chat.createTextBuilder().append("hello");
     * const plain = builder.getWidth();
     * builder.withFormatting(false, true, false, false, false);
     * const bold = builder.getWidth();
     * Chat.log(`${plain} plain, ${bold} bold`);
     * </pre>
     *
     * @return the width of this text.
     * @since 1.8.4
     */
    public int getWidth() {
        return Minecraft.getInstance().font.width(head);
    }

    /**
     * Build to a {@link TextHelper}
     * <p>
     * This is what the display calls such as the action bar and the title take. The result wraps
     * the text built so far rather than a copy of it, so the builder is not finished by this: text
     * appended afterwards shows up in what was already returned, and styling changed afterwards
     * shows up too.
     * example:
     * <pre>
     * // what is returned is not a copy, so a later append reaches the
     * // text that was already handed over
     * const builder = Chat.createTextBuilder().append("before");
     * const text = builder.build();
     * builder.append(" and after");
     * Chat.actionbar(text);
     * </pre>
     *
     * @return a {@link TextHelper} over the text built so far
     * @since 1.3.0
     */
    public TextHelper build() {
        return TextHelper.wrap(head);
    }

}
