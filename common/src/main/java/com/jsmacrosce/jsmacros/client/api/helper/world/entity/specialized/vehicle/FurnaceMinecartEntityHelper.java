package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle;

import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class FurnaceMinecartEntityHelper extends EntityHelper<MinecartFurnace> {

    public FurnaceMinecartEntityHelper(MinecartFurnace base) {
        super(base);
    }

    /**
     * @return {@code} true if the furnace minecart is powered, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPowered() {
        return base.getDisplayBlockState().getValue(BlockStateProperties.LIT);
    }

}
