package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.golem.SnowGolem;
*///? } else {
import net.minecraft.world.entity.animal.SnowGolem;
//? }

/**
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class SnowGolemEntityHelper extends MobEntityHelper<SnowGolem> {

    public SnowGolemEntityHelper(SnowGolem base) {
        super(base);
    }

    /**
     * @return {@code true} if the snow golem has a pumpkin on its head, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasPumpkin() {
        return base.hasPumpkin();
    }

    /**
     * @return {@code true} if this snow golem can be sheared, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isShearable() {
        return base.readyForShearing();
    }

}
