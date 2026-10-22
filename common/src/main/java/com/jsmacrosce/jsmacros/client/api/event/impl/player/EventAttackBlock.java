package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;

import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockDataHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the local player starts breaking a block with the attack button.<br>
 * The event is only raised for breaks the client itself accepted, so clicking a block the player
 * is not allowed to break, or a position outside the world border, produces no event. The break
 * itself is not instant, the event marks the start of the break and the block is only removed
 * once the server confirms it.<br>
 * This event is not cancellable, the break has already been sent on to the server by the time
 * listeners see it.
 * example:
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
     * the block that is being broken, as the client currently sees it, including its block
     * entity if it has one. The position is available from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.BlockDataHelper#getX() getX()} and
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.BlockDataHelper#getY() getY()},
     * and the block's id from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.BlockDataHelper#getId() getId()}.
     */
    public final BlockDataHelper block;
    /**
     * the face of the block that was hit, as the numeric direction id the game uses:
     * {@code 0} down, {@code 1} up, {@code 2} north, {@code 3} south, {@code 4} west and
     * {@code 5} east.
     */
    @DocletReplaceReturn("Side")
    public final int side;

    public EventAttackBlock(BlockDataHelper block, int side) {
        super(JsMacrosClient.clientCore);
        this.block = block;
        this.side = side;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"block\": %s}", this.getEventName(), block);
    }

}
