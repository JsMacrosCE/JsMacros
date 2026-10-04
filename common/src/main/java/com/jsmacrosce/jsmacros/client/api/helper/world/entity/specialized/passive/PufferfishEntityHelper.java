package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.fish.Pufferfish;
*///? } else {
import net.minecraft.world.entity.animal.Pufferfish;
//?}

/**
 * the pufferfish, which is a fish that can double and then double again in size.
 * <p>
 * {@link #getSize() getSize} is the state the game actually uses for the hitbox, so the
 * number is worth reading before measuring anything around a pufferfish: a fully inflated
 * one is much bigger than the fish that swims about normally.
 * example:
 * <pre>
 * const PufferfishEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.PufferfishEntityHelper");
 * const puffers = World.getEntities(32, "pufferfish");
 * if (puffers !== null) {
 *   for (const entity of puffers) {
 *     const puffer = PufferfishEntityHelper.class.cast(entity);
 *     Chat.log(`${puffer.getSize()}: ${puffer.getPos()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class PufferfishEntityHelper extends FishEntityHelper<Pufferfish> {

    public PufferfishEntityHelper(Pufferfish base) {
        super(base);
    }

    /**
     * A state of 0 means the fish is deflated, a state of 1 means the fish is inflated and a state
     * of 2 means the fish is fully inflated.
     * <p>
     * The figure is not cosmetic: the game rebuilds the fish's size and hitbox from it, so
     * a pufferfish at two really is a different size in the world from the same fish at
     * zero. The game also clamps what it reads back out of a saved fish down to two, so a
     * pufferfish loaded from disk can never come back above it.
     * example:
     * <pre>
     * const PufferfishEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.PufferfishEntityHelper");
     * const puffers = World.getEntities(16, "pufferfish");
     * if (puffers !== null) {
     *   for (const entity of puffers) {
     *     const puffer = PufferfishEntityHelper.class.cast(entity);
     *     if (puffer.getSize() === 2) {
     *       Chat.log(`that one is fully puffed up, ${puffer.getEyeHeight()} blocks tall`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the puff state of this pufferfish.
     * @since 1.8.4
     */
    public int getSize() {
        return base.getPuffState();
    }

}
