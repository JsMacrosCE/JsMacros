package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle;

import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class BoatEntityHelper extends EntityHelper<AbstractBoat> {

    public BoatEntityHelper(AbstractBoat base) {
        super(base);
    }

    /**
     * @return {@code true} if the boat is a chest boat, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isChestBoat() {
        return base instanceof ChestBoat;
    }

    /**
     * @return the item that the boat is spawned from
     * @since 2.0.0
     */
    public ItemStackHelper getBoatItem() {
        return new ItemStackHelper(base.getPickResult());
    }

    /**
     * @return {@code true} if the boat is on top of water, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isInWater() {
        return getLocation() == Boat.Status.IN_WATER;
    }

    /**
     * @return {@code true} if the boat is on land, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isOnLand() {
        return getLocation() == Boat.Status.ON_LAND;
    }

    /**
     * @return {@code true} if the boat is underwater, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isUnderwater() {
        return getLocation() == Boat.Status.UNDER_WATER;
    }

    /**
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
