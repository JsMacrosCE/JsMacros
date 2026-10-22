package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;

//?  if >=1.21.11 {
/*import net.minecraft.world.entity.animal.cow.MushroomCow;
*///? } else {
import net.minecraft.world.entity.animal.MushroomCow;
//?}

/**
 * the mooshroom, which is a cow in one of two colours.
 * <p>
 * {@link #isShearable() isShearable} is worth comparing with the same method on the sheep and
 * the snow golem. The three read alike and answer differently: this one only asks whether
 * the mooshroom is alive and grown up, because there is nothing to shear off it, while a
 * sheep also has to be un-sheared and a snow golem also has to still have its pumpkin.
 * example:
 * <pre>
 * const MooshroomEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.MooshroomEntityHelper");
 * const mooshrooms = World.getEntities(32, "mooshroom");
 * if (mooshrooms !== null) {
 *   for (const entity of mooshrooms) {
 *     const mooshroom = MooshroomEntityHelper.class.cast(entity);
 *     Chat.log(`red ${mooshroom.isRed()}, brown ${mooshroom.isBrown()}, `
 *       + `shearable ${mooshroom.isShearable()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class MooshroomEntityHelper extends AnimalEntityHelper<MushroomCow> {

    public MooshroomEntityHelper(MushroomCow base) {
        super(base);
    }

    /**
     * Whether the mooshroom is grown up and alive, which is all the game asks here. There is
     * no fleece on a mooshroom to shear, so unlike the sheep's {@code isShearable()} this
     * does not fall for a mooshroom that has already been sheared, and unlike the snow
     * golem's it does not depend on anything being worn.
     *
     * @return {@code true} if this mooshroom can be sheared, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isShearable() {
        return base.readyForShearing();
    }

    /**
     * Whether this mooshroom is the red kind, which is the one that can be milked for
     * suspicious stew. The two colours are the whole of the variety, so a mooshroom that is
     * neither red nor brown has not had its variant data arrive yet.
     * example:
     * <pre>
     * const MooshroomEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.MooshroomEntityHelper");
     * const mooshrooms = World.getEntities(32, "mooshroom");
     * if (mooshrooms !== null) {
     *   for (const entity of mooshrooms) {
     *     const mooshroom = MooshroomEntityHelper.class.cast(entity);
     *     if (mooshroom.isRed()) {
     *       Chat.log("that one can be stewed");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this mooshroom is a red mooshroom, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isRed() {
        return base.getVariant() == MushroomCow.Variant.RED;
    }

    /**
     * Whether this mooshroom is the brown kind. It is the rarer of the two: the game spawns
     * red by default and a brown mooshroom has to be spawned as one deliberately, and a calf
     * usually comes out matching one of its parents rather than being picked afresh.
     *
     * @return {@code true} if this mooshroom is a brown mooshroom, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isBrown() {
        return base.getVariant() == MushroomCow.Variant.BROWN;
    }

}
