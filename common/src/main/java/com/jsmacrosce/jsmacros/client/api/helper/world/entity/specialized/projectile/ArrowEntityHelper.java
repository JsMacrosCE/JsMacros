package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class ArrowEntityHelper extends EntityHelper<AbstractArrow> {

    public ArrowEntityHelper(AbstractArrow base) {
        super(base);
    }

    /**
     * @return the particle's color of the arrow, or {@code -1} if the arrow has no particles.
     * @since 1.8.4
     */
    public int getColor() {
        if (base instanceof Arrow) {
            return ((Arrow) base).getColor();
        }
        return -1;
    }

    /**
     * @return {@code true} if the arrow will deal critical damage, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCritical() {
        return base.isCritArrow();
    }

    /**
     * The piercing level will only be set if the arrow was fired from a crossbow with the piercing
     * enchantment.
     *
     * @return the piercing level of the arrow.
     * @since 1.8.4
     */
    public int getPiercingLevel() {
        return base.getPierceLevel();
    }

}
