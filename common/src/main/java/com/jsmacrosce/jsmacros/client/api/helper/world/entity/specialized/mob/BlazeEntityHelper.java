package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Blaze;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * the blaze, which is the one mob whose {@code isOnFire} is not about fire.
 * <p>
 * The blaze class answers the game's fire check with its own attack charge instead of
 * consulting whether it is alight at all, so {@link #isOnFire() isOnFire} here means the
 * blaze is wound up to shoot. A blaze standing in lava answers {@code false} through this
 * call, because the blaze never asks the fire question in the first place.
 * <p>
 * The call itself is the same one {@code EntityHelper.isOnFire} makes, so a script that
 * reaches a blaze through the base helper rather than through this class gets this reading
 * as well — there is no second, fire-shaped answer to fall back on.
 * example:
 * <pre>
 * const BlazeEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.BlazeEntityHelper");
 * const blazes = World.getEntities(32, "blaze");
 * if (blazes !== null) {
 *   for (const entity of blazes) {
 *     const blaze = BlazeEntityHelper.class.cast(entity);
 *     // this is the wind-up, and the fireballs follow a few ticks behind it
 *     if (blaze.isOnFire()) {
 *       Chat.log(`a blaze at ${blaze.getPos()} is charged up`);
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
public class BlazeEntityHelper extends MobEntityHelper<Blaze> {

    public BlazeEntityHelper(Blaze base) {
        super(base);
    }

    /**
     * Whether the blaze is wound up to shoot. The blaze sets this as its attack starts, a
     * while before it fires anything, and holds it across the shots that follow, clearing
     * it when the burst is over or the target is gone.
     * <p>
     * This is a report on the attack and not on the blaze being alight: the blaze's own
     * fire check is this flag, so a burning blaze and a blaze in lava both answer
     * {@code false} unless it happens to be charged.
     * example:
     * <pre>
     * const blazes = World.getEntities(32, "blaze");
     * if (blazes !== null) {
     *   for (const entity of blazes) {
     *     // the inherited call is the same one: there is no separate fire flag on a blaze
     *     if (entity.isOnFire()) {
     *       Chat.log(`charged at ${entity.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} while the blaze is charged up to shoot, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isOnFire() {
        return base.isOnFire();
    }

}
