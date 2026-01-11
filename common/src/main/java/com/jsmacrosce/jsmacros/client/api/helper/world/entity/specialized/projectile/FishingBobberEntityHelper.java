package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile;

import net.minecraft.world.entity.projectile.FishingHook;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinFishingBobberEntity;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class FishingBobberEntityHelper extends EntityHelper<FishingHook> {

    public FishingBobberEntityHelper(FishingHook base) {
        super(base);
    }

    /**
     * @return {@code true} if a fish has been caught, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasCaughtFish() {
        return ((MixinFishingBobberEntity) base).getCaughtFish();
    }

    /**
     * When in open water the player can get treasures from fishing.
     *
     * @return {@code true} if the bobber is in open water, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isInOpenWater() {
        return base.isOpenWaterFishing();
    }

    /**
     * @return {@code true} if the bobber has an entity hooked, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasEntityHooked() {
        return base.getHookedIn() != null;
    }

    /**
     * @return the hooked entity, or {@code null} if there is no entity hooked.
     * @since 1.8.4
     */
    @Nullable
    public EntityHelper<?> getHookedEntity() {
        return hasEntityHooked() ? EntityHelper.create(base.getHookedIn()) : null;
    }

}
