package xyz.wagyourtail.wagyourgui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.wagyourgui.overlays.IOverlayParent;
import xyz.wagyourtail.wagyourgui.overlays.OverlayContainer;

//? if >1.21.8 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
*///?}

public abstract class BaseScreen extends Screen implements IOverlayParent {
    protected Screen parent;
    protected OverlayContainer overlay;

    protected BaseScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    public static FormattedCharSequence trimmed(Font textRenderer, FormattedText str, int width) {
        return Language.getInstance().getVisualOrder(textRenderer.substrByWidth(str, width));
    }

    public void setParent(Screen parent) {
        this.parent = parent;
    }

    public void reload() {
        init();
    }

    @Override
    protected void init() {
        assert minecraft != null;
        clearWidgets();
        super.init();
        overlay = null;
        JsMacrosClient.prevScreen = this;
    }

    @Override
    public void removed() {
        assert minecraft != null;
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
    public OverlayContainer getChildOverlay() {
        if (overlay != null) {
            return overlay.getChildOverlay();
        }
        return null;
    }

    @Override
    public void openOverlay(OverlayContainer overlay, boolean disableButtons) {
        if (this.overlay != null) {
            this.overlay.openOverlay(overlay, disableButtons);
            return;
        }
        if (disableButtons) {
            for (GuiEventListener b : children()) {
                if (!(b instanceof AbstractWidget)) {
                    continue;
                }
                overlay.savedBtnStates.put((AbstractWidget) b, ((AbstractWidget) b).active);
                ((AbstractWidget) b).active = false;
            }
        }
        this.overlay = overlay;
        overlay.init();
    }

    @Override
    public void closeOverlay(OverlayContainer overlay) {
        if (overlay == null) {
            return;
        }
        for (AbstractWidget b : overlay.getButtons()) {
            this.removeWidget(b);
        }
        for (AbstractWidget b : overlay.savedBtnStates.keySet()) {
            b.active = overlay.savedBtnStates.get(b);
        }
        overlay.onClose();
        if (this.overlay == overlay) {
            this.overlay = null;
        }
    }

    @Override
    public void removeWidget(GuiEventListener btn) {
        super.removeWidget(btn);
    }

    @Override
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T renderableWidget) {
        return super.addRenderableWidget(renderableWidget);
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        super.setFocused(focused);
    }

    //? if >1.21.8 {
    /*@Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.isEscape()) {
            if (overlay != null) {
                this.overlay.closeOverlay(this.overlay.getChildOverlay());
                return true;
            }
        }
        return super.keyPressed(keyEvent);
    }
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (overlay != null) {
                this.overlay.closeOverlay(this.overlay.getChildOverlay());
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    //?}

    // TODO: This is a bad way to scroll
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horiz, double vert) {
        if (overlay != null && overlay.scroll != null) {
            //? if >1.21.8 {
            /*MouseButtonEvent evt = new MouseButtonEvent(
                    mouseX,
                    mouseY,
                    new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0));
            *///?}
            overlay.scroll.mouseDragged(
                    //? if >1.21.8 {
                    /*evt,
                    *///?} else {
                    mouseX,
                    mouseY,
                    0,
                    //?}
                    0, -vert * 2);
        }
        return super.mouseScrolled(mouseX, mouseY, horiz, vert);
    }

    //? if >1.21.8 {
    /*@Override
    public boolean mouseClicked(MouseButtonEvent buttonEvent, boolean debounce) {
        if (overlay != null) {
            overlay.onClick(buttonEvent, debounce);
        }
        return super.mouseClicked(buttonEvent, debounce);
    }
    *///?} else {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (overlay != null) {
            overlay.onClick(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    //?}

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        if (overlay != null) {
            overlay.render(drawContext, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return this.overlay == null;
    }

    public void updateSettings() {
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        if (minecraft.level == null) {
            openParent();
        } else {
            setFocused(null);
            minecraft.setScreen(null);
        }
    }

    public void openParent() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }

}
