package com.jsmacrosce.jsmacros.client.api.event.impl.inventory;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory;
import com.jsmacrosce.jsmacros.client.api.classes.render.IScreen;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author Wagyourtail
 * @since 1.6.5
 */
@Event(value = "OpenContainer", cancellable = true)
public class EventOpenContainer extends BaseEvent {
    public final Inventory<?> inventory;
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
