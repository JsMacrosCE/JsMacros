package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Slime;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * the slime, which is one mob at several different sizes rather than several mobs.
 * <p>
 * A slime spawns at a size the game picks from 1, 2 or 4, and a slime above 1 splits into
 * two to four of half its size when it dies, so {@link #getSize() getSize} is the number
 * everything else about a slime hangs off: the game sets the slime's health to the square
 * of it, its attack damage to it, and its movement speed to 0.2 plus a tenth of it.
 * {@link #isSmall() isSmall} is that same number read the other way rather than a separate
 * reading of the slime.
 * example:
 * <pre>
 * const SlimeEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.SlimeEntityHelper");
 * const slimes = World.getEntities(32, "slime");
 * if (slimes !== null) {
 *   for (const entity of slimes) {
 *     const slime = SlimeEntityHelper.class.cast(entity);
 *     // a size one slime is the harmless one, and the one a bigger one splits into
 *     if (!slime.isSmall()) {
 *       Chat.log(`a slime at ${slime.getPos()} is size ${slime.getSize()}, ${slime.getMaxHealth()} health`);
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
public class SlimeEntityHelper extends MobEntityHelper<Slime> {

    public SlimeEntityHelper(Slime base) {
        super(base);
    }

    /**
     * The size the slime is currently at, and the number it spawns at, splits down from and
     * is drawn to scale from. The game never lets it below 1, so a freshly spawned slime is
     * the smallest one there is rather than a zero.
     * <p>
     * The game sets the rest of the slime off this one number when it changes: health to the
     * square of the size, attack damage to the size itself, and movement speed to 0.2 plus a
     * tenth of it. Reading those through the inherited calls therefore saves working the
     * arithmetic out again.
     * example:
     * <pre>
     * const SlimeEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.SlimeEntityHelper");
     * const slimes = World.getEntities(32, "slime");
     * if (slimes !== null) {
     *   for (const entity of slimes) {
     *     const slime = SlimeEntityHelper.class.cast(entity);
     *     const size = slime.getSize();
     *     // the health is the size squared, which is a cheap way to spot a size one
     *     if (slime.getMaxHealth() !== size * size) {
     *       Chat.log(`a slime at ${slime.getPos()} is size ${size} but has ${slime.getMaxHealth()} health`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the size of this slime.
     * @since 1.8.4
     */
    public int getSize() {
        return base.getSize();
    }

    /**
     * Small slimes, at a size of 1, don't deal damage.
     * <p>
     * The game gates the slime's touch damage on not being tiny and on its AI being
     * effective, so a small slime hops around a player and can still pick it as a target —
     * it just does nothing when it lands. That makes this the same number
     * {@link #getSize() getSize} reports read the other way, and the boundary is 1: a
     * slime at size 1 is still small, and one at size 2 or more is not.
     * example:
     * <pre>
     * const SlimeEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.SlimeEntityHelper");
     * const slimes = World.getEntities(16, "slime");
     * if (slimes !== null) {
     *   for (const entity of slimes) {
     *     const slime = SlimeEntityHelper.class.cast(entity);
     *     if (slime.isSmall()) {
     *       // harmless on contact, so it is the one worth ignoring
     *       Chat.log(`a harmless slime at ${slime.getPos()}, size ${slime.getSize()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this slime is small, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSmall() {
        return base.isTiny();
    }

}
