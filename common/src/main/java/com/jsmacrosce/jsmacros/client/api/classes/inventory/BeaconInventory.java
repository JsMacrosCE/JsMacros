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
 * the handle for an open beacon screen.
 * <p>
 * Choosing a beacon's effects is a two-step affair and the methods here split along that
 * line. {@link #selectFirstEffect(String)} and {@link #selectSecondEffect(String)} only set
 * what the screen has highlighted, which is local to the client and can be re-read with
 * {@link #getFirstEffect()} and {@link #getSecondEffect()}. Nothing reaches the server until
 * {@link #applyEffects()} is called, which sends the pair and closes the screen.
 * <p>
 * A beacon offers its effects in four fixed tiers, and the tier an effect sits in decides
 * whether a beacon level reaches it: {@code minecraft:speed} and {@code minecraft:haste} are
 * the first, {@code minecraft:resistance} and {@code minecraft:jump_boost} the second,
 * {@code minecraft:strength} the third and {@code minecraft:regeneration} the fourth.
 * {@link #getLevel()} is what the pyramid underneath the beacon has built, and vanilla caps
 * that at four, which is why the fourth tier is reachable only by a full pyramid.
 * <p>
 * The effects are named by registry id, so they are strings such as {@code "minecraft:haste"}
 * rather than display names, and either effect can be unset, which reads back as {@code null}.
 * example:
 * <pre>
 * const inv = Player.openInventory();
 * if (inv.is("Beacon")) {
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
     * the level of the beacon underneath, which is how many pyramid blocks it is built from.
     * <p>
     * It is read from the menu's synced data rather than worked out from blocks in the world,
     * so it is whatever the server last sent, and it moves as the beacon is built up or torn
     * down rather than only when a script reads it. This is the number that decides which of
     * the four effect tiers {@link #selectFirstEffect(String)} and
     * {@link #selectSecondEffect(String)} can reach.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Beacon")) {
     *   Chat.log(`this beacon is level ${inv.getLevel()}`);
     * }
     * </pre>
     *
     * @return the beacon's level as the server reported it
     * @since 1.5.1
     */
    public int getLevel() {
        return inventory.getMenu().getLevels();
    }

    /**
     * the primary effect currently highlighted on the screen, as a registry id.
     * <p>
     * It is read from the screen rather than from the menu, so it is the client's current
     * choice and is {@code null} until something is selected. Selecting one does not send
     * it; {@link #applyEffects()} does. Picking the same effect for both is a supported way to
     * ask for a single effect, which is what {@link #selectSecondEffect(String)} does when the
     * id it is given is already the primary one.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Beacon")) {
     *   if (inv.getFirstEffect() !== null) {
     *     Chat.log(`primary effect is ${inv.getFirstEffect()}`);
     *   }
     * }
     * </pre>
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
     * the secondary effect currently highlighted on the screen, as a registry id.
     * <p>
     * This is the second half of the pair that gets sent, and it is {@code null} whenever only
     * one effect is selected. Selecting it still means nothing to the server until
     * {@link #applyEffects()} is called.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Beacon")) {
     *   if (inv.getSecondEffect() !== null) {
     *     Chat.log(`secondary effect is ${inv.getSecondEffect()}`);
     *   }
     * }
     * </pre>
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
     * highlights a registry id as the beacon's primary effect, if this beacon's level reaches
     * it.
     * <p>
     * The search covers the first two tiers, which is
     * {@code minecraft:speed}, {@code minecraft:haste},
     * {@code minecraft:resistance} and {@code minecraft:jump_boost}; the third tier's
     * {@code minecraft:strength} is outside the range this method walks, so it can be reached
     * only through {@link #selectSecondEffect(String)}. A level 1 beacon searches the first tier
     * only. This changes what the screen has highlighted and nothing else: read it back with
     * {@link #getFirstEffect()} and send it with {@link #applyEffects()}.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Beacon")) {
     *   if (inv.selectFirstEffect("minecraft:haste")) {
     *     Chat.log(`selected ${inv.getFirstEffect()}`);
     *   } else {
     *     Chat.log("no haste on this beacon");
     *   }
     * }
     * </pre>
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
     * highlights a registry id as the beacon's secondary effect, which a beacon only offers
     * from level 3 upwards.
     * <p>
     * Below level 3 this returns {@code false} without changing anything, because the beacon
     * cannot run two effects. At level 3 it walks all three tiers, so unlike
     * {@link #selectFirstEffect(String)} it can reach {@code minecraft:strength}.
     * <p>
     * Asking for the effect that is already primary is the supported way to set a single
     * effect: the secondary is set to the same one and this returns {@code true}. For an
     * effect that is not the primary, both slots are set to it, which the beacon screen then
     * reads as a pair. As with the primary, this is local to the client until
     * {@link #applyEffects()} is called.
     * <p>
     * Two limits worth knowing before calling this. The loop is bounded by the beacon's level
     * rather than by the number of tiers, so a level that exceeds the four tiers a beacon has
     * makes this walk off the end of the tier list and raise
     * {@link java.lang.IndexOutOfBoundsException} instead of returning {@code false}; vanilla
     * caps a beacon at level 4, so a normally synced menu never reaches that and an id
     * matching no tier simply returns {@code false}. And {@code minecraft:regeneration}, which
     * sits alone in the fourth tier, is a special case: on a level 4 beacon that already has a
     * primary effect set, asking for it overwrites the primary with regeneration, leaves the
     * secondary alone and still returns {@code false}. With no primary set it behaves like any
     * other id and fills both slots.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Beacon")) {
     *   inv.selectFirstEffect("minecraft:resistance");
     *   // the same id again means one effect rather than two
     *   if (inv.selectSecondEffect("minecraft:resistance")) {
     *     Chat.log(`beacon set to ${inv.getFirstEffect()}`);
     *   }
     * }
     * </pre>
     *
     * @param id the registry id of the effect to highlight, such as {@code "minecraft:resistance"}
     * @return {@code true} if the effect was found and highlighted, {@code false} otherwise
     * @throws IndexOutOfBoundsException if this is a beacon whose level is 5 or more, which
     *         vanilla never produces, since there are only four effect tiers
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
     * pays for the beacon and sends the chosen effects, which is the step that makes a
     * selection mean anything.
     * <p>
     * The beacon screen takes a payment item, and this refuses to act until one is in that
     * slot, so it returns {@code false} and sends nothing when the slot is empty. When it does
     * act it sends the primary and secondary read from the screen and closes the container,
     * so there is no screen left to read them back from afterwards. The beacon's level is not
     * checked here: whatever has been highlighted is what gets sent.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Beacon")) {
     *   inv.selectFirstEffect("minecraft:speed");
     *   if (inv.applyEffects()) {
     *     Chat.log("beacon effects sent");
     *   } else {
     *     Chat.log("no payment item in the beacon");
     *   }
     * }
     * </pre>
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

    @Override
    public String toString() {
        return String.format("BeaconInventory:{}");
    }

}
