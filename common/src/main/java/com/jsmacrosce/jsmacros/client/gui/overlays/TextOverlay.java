package com.jsmacrosce.jsmacros.client.gui.overlays;

import net.minecraft.client.gui.Font;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///? } else {
import net.minecraft.client.gui.GuiGraphics;
//? }
import net.minecraft.network.chat.Component;
import com.jsmacrosce.wagyourgui.elements.Button;
import com.jsmacrosce.wagyourgui.overlays.IOverlayParent;
import com.jsmacrosce.wagyourgui.overlays.OverlayContainer;

public class TextOverlay extends OverlayContainer {
    private final Component text;
    public boolean centered = true;

    public TextOverlay(int x, int y, int width, int height, Font textRenderer, IOverlayParent parent, Component text) {
        super(x, y, width, height, textRenderer, parent);
        this.text = text;
    }

    @Override
    public void init() {
        super.init();

        addRenderableWidget(new Button(x + 2, y + this.height - 12, this.width - 4, 10, this.textRenderer, 0, 0xFF000000, 0x7FFFFFFF, 0xFFFFFFFF, Component.translatable("jsmacrosce.confirm"), (btn) -> {
            this.close();
        }));
    }

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
