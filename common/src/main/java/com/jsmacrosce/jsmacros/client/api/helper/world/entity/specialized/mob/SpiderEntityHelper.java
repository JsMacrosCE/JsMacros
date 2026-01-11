package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Spider;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class SpiderEntityHelper extends MobEntityHelper<Spider> {

    public SpiderEntityHelper(Spider base) {
        super(base);
    }

    /**
     * @return {@code true} if this spider is currently climbing a wall, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isClimbing() {
        return base.onClimbable();
    }

}
