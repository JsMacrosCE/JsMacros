package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.entity.vehicle.boat.ChestRaft;
*///? } else {
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.entity.vehicle.ChestRaft;
//?}

/**
 * a boat, which covers the plain boat and the chest boat, and the raft as well since the game
 * keeps rafts and boats on one shared base class.
 * <p>
 * Four of the five calls here are about where the boat is: on land, in the air, floating on
 * water, or under it. The game keeps one status for the whole thing and each of the four reads
 * it, so at most one of them is {@code true} at a time. The game has five statuses:
 * {@link #isUnderwater()} covers submersion in both still and flowing water. The fifth call
 * is about the kind of boat rather than the state of it.
 * <p>
 * The status is worked out at the start of each tick from the blocks around the boat, so it
 * lags the world by a tick or so and can briefly disagree with what a script sees at that
 * moment.
 * example:
 * <pre>
 * const BoatEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle.BoatEntityHelper");
 * const boats = World.getEntities(32, "oak_boat", "oak_chest_boat", "bamboo_raft", "bamboo_chest_raft");
 * if (boats !== null) {
 *   for (const entity of boats) {
 *     const boat = BoatEntityHelper.class.cast(entity);
 *     // isUnderwater covers both underwater statuses
 *     let where = "somewhere";
 *     if (boat.isInWater()) {
 *       where = "floating";
 *     } else {
 *       if (boat.isUnderwater()) {
 *         where = "underwater";
 *       } else {
 *         if (boat.isInAir()) {
 *           where = "in the air";
 *         } else {
 *           if (boat.isOnLand()) {
 *             where = "ashore";
 *           }
 *         }
 *       }
 *     }
 *     Chat.log(`${boat.isChestBoat() ? "chest boat" : "boat"} is ${where}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class BoatEntityHelper extends EntityHelper<AbstractBoat> {

    public BoatEntityHelper(AbstractBoat base) {
        super(base);
    }

    /**
     * {@code true} if the boat is a chest boat, {@code false} otherwise.
     * <p>
          * A chest boat is a separate kind rather than a chest in a boat, and the difference is
     * that it has storage. The game only ever makes one of the two, so this answers
     * {@code false} for a plain boat no matter what a script has put in it.
     * <p>
     * This also returns {@code true} for a chest raft, and {@code false} for an ordinary raft.
     * example:
     * <pre>
     * const BoatEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle.BoatEntityHelper");
     * const boats = World.getEntities(32, "oak_boat", "oak_chest_boat", "bamboo_raft", "bamboo_chest_raft");
     * if (boats !== null) {
     *   for (const entity of boats) {
     *     const boat = BoatEntityHelper.class.cast(entity);
     *     // two searches give both kinds, and this is how they are told apart
     *     if (boat.isChestBoat()) {
     *       Chat.log("this one has storage in it");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} for a chest boat or chest raft, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isChestBoat() {
        return base instanceof ChestBoat || base instanceof ChestRaft;
    }

    /**
     * the item that the boat is spawned from, which is the item it breaks into and the one it
     * is picked up as.
     * <p>
     * It is decided by the kind of boat rather than by anything a script has done to it: a
     * chest boat hands back the chest boat item and a plain boat the plain one, so the two
     * never answer the same thing. It is a fresh stack of one, so the count is always
     * {@code 1} and any damage on it is nothing.
     * example:
     * <pre>
     * const BoatEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle.BoatEntityHelper");
     * const boats = World.getEntities(32, "boat", "chest_boat");
     * if (boats !== null) {
     *   for (const entity of boats) {
     *     const boat = BoatEntityHelper.class.cast(entity);
     *     // the item it breaks into, which follows the kind of boat
     *     Chat.log(`breaks into ${boat.getBoatItem().getCount()} ${boat.getBoatItem().getItemID()}`);
     *   }
     * }
     * </pre>
     *
     * @return the item that the boat is spawned from
     * @since 2.0.0
     */
    public ItemStackHelper getBoatItem() {
        return new ItemStackHelper(base.getPickResult());
    }

    /**
     * {@code true} if the boat is on top of water, {@code false} otherwise.
     * <p>
          * The status the game works out at the start of each tick, and one of the five values it
     * can have rather than a separate check: a boat on land or in the air answers
     * {@code false} here, and so does one that is in flowing water, since that is a status of
     * its own. The counterpart is {@link #isUnderwater() isUnderwater()}, which is the state
     * below the surface rather than on it.
     * example:
     * <pre>
     * const BoatEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle.BoatEntityHelper");
     * const boats = World.getEntities(32, "boat", "chest_boat");
     * if (boats !== null) {
     *   for (const entity of boats) {
     *     const boat = BoatEntityHelper.class.cast(entity);
     *     if (boat.isInWater()) {
     *       Chat.log(`floating at ${entity.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the boat is on top of water, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isInWater() {
        return getLocation() == Boat.Status.IN_WATER;
    }

    /**
     * {@code true} if the boat is on land, {@code false} otherwise.
     * <p>
          * The status the game works out at the start of each tick, and the one a boat is in when
     * it is in no water at all and there is something solid under it: the game reaches this
     * status when the blocks beneath the boat give it friction. It is separate from
     * {@link #isInAir() isInAir()} in that it is the branch taken when there is ground under
     * the boat, so a boat resting answers {@code true} here and {@code false} for that one.
     * example:
     * <pre>
     * const BoatEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle.BoatEntityHelper");
     * const boats = World.getEntities(32, "boat", "chest_boat");
     * if (boats !== null) {
     *   for (const entity of boats) {
     *     const boat = BoatEntityHelper.class.cast(entity);
     *     if (boat.isOnLand()) {
     *       Chat.log(`beached at ${entity.getPos()}, so it is not going anywhere`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the boat is on land, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isOnLand() {
        return getLocation() == Boat.Status.ON_LAND;
    }

    /**
     * {@code true} if the boat is underwater, {@code false} otherwise.
     * <p>
          * The status the game works out at the start of each tick, and the state of a boat that
     * has gone under rather than {@link #isInWater() isInWater()}, which is a boat floating on
     * the surface. This one is the whole boat being below the water rather than just in it,
     * which is what makes the two worth telling apart.
     * <p>
     * Includes both still-water and flowing-water submersion. A boat floating on the surface
     * is instead reported by {@link #isInWater()}.
     * example:
     * <pre>
     * const BoatEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle.BoatEntityHelper");
     * const boats = World.getEntities(32, "oak_boat", "oak_chest_boat", "bamboo_raft", "bamboo_chest_raft");
     * if (boats !== null) {
     *   for (const entity of boats) {
     *     const boat = BoatEntityHelper.class.cast(entity);
     *     if (boat.isUnderwater()) {
     *       Chat.log("under the surface, so it has lost control");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the boat is underwater, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isUnderwater() {
        return getLocation() == Boat.Status.UNDER_WATER || getLocation() == Boat.Status.UNDER_FLOWING_WATER;
    }

    /**
     * {@code true} if the boat is in the air, {@code false} otherwise.
     * <p>
          * The status the game works out at the start of each tick, and it is the last of the four:
     * the game reaches it when the boat is in no water and there is nothing solid under it
     * either, so a boat that is falling or has just been launched answers {@code true} here
     * and {@code false} for all of the others. A boat resting on the ground is
     * {@link #isOnLand() on land} rather than in the air.
     * example:
     * <pre>
     * const BoatEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle.BoatEntityHelper");
     * const boats = World.getEntities(32, "boat", "chest_boat");
     * if (boats !== null) {
     *   for (const entity of boats) {
     *     const boat = BoatEntityHelper.class.cast(entity);
     *     if (boat.isInAir()) {
     *       Chat.log("off the ground and falling");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the boat is in the air, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isInAir() {
        return getLocation() == Boat.Status.IN_AIR;
    }

    private Boat.Status getLocation() {
        return base.status;
    }

}
