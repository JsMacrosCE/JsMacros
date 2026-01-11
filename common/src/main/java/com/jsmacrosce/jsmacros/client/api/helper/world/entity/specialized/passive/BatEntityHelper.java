package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.ambient.Bat;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class BatEntityHelper extends MobEntityHelper<Bat> {

    public BatEntityHelper(Bat base) {
        super(base);
    }

    /**
     * @return {@code true} if the bat is hanging upside down, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isResting() {
        return base.isResting();
    }

}
