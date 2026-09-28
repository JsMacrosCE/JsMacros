package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.classes.render.IScreen;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the client's current screen changes, which includes both opening a screen and
 * closing one.<br>
 * The event only fires on an actual change, so setting the same screen again does not raise it
 * again. Closing a screen counts as a change: a screen going away is reported with a
 * {@code null} {@link #screen} and a {@code null} {@link #screenName}, so always null check both
 * before reading them.<br>
 * This event is not cancellable, the screen has already been swapped by the time listeners see
 * it. If you need to stop a container screen from opening, listen for
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventOpenContainer} instead,
 * that one is cancellable.
 * example:
 * <pre>
 * JsMacros.on("OpenScreen", JavaWrapper.methodToJava(function (event) {
 *   if (event.screenName === null) {
 *     Chat.log("A screen was closed");
 *   } else {
 *     Chat.log(`Opened ${event.screenName}`);
 *   }
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Render/UI")
@Event(value = "OpenScreen", oldName = "OPEN_SCREEN")
public class EventOpenScreen extends BaseEvent {
    /**
     * the screen that is now showing, wrapped in the
     * {@link com.jsmacrosce.jsmacros.client.api.classes.render.IScreen} interface that every
     * game screen implements, including the ones the game ships with. Its class name is
     * available from
     * {@link com.jsmacrosce.jsmacros.client.api.classes.render.IScreen#getScreenClassName() getScreenClassName()}.
     * <br>
     * This is {@code null} when the event is reporting a screen being closed.
     */
    @Nullable
    public final IScreen screen;
    /**
     * a short human readable name for the screen, for example {@code "Chest"} or
     * {@code "Survival Inventory"}. Container screens get a fixed name, other screens fall back
     * to their title and then to {@code "unknown"}, so this is not a stable identifier and it
     * should not be matched on.<br>
     * This is {@code null} when the event is reporting a screen being closed.
     */
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
