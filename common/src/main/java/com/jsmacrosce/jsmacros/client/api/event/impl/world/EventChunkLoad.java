package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "ChunkLoad", oldName = "CHUNK_LOAD")
public class EventChunkLoad extends BaseEvent {
    public final int x;
    public final int z;
    public final boolean isFull;

    public EventChunkLoad(int x, int z, boolean isFull) {
        super(JsMacrosClient.clientCore);
        this.x = x;
        this.z = z;
        this.isFull = isFull;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"x\": %d, \"z\": %d}", this.getEventName(), x, z);
    }

}
