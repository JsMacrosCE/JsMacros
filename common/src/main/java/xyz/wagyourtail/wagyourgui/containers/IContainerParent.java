package xyz.wagyourtail.wagyourgui.containers;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import xyz.wagyourtail.wagyourgui.overlays.IOverlayParent;
import xyz.wagyourtail.wagyourgui.overlays.OverlayContainer;

public interface IContainerParent {

    <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T renderableWidget);

    void removeWidget(GuiEventListener button);

    void openOverlay(OverlayContainer overlay);

    void openOverlay(OverlayContainer overlay, boolean disableButtons);

    IOverlayParent getFirstOverlayParent();

}
