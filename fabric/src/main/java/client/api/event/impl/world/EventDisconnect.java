package xyz.wagyourtail.jsmacros.client.api.event.impl.world;

import net.minecraft.network.chat.Component;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.client.api.helper.TextHelper;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.event.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "Disconnect", oldName = "DISCONNECT")
public class EventDisconnect extends BaseEvent {
    /**
     * @since 1.6.4
     */
    public final TextHelper message;

    public EventDisconnect(Component message) {
        super(JsMacrosClient.clientCore);
        this.message = TextHelper.wrap(message);
    }

    @Override
    public String toString() {
        return String.format("%s:{}", this.getEventName());
    }

}
