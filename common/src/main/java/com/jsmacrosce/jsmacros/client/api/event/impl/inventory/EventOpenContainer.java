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
 * Fires after a container screen is set. Cancelling restores the previously displayed screen, or
 * a {@code KeyMacrosScreen(null)} fallback if there was none. In creative mode it is skipped for
 * container screens other than the creative inventory. Unlike
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.player.EventOpenScreen}, it only fires for
 * container screens and fires after that event.
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
     * The opening container, wrapped in a fresh {@link Inventory} for the screen's menu.
     */
    public final Inventory<?> inventory;
    /**
     * The container screen that is opening, as an {@link IScreen}. Its class name is available from
     * {@link com.jsmacrosce.jsmacros.client.api.classes.render.IScreen#getScreenClassName() getScreenClassName()}.
     */
    public final IScreen screen;

    /**
     * Wraps the supplied container screen and retains it as the event screen. This does not open or
     * display a screen; a non-null {@code screen} must implement {@link IScreen}. A failed cast occurs
     * after the inventory wrapper is created. A null input leaves both {@link #screen} and
     * {@link #inventory} null.
     *
     * @param screen the container screen to wrap and retain; may be null
     */
    public EventOpenContainer(AbstractContainerScreen<?> screen) {
        super(JsMacrosClient.clientCore);
        this.inventory = Inventory.create(screen);
        this.screen = (IScreen) screen;
    }

    /**
     * Returns a diagnostic string containing the event name, screen name, and inventory.
     * The format is {@code %s:{"screenName": "%s", "inventory": %s}}; values are inserted
     * with {@link String#format(String, Object...)} formatting, not JSON serialization. The
     * computed screen name from the stored screen is rendered as {@code "null"} when null; a null inventory is rendered as
     * unquoted {@code null}.
     *
     * @return the formatted event and payload summary
     */
    @Override
    public String toString() {
        return String.format("%s:{\"screenName\": \"%s\", \"inventory\": %s}", this.getEventName(), JsMacrosClient.getScreenName((Screen) screen), inventory);
    }

}
