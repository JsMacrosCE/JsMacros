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
 * Fires after a block-state or block-entity update packet is handled. Section updates produce one
 * {@code 'STATE'} event per block; block-entity data packets produce {@code 'ENTITY'} events.
 * Listeners can be narrowed down with a
 * {@link com.jsmacrosce.jsmacros.client.api.event.filterer.FiltererBlockUpdate}, which can match
 * on position, area, block id, block states and update type.
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
     * Snapshot of the block state, position, and optional block entity associated with this update.
     * For {@code 'ENTITY'} updates, it also exposes block-entity NBT data. Read the position from
     * {@link BlockDataHelper#getBlockPos() getBlockPos()}, the id from
     * {@link BlockDataHelper#getId() getId()}, the NBT from
     * {@link BlockDataHelper#getNBT() getNBT()} and the state from
     * {@link BlockDataHelper#getBlockState() getBlockState()}.
     */
    public final BlockDataHelper block;
    /**
     * Kind of update that triggered this event:
     * {@code 'STATE'} for a block state change, {@code 'ENTITY'} for a block entity data update.
     */
    @DocletReplaceReturn("BlockUpdateType")
    @DocletDeclareType(name = "BlockUpdateType", type = "'STATE' | 'ENTITY'")
    public final String updateType;

    /**
     * Initializes the event with a helper wrapping the supplied block state and the supplied update label.
     * The block state must be non-null; a null {@code block} causes {@link NullPointerException} during
     * construction. The entity and position are retained as supplied without validation or copying;
     * a null position is accepted here but causes {@link NullPointerException} when position-dependent
     * access or {@link #toString()} is used. The update label is stored unchanged and is not restricted
     * to {@code "STATE"} or {@code "ENTITY"}.
     *
     * @param block the block state to wrap; must not be {@code null}
     * @param blockEntity the associated block entity, retained as supplied and possibly {@code null}
     * @param blockPos the associated position, retained as supplied and possibly {@code null}
     * @param updateType the update label, retained unchanged and possibly {@code null}
     */
    public EventBlockUpdate(BlockState block, BlockEntity blockEntity, BlockPos blockPos, String updateType) {
        super(JsMacrosClient.clientCore);
        this.block = new BlockDataHelper(block, blockEntity, blockPos);
        this.updateType = updateType;
    }

    /**
     * Returns a diagnostic string containing the event name and the block helper's string form.
     * Formatting dereferences the stored position; if it is {@code null}, this method throws
     * {@link NullPointerException}.
     *
     * @return the event name and block helper formatted as {@code %s:{"block": %s}}
     */
    @Override
    public String toString() {
        return String.format("%s:{\"block\": %s}", this.getEventName(), block);
    }

}
