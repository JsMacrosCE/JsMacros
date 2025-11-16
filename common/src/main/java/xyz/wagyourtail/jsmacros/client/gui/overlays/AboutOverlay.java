package xyz.wagyourtail.jsmacros.client.gui.overlays;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import xyz.wagyourtail.wagyourgui.elements.Button;
import xyz.wagyourtail.wagyourgui.overlays.IOverlayParent;
import xyz.wagyourtail.wagyourgui.overlays.OverlayContainer;

import java.util.List;

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
        this.addDrawableChild(new Button(x + width - 12, y + 2, 10, 10, textRenderer, 0, 0x7FFFFFFF, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal("X"), (btn) -> this.close()));

        this.addDrawableChild(new Button(x + 2, y + height - 14, w / 3, 12, textRenderer, 0, 0x7FFFFFFF, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal("Website"), (btn) -> Util.getPlatform().openUri("https://jsmacros.wagyourtail.xyz")));

        this.addDrawableChild(new Button(x + w / 3 + 2, y + height - 14, w / 3, 12, textRenderer, 0, 0x7FFFFFFF, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal("Discord"), (btn) -> Util.getPlatform().openUri("https://discord.gg/P6W58J8")));

        this.addDrawableChild(new Button(x + w * 2 / 3 + 2, y + height - 14, w / 3, 12, textRenderer, 0, 0x7FFFFFFF, 0x7FFFFFFF, 0xFFFFFFFF, Component.literal("CurseForge"), (btn) -> Util.getPlatform().openUri("https://www.curseforge.com/minecraft/mc-mods/jsmacros")));

        this.setMessage(Component.translatable("jsmacros.aboutinfo"));
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

        drawContext.drawWordWrap(textRenderer, Component.translatable("jsmacros.about"), x + 3, y + 3, width - 14, 0xFFFFFFFF, false);
        renderMessage(drawContext);

        drawContext.fill(x + 2, y + 12, x + width - 2, y + 13, 0xFFFFFFFF);
        drawContext.fill(x + 2, y + height - 15, x + width - 2, y + height - 14, 0xFFFFFFFF);
        super.render(drawContext, mouseX, mouseY, delta);

    }

}
