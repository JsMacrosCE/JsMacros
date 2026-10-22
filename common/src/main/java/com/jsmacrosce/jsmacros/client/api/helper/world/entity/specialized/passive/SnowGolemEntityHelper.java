package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.golem.SnowGolem;
*///? } else {
import net.minecraft.world.entity.animal.SnowGolem;
//? }

/**
 * the snow golem, which is two flags and not much else: whether it is wearing its pumpkin,
 * and whether that pumpkin is the thing in the way of shearing it.
 * <p>
 * The second is worth reading next to the sheep's own {@code isShearable()} and to the one
 * on {@link MooshroomEntityHelper}. All three ask whether shears would do something, and the
 * game has a different answer for each: a snow golem is shearable while it still has its
 * pumpkin, and stops being shearable the moment the pumpkin comes off.
 * example:
 * <pre>
 * const SnowGolemEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.SnowGolemEntityHelper");
 * const golems = World.getEntities(32, "snow_golem");
 * if (golems !== null) {
 *   for (const entity of golems) {
 *     const golem = SnowGolemEntityHelper.class.cast(entity);
 *     Chat.log(`pumpkin ${golem.hasPumpkin()}, shearable ${golem.isShearable()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class SnowGolemEntityHelper extends MobEntityHelper<SnowGolem> {

    public SnowGolemEntityHelper(SnowGolem base) {
        super(base);
    }

    /**
     * Whether the golem has a carved pumpkin on its head. A golem spawned into the world has
     * one, and a player can take it off with shears and put it back by hand. The game uses
     * the flag for what the golem does rather than for whether it defends itself: a golem
     * throws snowballs at whatever it has decided is an enemy whether or not it is wearing
     * anything, so the pumpkin is a question about the golem's look and not about its
     * behaviour.
     *
     * @return {@code true} if the snow golem has a pumpkin on its head, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasPumpkin() {
        return base.hasPumpkin();
    }

    /**
     * Whether shears would do anything to the golem, which is true exactly while it is alive
     * and still wearing its pumpkin. Take the pumpkin off and this goes {@code false} even
     * though the golem itself has not changed.
     * example:
     * <pre>
     * const SnowGolemEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.SnowGolemEntityHelper");
     * const golems = World.getEntities(32, "snow_golem");
     * if (golems !== null) {
     *   for (const entity of golems) {
     *     const golem = SnowGolemEntityHelper.class.cast(entity);
     *     if (golem.isShearable()) {
     *       Chat.log("shears would take that pumpkin off");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this snow golem can be sheared, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isShearable() {
        return base.readyForShearing();
    }

}
