package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.rabbit.Rabbit;
*///? } else {
import net.minecraft.world.entity.animal.Rabbit;
//?}

/**
 * Wraps a rabbit and exposes its variant, including the killer bunny variant.
 * <pre>
 * const RabbitEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.RabbitEntityHelper");
 * const rabbits = World.getEntities(32, "rabbit");
 * if (rabbits !== null) {
 *   for (const entity of rabbits) {
 *     const rabbit = RabbitEntityHelper.class.cast(entity);
 *     Chat.log(`a ${rabbit.getVariant()} rabbit, killer ${rabbit.isKillerBunny()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class RabbitEntityHelper extends AnimalEntityHelper<Rabbit> {

    /** Wraps the supplied rabbit and retains its reference for this helper.
     *
     * @param base rabbit entity to wrap
     */
    public RabbitEntityHelper(Rabbit base) {
        super(base);
    }

    /**
     * Returns the serialized variant name: {@code brown}, {@code white}, {@code black},
     * {@code white_splotched}, {@code gold}, {@code salt}, or {@code evil} for the killer bunny.
     *
     * @return the variant of this rabbit.
     * @since 1.8.4
     */
    @DocletReplaceReturn("RabbitVariant")
    public String getVariant() {
        return base.getVariant().getSerializedName();
    }

    /**
     * Returns whether this rabbit has the killer bunny variant.
     *
     * @return {@code true} if this rabbit is a killer bunny, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isKillerBunny() {
        return base.getVariant() == Rabbit.Variant.EVIL;
    }

}
