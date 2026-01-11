package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other;

import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.client.access.IMixinInteractionEntity;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinInteractionEntity2;

/**
 * @author aMelonRind
 * @since 1.9.1
 */
@SuppressWarnings("unused")
public class InteractionEntityHelper extends EntityHelper<Interaction> {

    public InteractionEntityHelper(Interaction base) {
        super(base);
    }

    /**
     * @since 1.9.1
     */
    public void setCanHit(boolean value) {
        ((IMixinInteractionEntity) base).jsmacros_setCanHitOverride(value);
    }

    /**
     * @since 1.9.1
     */
    @Nullable
    public EntityHelper<?> getLastAttacker() {
        LivingEntity e = base.getLastAttacker();
        return e == null ? null : EntityHelper.create(e);
    }

    /**
     * @since 1.9.1
     */
    @Nullable
    public EntityHelper<?> getLastInteracted() {
        LivingEntity e = base.getTarget();
        return e == null ? null : EntityHelper.create(e);
    }

    /**
     * @since 1.9.1
     */
    public float getWidth() {
        return ((MixinInteractionEntity2) base).callGetInteractionWidth();
    }

    /**
     * @since 1.9.1
     */
    public float getHeight() {
        return ((MixinInteractionEntity2) base).callGetInteractionHeight();
    }

    /**
     * @since 1.9.1
     */
    public boolean shouldRespond() {
        return ((MixinInteractionEntity2) base).callShouldRespond();
    }

}
