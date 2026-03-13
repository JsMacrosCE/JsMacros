package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.monster.spider.Spider;
*///? } else {
import net.minecraft.world.entity.monster.Spider;
//?}

/**
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class SpiderEntityHelper extends MobEntityHelper<Spider> {

    public SpiderEntityHelper(Spider base) {
        super(base);
    }

    /**
     * @return {@code true} if this spider is currently climbing a wall, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isClimbing() {
        return base.onClimbable();
    }

}
