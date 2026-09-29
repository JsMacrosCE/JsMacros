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
 * a furnace minecart, the minecart that smelts whatever is in it as it goes.
 * <p>
 * The single call here is whether the furnace is lit, which is the whole question about a
 * furnace minecart: the minecart is what moves and the furnace state is what says whether it is
 * doing anything. Nothing on this class sets it, so a script that wants a minecart going has to
 * put fuel in it.
 * example:
 * <pre>
 * const FurnaceMinecartEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle.FurnaceMinecartEntityHelper");
 * const carts = World.getEntities(32, "furnace_minecart");
 * if (carts !== null) {
 *   for (const entity of carts) {
 *     const cart = FurnaceMinecartEntityHelper.class.cast(entity);
 *     if (cart.isPowered()) {
 *       Chat.log(`smelting as it goes, at ${entity.getPos()}`);
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class FurnaceMinecartEntityHelper extends EntityHelper<MinecartFurnace> {

    public FurnaceMinecartEntityHelper(MinecartFurnace base) {
        super(base);
    }

    /**
     * <p>
          * Powered means the minecart's own furnace is lit, which is what makes it smelt what is in
     * it, and it is read from the lit flag on the block state the minecart is drawn with. It is
     * a question about the furnace rather than about the minecart: a lit furnace minecart
     * standing still answers {@code true} and a moving one with a cold furnace answers
     * {@code false}. It follows the minecart's own furnace and not any furnace block in the
     * world, so a lit furnace next to it changes nothing.
     * example:
     * <pre>
     * const FurnaceMinecartEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle.FurnaceMinecartEntityHelper");
     * const carts = World.getEntities(32, "furnace_minecart");
     * if (carts !== null) {
     *   for (const entity of carts) {
     *     const cart = FurnaceMinecartEntityHelper.class.cast(entity);
     *     // lit means smelting, which is not the same as moving
     *     if (cart.isPowered()) {
     *       Chat.log(`moving at ${cart.getSpeed()} blocks a tick, and smelting`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the furnace minecart is powered, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPowered() {
        return base.getDisplayBlockState().getValue(BlockStateProperties.LIT);
    }

}
