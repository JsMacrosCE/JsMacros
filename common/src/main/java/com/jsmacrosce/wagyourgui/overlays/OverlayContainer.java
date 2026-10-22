package com.jsmacrosce.wagyourgui.overlays;

import com.jsmacrosce.doclet.DocletCategory;

import net.minecraft.client.gui.Font;
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

import net.minecraft.client.gui.GuiGraphics;

/** A container that hosts nested overlays and delegates overlay operations through its parent. */
@DocletCategory("Screen and UI Elements")
public abstract class OverlayContainer extends MultiElementContainer<IOverlayParent> implements IOverlayParent {
    /** Mutable, replaceable map of hosting widgets to their saved active states, populated by standard hosts when opening this overlay with button disabling enabled.
     * Opening merges entries; initialization and closing do not clear them. Keep the map and stored Boolean values non-null for restoration.
     * Changes affect later restoration, but do not register widgets or immediately change their active states.
     */
    public Map<AbstractWidget, Boolean> savedBtnStates = new HashMap<>();
    /** Optional scroll control, initially null, used by standard screen hosts to forward wheel input to their direct overlay.
     * Assigning it does not register or replace widgets.
     */
    public Scrollbar scroll;
    protected OverlayContainer overlay;

    /** Stores the overlay geometry, font, and parent without initializing, opening or registering widgets.
     * @param x left coordinate in GUI space
     * @param y top coordinate in GUI space
     * @param width width in GUI units
     * @param height height in GUI units
     * @param textRenderer non-null font for subclass use
     * @param parent non-null parent for widget registration and overlay routing
     */
    public OverlayContainer(int x, int y, int width, int height, Font textRenderer, IOverlayParent parent) {
        super(x, y, width, height, textRenderer, parent);
    }

    /** Removes the first matching local tracked entry, then delegates the widget to the parent even if it was not tracked locally.
     * Duplicate local entries can remain; a parent failure does not roll back the local removal.
     * @param btn widget or listener to remove
     */
    @Override
    public void removeWidget(GuiEventListener btn) {
        this.buttons.remove(btn);
        parent.removeWidget(btn);
    }

    /** Opens the child overlay, disabling this container's buttons by default.
     * @param overlay the child overlay to open
     */
    @Override
    public void openOverlay(OverlayContainer overlay) {
        openOverlay(overlay, true);
    }

    /** Returns this container as the first overlay parent.
     * @return this overlay container
     */
    @Override
    public IOverlayParent getFirstOverlayParent() {
        return this;
    }

    /** Opens a child overlay at the deepest open level; optionally disables this container's buttons and saves their active states.
     * @param overlay the child overlay to open
     * @param disableButtons whether to disable this container's buttons and save their active states
     */
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

    /** Returns the deepest child overlay, or this container when it has no child.
     * @return the deepest child overlay, or this container if there is no child
     */
    @Override
    public OverlayContainer getChildOverlay() {
        if (overlay != null) {
            return overlay.getChildOverlay();
        } else {
            return this;
        }
    }

    /** Closes the matching direct child overlay, removes its buttons, restores saved active states, and calls its {@link #onClose()} hook.
     * @param overlay the direct child overlay to close
     */
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

    /** Forwards the focus target unchanged to the parent; {@code null} is forwarded to clear focus.
     * @param focused listener to focus, or {@code null} to clear focus
     */
    @Override
    public void setFocused(GuiEventListener focused) {
        parent.setFocused(focused);
    }

    //? if >1.21.8 {
    /*/^* Forwards the click to this container's direct child overlay, if present; otherwise does nothing.
     * @param buttonEvent native click event forwarded unchanged to the child
     * @param doubleClick caller-supplied double-click flag forwarded unchanged; not computed here
     ^/
    *///?}
    //? if >1.21.8 {
    /*public void onClick(MouseButtonEvent buttonEvent, boolean doubleClick) {
        if (overlay != null) {
            overlay.onClick(buttonEvent, doubleClick);
        }
    }
    *///?} else {
    /** Forwards the click to this container's direct child overlay, if present; otherwise does nothing.
     * @param mouseX mouse x-coordinate in GUI space
     * @param mouseY mouse y-coordinate in GUI space
     * @param button mouse-button code forwarded unchanged
     */
    public void onClick(double mouseX, double mouseY, int button) {
        if (overlay != null) {
            overlay.onClick(mouseX, mouseY, button);
        }
    }
    //?}

    /** Forwards the key press to this container's child overlay, if present.
     * @param keyEvent the key press event
     * @return {@code false}
     */
    //? if >1.21.8 {
    /*public boolean keyPressed(KeyEvent keyEvent) {
        if (overlay != null) {
            overlay.keyPressed(keyEvent);
        }
        return false;
    }
    *///?} else {
    /** Forwards the key press to this container's child overlay, if present; this method always returns {@code false}.
     * @param keyCode the key code
     * @param scanCode the scan code
     * @param modifiers the modifier flags
     * @return {@code false}
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (overlay != null) {
            overlay.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }
    //?}

    /** Requests that this overlay be closed through its parent. */
    public void close() {
        parent.closeOverlay(this);
    }

    /** No-op extension hook called by standard hosts after widget removal and active-state restoration, before clearing the matching child reference.
     * This base implementation does not close the overlay or clear its tracked widgets, saved states or child reference.
     */
    public void onClose() {
    }

    /** Submits a black background and gray outer and white inner one-unit border fills at the stored geometry.
     * Does not render widgets or child overlays, or check the stored container visibility flag.
     * @param drawContext graphics context receiving the fill operations
     */
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


    //? if >=26.1 {
    /*/^* Forwards render-state extraction to tracked widgets in live list order, then this container's direct child overlay if present.
     * Does not submit the background automatically or check the stored container visibility flag; each widget applies its own extraction behavior.
     * @param graphics graphics context passed to widgets and the child overlay
     * @param mouseX mouse x-coordinate in GUI space
     * @param mouseY mouse y-coordinate in GUI space
     * @param a frame value forwarded to widgets and the child overlay
     ^/
    *///?}
    //? if >=26.1 {
    /*@Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, int mouseX, int mouseY, final float a) {
        for (AbstractWidget button : buttons) {
            button.extractRenderState(graphics, mouseX, mouseY, a);
        }
        if (overlay != null) {
            overlay.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }
    *///? } else {
    /** Renders tracked widgets in live list order, then this container's direct child overlay if present.
     * Does not render the background automatically or check the stored container visibility flag; each widget applies its own rendering behavior.
     * @param drawContext graphics context passed to widgets and the child overlay
     * @param mouseX mouse x-coordinate in GUI space
     * @param mouseY mouse y-coordinate in GUI space
     * @param delta frame value forwarded to widgets and the child overlay
     */
    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        for (AbstractWidget btn : buttons) {
            btn.render(drawContext, mouseX, mouseY, delta);
        }
        if (this.overlay != null) {
            this.overlay.render(drawContext, mouseX, mouseY, delta);
        }
    }//? }

}
