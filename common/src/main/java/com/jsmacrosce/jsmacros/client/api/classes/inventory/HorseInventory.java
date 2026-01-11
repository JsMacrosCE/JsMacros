package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import com.jsmacrosce.jsmacros.client.access.IHorseScreen;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.AbstractHorseEntityHelper;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class HorseInventory extends Inventory<HorseInventoryScreen> {

    private final AbstractHorse horse;

    protected HorseInventory(HorseInventoryScreen inventory) {
        super(inventory);
        this.horse = (AbstractHorse) ((IHorseScreen) inventory).jsmacros_getEntity();
    }

    /**
     * @return {@code true} if the horse can be saddled, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canBeSaddled() {
        return horse.canUseSlot(EquipmentSlot.SADDLE);
    }

    /**
     * @return {@code true} if the horse is saddled, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSaddled() {
        return horse.isSaddled();
    }

    /**
     * @return the saddle item.
     * @since 1.8.4
     */
    public ItemStackHelper getSaddle() {
        return getSlot(0);
    }

    /**
     * @return {@code true} if the horse can equip armor, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasArmorSlot() {
        return horse.canUseSlot(EquipmentSlot.BODY);
    }

    /**
     * @return the armor item.
     * @since 1.8.4
     */
    public ItemStackHelper getArmor() {
        return getSlot(1);
    }

    /**
     * @return {@code true} if the horse has equipped a chest, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasChest() {
        return horse instanceof AbstractChestedHorse && ((AbstractChestedHorse) horse).hasChest();
    }

    /**
     * @return the horse's inventory size.
     * @since 1.8.4
     */
    public int getInventorySize() {
        return horse instanceof AbstractChestedHorse ? ((AbstractChestedHorse) horse).getInventoryColumns() * 3 : 0;
    }

    /**
     * @return a list of items in the horse's inventory.
     * @since 1.8.4
     */
    public List<ItemStackHelper> getHorseInventory() {
        final int otherSlots = 2;
        return IntStream.range(otherSlots, getInventorySize() + otherSlots).mapToObj(this::getSlot).collect(Collectors.toList());
    }

    /**
     * @return the horse this inventory belongs to.
     * @since 1.8.4
     */
    public AbstractHorseEntityHelper<?> getHorse() {
        return new AbstractHorseEntityHelper<>(horse);
    }

    @Override
    public String toString() {
        return String.format("HorseInventory:{\"hasChest\": %b}", hasChest());
    }

}
