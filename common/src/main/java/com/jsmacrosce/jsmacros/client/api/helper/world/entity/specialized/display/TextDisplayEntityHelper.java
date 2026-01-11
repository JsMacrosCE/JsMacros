package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display;

import net.minecraft.world.entity.Display;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * @author aMelonRind
 * @since 1.9.1
 */
@SuppressWarnings("unused")
public class TextDisplayEntityHelper extends DisplayEntityHelper<Display.TextDisplay> {

    public TextDisplayEntityHelper(Display.TextDisplay base) {
        super(base);
    }

    /**
     * @since 1.9.1
     */
    @Nullable
    public TextDisplayDataHelper getData() {
        Display.TextDisplay.TextRenderState data = base.textRenderState();
        if (data == null) return null;
        return new TextDisplayDataHelper(data);
    }

    public static class TextDisplayDataHelper extends BaseHelper<Display.TextDisplay.TextRenderState> {

        public TextDisplayDataHelper(Display.TextDisplay.TextRenderState base) {
            super(base);
        }

        /**
         * @since 1.9.1
         */
        public TextHelper getText() {
            return TextHelper.wrap(base.text());
        }

        /**
         * @since 1.9.1
         */
        public int getLineWidth() {
            return base.lineWidth();
        }

        /**
         * @since 1.9.1
         */
        public int getTextOpacity() {
            return base.textOpacity().get(1.0f);
        }

        /**
         * @since 1.9.1
         */
        public int getBackgroundColor() {
            return base.backgroundColor().get(1.0f);
        }

        /**
         * @since 1.9.1
         */
        public boolean hasShadowFlag() {
            return (base.flags() & 1) != 0;
        }

        /**
         * @since 1.9.1
         */
        public boolean hasSeeThroughFlag() {
            return (base.flags() & 2) != 0;
        }

        /**
         * @since 1.9.1
         */
        public boolean hasDefaultBackgroundFlag() {
            return (base.flags() & 4) != 0;
        }

        /**
         * @return "center", "left" or "right"
         * @since 1.9.1
         */
        public String getAlignment() {
            return Display.TextDisplay.getAlign(base.flags()).getSerializedName();
        }

    }

}
