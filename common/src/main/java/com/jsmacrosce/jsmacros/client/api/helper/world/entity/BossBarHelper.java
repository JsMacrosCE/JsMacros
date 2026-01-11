package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.world.BossEvent;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.FormattingHelper;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.Locale;

/**
 * @author Wagyourtail
 * @since 1.2.1
 */
@SuppressWarnings("unused")
public class BossBarHelper extends BaseHelper<BossEvent> {

    public BossBarHelper(BossEvent b) {
        super(b);
    }

    /**
     * @return boss bar uuid.
     * @since 1.2.1
     */
    public String getUUID() {
        return base.getId().toString();
    }

    /**
     * @return percent of boss bar remaining.
     * @since 1.2.1
     */
    public float getPercent() {
        return base.getProgress();
    }

    /**
     * @return boss bar color.
     * @since 1.2.1
     */
    @DocletReplaceReturn("BossBarColor")
    public String getColor() {
        return base.getColor().getName().toUpperCase(Locale.ROOT);
    }

    /**
     * @return boss bar notch style.
     * @since 1.2.1
     */
    @DocletReplaceReturn("BossBarStyle")
    public String getStyle() {
        return base.getOverlay().getName().toUpperCase(Locale.ROOT);
    }

    /**
     * @return the color of this boss bar.
     * @since 1.8.4
     */
    public int getColorValue() {
        ChatFormatting f = base.getColor().getFormatting();
        return f.getColor() == null ? -1 : f.getColor();
    }

    /**
     * @return the format of the boss bar's color.
     * @since 1.8.4
     */
    public FormattingHelper getColorFormat() {
        return new FormattingHelper(base.getColor().getFormatting());
    }

    /**
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
