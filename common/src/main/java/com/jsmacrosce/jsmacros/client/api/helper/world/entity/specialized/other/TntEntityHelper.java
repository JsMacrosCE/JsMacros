package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other;

import net.minecraft.world.entity.item.PrimedTnt;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * a primed block of TNT, the entity a lit TNT block becomes for the four seconds before it
 * goes off.
 * <p>
 * There is no unprimed state: a TNT block that has not been lit is not this entity at all, so
 * a script that finds one has already found something that is about to explode. What is worth
 * reading is the fuse, and it is the only call here, because the entity has nothing else of its
 * own: the position and the entity id come from the shared helpers.
 * example:
 * <pre>
 * const TntEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other.TntEntityHelper");
 * const bombs = World.getEntities(32, "tnt");
 * if (bombs !== null) {
 *   for (const entity of bombs) {
 *     const tnt = TntEntityHelper.class.cast(entity);
 *     // it has already been lit, so the only question is how long is left
 *     Chat.log(`goes off in ${(tnt.getRemainingTime() / 20).toFixed(1)} seconds`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class TntEntityHelper extends EntityHelper<PrimedTnt> {

    public TntEntityHelper(PrimedTnt base) {
        super(base);
    }

    /**
     * how many ticks are left before this TNT explodes, counting the current tick, and the
     * default fuse is 80 ticks.
     * <p>
     * The number comes down by one every tick and the entity is gone once it reaches zero, so a
     * value of 0 means it is exploding this tick rather than that it is a live bomb. 80 ticks is
     * four seconds at twenty to the second, which is what a TNT block lit with a flint and
     * steel gets; the game lets a command or a datapack set it to something else and the value
     * is stored with the entity, so it survives a chunk reload. There is no unprimed value to
     * see here, since an unlit TNT block is not an entity.
     * example:
     * <pre>
     * const TntEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other.TntEntityHelper");
     * const bombs = World.getEntities(32, "tnt");
     * if (bombs !== null) {
     *   for (const entity of bombs) {
     *     const tnt = TntEntityHelper.class.cast(entity);
     *     const left = tnt.getRemainingTime();
     *     // a value of 0 is exploding now, not a bomb that is safe to stand next to
     *     if (20 > left) {
     *       Chat.log(`about a second left at ${entity.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the remaining time until this TNT explodes.
     * @since 1.8.4
     */
    public int getRemainingTime() {
        return base.getFuse();
    }

}
