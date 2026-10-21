package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Blaze;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * Provides helper access to a blaze. Its {@link #isOnFire()} method reports attack charge,
 * not burning.
 * <pre>
 * const BlazeEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.BlazeEntityHelper");
 * const blazes = World.getEntities(32, "blaze");
 * if (blazes !== null) {
 *   for (const entity of blazes) {
 *     const blaze = BlazeEntityHelper.class.cast(entity);
 *     // isOnFire reports the blaze's attack charge, not burning
 *     if (blaze.isOnFire()) {
 *       Chat.log(`a blaze at ${blaze.getPos()} is charged up`);
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
public class BlazeEntityHelper extends MobEntityHelper<Blaze> {

    /**
     * Wraps the supplied blaze and retains its reference for this helper.
     *
     * @param base blaze entity to wrap
     */
    public BlazeEntityHelper(Blaze base) {
        super(base);
    }

    /**
     * Returns whether the blaze's attack charge is set; this does not indicate whether it
     * is burning.
     *
     * @return {@code true} while the blaze is charged up to shoot, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isOnFire() {
        return base.isOnFire();
    }

}
