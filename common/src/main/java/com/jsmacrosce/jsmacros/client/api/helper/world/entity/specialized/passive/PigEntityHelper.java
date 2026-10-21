package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.pig.Pig;
*///? } else {
import net.minecraft.world.entity.animal.Pig;
//?}

/**
 * Provides pig-specific access to saddle state.
 * <pre>
 * const PigEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.PigEntityHelper");
 * const pigs = World.getEntities(32, "pig");
 * if (pigs !== null) {
 *   for (const entity of pigs) {
 *     const pig = PigEntityHelper.class.cast(entity);
 *     if (pig.isSaddled()) {
 *       Chat.log(`a saddled pig at ${pig.getPos()}`);
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class PigEntityHelper extends AnimalEntityHelper<Pig> {

    /**
     * Wraps the supplied pig and retains its reference for this helper.
     *
     * @param base pig entity to wrap
     */
    public PigEntityHelper(Pig base) {
        super(base);
    }

    /**
     * Checks whether the pig has a valid equippable item in its saddle slot.
     *
     * @return {@code true} if the pig has a valid saddle-slot item; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSaddled() {
        return base.isSaddled();
    }

}
