package com.jsmacrosce.wagyourgui;

import com.jsmacrosce.jsmacros.client.util.GuiCompat;
import com.jsmacrosce.doclet.DocletCategory;

import net.minecraft.client.gui.Font;
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
import com.mojang.blaze3d.platform.InputConstants;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.wagyourgui.overlays.IOverlayParent;
import com.jsmacrosce.wagyourgui.overlays.OverlayContainer;

//? if >1.21.8 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
*///?}

import net.minecraft.client.gui.GuiGraphics;

/** Base class for screens that host nested overlays and retain a navigation parent.
 * Subclasses provide GUI initialization and rendering behavior. The client stores the
 * most recently initialized instance in {@code prevScreen}; that need not be the
 * currently displayed screen.
 * <pre>
 * const Client = Java.type('com.jsmacrosce.jsmacros.client.JsMacrosClient');
 * const screen = Client.prevScreen;
 * if (screen !== null) {
 *   screen.reload();
 * }
 * </pre>
 */
@DocletCategory("Screen and UI Elements")
public abstract class BaseScreen extends Screen implements IOverlayParent {
    protected Screen parent;
    protected OverlayContainer overlay;

    protected BaseScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    /** Trims formatted text to a font-measured prefix and applies the current language's visual ordering.
     * @param textRenderer non-null font used to measure the trimmed prefix
     * @param str non-null formatted text in logical order
     * @param width requested maximum measured width
     * @return language-ordered sequence for the trimmed prefix
     */
    public static FormattedCharSequence trimmed(Font textRenderer, FormattedText str, int width) {
        return Language.getInstance().getVisualOrder(textRenderer.substrByWidth(str, width));
    }

    /** Sets the screen used as this screen's navigation parent; this does not open or display it.
     * @param parent parent screen, or {@code null} when there is no parent
     */
    public void setParent(Screen parent) {
        this.parent = parent;
    }

    /** Invokes this screen's overridable initialization without opening it; the base implementation expects the Minecraft client to be assigned.
     * The base initialization clears registered widgets and discards the current overlay
     * without its normal close callback or saved active-state restoration, then records this instance as {@code prevScreen}.
     */
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

    /** Base implementation performs only an assertion that the client is available. */
    @Override
    public void removed() {
        assert minecraft != null;
    }

    /** Opens an overlay at the deepest open level, saving and deactivating existing host-level AbstractWidget entries by default.
     * @param overlay non-null overlay to open
     */
    @Override
    public void openOverlay(OverlayContainer overlay) {
        openOverlay(overlay, true);
    }

    /** Returns this screen as the first overlay host.
     * @return this screen as overlay host
     */
    @Override
    public IOverlayParent getFirstOverlayParent() {
        return this;
    }

    /** Returns the deepest open overlay, or {@code null} when none is open.
     * @return deepest open overlay, or {@code null}
     */
    @Override
    public OverlayContainer getChildOverlay() {
        if (overlay != null) {
            return overlay.getChildOverlay();
        }
        return null;
    }

    /** Opens an overlay at the deepest level; at this level, optionally saves and deactivates existing widgets.
     * Only existing AbstractWidget entries are deactivated; false leaves their active states unchanged. The overlay is assigned before its
     * initialization, so an initialization failure leaves the assignment and any deactivation in place.
     * Saved-state entries are merged rather than cleared, and the overlay is not automatically reparented.
     * @param overlay non-null overlay to open
     * @param disableButtons whether to save and deactivate existing host-level widgets
     */
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

    /** Removes the supplied overlay's tracked widgets from this screen, restores its saved widget states, and calls its close hook.
     * The supplied overlay need not be this screen's current overlay; only a matching current reference is cleared, after the hook returns.
     * A hook failure leaves that reference uncleared. The supplied tracking list, saved-state map and child overlay are not cleared or recursively closed.
     * @param overlay overlay to clean up and notify, or {@code null} for a no-op
     */
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

    /** Removes a listener from this screen's native widget registrations.
     * Removes one matching entry from each applicable rendering, narration and input list, not from an overlay's local tracking list.
     * On Minecraft 1.21.10 and later, native removal clears focus when this is the focused listener; 1.21.5 and 1.21.8 do not.
     * @param btn listener to remove
     */
    @Override
    public void removeWidget(GuiEventListener btn) {
        super.removeWidget(btn);
    }

