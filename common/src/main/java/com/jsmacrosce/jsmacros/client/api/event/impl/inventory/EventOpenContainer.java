package com.jsmacrosce.jsmacros.client.api.event.impl.inventory;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory;
import com.jsmacrosce.jsmacros.client.api.classes.render.IScreen;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when a container screen finishes opening, which is every screen the player opens that
 * holds a menu, so chests, crafting tables, furnaces, shulker boxes, the ender chest and the
 * player's own inventory included.<br>
 * This event is cancellable, and cancelling does close the container again. It is raised at the
 * very end of the call that switches the screen, so the container has already been set and
 * initialised by the time listeners see it, and cancelling puts the screen that was open before
 * back instead. When nothing was open before, that means the container is simply closed again.
 * <br>
 * Note that in creative mode this event is skipped for everything except the creative inventory
 * screen itself, so opening a chest in creative does not raise it. It is also distinct from
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.player.EventOpenScreen}, which fires for
 * every screen change and is raised before this one.
 * example:
 * <pre>
 * JsMacros.on("OpenContainer", JavaWrapper.methodToJava(function (event) {
 *   if (event.screen.getScreenClassName() === "ShulkerBoxScreen") {
 *     Chat.log("no shulker boxes today");
 *     event.cancel();
 *   }
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.6.5
 */
@DocletCategory("Inventory")
@Event(value = "OpenContainer", cancellable = true)
public class EventOpenContainer extends BaseEvent {
    /**
     * the container that is opening, wrapped in a fresh {@link Inventory}. The concrete type
     * follows the screen, so a chest gives back a container inventory and a crafting table gives
     * back a crafting inventory. It is the same menu the screen is showing, so an action taken
     * through it reaches the open container.
     */
    public final Inventory<?> inventory;
    /**
     * the screen that is opening, as the {@link IScreen} interface every game screen implements.
     * Its class name is available from
     * {@link com.jsmacrosce.jsmacros.client.api.classes.render.IScreen#getScreenClassName() getScreenClassName()}.
     */
    public final IScreen screen;

    public EventOpenContainer(AbstractContainerScreen<?> screen) {
        super(JsMacrosClient.clientCore);
        this.inventory = Inventory.create(screen);
        this.screen = (IScreen) screen;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"screenName\": \"%s\", \"inventory\": %s}", this.getEventName(), JsMacrosClient.getScreenName((Screen) screen), inventory);
    }

}
