package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;

import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockDataHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the client's {@code startDestroyBlock} call returns {@code true} for a block
 * target. This does not guarantee a new break request was sent or that the block was destroyed.
 * The event is not cancellable.
 * <pre>
 * JsMacros.on("AttackBlock", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`Breaking ${event.block.getId()} at ${event.block.getX()}, ${event.block.getY()}, ${event.block.getZ()}`);
 * }))
 * </pre>
 */
@DocletCategory("Inputs/Interactions")
@Event("AttackBlock")
public class EventAttackBlock extends BaseEvent {
    /**
     * The block at the target position, using the client's current block state and block entity.
     */
    public final BlockDataHelper block;
    /**
     * The hit face as the direction's 3D data value:
     * {@code 0} down, {@code 1} up, {@code 2} north, {@code 3} south, {@code 4} west and
     * {@code 5} east.
     */
    @DocletReplaceReturn("Side")
    public final int side;

    /**
     * Initializes the event with the client's core and stores the supplied block helper and side value unchanged.
     * This does not break, destroy, or update the world, or dispatch the event.
     * @param block the supplied helper for the target block
     * @param side the provided hit-face code
     */
    public EventAttackBlock(BlockDataHelper block, int side) {
        super(JsMacrosClient.clientCore);
        this.block = block;
        this.side = side;
    }

    /**
     * Returns a diagnostic string containing the event name and stored block-helper representation.
     * The format is {@code %s:{"block": %s}}; it omits {@code side} and is not JSON.
     * @return the event name and stored block-helper representation in the diagnostic format
     */
    @Override
    public String toString() {
        return String.format("%s:{\"block\": %s}", this.getEventName(), block);
    }

}
