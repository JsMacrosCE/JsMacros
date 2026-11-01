package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires after the client processes a server packet to forget a chunk.
 * <pre>
 * JsMacros.on("ChunkUnload", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`Chunk ${event.x}, ${event.z} unloaded`);
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("World")
@Event(value = "ChunkUnload", oldName = "CHUNK_UNLOAD")
public class EventChunkUnload extends BaseEvent {
    /**
     * The chunk's x coordinate, in chunk coordinates.
     */
    public final int x;
    /**
     * The chunk's z coordinate, in chunk coordinates.
     */
    public final int z;

    /**
     * Stores the coordinates for this event; constructing it does not unload a chunk or modify the world.
     * @param x x chunk coordinate
     * @param z z chunk coordinate
     */
    public EventChunkUnload(int x, int z) {
        super(JsMacrosClient.clientCore);
        this.x = x;
        this.z = z;
    }

    /**
     * Returns a diagnostic string containing the event name and stored coordinates in the form {@code %s:{"x": %d, "z": %d}}.
     * @return the diagnostic string with the event name and decimal x and z values
     */
    @Override
    public String toString() {
        return String.format("%s:{\"x\": %d, \"z\": %d}", this.getEventName(), x, z);
    }

}
