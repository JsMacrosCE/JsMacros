package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.AreaEffectCloud;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class AreaEffectCloudEntityHelper extends EntityHelper<AreaEffectCloud> {

    public AreaEffectCloudEntityHelper(AreaEffectCloud e) {
        super(e);
    }

    /**
     * @return the radius of this cloud.
     * @since 1.8.4
     */
    public float getRadius() {
        return base.getRadius();
    }

    /**
     * @return the color of this cloud.
     * @since 1.8.4
     */
    public int getColor() {
        return base.getTeamColor();
    }

    /**
     * @return the id of this cloud's particles.
     * @since 1.8.4
     */
    @DocletReplaceReturn("ParticleId")
    public String getParticleType() {
        return BuiltInRegistries.PARTICLE_TYPE.getKey(base.getParticle().getType()).toString();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public boolean isWaiting() {
        return base.isWaiting();
    }

}
