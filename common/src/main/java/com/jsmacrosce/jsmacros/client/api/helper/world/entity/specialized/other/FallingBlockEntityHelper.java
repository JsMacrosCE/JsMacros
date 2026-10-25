package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other;

import net.minecraft.world.entity.item.FallingBlockEntity;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * Represents a block moving as a falling entity, exposing its origin and block state.
 * <pre>
 * const blocks = World.getEntities(32, "falling_block");
 * if (blocks !== null) {
 *   for (const entity of blocks) {
 *     if (!(entity instanceof EntityHelpers.FallingBlock)) continue;
 *     // where it started falling
 *     const origin = entity.getOriginBlockPos();
 *     Chat.log(`${entity.getBlockState().getId()} from ${origin.getX()}, ${origin.getY()}, ${origin.getZ()}`);
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

    /** Wraps the supplied falling-block entity reference.
     * @param base the falling-block entity to wrap
     */
    public FallingBlockEntityHelper(FallingBlockEntity base) {
        super(base);
    }

    /**
     * Returns the block position recorded when the falling entity was created; this is distinct
     * from its current entity position.
     *
     * @return the recorded starting block position.
     * @since 1.8.4
     */
    public BlockPosHelper getOriginBlockPos() {
        return new BlockPosHelper(base.getStartPos());
    }

    /**
     * Returns the state represented by the falling entity, including its block properties.
     *
     * @return the entity's represented block state.
     * @since 1.8.4
     */
    public BlockStateHelper getBlockState() {
        return new BlockStateHelper(base.getBlockState());
    }

}
