package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Vex;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * Provides access to a vex's charge state. {@link #isCharging()} is set when its charge
 * goal starts and cleared when that goal ends; it does not indicate that damage has landed.
 * <pre>
 * const VexEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.VexEntityHelper");
 * const vexes = World.getEntities(32, "vex");
 * if (vexes !== null) {
 *   for (const entity of vexes) {
 *     const vex = VexEntityHelper.class.cast(entity);
 *     if (vex.isCharging()) {
 *       Chat.log(`a vex at ${vex.getPos()} is closing on something`);
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
public class VexEntityHelper extends MobEntityHelper<Vex> {

    /**
     * Wraps the supplied vex and retains its reference for this helper.
     *
     * @param base vex entity to wrap
     */
    public VexEntityHelper(Vex base) {
        super(base);
    }

    /**
     * Returns whether the vex's charge goal is active. The flag is set when the goal starts
     * and cleared when it stops or when the charge connects; it does not indicate damage.
     *
     * @return {@code true} while the charge flag is set; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCharging() {
        return base.isCharging();
    }

}
