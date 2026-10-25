package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other;

import net.minecraft.world.entity.item.PrimedTnt;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * Represents primed TNT; its fuse can be read with {@link #getRemainingTime()}.
 * <p>
 * <pre>
 * const bombs = World.getEntities(32, "tnt");
 * if (bombs !== null) {
 *   for (const entity of bombs) {
 *     if (!(entity instanceof EntityHelpers.Tnt)) continue;
 *     // it has already been lit, so the only question is how long is left
 *     Chat.log(`goes off in ${(entity.getRemainingTime() / 20).toFixed(1)} seconds`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class TntEntityHelper extends EntityHelper<PrimedTnt> {

    /**
     * Wraps the supplied primed TNT entity, retaining its reference.
     *
     * @param base primed TNT entity to wrap
     */
    public TntEntityHelper(PrimedTnt base) {
        super(base);
    }

    /**
     * Returns the primed TNT entity's fuse in ticks. Newly primed TNT normally starts at 80;
     * each tick decrements the fuse, and at zero the entity is discarded and explodes server-side.
     *
     * @return the current fuse value in ticks
     * @since 1.8.4
     */
    public int getRemainingTime() {
        return base.getFuse();
    }

}
