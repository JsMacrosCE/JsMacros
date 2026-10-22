package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display;

import net.minecraft.world.entity.Display;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * a display entity that shows a piece of text floating in the world, the thing behind every
 * nametag and every hologram made with the {@code /data} command.
 * <p>
  * The text and the way it is drawn live on a separate render state rather than on the entity
 * itself, so {@link #getData() getData()} is how a script reaches any of it and is
 * {@code null} until the client has built that state at least once. The state is rebuilt
 * whenever the entity data changes, and while the display's transformation is interpolating,
 * the opacity and the background are sampled from between the old state and the new one, so
 * two calls a tick apart can differ even with nothing changed in between.
 * example:
 * <pre>
 * const TextDisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.TextDisplayEntityHelper");
 * const names = World.getEntities(32, "text_display");
 * if (names !== null) {
 *   for (const entity of names) {
 *     const display = TextDisplayEntityHelper.class.cast(entity);
 *     const data = display.getData();
 *     // the render state is built on the client, so it is null until it has been ticked once
 *     if (data === null) {
 *       continue;
 *     }
 *     Chat.log(`"${data.getText().getString()}" is ${display.getDisplayWidth()} by ${display.getDisplayHeight()}`);
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

    public TextDisplayEntityHelper(Display.TextDisplay base) {
        super(base);
    }

    /**
     * the text and the way it is drawn, as one object holding both.
     * <p>
     * This is the client's render state for the text rather than the entity data the server
     * sent, and it is {@code null} until the client has built it once, which is why this can
     * answer {@code null} for a text display that plainly exists. A non-{@code null} result
     * still changes over time: it is rebuilt when any of the text data updates, and while a
     * transformation interpolation is running the opacity and the background are the values
     * partway between the two states rather than either of them.
     * example:
     * <pre>
     * const TextDisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.TextDisplayEntityHelper");
     * const names = World.getEntities(32, "text_display");
     * if (names !== null) {
     *   for (const entity of names) {
     *     const display = TextDisplayEntityHelper.class.cast(entity);
     *     const data = display.getData();
     *     if (data === null) {
     *       continue;
     *     }
     *     // a shadow flag, and what the text says
     *     Chat.log(`${data.hasShadowFlag() ? "with" : "without"} a shadow: ${data.getText().getString()}`);
     *   }
     * }
     * </pre>
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
     * the contents of a text display and the flags and numbers that decide how it is drawn.
     * <p>
     * Nothing here can be changed; it is all a read of the render state
     * {@link TextDisplayEntityHelper#getData() getData()} hands out. The three flags and the
     * alignment are all bits of the same value, so they can all be set at once and none of them
     * excludes another.
     */
    public static class TextDisplayDataHelper extends BaseHelper<Display.TextDisplay.TextRenderState> {

        public TextDisplayDataHelper(Display.TextDisplay.TextRenderState base) {
            super(base);
        }

        /**
         * the text itself, as the game's own text object rather than a plain string.
         * <p>
         * A {@link TextHelper} keeps the colours, styles, hover text and click actions that a
         * string would lose, so this is the right one to read when the display was given a
         * formatted component. {@code getString()} on it gives the plain text back.
         * example:
         * <pre>
         * const TextDisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.TextDisplayEntityHelper");
         * const names = World.getEntities(32, "text_display");
         * if (names !== null) {
         *   for (const entity of names) {
         *     const display = TextDisplayEntityHelper.class.cast(entity);
         *     const data = display.getData();
         *     if (data === null) {
         *       continue;
         *     }
         *     // a TextHelper rather than a string, so the styling survives
         *     Chat.log(data.getText());
         *   }
         * }
         * </pre>
         *
         * @return the text of the display
         * @since 1.9.1
         */
        public TextHelper getText() {
            return TextHelper.wrap(base.text());
        }

        /**
     * how wide a line of the text is allowed to be before it wraps, in the game's own font
     * pixels, and {@code 200} is what a text display with no line width set reports.
     * <p>
     * This is the width the text is laid out against rather than the width of the display,
     * which is {@link TextDisplayEntityHelper#getDisplayWidth() getDisplayWidth()} and is
     * only what the client culls against. The text is measured with the game's own font, so
     * the number is in that font's pixels rather than in blocks, and a display with no text on
     * it still reports its line width whatever it is set to.
         * example:
         * <pre>
         * const TextDisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.TextDisplayEntityHelper");
         * const names = World.getEntities(32, "text_display");
         * if (names !== null) {
         *   for (const entity of names) {
         *     const display = TextDisplayEntityHelper.class.cast(entity);
         *     const data = display.getData();
         *     if (data === null) {
         *       continue;
         *     }
              *     // 200 font pixels is the default, and it is not a block measurement
     *     Chat.log(`wrapping at ${data.getLineWidth()} font pixels`);
         *   }
         * }
         * </pre>
         *
         * @return the line width the text is laid out at
         * @since 1.9.1
         */
        public int getLineWidth() {
            return base.lineWidth();
        }

        /**
     * how opaque the text is, where {@code 0} is invisible and {@code 255} is solid.
     * <p>
     * The value is a single byte, and it defaults to {@code -1} rather than to a number in
     * that range, so a text display with no opacity set reports {@code -1}. The renderer takes
     * the byte, shifts it into the top of the colour, and lets {@code -1} come out as
     * {@code 0xFF}, so the default draws as fully opaque. That is why the default is not
     * {@code 255}: {@code 255} and {@code -1} are two different stored values that both mean
     * solid, and a script comparing against {@code 255} will miss the default.
     * <p>
     * This is the text alone and not the box behind it, which is
     * {@link #getBackgroundColor() getBackgroundColor()} and is a separate number. While the
     * display's transformation is interpolating, the value here is partway between the old
     * opacity and the new one, so it can be a number that was never set.
         * example:
         * <pre>
         * const TextDisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.TextDisplayEntityHelper");
         * const names = World.getEntities(32, "text_display");
         * if (names !== null) {
         *   for (const entity of names) {
         *     const display = TextDisplayEntityHelper.class.cast(entity);
         *     const data = display.getData();
         *     if (data === null) {
         *       continue;
         *     }
              *     // the default is -1, which the renderer draws as fully opaque
     *     const opacity = data.getTextOpacity();
     *     if (opacity !== -1) {
     *       Chat.log(`text opacity set to ${opacity}`);
     *     }
         *   }
         * }
         * </pre>
              * @return the opacity of the text, from 0 to 255, or -1 for the default of fully opaque
         * @since 1.9.1
         */
        public int getTextOpacity() {
            return base.textOpacity().get(1.0f);
        }

        /**
         * the colour of the box drawn behind the text, as a packed ARGB integer, and
         * {@code 0x40000000} is what a text display with no background set reports.
         * <p>
              * A packed colour is {@code 0xAARRGGBB}, so the top byte is the alpha. The default
     * {@code 0x40000000} is a quarter-opaque black, which is why a text display with no
     * background of its own still has one. A value of exactly {@code 0} is the one that means
     * no background at all, because the renderer only draws the box when the colour is not
     * zero, so {@code 0x00000000} removes the box rather than drawing a black one.
     * <p>
     * The alpha of this box is separate from {@link #getTextOpacity() getTextOpacity()},
     * which fades the letters rather than the box, so the two can be faded independently.
         * example:
         * <pre>
         * const TextDisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.TextDisplayEntityHelper");
         * const names = World.getEntities(32, "text_display");
         * if (names !== null) {
         *   for (const entity of names) {
         *     const display = TextDisplayEntityHelper.class.cast(entity);
         *     const data = display.getData();
         *     if (data === null) {
         *       continue;
         *     }
         *     // the default is a quarter-opaque black: alpha 0x40 at the top
         *     Chat.log(`background 0x${data.getBackgroundColor().toString(16)}`);
         *   }
         * }
         * </pre>
         *
         * @return the packed ARGB colour of the background box
         * @since 1.9.1
         */
        public int getBackgroundColor() {
            return base.backgroundColor().get(1.0f);
        }

        /**
         * whether the text is drawn with a drop shadow behind the letters.
         * <p>
         * The first of three flags that all live in the same value, and this one is the bit
         * worth {@code 1}. It is the {@code shadow} flag from the entity data, and a text
         * display with no flags set reports {@code false} for it.
         * example:
         * <pre>
         * const TextDisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.TextDisplayEntityHelper");
         * const names = World.getEntities(32, "text_display");
         * if (names !== null) {
         *   for (const entity of names) {
         *     const display = TextDisplayEntityHelper.class.cast(entity);
         *     const data = display.getData();
         *     if (data === null) {
         *       continue;
         *     }
         *     if (data.hasShadowFlag()) {
         *       Chat.log(`shadowed: ${data.getText().getString()}`);
         *     }
         *   }
         * }
         * </pre>
         *
         * @return {@code true} if the text is drawn with a shadow, {@code false} otherwise
         * @since 1.9.1
         */
        public boolean hasShadowFlag() {
            return (base.flags() & 1) != 0;
        }

        /**
     * whether the text and its background are drawn see-through, so whatever is already on
     * screen shows through both.
     * <p>
     * The second of the three flags in the same value, and this one is the bit worth
     * {@code 2}. It is the {@code see_through} flag, and it applies to the letters as well as
     * to the box behind them: the renderer picks a see-through draw for both rather than
     * giving the text an offset against whatever is behind it. It is a separate setting from
     * {@link #hasShadowFlag() hasShadowFlag()}, so a display can have either, both or
     * neither.
         * example:
         * <pre>
         * const TextDisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.TextDisplayEntityHelper");
         * const names = World.getEntities(32, "text_display");
         * if (names !== null) {
         *   for (const entity of names) {
         *     const display = TextDisplayEntityHelper.class.cast(entity);
         *     const data = display.getData();
         *     if (data === null) {
         *       continue;
         *     }
         *     if (data.hasSeeThroughFlag()) {
         *       Chat.log(`see-through background behind "${data.getText().getString()}"`);
         *     }
         *   }
         * }
         * </pre>
         *
         * @return {@code true} if the background box is see-through, {@code false} otherwise
         * @since 1.9.1
         */
        public boolean hasSeeThroughFlag() {
            return (base.flags() & 2) != 0;
        }

        /**
         * whether the display falls back to the game's own default background rather than the
         * one it was given.
         * <p>
         * The third of the three flags in the same value, and this one is the bit worth
         * {@code 4}. It is the {@code default_background} flag, and it is about which
         * background is used rather than about the background being visible, so it can be set
         * at the same time as {@link #hasSeeThroughFlag() hasSeeThroughFlag()}.
         * example:
         * <pre>
         * const TextDisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.TextDisplayEntityHelper");
         * const names = World.getEntities(32, "text_display");
         * if (names !== null) {
         *   for (const entity of names) {
         *     const display = TextDisplayEntityHelper.class.cast(entity);
         *     const data = display.getData();
         *     if (data === null) {
         *       continue;
         *     }
         *     if (data.hasDefaultBackgroundFlag()) {
         *       Chat.log("using the game's own default background");
         *     }
         *   }
         * }
         * </pre>
         *
         * @return {@code true} if the default background is used, {@code false} otherwise
         * @since 1.9.1
         */
        public boolean hasDefaultBackgroundFlag() {
            return (base.flags() & 4) != 0;
        }

        /**
         * which side the text is lined up on within the display, and {@code "center"} is what a
         * text display with no alignment set reports.
         * <p>
         * This is the {@code alignment} the display was given, and it is set by two further bits
         * in the same value as the three flags, so a display with both of those bits set reads
         * as {@code "left"}: the left bit is checked first. It has nothing to do with
         * {@link TextDisplayEntityHelper#getLerpTargetYaw() getLerpTargetYaw()}, which is
         * which way the display itself is turned.
         * example:
         * <pre>
         * const TextDisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.TextDisplayEntityHelper");
         * const names = World.getEntities(32, "text_display");
         * if (names !== null) {
         *   for (const entity of names) {
         *     const display = TextDisplayEntityHelper.class.cast(entity);
         *     const data = display.getData();
         *     if (data === null) {
         *       continue;
         *     }
         *     Chat.log(`${data.getAlignment()}: ${data.getText().getString()}`);
         *   }
         * }
         * </pre>
         *
         * @return "center", "left" or "right"
         * @since 1.9.1
         */
        public String getAlignment() {
            return Display.TextDisplay.getAlign(base.flags()).getSerializedName();
        }

    }

}
