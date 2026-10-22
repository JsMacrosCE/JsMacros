package com.jsmacrosce.wagyourgui.overlays;

import net.minecraft.client.gui.components.events.GuiEventListener;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.wagyourgui.containers.IContainerParent;

/**
 * Parent contract for overlay operations in the WagYourGUI hierarchy, implemented by {@code BaseScreen} and {@code OverlayContainer}.
 * <pre>
 * const Client = Java.type("com.jsmacrosce.jsmacros.client.JsMacrosClient");
 * const parent = Client.prevScreen;
 * if (parent !== null) {
 *   Chat.log(parent.getChildOverlay() !== null);
 * }
 * </pre>
 */
@DocletCategory("Screen and UI Elements")
public interface IOverlayParent extends IContainerParent {

    /** Requests that the given overlay be closed through this hierarchy.
     * @param overlay overlay to close in this hierarchy
     */
    void closeOverlay(OverlayContainer overlay);

    /** Sets the focused GUI listener for the owning screen.
     * @param focused listener to focus, or {@code null} to clear focus
     */
    void setFocused(GuiEventListener focused);

    /** Returns the deepest nested overlay.
     * @return the deepest overlay, or this overlay when the receiver has no child, or {@code null} when a screen has no overlay
     */
    OverlayContainer getChildOverlay();

}
