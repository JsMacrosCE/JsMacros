package com.jsmacrosce.jsmacros.client.api.event.impl.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.event.filterer.FiltererBlockUpdate;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockDataHelper;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Fires when the client receives a block update from the server.<br>
 * The same event is used for three different server packets, which are told apart by
 * {@link #updateType}: a single block state change ({@code 'STATE'}), a batch of section block
 * changes (also {@code 'STATE'}, fired once for every changed block in the batch), and a block
 * entity data packet ({@code 'ENTITY'}).<br>
 * This event is not cancellable, it only reports what the server sent.<br>
 * Listeners can be narrowed down with a
 * {@link com.jsmacrosce.jsmacros.client.api.event.filterer.FiltererBlockUpdate}, which can match
 * on position, area, block id, block states and update type.
 * example:
 * <pre>
 * JsMacros.on("BlockUpdate", JavaWrapper.methodToJava(function (event) {
 *   const pos = event.block.getBlockPos();
 *   if (event.updateType === "ENTITY") {
 *     Chat.log(`BlockEntity data at ${pos.getX()}, ${pos.getY()}, ${pos.getZ()}`);
 *   } else {
 *     Chat.log(`Block became ${event.block.getId()} at ${pos.getX()}, ${pos.getY()}, ${pos.getZ()}`);
 *   }
 * }))
 * </pre>
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("World")
@Event(value = "BlockUpdate", oldName = "BLOCK_UPDATE", filterer = FiltererBlockUpdate.class)
public class EventBlockUpdate extends BaseEvent {
    /**
     * the block that was updated, exposes the block's position, id, name, state properties and,
     * for {@code 'ENTITY'} updates, the block entity's NBT data. Read the position from
     * {@link BlockDataHelper#getBlockPos() getBlockPos()}, the id from
     * {@link BlockDataHelper#getId() getId()}, the NBT from
     * {@link BlockDataHelper#getNBT() getNBT()} and the state from
     * {@link BlockDataHelper#getBlockState() getBlockState()}.
     */
    public final BlockDataHelper block;
    /**
     * what kind of update triggered this event.<br>
     * {@code 'STATE'} for a block state change, {@code 'ENTITY'} for a block entity data update.
     */
    @DocletReplaceReturn("BlockUpdateType")
    @DocletDeclareType(name = "BlockUpdateType", type = "'STATE' | 'ENTITY'")
    public final String updateType;

    public EventBlockUpdate(BlockState block, BlockEntity blockEntity, BlockPos blockPos, String updateType) {
        super(JsMacrosClient.clientCore);
        this.block = new BlockDataHelper(block, blockEntity, blockPos);
        this.updateType = updateType;
    }

    @Override
    public String toString() {
        return String.format("%s:{\"block\": %s}", this.getEventName(), block);
    }

}
