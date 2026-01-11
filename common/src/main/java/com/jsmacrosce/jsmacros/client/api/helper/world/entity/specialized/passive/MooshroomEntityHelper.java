package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.MushroomCow;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class MooshroomEntityHelper extends AnimalEntityHelper<MushroomCow> {

    public MooshroomEntityHelper(MushroomCow base) {
        super(base);
    }

    /**
     * @return {@code true} if this mooshroom can be sheared, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isShearable() {
        return base.readyForShearing();
    }

    /**
     * @return {@code true} if this mooshroom is a red mooshroom, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isRed() {
        return base.getVariant() == MushroomCow.Variant.RED;
    }

    /**
     * @return {@code true} if this mooshroom is a brown mooshroom, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isBrown() {
        return base.getVariant() == MushroomCow.Variant.BROWN;
    }

}
