package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.equine.Llama;
*///? } else {
import net.minecraft.world.entity.animal.horse.Llama;
 //?}

/**
 * the llama, which is a chested horse as far as this class tree is concerned: everything an
 * {@link AbstractHorseEntityHelper} has, the chest from {@link DonkeyEntityHelper}, and then
 * a coat, a strength, and the fact that a trader llama is not really a llama at all.
 * <p>
 * The strength is not a stat a script sets and is not about how hard the llama hits. It is
 * the width of its chest: a llama with a chest carries one row per point, so a strength of
 * one gives three slots and a strength of five gives fifteen. The game draws an ordinary
 * llama's from the lower end of that range with an occasional higher one, and a foal's is
 * drawn at random up to the stronger of its two parents.
 * <p>
 * {@link #getVariant() getVariant} is the bare coat name with no namespace - {@code creamy}
 * and the like - so it matches the parrot, rabbit and axolotl variants rather than the cat
 * and frog ones.
 * example:
 * <pre>
 * const LlamaEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.LlamaEntityHelper");
 * const llamas = World.getEntities(32, "llama", "trader_llama");
 * if (llamas !== null) {
 *   for (const entity of llamas) {
 *     const llama = LlamaEntityHelper.class.cast(entity);
 *     Chat.log(`${llama.getType()}: ${llama.getVariant()}, `
 *       + `strength ${llama.getStrength()}, `
 *       + `${llama.hasChest()} chest, `
 *       + `${llama.getInventorySize()} slots`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class LlamaEntityHelper<T extends Llama> extends DonkeyEntityHelper<T> {

    public LlamaEntityHelper(T base) {
        super(base);
    }

    /**
     * The coat name as the game writes it in its save data, with no namespace on the front:
     * {@code creamy}, {@code white}, {@code brown} or {@code gray}. A trader llama has a
     * coat of its own rather than one of these four.
     *
     * @return the variant of this llama.
     * @since 1.8.4
     */
    @DocletReplaceReturn("LlamaVariant")
    public String getVariant() {
        return base.getVariant().getSerializedName();
    }

    /**
     * How wide the llama's chest is, from one to five, and what its chest capacity follows
     * from: three slots per point, so a llama with a chest at strength one carries three and
     * at strength five carries fifteen. A llama without a chest carries nothing whatever its
     * strength.
     * <p>
     * The game keeps a llama's strength inside those five bounds however it is set, and a
     * foal's is drawn at random from one up to the stronger of its two parents rather than
     * being copied from either one.
     * example:
     * <pre>
     * const LlamaEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.LlamaEntityHelper");
     * const llamas = World.getEntities(32, "llama");
     * if (llamas !== null) {
     *   for (const entity of llamas) {
     *     const llama = LlamaEntityHelper.class.cast(entity);
     *     if (llama.hasChest()) {
     *       Chat.log(`a ${llama.getStrength()} strength llama, `
     *         + `${llama.getInventorySize()} slots`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the strength of this llama.
     * @since 1.8.4
     */
    public int getStrength() {
        return base.getStrength();
    }

    /**
     * Whether this is a trader llama rather than an ordinary one. The ordinary llama always
     * answers {@code false} and the trader kind always answers {@code true}; nothing
     * switches between them, so this is a way of telling the two apart rather than a state
     * a llama is in.
     *
     * @return {@code true} if this llama belongs to a wandering trader, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isTraderLlama() {
        return base.isTraderLlama();
    }

}
