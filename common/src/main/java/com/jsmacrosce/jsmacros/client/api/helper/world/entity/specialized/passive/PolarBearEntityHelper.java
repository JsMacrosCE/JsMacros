package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.polarbear.PolarBear;
*///? } else {
import net.minecraft.world.entity.animal.PolarBear;
//?}

/**
 * Provides access to a polar bear's standing state.
 * <pre>
 * const bears = World.getEntities(32, "polar_bear");
 * if (bears !== null) {
 *   for (const entity of bears) {
 *     if (!(entity instanceof EntityHelpers.PolarBear)) continue;
 *     if (entity.isAttacking()) {
 *       Chat.log(`a standing polar bear at ${entity.getPos()}`);
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
public class PolarBearEntityHelper extends AnimalEntityHelper<PolarBear> {

    /**
     * Wraps the supplied polar bear and retains its reference in this helper.
     *
     * @param base the polar bear entity to wrap
     */
    public PolarBearEntityHelper(PolarBear base) {
        super(base);
    }

    /**
     * Returns whether the polar bear is standing.
     *
     * @return {@code true} while the polar bear's standing state is set; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAttacking() {
        return base.isStanding();
    }

}
