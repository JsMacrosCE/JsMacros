package com.jsmacrosce.wagyourgui.elements;

import com.jsmacrosce.doclet.DocletCategory;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

//? if >1.21.8 {
/*import net.minecraft.client.input.InputWithModifiers;
*///?}

import net.minecraft.client.gui.GuiGraphics;

/** A message-bearing checkbox with a square toggle area at the widget's right edge.
 * The factory takes a caller-supplied native font, label component and JavaScript callback. Register the returned widget with a host for normal GUI input and rendering.
 * <pre>
 * function makeCheckBox(font, label, onChanged) {
 *   const CheckBox = Java.type('com.jsmacrosce.wagyourgui.elements.AnnotatedCheckBox');
 *   return new CheckBox(10, 10, 140, 16, font, 0xFF000000, 0xFFFFFFFF,
 *     0xFF808080, 0xFFFFFFFF, label, false,
 *     JavaWrapper.methodToJava(button => onChanged(button.value)));
 * }
 * </pre>
 */
@DocletCategory("Screen and UI Elements")
public class AnnotatedCheckBox extends Button {
    /** Mutable checked state used by the next visible contents hook; direct assignment does not invoke the press callback or refresh label layout. */
    public boolean value;

    /** Sets up colors and cached label layout through the parent button, then stores the checked state and disables horizontal label centering.
     * Does not invoke the press callback or register the widget with a host. Colors use packed ARGB; zero alpha with nonzero RGB is made opaque.
     * @param x GUI left coordinate
     * @param y GUI top coordinate
     * @param width total widget width, including the square toggle area
     * @param height widget height and square toggle area side length
     * @param textRenderer non-null native font used to render and wrap the label
     * @param color checked square fill when not hovered
     * @param borderColor square outline unless unchecked and hovered
     * @param highlightColor checked hover fill or unchecked hover outline
     * @param textColor label text color
     * @param message non-null native label component
     * @param initialValue initial checked state
     * @param onPress optional callback receiving this widget after its state toggles
     */
    public AnnotatedCheckBox(int x, int y, int width, int height, Font textRenderer, int color, int borderColor, int highlightColor, int textColor, Component message, boolean initialValue, Consumer<Button> onPress) {
        super(x, y, width, height, textRenderer, color, borderColor, highlightColor, textColor, message, onPress);
        value = initialValue;
        horizCenter = false;
    }

    //? if >1.21.8 {
    /*/^* Toggles the checked state before invoking the optional inherited press callback with this widget.
     * A direct call does not check active or visible state; a callback failure propagates without rolling back the toggle.
     * @param input activation input forwarded to the parent, which does not use it
     ^/
    *///?}
    //? if >1.21.8 {
    /*@Override
    public void onPress(InputWithModifiers input) {
        value = !value;
        super.onPress(input);
    }
    *///?} else {
    /** Toggles the checked state before invoking the optional inherited press callback with this widget.
     * A direct call does not check active or visible state; a callback failure propagates without rolling back the toggle.
     */
    @Override
    public void onPress() {
        value = !value;
        super.onPress();
    }
    //?}

    /** Updates the native label and caches wrapping in the left region using {@code width - height - 4}, with visible line count and vertical offset based on current height.
     * Changing the widget size does not refresh this cache; call this method again after resizing.
     * Does not change the checked state or invoke the press callback.
     * @param message non-null native label component
     */
    @Override
    public void setMessage(Component message) {
        setMessageSuper(message);
        int width = this.width - height;
        this.textLines = textRenderer.split(message, width - 4);
        this.visibleLines = Math.min(Math.max((height - 2) / textRenderer.lineHeight, 1), textLines.size());
        this.verticalCenter = ((height - 4) - (visibleLines * textRenderer.lineHeight)) / 2;
    }

    @Override
    protected void renderMessage(GuiGraphics drawContext) {
        int width = this.width - height;
        for (int i = 0; i < visibleLines; ++i) {
            int w = textRenderer.width(textLines.get(i));
            //? if >=26.1 {
            /*drawContext.text(textRenderer, textLines.get(i), (int) (horizCenter ? getX() + width / 2F - w / 2F : getX() + 2), getY() + 2 + verticalCenter + (i * textRenderer.lineHeight), textColor, false);
            *///?} else {
            drawContext.drawString(textRenderer, textLines.get(i), (int) (horizCenter ? getX() + width / 2F - w / 2F : getX() + 2), getY() + 2 + verticalCenter + (i * textRenderer.lineHeight), textColor, false);
            //?}
        }
    }

    /** Submits the cached label and right-edge checkbox square when visible; hidden widgets submit nothing and retain the previous hover flag.
     * Label submission precedes storing hover as mouse-inside-and-active or {@code forceHover}; forced hover also works while inactive.
     * A checked square uses the idle or hover fill; an unchecked hovered square uses the hover outline.
     * Does not call the parent contents hook, forward to children, toggle the value or invoke the press callback.
     * @param drawContext graphics context receiving the drawing operations
     * @param mouseX GUI x-coordinate used to determine hover
     * @param mouseY GUI y-coordinate used to determine hover
     * @param delta frame value, unused by this implementation
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
            this.renderMessage(drawContext);

            boolean hovered = (mouseX - getX() >= 0 && mouseX - getX() - width < 0 && mouseY - getY() >= 0 && mouseY - getY() - height < 0 && this.active) || forceHover;
            hovering = hovered;
            // fill
            if (value) {
                // checked: use highlightColor fill on hover, color fill when idle
                drawContext.fill(getX() + width - height + 1, getY() + 1, getX() + width - 1, getY() + height - 1, hovered ? highlightColor : color);
            }
            // outline: use highlightColor border on hover when unchecked, borderColor otherwise
            int outlineColor = (!value && hovered) ? highlightColor : borderColor;
            drawContext.fill(getX() + width - height, getY(), getX() + width - height + 1, getY() + height, outlineColor);
            drawContext.fill(getX() + width - 1, getY(), getX() + width, getY() + height, outlineColor);
            drawContext.fill(getX() + width - height + 1, getY(), getX() + width - 1, getY() + 1, outlineColor);
            drawContext.fill(getX() + width - height + 1, getY() + height - 1, getX() + width - 1, getY() + height, outlineColor);
        }
    }

}
