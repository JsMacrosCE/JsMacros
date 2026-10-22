package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockDataHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires after a local block-use attempt returns, unless the result is {@code FAIL}. The event is
 * not cancellable. {@link #result} is true when the interaction consumes an action; false means
 * it was passed along without consuming one, not that it failed.
 * <pre>
 * JsMacros.on("InteractBlock", JavaWrapper.methodToJava(function (event) {
 *   if (event.result) {
 *     Chat.log(`${event.offhand ? "Off hand" : "Main hand"} used on ${event.block.getId()}`);
 *   }
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.8.0
 */
@DocletCategory("Inputs/Interactions")
@Event("InteractBlock")
public class EventInteractBlock extends BaseEvent {
    /**
     * {@code true} if the click was made with the off hand, {@code false} if it was made with the
     * main hand.
     */
    public final boolean offhand;
    /**
     * {@code true} when the interaction consumes an action; {@code false} when it passes without
     * consuming one. This event is not fired for a {@code FAIL} result.
     */
    public final boolean result;
    /**
     * The clicked block as seen by the client, including its block entity when present.
     */
    public final BlockDataHelper block;
    /**
     * The clicked face's numeric direction id:
     * {@code 0} down, {@code 1} up, {@code 2} north, {@code 3} south, {@code 4} west and
     * {@code 5} east.
     */
    @DocletReplaceReturn("Side")
    public final int side;

    /**
     * Stores the supplied block-interaction data and initializes the event with the client core.
     * @param offhand whether the supplied hand flag identifies the off hand; {@code false} identifies the main hand
     * @param accepted supplied action-consumption flag, stored as {@code result}
     * @param block the supplied block helper reference
     * @param side the supplied hit-face code
     */
    public EventInteractBlock(boolean offhand, boolean accepted, BlockDataHelper block, int side) {
        super(JsMacrosClient.clientCore);
        this.offhand = offhand;
        this.result = accepted;
        this.block = block;
        this.side = side;
    }

    /**
     * Returns this event's formatted string representation.
     * The payload has the form {@code eventName:{"block": helper, "result": "boolean"}}; the
     * boolean value is quoted, and the offhand and side values are omitted. This is not JSON.
     * @return the event-name-prefixed representation containing the block helper and stored result
     */
    @Override
    public String toString() {
        return String.format("%s:{\"block\": %s, \"result\": \"%s\"}", this.getEventName(), block, result);
    }

}
