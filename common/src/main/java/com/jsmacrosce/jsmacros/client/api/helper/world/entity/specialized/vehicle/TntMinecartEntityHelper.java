package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

//?  if >=1.21.11 {
/*import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
*///? } else {
import net.minecraft.world.entity.vehicle.MinecartTNT;
//? }

/**
 * Provides fuse and primed-state queries for TNT minecarts.
 * <pre>
 * const carts = World.getEntities(32, "tnt_minecart");
 * if (carts !== null) {
 *   for (const entity of carts) {
 *     if (!(entity instanceof EntityHelpers.TntMinecart)) continue;
 *     if (entity.isPrimed()) {
 *       Chat.log(`it is lit, and goes off in ${entity.getRemainingTime()} ticks`);
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
public class TntMinecartEntityHelper extends EntityHelper<MinecartTNT> {

    /**
     * Wraps the supplied TNT minecart and retains a reference to it.
     * @param base the TNT minecart entity to wrap
     */
    public TntMinecartEntityHelper(MinecartTNT base) {
        super(base);
    }

    /**
     * Returns the remaining fuse time in ticks, or {@code -1} if the minecart is not primed.
     *
     * @return the remaining fuse time in ticks, or {@code -1} if the minecart is not primed.
     * @since 1.8.4
     */
    public int getRemainingTime() {
        return base.getFuse();
    }

    /**
     * Returns whether the TNT minecart is primed.
     *
     * @return {@code true} if the minecart is primed; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPrimed() {
        return base.isPrimed();
    }

}
