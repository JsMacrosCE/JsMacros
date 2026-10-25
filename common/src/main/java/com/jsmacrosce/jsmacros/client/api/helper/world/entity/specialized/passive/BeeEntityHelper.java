package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.bee.Bee;
*///? } else {
import net.minecraft.world.entity.animal.Bee;
//?}

/**
 * Provides access to a bee's nectar, anger, and sting states.
 * <pre>
 * const bees = World.getEntities(32, "bee");
 * if (bees !== null) {
 *   for (const entity of bees) {
 *     if (!(entity instanceof EntityHelpers.Bee)) continue;
 *     Chat.log(`${entity.getType()} at ${entity.getPos()}: `
 *       + `nectar ${entity.hasNectar()}, `
 *       + `angry ${entity.isAngry()}, `
 *       + `stung ${entity.hasStung()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class BeeEntityHelper extends AnimalEntityHelper<Bee> {

    /**
     * Wraps the supplied bee and retains its reference for this helper.
     *
     * @param base bee entity to wrap
     */
    public BeeEntityHelper(Bee base) {
        super(base);
    }

    /**
     * Returns whether the bee currently has nectar.
     *
     * @return {@code true} if the bee has nectar; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasNectar() {
        return base.hasNectar();
    }

    /**
     * Returns whether the bee is angry.
     *
     * @return {@code true} if the bee is angry; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAngry() {
        return base.isAngry();
    }

    /**
     * Returns whether the bee has stung.
     *
     * @return {@code true} if the bee has stung; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasStung() {
        return base.hasStung();
    }

}
