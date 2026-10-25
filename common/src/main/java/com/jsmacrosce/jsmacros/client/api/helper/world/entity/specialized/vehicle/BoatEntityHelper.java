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
 * A helper for boats and rafts, including their chest variants.
 * <p>
 * The status checks read the boat's current status; {@link #isUnderwater()} matches both
 * underwater statuses. The four checks are mutually exclusive.
 * <pre>
 * const boats = World.getEntities(32, "oak_boat", "oak_chest_boat", "bamboo_raft", "bamboo_chest_raft");
 * if (boats !== null) {
 *   for (const entity of boats) {
 *     if (!(entity instanceof EntityHelpers.Boat)) continue;
 *     // isUnderwater covers both underwater statuses
 *     let where = "somewhere";
 *     if (entity.isInWater()) {
 *       where = "floating";
 *     } else {
 *       if (entity.isUnderwater()) {
 *         where = "underwater";
 *       } else {
 *         if (entity.isInAir()) {
 *           where = "in the air";
 *         } else {
 *           if (entity.isOnLand()) {
 *             where = "ashore";
 *           }
 *         }
 *       }
 *     }
 *     Chat.log(`${entity.isChestBoat() ? "chest boat" : "boat"} is ${where}`);
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

    /**
     * Wraps the supplied boat or raft and retains its reference for this helper.
     *
     * @param base the boat or raft entity to wrap
     */
    public BoatEntityHelper(AbstractBoat base) {
        super(base);
    }

    /**
     * whether this is a chest boat or chest raft.
     *
     * @return {@code true} for a chest boat or chest raft, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isChestBoat() {
        return base instanceof ChestBoat || base instanceof ChestRaft;
    }

    /**
     * Returns the game's pick-result stack for this boat, with a count of one.
     *
     * @return a newly created stack for the boat's pick-result item
     * @since 2.0.0
     */
    public ItemStackHelper getBoatItem() {
        return new ItemStackHelper(base.getPickResult());
    }

    /**
     * whether the boat's current status is {@code IN_WATER}.
     *
     * @return {@code true} if its status is {@code IN_WATER}
     * @since 1.8.4
     */
    public boolean isInWater() {
        return getLocation() == Boat.Status.IN_WATER;
    }

    /**
     * whether the boat's current status is {@code ON_LAND}.
     *
     * @return {@code true} if its status is {@code ON_LAND}
     * @since 1.8.4
     */
    public boolean isOnLand() {
        return getLocation() == Boat.Status.ON_LAND;
    }

    /**
     * whether the boat's current status is {@code UNDER_WATER} or
     * {@code UNDER_FLOWING_WATER}.
     *
     * @return {@code true} if its status is {@code UNDER_WATER} or {@code UNDER_FLOWING_WATER}
     * @since 1.8.4
     */
    public boolean isUnderwater() {
        return getLocation() == Boat.Status.UNDER_WATER || getLocation() == Boat.Status.UNDER_FLOWING_WATER;
    }

    /**
     * whether the boat's current status is {@code IN_AIR}.
     *
     * @return {@code true} if its status is {@code IN_AIR}
     * @since 1.8.4
     */
    public boolean isInAir() {
        return getLocation() == Boat.Status.IN_AIR;
    }

    private Boat.Status getLocation() {
        return base.status;
    }

}
