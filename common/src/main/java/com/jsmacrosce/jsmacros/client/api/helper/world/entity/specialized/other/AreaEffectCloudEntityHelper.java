package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.AreaEffectCloud;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * an area effect cloud, the lingering sphere a thrown potion leaves behind.
 * <p>
 * The cloud is what actually applies the potion over time rather than the splash that created
 * it, so the useful things to read are how big it is, how long it is still counting down and
 * whether it has started working yet. The colour and the particle are how the game shows which
 * potion it is, and neither is the only one: a cloud coloured by its contents is not
 * necessarily a cloud drawing the particle its contents would give it, because a cloud with a
 * particle set by hand draws that instead.
 * example:
 * <pre>
 * const AreaEffectCloudEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other.AreaEffectCloudEntityHelper");
 * const clouds = World.getEntities(32, "area_effect_cloud");
 * if (clouds !== null) {
 *   for (const entity of clouds) {
 *     const cloud = AreaEffectCloudEntityHelper.class.cast(entity);
 *     // a waiting cloud is still counting down to when it starts working
 *     Chat.log(`${cloud.getRadius()} blocks of ${cloud.getParticleType()}, waiting: ${cloud.isWaiting()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class AreaEffectCloudEntityHelper extends EntityHelper<AreaEffectCloud> {

    public AreaEffectCloudEntityHelper(AreaEffectCloud e) {
        super(e);
    }

    /**
     * the radius of this cloud in blocks, which is {@code 3.0} for a cloud that has not been
     * given a size.
     * <p>
     * This is a radius rather than a width, so a cloud of radius {@code 3.0} covers six blocks
     * across. The game clamps it to between {@code 0.0} and {@code 32.0} when it is set, so
     * nothing here is ever outside that, and the cloud's own hitbox is twice this on x and z
     * and half a block high whatever the radius.
     * example:
     * <pre>
     * const AreaEffectCloudEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other.AreaEffectCloudEntityHelper");
     * const clouds = World.getEntities(32, "area_effect_cloud");
     * if (clouds !== null) {
     *   for (const entity of clouds) {
     *     const cloud = AreaEffectCloudEntityHelper.class.cast(entity);
     *     // a radius, so the cloud is this many blocks out from the middle
     *     Chat.log(`reaches ${cloud.getRadius()} blocks out`);
     *   }
     * }
     * </pre>
     *
     * @return the radius of this cloud.
     * @since 1.8.4
     */
    public float getRadius() {
        return base.getRadius();
    }

    /**
     * The packed colour of the cloud's particles, rather than its scoreboard team colour.
     * A custom particle without colour components returns {@code -1}.
     * <p>
     * A packed colour is {@code 0xAARRGGBB}, so the top byte is the alpha and is {@code 0xFF} for
     * a fully opaque particle.
     * example:
     * <pre>
     * const AreaEffectCloudEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other.AreaEffectCloudEntityHelper");
     * const clouds = World.getEntities(32, "area_effect_cloud");
     * if (clouds !== null) {
     *   for (const entity of clouds) {
     *     const cloud = AreaEffectCloudEntityHelper.class.cast(entity);
     *     const color = cloud.getColor();
     *     // -1 is also opaque white, so it does not prove that the particle lacks a color
     *     Chat.log(`particle color 0x${(color >>> 0).toString(16).padStart(8, "0")}`);
     *   }
     * }
     * </pre>
     *
     * @return the color of this cloud's particles, or {@code -1} if its custom particle has no color.
     * @since 1.8.4
     */
    public int getColor() {
        if (base.getParticle() instanceof ColorParticleOption particle) {
            return ARGB.colorFromFloat(particle.getAlpha(), particle.getRed(), particle.getGreen(), particle.getBlue());
        }
        return -1;
    }

    /**
     * the registry id of the particle the cloud is drawing, such as
     * {@code minecraft:entity_effect}.
     * <p>
     * This is the type of the particle rather than the whole particle option, so for a cloud
     * carrying a colour the id names the effect particle and the colour is not part of it. A
     * cloud with no particle of its own takes the one its potion contents give it, which for an
     * ordinary potion is the effect particle, so that is what a splash potion answers here.
     * <p>
     * The id is namespaced and does not change with a rename, which makes it the thing to
     * compare against rather than a name.
     * example:
     * <pre>
     * const AreaEffectCloudEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other.AreaEffectCloudEntityHelper");
     * const clouds = World.getEntities(32, "area_effect_cloud");
     * if (clouds !== null) {
     *   for (const entity of clouds) {
     *     const cloud = AreaEffectCloudEntityHelper.class.cast(entity);
     *     // the id, not a name, so a rename does not break the comparison
     *     if (cloud.getParticleType() === "minecraft:entity_effect") {
     *       Chat.log("an ordinary potion cloud");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the id of this cloud's particles.
     * @since 1.8.4
     */
    @DocletReplaceReturn("ParticleId")
    public String getParticleType() {
        return BuiltInRegistries.PARTICLE_TYPE.getKey(base.getParticle().getType()).toString();
    }

    /**
     * whether the cloud is still in its delay before it starts applying its effect, which is
     * {@code false} for a cloud that is already working.
     * <p>
     * A cloud waits for a short fixed time after it spawns before it begins, and this is that
     * flag. The two visible differences while it is set are that the cloud only puffs a couple
     * of particles in a small area rather than filling its whole radius, and that the effect is
     * not being applied yet. The game clears it once the wait is over, and nothing on this class
     * sets it.
     * example:
     * <pre>
     * const AreaEffectCloudEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other.AreaEffectCloudEntityHelper");
     * const clouds = World.getEntities(32, "area_effect_cloud");
     * if (clouds !== null) {
     *   for (const entity of clouds) {
     *     const cloud = AreaEffectCloudEntityHelper.class.cast(entity);
     *     // a waiting cloud is not applying its effect yet
     *     if (cloud.isWaiting()) {
     *       Chat.log("still counting down before it starts working");
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the cloud is waiting before it starts working, {@code false}
     * otherwise
     * @since 1.8.4
     */
    public boolean isWaiting() {
        return base.isWaiting();
    }

}
