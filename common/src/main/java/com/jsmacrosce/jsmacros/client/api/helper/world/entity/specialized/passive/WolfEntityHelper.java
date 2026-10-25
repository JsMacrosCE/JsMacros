package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.wolf.Wolf;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.DyeColorHelper;

/**
 * Provides access to wolf-specific state, including taming, collar color, begging, anger,
 * and wetness.
 * <pre>
 * const player = Player.getPlayer();
 * const wolves = World.getEntities(32, "wolf");
 * if (player !== null) {
 *   if (wolves !== null) {
 *     for (const entity of wolves) {
 *       if (!(entity instanceof EntityHelpers.Wolf)) continue;
 *       Chat.log(`${entity.isTamed() ? "tamed" : "wild"}, `
 *         + `collar ${entity.getCollarColor().getName()}, `
 *         + `begging ${entity.isBegging()}, angry ${entity.isAngry()}, `
 *         + `wet ${entity.isWet()}`);
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
public class WolfEntityHelper extends TameableEntityHelper<Wolf> {

    /** Wraps the wolf and retains its reference for this helper.
     * @param base wolf entity to wrap
     */
    public WolfEntityHelper(Wolf base) {
        super(base);
    }

    /**
     * Whether the wolf's begging goal is active. Wild wolves can beg too; the goal checks
     * for a nearby player holding a bone or wolf food.
     *
     * @return {@code true} if this wolf is begging off a nearby player holding something it
     * wants, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isBegging() {
        return base.isInterested();
    }

    /**
     * Returns the wolf's collar color. Wild wolves have a collar color too, so this does not
     * indicate whether the wolf is tamed.
     *
     * @return the color of this wolf's collar.
     * @since 1.8.4
     */
    public DyeColorHelper getCollarColor() {
        return new DyeColorHelper(base.getCollarColor());
    }

    /**
     * Whether the wolf is angry; this reports anger state, not a timer value or its current
     * target.
     *
     * @return {@code true} if this wolf is angry, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAngry() {
        return base.isAngry();
    }

    /**
     * Whether the wolf's wet flag is set. The flag is updated by its water/rain and drying
     * behavior, but is not synchronized or saved, so the client-side value may not reflect
     * the server's state.
     *
     * @return {@code true} if this wolf is wet, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isWet() {
        return base.isWet;
    }

}
