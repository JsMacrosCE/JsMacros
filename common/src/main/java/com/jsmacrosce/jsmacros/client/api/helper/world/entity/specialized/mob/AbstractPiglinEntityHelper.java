package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinAbstractPiglinEntity;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class AbstractPiglinEntityHelper<T extends AbstractPiglin> extends MobEntityHelper<T> {

    public AbstractPiglinEntityHelper(T base) {
        super(base);
    }

    /**
     * @return {@code true} if this piglin can be zombified in the current dimension, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean canBeZombified() {
        return !((MixinAbstractPiglinEntity) base).invokeIsImmuneToZombification();
    }

}
