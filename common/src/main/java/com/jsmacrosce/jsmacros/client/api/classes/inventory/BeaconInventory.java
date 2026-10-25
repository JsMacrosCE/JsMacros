package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;

import java.util.Optional;

/**
 * Handle for an open beacon screen.
 * <p>
 * {@link #selectFirstEffect(String)} and {@link #selectSecondEffect(String)} change the
 * client-side selection; {@link #applyEffects()} sends the selected pair and closes the screen.
 * <p>
 * A beacon has four effect tiers: speed and haste; resistance and jump boost; strength; then
 * regeneration. Its level determines which tiers these methods can search.
 * <p>
 * Effects use registry ids, and either selection can be unset ({@code null}).
 * <pre>
 * const inv = Player.openInventory();
 * if (inv instanceof Inventories.Beacon) {
 *   Chat.log(`beacon level ${inv.getLevel()}`);
 *   inv.selectFirstEffect("minecraft:haste");
 *   inv.selectSecondEffect("minecraft:jump_boost");
 *   Chat.log(`primary ${inv.getFirstEffect()}, secondary ${inv.getSecondEffect()}`);
 * }
 * </pre>
 *
 * @since 1.5.1
 */
@DocletCategory("Inventory")
@SuppressWarnings("unused")
public class BeaconInventory extends Inventory<BeaconScreen> {
    protected BeaconInventory(BeaconScreen inventory) {
        super(inventory);
    }

    /**
     * Returns the beacon level from the menu's synced data; the level governs which effect
     * tiers the selection methods search.
     *
     * @return the synced beacon level
     * @since 1.5.1
     */
    public int getLevel() {
        return inventory.getMenu().getLevels();
    }

    /**
     * Returns the client's selected primary effect as a registry id; selecting an effect does
     * not send it until {@link #applyEffects()} is called.
     *
     * @return the selected primary effect's registry id, or {@code null} when none is selected
     * @since 1.5.1
     */
    @DocletReplaceReturn("BeaconStatusEffect | null")
    @Nullable
    public String getFirstEffect() {
        Holder<MobEffect> effect = inventory.primary;
        return effect == null ? null : BuiltInRegistries.MOB_EFFECT.getKey(effect.value()).toString();
    }

    /**
     * Returns the client's selected secondary effect as a registry id; selecting an effect does
     * not send it until {@link #applyEffects()} is called.
     *
     * @return the selected secondary effect's registry id, or {@code null} when none is selected
     * @since 1.5.1
     */
    @DocletReplaceReturn("BeaconStatusEffect | null")
    @Nullable
    public String getSecondEffect() {
        Holder<MobEffect> effect = inventory.secondary;
        return effect == null ? null : BuiltInRegistries.MOB_EFFECT.getKey(effect.value()).toString();
    }

    /**
     * Selects a matching primary effect from tiers up to level 2. This changes only the
     * client-side selection; {@link #applyEffects()} sends it.
     *
     * @param id the registry id of the effect to highlight, such as {@code "minecraft:haste"}
     * @return {@code true} if the effect was found and highlighted, {@code false} otherwise
     * @since 1.5.1
     */
    @DocletReplaceParams("id: BeaconStatusEffect")
    public boolean selectFirstEffect(String id) {
        Holder<MobEffect> matchEffect;
        for (int i = 0; i < Math.min(getLevel(), 2); i++) {
            matchEffect = BeaconBlockEntity.BEACON_EFFECTS.get(i).stream().filter(e -> BuiltInRegistries.MOB_EFFECT.getKey(e.value()).toString().equals(id)).findFirst().orElse(null);
            if (matchEffect != null) {
                inventory.primary = matchEffect;
                return true;
            }
        }
        return false;
    }

    /**
     * Selects a matching secondary effect when the level is at least 3. Below level 3 it
     * returns {@code false} without changing the selection. Matching the current primary sets
     * the secondary to that same effect; otherwise a match sets both selections to it. Changes
     * remain client-side until {@link #applyEffects()}.
     * <p>
     * At level 4, selecting regeneration while a primary is already set instead replaces the
     * primary, leaves the secondary unchanged, and returns {@code false}. A level above 4 can
     * cause an {@link java.lang.IndexOutOfBoundsException} while searching.
     *
     * @param id the registry id of the effect to highlight, such as {@code "minecraft:resistance"}
     * @return {@code true} if the effect was found and highlighted, {@code false} otherwise
     * @since 1.5.1
     */
    @DocletReplaceParams("id: BeaconStatusEffect")
    public boolean selectSecondEffect(String id) {
        if (getLevel() >= 3) {
            Holder<MobEffect> primaryEffect = inventory.primary;
            if (primaryEffect != null && BuiltInRegistries.MOB_EFFECT.getKey(primaryEffect.value()).toString().equals(id)) {
                inventory.secondary = primaryEffect;
                return true;
            }
            Holder<MobEffect> matchEffect;
            for (int i = 0; i < getLevel(); i++) {
                matchEffect = BeaconBlockEntity.BEACON_EFFECTS.get(i).stream().filter(e -> BuiltInRegistries.MOB_EFFECT.getKey(e.value()).toString().equals(id)).findFirst().orElse(null);
                if (matchEffect != null) {
                    if (primaryEffect != null && matchEffect.equals(MobEffects.REGENERATION)) {
                        inventory.primary = matchEffect;
                    } else {
                        inventory.primary = matchEffect;
                        inventory.secondary = matchEffect;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Sends the selected effects and closes the container if the payment slot is occupied;
     * otherwise it sends nothing. It does not check the beacon level.
     *
     * @return {@code true} if the effects were sent and the screen closed, {@code false} if
     *         the payment slot was empty
     * @since 1.5.1
     */
    public boolean applyEffects() {
        if (inventory.getMenu().hasPayment()) {
            mc.getConnection().send(new ServerboundSetBeaconPacket(
                Optional.ofNullable(inventory.primary),
                Optional.ofNullable(inventory.secondary)
            ));
            player.closeContainer();
            return true;
        }
        return false;
    }

    /**
     * Returns the fixed diagnostic label {@code BeaconInventory:{}}.
     *
     * @return the fixed diagnostic label {@code BeaconInventory:{}}
     */
    @Override
    public String toString() {
        return String.format("BeaconInventory:{}");
    }

}
