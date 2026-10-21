package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.goat.Goat;
import com.jsmacrosce.doclet.DocletCategory;

/**
 * Provides access to whether a goat is a screaming goat and which horns remain.
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

    /**
     * Wraps the supplied goat and retains its reference for this helper.
     *
     * @param base the goat entity to wrap
     */
    public GoatEntityHelper(Goat base) {
        super(base);
    }

    /**
     * Returns whether this goat is a screaming goat.
     *
     * @return {@code true} if this goat is a screaming goat; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isScreaming() {
        return base.isScreamingGoat();
    }

    /**
     * Returns whether this goat has its left horn.
     *
     * @return {@code true} if this goat has its left horn; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasLeftHorn() {
        return base.hasLeftHorn();
    }

    /**
     * Returns whether this goat has its right horn.
     *
     * @return {@code true} if this goat has its right horn; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasRightHorn() {
        return base.hasRightHorn();
    }

    /**
     * Returns whether either horn remains.
     *
     * @return {@code true} if this goat still has a horn, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasHorns() {
        return hasLeftHorn() || hasRightHorn();
    }

}
