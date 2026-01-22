package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Zombie;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class ZombieEntityHelper<T extends Zombie> extends MobEntityHelper<T> {

    public ZombieEntityHelper(T base) {
        super(base);
    }

    /**
     * @return {@code true} if this zombie is converting to a drowned, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isConvertingToDrowned() {
        return base.isUnderWaterConverting();
    }

}
