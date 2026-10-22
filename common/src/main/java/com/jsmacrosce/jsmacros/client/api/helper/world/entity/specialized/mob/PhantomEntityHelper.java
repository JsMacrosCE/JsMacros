package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Phantom;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * the phantom, and the one number the game varies on it.
 * <p>
 * Phantoms carry a size, and {@link #getSize() getSize} is that number. The game uses it
 * to resize the phantom's hitbox and to set its base attack damage to 6 plus the size, so
 * a size is also a read of how hard a phantom hits.
 * <p>
 * In the current game a spawned phantom is always set to 0, so this reads 0 on an ordinary
 * one; the field is kept because the size is part of the phantom's saved state and other
 * things can set it, and the game clamps it to 0 through 64 when they do.
 * example:
 * <pre>
 * const PhantomEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.PhantomEntityHelper");
 * const phantoms = World.getEntities(64, "phantom");
 * if (phantoms !== null) {
 *   for (const entity of phantoms) {
 *     const phantom = PhantomEntityHelper.class.cast(entity);
 *     // an ordinary phantom reads 0 here, and the size is what sizes its hitbox
 *     if (phantom.getSize() !== 0) {
 *       Chat.log(`a phantom at ${phantom.getPos()} is size ${phantom.getSize()}`);
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
public class PhantomEntityHelper extends MobEntityHelper<Phantom> {

    public PhantomEntityHelper(Phantom base) {
        super(base);
    }

    /**
     * The size the phantom is at, which the game syncs on it and clamps to 0 through 64.
     * Every phantom the game spawns is set to 0, and the number is restored from the
     * phantom's saved state rather than worked out from the world, so an ordinary phantom
     * answers 0 and a different reading is one something set deliberately.
     * <p>
     * When the size does change the game resizes the phantom's hitbox to match and sets
     * its base attack damage to 6 plus the size, so the size doubles as a read of how hard
     * the phantom hits.
     * example:
     * <pre>
     * const PhantomEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.PhantomEntityHelper");
     * const phantoms = World.getEntities(64, "phantom");
     * if (phantoms !== null) {
     *   for (const entity of phantoms) {
     *     const phantom = PhantomEntityHelper.class.cast(entity);
     *     const size = phantom.getSize();
     *     // the base attack damage the game derives from the size
     *     Chat.log(`size ${size} at ${phantom.getPos()}, ${size + 6} base damage`);
     *   }
     * }
     * </pre>
     *
     * @return the size of this phantom.
     * @since 1.8.4
     */
    public int getSize() {
        return base.getPhantomSize();
    }

}
