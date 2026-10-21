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
 * A wrapper exposing the state of one boss bar known to the client.
 * <p>
 * {@code World.getBossBars()} returns a map keyed by the string from {@link #getUUID()}.
 * The boss-bar event's {@code bossBar} field is {@code null} when a bar is removed.
 * <p>
 * The percentage is the smoothed one the overlay animates towards rather than the
 * value the server last sent: the bar eases from the previous percentage to the new
 * one over 100 milliseconds, so reads can differ even when the server has sent nothing
 * in between.
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

    /** Wraps the supplied boss-bar event.
     * @param b raw boss event to wrap
     */
    public BossBarHelper(BossEvent b) {
        super(b);
    }

    /**
     * Returns the bar's UUID as a string, matching its key in {@code World.getBossBars()}.
     *
     * @return this bar's UUID.
     * @since 1.2.1
     */
    public String getUUID() {
        return base.getId().toString();
    }

    /**
     * Returns the current smoothed progress as a fraction from {@code 0} to {@code 1}.
     * The client animates this value toward the latest progress received from the server.
     *
     * @return current progress fraction; values are not converted to percentages.
     * @since 1.2.1
     */
    public float getPercent() {
        return base.getProgress();
    }

    /**
     * Returns the {@link BossEvent.BossBarColor} enum name in uppercase,
     * so one of {@code PINK}, {@code BLUE}, {@code RED}, {@code GREEN}, {@code YELLOW},
     * {@code PURPLE} or {@code WHITE}. These are the seven the game defines, and the
     * string is what the TypeScript typings narrow this to.
     * This name does not uniquely identify the formatting: {@code PINK} and {@code RED}
     * both use red formatting. Use {@link #getColorFormat()} or {@link #getColorValue()}
     * for the formatting or its RGB value.
     *
     * @return the uppercase enum name.
     * @since 1.2.1
     */
    @DocletReplaceReturn("BossBarColor")
    public String getColor() {
        return base.getColor().getName().toUpperCase(Locale.ROOT);
    }

    /**
     * Returns the {@link BossEvent.BossBarOverlay} enum name in uppercase,
     * so one of {@code PROGRESS}, {@code NOTCHED_6}, {@code NOTCHED_10},
     * {@code NOTCHED_12} or {@code NOTCHED_20}. The notched ones are the same bar with
     * the notches added, and the number is how many segments it is cut into.
     *
     * @return the uppercase overlay enum name.
     * @since 1.2.1
     */
    @DocletReplaceReturn("BossBarStyle")
    public String getStyle() {
        return base.getOverlay().getName().toUpperCase(Locale.ROOT);
    }

    /**
     * Returns the packed RGB value of the bar's formatting, or {@code -1} if it has no RGB value.
     *
     * @return the formatting's RGB value, or {@code -1} if absent.
     * @since 1.8.4
     */
    public int getColorValue() {
        ChatFormatting f = base.getColor().getFormatting();
        return f.getColor() == null ? -1 : f.getColor();
    }

    /**
     * Returns the formatting associated with the bar's color.
     *
     * @return the bar color's formatting.
     * @since 1.8.4
     */
    public FormattingHelper getColorFormat() {
        return new FormattingHelper(base.getColor().getFormatting());
    }

    /**
     * Returns the bar's title as a {@link TextHelper}, preserving its text styling.
     *
     * @return the bar's styled title.
     * @since 1.2.1
     */
    public TextHelper getName() {
        return TextHelper.wrap(base.getName());
    }

    /** Returns a diagnostic string containing the bar's plain name and raw progress value.
     * @return a diagnostic representation of the bar
     */
    @Override
    public String toString() {
        return String.format("BossBarHelper:{\"name:\": \"%s\", \"percent\": %f}", base.getName().getString(), base.getProgress());
    }

}
