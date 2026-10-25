package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.monster.zombie.Zombie;
*///? } else {
import net.minecraft.world.entity.monster.Zombie;
//? }

/**
 * Provides access to zombie-specific properties, including whether drowned conversion is active.
 * Drowned and zombie-villager helpers inherit this API, but those vanilla entity types do not
 * enter the underwater-conversion path and normally report {@code false}.
 * <pre>
 * const zombies = World.getEntities(32, "zombie");
 * if (zombies !== null) {
 *   for (const entity of zombies) {
 *     if (!(entity instanceof EntityHelpers.Zombie)) continue;
 *     if (entity.isConvertingToDrowned()) {
 *       Chat.log(`a zombie at ${entity.getPos()} is turning, with maximum health ${entity.getMaxHealth()}`);
 *     }
 *   }
 * }
 * </pre>
 *
 * @param <T> the zombie entity type wrapped by this helper
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class ZombieEntityHelper<T extends Zombie> extends MobEntityHelper<T> {

    /** Wraps the supplied zombie and retains its reference for this helper.
     * @param base zombie entity to wrap
     */
    public ZombieEntityHelper(T base) {
        super(base);
    }

    /**
     * Returns whether the zombie's drowned-conversion countdown is active.
     *
     * @return {@code true} while the conversion countdown is active; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isConvertingToDrowned() {
        return base.isUnderWaterConverting();
    }

}
