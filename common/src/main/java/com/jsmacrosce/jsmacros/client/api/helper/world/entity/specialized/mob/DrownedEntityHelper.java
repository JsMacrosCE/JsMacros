package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.item.Items;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class DrownedEntityHelper extends ZombieEntityHelper<Drowned> {

    public DrownedEntityHelper(Drowned base) {
        super(base);
    }

    /**
     * @return {@code true} if this drowned is holding a trident, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasTrident() {
        return base.isHolding(Items.TRIDENT);
    }

    /**
     * @return {@code true} if this drowned is holding a nautilus shell, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasNautilusShell() {
        return base.isHolding(Items.NAUTILUS_SHELL);
    }

}
