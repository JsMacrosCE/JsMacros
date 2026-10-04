package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.fish.AbstractFish;
*///? } else {
import net.minecraft.world.entity.animal.AbstractFish;
//? }

/**
 * the fish as a whole: the pufferfish, the tropical fish, and everything else the game files
 * under a fish, with the one thing they have in common being a bucket.
 * <p>
 * Nothing here is fish-shaped beyond that. What a particular fish can do - puff up, dance,
 * carry a pattern - is on the class for that fish.
 * example:
 * <pre>
 * const FishEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.FishEntityHelper");
 * const fish = World.getEntities(32, "pufferfish", "tropical_fish", "cod", "salmon");
 * if (fish !== null) {
 *   for (const entity of fish) {
 *     const f = FishEntityHelper.class.cast(entity);
 *     if (f.isFromBucket()) {
 *       Chat.log(`that ${f.getType()} was not released, it was fished out`);
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
public class FishEntityHelper<T extends AbstractFish> extends MobEntityHelper<T> {

    public FishEntityHelper(T base) {
        super(base);
    }

    /**
     * Whether this fish was fished out of a bucket rather than found in the water. The game
     * keeps the flag for good and will not let such a fish despawn, so it is also the way to
     * tell a released fish from one that is still somebody's pet.
     *
     * @return {@code true} if this fish came from a bucket, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isFromBucket() {
        return base.fromBucket();
    }

}
