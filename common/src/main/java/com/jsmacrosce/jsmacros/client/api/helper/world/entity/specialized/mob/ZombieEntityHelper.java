package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.monster.zombie.Zombie;
*///? } else {
import net.minecraft.world.entity.monster.Zombie;
//?}

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
