package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle;

import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

//?  if >=1.21.11 {
/*import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
*///? } else {
import net.minecraft.world.entity.vehicle.MinecartFurnace;
//? }

/**
 * Provides access to the furnace minecart's displayed furnace lit state.
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class FurnaceMinecartEntityHelper extends EntityHelper<MinecartFurnace> {

    /**
     * Wraps the supplied furnace minecart and retains its reference for this helper.
     *
     * @param base furnace minecart entity to wrap
     */
    public FurnaceMinecartEntityHelper(MinecartFurnace base) {
        super(base);
    }

    /**
     * Returns whether the furnace minecart's displayed furnace block state is lit (indicating fuel, not movement).
     *
     * @return {@code true} when the displayed furnace is lit; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPowered() {
        return base.getDisplayBlockState().getValue(BlockStateProperties.LIT);
    }

}
