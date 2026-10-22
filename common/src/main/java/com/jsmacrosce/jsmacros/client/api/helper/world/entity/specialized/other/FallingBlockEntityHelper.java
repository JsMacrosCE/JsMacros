package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other;

import net.minecraft.world.entity.item.FallingBlockEntity;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * a falling block, the entity a sand, gravel or an anvil becomes while it is falling.
 * <p>
 * A falling block is two things at once: it is falling, so it has a position that is changing,
 * and it is a block, so it has a block state it will put back into the world when it lands. The
 * two calls here are one of each. The entity is not there permanently: when it lands it either
 * places itself and is gone, or breaks and drops an item, so a script watching one has a short
 * window to look at it.
 * example:
 * <pre>
 * const FallingBlockEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other.FallingBlockEntityHelper");
 * const falling = World.getEntities(32, "falling_block");
 * if (falling !== null) {
 *   for (const entity of falling) {
 *     const block = FallingBlockEntityHelper.class.cast(entity);
 *     // where it came from, which is not where it is now
 *     const origin = block.getOriginBlockPos();
 *     Chat.log(`${block.getBlockState().getId()} from ${origin.getX()}, ${origin.getY()}, ${origin.getZ()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class FallingBlockEntityHelper extends EntityHelper<FallingBlockEntity> {

    public FallingBlockEntityHelper(FallingBlockEntity base) {
        super(base);
    }

    /**
     * the block position this block is falling from, which is where it was taken from rather
     * than where it is now.
     * <p>
     * This is the position the block was at when the entity spawned, so on a block that has
     * been falling for a while it is a long way above the entity's current position. It is
     * also the block the entity will try to occupy when it lands, which is why a script
     * watching a falling block usually wants this rather than {@link #getPos() getPos()}.
     * example:
     * <pre>
     * const FallingBlockEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other.FallingBlockEntityHelper");
     * const falling = World.getEntities(32, "falling_block");
     * if (falling !== null) {
     *   for (const entity of falling) {
     *     const block = FallingBlockEntityHelper.class.cast(entity);
     *     // the block it came from, which is where it is going back to
     *     Chat.log(`falling from ${block.getOriginBlockPos()}`);
     *   }
     * }
     * </pre>
     *
     * @return the block position this block is falling from.
     * @since 1.8.4
     */
    public BlockPosHelper getOriginBlockPos() {
        return new BlockPosHelper(base.getStartPos());
    }

    /**
     * the block state of this falling block, which is the block it is imitating while it falls
     * and the block it becomes when it lands.
     * <p>
     * A state rather than an id, so for a block such as a chest the properties come with it.
     * The waterlogged flag is cleared when the entity is created, so a falling block that
     * started life in water reports the block without it. This is the block as it is falling
     * and it does not change while the entity exists.
     * example:
     * <pre>
     * const FallingBlockEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other.FallingBlockEntityHelper");
     * const falling = World.getEntities(32, "falling_block");
     * if (falling !== null) {
     *   for (const entity of falling) {
     *     const block = FallingBlockEntityHelper.class.cast(entity);
     *     // the state it will place, not just the block id
     *     Chat.log(`a ${block.getBlockState().getId()} on its way down`);
     *   }
     * }
     * </pre>
     *
     * @return the block state of this falling block.
     * @since 1.8.4
     */
    public BlockStateHelper getBlockState() {
        return new BlockStateHelper(base.getBlockState());
    }

}
