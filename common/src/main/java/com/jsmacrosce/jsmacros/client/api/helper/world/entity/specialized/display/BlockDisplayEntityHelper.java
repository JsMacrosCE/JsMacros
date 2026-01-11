package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display;

import net.minecraft.world.entity.Display;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;

/**
 * @author aMelonRind
 * @since 1.9.1
 */
@SuppressWarnings("unused")
public class BlockDisplayEntityHelper extends DisplayEntityHelper<Display.BlockDisplay> {

    public BlockDisplayEntityHelper(Display.BlockDisplay base) {
        super(base);
    }

    /**
     * @since 1.9.1
     */
    @Nullable
    public BlockStateHelper getBlockState() {
        Display.BlockDisplay.BlockRenderState data = base.blockRenderState();
        if (data == null) return null;
        return new BlockStateHelper(data.blockState());
    }

}
