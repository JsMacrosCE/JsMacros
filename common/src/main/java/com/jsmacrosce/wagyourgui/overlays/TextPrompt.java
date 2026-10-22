package com.jsmacrosce.wagyourgui.overlays;

import com.jsmacrosce.doclet.DocletCategory;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import com.jsmacrosce.wagyourgui.elements.Button;
import com.jsmacrosce.wagyourgui.elements.TextInput;

import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphics;

/** A WagYourGUI text-input overlay; open it through its parent to initialize and register its widgets.
 * Confirm passes the current input to the optional callback and closes after the callback returns normally;
 * it also closes when no callback was supplied. Cancel closes without calling it. The factory below takes
 * a caller-supplied {@code IOverlayParent}, native {@code Font}, native {@code Component} message and JavaScript callback:
 * <pre>
 * function showTextPrompt(parent, font, message, onAccept) {
 *   const TextPrompt = Java.type('com.jsmacrosce.wagyourgui.overlays.TextPrompt');
 *   const prompt = new TextPrompt(10, 10, 260, 100, font, message, '', parent, JavaWrapper.methodToJava(onAccept));
 *   parent.openOverlay(prompt);
 *   return prompt;
 * }
 * </pre>
 */
@DocletCategory("Screen and UI Elements")
public class TextPrompt extends OverlayContainer {
    private final Component message;
    private final Consumer<String> accept;
    /** The input widget registered by the latest {@code init()} call; null before initialization and replaced on each initialization.
     * Confirm reads this field's current content. Reassigning the field does not replace the registered widget.
     */
    public TextInput ti;
    private final String defText;

    /** Stores the overlay configuration without opening or initializing it; opening through the parent performs widget registration.
     * @param x left edge in GUI coordinates
     * @param y top edge in GUI coordinates
     * @param width width in GUI coordinates
     * @param height height in GUI coordinates
     * @param textRenderer non-null font used for the prompt and its widgets
     * @param message non-null native message component, stored by reference
     * @param defaultText initial input text; null is treated as an empty string
     * @param parent non-null overlay host
     * @param accept optional callback receiving the input content on Confirm, before the overlay closes
     */
    public TextPrompt(int x, int y, int width, int height, Font textRenderer, Component message, String defaultText, IOverlayParent parent, Consumer<String> accept) {
        super(x, y, width, height, textRenderer, parent);
        this.message = message;
        this.accept = accept;
        this.defText = defaultText == null ? "" : defaultText;
    }

    /** Clears local widget tracking, registers a new input and the Cancel and Confirm buttons, then requests
     * focus through the parent and marks the input selected; this is not a select-all operation.
     * Reinitialization replaces the input with the original default text; prior parent registrations are not removed.
     */
    @Override
    public void init() {
        super.init();
        int w = width - 4;

        ti = this.addRenderableWidget(new TextInput(x + 3, y + 25, w - 2, 14, textRenderer, 0xFF101010, 0, 0xFF4040FF, 0xFFFFFFFF, defText, null, null));

        this.addRenderableWidget(new Button(x + 2, y + height - 14, w / 2, 12, textRenderer, 0, 0, 0x7FFFFFFF, 0xFFFFFFFF, Component.translatable("gui.cancel"), (btn) -> close()));

        this.addRenderableWidget(new Button(x + w / 2, y + height - 14, w / 2, 12, textRenderer, 0, 0, 0x7FFFFFFF, 0xFFFFFFFF, Component.translatable("jsmacrosce.confirm"), (btn) -> {
            if (this.accept != null) {
                this.accept.accept(ti.content);
            }
            close();
        }));

        setFocused(ti);
        ti.setSelected(true);
    }

    /** Submits the background and horizontally centered message lines, then forwards the hook to widgets and any child overlay.
     * The stored message is split on each call using the current width minus four. Lines start at {@code y + 5}
     * and advance by the font line height, without an overlay-height limit, so long messages can overlap widgets.
     * This method does not consult the inherited visibility flag.
     * @param drawContext graphics context receiving the submitted drawing operations
     * @param mouseX mouse position in GUI coordinates, forwarded to widgets and child overlay
     * @param mouseY mouse position in GUI coordinates, forwarded to widgets and child overlay
     * @param delta frame value forwarded to widgets and child overlay
     */
    @Override
    //? if >=26.1 {
    /*public void extractRenderState(final GuiGraphicsExtractor drawContext, int mouseX, int mouseY, final float delta) {
    *///? } else {
    public void render(final GuiGraphics drawContext, int mouseX, int mouseY, final float delta) {
    //? }
        this.renderBackground(drawContext);
        int lineNum = 0;
        for (FormattedCharSequence line : textRenderer.split(message, width - 4)) {
            //? if >=26.1 {
            /*drawContext.text(textRenderer, line, (int) (x + width / 2F - textRenderer.width(line) / 2F), y + 5 + (lineNum++) * textRenderer.lineHeight, 0xFFFFFFFF, false);
            *///? } else {
            drawContext.drawString(textRenderer, line, (int) (x + width / 2F - textRenderer.width(line) / 2F), y + 5 + (lineNum++) * textRenderer.lineHeight, 0xFFFFFFFF, false);
            //? }
        }
        //? if >=26.1 {
        /*super.extractRenderState(drawContext, mouseX, mouseY, delta);
        *///? } else {
        super.render(drawContext, mouseX, mouseY, delta);
        //? }
    }

}
