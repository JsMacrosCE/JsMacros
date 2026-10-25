package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Phantom;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * Provides access to a phantom's size, which scales its dimensions and sets its base attack damage to 6 plus the size.
 * <pre>
 * const phantoms = World.getEntities(64, "phantom");
 * if (phantoms !== null) {
 *   for (const entity of phantoms) {
 *     if (!(entity instanceof EntityHelpers.Phantom)) continue;
 *     // An ordinary phantom has size 0.
 *     if (entity.getSize() !== 0) {
 *       Chat.log(`a phantom at ${entity.getPos()} is size ${entity.getSize()}`);
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

    /**
     * Wraps the supplied phantom and retains its reference for this helper.
     *
     * @param base phantom entity to wrap
     */
    public PhantomEntityHelper(Phantom base) {
        super(base);
    }

    /**
     * Returns the phantom's size, a value from 0 to 64. This value scales its dimensions and sets its base attack damage to 6 plus the size.
     *
     * @return this phantom's size.
     * @since 1.8.4
     */
    public int getSize() {
        return base.getPhantomSize();
    }

}
