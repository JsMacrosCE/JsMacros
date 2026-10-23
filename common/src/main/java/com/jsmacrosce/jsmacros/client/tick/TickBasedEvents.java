package com.jsmacrosce.jsmacros.client.tick;

import com.jsmacrosce.jsmacros.client.util.GuiCompat;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.client.gui.screens.KeyMacrosScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.event.impl.inventory.EventItemDamage;
import com.jsmacrosce.jsmacros.client.api.event.impl.player.EventArmorChange;
import com.jsmacrosce.jsmacros.client.api.event.impl.player.EventFallFlying;
import com.jsmacrosce.jsmacros.client.api.event.impl.player.EventHeldItemChange;
import com.jsmacrosce.jsmacros.client.api.event.impl.world.EventTick;
import com.jsmacrosce.jsmacros.client.api.library.impl.FClient;

/**
 * Provides client tick bookkeeping and native {@code ItemStack} comparison helpers.
 * <pre>
 * const Ticks = Java.type('com.jsmacrosce.jsmacros.client.tick.TickBasedEvents');
 * const player = Player.getPlayer();
 * if (player !== null) {
 *   Chat.log(Ticks.areNotEqual(player.getMainHand().getRaw(), player.getOffHand().getRaw()));
 * }
 * </pre>
 */
public class TickBasedEvents {
    private static ItemStack mainHand = ItemStack.EMPTY;
    private static ItemStack offHand = ItemStack.EMPTY;

    private static ItemStack footArmor = ItemStack.EMPTY;
    private static ItemStack legArmor = ItemStack.EMPTY;
    private static ItemStack chestArmor = ItemStack.EMPTY;
    private static ItemStack headArmor = ItemStack.EMPTY;
    private static boolean previousFallFlyState = false;
    private static long counter = 0;

    /**
     * Client server-status pinger used by {@link FClient#ping}, {@link FClient#pingAsync}, and {@link FClient#cancelAllPings}.
     */
    public static final ServerStatusPinger serverListPinger = new ServerStatusPinger();

    /**
     * Tests whether these stacks differ by emptiness, item identity, count, components, or damage value.
     * Both empty stacks compare as not different.
     * @param a the first non-null stack
     * @param b the second non-null stack
     * @return {@code true} if exactly one stack is empty or the nonempty stacks differ by a listed check; {@code false} otherwise
     */
    public static boolean areNotEqual(ItemStack a, ItemStack b) {
        return (!a.isEmpty() || !b.isEmpty()) && (a.isEmpty() || b.isEmpty() || !ItemStack.isSameItem(a, b) || a.getCount() != b.getCount() || !a.getComponents().equals(b.getComponents()) || a.getDamageValue() != b.getDamageValue());
    }

