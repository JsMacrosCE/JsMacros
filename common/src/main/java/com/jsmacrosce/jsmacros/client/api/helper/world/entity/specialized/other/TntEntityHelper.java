package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other;

import net.minecraft.world.entity.item.PrimedTnt;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class TntEntityHelper extends EntityHelper<PrimedTnt> {

    public TntEntityHelper(PrimedTnt base) {
        super(base);
    }

    /**
     * @return the remaining time until this TNT explodes.
     * @since 1.8.4
     */
    public int getRemainingTime() {
        return base.getFuse();
    }

}
