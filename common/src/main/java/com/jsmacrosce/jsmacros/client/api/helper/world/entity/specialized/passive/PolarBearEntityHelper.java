package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.polarbear.PolarBear;
*///? } else {
import net.minecraft.world.entity.animal.PolarBear;
//?}

/**
 * the polar bear, which spends most of its life on all fours and rears up for the last
 * moment of an attack.
 * example:
 * <pre>
 * const PolarBearEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.PolarBearEntityHelper");
 * const bears = World.getEntities(32, "polar_bear");
 * if (bears !== null) {
 *   for (const entity of bears) {
 *     const bear = PolarBearEntityHelper.class.cast(entity);
 *     if (bear.isAttacking()) {
 *       Chat.log(`it is about to swipe at ${bear.getPos()}`);
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

    public PolarBearEntityHelper(PolarBear base) {
        super(base);
    }

    /**
     * Whether the bear has reared up, which it does only when it is about to swipe. The flag
     * goes up in the last stretch before the hit and comes straight down again once the
     * attack has landed or been called off, so it is a warning rather than a mood.
     * <p>
     * The bear's own size follows the flag, so a standing bear is genuinely taller in the
     * world and not just drawn that way.
     *
     * @return {@code true} if the polar bear is standing up to attack, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAttacking() {
        return base.isStanding();
    }

}