    /**
     * Tests component entries from {@code a} against the raw value for each component type in {@code b}, ignoring damage.
     * This iterates only {@code a}'s components; it does not compare item identity or count. Because each entry is compared
     * directly with the raw value from {@code b}, this is not a symmetric or full component-value equality test.
     * Both empty stacks pass; exactly one empty stack does not.
     * @param a the non-null first stack whose component entries are iterated
     * @param b the non-null second stack supplying component values
     * @return {@code true} for two empty stacks or when both stacks are nonempty and every non-damage entry from {@code a} passes the described comparison; {@code false} otherwise
     */
    public static boolean areTagsEqualIgnoreDamage(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) {
            return true;
        } else if (!a.isEmpty() && !b.isEmpty()) {
            DataComponentMap bc = b.getComponents();
            return a.getComponents().stream().allMatch(e -> e.type() == DataComponents.DAMAGE || e.equals(bc.get(e.type())));
        } else {
            return false;
        }
    }

    /**
     * Tests whether stacks are both empty or are nonempty stacks with the same item and count whose components pass
     * {@code areTagsEqualIgnoreDamage}. That helper compares first-stack entries with raw second-stack values,
     * so this is not a general equality test excluding damage.
     * @param a the first non-null stack
     * @param b the second non-null stack
     * @return {@code true} for two empty stacks, or when both nonempty stacks have the same item and count and pass the component test; {@code false} otherwise
     */
    public static boolean areEqualIgnoreDamage(ItemStack a, ItemStack b) {
        return (a.isEmpty() && b.isEmpty()) || (!a.isEmpty() && !b.isEmpty() && ItemStack.isSameItem(a, b) && a.getCount() == b.getCount() && areTagsEqualIgnoreDamage(a, b));
    }

    @DocletIgnore
    public static void onTick(Minecraft mc) {
        if (JsMacrosClient.keyBinding.consumeClick() && GuiCompat.screen(mc) == null) {
            if (JsMacrosClient.prevScreen == null) {
                JsMacrosClient.prevScreen = new KeyMacrosScreen(null);
            }
            GuiCompat.setScreen(mc, JsMacrosClient.prevScreen);
        }

        FClient.tickSynchronizer.tick();
        serverListPinger.tick();

        new EventTick().trigger();

        if (++counter % 10 == 0) {
            JsMacrosClient.clientCore.services.tickReloadListener();
        }

        if (mc.player != null) {
            boolean state = mc.player.isFallFlying();
            if (previousFallFlyState ^ state) {
                new EventFallFlying(state).trigger();
                previousFallFlyState = state;
            }
        }

        if (mc.player != null && mc.player.getInventory() != null) {
            ItemStack newMainHand = mc.player.getMainHandItem();
            if (areNotEqual(newMainHand, mainHand)) {
                if (areEqualIgnoreDamage(newMainHand, mainHand)) {
                    new EventItemDamage(newMainHand, newMainHand.getDamageValue()).trigger();
                }
                new EventHeldItemChange(newMainHand, mainHand, false).trigger();
                mainHand = newMainHand.copy();
            }

            ItemStack newOffHand = mc.player.getOffhandItem();
            if (areNotEqual(newOffHand, offHand)) {
                if (areEqualIgnoreDamage(newOffHand, offHand)) {
                    new EventItemDamage(newOffHand, newOffHand.getDamageValue()).trigger();
                }
                new EventHeldItemChange(newOffHand, offHand, true).trigger();
                offHand = newOffHand.copy();
            }

            ItemStack newHeadArmor = mc.player.getItemBySlot(EquipmentSlot.HEAD);
            if (areNotEqual(newHeadArmor, headArmor)) {
                if (areEqualIgnoreDamage(newHeadArmor, headArmor)) {
                    new EventItemDamage(newHeadArmor, newHeadArmor.getDamageValue()).trigger();
                }
                new EventArmorChange("HEAD", newHeadArmor, headArmor).trigger();
                headArmor = newHeadArmor.copy();
            }

            ItemStack newChestArmor = mc.player.getItemBySlot(EquipmentSlot.CHEST);
            if (areNotEqual(newChestArmor, chestArmor)) {
                if (areEqualIgnoreDamage(newChestArmor, chestArmor)) {
                    new EventItemDamage(newChestArmor, newChestArmor.getDamageValue()).trigger();
                }
                new EventArmorChange("CHEST", newChestArmor, chestArmor).trigger();
                chestArmor = newChestArmor.copy();

            }

            ItemStack newLegArmor = mc.player.getItemBySlot(EquipmentSlot.LEGS);
            if (areNotEqual(newLegArmor, legArmor)) {
                if (areEqualIgnoreDamage(newLegArmor, legArmor)) {
                    new EventItemDamage(newLegArmor, newLegArmor.getDamageValue()).trigger();
                }
                new EventArmorChange("LEGS", newLegArmor, legArmor).trigger();
                legArmor = newLegArmor.copy();
            }

            ItemStack newFootArmor = mc.player.getItemBySlot(EquipmentSlot.FEET);
            if (areNotEqual(newFootArmor, footArmor)) {
                if (areEqualIgnoreDamage(newFootArmor, footArmor)) {
                    new EventItemDamage(newFootArmor, newFootArmor.getDamageValue()).trigger();
                }
                new EventArmorChange("FEET", newFootArmor, footArmor).trigger();
                footArmor = newFootArmor.copy();
            }
        }
    }

}
