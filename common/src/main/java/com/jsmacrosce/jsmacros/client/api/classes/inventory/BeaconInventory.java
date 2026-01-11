package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;

import java.util.Optional;

/**
 * @since 1.5.1
 */
@SuppressWarnings("unused")
public class BeaconInventory extends Inventory<BeaconScreen> {
    protected BeaconInventory(BeaconScreen inventory) {
        super(inventory);
    }

    /**
     * @return
     * @since 1.5.1
     */
    public int getLevel() {
        return inventory.getMenu().getLevels();
    }

    /**
     * @return
     * @since 1.5.1
     */
    @DocletReplaceReturn("BeaconStatusEffect | null")
    @Nullable
    public String getFirstEffect() {
        Holder<MobEffect> effect = inventory.primary;
        return effect == null ? null : BuiltInRegistries.MOB_EFFECT.getKey(effect.value()).toString();
    }

    /**
     * @return
     * @since 1.5.1
     */
    @DocletReplaceReturn("BeaconStatusEffect | null")
    @Nullable
    public String getSecondEffect() {
        Holder<MobEffect> effect = inventory.secondary;
        return effect == null ? null : BuiltInRegistries.MOB_EFFECT.getKey(effect.value()).toString();
    }

    /**
     * @param id
     * @return
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
     * @param id
     * @return
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
     * @return
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

    @Override
    public String toString() {
        return String.format("BeaconInventory:{}");
    }

}
