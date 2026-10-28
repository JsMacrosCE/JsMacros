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
 * Fires when the client's current screen changes, including when a screen is closed. The event
 * is dispatched after the screen field is assigned. Reassigning the current screen does not fire
 * it. On close, both {@link #screen} and {@link #screenName} are {@code null}. Container screens
 * have a separate event that can be cancelled:
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventOpenContainer}.
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
     * The newly assigned screen, wrapped as an {@link IScreen}; {@code null} when a screen is
     * closed. Its class name is available from
     * {@link IScreen#getScreenClassName()}.
     */
    @Nullable
    public final IScreen screen;
    /**
     * A display name for the screen: container screens use names assigned by JsMacros, while
     * other screens use their title or {@code "unknown"} when the title is empty. This is not a
     * stable identifier. {@code null} when a screen is closed.
     */
    @DocletReplaceReturn("ScreenName")
    public final String screenName;

    /**
     * Captures the supplied screen and its display name when constructing the event; this does
     * not open or close a screen. A {@code null} screen sets both event fields to {@code null}.
     * A non-null screen must implement {@link IScreen}, or the cast fails with
     * {@link ClassCastException}.
     * @param screen the Minecraft screen to capture, or {@code null} when closing a screen
     */
    public EventOpenScreen(Screen screen) {
        super(JsMacrosClient.clientCore);
        this.screen = (IScreen) screen;
        this.screenName = JsMacrosClient.getScreenName(screen);
    }

    /**
     * Returns a diagnostic string containing the event name and captured screen name, in the
     * form {@code eventName:{"screenName": "name"}}. A null screen name is formatted as the
     * literal {@code null}; this is not JSON serialization and the screen itself is omitted.
     * @return the event name and captured screen name in diagnostic form
     */
    @Override
    public String toString() {
        return String.format("%s:{\"screenName\": \"%s\"}", this.getEventName(), screenName);
    }

}
