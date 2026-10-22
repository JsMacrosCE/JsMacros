package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.fish.Pufferfish;
*///? } else {
import net.minecraft.world.entity.animal.Pufferfish;
//?}

/**
 * Provides access to a pufferfish's inflation state.
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class PufferfishEntityHelper extends FishEntityHelper<Pufferfish> {

    /**
     * Wraps the supplied pufferfish and retains its reference for this helper.
     *
     * @param base pufferfish entity to wrap
     */
    public PufferfishEntityHelper(Pufferfish base) {
        super(base);
    }

    /**
     * Returns the pufferfish's puff state.
     *
     * @return {@code 0} when deflated, {@code 1} when partially inflated, or {@code 2} when fully inflated.
     * @since 1.8.4
     */
    public int getSize() {
        return base.getPuffState();
    }

}
