package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.AreaEffectCloud;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * A lingering area-effect cloud applies its potion effects over time. Its particle may be
 * customized independently of its potion contents.
 * <pre>
 * const AreaEffectCloudEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other.AreaEffectCloudEntityHelper");
 * const clouds = World.getEntities(32, "area_effect_cloud");
 * if (clouds !== null) {
 *   for (const entity of clouds) {
 *     const cloud = AreaEffectCloudEntityHelper.class.cast(entity);
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

    /** Wraps the supplied area-effect cloud by reference.
     * @param e the area-effect cloud entity to wrap
     */
    public AreaEffectCloudEntityHelper(AreaEffectCloud e) {
        super(e);
    }

    /**
     * Returns the cloud's radius in blocks; the default is {@code 3.0}. Its bounding-box width
     * is twice the radius and its height is {@code 0.5} blocks.
     *
     * @return the radius of this cloud.
     * @since 1.8.4
     */
    public float getRadius() {
        return base.getRadius();
    }

    /**
     * Returns the particle's packed {@code 0xAARRGGBB} color, or {@code -1} when the particle
     * option is not a color particle. The sentinel is also the packed value for opaque white.
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
     * Returns the namespaced registry id of the particle type, not its particle options (such
     * as color). When no custom particle is set, the type is {@code minecraft:entity_effect}.
     *
     * @return the id of this cloud's particles.
     * @since 1.8.4
     */
    @DocletReplaceReturn("ParticleId")
    public String getParticleType() {
        return BuiltInRegistries.PARTICLE_TYPE.getKey(base.getParticle().getType()).toString();
    }

    /**
     * Returns whether the cloud is in its waiting state. While waiting, it emits fewer particles
     * and does not apply its effects.
     *
     * @return {@code true} if the cloud is waiting before it starts working, {@code false}
     * otherwise
     * @since 1.8.4
     */
    public boolean isWaiting() {
        return base.isWaiting();
    }

}
