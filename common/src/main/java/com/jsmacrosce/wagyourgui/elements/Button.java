package com.jsmacrosce.wagyourgui.elements;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import com.jsmacrosce.jsmacros.util.ColorMath;

//? if >1.21.8 {
/*import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

import java.util.List;
import java.util.function.Consumer;

/** A native-widget button with a wrapped label, configurable packed ARGB colors, and an optional press callback.
 * The constructor does not add the button to a host; use an existing host for normal input and rendering.
 * <pre>
 * function makeButton(font, label, onPressed) {
 *   const Button = Java.type('com.jsmacrosce.wagyourgui.elements.Button');
 *   return new Button(10, 10, 120, 20, font, 0xFF000000, 0xFFFFFFFF,
 *     0xFF808080, 0xFFFFFFFF, label, JavaWrapper.methodToJava(onPressed));
 * }
 * </pre>
 * The factory takes a native font, component label, and JavaScript callback; add the result to your existing host separately.
 */
@DocletCategory("Screen and UI Elements")
public class Button extends AbstractButton {
    protected final Font textRenderer;
    protected int color;
    protected int borderColor;
    protected int highlightColor;
    protected int textColor;
    protected List<FormattedCharSequence> textLines;
    protected int visibleLines;
    protected int verticalCenter;
    /** Whether this button's cached label is horizontally centered, initially true; false uses a two-GUI-unit left inset.
     * A direct change affects the next label draw, without refreshing wrapping or invoking the press callback.
     */
    public boolean horizCenter = true;
    /** Mutable callback invoked with this button when its press handler dispatches; initially the constructor argument, or null to suppress the callback.
     * Assigning this field neither invokes it nor registers the widget.
     */
    public Consumer<Button> onPress;
    /** Hover result last recorded by a visible contents-render hook; initially false and not refreshed while hidden.
     * This button records mouse-inside-and-active or forced hover. A direct write is overwritten by the next visible hook and does not control activation.
     */
    public boolean hovering = false;
    /** Forces the hover fill and recorded hover state during a visible contents-render hook, independent of bounds or active state; initially false.
     * Does not make the widget visible or active, or invoke the press callback.
     */
    public boolean forceHover = false;

    /** Initializes native geometry and message, stores the font and optional callback, configures colors, then calls the overridable setMessage to prepare the label cache.
     * Does not invoke the callback or register this button with a host. Packed ARGB colors with zero alpha and nonzero RGB are made opaque; zero remains zero.
     * @param x left position in GUI units
     * @param y top position in GUI units
     * @param width width in GUI units
     * @param height height in GUI units
     * @param textRenderer non-null native font used to wrap and draw the label
     * @param color idle background fill as packed ARGB
     * @param borderColor outline as packed ARGB
     * @param highlightColor hover or forced-hover fill as packed ARGB
     * @param textColor label color as packed ARGB
     * @param message nonnull native component label
     * @param onPress optional callback receiving this button when invoked; may be null
     */
    public Button(int x, int y, int width, int height, Font textRenderer, int color, int borderColor, int highlightColor, int textColor, Component message, Consumer<Button> onPress) {
        super(x, y, width, height, message);
        this.textRenderer = textRenderer;
        setColor(color);
        setBorderColor(borderColor);
        setHighlightColor(highlightColor);
        setTextColor(textColor);
        this.onPress = onPress;
        this.setMessage(message);
    }

    /** Updates geometry only; call setMessage again to refresh label wrapping, height-limited visible-line count and vertical offset after resizing. No press callback is invoked.
     * @param x left position in GUI units
     * @param y top position in GUI units
     * @param width width in GUI units
     * @param height height in GUI units
     * @return self for chaining
     */
    public Button setPos(int x, int y, int width, int height) {
        this.setX(x);
        this.setY(y);
        this.width = width;
        this.height = height;
        return this;
    }

    /** Reports whether the cached wrapped label has more lines than the cached visible-line count.
     * This is a cached line-overflow test, not a fresh horizontal-fit or clipping measurement; dimensions changed by setPos do not refresh it.
     * @return true when cached wrapped lines exceed the visible count; false otherwise
     */
    public boolean cantRenderAllText() {
        return this.textLines.size() > this.visibleLines;
    }

    protected void setMessageSuper(Component message) {
        super.setMessage(message);
    }

    /** Sets the native label and caches wrapping at width minus four GUI units, a height-limited visible-line count and vertical offset for the current geometry.
     * The cache is not refreshed by later geometry changes until this method is called again. This method does not invoke the press callback.
     * @param message nonnull native component label
     */
    @Override
    public void setMessage(Component message) {
        super.setMessage(message);
        this.textLines = textRenderer.split(message, width - 4);
        this.visibleLines = Math.min(Math.max((height - 2) / textRenderer.lineHeight, 1), textLines.size());
        this.verticalCenter = ((height - 4) - (visibleLines * textRenderer.lineHeight)) / 2;
    }

    /** Sets the idle background fill; a zero alpha byte with nonzero RGB is made opaque, while zero remains zero.
     * @param color packed ARGB color
     */
    public void setColor(int color) {
        this.color = ColorMath.fixAlpha(color);
    }

    /** Sets the outline color; a zero alpha byte with nonzero RGB is made opaque, while zero remains zero.
     * @param color packed ARGB color
     */
    public void setBorderColor(int color) {
        this.borderColor = ColorMath.fixAlpha(color);
    }

