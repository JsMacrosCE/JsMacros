package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires after a full chunk-data packet has updated the client world. This event is not cancellable;
 * {@code isFull} is always {@code true} on this event path.
 * <pre>
 * JsMacros.on("ChunkLoad", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`Chunk ${event.x}, ${event.z} loaded`);
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("World")
@Event(value = "ChunkLoad", oldName = "CHUNK_LOAD")
public class EventChunkLoad extends BaseEvent {
    /**
     * The chunk's x coordinate, in chunk coordinates.
     */
    public final int x;
    /**
     * The chunk's z coordinate, in chunk coordinates.
     */
    public final int z;
    /**
     * Whether the packet contained full chunk data; always {@code true} on this event path.
     */
    public final boolean isFull;

    /**
     * Stores the supplied chunk coordinates and full-data flag; this constructor does not load or modify a chunk.
     * @param x chunk x coordinate, in chunk coordinates
     * @param z chunk z coordinate, in chunk coordinates
     * @param isFull whether the supplied data represents a full chunk; {@code false} is stored unchanged
     */
    public EventChunkLoad(int x, int z, boolean isFull) {
        super(JsMacrosClient.clientCore);
        this.x = x;
        this.z = z;
        this.isFull = isFull;
    }

    /**
     * Returns a diagnostic string containing the event name and stored chunk coordinates.
     * The format is {@code %s:{"x": %d, "z": %d}}; {@code isFull} is omitted.
     * @return the formatted diagnostic string
     */
    @Override
    public String toString() {
        return String.format("%s:{\"x\": %d, \"z\": %d}", this.getEventName(), x, z);
    }

}
