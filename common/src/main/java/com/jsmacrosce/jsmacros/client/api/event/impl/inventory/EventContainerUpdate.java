package com.jsmacrosce.jsmacros.client.api.event.impl.inventory;

import com.jsmacrosce.doclet.DocletCategory;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory;
import com.jsmacrosce.jsmacros.client.api.classes.render.IScreen;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires after the client handles a full container-content packet. For individual slot updates, see
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventSlotUpdate}. For screen
 * openings, see {@link com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventOpenContainer}.
 * A packet for the player's inventory supplies a newly created inventory screen as {@link #screen},
 * which may not be the screen currently displayed.
 * @author Wagyourtail
 */
@DocletCategory("Inventory")
@Event(value = "ContainerUpdate")
public class EventContainerUpdate extends BaseEvent {
    /**
     * The container represented by the associated screen, wrapped in a new {@link Inventory}.
     */
    public final Inventory<?> inventory;
    /**
     * The screen associated with the resynced container. For a player-inventory packet this is a
     * newly created inventory screen, not necessarily the screen currently displayed.
     */
    public final IScreen screen;

    /**
     * Wraps the supplied container screen as the event's screen and inventory.
     * A {@code null} screen produces null values for both fields. A non-null screen must implement
     * {@link IScreen}; otherwise the cast fails after the inventory wrapper is created.
     * @param screen the container screen to wrap and retain as {@link #screen}; may be null
     */
    public EventContainerUpdate(AbstractContainerScreen<?> screen) {
        super(JsMacrosClient.clientCore);
        this.inventory = Inventory.create(screen);
        this.screen = (IScreen) screen;
    }

    /**
     * Returns a diagnostic string containing the event name, current screen name, and inventory.
     * A null screen name is rendered as the quoted string {@code "null"}; a null inventory is
     * rendered as the unquoted token {@code null}. This is a formatted diagnostic, not a JSON
     * serialization of the screen or container.
     * @return a brace-delimited diagnostic of the form {@code eventName:{"screenName": "name", "inventory": value}}
     */
    @Override
    public String toString() {
        return String.format("%s:{\"screenName\": \"%s\", \"inventory\": %s}", this.getEventName(), JsMacrosClient.getScreenName((Screen) screen), inventory);
    }

}