    /** Sets the hover or forced-hover fill; a zero alpha byte with nonzero RGB is made opaque, while zero remains zero.
     * @param color packed ARGB color
     */
    public void setHighlightColor(int color) {
        this.highlightColor = ColorMath.fixAlpha(color);
    }

    /** Sets the packed ARGB label color, making zero-alpha nonzero RGB opaque; zero remains transparent.
     * The color is used when cached label lines are next drawn; this does not refresh their wrapping.
     * @param color packed ARGB label color
     */
    public void setTextColor(int color) {
        this.textColor = ColorMath.fixAlpha(color);
    }

    protected void renderMessage(GuiGraphics drawContext) {
        for (int i = 0; i < visibleLines; ++i) {
            int w = textRenderer.width(textLines.get(i));
            //? if >=26.1 {
            /*drawContext.text(textRenderer, textLines.get(i), (int) (horizCenter ? getX() + width / 2F - w / 2F : getX() + 2), getY() + 2 + verticalCenter + (i * textRenderer.lineHeight), textColor, false);
            *///?} else {
            drawContext.drawString(textRenderer, textLines.get(i), (int) (horizCenter ? getX() + width / 2F - w / 2F : getX() + 2), getY() + 2 + verticalCenter + (i * textRenderer.lineHeight), textColor, false);
            //?}
        }
    }

    /** Draws this button's fill, outline and cached label when visible, and records its hover result.
     * Forced hover selects the highlight even outside the bounds or while inactive; hidden calls do nothing and leave the recorded value unchanged.
     * Mouse coordinates use GUI units; {@code delta} is unused by this implementation.
     * @param drawContext graphics used for drawing
     * @param mouseX mouse x-coordinate in GUI units
     * @param mouseY mouse y-coordinate in GUI units
     * @param delta unused frame delta
     */
    @Override
    //? if >=26.1 {
    /*public void extractContents(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
    *///? } else if >=1.21.11 {
    /*public void renderContents(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
    *///? } else {
    public void renderWidget(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
    //? }
        if (this.visible) {
            // fill
            if (mouseX - getX() >= 0 && mouseX - getX() - width < 0 && mouseY - getY() >= 0 && mouseY - getY() - height < 0 && this.active || forceHover) {
                hovering = true;
                drawContext.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, highlightColor);
            } else {
                hovering = false;
                drawContext.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, color);
            }
            // outline
            drawContext.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);
            drawContext.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor);
            drawContext.fill(getX() + 1, getY(), getX() + width - 1, getY() + 1, borderColor);
            drawContext.fill(getX() + 1, getY() + height - 1, getX() + width - 1, getY() + height, borderColor);
            this.renderMessage(drawContext);
        }
    }

    // This method is overridden to prevent mouseDown from triggering onPress
    // if onPress is triggered on mouseDown, the keybind editor will detect the mouseUp
    // as the target key.
    //? if >1.21.8 {
    /*/^* Ignores this mouse-press hook; mouse activation is dispatched by the release hook instead.
     * @param buttonEvent unused pointer event
     * @param doubleClick unused double-click indicator
     ^/
    @Override
    public void onClick(MouseButtonEvent buttonEvent, boolean doubleClick) {
    }
    *///?} else {
    /** Ignores the press hook; press dispatch is handled on release instead.
     * @param mouseX mouse x-coordinate in GUI units; unused
     * @param mouseY mouse y-coordinate in GUI units; unused
     */
    @Override
    public void onClick(double mouseX, double mouseY) {
    }
    //?}

    //? if >1.21.8 {
    /*/^* On release, forwards the pointer event and a false double-click flag to the superclass click when {@code isActive()} is true.
     * The inherited active test requires visibility and active state. The superclass click invokes this button's overridable press handler; this hook adds no bounds check.
     * @param buttonEvent pointer event forwarded to the superclass click
     ^/
    @Override
    public void onRelease(MouseButtonEvent buttonEvent) {
        if (this.isActive()) {
            super.onClick(buttonEvent, false);
        }
    }
    *///?} else {
    /** On release, dispatches the superclass click only when this button is active; the coordinates are forwarded to that call.
     * The superclass click invokes this button's overridable press handler. This hook does not perform its own bounds or visibility check.
     * @param mouseX mouse x-coordinate in GUI units, forwarded to the superclass click
     * @param mouseY mouse y-coordinate in GUI units, forwarded to the superclass click
     */
    @Override
    public void onRelease(double mouseX, double mouseY) {
        if (this.active) {
            super.onClick(mouseX, mouseY);
        }
    }
    //?}

    //? if >1.21.8 {
    /*/^* Invokes the current press callback with this button, if a callback is set.
     * A null callback is a no-op; this method does not itself check widget state or change it.
     * @param input unused activation input
     ^/
    @Override
    public void onPress(InputWithModifiers input) {
        if (onPress != null) {
            onPress.accept(this);
        }
    }
    *///?} else {
    /** Invokes the current press callback with this button, if a callback is set.
     * A null callback is a no-op; this method does not itself check widget state or change it.
     */
    @Override
    public void onPress() {
        if (onPress != null) {
            onPress.accept(this);
        }
    }
    //?}

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {

    }

}
