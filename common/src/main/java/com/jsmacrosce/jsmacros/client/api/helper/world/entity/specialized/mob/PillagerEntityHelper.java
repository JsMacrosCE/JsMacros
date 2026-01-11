package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.item.Items;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class PillagerEntityHelper extends IllagerEntityHelper<Pillager> {

    public PillagerEntityHelper(Pillager base) {
        super(base);
    }

    /**
     * @return {@code true} if this pillager is a captain, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCaptain() {
        return base.getItemBySlot(EquipmentSlot.HEAD).is(Items.WHITE_BANNER);
    }

}
