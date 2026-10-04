package com.jsmacrosce.jsmacros.client.api.helper;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * one status effect on one entity: what it is, how strong, how long left, and how it behaves.
 * <p>
 * A status effect is more than a name. The same effect at a different strength is a different
 * thing in game terms, so the two numbers here matter as much as the identifier: the
 * <b>strength</b> is the amplifier, where 0 is level I and 1 is level II, and the
 * <b>time</b> is the remaining duration in ticks, so 20 ticks is one second and 24000 is
 * twenty minutes. Reading a level as though it were the amplifier, or a duration as though it
 * were seconds, is the easy mistake with this class.
 * <p>
 * The rest describes how the effect presents itself rather than what it does. A beacon's effect
 * is <b>ambient</b>, which is a different rendering rather than a different power. An effect
 * can be invisible or have its icon hidden without not being there, so {@link #isVisible()} and
 * {@link #hasIcon()} describe the display while {@link #isHarmful()},
 * {@link #isNeutral()} and {@link #isBeneficial()} describe the effect. Exactly one of those
 * three is true for any effect.
 * <p>
 * The effect is only half of the story: whether the player has it, at what strength and for
 * how long, is on the entity, through
 * {@code LivingEntityHelper.getStatusEffects()}. The registry also hands out effects on their
 * own through {@code Client.getRegistryManager().getStatusEffect(id)}, which is the one to use
 * to find out about an effect rather than to apply it — those have a duration of zero and are
 * not on anything.
 * <br>
 * An effect on an entity is a snapshot: the object does not update as the duration runs down,
 * so a script watching an effect has to read it again.
 * example:
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

    public StatusEffectHelper(MobEffectInstance s) {
        super(s);
    }

    /**
     * @since 1.8.4
     */
    public StatusEffectHelper(MobEffect s) {
        this(s, 0);
    }

    /**
     * @since 1.8.4
     */
    public StatusEffectHelper(MobEffect s, int t) {
        super(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(s), t));
    }

    /**
     * the effect's identifier, the one it is written as in the data files.
     * <p>
     * This is the same string the registry's {@code getStatusEffect} takes, so the two round
     * trip, and it is the one to log or compare rather than a display name.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   for (const effect of player.getStatusEffects()) {
     *     if (effect.getId() === "minecraft:regeneration") {
     *       Chat.log("regenerating");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return
     * @since 1.2.4
     */
    @DocletReplaceReturn("StatusEffectId")
    public String getId() {
        return BuiltInRegistries.MOB_EFFECT.getKey(base.getEffect().value()).toString();
    }

    /**
     * the strength of this effect, as the game's amplifier rather than its level.
     * <p>
     * This is zero-based, so a plain effect gives 0 and what the game calls level II gives 1.
     * The level shown in the inventory is this plus one, and getting that wrong is the usual
     * reason an effect's strength looks a step out.
     * <br>
     * An instant effect such as instant health also has a strength, even though it applies
     * once, because the same value is reused as the amount for those.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   for (const effect of player.getStatusEffects()) {
     *     // what the inventory shows is one more than this
     *     Chat.log(`${effect.getId()} is level ${effect.getStrength() + 1}`);
     *   }
     * }
     * </pre>
     *
     * @return
     * @since 1.2.4
     */
    public int getStrength() {
        return base.getAmplifier();
    }

    /**
     * @return the string name of the category of the status effect, "HARMFUL", "NEUTRAL", or "BENEFICIAL".
     * @since 1.8.4
     */
    @DocletReplaceReturn("StatusEffectCategory")
    public String getCategory() {
        return base.getEffect().value().getCategory().name();
    }

    /**
     * how long this effect has left, in ticks.
     * <p>
     * This is the game's own unit and not seconds: twenty ticks is one second, so 24000 is
     * twenty minutes. Infinite duration is {@code -1}; check {@link #isPermanent()} before
     * dividing a finite duration by 20 to get seconds.
     * <br>
     * A duration of zero is normal and means the effect has run out, so it is not by itself a
     * sign of anything wrong; the game's own default when an effect is created with no
     * duration is zero.
     * <br>
     * It is a snapshot. The duration runs down on the entity's copy, not on this object, so a
     * script that wants the time left as the effect wears off has to read it again rather than
     * hold on to one of these.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   for (const effect of player.getStatusEffects()) {
     *     const remaining = effect.isPermanent() ? "infinite duration" : `${Math.ceil(effect.getTime() / 20)}s left`;
     *     Chat.log(`${effect.getId()} has ${remaining}`);
     *   }
     * }
     * </pre>
     *
     * @return remaining ticks, or {@code -1} for infinite duration
     * @since 1.2.4
     */
    public int getTime() {
        return base.getDuration();
    }

    /**
     * whether this effect is applied permanently.
     * <p>
     * Reads the effect's infinite-duration flag. Infinite duration is represented by
     * {@code -1}; a duration of zero is not permanent.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   for (const effect of player.getStatusEffects()) {
     *     const wearsOff = !effect.isPermanent();
     *     Chat.log(`${effect.getId()} ${wearsOff ? "wears off" : "does not wear off"}`);
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this effect is applied permanently, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPermanent() {
        return base.isInfiniteDuration();
    }

    /**
     * Ambient effects are usually applied through beacons and they make the particles more
     * translucent.
     * <p>
     * This is a presentation flag rather than a different effect: an ambient effect behaves
     * exactly like a non-ambient one, it is only drawn as the faint swirl a beacon gives. It is
     * the quickest way for a script to tell where an effect came from.
     * <br>
     * An effect from a beacon is ambient, and so is one the game applies itself for the player's
     * own benefit; a potion or a command is not.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   for (const effect of player.getStatusEffects()) {
     *     if (effect.isAmbient()) {
     *       Chat.log(`${effect.getId()} came from a beacon, not a potion`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this effect is an ambient one, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isAmbient() {
        return base.isAmbient();
    }

    /**
     * @return {@code true} if this effect has an icon it should render, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasIcon() {
        return base.showIcon();
    }

    /**
     * @return {@code true} if this effect affects the particle color and gets rendered in game,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isVisible() {
        return base.isVisible();
    }

    /**
     * An effect which is instant can still carry a duration on the instance.
     * <p>
     * This is a property of the effect itself rather than of this instance: instant effects are
     * the ones that apply their effect all at once and wear off, and in this build they are the
     * ones built on the game's instant base class — instant health, instant damage and
     * saturation. Every instance of the same effect answers the same way here, whatever
     * duration the instance happens to have.
     * <br>
     * The game still records a duration on an instant effect, and the {@code /effect} command
     * is one place a longer one can be asked for: given no duration it gives an instant effect
     * a single tick and a continuous one 600 ticks, and given a duration it uses that number
     * as-is for an instant effect where it multiplies a continuous one by 20. A script that
     * wants to know why an instant effect has a duration should read {@link #getTime()}.
     * example:
     * <pre>
     * const registry = Client.getRegistryManager();
     * const instantHealth = registry.getStatusEffect("minecraft:instant_health");
     * if (instantHealth.isInstant()) {
     *   // applies all at once, so a duration on it would only come from a command
     *   Chat.log("instant health is an instant effect");
     * }
     * </pre>
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

    @Override
    public String toString() {
        return String.format("StatusEffectHelper:{\"id\": \"%s\", \"strength\": %d, \"time\": %d}", getId(), getStrength(), getTime());
    }

}
