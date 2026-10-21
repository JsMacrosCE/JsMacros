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
 * Builds styled text by appending sections and applying styles to the latest section. Obtain it
 * with {@code Chat.createTextBuilder()} and pass {@link #build()} to text-display APIs.
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
     * Creates an empty text builder. Style calls before the first append affect the empty root
     * component, not a text section.
     * @since 1.3.0
     */
    public TextBuilder() {

    }

    /**
     * Appends a section and makes it the target of subsequent style calls. A {@link TextHelper}
     * contributes its underlying component by reference; a {@link TextBuilder} contributes its
     * built component, while other values are converted with {@code toString()}.
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
     * @param text text, a {@link TextHelper}, a {@link TextBuilder}, or another value converted with {@code toString()}
     * @return this builder, for chaining
     * @throws NullPointerException if {@code text} is {@code null}, since its {@code toString} is
     *         then asked for
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
     * Sets the current section's color using one of the game's sixteen color codes,
     * such as {@code 0x6} for gold or {@code 0xc} for red.
     * <p>
     * The argument is the numeric value of the code character, not an RGB value. Decoration
     * formats do not have color-code IDs and cannot be selected here; use
     * {@link #withFormatting(boolean, boolean, boolean, boolean, boolean)} for them.
     * <p>
     * An unmapped non-negative ID supplies no color; a negative ID selects the reset format.
     * <pre>
     * // gold then red, using the game's color IDs
     * const text = Chat.createTextBuilder()
     *   .append("gold ").withColor(0x6)
     *   .append("red").withColor(0xc)
     *   .build();
     * Chat.actionbar(text);
     * </pre>
     *
     * @param color the numeric value of a chat color code from {@code 0} to {@code 0xf}
     * @return this builder, for chaining
     * @since 1.3.0
     */
    public TextBuilder withColor(int color) {
        self.withStyle(style -> style.withColor(ChatFormatting.getById(color)));
        return this;
    }

    /**
     * Sets the current section's color from RGB channel values.
     * <p>
     * This is a true rgb value rather than one of the sixteen codes, so it is the form to reach
     * for when the colour is not one the game has a code for. Each channel is masked to a single
     * byte, so a value outside {@code 0-255} is wrapped into range rather than refused, and the
     * colour is opaque: there is no alpha to set here.
     * <p>
     * A colour set this way replaces one set by {@link #withColor(int)} on the same section
     * rather than adding to it, since the section has one colour.
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
     * Sets the current section's decorations.
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
     * Applies the supplied decorations to the current section, replacing its existing decorations.
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
     * Sets the current section's hover event to show text.
     * <p>
     * This is the hover the game shows when the cursor is over the section, and the text it shows
     * is a {@link TextHelper} rather than a plain string, so it can be styled in turn. The hover
     * belongs to the section that was appended last and it replaces any hover that section already
     * had.
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
     * Sets the current section's hover event to show an item's tooltip.
     * <p>
     * This is the hover that shows an item's own tooltip, name, enchants and all, so what appears
     * is whatever that item would show on its own rather than text of the script's choosing. The
     * stack is taken as it is, so its count, damage and custom name are all part of what is shown.
     * <pre>
     * // show an item's tooltip when this section is hovered
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
     * Sets the current section's hover event to show the entity's name and type, captured when
     * this method is called.
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
     * Runs a script function when the current section is clicked. The function takes no arguments;
     * thrown exceptions are caught and logged.
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
     * Sets a built-in click action on the current section. The action name is case-insensitive;
     * the value is interpreted according to the action (for example, a URL for {@code open_url}
     * or a page number for {@code change_page}). {@code show_dialog} resolves the value as a
     * registered dialog identifier.
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
     * Replaces the current section's entire style with the supplied style, including any existing
     * color, formatting, hover, or click event.
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
     * Returns the pixel width of the text currently built, measured with the game's font.
     *
     * @return the width of this text.
     * @since 1.8.4
     */
    public int getWidth() {
        return Minecraft.getInstance().font.width(head);
    }

    /**
     * Returns a {@link TextHelper} wrapping the builder's text component. The returned helper
     * reflects later appends and style changes to this builder.
     *
     * @return a {@link TextHelper} over the text built so far
     * @since 1.3.0
     */
    public TextHelper build() {
        return TextHelper.wrap(head);
    }

}
