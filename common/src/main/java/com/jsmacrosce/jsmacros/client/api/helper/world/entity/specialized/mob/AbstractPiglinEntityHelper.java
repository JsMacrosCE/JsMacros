package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinAbstractPiglinEntity;

/**
 * Provides the shared zombification-immunity query for piglins.
 * <p>
 * <pre>
 * const brute = World.getEntities(32, "piglin_brute");
 * if (brute !== null) {
 *   for (const entity of brute) {
 *     // a brute reaches a script as this class, a piglin as PiglinEntityHelper
 *     if (!(entity instanceof EntityHelpers.AbstractPiglin)) continue;
 *     if (entity.canBeZombified()) {
 *       Chat.log(`the brute at ${entity.getPos()} carries no immunity flag`);
 *     }
 *   }
 * }
 * </pre>
 *
 * @param <T> the piglin type wrapped by this helper
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class AbstractPiglinEntityHelper<T extends AbstractPiglin> extends MobEntityHelper<T> {

    /** Wraps the supplied piglin entity and retains its reference for this helper.
     * @param base piglin entity to wrap
     */
    public AbstractPiglinEntityHelper(T base) {
        super(base);
    }

    /**
     * Whether this piglin's immunity-to-zombification flag is clear. This reports the flag,
     * not whether conversion will occur; conversion also depends on the dimension and AI state.
     *
     * @return {@code true} if the immunity flag is clear; {@code false} if it is set.
     * @since 1.8.4
     */
    public boolean canBeZombified() {
        return !((MixinAbstractPiglinEntity) base).invokeIsImmuneToZombification();
    }

}
