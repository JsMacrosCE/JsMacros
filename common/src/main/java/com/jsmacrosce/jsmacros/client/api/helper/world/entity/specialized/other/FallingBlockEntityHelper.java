package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other;

import net.minecraft.world.entity.item.FallingBlockEntity;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class FallingBlockEntityHelper extends EntityHelper<FallingBlockEntity> {

    public FallingBlockEntityHelper(FallingBlockEntity base) {
        super(base);
    }

    /**
     * @return the block position this block is falling from.
     * @since 1.8.4
     */
    public BlockPosHelper getOriginBlockPos() {
        return new BlockPosHelper(base.getStartPos());
    }

    /**
     * @return the block state of this falling block.
     * @since 1.8.4
     */
    public BlockStateHelper getBlockState() {
        return new BlockStateHelper(base.getBlockState());
    }

}
