package com.jsmacrosce.wagyourgui.overlays;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import com.jsmacrosce.wagyourgui.elements.Button;
import com.jsmacrosce.wagyourgui.elements.TextInput;

import java.util.function.Consumer;

public class TextPrompt extends OverlayContainer {
    private final Component message;
    private final Consumer<String> accept;
    public TextInput ti;
    private final String defText;

    public TextPrompt(int x, int y, int width, int height, Font textRenderer, Component message, String defaultText, IOverlayParent parent, Consumer<String> accept) {
        super(x, y, width, height, textRenderer, parent);
        this.message = message;
        this.accept = accept;
        this.defText = defaultText == null ? "" : defaultText;
    }

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

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        this.renderBackground(drawContext);
        int lineNum = 0;
        for (FormattedCharSequence line : textRenderer.split(message, width - 4)) {
            drawContext.drawString(textRenderer, line, (int) (x + width / 2F - textRenderer.width(line) / 2F), y + 5 + (lineNum++) * textRenderer.lineHeight, 0xFFFFFFFF, false);
        }
        super.render(drawContext, mouseX, mouseY, delta);
    }

}
