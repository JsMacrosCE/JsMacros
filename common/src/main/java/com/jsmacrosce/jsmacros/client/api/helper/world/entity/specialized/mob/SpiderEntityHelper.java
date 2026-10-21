package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.monster.spider.Spider;
*///? } else {
import net.minecraft.world.entity.monster.Spider;
//? }

/**
 * Provides access to a spider's climbing state through {@link #isClimbing()}.
 * <pre>
 * const SpiderEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.SpiderEntityHelper");
 * const spiders = World.getEntities(16, "spider");
 * if (spiders !== null) {
 *   for (const entity of spiders) {
 *     const spider = SpiderEntityHelper.class.cast(entity);
 *     if (spider.isClimbing()) {
 *       Chat.log(`a spider at ${spider.getPos()} is up the wall`);
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
public class SpiderEntityHelper extends MobEntityHelper<Spider> {

    /**
     * Wraps the supplied spider and retains its reference for this helper.
     *
     * @param base spider entity to wrap
     */
    public SpiderEntityHelper(Spider base) {
        super(base);
    }

    /**
     * Returns whether the spider's climbing flag is set.
     *
     * @return {@code true} if the spider's climbing flag is set; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isClimbing() {
        return base.onClimbable();
    }

}
