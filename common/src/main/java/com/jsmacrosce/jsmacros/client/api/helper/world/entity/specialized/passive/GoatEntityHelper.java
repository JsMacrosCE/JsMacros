package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.goat.Goat;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class GoatEntityHelper extends AnimalEntityHelper<Goat> {

    public GoatEntityHelper(Goat base) {
        super(base);
    }

    /**
     * @return {@code true} if this goat is currently screaming, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isScreaming() {
        return base.isScreamingGoat();
    }

    /**
     * @return {@code true} if this goat has its left horn still left, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasLeftHorn() {
        return base.hasLeftHorn();
    }

    /**
     * @return {@code true} if this goat has its right horn still left, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasRightHorn() {
        return base.hasRightHorn();
    }

    /**
     * @return {@code true} if this goat still has a horn, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasHorns() {
        return hasLeftHorn() || hasRightHorn();
    }

}
