package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;

//?  if >=1.21.11 {
/*import net.minecraft.world.entity.animal.cow.MushroomCow;
*///? } else {
import net.minecraft.world.entity.animal.MushroomCow;
//?}

/**
 * Provides access to a mooshroom's variant and whether it is ready for shearing.
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

    /**
     * Wraps the supplied mooshroom and retains its reference for this helper.
     *
     * @param base mooshroom entity to wrap
     */
    public MooshroomEntityHelper(MushroomCow base) {
        super(base);
    }

    /**
     * Returns whether this mooshroom is ready for shearing: it is alive and not a baby.
     *
     * @return {@code true} if this mooshroom can be sheared, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isShearable() {
        return base.readyForShearing();
    }

    /**
     * Returns whether this mooshroom's variant is red.
     *
     * @return {@code true} if this mooshroom is a red mooshroom, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isRed() {
        return base.getVariant() == MushroomCow.Variant.RED;
    }

    /**
     * Returns whether this mooshroom's variant is brown.
     *
     * @return {@code true} if this mooshroom is a brown mooshroom, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isBrown() {
        return base.getVariant() == MushroomCow.Variant.BROWN;
    }

}
