package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the client receives the data for a chunk from the server.<br>
 * The event is fired once per chunk the server sends, after the chunk data has been applied to
 * the client world, so any block in that chunk can be queried from the
 * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FWorld world} library at that point.<br>
 * This event is not cancellable, it only reports that a chunk arrived.
 * example:
 * <pre>
 * JsMacros.on("ChunkLoad", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`Chunk ${event.x}, ${event.z} loaded (full: ${event.isFull})`);
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("World")
@Event(value = "ChunkLoad", oldName = "CHUNK_LOAD")
public class EventChunkLoad extends BaseEvent {
    /**
     * the chunk's x coordinate, in chunk coordinates. Multiply by 16 for the block x coordinate.
     */
    public final int x;
    /**
     * the chunk's z coordinate, in chunk coordinates. Multiply by 16 for the block z coordinate.
     */
    public final int z;
    /**
     * whether the chunk was sent as a full chunk.<br>
     * Note: the client currently only fires this event for full chunk data, so this field is
     * always {@code true}.
     */
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
