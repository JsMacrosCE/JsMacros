package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.monster.spider.Spider;
*///? } else {
import net.minecraft.world.entity.monster.Spider;
//? }

/**
 * the spider, which is the one hostile mob that climbs.
 * <p>
 * A spider can walk up a wall the way a player can, and {@link #isClimbing() isClimbing}
 * is the flag the game keeps for it, synced from the server with everything else. Nothing
 * else about a spider is exposed here, so this is a thin class on purpose.
 * example:
 * <pre>
 * const SpiderEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.SpiderEntityHelper");
 * const spiders = World.getEntities(16, "spider");
 * if (spiders !== null) {
 *   for (const entity of spiders) {
 *     const spider = SpiderEntityHelper.class.cast(entity);
 *     // a climbing spider is off the floor, which a script walking the ground can miss
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

    public SpiderEntityHelper(Spider base) {
        super(base);
    }

    /**
     * Whether the spider is climbing. The game keeps this as a bit in the spider's own flag
     * byte rather than deriving it from where it is, so it is a state the spider is in
     * rather than a question about the block it happens to be next to.
     * <p>
     * The call this makes is the general "is it on something climbable" question rather
     * than a spider-only one, and a spider answers it from its own flag.
     * example:
     * <pre>
     * const SpiderEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.SpiderEntityHelper");
     * const spiders = World.getEntities(16, "spider");
     * if (spiders !== null) {
     *   for (const entity of spiders) {
     *     const spider = SpiderEntityHelper.class.cast(entity);
     *     if (spider.isClimbing()) {
     *       // a climbing spider is off the floor, so a ground-level sweep walks past it
     *       Chat.log(`a climbing spider at y ${spider.getY()}, x ${spider.getX()}, z ${spider.getZ()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this spider is currently climbing a wall, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isClimbing() {
        return base.onClimbable();
    }

}
