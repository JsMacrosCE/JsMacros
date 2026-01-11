package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Blaze;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class BlazeEntityHelper extends MobEntityHelper<Blaze> {

    public BlazeEntityHelper(Blaze base) {
        super(base);
    }

    /**
     * A blaze can only shoot fireballs when it's on fire.
     *
     * @return {@code true} if the blaze is on fire, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isOnFire() {
        return base.isOnFire();
    }

}
