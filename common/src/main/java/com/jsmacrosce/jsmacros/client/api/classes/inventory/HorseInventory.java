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
 * the handle for an open horse, mule, donkey, llama or camel inventory screen.
 * <p>
 * A mount's inventory is the saddle slot, the armour slot and then the mount's own carried
 * items, which is the order the menu builds them in, so 0 is the saddle and 1 is the armour.
 * Both of those slots are in the menu whether or not the mount will take anything, so reading
 * them is always safe; {@link #canBeSaddled()} and {@link #hasArmorSlot()} answer the separate
 * question of whether putting something in them will stick.
 * <p>
 * The carried items are only there when the mount has a chest, which {@link #hasChest()}
 * reports, and {@link #getInventorySize()} and {@link #getHorseInventory()} are the two ways
 * in to them.
 * example:
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
     * whether this mount can be saddled at all, as opposed to whether it is.
     * <p>
     * A mount accepts a saddle only once it is alive, no longer a baby and tamed, so this is
     * false for an untamed mount and for a foal even though the saddle slot is still on screen
     * and still readable. {@link #isSaddled()} answers the other half of the question.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Horse")) {
     *   if (inv.canBeSaddled()) {
     *     if (!inv.isSaddled()) {
     *       Chat.log("this mount could take a saddle");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the mount can be saddled, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean canBeSaddled() {
        return horse.canUseSlot(EquipmentSlot.SADDLE);
    }

    /**
     * whether a saddle is on the mount right now.
     * <p>
     * This reads the mount itself rather than the saddle slot, so it is the state of the
     * entity: a saddle sitting in the open slot is not a saddle on the mount yet. Taking the
     * slot's contents is what puts it on.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Horse")) {
     *   Chat.log(inv.isSaddled() ? "saddled" : "not saddled");
     * }
     * </pre>
     *
     * @return {@code true} if the mount is saddled, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSaddled() {
        return horse.isSaddled();
    }

    /**
     * the saddle slot, which is slot 0.
     * <p>
     * The slot is part of the menu whether or not the mount will take a saddle, so this is
     * always safe to read; {@link #canBeSaddled()} is what says whether putting something in it
     * will do anything.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Horse")) {
     *   if (!inv.getSaddle().isEmpty()) {
     *     Chat.log(`saddle slot holds ${inv.getSaddle().getItemId()}`);
     *   }
     * }
     * </pre>
     *
     * @return the saddle item.
     * @since 1.8.4
     */
    public ItemStackHelper getSaddle() {
        return getSlot(0);
    }

    /**
     * whether this mount has a slot for body armour, as opposed to whether it is wearing any.
     * <p>
     * Not every mount that opens this screen will take armour, but the slot is in the menu
     * either way, so this does not decide whether {@link #getArmor()} can be read; it decides
     * whether putting something in it will stick.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Horse")) {
     *   if (inv.hasArmorSlot()) {
     *     if (!inv.getArmor().isEmpty()) {
     *       Chat.log(`wearing ${inv.getArmor().getItemId()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the mount can equip armor, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasArmorSlot() {
        return horse.canUseSlot(EquipmentSlot.BODY);
    }

    /**
     * the body armour slot, which is slot 1.
     * <p>
     * As with the saddle, this reads the slot rather than the mount, so it is empty while the
     * armour is on the mount rather than in the slot. The slot is always present, so this is
     * safe to read; {@link #hasArmorSlot()} is what says whether it will hold anything.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Horse")) {
     *   if (inv.hasArmorSlot()) {
     *     Chat.log(`armour slot holds ${inv.getArmor().getItemId()}`);
     *   }
     * }
     * </pre>
     *
     * @return the armor item.
     * @since 1.8.4
     */
    public ItemStackHelper getArmor() {
        return getSlot(1);
    }

    /**
     * whether the mount is carrying a chest, which is what its carried slots come from.
     * <p>
     * This is false for a mount without one, and a mount with one that has been removed does
     * not answer true either: the slots belong to the chest, so a chested mount that has lost
     * its chest has neither the slots nor the items in them.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Horse")) {
     *   if (inv.hasChest()) {
     *     Chat.log(`chested mount with ${inv.getInventorySize()} carried slots`);
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the mount has equipped a chest, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasChest() {
        return horse instanceof AbstractChestedHorse && ((AbstractChestedHorse) horse).hasChest();
    }

    /**
     * the number of carried slots the mount's inventory has, which is 15 or 0 rather than a
     * range.
     * <p>
     * A mount only carries items when it has a chest, and when it does it carries 15 slots,
     * so this comes back as 15 or as 0 and never as an intermediate number. Five columns of
     * three rows is where the 15 comes from, and it is the same count
     * {@link #getHorseInventory()} walks.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Horse")) {
     *   const size = inv.getInventorySize();
     *   Chat.log(size > 0 ? `${size} carried slots` : "no chest, no carried slots");
     * }
     * </pre>
     *
     * @return the horse's inventory size, 15 when it has a chest and 0 when it does not
     * @since 1.8.4
     */
    public int getInventorySize() {
        return horse instanceof AbstractChestedHorse ? ((AbstractChestedHorse) horse).getInventoryColumns() * 3 : 0;
    }

    /**
     * the mount's carried slots, in order, as a list of items.
     * <p>
     * The list is empty for a mount without a chest, because the carried slots only exist
     * then, and it holds one entry per slot whether or not that slot is filled, so the
     * position in the list is the slot number counted from the first carried slot rather than
     * a running index over the non-empty ones. A mount with a chest gives 15 entries.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Horse")) {
     *   if (inv.hasChest()) {
     *     const carried = inv.getHorseInventory();
     *     for (let i = 0; carried.size() > i; i++) {
     *       const stack = carried.get(i);
     *       if (!stack.isEmpty()) {
     *         Chat.log(`carried ${i}: ${stack.getItemId()}`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return a list of items in the horse's carried slots, empty when it has no chest.
     * @since 1.8.4
     */
    public List<ItemStackHelper> getHorseInventory() {
        final int otherSlots = 2;
        return IntStream.range(otherSlots, getInventorySize() + otherSlots).mapToObj(this::getSlot).collect(Collectors.toList());
    }

    /**
     * the mount this screen was opened for, as an entity helper.
     * <p>
     * The mount is read once when this handle is built and kept, so every call wraps that same
     * entity rather than looking it up again, and asking it about the mount's own state answers
     * what {@link #isSaddled()} answers. This is the way to reach the rest of the mount: its
     * attributes, whether it is tamed, and its owner's UUID as a string.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Horse")) {
     *   const horse = inv.getHorse();
     *   const owner = horse.getOwner();
     *   Chat.log(`speed ${horse.getSpeedStat().toFixed(2)}, owner ${owner === null ? "none" : owner}`);
     * }
     * </pre>
     *
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
