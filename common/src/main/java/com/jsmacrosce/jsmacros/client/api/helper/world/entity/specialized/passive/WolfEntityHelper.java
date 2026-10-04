package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.wolf.Wolf;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.DyeColorHelper;

/**
 * the wolf, which is a tameable animal with a collar and two moods.
 * <p>
 * {@link #isBegging() isBegging} is the one worth reading first, because the obvious reading
 * of it is wrong: a wolf begs whether or not it is tamed. The game gives every wolf the
 * goal, and it runs for a wolf that has never been hand-fed anything.
 * <p>
 * {@link #isAngry() isAngry} is the anger timer and not the wolf's mood, and it is the same
 * figure a bee reports. The game starts a timer when the wolf is provoked and everything the
 * wolf does about it hangs off that timer.
 * example:
 * <pre>
 * const WolfEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.WolfEntityHelper");
 * const player = Player.getPlayer();
 * const wolves = World.getEntities(32, "wolf");
 * if (player !== null) {
 *   if (wolves !== null) {
 *     for (const entity of wolves) {
 *       const wolf = WolfEntityHelper.class.cast(entity);
 *       Chat.log(`${wolf.isTamed() ? "tamed" : "wild"}, `
 *         + `collar ${wolf.getCollarColor().getName()}, `
 *         + `begging ${wolf.isBegging()}, angry ${wolf.isAngry()}, `
 *         + `wet ${wolf.isWet()}`);
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

    public WolfEntityHelper(Wolf base) {
        super(base);
    }

    /**
     * Whether the wolf is begging, which is the game staring at the nearest player holding
     * something interesting. A bone or anything the wolf counts as food does it, and the
     * player has to be within about eight blocks.
     * <p>
     * The name reads as though the wolf has to be tamed for this, and it does not: the game
     * gives the goal to every wolf, so a wolf that has never been hand-fed anything will
     * still come and beg off a player holding a bone.
     * example:
     * <pre>
     * const WolfEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.WolfEntityHelper");
     * const wolves = World.getEntities(16, "wolf");
     * if (wolves !== null) {
     *   for (const entity of wolves) {
     *     const wolf = WolfEntityHelper.class.cast(entity);
     *     if (wolf.isBegging()) {
     *       Chat.log(`begging, and tamed: ${wolf.isTamed()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this wolf is begging off a nearby player holding something it
     * wants, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isBegging() {
        return base.isInterested();
    }

    /**
     * The colour of the wolf's collar. The game keeps a colour on every wolf whatever its
     * state, so a collar colour is not evidence that the wolf has been tamed - a wild wolf
     * answers the same as a tame one.
     *
     * @return the color of this wolf's collar.
     * @since 1.8.4
     */
    public DyeColorHelper getCollarColor() {
        return new DyeColorHelper(base.getCollarColor());
    }

    /**
     * The anger timer, so it answers for how long the wolf has been cross rather than whether
     * it is currently looking at whoever it is cross about. It is the same figure a bee
     * reports: the game starts a timer when the wolf is provoked, and the wolf going after
     * whoever it was provoked by hangs off that timer rather than off a separate flag.
     * example:
     * <pre>
     * const WolfEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.WolfEntityHelper");
     * const wolves = World.getEntities(24, "wolf");
     * if (wolves !== null) {
     *   for (const entity of wolves) {
     *     const wolf = WolfEntityHelper.class.cast(entity);
     *     if (wolf.isAngry()) {
     *       Chat.log(`that wolf is still cross at ${wolf.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this wolf is angry, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAngry() {
        return base.isAngry();
    }

    /**
     * Whether the wolf is wet, which is the game's own plain field rather than anything
     * worked out from the weather, and it is the clearest caveat on this class: because it
     * is neither synced nor saved, a wolf on the client normally answers {@code false}
     * whatever the weather is doing.
     * <p>
     * The game sets it when the wolf is in water or in rain and clears it once the wolf has
     * finished shaking itself dry on the ground, and until then the wolf is drawn darker and
     * throws water about. That shake is run by the server, which is why the flag never gets
     * as far as the client.
     *
     * @return {@code true} if this wolf is wet, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isWet() {
        return base.isWet;
    }

}
