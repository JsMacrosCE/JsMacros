package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.equine.Llama;
*///? } else {
import net.minecraft.world.entity.animal.horse.Llama;
 //?}

/**
 * Provides llama coat, strength, and trader status alongside the chested-horse APIs inherited from {@link DonkeyEntityHelper}.
 * <pre>
 * const llamas = World.getEntities(32, "llama", "trader_llama");
 * if (llamas !== null) {
 *   for (const entity of llamas) {
 *     if (!(entity instanceof EntityHelpers.Llama)) continue;
 *     Chat.log(`${entity.getType()}: ${entity.getVariant()}, `
 *       + `strength ${entity.getStrength()}, `
 *       + `${entity.hasChest()} chest, `
 *       + `${entity.getInventorySize()} slots`);
 *   }
 * }
 * </pre>
 * @param <T> the llama subtype wrapped by this helper
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class LlamaEntityHelper<T extends Llama> extends DonkeyEntityHelper<T> {

    /**
     * Wraps the supplied llama and retains its reference for this helper.
     *
     * @param base the llama entity to wrap
     */
    public LlamaEntityHelper(T base) {
        super(base);
    }

    /**
     * Returns this llama's coat variant name (such as {@code creamy} or {@code white}), without a namespace.
     *
     * @return the serialized name of this llama's coat variant.
     * @since 1.8.4
     */
    @DocletReplaceReturn("LlamaVariant")
    public String getVariant() {
        return base.getVariant().getSerializedName();
    }

    /**
     * Returns the llama's strength, from one to five; when it has a chest, this value determines its inventory column count.
     *
     * @return the llama's strength.
     * @since 1.8.4
     */
    public int getStrength() {
        return base.getStrength();
    }

    /**
     * Returns whether this is a trader llama.
     *
     * @return {@code true} for a trader llama; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isTraderLlama() {
        return base.isTraderLlama();
    }

}
