package com.jsmacrosce.jsmacros.client.api.helper;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * Describes a status-effect instance, including its type, amplifier, duration, and display flags.
 * Amplifiers are zero-based (level I is 0); duration is in ticks, with {@code -1} indicating
 * infinite duration. Values are snapshots and do not count down after retrieval. Use an entity's
 * status-effect list for applied instances, or the registry to inspect an effect type.
 * <pre>
 * const player = Player.getPlayer();
 * if (player !== null) {
 *   for (const effect of player.getStatusEffects()) {
 *     // strength is the amplifier, so level II reads as 1
 *     const level = effect.getStrength() + 1;
 *     // the duration is in ticks, and twenty of those is a second
 *     const remaining = effect.isPermanent() ? "infinite" : `${Math.ceil(effect.getTime() / 20)}s left`;
 *     Chat.log(`${effect.getId()} ${effect.getCategory()} level ${level}, ${remaining}`);
 *   }
 * }
 *
 * // the registry knows about every effect, whether or not anyone has it
 * const registry = Client.getRegistryManager();
 * const speed = registry.getStatusEffect("minecraft:speed");
 * if (speed.isBeneficial()) {
 *   Chat.log(`speed is a beneficial effect, instant: ${speed.isInstant()}`);
 * }
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.2.4
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class StatusEffectHelper extends BaseHelper<MobEffectInstance> {

    /** Wraps the supplied status-effect instance.
     * @param s raw status-effect instance to wrap
     */
    public StatusEffectHelper(MobEffectInstance s) {
        super(s);
    }

    /**
     * Wraps the status-effect type with zero duration.
     *
     * @param s the status-effect type to wrap
     * @since 1.8.4
     */
    public StatusEffectHelper(MobEffect s) {
        this(s, 0);
    }

    /**
     * Wraps the status-effect type with the supplied duration.
     *
     * @param s the status-effect type to wrap
     * @param t the initial duration in ticks, or {@code -1} for infinite duration
     * @since 1.8.4
     */
    public StatusEffectHelper(MobEffect s, int t) {
        super(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(s), t));
    }

    /**
     * Returns the effect's registry identifier.
     *
     * @return the identifier
     * @since 1.2.4
     */
    @DocletReplaceReturn("StatusEffectId")
    public String getId() {
        return BuiltInRegistries.MOB_EFFECT.getKey(base.getEffect().value()).toString();
    }

    /**
     * Returns the zero-based amplifier (level I is {@code 0}).
     *
     * @return the amplifier
     * @since 1.2.4
     */
    public int getStrength() {
        return base.getAmplifier();
    }

    /**
     * Returns the effect category.
     *
     * @return {@code "HARMFUL"}, {@code "NEUTRAL"}, or {@code "BENEFICIAL"}
     * @since 1.8.4
     */
    @DocletReplaceReturn("StatusEffectCategory")
    public String getCategory() {
        return base.getEffect().value().getCategory().name();
    }

    /**
     * Returns the instance's remaining duration snapshot in ticks. Infinite duration is
     * {@code -1}; zero is an expired duration. Re-read the entity's effect to observe its
     * countdown.
     *
     * @return remaining ticks, or {@code -1} for infinite duration
     * @since 1.2.4
     */
    public int getTime() {
        return base.getDuration();
    }

    /**
     * Returns whether this instance has infinite duration.
     *
     * @return {@code true} for infinite duration; {@code false} otherwise
     * @since 1.8.4
     */
    public boolean isPermanent() {
        return base.isInfiniteDuration();
    }

    /**
     * Returns the instance's ambient flag. This is a display/state flag and does not reliably
     * identify how the effect was applied.
     *
     * @return {@code true} if ambient; {@code false} otherwise
     * @since 1.8.4
     */
    public boolean isAmbient() {
        return base.isAmbient();
    }

    /**
     * Returns whether the effect display should show an icon.
     *
     * @return {@code true} if the icon is shown; {@code false} otherwise
     * @since 1.8.4
     */
    public boolean hasIcon() {
        return base.showIcon();
    }

    /**
     * @return {@code true} if this effect is shown in the game's effect display; {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isVisible() {
        return base.isVisible();
    }

    /**
     * Reports whether this effect type applies its effect instantly; this is independent of the
     * instance's duration.
     *
     * @return {@code true} if this effect should be applied instantly, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isInstant() {
        return base.getEffect().value().isInstantenous();
    }

    /**
     * @return {@code true} if this effect is considered beneficial, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isBeneficial() {
        return base.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL;
    }

    /**
     * @return {@code true} if this effect is considered neutral, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isNeutral() {
        return base.getEffect().value().getCategory() == MobEffectCategory.NEUTRAL;
    }

    /**
     * @return {@code true} if this effect is considered harmful, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isHarmful() {
        return base.getEffect().value().getCategory() == MobEffectCategory.HARMFUL;
    }

    /** Returns a diagnostic string containing the effect identifier, strength, and duration.
     * @return diagnostic representation of the wrapped status-effect instance
     */
    @Override
    public String toString() {
        return String.format("StatusEffectHelper:{\"id\": \"%s\", \"strength\": %d, \"time\": %d}", getId(), getStrength(), getTime());
    }

}
