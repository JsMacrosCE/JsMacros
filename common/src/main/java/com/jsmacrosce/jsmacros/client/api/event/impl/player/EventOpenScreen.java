package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.render.IScreen;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "OpenScreen", oldName = "OPEN_SCREEN")
public class EventOpenScreen extends BaseEvent {
    @Nullable
    public final IScreen screen;
    @DocletReplaceReturn("ScreenName")
    public final String screenName;

    public EventOpenScreen(Screen screen) {
        super(JsMacrosClient.clientCore);
        this.screen = (IScreen) screen;
        this.screenName = JsMacrosClient.getScreenName(screen);
    }

    @Override
    public String toString() {
        return String.format("%s:{\"screenName\": \"%s\"}", this.getEventName(), screenName);
    }

}
