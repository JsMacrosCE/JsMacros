package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Vex;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class VexEntityHelper extends MobEntityHelper<Vex> {

    public VexEntityHelper(Vex base) {
        super(base);
    }

    /**
     * @return {@code true} if this vex is currently charging at its target, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isCharging() {
        return base.isCharging();
    }

}
