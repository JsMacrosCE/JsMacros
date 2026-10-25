package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.fish.AbstractFish;
*///? } else {
import net.minecraft.world.entity.animal.AbstractFish;
//? }

/**
 * Helper for fish entities. Fish-specific behavior is documented on the corresponding
 * specialized helper.
 * <pre>
 * const fish = World.getEntities(32, "pufferfish", "tropical_fish", "cod", "salmon");
 * if (fish !== null) {
 *   for (const entity of fish) {
 *     if (!(entity instanceof EntityHelpers.Fish)) continue;
 *     if (entity.isFromBucket()) {
 *       Chat.log(`that ${entity.getType()} was not released, it was fished out`);
 *     }
 *   }
 * }
 * </pre>
 *
 * @param <T> the fish entity type wrapped by this helper
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class FishEntityHelper<T extends AbstractFish> extends MobEntityHelper<T> {

    /**
     * Wraps the supplied fish entity and retains its reference for this helper.
     *
     * @param base fish entity to wrap
     */
    public FishEntityHelper(T base) {
        super(base);
    }

    /**
     * Whether this fish has its from-bucket flag set. Fish with this flag are not removed
     * when far away and require custom persistence.
     *
     * @return {@code true} if this fish came from a bucket, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isFromBucket() {
        return base.fromBucket();
    }

}
