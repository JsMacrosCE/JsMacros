package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.pig.Pig;
*///? } else {
import net.minecraft.world.entity.animal.Pig;
//?}

/**
 * the pig, which is the one animal here with a saddle and almost nothing else.
 * <p>
 * A saddle on a pig is only worth anything to a player riding it; the game itself has nothing
 * to say about one, so unlike the horse beside it there is no second question here about
 * whether the pig may be saddled.
 * example:
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

    public PigEntityHelper(Pig base) {
        super(base);
    }

    /**
     * Whether there is a saddle in the pig's saddle slot. This is the figure itself rather
     * than whether the pig may be saddled: a player can put one on an untamed pig with a
     * saddle in hand, and the game will not take it off again.
     *
     * @return {@code true} if this pig is saddled, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSaddled() {
        return base.isSaddled();
    }

}
