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
 * Fires when the client receives a full container content packet, which is the server resending
 * every slot of a container at once. This happens when a container is first opened, when the
 * server decides to resync it, and when the player's own inventory is filled in on join, so it is
 * much rarer than a single slot changing.<br>
 * This event is not cancellable, it only reports what the server sent.<br>
 * For a single slot changing, listen for
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventSlotUpdate} instead. For the
 * screen itself opening, see
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventOpenContainer}.
 * <br>
 * The player inventory case wraps a screen that is not on display: when the packet is for the
 * player's own inventory the event builds a throwaway inventory screen for it, so
 * {@link #screen} there is not the screen the player is looking at, if any.
 * example:
 * <pre>
 * JsMacros.on("ContainerUpdate", JavaWrapper.methodToJava(function (event) {
 *   const inv = event.inventory;
 *   Chat.log(`${inv.getType()} resynced, ${inv.getItems().size()} non empty stacks in ${inv.getTotalSlots()} slots`);
 * }))
 * </pre>
 * @author Wagyourtail
 */
@DocletCategory("Inventory")
@Event(value = "ContainerUpdate")
public class EventContainerUpdate extends BaseEvent {
    /**
     * the container that was resynced, wrapped in a fresh {@link Inventory}. The concrete type
     * follows the screen, so a chest gives back a container inventory and the player's own
     * inventory gives back a player inventory. Read the contents through this rather than through
     * {@link #screen}, because this is already the type-specific wrapper for the container.
     */
    public final Inventory<?> inventory;
    /**
     * the screen the resynced container belongs to, as the
     * {@link IScreen} interface every game screen implements.<br>
     * This is not {@code null}, but for a resync of the player's own inventory it is a screen that
     * was built just for this event rather than the one the player is actually looking at.
     */
    public final IScreen screen;

    public EventContainerUpdate(AbstractContainerScreen<?> screen) {
        super(JsMacrosClient.clientCore);
        this.inventory = Inventory.create(screen);
        this.screen = (IScreen) screen;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"screenName\": \"%s\", \"inventory\": %s}", this.getEventName(), JsMacrosClient.getScreenName((Screen) screen), inventory);
    }

}
