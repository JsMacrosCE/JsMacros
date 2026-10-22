package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Vex;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * the vex, whose only attack the game syncs a flag for is the charge at its target.
 * <p>
 * {@link #isCharging() isCharging} is raised as it commits to that charge and lowered when
 * the charge lands or the target dies, so the flag is both "about to hit" and "has not hit
 * yet" in one.
 * example:
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

    public VexEntityHelper(Vex base) {
        super(base);
    }

    /**
     * Whether the vex is charging at its target. The game raises this as its charge goal
     * starts, which is also when it plays the charge sound, and lowers it when the charge
     * ends — including on the tick the charge actually connects, so this is a flag about
     * the approach rather than about damage.
     * <p>
     * The flag is one bit in the vex's own flag byte, so it is a state the vex is in rather
     * than a reading of how far away its target is.
     * example:
     * <pre>
     * const VexEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.VexEntityHelper");
     * const vexes = World.getEntities(32, "vex");
     * if (vexes !== null) {
     *   for (const entity of vexes) {
     *     const vex = VexEntityHelper.class.cast(entity);
     *     if (vex.isCharging()) {
     *       // the charge lands and clears the flag, so this is the run-up only
     *       Chat.log(`a vex at ${vex.getPos()} is mid-charge, health ${vex.getHealth()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this vex is currently charging at its target, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isCharging() {
        return base.isCharging();
    }

}
