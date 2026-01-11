package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Ghast;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class GhastEntityHelper extends MobEntityHelper<Ghast> {

    public GhastEntityHelper(Ghast base) {
        super(base);
    }

    /**
     * @return {@code true} if this ghast is currently about to shoot a fireball, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isShooting() {
        return base.isCharging();
    }

}
