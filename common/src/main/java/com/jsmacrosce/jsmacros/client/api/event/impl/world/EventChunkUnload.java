package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "ChunkUnload", oldName = "CHUNK_UNLOAD")
public class EventChunkUnload extends BaseEvent {
    public final int x;
    public final int z;

    public EventChunkUnload(int x, int z) {
        super(JsMacrosClient.clientCore);
        this.x = x;
        this.z = z;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"x\": %d, \"z\": %d}", this.getEventName(), x, z);
    }

}
