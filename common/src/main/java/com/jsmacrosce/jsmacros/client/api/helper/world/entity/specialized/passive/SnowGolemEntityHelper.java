package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.golem.SnowGolem;
*///? } else {
import net.minecraft.world.entity.animal.SnowGolem;
//? }

/**
 * Provides access to a snow golem's pumpkin and shearing state.
 * <pre>
 * const golems = World.getEntities(32, "snow_golem");
 * if (golems !== null) {
 *   for (const entity of golems) {
 *     if (!(entity instanceof EntityHelpers.SnowGolem)) continue;
 *     Chat.log(`pumpkin ${entity.hasPumpkin()}, shearable ${entity.isShearable()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class SnowGolemEntityHelper extends MobEntityHelper<SnowGolem> {

    /**
     * Wraps the supplied snow golem and retains its reference for this helper.
     *
     * @param base snow golem entity to wrap
     */
    public SnowGolemEntityHelper(SnowGolem base) {
        super(base);
    }

    /**
     * Returns whether the golem has its pumpkin equipped.
     *
     * @return {@code true} if the snow golem has a pumpkin on its head, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasPumpkin() {
        return base.hasPumpkin();
    }

    /**
     * Returns whether the golem is alive and has its pumpkin equipped.
     *
     * @return {@code true} if the golem is alive and has its pumpkin equipped; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isShearable() {
        return base.readyForShearing();
    }

}
