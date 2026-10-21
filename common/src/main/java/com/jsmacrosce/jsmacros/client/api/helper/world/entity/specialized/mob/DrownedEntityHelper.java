package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.item.Items;
import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.monster.zombie.Drowned;
*///? } else {
import net.minecraft.world.entity.monster.Drowned;
//? }

/**
 * Provides access to drowned-specific checks for held tridents and nautilus shells.
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

    /**
     * Wraps the supplied drowned entity and retains its reference for this helper.
     *
     * @param base drowned entity to wrap
     */
    public DrownedEntityHelper(Drowned base) {
        super(base);
    }

    /**
     * Returns whether the drowned holds a trident in either hand.
     *
     * @return {@code true} if this drowned is holding a trident, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasTrident() {
        return base.isHolding(Items.TRIDENT);
    }

    /**
     * Returns whether the drowned holds a nautilus shell in either hand.
     *
     * @return {@code true} if this drowned is holding a nautilus shell, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean hasNautilusShell() {
        return base.isHolding(Items.NAUTILUS_SHELL);
    }

}
