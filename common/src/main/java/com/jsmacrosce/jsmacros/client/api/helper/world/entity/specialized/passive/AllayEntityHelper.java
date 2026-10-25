package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.allay.Allay;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinAllayEntity;

/**
 * Provides access to an allay's dancing, duplication-cooldown, and held-item states.
 * <pre>
 * const allays = World.getEntities(32, "allay");
 * if (allays !== null) {
 *   for (const entity of allays) {
 *     if (!(entity instanceof EntityHelpers.Allay)) continue;
 *     Chat.log(`dancing ${entity.isDancing()}, `
 *       + `holding ${entity.isHoldingItem()}, `
 *       + `ready to duplicate ${entity.canDuplicate()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class AllayEntityHelper extends MobEntityHelper<Allay> {

    /**
     * Wraps the supplied allay, retaining its reference through the superclass constructors.
     *
     * @param base the allay entity to wrap
     */
    public AllayEntityHelper(Allay base) {
        super(base);
    }

    /**
     * Returns whether the allay is dancing.
     * @return {@code true} while the allay is dancing; {@code false} otherwise
     * @since 1.8.4
     */
    public boolean isDancing() {
        return base.isDancing();
    }

    /**
     * Returns whether the allay's duplication cooldown has elapsed; this does not indicate whether other duplication conditions are met.
     * @return {@code true} if the duplication cooldown has elapsed; {@code false} otherwise
     * @since 1.8.4
     */
    public boolean canDuplicate() {
        return ((MixinAllayEntity) base).invokeCanDuplicate();
    }

    /**
     * Returns whether the allay has an item in its main hand.
     * @return {@code true} if the allay has a main-hand item; {@code false} otherwise
     * @since 1.8.4
     */
    public boolean isHoldingItem() {
        return base.hasItemInHand();
    }

}
