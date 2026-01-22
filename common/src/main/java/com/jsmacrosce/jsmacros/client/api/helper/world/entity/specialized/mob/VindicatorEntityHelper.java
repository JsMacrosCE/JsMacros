package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Vindicator;
import com.jsmacrosce.doclet.DocletCategory;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class VindicatorEntityHelper extends IllagerEntityHelper<Vindicator> {

    public VindicatorEntityHelper(Vindicator base) {
        super(base);
    }

    /**
     * @return {@code true} if this vindicator is johnny, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isJohnny() {
        return base.hasCustomName() && base.getCustomName().getString().equals("Johnny");
    }

}
