package com.jsmacrosce.wagyourgui.elements;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import com.jsmacrosce.jsmacros.client.util.ColorUtil;

//? if >1.21.8 {
/*import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

import java.util.List;
import java.util.function.Consumer;

public class Button extends AbstractButton {
    protected final Font textRenderer;
    protected int color;
    protected int borderColor;
    protected int highlightColor;
    protected int textColor;
    protected List<FormattedCharSequence> textLines;
    protected int visibleLines;
    protected int verticalCenter;
    public boolean horizCenter = true;
    public Consumer<Button> onPress;
    public boolean hovering = false;
    public boolean forceHover = false;

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

    public Button setPos(int x, int y, int width, int height) {
        this.setX(x);
        this.setY(y);
        this.width = width;
        this.height = height;
        return this;
    }

    public boolean cantRenderAllText() {
        return this.textLines.size() > this.visibleLines;
    }

    protected void setMessageSuper(Component message) {
        super.setMessage(message);
    }

    @Override
    public void setMessage(Component message) {
        super.setMessage(message);
        this.textLines = textRenderer.split(message, width - 4);
        this.visibleLines = Math.min(Math.max((height - 2) / textRenderer.lineHeight, 1), textLines.size());
        this.verticalCenter = ((height - 4) - (visibleLines * textRenderer.lineHeight)) / 2;
    }

    public void setColor(int color) {
        this.color = ColorUtil.fixAlpha(color);
    }

    public void setBorderColor(int color) {
        this.borderColor = ColorUtil.fixAlpha(color);
    }

    public void setHighlightColor(int color) {
        this.highlightColor = ColorUtil.fixAlpha(color);
    }

    public void setTextColor(int color) {
        this.textColor = ColorUtil.fixAlpha(color);
    }

    protected void renderMessage(GuiGraphics drawContext) {
        for (int i = 0; i < visibleLines; ++i) {
            int w = textRenderer.width(textLines.get(i));
            drawContext.drawString(textRenderer, textLines.get(i), (int) (horizCenter ? getX() + width / 2F - w / 2F : getX() + 1), getY() + 2 + verticalCenter + (i * textRenderer.lineHeight), textColor, false);
        }
    }

    @Override
    public void renderWidget(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        if (this.visible) {
            // fill
            if (mouseX - getX() >= 0 && mouseX - getX() - width <= 0 && mouseY - getY() >= 0 && mouseY - getY() - height <= 0 && this.active || forceHover) {
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
    @Override
    //? if >1.21.8 {
    /*public void onClick(MouseButtonEvent buttonEvent, boolean debounce) {
    }
    *///?} else {
    public void onClick(double mouseX, double mouseY) {
    }
    //?}

    @Override
    //? if >1.21.8 {
    /*public void onRelease(MouseButtonEvent buttonEvent) {
        if (this.isActive()) {
            super.onClick(buttonEvent, true);
        }
    }
    *///?} else {
    public void onRelease(double mouseX, double mouseY) {
        if (this.active) {
            super.onClick(mouseX, mouseY);
        }
    }
    //?}

    @Override
    //? if >1.21.8 {
    /*public void onPress(InputWithModifiers input) {
        if (onPress != null) {
            onPress.accept(this);
        }
    }
    *///?} else {
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
