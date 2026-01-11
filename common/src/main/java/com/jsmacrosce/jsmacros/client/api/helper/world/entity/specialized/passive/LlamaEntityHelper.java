package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.horse.Llama;
import com.jsmacrosce.doclet.DocletReplaceReturn;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class LlamaEntityHelper<T extends Llama> extends DonkeyEntityHelper<T> {

    public LlamaEntityHelper(T base) {
        super(base);
    }

    /**
     * @return the variant of this llama.
     * @since 1.8.4
     */
    @DocletReplaceReturn("LlamaVariant")
    public String getVariant() {
        return base.getVariant().getSerializedName();
    }

    /**
     * @return the strength of this llama.
     * @since 1.8.4
     */
    public int getStrength() {
        return base.getStrength();
    }

    /**
     * @return {@code true} if this llama belongs to a wandering trader, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isTraderLlama() {
        return base.isTraderLlama();
    }

}