    /** Registers a widget for input, rendering, and narration, returning the same widget.
     * This adds to the native rendering, input and narration lists without duplicate filtering or overlay-local tracking.
     * @param <T> widget type supporting input, rendering, and narration
     * @param renderableWidget non-null widget to register
     * @return the supplied widget reference
     */
    @Override
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T renderableWidget) {
        return super.addRenderableWidget(renderableWidget);
    }

    /** Sets the current focused listener, unfocusing the old listener and focusing a non-null replacement even if it is not registered as a child.
     * A null replacement clears focus; this does not register a widget or check its visibility or activation.
     * @param focused listener to focus, or null to clear focus
     */
    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        super.setFocused(focused);
    }

    //? if >1.21.8 {
    /*/^* On Escape with a direct overlay, requests closing its deepest overlay and returns true if closing returns normally.
     * Other paths delegate to the native screen handler; this method does not forward ordinary keys directly to the overlay's key hook.
     * @param keyEvent non-null native key event; its key code is tested for Escape before inspecting the overlay, and the event is forwarded unchanged on delegation
     * @return true for the intercepted Escape path, otherwise the native screen handler's result
     ^/
    @Override
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
    /** On Escape with a direct overlay, requests closing its deepest overlay and returns true if closing returns normally.
     * Other paths delegate to the native screen handler; this method does not forward ordinary keys directly to the overlay's key hook.
     * @param keyCode GLFW key code
     * @param scanCode platform-specific scan code forwarded to the screen handler
     * @param modifiers modifier flags forwarded to the screen handler
     * @return true for the intercepted Escape path, otherwise the native screen handler's result
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            if (overlay != null) {
                this.overlay.closeOverlay(this.overlay.getChildOverlay());
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    //?}

    // TODO: This is a bad way to scroll
    /** If the direct overlay has a scrollbar, forwards vertical scrolling to it as a drag, then delegates the original scroll event to the native screen handler.
     * The drag uses zero horizontal delta and -2 times the vertical scroll amount, without a local activation or visibility check. Its result is ignored; a failure prevents native delegation.
     * @param mouseX mouse X coordinate in GUI space
     * @param mouseY mouse Y coordinate in GUI space
     * @param horiz horizontal scroll amount, forwarded unchanged to the native handler
     * @param vert vertical scroll amount, multiplied by -2 for the scrollbar drag
     * @return the native screen handler's result, not whether the scrollbar moved
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horiz, double vert) {
        if (overlay != null && overlay.scroll != null) {
            //? if >1.21.8 {
            /*MouseButtonEvent evt = new MouseButtonEvent(
                    mouseX,
                    mouseY,
                    new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
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
    /*/^* Calls the direct overlay's click hook, if present, before delegating to the native screen handler.
     * Overlay presence does not itself consume the click; if the overlay hook throws, native delegation is not reached.
     * @param buttonEvent non-null native click event forwarded unchanged to the overlay and native handler
     * @param doubleClick caller-supplied double-click flag forwarded unchanged; this method does not compute it
     * @return the native screen handler's result, not the overlay hook's handling state
     ^/
    @Override
    public boolean mouseClicked(MouseButtonEvent buttonEvent, boolean doubleClick) {
        if (overlay != null) {
            overlay.onClick(buttonEvent, doubleClick);
        }
        return super.mouseClicked(buttonEvent, doubleClick);
    }
    *///?} else {
    /** Calls the direct overlay's click hook, if present, before delegating to the native screen handler.
     * Overlay presence does not itself consume the click; if the overlay hook throws, native delegation is not reached.
     * @param mouseX mouse X coordinate in GUI space
     * @param mouseY mouse Y coordinate in GUI space
     * @param button clicked mouse button code
     * @return the native screen handler's result, not the overlay hook's handling state
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (overlay != null) {
            overlay.onClick(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    //?}

    //? if >=26.1 {
    /*/^* Submits the direct overlay's render state when present, without calling the superclass's widget-submission hook.
     * This implementation does not call the screen's background hook or submit the screen's own widgets; subclasses handle their own widget submission.
     * @param graphics non-null native graphics context forwarded to the overlay
     * @param mouseX mouse X coordinate in GUI space forwarded to the overlay
     * @param mouseY mouse Y coordinate in GUI space forwarded to the overlay
     * @param a caller-supplied frame value forwarded unchanged to the overlay
     ^/
    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, int mouseX, int mouseY, final float a) {
        if (overlay != null) {
            overlay.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }
    *///? } else {
    /** Renders the direct overlay when present without calling the superclass to render this screen's own widgets.
     * Minecraft 1.21.5 first renders the screen background, even without an overlay; later targets using this hook do not automatically render that background. Subclasses handle their own widgets.
     * @param drawContext non-null native graphics context
     * @param mouseX mouse X coordinate in GUI space
     * @param mouseY mouse Y coordinate in GUI space
     * @param delta frame value forwarded to the overlay
     */
    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        //? if <=1.21.5 {
        /*this.renderBackground(drawContext, mouseX, mouseY, delta);
        *///?}
        if (overlay != null) {
            overlay.render(drawContext, mouseX, mouseY, delta);
        }
    }
    //? }

    /** Reports whether the native Escape-close path is allowed; this query does not close anything.
     * @return true when no direct overlay is set, false otherwise
     */
    @Override
    public boolean shouldCloseOnEsc() {
        return this.overlay == null;
    }

    /** No-op extension hook for subclass settings updates or persistence.
     * BaseScreen does not automatically invoke it from closing, parent navigation, reinitialization or removal.
     */
    public void updateSettings() {
    }

    /** With no loaded level, calls the overridable openParent; with a level, explicitly clears focus and asks the client to clear the current screen.
     * Expects the Minecraft client to be assigned. Does not call updateSettings or explicitly close overlays or clear their saved state.
     */
    @Override
    public void onClose() {
        assert minecraft != null;
        if (minecraft.level == null) {
            openParent();
        } else {
            setFocused(null);
            GuiCompat.setScreen(minecraft, null);
        }
    }

    /** Asks the client to open the stored parent screen, or clear the current screen when the parent is null.
     * Expects the Minecraft client to be assigned. Does not call updateSettings, clear the stored parent, or explicitly close overlays.
     */
    public void openParent() {
        assert minecraft != null;
        GuiCompat.setScreen(minecraft, parent);
    }

}
