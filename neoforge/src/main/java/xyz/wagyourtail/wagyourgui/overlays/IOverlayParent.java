package xyz.wagyourtail.wagyourgui.overlays;

import net.minecraft.client.gui.components.events.GuiEventListener;
import xyz.wagyourtail.wagyourgui.containers.IContainerParent;

public interface IOverlayParent extends IContainerParent {

    void closeOverlay(OverlayContainer overlay);

    void setFocused(GuiEventListener focused);

    OverlayContainer getChildOverlay();

}
