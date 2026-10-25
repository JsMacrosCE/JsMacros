package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
*///? } else {
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
//?}

/**
 * Provides horse APIs for donkeys and mules, including whether a chest is attached.
 * <pre>
 * const donkeys = World.getEntities(32, "donkey");
 * if (donkeys !== null) {
 *   for (const entity of donkeys) {
 *     if (!(entity instanceof EntityHelpers.Donkey)) continue;
 *     if (entity.hasChest()) {
 *       Chat.log(`${entity.getInventorySize()} slots of storage`);
 *     }
 *   }
 * }
 * </pre>
 * @param <T> the chested-horse subtype wrapped by this helper
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class DonkeyEntityHelper<T extends AbstractChestedHorse> extends AbstractHorseEntityHelper<T> {

    /**
     * Wraps the supplied chested-horse entity and retains its reference for this helper.
     *
     * @param base chested-horse entity to wrap
     */
    public DonkeyEntityHelper(T base) {
        super(base);
    }

    /**
     * Returns whether this donkey or mule has a chest attached.
     *
     * @return {@code true} if it has a chest attached; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasChest() {
        return base.hasChest();
    }

}
