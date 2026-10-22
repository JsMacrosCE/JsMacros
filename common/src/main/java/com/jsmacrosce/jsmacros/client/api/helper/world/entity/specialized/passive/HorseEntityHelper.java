package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinHorseEntity;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.equine.Horse;
*///? } else {
import net.minecraft.world.entity.animal.horse.Horse;
//?}

/**
 * the plain horse, which is everything an {@link AbstractHorseEntityHelper} is plus the coat
 * and markings.
 * <p>
 * {@link #getVariant() getVariant} is the one to read carefully, and it is not a variant
 * id. The game packs two separate things into the one number: the coat colour in the low
 * byte and the markings in the byte above it. To read one of them, mask it out - the coat is
 * the low eight bits and the markings are the next eight.
 * example:
 * <pre>
 * const HorseEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.HorseEntityHelper");
 * const horses = World.getEntities(32, "horse");
 * if (horses !== null) {
 *   for (const entity of horses) {
 *     const horse = HorseEntityHelper.class.cast(entity);
 *     const packed = horse.getVariant();
 *     const coat = packed % 256;
 *     const markings = Math.floor(packed / 256) % 256;
 *     Chat.log(`coat ${coat}, markings ${markings}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class HorseEntityHelper extends AbstractHorseEntityHelper<Horse> {

    public HorseEntityHelper(Horse base) {
        super(base);
    }

    /**
     * The packed number the game keeps the horse's look in, which is two figures rather than
     * one: the coat colour in the low byte and the markings in the byte above it.
     * <p>
     * The coat is one of {@code 0} white, {@code 1} creamy, {@code 2} chestnut, {@code 3}
     * brown, {@code 4} black, {@code 5} gray, {@code 6} dark brown. The markings are {@code
     * 0} none, {@code 1} white, {@code 2} a white field, {@code 3} white dots, {@code 4}
     * black dots. A horse with the default look therefore answers {@code 0}, which is not
     * any particular coat on its own.
     * example:
     * <pre>
     * const HorseEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.HorseEntityHelper");
     * const horses = World.getEntities(32, "horse");
     * if (horses !== null) {
     *   for (const entity of horses) {
     *     const horse = HorseEntityHelper.class.cast(entity);
     *     const coat = horse.getVariant() % 256;
     *     if (coat === 4) {
     *       Chat.log("a black horse");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the variant of this horse.
     * @since 1.8.4
     */
    public int getVariant() {
        return ((MixinHorseEntity) base).invokeGetHorseVariant();
    }

}
