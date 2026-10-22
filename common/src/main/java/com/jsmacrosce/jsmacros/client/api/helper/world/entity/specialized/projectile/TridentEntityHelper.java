package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinTridentEntity;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
*///? } else {
import net.minecraft.world.entity.projectile.ThrownTrident;
 //?}

/**
 * a thrown trident, the entity a trident becomes between leaving the hand and coming back.
 * <p>
 * Both calls here are about the trident it was thrown as rather than about the flight: whether
 * the item had loyalty, which is what makes it come back, and whether it had any enchantment
 * on it at all, which is what puts the glint on it. A trident with neither is an ordinary one
 * that has to be picked up off the ground.
 * example:
 * <pre>
 * const TridentEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.TridentEntityHelper");
 * const tridents = World.getEntities(32, "trident");
 * if (tridents !== null) {
 *   for (const entity of tridents) {
 *     const trident = TridentEntityHelper.class.cast(entity);
 *     if (trident.hasLoyalty()) {
 *       Chat.log("this one is on its way back by itself");
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
public class TridentEntityHelper extends EntityHelper<ThrownTrident> {

    public TridentEntityHelper(ThrownTrident base) {
        super(base);
    }

    /**
     * {@code true} if the trident is enchanted with loyalty, {@code false} otherwise.
     * <p>
     * This is the loyalty value the item had when it was thrown rather than a flag, and the
     * value is what decides how quickly the trident comes back and how high it rises as it
     * does. It is worked out on the server when the entity is created and stored with it, so a
     * trident that was thrown by a command or spawned into the world answers {@code false}
     * however it was made.
     * example:
     * <pre>
     * const TridentEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.TridentEntityHelper");
     * const tridents = World.getEntities(32, "trident");
     * if (tridents !== null) {
     *   for (const entity of tridents) {
     *     const trident = TridentEntityHelper.class.cast(entity);
     *     if (trident.hasLoyalty()) {
     *       Chat.log("it will come back on its own, so there is no need to chase it");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the trident is enchanted with loyalty, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasLoyalty() {
        return base.getEntityData().get(((MixinTridentEntity) base).getLoyalty()) > 0;
    }

    /**
     * {@code true} if the trident is enchanted, {@code false} otherwise.
     * <p>
     * This is the glint flag rather than a count or a list: it is {@code true} for any
     * enchantment at all, including one that has nothing to do with how the trident flies. It
     * is set from the item when the entity is created and does not change while the trident is
     * in the air, so a trident that was enchanted after being thrown reports what it was
     * thrown with.
     * <p>
          * The two calls here are not the same question: a trident with loyalty is always
     * enchanted, since the glint is set from the whole item, but the converse does not hold,
     * because any other enchantment sets the glint without giving the trident loyalty.
     * example:
     * <pre>
     * const TridentEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.TridentEntityHelper");
     * const tridents = World.getEntities(32, "trident");
     * if (tridents !== null) {
     *   for (const entity of tridents) {
     *     const trident = TridentEntityHelper.class.cast(entity);
     *     if (trident.isEnchanted()) {
     *       if (!trident.hasLoyalty()) {
     *         Chat.log("enchanted, but with something other than loyalty");
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the trident is enchanted, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isEnchanted() {
        return base.isFoil();
    }

}
