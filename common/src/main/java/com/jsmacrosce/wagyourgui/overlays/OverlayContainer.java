package com.jsmacrosce.wagyourgui.overlays;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import com.jsmacrosce.wagyourgui.containers.MultiElementContainer;
import com.jsmacrosce.wagyourgui.elements.Scrollbar;

import java.util.HashMap;
import java.util.Map;

//? if >1.21.8 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

public abstract class OverlayContainer extends MultiElementContainer<IOverlayParent> implements IOverlayParent {
    public Map<AbstractWidget, Boolean> savedBtnStates = new HashMap<>();
    public Scrollbar scroll;
    protected OverlayContainer overlay;

    public OverlayContainer(int x, int y, int width, int height, Font textRenderer, IOverlayParent parent) {
        super(x, y, width, height, textRenderer, parent);
    }

    @Override
    public void removeWidget(GuiEventListener btn) {
        this.buttons.remove(btn);
        parent.removeWidget(btn);
    }

    @Override
    public void openOverlay(OverlayContainer overlay) {
        openOverlay(overlay, true);
    }

    @Override
    public IOverlayParent getFirstOverlayParent() {
        return this;
    }

    @Override
    public void openOverlay(OverlayContainer overlay, boolean disableButtons) {
        if (this.overlay != null) {
            this.overlay.openOverlay(overlay, disableButtons);
            return;
        }
        if (disableButtons) {
            for (AbstractWidget b : buttons) {
                overlay.savedBtnStates.put(b, b.active);
                b.active = false;
            }
        }
        this.overlay = overlay;
        overlay.init();
    }

    @Override
    public OverlayContainer getChildOverlay() {
        if (overlay != null) {
            return overlay.getChildOverlay();
        } else {
            return this;
        }
    }

    @Override
    public void closeOverlay(OverlayContainer overlay) {
        if (this.overlay != null && this.overlay == overlay) {
            for (AbstractWidget b : overlay.getButtons()) {
                removeWidget(b);
            }
            for (AbstractWidget b : overlay.savedBtnStates.keySet()) {
                b.active = overlay.savedBtnStates.get(b);
            }
            overlay.onClose();
            this.overlay = null;
        } else {
            parent.closeOverlay(overlay);
        }
    }

    @Override
    public void setFocused(GuiEventListener focused) {
        parent.setFocused(focused);
    }

    //? if >1.21.8 {
    /*public void onClick(MouseButtonEvent buttonEvent, boolean debounce) {
        if (overlay != null) {
            overlay.onClick(buttonEvent, debounce);
        }
    }
    *///?} else {
    public void onClick(double mouseX, double mouseY, int button) {
        if (overlay != null) {
            overlay.onClick(mouseX, mouseY, button);
        }
    }
    //?}

    /**
     * @return true if should be handled by overlay
     */
    //? if >1.21.8 {
    /*public boolean keyPressed(KeyEvent keyEvent) {
        if (overlay != null) {
            overlay.keyPressed(keyEvent);
        }
        return false;
    }
    *///?} else {
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (overlay != null) {
            overlay.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }
    //?}

    public void close() {
        parent.closeOverlay(this);
    }

    public void onClose() {
    }

    public void renderBackground(GuiGraphics drawContext) {
        // black bg
        drawContext.fill(x, y, x + width, y + height, 0xFF000000);
        // 2 layer border
        drawContext.fill(x, y, x + width, y + 1, 0x7F7F7F7F);
        drawContext.fill(x, y + height - 1, x + width, y + height, 0x7F7F7F7F);
        drawContext.fill(x, y + 1, x + 1, y + height - 1, 0x7F7F7F7F);
        drawContext.fill(x + width - 1, y + 1, x + width, y + height - 1, 0x7F7F7F7F);

        drawContext.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFFFFFFFF);
        drawContext.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xFFFFFFFF);
        drawContext.fill(x + 1, y + 1, x + 2, y + height - 1, 0xFFFFFFFF);
        drawContext.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, 0xFFFFFFFF);

    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        for (AbstractWidget btn : buttons) {
            btn.render(drawContext, mouseX, mouseY, delta);
        }
        if (this.overlay != null) {
            this.overlay.render(drawContext, mouseX, mouseY, delta);
        }
    }

}
