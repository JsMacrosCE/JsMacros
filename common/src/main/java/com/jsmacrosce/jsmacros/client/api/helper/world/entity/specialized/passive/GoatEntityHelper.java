package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.goat.Goat;
import com.jsmacrosce.doclet.DocletCategory;

/**
 * the goat, which is an ordinary animal that comes in a screaming variety and may be
 * missing one of its two horns.
 * <p>
 * {@link #hasHorns() hasHorns} is the one to reach for rather than either side on its own:
 * it is whether <em>either</em> horn is still on, which is the only one of the three that
 * stays true for a goat that has lost one. A goat loses at most one, at the moment it is
 * spawned, and which side goes is a coin toss.
 * example:
 * <pre>
 * const GoatEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.GoatEntityHelper");
 * const goats = World.getEntities(32, "goat");
 * if (goats !== null) {
 *   for (const entity of goats) {
 *     const goat = GoatEntityHelper.class.cast(entity);
 *     Chat.log(`${goat.getType()}: left ${goat.hasLeftHorn()}, `
 *       + `right ${goat.hasRightHorn()}, any ${goat.hasHorns()}, `
 *       + `screaming ${goat.isScreaming()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class GoatEntityHelper extends AnimalEntityHelper<Goat> {

    public GoatEntityHelper(Goat base) {
        super(base);
    }

    /**
     * Whether this goat is the screaming kind. It is a one-in-fifty chance at spawn, it is
     * inherited by a foal from whichever parent the game picks, and the game uses it
     * everywhere it picks a sound - the ambient call, the hurt sound, the death sound and
     * the horn it drops - so a screaming goat sounds different doing all of them.
     *
     * @return {@code true} if this goat is currently screaming, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isScreaming() {
        return base.isScreamingGoat();
    }

    /**
     * Whether the goat still has its left horn. A goat spawned without one answers {@code
     * false} here and {@code true} for {@link #hasRightHorn() hasRightHorn}, and a goat
     * that has both answers {@code true} for both.
     *
     * @return {@code true} if this goat has its left horn still on, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasLeftHorn() {
        return base.hasLeftHorn();
    }

    /**
     * Whether the goat still has its right horn. A goat spawned without one answers {@code
     * false} here and {@code true} for {@link #hasLeftHorn() hasLeftHorn}, and a goat that
     * has both answers {@code true} for both.
     *
     * @return {@code true} if this goat has its right horn still on, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasRightHorn() {
        return base.hasRightHorn();
    }

    /**
     * Whether the goat has any horn at all, which is either of the two rather than both.
     * This is the one to ask when the question is whether the goat can drop a horn when it
     * is killed.
     * example:
     * <pre>
     * const GoatEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.GoatEntityHelper");
     * const goats = World.getEntities(32, "goat");
     * if (goats !== null) {
     *   for (const entity of goats) {
     *     const goat = GoatEntityHelper.class.cast(entity);
     *     if (goat.hasHorns()) {
     *       Chat.log(`that goat can drop a horn, from ${goat.getBlockPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this goat still has a horn, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasHorns() {
        return hasLeftHorn() || hasRightHorn();
    }

}
