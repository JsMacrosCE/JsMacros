package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.Bee;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class BeeEntityHelper extends AnimalEntityHelper<Bee> {

    public BeeEntityHelper(Bee base) {
        super(base);
    }

    /**
     * @return {@code true} if the bee has nectar, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasNectar() {
        return base.hasNectar();
    }

    /**
     * @return {@code true} if the bee is angry at a player, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAngry() {
        return base.isAngry();
    }

    /**
     * @return {@code true} if the bee has already stung a player, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasStung() {
        return base.hasStung();
    }

}
