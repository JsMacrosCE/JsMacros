package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.Rabbit;
import com.jsmacrosce.doclet.DocletReplaceReturn;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class RabbitEntityHelper extends AnimalEntityHelper<Rabbit> {

    public RabbitEntityHelper(Rabbit base) {
        super(base);
    }

    /**
     * @return the variant of this rabbit.
     * @since 1.8.4
     */
    @DocletReplaceReturn("RabbitVariant")
    public String getVariant() {
        return base.getVariant().getSerializedName();
    }

    /**
     * @return {@code true} if this rabbit is a killer bunny, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isKillerBunny() {
        return base.getVariant() == Rabbit.Variant.EVIL;
    }

}
