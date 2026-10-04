package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.world.BossEvent;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.FormattingHelper;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.Locale;

/**
 * one boss bar as the client is drawing it right now, and a wrapper is a snapshot of
 * the bar rather than a handle on the live event it came from. Nothing on this class
 * writes to the bar, so it is for reading one.
 * <p>
 * A script gets one from {@code World.getBossBars()}, which is keyed by the same
 * string {@link #getUUID()} hands back, or from the {@code bossBar} field on the boss
 * bar event, which is {@code null} on the remove case.
 * <p>
 * The percentage is the smoothed one the overlay animates towards rather than the
 * value the server last sent: the bar eases from the previous percentage to the new
 * one over about a tenth of a second, so two reads a moment apart can differ even
 * though the server has sent nothing in between.
 * example:
 * <pre>
 * const bars = World.getBossBars();
 * Chat.log(`${bars.size()} boss bar(s) on screen`);
 * for (const entry of bars.entrySet()) {
 *   const bar = entry.getValue();
 *   // the percentage is a fraction from 0 to 1, not a number from 0 to 100
 *   const percent = Math.round(bar.getPercent() * 100);
 *   Chat.log(`${bar.getName().getString()} at ${percent}% (${bar.getColor()}, ${bar.getStyle()})`);
 * }
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.2.1
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class BossBarHelper extends BaseHelper<BossEvent> {

    public BossBarHelper(BossEvent b) {
        super(b);
    }

    /**
     * the id the bar was created with, as a string. This is the same string the key in
     * the map from {@code World.getBossBars()} is, so a bar can be looked back up with
     * it.
     * example:
     * <pre>
     * for (const entry of World.getBossBars().entrySet()) {
     *   const bar = entry.getValue();
     *   // the key and the id are the same string
     *   Chat.log(`${entry.getKey()} == ${bar.getUUID()}`);
     * }
     * </pre>
     *
     * @return boss bar uuid.
     * @since 1.2.1
     */
    public String getUUID() {
        return base.getId().toString();
    }

    /**
     * a fraction from 0 to 1 rather than a number from 0 to 100, so it has to be
     * multiplied to be shown as a percentage. A full bar is 1, because the bar starts
     * out full and fills up as the fight goes on. It is read at the moment of the call
     * and the bar keeps moving afterwards, so a bar read later can be a different
     * number.
     * example:
     * <pre>
     * const bars = World.getBossBars();
     * for (const bar of bars.values()) {
     *   const done = bar.getPercent() * 100;
     *   if (done > 50) {
     *     Chat.log(`${bar.getName().getString()} is past halfway, ${Math.round(done)}%`);
     *   }
     * }
     * </pre>
     *
     * @return percent of boss bar remaining.
     * @since 1.2.1
     */
    public float getPercent() {
        return base.getProgress();
    }

    /**
     * the name the bar is given by the {@link BossEvent.BossBarColor} enum, uppercased,
     * so one of {@code PINK}, {@code BLUE}, {@code RED}, {@code GREEN}, {@code YELLOW},
     * {@code PURPLE} or {@code WHITE}. These are the seven the game defines, and the
     * string is what the TypeScript typings narrow this to.
     * <p>
     * This is the enum's own name, not the formatting code behind it: the red bar
     * reports {@code RED} and the dark red one {@code RED} as well, so
     * {@link #getColorFormat()} is what tells those two apart. The rgb value is
     * available as {@link #getColorValue()}.
     * example:
     * <pre>
     * for (const bar of World.getBossBars().values()) {
     *   // the same name, the rgb behind it
     *   Chat.log(`${bar.getColor()} is 0x${bar.getColorValue().toString(16)}`);
     * }
     * </pre>
     *
     * @return boss bar color.
     * @since 1.2.1
     */
    @DocletReplaceReturn("BossBarColor")
    public String getColor() {
        return base.getColor().getName().toUpperCase(Locale.ROOT);
    }

    /**
     * the name the bar is given by the {@link BossEvent.BossBarOverlay} enum, uppercased,
     * so one of {@code PROGRESS}, {@code NOTCHED_6}, {@code NOTCHED_10},
     * {@code NOTCHED_12} or {@code NOTCHED_20}. The notched ones are the same bar with
     * the notches added, and the number is how many segments it is cut into.
     * <p>
     * The underscore in the notched names comes from the game and is kept, so a name
     * with a dash rather than an underscore is not one this can return.
     * example:
     * <pre>
     * for (const bar of World.getBossBars().values()) {
     *   const style = bar.getStyle();
     *   if (style.startsWith("NOTCHED_")) {
     *     const notches = Number(style.substring(8));
     *     Chat.log(`${bar.getName().getString()} is drawn in ${notches} segments`);
     *   }
     * }
     * </pre>
     *
     * @return boss bar notch style.
     * @since 1.2.1
     */
    @DocletReplaceReturn("BossBarStyle")
    public String getStyle() {
        return base.getOverlay().getName().toUpperCase(Locale.ROOT);
    }

    /**
     * the packed rgb number of the chat formatting the bar's colour is drawn with,
     * which is the form a colour has to be in to go into a drawing call. The one colour
     * a boss bar cannot have is a format with no rgb behind it, and that gives
     * {@code -1}.
     * <p>
     * The bar's own {@link #getColor()} name is not enough to rebuild the colour from,
     * because two of the enum's entries share a name. The formatting behind the name
     * is available as {@link #getColorFormat()}, and this is that formatting's rgb.
     * example:
     * <pre>
     * for (const bar of World.getBossBars().values()) {
     *   // the packed rgb, printed the way a colour constant is usually written
     *   Chat.log(`this bar is 0x${bar.getColorValue().toString(16)}`);
     * }
     * </pre>
     *
     * @return the color of this boss bar.
     * @since 1.8.4
     */
    public int getColorValue() {
        ChatFormatting f = base.getColor().getFormatting();
        return f.getColor() == null ? -1 : f.getColor();
    }

    /**
     * the chat formatting behind the bar's colour, which is the thing that carries both
     * the rgb in {@link #getColorValue()} and the formatting code, a paragraph sign
     * followed by a character. It is a formatting rather than a colour on its own, so it
     * can be turned into text as well as being read for a number.
     * <p>
     * The formatting is the game's own, so there is no setter here: this is for reading
     * a bar that has already been given its colour.
     * example:
     * <pre>
     * for (const bar of World.getBossBars().values()) {
     *   const format = bar.getColorFormat();
     *   // the name is the formatting's own, the index is the legacy colour code
     *   // and the value is the packed rgb
     *   Chat.log(`${bar.getColor()}: ${format.getName()} ${format.getColorIndex()} ${format.getColorValue()}`);
     * }
     * </pre>
     *
     * @return the format of the boss bar's color.
     * @since 1.8.4
     */
    public FormattingHelper getColorFormat() {
        return new FormattingHelper(base.getColor().getFormatting());
    }

    /**
     * the title the bar is drawn with, as text rather than a plain string, so the
     * colouring and any styling the server sent come with it. This is a wrapper rather
     * than the raw component, and it is a read: there is no setter, so a bar's name
     * only changes when the server sends a new one.
     * example:
     * <pre>
     * for (const bar of World.getBossBars().values()) {
     *   // getString() strips the styling, getName() keeps it
     *   Chat.log(`plain: ${bar.getName().getString()}`);
     *   Chat.log(`styled: ${bar.getName()}`);
     * }
     * </pre>
     *
     * @return name of boss bar
     * @since 1.2.1
     */
    public TextHelper getName() {
        return TextHelper.wrap(base.getName());
    }

    @Override
    public String toString() {
        return String.format("BossBarHelper:{\"name:\": \"%s\", \"percent\": %f}", base.getName().getString(), base.getProgress());
    }

}
