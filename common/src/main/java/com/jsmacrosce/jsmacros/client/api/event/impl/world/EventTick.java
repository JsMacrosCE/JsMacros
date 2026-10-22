package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires once per client tick, at a fixed point in the tick after the key macro check, the tick
 * synchronizer and the server list pinger have run.<br>
 * It fires no matter what the client is doing, so a {@code null} player, a loaded world and the
 * title screen all produce ticks. Nothing in the world is guaranteed to have changed since the
 * previous tick, check the helpers you use for that.<br>
 * This event is not cancellable, and it is cheap to fire but not cheap to receive, so avoid
 * doing heavy work in the listener.
 * example:
 * <pre>
 * const listener = JsMacros.on("Tick", JavaWrapper.methodToJava(function () {
 *   // runs 20 times a second while the client is running
 * }));
 * // later
 * JsMacros.off(listener);
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("World")
@Event(value = "Tick", oldName = "TICK")
public class EventTick extends BaseEvent {
    public EventTick() {
        super(JsMacrosClient.clientCore);
    }

    @Override
    public String toString() {
        return String.format("%s:{}", this.getEventName());
    }

}
