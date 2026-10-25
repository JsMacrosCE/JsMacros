package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.ambient.Bat;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * Provides access to a bat's resting state.
 * <pre>
 * const bats = World.getEntities(24, "bat");
 * if (bats !== null) {
 *   for (const entity of bats) {
 *     if (!(entity instanceof EntityHelpers.Bat)) continue;
 *     if (!entity.isResting()) {
 *       Chat.log(`a bat out at ${entity.getPos()}`);
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
public class BatEntityHelper extends MobEntityHelper<Bat> {

    /** Wraps the bat and retains its reference.
     * @param base bat entity to wrap
     */
    public BatEntityHelper(Bat base) {
        super(base);
    }

    /**
     * Returns whether the bat is resting.
     *
     * @return {@code true} when the bat is resting; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isResting() {
        return base.isResting();
    }

}
