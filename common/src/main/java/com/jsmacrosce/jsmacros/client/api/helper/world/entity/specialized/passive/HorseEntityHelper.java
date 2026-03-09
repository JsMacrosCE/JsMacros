package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinHorseEntity;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.equine.Horse;
*///? } else {
import net.minecraft.world.entity.animal.horse.Horse;
//?}

/**
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
     * @return the variant of this horse.
     * @since 1.8.4
     */
    public int getVariant() {
        return ((MixinHorseEntity) base).invokeGetHorseVariant();
    }

}
