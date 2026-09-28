package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the server tells the client to drop a chunk, so the chunk leaves the client world.<br>
 * This usually means the chunk went outside of the client's view distance, but the server can
 * also unload a chunk that is still in range.<br>
 * This event is not cancellable, it only reports that a chunk is going away.
 * example:
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
     * the chunk's x coordinate, in chunk coordinates. Multiply by 16 for the block x coordinate.
     */
    public final int x;
    /**
     * the chunk's z coordinate, in chunk coordinates. Multiply by 16 for the block z coordinate.
     */
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
