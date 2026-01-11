package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.wolf.Wolf;
import com.jsmacrosce.jsmacros.client.api.helper.DyeColorHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class WolfEntityHelper extends TameableEntityHelper<Wolf> {

    public WolfEntityHelper(Wolf base) {
        super(base);
    }

    /**
     * @return {@code true} if this wolf is tamed and the player has either a bone or meat in one of
     * their hands, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isBegging() {
        return base.isInterested();
    }

    /**
     * @return the color of this wolf's collar.
     * @since 1.8.4
     */
    public DyeColorHelper getCollarColor() {
        return new DyeColorHelper(base.getCollarColor());
    }

    /**
     * @return {@code true} if this wolf is angry, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAngry() {
        return base.isAngry();
    }

    /**
     * @return {@code true} if this wolf is wet, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isWet() {
        return base.isWet;
    }

}
