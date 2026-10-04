package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.item.Items;
import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.monster.zombie.Drowned;
*///? } else {
import net.minecraft.world.entity.monster.Drowned;
//? }

/**
 * the drowned, which is a zombie that has already been through its conversion and carries
 * the two things a drowned can be holding.
 * <p>
 * Both calls here look in both hands rather than just the main one, which is what the
 * question is worth asking: a drowned holding a trident in either hand answers
 * {@code true}. The conversion the zombie class exposes is off on a drowned, so that flag
 * never comes up here — the class is reached through {@code ZombieEntityHelper} and the
 * calls are there to use.
 * example:
 * <pre>
 * const DrownedEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.DrownedEntityHelper");
 * const drowned = World.getEntities(32, "drowned");
 * if (drowned !== null) {
 *   for (const entity of drowned) {
 *     const zombie = DrownedEntityHelper.class.cast(entity);
 *     if (zombie.hasTrident()) {
 *       // either hand counts, so the off hand is worth reading too
 *       Chat.log(`a trident at ${zombie.getPos()}, main hand ${zombie.getMainHand().getName().getString()}`);
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
public class DrownedEntityHelper extends ZombieEntityHelper<Drowned> {

    public DrownedEntityHelper(Drowned base) {
        super(base);
    }

    /**
     * Whether the drowned has a trident on it, in either hand. A trident is what the
     * drowned throws, so this is the check for "is this one armed" rather than a check of
     * what it is currently doing with it.
     * example:
     * <pre>
     * const DrownedEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.DrownedEntityHelper");
     * const drowned = World.getEntities(32, "drowned");
     * if (drowned !== null) {
     *   for (const entity of drowned) {
     *     const zombie = DrownedEntityHelper.class.cast(entity);
     *     if (zombie.hasTrident()) {
     *       Chat.log(`an armed drowned at ${zombie.getPos()}, health ${zombie.getHealth()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this drowned is holding a trident, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasTrident() {
        return base.isHolding(Items.TRIDENT);
    }

    /**
     * Whether the drowned has a nautilus shell on it, in either hand. The game refuses to
     * swap the shell for something picked up off the ground, so a drowned carrying one is
     * not going to replace it, which is what makes the shell worth checking for.
     * example:
     * <pre>
     * const DrownedEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.DrownedEntityHelper");
     * const drowned = World.getEntities(32, "drowned");
     * if (drowned !== null) {
     *   for (const entity of drowned) {
     *     const zombie = DrownedEntityHelper.class.cast(entity);
     *     if (zombie.hasNautilusShell()) {
     *       // a shell-holder will not pick anything else up off the floor
     *       Chat.log(`a looting drowned at ${zombie.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this drowned is holding a nautilus shell, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean hasNautilusShell() {
        return base.isHolding(Items.NAUTILUS_SHELL);
    }

}
