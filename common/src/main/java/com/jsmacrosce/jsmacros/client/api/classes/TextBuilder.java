package com.jsmacrosce.jsmacros.client.api.classes;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
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
 *
 * @author Wagyourtail
 * @since 1.3.0
 */
@SuppressWarnings("unused")
public class TextBuilder {
    private final MutableComponent head = Component.literal("");
    private MutableComponent self = head;

    public TextBuilder() {

    }

    /**
     * move on to next section and set it's text.
     *
     * @param text a {@link String}, {@link TextHelper} or {@link TextBuilder}
     * @return
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
     *
     * @param color
     * @return
     * @since 1.3.0
     */
    public TextBuilder withColor(int color) {
        self.withStyle(style -> style.withColor(ChatFormatting.getById(color)));
        return this;
    }

    /**
     * Add text with custom colors.
     *
     * @param r red {@code 0-255}
     * @param g green {@code 0-255}
     * @param b blue {@code 0-255}
     * @return
     * @since 1.3.1
     */
    public TextBuilder withColor(int r, int g, int b) {
        self.withStyle(style -> style.withColor(TextColor.fromRgb((r & 255) << 16 | (g & 255) << 8 | (b & 255))));
        return this;
    }

    /**
     * set other formatting options for the current section
     *
     * @param underline
     * @param bold
     * @param italic
     * @param strikethrough
     * @param magic
     * @return
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
     *
     * @param text
     * @return
     * @since 1.3.0
     */
    public TextBuilder withShowTextHover(TextHelper text) {
        self.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(text.getRaw())));
        return this;
    }

    /**
     * set current section's hover event to show an item
     *
     * @param item
     * @return
     * @since 1.3.0
     */
    public TextBuilder withShowItemHover(ItemStackHelper item) {
        self.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowItem(item.getRaw())));
        return this;
    }

    /**
     * set current section's hover event to show an entity
     *
     * @param entity
     * @return
     * @since 1.3.0
     */
    public TextBuilder withShowEntityHover(EntityHelper<Entity> entity) {
        Entity raw = entity.getRaw();
        self.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowEntity(new HoverEvent.EntityTooltipInfo(raw.getType(), raw.getUUID(), raw.getName()))));
        return this;
    }

    /**
     * custom click event.
     *
     * @param action
     * @return
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
     *
     * @param action
     * @param value
     * @return
     * @since 1.3.0
     */
    @DocletReplaceParams("action: TextClickAction, value: string")
    public TextBuilder withClickEvent(String action, String value) {
        ClickEvent.Action clickAction = ClickEvent.Action.valueOf(action.toUpperCase(Locale.ROOT));
        ResourceLocation id = ResourceLocation.parse(value);
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

    public TextBuilder withStyle(StyleHelper style) {
        self.setStyle(style.getRaw());
        return this;
    }

    /**
     * @return the width of this text.
     * @since 1.8.4
     */
    public int getWidth() {
        return Minecraft.getInstance().font.width(head);
    }

    /**
     * Build to a {@link TextHelper}
     *
     * @return
     * @since 1.3.0
     */
    public TextHelper build() {
        return TextHelper.wrap(head);
    }

}
