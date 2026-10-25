package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
*///? } else {
import net.minecraft.world.entity.projectile.WitherSkull;
//? }

/**
 * A wither skull projectile, with access to its dangerous flag.
 * <pre>
 * const skulls = World.getEntities(32, "wither_skull");
 * if (skulls !== null) {
 *   for (const entity of skulls) {
 *     if (!(entity instanceof EntityHelpers.WitherSkull)) continue;
 *     if (entity.isCharged()) {
 *       Chat.log("dangerous wither skull");
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
public class WitherSkullEntityHelper extends EntityHelper<WitherSkull> {

    /**
     * Wraps the supplied wither skull and retains its reference for this helper.
     * @param base wither skull projectile to wrap
     */
    public WitherSkullEntityHelper(WitherSkull base) {
        super(base);
    }

    /**
     * Returns whether this skull has its dangerous flag set. For blocks the Wither can
     * destroy, this flag caps explosion resistance at {@code 0.8}; it also changes the
     * skull's inertia to {@code 0.73}.
     *
     * @return {@code true} if the wither skull is charged, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCharged() {
        return base.isDangerous();
    }

}
