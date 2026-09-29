package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.rabbit.Rabbit;
*///? } else {
import net.minecraft.world.entity.animal.Rabbit;
//?}

/**
 * the rabbit, which comes in six coats and one very rare seventh.
 * <p>
 * The rare one is what {@link #isKillerBunny() isKillerBunny} reports, and it is rare in
 * the game's own arithmetic rather than by name: its id is far past the others, so the game
 * has to be told to look for it rather than reaching it by counting.
 * example:
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

    public RabbitEntityHelper(Rabbit base) {
        super(base);
    }

    /**
     * The coat name as the game writes it in its save data, with no namespace on the front:
     * {@code brown}, {@code white}, {@code black}, {@code white_splotched}, {@code gold} or
     * {@code salt}, with {@code evil} for the killer bunny.
     * <p>
     * There is one coat per rabbit rather than a separate one for a baby, so a young rabbit
     * answers the same as the one it will grow into.
     *
     * @return the variant of this rabbit.
     * @since 1.8.4
     */
    @DocletReplaceReturn("RabbitVariant")
    public String getVariant() {
        return base.getVariant().getSerializedName();
    }

    /**
     * Whether this is the killer bunny, which is the same animal in every other respect and
     * is only a coat. The game reaches it by a special case rather than by counting through
     * the coats, which is why it has an id well past the rest.
     * example:
     * <pre>
     * const RabbitEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.RabbitEntityHelper");
     * const rabbits = World.getEntities(48, "rabbit");
     * if (rabbits !== null) {
     *   for (const entity of rabbits) {
     *     const rabbit = RabbitEntityHelper.class.cast(entity);
     *     if (rabbit.isKillerBunny()) {
     *       Chat.log(`the killer bunny at ${rabbit.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this rabbit is a killer bunny, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isKillerBunny() {
        return base.getVariant() == Rabbit.Variant.EVIL;
    }

}
