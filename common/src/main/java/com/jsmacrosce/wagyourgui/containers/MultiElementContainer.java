package com.jsmacrosce.wagyourgui.containers;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import com.jsmacrosce.wagyourgui.overlays.IOverlayParent;
import com.jsmacrosce.wagyourgui.overlays.OverlayContainer;

import java.util.ArrayList;
import java.util.List;

public abstract class MultiElementContainer<T extends IContainerParent> implements IContainerParent {
    protected List<AbstractWidget> buttons = new ArrayList<>();
    protected Font textRenderer;
    protected boolean visible = true;
    public final T parent;
    public int x;
    public int y;
    public int width;
    public int height;

    public MultiElementContainer(int x, int y, int width, int height, Font textRenderer, T parent) {
        this.textRenderer = textRenderer;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.parent = parent;
    }

    public void init() {
        buttons.clear();
    }

    public boolean getVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        for (AbstractWidget btn : buttons) {
            btn.visible = visible;
            btn.active = visible;
        }
        this.visible = visible;
    }

    @Override
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T drawableElement) {
        buttons.add((AbstractWidget) drawableElement);
        parent.addRenderableWidget(drawableElement);
        return drawableElement;
    }

    public List<AbstractWidget> getButtons() {
        return buttons;
    }

    public void setPos(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void openOverlay(OverlayContainer overlay) {
        parent.openOverlay(overlay);
    }

    @Override
    public void openOverlay(OverlayContainer overlay, boolean disableButtons) {
        parent.openOverlay(overlay, disableButtons);
    }

    @Override
    public void removeWidget(GuiEventListener button) {
        this.buttons.remove(button);
        parent.removeWidget(button);
    }

    @Override
    public IOverlayParent getFirstOverlayParent() {
        return parent.getFirstOverlayParent();
    }

    public abstract void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta);

}
