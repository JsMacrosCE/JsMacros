package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.golem.IronGolem;
*///? } else {
import net.minecraft.world.entity.animal.IronGolem;
//?}

/**
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class IronGolemEntityHelper extends MobEntityHelper<IronGolem> {

    public IronGolemEntityHelper(IronGolem base) {
        super(base);
    }

    /**
     * @return {@code true} if this iron golem was created by a player, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPlayerCreated() {
        return base.isPlayerCreated();
    }

}
