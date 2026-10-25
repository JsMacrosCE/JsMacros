package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display;

import net.minecraft.world.entity.Display;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * Exposes the client-built render state for a text display; {@link #getData()} returns {@code null}
 * until it exists.
 * <pre>
 * const names = World.getEntities(32, "text_display");
 * if (names !== null) {
 *   for (const entity of names) {
 *     if (!(entity instanceof EntityHelpers.TextDisplay)) continue;
 *     const data = entity.getData();
 *     // the state may not exist yet
 *     if (data === null) {
 *       continue;
 *     }
 *     Chat.log(`"${data.getText().getString()}" is ${entity.getDisplayWidth()} by ${entity.getDisplayHeight()}`);
 *   }
 * }
 * </pre>
 *
 * @author aMelonRind
 * @since 1.9.1
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class TextDisplayEntityHelper extends DisplayEntityHelper<Display.TextDisplay> {

    /** Wraps the supplied text display entity.
     * @param base text display entity to wrap
     */
    public TextDisplayEntityHelper(Display.TextDisplay base) {
        super(base);
    }

    /**
      * Returns the client's text render state, or {@code null} until it has been built.
     *
     * @return the render state for the text, or {@code null} if the client has not built it
     * @since 1.9.1
     */
    @Nullable
    public TextDisplayDataHelper getData() {
        Display.TextDisplay.TextRenderState data = base.textRenderState();
        if (data == null) return null;
        return new TextDisplayDataHelper(data);
    }

    /**
     * Read-only contents and drawing properties from a text display's render state.
     */
    public static class TextDisplayDataHelper extends BaseHelper<Display.TextDisplay.TextRenderState> {

        /** Wraps the supplied text render state.
         * @param base text render state to wrap
         */
        public TextDisplayDataHelper(Display.TextDisplay.TextRenderState base) {
            super(base);
        }

        /**
         * Returns the display's text as a {@link TextHelper}, preserving the component formatting.
         *
         * @return the text of the display
         * @since 1.9.1
         */
        public TextHelper getText() {
            return TextHelper.wrap(base.text());
        }

        /**
         * Returns the maximum line width before wrapping, in font pixels; the default is {@code 200}.
         *
         * @return the line width the text is laid out at
         * @since 1.9.1
         */
        public int getLineWidth() {
            return base.lineWidth();
        }

        /**
         * Returns the text opacity; {@code 0} is transparent, {@code 255} opaque, and {@code -1}
         * (the default) also renders opaque. This affects text, not the background.
         * @return the opacity from 0 to 255, or -1 for the opaque default
         * @since 1.9.1
         */
        public int getTextOpacity() {
            return base.textOpacity().get(1.0f);
        }

        /**
         * Returns the background box color as packed ARGB; the default is {@code 0x40000000}
         * (quarter-opaque black), and {@code 0} disables the box. Its alpha is independent of
         * {@link #getTextOpacity()}.
         *
         * @return the packed ARGB colour of the background box
         * @since 1.9.1
         */
        public int getBackgroundColor() {
            return base.backgroundColor().get(1.0f);
        }

        /**
         * Returns whether the text is drawn with a drop shadow.
         *
         * @return {@code true} if the text is drawn with a shadow, {@code false} otherwise
         * @since 1.9.1
         */
        public boolean hasShadowFlag() {
            return (base.flags() & 1) != 0;
        }

        /**
     * Returns whether text uses the see-through rendering mode.
         *
         * @return {@code true} if text and any background box use see-through rendering, {@code false} otherwise
         * @since 1.9.1
         */
        public boolean hasSeeThroughFlag() {
            return (base.flags() & 2) != 0;
        }

        /**
     * Returns whether the display uses the default background.
         *
         * @return {@code true} if the default background is used, {@code false} otherwise
         * @since 1.9.1
         */
        public boolean hasDefaultBackgroundFlag() {
            return (base.flags() & 4) != 0;
        }

        /**
         * Returns the text alignment: {@code "center"}, {@code "left"} or {@code "right"}.
         * If both alignment bits are set, {@code "left"} takes precedence.
         *
         * @return "center", "left" or "right"
         * @since 1.9.1
         */
        public String getAlignment() {
            return Display.TextDisplay.getAlign(base.flags()).getSerializedName();
        }

    }

}
