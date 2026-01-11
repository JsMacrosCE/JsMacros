package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Witch;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class WitchEntityHelper extends MobEntityHelper<Witch> {

    public WitchEntityHelper(Witch base) {
        super(base);
    }

    /**
     * @return {@code true} if this witch is drinking a potion, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isDrinkingPotion() {
        return base.isDrinkingPotion();
    }

    /**
     * @return the held potion item.
     * @since 1.8.4
     */
    public ItemStackHelper getPotion() {
        return getMainHand();
    }

}
