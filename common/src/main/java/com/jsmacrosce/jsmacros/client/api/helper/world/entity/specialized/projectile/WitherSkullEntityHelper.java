package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile;

import net.minecraft.world.entity.projectile.WitherSkull;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class WitherSkullEntityHelper extends EntityHelper<WitherSkull> {

    public WitherSkullEntityHelper(WitherSkull base) {
        super(base);
    }

    /**
     * @return {@code true} if the wither skull is charged, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCharged() {
        return base.isDangerous();
    }

}
