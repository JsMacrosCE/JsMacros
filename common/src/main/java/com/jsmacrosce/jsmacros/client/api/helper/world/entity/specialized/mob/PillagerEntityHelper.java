package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.monster.illager.Pillager;
*///? } else {
import net.minecraft.world.entity.monster.Pillager;
//? }

/**
 * Provides access to a pillager's entity-helper API, including a check for a white banner in its head slot.
 * <pre>
 * const PillagerEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.PillagerEntityHelper");
 * const pillagers = World.getEntities(32, "pillager");
 * if (pillagers !== null) {
 *   for (const entity of pillagers) {
 *     const pillager = PillagerEntityHelper.class.cast(entity);
 *     if (pillager.isCaptain()) {
 *       Chat.log(`a pillager with a white head-slot banner at ${pillager.getPos()}`);
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
public class PillagerEntityHelper extends IllagerEntityHelper<Pillager> {

    /** Wraps the supplied pillager and retains its reference for this helper.
     * @param base pillager entity to wrap
     */
    public PillagerEntityHelper(Pillager base) {
        super(base);
    }

    /**
     * Returns whether the pillager's head slot contains a white banner; this checks its equipment, not a separate captain status.
     *
     * @return {@code true} if its head slot contains a white banner; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCaptain() {
        //? if >=26.2 {
        /*return base.getItemBySlot(EquipmentSlot.HEAD).is(Items.BANNER.white());
        *///?} else {
        return base.getItemBySlot(EquipmentSlot.HEAD).is(Items.WHITE_BANNER);
        //?}
    }

}
