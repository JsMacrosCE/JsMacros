package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile;

import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
*///? } else {
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
//? }

/**
 * an arrow in flight, which covers the ordinary arrow and everything built on it such as a
 * tipped arrow or a spectral one.
 * <p>
 * The three calls here are the two ways an arrow carries a payload and the flag that says it
 * will do extra damage. Only the colour is restricted to the plain arrow, because only the
 * plain arrow has a potion in it: on any other kind {@link #getColor() getColor()} answers
 * {@code -1}, which is the same value a plain arrow with no potion gives. So a script cannot
 * tell a spectral arrow from an unpotioned one by this call alone.
 * example:
 * <pre>
 * const ArrowEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.ArrowEntityHelper");
 * const arrows = World.getEntities(32, "arrow", "spectral_arrow");
 * if (arrows !== null) {
 *   for (const entity of arrows) {
 *     const arrow = ArrowEntityHelper.class.cast(entity);
 *     // -1 means no colour, which covers both an unpotioned arrow and a spectral one
 *     const colour = arrow.getColor();
 *     Chat.log(`colour ${colour === -1 ? "none" : `0x${colour.toString(16)}`}, critical: ${arrow.isCritical()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class ArrowEntityHelper extends EntityHelper<AbstractArrow> {

    public ArrowEntityHelper(AbstractArrow base) {
        super(base);
    }

    /**
     * the packed colour of the potion the arrow is carrying, or {@code -1} if it has none.
     * <p>
     * A packed colour is {@code 0xAARRGGBB}. There are two ways to get {@code -1} here and
     * they are not distinguishable by this call: an arrow with no potion at all gives it, and
     * so does any arrow that is not a plain one, because a spectral arrow has no potion
     * contents to colour it with. The colour is what tints the trail the arrow leaves and
     * nothing else, so it says nothing about what the arrow will do on impact.
     * example:
     * <pre>
     * const ArrowEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.ArrowEntityHelper");
     * const arrows = World.getEntities(32, "arrow", "spectral_arrow");
     * if (arrows !== null) {
     *   for (const entity of arrows) {
     *     const arrow = ArrowEntityHelper.class.cast(entity);
     *     const colour = arrow.getColor();
     *     if (colour !== -1) {
     *       // a tipped arrow, and the trail it leaves is this colour
     *       Chat.log(`a potion arrow, tinted 0x${colour.toString(16)}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the particle's color of the arrow, or {@code -1} if the arrow has no particles.
     * @since 1.8.4
     */
    public int getColor() {
        if (base instanceof Arrow) {
            return ((Arrow) base).getColor();
        }
        return -1;
    }

    /**
     * whether the arrow is flying as a critical hit, which is what a fully drawn bow shot from
     * a height produces.
     * <p>
     * This is a flag the game sets and the damage calculation reads, so it is the same question
     * as "will this do bonus damage" rather than anything about the arrow's appearance. The
     * game clears it when the arrow sticks in the ground, so an arrow that has landed reads
     * {@code false} whatever it was shot as.
     * example:
     * <pre>
     * const ArrowEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.ArrowEntityHelper");
     * const arrows = World.getEntities(32, "arrow");
     * if (arrows !== null) {
     *   for (const entity of arrows) {
     *     const arrow = ArrowEntityHelper.class.cast(entity);
     *     if (arrow.isCritical()) {
     *       Chat.log("a critical arrow, so it will do more than a normal one");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the arrow will deal critical damage, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCritical() {
        return base.isCritArrow();
    }

    /**
     * The piercing level will only be set if the arrow was fired from a crossbow with the piercing
     * enchantment.
     *
     * The level is how many more entities the arrow can pass through before it stops, so
     * {@code 0} is an ordinary arrow and {@code 1} goes through one and is then spent. The game
     * works the number out from the crossbow that fired it when the arrow is created, and only
     * on the server, so an arrow that was fired by anything else is {@code 0} however it
     * reaches the client. The game also clears it when the arrow lands, so a spent arrow stuck
     * in the ground reads {@code 0}.
     * example:
     * <pre>
     * const ArrowEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.ArrowEntityHelper");
     * const arrows = World.getEntities(32, "arrow");
     * if (arrows !== null) {
     *   for (const entity of arrows) {
     *     const arrow = ArrowEntityHelper.class.cast(entity);
     *     const pierce = arrow.getPiercingLevel();
     *     if (pierce > 0) {
     *       Chat.log(`goes through ${pierce} more before it stops`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the piercing level of the arrow.
     * @since 1.8.4
     */
    public int getPiercingLevel() {
        return base.getPierceLevel();
    }

}
