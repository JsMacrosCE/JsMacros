package com.jsmacrosce.jsmacros.client.gui.overlays;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import com.jsmacrosce.wagyourgui.elements.Button;
import com.jsmacrosce.wagyourgui.overlays.IOverlayParent;
import com.jsmacrosce.wagyourgui.overlays.OverlayContainer;

import java.util.List;

//? if >=1.21.11 {
/*import net.minecraft.util.Util;
*///? } else {
import net.minecraft.Util;
//? }

public class AboutOverlay extends OverlayContainer {
    private List<FormattedCharSequence> text;
    private int lines;
    private int vcenter;

    public AboutOverlay(int x, int y, int width, int height, Font textRenderer, IOverlayParent parent) {
        super(x, y, width, height, textRenderer, parent);
    }

    @Override
    public void init() {
        super.init();
        int w = width - 4;
        this.addRenderableWidget(new Button(x + width - 12, y + 2, 10, 10, textRenderer, 0, 0x7FFFFFFF, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal("X"), (btn) -> this.close()));

        this.addRenderableWidget(new Button(x + 2, y + height - 14, w / 3, 12, textRenderer, 0, 0x7FFFFFFF, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal("Website"), (btn) -> Util.getPlatform().openUri("https://jsmacrosce.com")));

        this.addRenderableWidget(new Button(x + w / 3 + 2, y + height - 14, w / 3, 12, textRenderer, 0, 0x7FFFFFFF, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal("Discord"), (btn) -> Util.getPlatform().openUri("https://discord.gg/UaB82D59Fu")));

        this.addRenderableWidget(new Button(x + w * 2 / 3 + 2, y + height - 14, w / 3, 12, textRenderer, 0, 0x7FFFFFFF, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal("Modrinth"), (btn) -> Util.getPlatform().openUri("https://modrinth.com/mod/jsmacrosce")));

        this.setMessage(Component.translatable("jsmacrosce.aboutinfo"));
    }

    public void setMessage(Component message) {
        this.text = textRenderer.split(message, width - 6);
        this.lines = Math.min(Math.max((height - 27) / textRenderer.lineHeight, 1), text.size());
        this.vcenter = ((height - 12) - (lines * textRenderer.lineHeight)) / 2;
    }

    protected void renderMessage(GuiGraphics drawContext) {
        for (int i = 0; i < lines; ++i) {
            int w = textRenderer.width(text.get(i));
            drawContext.drawString(textRenderer, text.get(i), (int) (x + width / 2F - w / 2F), y + 2 + vcenter + (i * textRenderer.lineHeight), 0xFFFFFFFF, false);
        }
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        renderBackground(drawContext);

        drawContext.drawWordWrap(textRenderer, Component.translatable("jsmacrosce.about"), x + 3, y + 3, width - 14, 0xFFFFFFFF, false);
        renderMessage(drawContext);

        drawContext.fill(x + 2, y + 12, x + width - 2, y + 13, 0xFFFFFFFF);
        drawContext.fill(x + 2, y + height - 15, x + width - 2, y + height - 14, 0xFFFFFFFF);
        super.render(drawContext, mouseX, mouseY, delta);

    }

}
