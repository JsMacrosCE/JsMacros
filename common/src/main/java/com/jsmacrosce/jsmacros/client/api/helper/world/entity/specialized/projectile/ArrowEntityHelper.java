package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile;

import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
*///? } else {
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
//? }

/**
 * An arrow projectile, including ordinary, tipped, and spectral arrows.
 *
 * <pre>
 * const color = arrow.getColor();
 * if (color !== -1) Chat.log(color);
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class ArrowEntityHelper extends EntityHelper<AbstractArrow> {

    /**
     * Wraps the supplied arrow projectile and retains its reference for this helper.
     *
     * @param base the arrow projectile to wrap
     */
    public ArrowEntityHelper(AbstractArrow base) {
        super(base);
    }

    /**
     * Returns the potion color of a plain arrow. Other arrow types and plain arrows without a
     * potion color both return {@code -1}.
     *
     * @return the packed potion color, or {@code -1} when unavailable.
     * @since 1.8.4
     */
    public int getColor() {
        if (base instanceof Arrow) {
            return ((Arrow) base).getColor();
        }
        return -1;
    }

    /**
     * Returns whether the arrow is marked for critical-hit damage. Hitting a block clears this state.
     *
     * @return {@code true} if the critical-hit flag is set; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCritical() {
        return base.isCritArrow();
    }

    /**
     * Returns the arrow's piercing level. Hitting a block clears the level.
     *
     * @return the arrow's piercing level.
     * @since 1.8.4
     */
    public int getPiercingLevel() {
        return base.getPierceLevel();
    }

}
