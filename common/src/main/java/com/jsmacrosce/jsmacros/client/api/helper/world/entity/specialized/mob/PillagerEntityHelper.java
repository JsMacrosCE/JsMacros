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
 * the pillager, which is a raider with a crossbow and, sometimes, a captain's banner.
 * <p>
 * Everything a raider has is on {@code IllagerEntityHelper} and reached through it, so the
 * arm pose and the celebration flag are still one call away. What this adds is
 * {@link #isCaptain() isCaptain}, which is the raid role rather than a mood.
 * example:
 * <pre>
 * const PillagerEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.PillagerEntityHelper");
 * const pillagers = World.getEntities(32, "pillager");
 * if (pillagers !== null) {
 *   for (const entity of pillagers) {
 *     const pillager = PillagerEntityHelper.class.cast(entity);
 *     if (pillager.isCaptain()) {
 *       // a captain is the one wearing the banner, and the raid buffs ride on that
 *       Chat.log(`a captain at ${pillager.getPos()}, pose ${pillager.getState()}`);
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

    public PillagerEntityHelper(Pillager base) {
        super(base);
    }

    /**
     * Whether the pillager is a raid captain, which the game decides by what the pillager
     * has on: the check is for a white banner in the head slot, so it follows the
     * equipment rather than a separate flag.
     * <p>
     * That means it follows the equipment rather than a separate flag: any pillager wearing
     * a white banner on its head answers {@code true}, whether or not there is a raid
     * running at that moment and whether or not anything put the banner there.
     * example:
     * <pre>
     * const PillagerEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.PillagerEntityHelper");
     * const pillagers = World.getEntities(32, "pillager");
     * if (pillagers !== null) {
     *   for (const entity of pillagers) {
     *     const pillager = PillagerEntityHelper.class.cast(entity);
     *     if (pillager.isCaptain()) {
     *       // the banner is on its head, which is what the check looks at
     *       Chat.log(`a captain at ${pillager.getPos()} wearing ${pillager.getHeadArmor().getName()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this pillager is a captain, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isCaptain() {
        return base.getItemBySlot(EquipmentSlot.HEAD).is(Items.WHITE_BANNER);
    }

}
