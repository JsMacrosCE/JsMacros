package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.monster.illager.Vindicator;
*///? } else {
import net.minecraft.world.entity.monster.Vindicator;
//? }

/**
 * Provides access to vindicator-specific helpers, including a custom-name-based Johnny check.
 * <p>
 * {@link #isJohnny() isJohnny} checks the custom name, not the game's separate Johnny flag.
 * <pre>
 * const vindicators = World.getEntities(32, "vindicator");
 * if (vindicators !== null) {
 *   for (const entity of vindicators) {
 *     if (!(entity instanceof EntityHelpers.Vindicator)) continue;
 *     if (entity.isJohnny()) {
 *       // this is the name check, so the name is worth reading back
 *       Chat.log(`Johnny at ${entity.getPos()}: ${entity.getName().getString()}`);
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
public class VindicatorEntityHelper extends IllagerEntityHelper<Vindicator> {

    /** Wraps the supplied vindicator and retains its reference for this helper.
     * @param base vindicator entity to wrap
     */
    public VindicatorEntityHelper(Vindicator base) {
        super(base);
    }

    /**
     * Returns whether the vindicator has a custom name whose plain text is exactly
     * {@code Johnny}. This does not read the game's separate Johnny flag.
     *
     * @return {@code true} when the custom name matches; {@code false} when it does not or is absent.
     * @since 1.8.4
     */
    public boolean isJohnny() {
        return base.hasCustomName() && base.getCustomName().getString().equals("Johnny");
    }

}
