package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile;

import net.minecraft.world.entity.projectile.FishingHook;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinFishingBobberEntity;

/**
 * A fishing bobber projectile, with access to its bite and open-water state and hooked entity.
 * <pre>
 * const bobbers = World.getEntities(32, "fishing_bobber");
 * if (bobbers !== null) {
 *   for (const entity of bobbers) {
 *     if (!(entity instanceof EntityHelpers.FishingBobber)) continue;
 *     if (entity.hasCaughtFish()) {
 *       Chat.log("reel in now");
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class FishingBobberEntityHelper extends EntityHelper<FishingHook> {

    /**
     * Wraps the supplied fishing bobber and retains its reference for this helper.
     *
     * @param base fishing bobber projectile to wrap
     */
    public FishingBobberEntityHelper(FishingHook base) {
        super(base);
    }

    /**
     * Returns whether a fish is currently biting; this flag can return to {@code false} without
     * being reeled in.
     *
     * @return {@code true} while a fish is biting; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasCaughtFish() {
        return ((MixinFishingBobberEntity) base).getCaughtFish();
    }

    /**
     * Returns the bobber's open-water fishing flag; this affects fishing loot eligibility.
     *
     * @return {@code true} if the bobber is in open water, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isInOpenWater() {
        return base.isOpenWaterFishing();
    }

    /**
     * Returns whether the bobber currently references a hooked entity. This is distinct from
     * {@link #hasCaughtFish()}, which reports a bite.
     *
     * @return {@code true} if the bobber has an entity hooked, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasEntityHooked() {
        return base.getHookedIn() != null;
    }

    /**
     * Returns the hooked entity, if it is currently present in the world. The bobber's owner is
     * not the hooked entity.
     *
     * @return the hooked entity, or {@code null} if there is no entity hooked.
     * @since 1.8.4
     */
    @Nullable
    public EntityHelper<?> getHookedEntity() {
        return hasEntityHooked() ? EntityHelper.create(base.getHookedIn()) : null;
    }

}
