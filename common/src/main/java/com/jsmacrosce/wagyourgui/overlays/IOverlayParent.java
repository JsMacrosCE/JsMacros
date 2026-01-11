package com.jsmacrosce.wagyourgui.overlays;

import net.minecraft.client.gui.components.events.GuiEventListener;
import com.jsmacrosce.wagyourgui.containers.IContainerParent;

public interface IOverlayParent extends IContainerParent {

    void closeOverlay(OverlayContainer overlay);

    void setFocused(GuiEventListener focused);

    OverlayContainer getChildOverlay();

}
