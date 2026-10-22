package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.world.entity.EquipmentSlot;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.access.IAbstractMountInventoryScreen;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.AbstractHorseEntityHelper;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
*///? } else {
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
//? }

/**
 * Handle for an open horse, mule, donkey, llama, or camel inventory screen. Slots 0 and 1 are
 * the saddle and body-armor slots; carried slots follow them when present. The first two slots
 * can be read regardless of whether the mount accepts those items. Use {@link #hasChest()} to
 * check whether carried slots are available.
 * <pre>
 * const inv = Player.openInventory();
 * if (inv.is("Horse")) {
 *   const horse = inv.getHorse();
 *   if (inv.hasChest()) {
 *     Chat.log(`${horse.isTame() ? "tamed" : "wild"} mount, ${inv.getInventorySize()} carried slots`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Inventory")
@SuppressWarnings("unused")
public class HorseInventory extends Inventory<HorseInventoryScreen> {

    private final AbstractHorse horse;

    protected HorseInventory(HorseInventoryScreen inventory) {
        super(inventory);
        this.horse = (AbstractHorse) ((IAbstractMountInventoryScreen) inventory).jsmacros_getEntity();
    }

    /**
     * Reports whether the mount can use a saddle slot. This is false for an untamed, baby, or
     * dead mount; it does not report whether the mount currently has a saddle.
     *
     * @return {@code true} if the mount can be saddled, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canBeSaddled() {
        return horse.canUseSlot(EquipmentSlot.SADDLE);
    }

    /**
     * Reports whether the mount is saddled, independently of the contents currently in the
     * saddle slot.
     *
     * @return {@code true} if the mount is saddled, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSaddled() {
        return horse.isSaddled();
    }

    /**
     * Returns the saddle slot (menu slot 0), whether or not the mount can use a saddle.
     *
     * @return the saddle item.
     * @since 1.8.4
     */
    public ItemStackHelper getSaddle() {
        return getSlot(0);
    }

    /**
     * Reports whether the mount can use the body-armor slot; it does not indicate whether
     * armor is equipped.
     *
     * @return {@code true} if the mount can equip armor, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasArmorSlot() {
        return horse.canUseSlot(EquipmentSlot.BODY);
    }

    /**
     * Returns the body-armor slot (menu slot 1), whether or not the mount can use it.
     *
     * @return the armor item.
     * @since 1.8.4
     */
    public ItemStackHelper getArmor() {
        return getSlot(1);
    }

    /**
     * Reports whether this mount has a chest.
     *
     * @return {@code true} if the mount has equipped a chest, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasChest() {
        return horse instanceof AbstractChestedHorse && ((AbstractChestedHorse) horse).hasChest();
    }

    /**
     * Returns the number of carried slots: 15 for a chested mount, otherwise 0.
     *
     * @return the number of carried slots
     * @since 1.8.4
     */
    public int getInventorySize() {
        return horse instanceof AbstractChestedHorse ? ((AbstractChestedHorse) horse).getInventoryColumns() * 3 : 0;
    }

    /**
     * Returns carried slots in order, including empty slots. The list is empty when there are
     * no carried slots.
     *
     * @return a list of items in the horse's carried slots, empty when it has no chest.
     * @since 1.8.4
     */
    public List<ItemStackHelper> getHorseInventory() {
        final int otherSlots = 2;
        return IntStream.range(otherSlots, getInventorySize() + otherSlots).mapToObj(this::getSlot).collect(Collectors.toList());
    }

    /**
     * Returns a helper for the mount associated with this inventory screen.
     *
     * @return the horse this inventory belongs to.
     * @since 1.8.4
     */
    public AbstractHorseEntityHelper<?> getHorse() {
        return new AbstractHorseEntityHelper<>(horse);
    }

    /**
     * Returns a diagnostic label containing the current value returned by {@link #hasChest()}.
     *
     * @return {@code HorseInventory:{"hasChest": true}} or {@code HorseInventory:{"hasChest": false}},
     *         with the boolean in lowercase.
     */
    @Override
    public String toString() {
        return String.format("HorseInventory:{\"hasChest\": %b}", hasChest());
    }

}
