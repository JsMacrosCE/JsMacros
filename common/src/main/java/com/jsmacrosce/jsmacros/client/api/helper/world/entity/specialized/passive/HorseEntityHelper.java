package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinHorseEntity;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.equine.Horse;
*///? } else {
import net.minecraft.world.entity.animal.horse.Horse;
//?}

/**
 * Provides access to horse coat and markings data in addition to {@link AbstractHorseEntityHelper}.
 * <pre>
 * const entities = World.getEntities(32);
 * if (entities !== null) {
 *   for (const entity of entities) {
 *     if (!(entity instanceof EntityHelpers.Horse)) continue;
 *     const packed = entity.getVariant();
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

    /**
     * Wraps the supplied horse, forwarding it to the superclass, where {@code this.base} retains the reference.
     *
     * @param base horse entity to wrap
     */
    public HorseEntityHelper(Horse base) {
        super(base);
    }

    /**
     * Returns the packed horse variant: the coat ID occupies the low byte and the markings ID the next byte.
     *
     * @return the packed coat and markings IDs; decode each as an unsigned byte.
     * @since 1.8.4
     */
    public int getVariant() {
        return ((MixinHorseEntity) base).invokeGetHorseVariant();
    }

}
