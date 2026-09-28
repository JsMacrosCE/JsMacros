package com.jsmacrosce.jsmacros.client.api.event.impl.player;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockDataHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the local player right clicks a block, for example to open a chest, place a block or
 * press a button.<br>
 * The event fires after the game has processed the click, and only if the click was not rejected
 * outright, so clicking a block that cannot be interacted with produces no event.<br>
 * {@link #result} tells the two useful cases apart. It is {@code true} when the block actually
 * did something, and {@code false} when the click was only passed along the chain, which is
 * what happens when sneaking past a block to use the item in hand instead, or when the block
 * defers the interaction to the item.<br>
 * This event is not cancellable, the interaction has already been sent on to the server by the
 * time listeners see it. For a client side block place or break of your own, use
 * {@link com.jsmacrosce.jsmacros.client.api.helper.InteractionManagerHelper} instead.
 * example:
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
     * {@code true} if the block actually reacted to the click, {@code false} if the interaction
     * was only passed along the chain without the block doing anything. The event does not fire
     * at all when the click was rejected outright, so {@code false} means "passed on", not
     * "failed".
     */
    public final boolean result;
    /**
     * the block that was clicked, as the client currently sees it, including its block entity if
     * it has one. The position is available from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.BlockDataHelper#getX() getX()} and
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.BlockDataHelper#getY() getY()},
     * and the block's id from
     * {@link com.jsmacrosce.jsmacros.client.api.helper.world.BlockDataHelper#getId() getId()}.
     */
    public final BlockDataHelper block;
    /**
     * the face of the block that was clicked, as the numeric direction id the game uses:
     * {@code 0} down, {@code 1} up, {@code 2} north, {@code 3} south, {@code 4} west and
     * {@code 5} east.
     */
    @DocletReplaceReturn("Side")
    public final int side;

    public EventInteractBlock(boolean offhand, boolean accepted, BlockDataHelper block, int side) {
        super(JsMacrosClient.clientCore);
        this.offhand = offhand;
        this.result = accepted;
        this.block = block;
        this.side = side;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"block\": %s, \"result\": \"%s\"}", this.getEventName(), block, result);
    }

}
