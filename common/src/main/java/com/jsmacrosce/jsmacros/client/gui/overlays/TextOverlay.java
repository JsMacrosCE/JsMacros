package com.jsmacrosce.jsmacros.client.gui.overlays;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import com.jsmacrosce.wagyourgui.elements.Button;
import com.jsmacrosce.wagyourgui.overlays.IOverlayParent;
import com.jsmacrosce.wagyourgui.overlays.OverlayContainer;

/** A WagYourGUI text-message overlay with a confirmation button that closes it.
 * The constructor stores the supplied text component; open the overlay through its parent to initialize it.
 * <pre>
 * function showText(parent, font, text) {
 *   const TextOverlay = Java.type('com.jsmacrosce.jsmacros.client.gui.overlays.TextOverlay');
 *   const overlay = new TextOverlay(10, 10, 180, 80, font, parent, text);
 *   overlay.centered = false;
 *   parent.openOverlay(overlay);
 * }
 * </pre>
 * Here {@code parent}, {@code font}, and {@code text} are caller-supplied overlay parent, native font, and native text component.
 */
@DocletCategory("Screen and UI Elements")
public class TextOverlay extends OverlayContainer {
    private final Component text;
    /** Whether to horizontally offset the text toward the center of the overlay; defaults to {@code true}.
     * The offset uses the full component's measured width, is clamped to the left text inset, and does not center wrapped lines or affect wrapping.
     */
    public boolean centered = true;

    /** Stores the overlay bounds, font, parent, and text; it does not open or initialize the overlay.
     * @param x left edge of the overlay in screen GUI coordinates
     * @param y top edge of the overlay in screen GUI coordinates
     * @param width overlay width
     * @param height overlay height
     * @param textRenderer non-null font used to measure and draw the text
     * @param parent non-null overlay host used to register widgets and close this overlay
     * @param text non-null native text component to display
     */
    public TextOverlay(int x, int y, int width, int height, Font textRenderer, IOverlayParent parent, Component text) {
        super(x, y, width, height, textRenderer, parent);
        this.text = text;
    }

    /** Clears this overlay's tracked buttons and registers a confirmation button with its parent; the button closes this overlay.
     * Reinitializing clears the local tracking list but does not unregister buttons previously registered with the parent.
     */
    @Override
    public void init() {
        super.init();

        addRenderableWidget(new Button(x + 2, y + this.height - 12, this.width - 4, 10, this.textRenderer, 0, 0xFF000000, 0x7FFFFFFF, 0xFFFFFFFF, Component.translatable("jsmacrosce.confirm"), (btn) -> {
            this.close();
        }));
    }

    /** Submits the overlay background and wrapped text, then forwards the hook to its buttons and any child overlay.
     * This method does not consult the inherited visibility flag.
     * When {@code centered} is true, the text's horizontal offset is based on the full component width and clamped to the left inset.
     * @param drawContext graphics context receiving the overlay content
     * @param mouseX current mouse x-coordinate, forwarded to widgets and a child overlay
     * @param mouseY current mouse y-coordinate, forwarded to widgets and a child overlay
     * @param delta frame value forwarded to widgets and a child overlay
     */
    @Override
    //? if >=26.1 {
    /*public void extractRenderState(final GuiGraphicsExtractor drawContext, int mouseX, int mouseY, final float delta) {
    *///? } else {
    public void render(final GuiGraphics drawContext, int mouseX, int mouseY, final float delta) {
    //? }
        renderBackground(drawContext);
        int x = this.centered ? Math.max(this.x + 3, this.x + 3 + (this.width - 6) / 2 - this.textRenderer.width(this.text) / 2) : this.x + 3;
        //? if >=26.1 {
        /*drawContext.textWithWordWrap(textRenderer, this.text, x, this.y + 5, width - 6, 0xFFFFFFFF, false);
        *///? } else {
        drawContext.drawWordWrap(textRenderer, this.text, x, this.y + 5, width - 6, 0xFFFFFFFF, false);
        //? }
        //? if >=26.1 {
        /*super.extractRenderState(drawContext, mouseX, mouseY, delta);
        *///? } else {
        super.render(drawContext, mouseX, mouseY, delta);
        //? }
    }

}
