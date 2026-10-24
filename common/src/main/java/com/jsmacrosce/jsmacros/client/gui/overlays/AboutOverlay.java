package com.jsmacrosce.jsmacros.client.gui.overlays;

import com.jsmacrosce.jsmacros.client.util.PlatformCompat;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import com.jsmacrosce.wagyourgui.elements.Button;
import com.jsmacrosce.wagyourgui.overlays.IOverlayParent;
import com.jsmacrosce.wagyourgui.overlays.OverlayContainer;

import java.util.List;

import net.minecraft.Util;

/**
 * WagYourGUI about-information overlay with close, Website, Discord, and Modrinth buttons; it is not a global HUD library.
 * The constructor takes an overlay parent and native font, but no message. Set a custom native text
 * component with {@code setMessage} after opening, since initialization resets the default about information.
 * In this factory, the caller supplies an {@code IOverlayParent}, native {@code Font}, and native {@code Component}:
 * <pre>
 * function showAbout(parent, font, message) {
 *   const AboutOverlay = Java.type('com.jsmacrosce.jsmacros.client.gui.overlays.AboutOverlay');
 *   const overlay = new AboutOverlay(10, 10, 220, 140, font, parent);
 *   parent.openOverlay(overlay);
 *   overlay.setMessage(message);
 * }
 * </pre>
 */
@DocletCategory("Screen and UI Elements")
public class AboutOverlay extends OverlayContainer {
    private List<FormattedCharSequence> text;
    private int lines;
    private int vcenter;

    /** Stores the overlay bounds, font, and parent; it does not open or initialize the overlay.
     * @param x left screen GUI coordinate
     * @param y top screen GUI coordinate
     * @param width overlay width
     * @param height overlay height
     * @param textRenderer non-null font used to measure and draw text
     * @param parent non-null overlay host used to register widgets and close this overlay
     */
    public AboutOverlay(int x, int y, int width, int height, Font textRenderer, IOverlayParent parent) {
        super(x, y, width, height, textRenderer, parent);
    }

    /** Registers the close and external-link buttons and resets the message to the default about information.
     * Reinitialization clears this container's button tracking before registering replacements, but does not
     * unregister the previously registered widgets from the parent. The close button requests closing through
     * the parent; the other buttons request platform URI opening without guaranteeing an external outcome.
     */
    @Override
    public void init() {
        super.init();
        int w = width - 4;
        this.addRenderableWidget(new Button(x + width - 12, y + 2, 10, 10, textRenderer, 0, 0x7FFFFFFF, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal("X"), (btn) -> this.close()));

        this.addRenderableWidget(new Button(x + 2, y + height - 14, w / 3, 12, textRenderer, 0, 0x7FFFFFFF, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal("Website"), (btn) -> PlatformCompat.openUri("https://jsmacrosce.com")));

        this.addRenderableWidget(new Button(x + w / 3 + 2, y + height - 14, w / 3, 12, textRenderer, 0, 0x7FFFFFFF, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal("Discord"), (btn) -> PlatformCompat.openUri("https://discord.gg/UaB82D59Fu")));

        this.addRenderableWidget(new Button(x + w * 2 / 3 + 2, y + height - 14, w / 3, 12, textRenderer, 0, 0x7FFFFFFF, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal("Modrinth"), (btn) -> PlatformCompat.openUri("https://modrinth.com/mod/jsmacrosce")));

        this.setMessage(Component.translatable("jsmacrosce.aboutinfo"));
    }

    /** Wraps and caches the message for the current bounds and font; only the leading cached lines are drawn.
     * Wrapping uses the current width minus six; the visible-line count and vertical offset are cached.
     * Call again after changing the bounds or font to refresh the layout. A nonempty split selects at least
     * one line even in a short overlay; this does not guarantee that the selected lines fit.
     * @param message non-null text component to wrap and display
     */
    public void setMessage(Component message) {
        this.text = textRenderer.split(message, width - 6);
        this.lines = Math.min(Math.max((height - 27) / textRenderer.lineHeight, 1), text.size());
        this.vcenter = ((height - 12) - (lines * textRenderer.lineHeight)) / 2;
    }

    protected void renderMessage(GuiGraphics drawContext) {
        for (int i = 0; i < lines; ++i) {
            int w = textRenderer.width(text.get(i));
            //? if >=26.1 {
            /*drawContext.text(textRenderer, text.get(i), (int) (x + width / 2F - w / 2F), y + 2 + vcenter + (i * textRenderer.lineHeight), 0xFFFFFFFF, false);
            *///? } else {
            drawContext.drawString(textRenderer, text.get(i), (int) (x + width / 2F - w / 2F), y + 2 + vcenter + (i * textRenderer.lineHeight), 0xFFFFFFFF, false);
            //? }
        }
    }

    /** Submits the background, title, cached message and separators, then forwards the hook to widgets and a child overlay.
     * The cached message layout is not recomputed here; only its selected leading lines are submitted, each
     * horizontally centered using its measured width and positioned using the cached vertical offset.
     * This method does not consult the inherited visibility flag.
     * @param drawContext graphics context receiving the overlay contents
     * @param mouseX current mouse x-coordinate, forwarded to widgets and child overlay
     * @param mouseY current mouse y-coordinate, forwarded to widgets and child overlay
     * @param delta frame value forwarded to widgets and child overlay
     */
    @Override
    //? if >=26.1 {
    /*public void extractRenderState(final GuiGraphicsExtractor drawContext, int mouseX, int mouseY, final float delta) {
    *///? } else {
    public void render(final GuiGraphics drawContext, int mouseX, int mouseY, final float delta) {
    //? }
        renderBackground(drawContext);

        //? if >=26.1 {
        /*drawContext.textWithWordWrap(textRenderer, Component.translatable("jsmacrosce.about"), x + 3, y + 3, width - 14, 0xFFFFFFFF, false);
        *///? } else {
        drawContext.drawWordWrap(textRenderer, Component.translatable("jsmacrosce.about"), x + 3, y + 3, width - 14, 0xFFFFFFFF, false);
        //? }
        renderMessage(drawContext);

        drawContext.fill(x + 2, y + 12, x + width - 2, y + 13, 0xFFFFFFFF);
        drawContext.fill(x + 2, y + height - 15, x + width - 2, y + height - 14, 0xFFFFFFFF);
        //? if >=26.1 {
        /*super.extractRenderState(drawContext, mouseX, mouseY, delta);
        *///? } else {
        super.render(drawContext, mouseX, mouseY, delta);
        //? }

    }

}
