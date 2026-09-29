package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.sheep.Sheep;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.DyeColorHelper;

/**
 * the sheep, which comes in sixteen colours and one very special name.
 * <p>
 * {@link #isSheared() isSheared} and {@link #isShearable() isShearable} answer different
 * questions and one of them is the negative of the other plus two more conditions: a sheep
 * can be grown up and still not shearable because its fleece is already off. The method of
 * the same name on a mooshroom and on a snow golem each ask for something different again.
 * <p>
 * {@link #isJeb() isJeb} is not a colour. A rainbow sheep is one whose <em>name</em> is
 * exactly {@code jeb_}, and the name has to have been set deliberately; a sheep merely
 * called something with those letters in it will not do it.
 * example:
 * <pre>
 * const SheepEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.SheepEntityHelper");
 * const sheep = World.getEntities(32, "sheep");
 * if (sheep !== null) {
 *   for (const entity of sheep) {
 *     const ewe = SheepEntityHelper.class.cast(entity);
 *     Chat.log(`${ewe.getColor().getName()}, sheared ${ewe.isSheared()}, `
 *       + `shearable ${ewe.isShearable()}, rainbow ${ewe.isJeb()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class SheepEntityHelper extends AnimalEntityHelper<Sheep> {

    public SheepEntityHelper(Sheep base) {
        super(base);
    }

    /**
     * Whether the fleece has been taken off. The game keeps this separate from the wool
     * colour, so a sheared sheep still has the colour it would grow back into rather than a
     * blank one.
     *
     * @return {@code true} if this sheep is sheared, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSheared() {
        return base.isSheared();
    }

    /**
     * Whether the sheep is alive, grown up and still has its fleece on - three conditions,
     * where {@link #isSheared() isSheared} asks about one of them and nothing else.
     * <p>
     * The same method name on a mooshroom asks only whether the mooshroom is alive and grown
     * up, because there is no fleece on it, and on a snow golem it asks whether the golem is
     * alive and still has its pumpkin on.
     *
     * @return {@code true} if this sheep can be sheared, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isShearable() {
        return base.readyForShearing();
    }

    /**
     * The colour of the sheep's wool, which is the colour it will be drawn in whether or not
     * it is sheared and whether or not it is a rainbow one.
     * example:
     * <pre>
     * const SheepEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.SheepEntityHelper");
     * const sheep = World.getEntities(24, "sheep");
     * if (sheep !== null) {
     *   for (const entity of sheep) {
     *     const ewe = SheepEntityHelper.class.cast(entity);
     *     if (!ewe.isJeb()) {
     *       Chat.log(`a plain ${ewe.getColor().getName()} sheep`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the color of this sheep.
     * @since 1.8.4
     */
    public DyeColorHelper getColor() {
        return new DyeColorHelper(base.getColor());
    }

    /**
     * Sheep named {@code jeb_} will cycle through all colors when rendered. If sheared, they will
     * drop their original colored wool.
     * <p>
     * The test is an exact match on the name rather than something looser: the sheep has to
     * have a custom name at all, and it has to be those four characters and nothing else.
     * The game's own renderer asks the same question in the same way, so this answers
     * {@code true} for exactly the sheep that are drawn cycling.
     *
     * @return {@code true} if the sheep has a rainbow overlay, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isJeb() {
        return base.hasCustomName() && "jeb_".equals(base.getName().getString());
    }

}
