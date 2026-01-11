package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.allay.Allay;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinAllayEntity;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class AllayEntityHelper extends MobEntityHelper<Allay> {

    public AllayEntityHelper(Allay base) {
        super(base);
    }

    /**
     * @return {@code true} if this allay is dancing, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isDancing() {
        return base.isDancing();
    }

    /**
     * @return {@code true} if this allay can be duplicated, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canDuplicate() {
        return ((MixinAllayEntity) base).invokeCanDuplicate();
    }

    /**
     * @return {@code true} if this allay is holding a item, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isHoldingItem() {
        return base.hasItemInHand();
    }

}
