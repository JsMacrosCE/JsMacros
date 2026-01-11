package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.Pufferfish;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class PufferfishEntityHelper extends FishEntityHelper<Pufferfish> {

    public PufferfishEntityHelper(Pufferfish base) {
        super(base);
    }

    /**
     * A state of 0 means the fish is deflated, a state of 1 means the fish is inflated and a state
     * of 2 means the fish is fully inflated.
     *
     * @return the puff state of this pufferfish.
     * @since 1.8.4
     */
    public int getSize() {
        return base.getPuffState();
    }

}
