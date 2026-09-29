package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.doclet.DocletCategory;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
*///? } else {
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
//?}

/**
 * the donkey side of the horse family: everything an {@link AbstractHorseEntityHelper} has,
 * plus the chest.
 * <p>
 * The chest is not decoration. A donkey carries one of its own only when it has been given
 * one, and the size of that chest is what {@code getInventorySize} on the base class is
 * reporting, so a chestless donkey answers {@code 0} slots where a chested one answers
 * {@code 15}.
 * example:
 * <pre>
 * const DonkeyEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.DonkeyEntityHelper");
 * const donkeys = World.getEntities(32, "donkey");
 * if (donkeys !== null) {
 *   for (const entity of donkeys) {
 *     const donkey = DonkeyEntityHelper.class.cast(entity);
 *     if (donkey.hasChest()) {
 *       Chat.log(`${donkey.getInventorySize()} slots of storage`);
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
public class DonkeyEntityHelper<T extends AbstractChestedHorse> extends AbstractHorseEntityHelper<T> {

    public DonkeyEntityHelper(T base) {
        super(base);
    }

    /**
     * Whether this donkey or mule is carrying a chest. A player puts one on with a shift
     * click while holding it, and takes it off the same way, and a llama can be given one
     * too - which is why the llama reports through the same base class rather than through
     * the horse.
     * <p>
     * The chest is what the donkey's inventory size follows, so this is worth checking before
     * reading anything off that size.
     *
     * @return {@code true} if the donkey is carrying a chest, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasChest() {
        return base.hasChest();
    }

}
