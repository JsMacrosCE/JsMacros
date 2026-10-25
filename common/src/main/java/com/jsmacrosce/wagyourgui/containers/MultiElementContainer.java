package com.jsmacrosce.wagyourgui.containers;

import com.jsmacrosce.doclet.DocletCategory;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import com.jsmacrosce.wagyourgui.overlays.IOverlayParent;
import com.jsmacrosce.wagyourgui.overlays.OverlayContainer;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;

/** Base for WagYourGUI groups that track widgets registered with a parent, along with shared geometry and font state.
 * Subclasses provide their own initialization and rendering behavior; this base does not automatically render registered widgets.
 * Given an existing subclass instance, this function hides its tracked widgets and returns their count without
 * clearing or unregistering them. The stored visibility flag is not a universal rendering guard for subclasses.
 * <pre>
 * function hideGroup(container) {
 *   container.setVisible(false);
 *   return container.getButtons().size();
 * }
 * </pre>
 * @param <T> parent type implementing {@code IContainerParent}
 */
@DocletCategory("Screen and UI Elements")
public abstract class MultiElementContainer<T extends IContainerParent> implements IContainerParent {
    protected List<AbstractWidget> buttons = new ArrayList<>();
    protected Font textRenderer;
    protected boolean visible = true;
    /** Non-null parent reference used to register widgets and route overlay operations. */
    public final T parent;
    /** Stored left coordinate in GUI space; changing it does not move registered widgets. */
    public int x;
    /** Stored top coordinate in GUI space; changing it does not move registered widgets. */
    public int y;
    /** Stored width in GUI units; changing it does not resize registered widgets. */
    public int width;
    /** Stored height in GUI units; changing it does not resize registered widgets. */
    public int height;

    /** Stores the container geometry, font, and parent without initializing or registering widgets.
     * @param x left coordinate in GUI space
     * @param y top coordinate in GUI space
     * @param width container width in GUI units
     * @param height container height in GUI units
     * @param textRenderer non-null font used by subclasses
     * @param parent non-null parent used for widget registration and overlay routing
     */
    public MultiElementContainer(int x, int y, int width, int height, Font textRenderer, T parent) {
        this.textRenderer = textRenderer;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.parent = parent;
    }

    /** Clears this container's current tracked-widget list; it does not unregister those widgets from the parent,
     * reset stored geometry or reset the visibility flag. */
    public void init() {
        buttons.clear();
    }

    /** Returns this container's stored visibility flag, initially {@code true}; this need not match every widget
     * or determine whether a subclass submits content.
     * @return the stored container visibility flag
     */
    public boolean getVisible() {
        return visible;
    }

    /** Sets the visibility and active state of currently tracked widgets, then stores the container flag.
     * Earlier independent visible and active states are overwritten rather than saved for restoration.
     * Later registrations do not automatically inherit this flag; untracked widgets and child overlays are not changed.
     * @param visible {@code true} to show and activate tracked widgets, or {@code false} to hide and deactivate them
     */
    public void setVisible(boolean visible) {
        for (AbstractWidget btn : buttons) {
            btn.visible = visible;
            btn.active = visible;
        }
        this.visible = visible;
    }

    /** Tracks a widget locally, registers it with the parent, and returns the supplied reference.
     * The value must be an {@code AbstractWidget}; an object satisfying only the declared interfaces fails the cast
     * before tracking or delegation. The widget is tracked before parent registration, with no rollback if registration fails.
     * Duplicates are not filtered, the parent's return value is ignored and the stored visibility flag is not applied.
     * @param <T> widget type supporting GUI input, rendering, and narration
     * @param drawableElement non-null {@code AbstractWidget} supporting those interfaces
     * @return the supplied widget reference
     */
    @Override
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T drawableElement) {
        buttons.add((AbstractWidget) drawableElement);
        parent.addRenderableWidget(drawableElement);
        return drawableElement;
    }

    /** Returns the live mutable list of widgets tracked by this container; mutations do not register or unregister widgets with the parent.
     * @return the current mutable tracked-widget list, not a copy; external mutations affect later tracking-based operations
     */
    public List<AbstractWidget> getButtons() {
        return buttons;
    }

    /** Stores the container position and dimensions; this base implementation does not reposition or resize registered widgets.
     * @param x left coordinate in GUI space
     * @param y top coordinate in GUI space
     * @param width width in GUI units
     * @param height height in GUI units
     */
    public void setPos(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /** Delegates opening the overlay to the parent, which applies its default overlay policy.
     * Standard WagYourGUI hosts initialize it at the deepest available overlay level, saving and disabling that level's existing button active states.
     * @param overlay non-null overlay to open through the parent
     */
    @Override
    public void openOverlay(OverlayContainer overlay) {
        parent.openOverlay(overlay);
    }

    /** Delegates opening the overlay and button policy unchanged to the parent. In standard WagYourGUI hosts, the overlay is initialized at the deepest available level; disabling buttons affects only the hosting level.
     * @param overlay non-null overlay to open through the parent
     * @param disableButtons whether to save and disable existing button active states at the hosting level; false leaves those states unchanged
     */
    @Override
    public void openOverlay(OverlayContainer overlay, boolean disableButtons) {
        parent.openOverlay(overlay, disableButtons);
    }

    /** Removes the first matching entry from this container's tracked list, then delegates the same widget to the parent
     * even when it was not tracked locally. Duplicate entries may remain; a parent failure does not roll back local removal.
     * @param button widget to remove
     */
    @Override
    public void removeWidget(GuiEventListener button) {
        this.buttons.remove(button);
        parent.removeWidget(button);
    }

    /** Returns the parent lookup result for the nearest overlay-capable host in the standard parent hierarchy.
     * @return the parent's nearest overlay-capable host lookup result
     */
    @Override
    public IOverlayParent getFirstOverlayParent() {
        return parent.getFirstOverlayParent();
    }

    //? if >=26.1 {
    /*/^* Subclass hook for submitting container-specific render state; parent widget registration does not invoke this hook automatically.
     * Subclasses decide what to submit and whether to honor the stored visibility flag; there is no base extraction body.
     * @param graphics graphics context receiving render state
     * @param mouseX mouse x-coordinate in GUI space
     * @param mouseY mouse y-coordinate in GUI space
     * @param a frame value supplied by the caller
     ^/
    public abstract void extractRenderState(final GuiGraphicsExtractor graphics, int mouseX, int mouseY, final float a);
    *///? } else {
    /** Subclass hook for rendering container-specific content; parent widget registration does not invoke this hook automatically.
     * Subclasses decide what to render and whether to honor the stored visibility flag; there is no base rendering body.
     * @param drawContext graphics context for drawing
     * @param mouseX mouse x-coordinate in GUI space
     * @param mouseY mouse y-coordinate in GUI space
     * @param delta frame value supplied by the caller
     */
    public abstract void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta);
    //? }
}
