package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Ghast;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * Provides access to whether a ghast is charging its fireball attack.
 * <pre>
 * const ghasts = World.getEntities(48, "ghast");
 * if (ghasts !== null) {
 *   for (const entity of ghasts) {
 *     if (!(entity instanceof EntityHelpers.Ghast)) continue;
 *     if (entity.isShooting()) Chat.log(`a ghast at ${entity.getPos()} is charging`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class GhastEntityHelper extends MobEntityHelper<Ghast> {

    /** Wraps the supplied ghast and retains its reference for this helper.
     *
     * @param base ghast entity to wrap
     */
    public GhastEntityHelper(Ghast base) {
        super(base);
    }

    /**
     * Returns whether the ghast's fireball charge flag is set. The flag is set when charge
     * time is greater than 10 ticks. Firing at 20 ticks resets the counter to -40 and clears
     * the flag for the cooldown; stopping the attack goal also clears it.
     *
     * @return {@code true} while the charge flag is set; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isShooting() {
        return base.isCharging();
    }

}
