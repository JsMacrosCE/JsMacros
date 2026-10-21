package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinTridentEntity;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
*///? } else {
import net.minecraft.world.entity.projectile.ThrownTrident;
 //?}

/**
 * A helper for querying a thrown trident's stored loyalty level and foil state.
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class TridentEntityHelper extends EntityHelper<ThrownTrident> {

    /**
     * Wraps the supplied thrown trident and retains its reference for this helper.
     *
     * @param base thrown trident entity to wrap
     */
    public TridentEntityHelper(ThrownTrident base) {
        super(base);
    }

    /**
     * Returns whether the trident's stored loyalty level is greater than zero.
     *
     * @return {@code true} if the trident is enchanted with loyalty, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasLoyalty() {
        return base.getEntityData().get(((MixinTridentEntity) base).getLoyalty()) > 0;
    }

    /**
     * Returns the trident's stored foil (glint) state.
     *
     * @return {@code true} if the foil state is set, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isEnchanted() {
        return base.isFoil();
    }

}
