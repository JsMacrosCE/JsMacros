package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.feline.Ocelot;
*///? } else {
import net.minecraft.world.entity.animal.Ocelot;
//?}

/**
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class OcelotEntityHelper extends AnimalEntityHelper<Ocelot> {

    public OcelotEntityHelper(Ocelot base) {
        super(base);
    }

    /**
     * Ocelots trust players after being fed with cod or salmon.
     *
     * @return {@code true} if this ocelot is trusting player and not running away form them,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isTrusting() {
        return ((MixinOcelotEntity) base).invokeIsTrusting();
    }

}
