package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
*///? } else {
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
//?}

/**
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class DonkeyEntityHelper<T extends AbstractChestedHorse> extends AbstractHorseEntityHelper<T> {

    public DonkeyEntityHelper(T base) {
        super(base);
    }

    /**
     * @return {@code true} if the donkey is carrying a chest, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasChest() {
        return base.hasChest();
    }

}
