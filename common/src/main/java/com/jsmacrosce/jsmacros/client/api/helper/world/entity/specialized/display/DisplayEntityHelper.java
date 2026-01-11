package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display;

import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.AABB;
import com.jsmacrosce.jsmacros.api.math.Vec3D;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinDisplayEntity;

/**
 * @author aMelonRind
 * @since 1.9.1
 */
@SuppressWarnings("unused")
public class DisplayEntityHelper<T extends Display> extends EntityHelper<T> {

    public DisplayEntityHelper(T base) {
        super(base);
    }

    /**
     * @since 1.9.1
     */
    public double getLerpTargetX() {
        return base.getPosition(getLerpProgress(0)).x;
    }

    /**
     * @since 1.9.1
     */
    public double getLerpTargetY() {
        return base.getPosition(getLerpProgress(0)).y;
    }

    /**
     * @since 1.9.1
     */
    public double getLerpTargetZ() {
        return base.getPosition(getLerpProgress(0)).z;
    }

    /**
     * @since 1.9.1
     */
    public float getLerpTargetPitch() {
        return base.getXRot(getLerpProgress(0));
    }

    /**
     * @since 1.9.1
     */
    public float getLerpTargetYaw() {
        return base.getYRot(getLerpProgress(0));
    }

    /**
     * @since 1.9.1
     */
    public Vec3D getVisibilityBoundingBox() {
        AABB box = base.getBoundingBoxForCulling();
        return new Vec3D(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    /**
     * @return "fixed", "vertical", "horizontal" or "center"
     * @since 1.9.1
     */
    public String getBillboardMode() {
        return ((MixinDisplayEntity) base).callGetBillboardMode().getSerializedName();
    }

    /**
     * @since 1.9.1
     */
    public int getBrightness() {
        Brightness bri = ((MixinDisplayEntity) base).callGetBrightnessUnpacked();
        return bri == null ? 0 : Math.max(bri.sky(), bri.block());
    }

    /**
     * @since 1.9.1
     */
    public int getSkyBrightness() {
        Brightness bri = ((MixinDisplayEntity) base).callGetBrightnessUnpacked();
        return bri == null ? 0 : bri.sky();
    }

    /**
     * @since 1.9.1
     */
    public int getBlockBrightness() {
        Brightness bri = ((MixinDisplayEntity) base).callGetBrightnessUnpacked();
        return bri == null ? 0 : bri.block();
    }

    /**
     * @since 1.9.1
     */
    public float getViewRange() {
        return ((MixinDisplayEntity) base).callGetViewRange();
    }

    /**
     * @since 1.9.1
     */
    public float getShadowRadius() {
        return ((MixinDisplayEntity) base).callGetShadowRadius();
    }

    /**
     * @since 1.9.1
     */
    public float getShadowStrength() {
        return ((MixinDisplayEntity) base).callGetShadowStrength();
    }

    /**
     * @since 1.9.1
     */
    public float getDisplayWidth() {
        return ((MixinDisplayEntity) base).callGetDisplayWidth();
    }

    /**
     * @since 1.9.1
     */
    public int getGlowColorOverride() {
        return ((MixinDisplayEntity) base).callGetGlowColorOverride();
    }

    /**
     * @since 1.9.1
     */
    public float getLerpProgress(double delta) {
        return base.calculateInterpolationProgress((float) delta);
    }

    /**
     * @since 1.9.1
     */
    public float getDisplayHeight() {
        return ((MixinDisplayEntity) base).callGetDisplayHeight();
    }

}
