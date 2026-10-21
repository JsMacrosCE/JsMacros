package com.jsmacrosce.wagyourgui.containers;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import com.jsmacrosce.wagyourgui.overlays.IOverlayParent;
import com.jsmacrosce.wagyourgui.overlays.OverlayContainer;

/** Parent contract for registering and removing widgets and routing overlays in WagYourGUI containers and screens.
 * <pre>
 * const Client = Java.type("com.jsmacrosce.jsmacros.client.JsMacrosClient");
 * const parent = Client.prevScreen;
 * if (parent !== null) {
 *   const host = parent.getFirstOverlayParent();
 *   Chat.log(host.getChildOverlay() !== null);
 * }
 * </pre>
 */
public interface IContainerParent {

    /** Registers a widget with this parent for GUI input, rendering, and narration.
     * Multi-element containers require widgets to extend {@code AbstractWidget}; other implementations may have different requirements.
     * @param <T> widget type supporting GUI input, rendering, and narration
     * @param renderableWidget widget to register
     * @return the supplied widget reference
     */
    <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T renderableWidget);

    /** Unregisters a widget through this parent hierarchy.
     * @param button widget to remove
     */
    void removeWidget(GuiEventListener button);

    /** Opens an overlay at the deepest available overlay level, disabling that level's existing buttons by default.
     * @param overlay overlay to open
     */
    void openOverlay(OverlayContainer overlay);

    /** Opens and initializes an overlay at the deepest available overlay level.
     * When {@code disableButtons} is {@code true}, the hosting level saves the active states of its existing buttons and disables them; {@code false} leaves their states unchanged.
     * @param overlay overlay to open
     * @param disableButtons whether to save and disable existing buttons at the level hosting the new overlay
     */
    void openOverlay(OverlayContainer overlay, boolean disableButtons);

    /** Finds the nearest overlay-capable parent in the parent chain.
     * @return the nearest overlay-capable parent, or this object when it is already one
     */
    IOverlayParent getFirstOverlayParent();

}
