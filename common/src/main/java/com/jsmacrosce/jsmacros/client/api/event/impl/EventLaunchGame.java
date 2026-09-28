package com.jsmacrosce.jsmacros.client.api.event.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires exactly once, at the very head of the client's main loop, before the splash screen has
 * finished and long before there is a world, a player or any loaded resources. The window already
 * exists at this point and the first resource reload is already under way.<br>
 * Because it is the first thing that happens, this is the earliest event a script can react to, and
 * the only one that reports the account name without a server having been joined.
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.world.EventJoinServer} reports the same name
 * again much later, through the local player it carries.<br>
 * This event is not cancellable, and there is nothing to cancel, since it is raised before the
 * game has started doing anything. It is also not one of the events a {@code Joined} trigger can be
 * attached to, because those only work on cancellable or explicitly joinable events. Anything
 * that needs the world, the player or the loaded resources has to wait for
 * {@link com.jsmacrosce.jsmacros.client.api.event.impl.EventResourcePackLoaded} or later instead.
 * example:
 * <pre>
 * JsMacros.on("LaunchGame", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`client starting as ${event.playerName}`);
 * }))
 * </pre>
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("System/Lifecycle")
@Event(value = "LaunchGame")
public class EventLaunchGame extends BaseEvent {

    /**
     * the name of the account the client is running under, which is the name the session was set up
     * with. On an offline or a launcher session that is whatever name was entered rather than an
     * authenticated account.<br>
     * It is read once, as the client's run starts, so it is not a name the player can change later
     * in the session without restarting the game.
     */
    public final String playerName;

    public EventLaunchGame(String playerName) {
        super(JsMacrosClient.clientCore);
        this.playerName = playerName;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"name\": \"%s\"}", this.getEventName(), playerName);
    }

}
