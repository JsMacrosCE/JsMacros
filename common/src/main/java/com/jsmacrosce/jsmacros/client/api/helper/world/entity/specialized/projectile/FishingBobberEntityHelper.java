package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile;

import net.minecraft.world.entity.projectile.FishingHook;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinFishingBobberEntity;

/**
 * a fishing bobber, the float on the end of a fishing line.
 * <p>
 * The four calls here are the three states a bobber passes through and the entity it has caught.
 * A bobber in open water is what decides whether treasure is on the table, a bobber with
 * something hooked on it is a different state again, and {@link #hasCaughtFish()
 * hasCaughtFish()} is the moment the fish is on rather than a fish being possible. The three
 * are not exclusive: a bobber with a fish on it is normally in open water as well.
 * <p>
 * These are reads of what the server has told the client, so a bobber that has just been cast
 * can report {@code true} for the water before the line has settled.
 * example:
 * <pre>
 * const FishingBobberEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.FishingBobberEntityHelper");
 * const bobbers = World.getEntities(32, "fishing_bobber");
 * if (bobbers !== null) {
 *   for (const entity of bobbers) {
 *     const bobber = FishingBobberEntityHelper.class.cast(entity);
 *     if (bobber.hasCaughtFish()) {
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

    public FishingBobberEntityHelper(FishingHook base) {
        super(base);
    }

    /**
     * {@code true} if a fish has been caught, {@code false} otherwise.
     * <p>
     * This is the moment the fish is on the line and not a fish being possible, and it is what
     * a player watches for before reeling in. The game holds it for between twenty and forty
     * ticks once it goes true, so a script polling it may see it go true and then false
     * without the fish having been taken: the window closes on its own if nobody reels in. It
     * is a client-side read of a synced flag and nothing here sets it.
     * example:
     * <pre>
     * const FishingBobberEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.FishingBobberEntityHelper");
     * const bobbers = World.getEntities(32, "fishing_bobber");
     * if (bobbers !== null) {
     *   for (const entity of bobbers) {
     *     const bobber = FishingBobberEntityHelper.class.cast(entity);
     *     // the window is twenty to forty ticks, so this can go true and then false again
     *     if (bobber.hasCaughtFish()) {
     *       Chat.log("a fish is on, reel in now");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if a fish has been caught, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasCaughtFish() {
        return ((MixinFishingBobberEntity) base).getCaughtFish();
    }

    /**
     * When in open water the player can get treasures from fishing.
     * <p>
     * Open water is a source block of water with room above it for the bobber, not flowing
     * water and not water with a lid on it, and the game requires it for the good loot rather
     * than just for any catch. The flag starts {@code true} and the game clears it while the
     * bobber is over water that is not open, then sets it back once the line has been in the
     * right place long enough, so it can flicker on the way in and out rather than being
     * simply the water underneath.
     * example:
     * <pre>
     * const FishingBobberEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.FishingBobberEntityHelper");
     * const bobbers = World.getEntities(32, "fishing_bobber");
     * if (bobbers !== null) {
     *   for (const entity of bobbers) {
     *     const bobber = FishingBobberEntityHelper.class.cast(entity);
     *     // open water is what treasure needs, not just any water
     *     if (bobber.isInOpenWater()) {
     *       Chat.log("good water, the good loot is on the table");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the bobber is in open water, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isInOpenWater() {
        return base.isOpenWaterFishing();
    }

    /**
     * {@code true} if the bobber has an entity hooked, {@code false} otherwise.
     * <p>
     * This is the line snagging something rather than a fish being caught, and the two are
     * different: a bobber can have an entity hooked without a fish and a fish without
     * anything snagged. It is what a script tests before calling
     * {@link #getHookedEntity() getHookedEntity()}, since that is {@code null} in exactly the
     * case this is {@code false}.
     * example:
     * <pre>
     * const FishingBobberEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.FishingBobberEntityHelper");
     * const bobbers = World.getEntities(32, "fishing_bobber");
     * if (bobbers !== null) {
     *   for (const entity of bobbers) {
     *     const bobber = FishingBobberEntityHelper.class.cast(entity);
     *     // a snagged entity is not the same thing as a caught fish
     *     if (bobber.hasEntityHooked()) {
     *       Chat.log(`the line is snagged on ${bobber.getHookedEntity().getName().getString()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the bobber has an entity hooked, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasEntityHooked() {
        return base.getHookedIn() != null;
    }

    /**
     * the hooked entity, or {@code null} if there is no entity hooked.
     * <p>
     * This is whatever the line has snagged on, which is {@code null} in exactly the case
     * {@link #hasEntityHooked() hasEntityHooked()} answers {@code false}. It is not the player
     * who cast the line: that is the owner, which the shared entity helpers reach, and a
     * bobber being held has its owner rather than a hooked entity. The hooked entity is tracked
     * by id, so it is {@code null} if that entity has since left the world.
     * example:
     * <pre>
     * const FishingBobberEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.FishingBobberEntityHelper");
     * const bobbers = World.getEntities(32, "fishing_bobber");
     * if (bobbers !== null) {
     *   for (const entity of bobbers) {
     *     const bobber = FishingBobberEntityHelper.class.cast(entity);
     *     const hooked = bobber.getHookedEntity();
     *     if (hooked !== null) {
     *       Chat.log(`hooked a ${hooked.getType()} at ${hooked.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the hooked entity, or {@code null} if there is no entity hooked.
     * @since 1.8.4
     */
    @Nullable
    public EntityHelper<?> getHookedEntity() {
        return hasEntityHooked() ? EntityHelper.create(base.getHookedIn()) : null;
    }

}
