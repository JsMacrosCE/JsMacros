package com.jsmacrosce.wagyourgui.overlays;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.wagyourgui.elements.Button;

import java.util.List;
import java.util.function.Consumer;

/** A WagYourGUI message overlay with Confirm and Cancel buttons. Confirm passes this overlay to the optional callback,
 * then closes it after a normal callback return; it also closes when no callback was supplied. Cancel closes without
 * invoking the callback. The factory takes a caller-supplied {@code IOverlayParent}, native {@code Font},
 * native {@code Component} message and JavaScript callback:
 * <pre>
 * function showConfirm(parent, font, message, onConfirm) {
 *   const ConfirmOverlay = Java.type('com.jsmacrosce.wagyourgui.overlays.ConfirmOverlay');
 *   const overlay = new ConfirmOverlay(10, 10, 240, 100, font, message, parent, JavaWrapper.methodToJava(onConfirm));
 *   parent.openOverlay(overlay);
 *   return overlay;
 * }
 * </pre>
 */
@DocletCategory("Screen and UI Elements")
public class ConfirmOverlay extends OverlayContainer {
    private final Consumer<ConfirmOverlay> accept;
    private List<FormattedCharSequence> text;
    private int lines;
    /** Whether each cached message line is horizontally centered; defaults to {@code true}, while {@code false} places it at {@code x + 3}. Changing this affects placement on the next hook but does not rewrap the lines or recompute the cached vertical layout. */
    public boolean hcenter = true;
    private int vcenter;

    /** Stores the overlay configuration and computes the cached message layout immediately, with centered message alignment.
     * This does not open the overlay or register its buttons; opening through the parent initializes them.
     * @param x left edge in GUI coordinates
     * @param y top edge in GUI coordinates
     * @param width overlay width in GUI coordinates
     * @param height overlay height in GUI coordinates
     * @param textRenderer non-null font used to measure and draw the message
     * @param message non-null message to split into cached display lines
     * @param parent non-null overlay host
     * @param accept optional callback receiving this overlay when Confirm is pressed, before it closes
     */
    public ConfirmOverlay(int x, int y, int width, int height, Font textRenderer, Component message, IOverlayParent parent, Consumer<ConfirmOverlay> accept) {
        super(x, y, width, height, textRenderer, parent);
        this.setMessage(message);
        this.accept = accept;
    }

    /** Stores the overlay configuration and computes the cached message layout immediately, then sets the specified alignment.
     * This does not open the overlay or register its buttons; opening through the parent initializes them.
     * @param x left edge in GUI coordinates
     * @param y top edge in GUI coordinates
     * @param width overlay width in GUI coordinates
     * @param height overlay height in GUI coordinates
     * @param hcenter {@code true} to center each line; {@code false} to place lines at {@code x + 3}
     * @param textRenderer non-null font used to measure and draw the message
     * @param message non-null message to split into cached display lines
     * @param parent non-null overlay host
     * @param accept optional callback receiving this overlay when Confirm is pressed, before it closes
     */
    public ConfirmOverlay(int x, int y, int width, int height, boolean hcenter, Font textRenderer, Component message, IOverlayParent parent, Consumer<ConfirmOverlay> accept) {
        this(x, y, width, height, textRenderer, message, parent, accept);
        this.hcenter = hcenter;
    }

    /** Replaces the cached wrapped lines using the current font and width minus six, caching the line count and vertical offset.
     * Only the leading lines selected by the current height are submitted. A nonempty split selects at least one line,
     * even in a short overlay; this does not guarantee that the selected lines fit. Call again after changing bounds
     * or font when the cached wrapping or vertical placement should be refreshed.
     * @param message non-null message to wrap and display
     */
    public void setMessage(Component message) {
        this.text = textRenderer.split(message, width - 6);
        this.lines = Math.min(Math.max((height - 15) / textRenderer.lineHeight, 1), text.size());
        this.vcenter = ((height - 15) - (lines * textRenderer.lineHeight)) / 2;
    }

    /** Clears local button tracking and registers fresh Cancel and Confirm buttons with the host. This does not reset the cached message or alignment; repeated initialization does not remove earlier button registrations from the parent.
     */
    @Override
    public void init() {
        super.init();

        this.addRenderableWidget(new Button(x + 2, y + height - 12, (width - 4) / 2, 10, textRenderer, 0, 0, 0x7FFFFFFF, 0xFFFFFFFF, Component.translatable("gui.cancel"), (btn) -> {
            this.close();
        }));

        this.addRenderableWidget(new Button(x + (width - 4) / 2 + 2, y + height - 12, (width - 4) / 2, 10, textRenderer, 0, 0, 0x7FFFFFFF, 0xFFFFFFFF, Component.translatable("jsmacrosce.confirm"), (btn) -> {
            if (this.accept != null) {
                this.accept.accept(this);
            }
            this.close();
        }));

    }

    protected void renderMessage(GuiGraphics drawContext) {
        for (int i = 0; i < lines; ++i) {
            int w = textRenderer.width(text.get(i));
            int centeredX = (int) (hcenter ? x + width / 2F - w / 2F : x + 3);
            //? if >=26.1 {
            /*drawContext.text(textRenderer, text.get(i), centeredX, y + 2 + vcenter + (i * textRenderer.lineHeight), 0xFFFFFFFF, false);
            *///? } else {
            drawContext.drawString(textRenderer, text.get(i), centeredX, y + 2 + vcenter + (i * textRenderer.lineHeight), 0xFFFFFFFF, false);
            //? }
        }
    }

    /** Submits the background, bottom separator and cached message lines, then forwards the hook to registered widgets and a child overlay.
     * Placement reads the current position, width and alignment, but wrapping, selected line count and vertical offset
     * remain cached until {@code setMessage} is called. Lines start at {@code y + 2 + vcenter} and advance by the
     * font line height. This method does not consult the inherited visibility flag.
     * @param drawContext graphics context receiving the drawing operations
     * @param mouseX mouse x-coordinate in GUI coordinates, forwarded to widgets and child overlay
     * @param mouseY mouse y-coordinate in GUI coordinates, forwarded to widgets and child overlay
     * @param delta frame value forwarded to widgets and child overlay
     */
    @Override
    //? if >=26.1 {
    /*public void extractRenderState(final GuiGraphicsExtractor drawContext, int mouseX, int mouseY, final float delta) {
    *///? } else {
    public void render(final GuiGraphics drawContext, int mouseX, int mouseY, final float delta) {
    //? }
        this.renderBackground(drawContext);
        drawContext.fill(x + 1, y + height - 13, x + width - 1, y + height - 12, 0xFFFFFFFF);
        this.renderMessage(drawContext);
        //? if >=26.1 {
        /*super.extractRenderState(drawContext, mouseX, mouseY, delta);
        *///? } else {
        super.render(drawContext, mouseX, mouseY, delta);
        //? }
    }

}
