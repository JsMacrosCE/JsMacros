package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.AbstractFish;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class FishEntityHelper<T extends AbstractFish> extends MobEntityHelper<T> {

    public FishEntityHelper(T base) {
        super(base);
    }

    /**
     * @return {@code true} if this fish came from a bucket, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isFromBucket() {
        return base.fromBucket();
    }

}
