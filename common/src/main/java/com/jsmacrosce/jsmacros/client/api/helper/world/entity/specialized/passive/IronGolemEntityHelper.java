package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.IronGolem;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class IronGolemEntityHelper extends MobEntityHelper<IronGolem> {

    public IronGolemEntityHelper(IronGolem base) {
        super(base);
    }

    /**
     * @return {@code true} if this iron golem was created by a player, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPlayerCreated() {
        return base.isPlayerCreated();
    }

}
