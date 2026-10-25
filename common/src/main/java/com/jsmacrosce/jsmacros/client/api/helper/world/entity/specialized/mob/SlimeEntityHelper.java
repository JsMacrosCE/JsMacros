package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Slime;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * Provides access to a slime's size and whether it is tiny.
 * <pre>
 * const slimes = World.getEntities(32, "slime");
 * if (slimes !== null) {
 *   for (const entity of slimes) {
 *     if (!(entity instanceof EntityHelpers.Slime)) continue;
 *     // Tiny slimes do not deal melee damage; larger slimes can split when killed.
 *     if (!entity.isSmall()) {
 *       Chat.log(`a slime at ${entity.getPos()} is size ${entity.getSize()}, ${entity.getMaxHealth()} health`);
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
public class SlimeEntityHelper extends MobEntityHelper<Slime> {

    /**
     * Wraps the supplied slime and retains its reference for this helper.
     *
     * @param base slime entity to wrap
     */
    public SlimeEntityHelper(Slime base) {
        super(base);
    }

    /**
     * Returns the slime's current size. Its maximum health is the square of its size, and its attack damage equals its size.
     *
     * @return the slime's size.
     * @since 1.8.4
     */
    public int getSize() {
        return base.getSize();
    }

    /**
     * Returns whether this slime is tiny (size 1 or less); tiny slimes do not deal melee damage.
     *
     * @return {@code true} if the slime is tiny; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSmall() {
        return base.isTiny();
    }

}
