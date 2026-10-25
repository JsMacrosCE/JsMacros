package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display;

import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.AABB;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.api.math.Vec3D;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinDisplayEntity;

/**
 * Provides properties shared by block, item, and text display entities. Teleport interpolation
 * applies to the entity's position and rotation, not the displayed content's transformation.
 * Interpolation progress is client-side and is {@code 1.0} when no duration is set.
 * <pre>
 * const displays = World.getEntities(64, "text_display", "block_display", "item_display");
 * if (displays !== null) {
 *   for (const entity of displays) {
 *     if (!(entity instanceof EntityHelpers.Display)) continue;
 *     // the display's culling bounds at its current position
 *     const box = entity.getVisibilityBoundingBox();
 *     Chat.log(`${entity.getType()} at ${entity.getPos()}, culling bounds ${box.getDeltaX()} by ${box.getDeltaY()}`);
 *   }
 * }
 * </pre>
 *
 * @param <T> the display entity wrapped by this helper
 * @author aMelonRind
 * @since 1.9.1
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class DisplayEntityHelper<T extends Display> extends EntityHelper<T> {

    /** Wraps the supplied display entity without copying it.
     * @param base display entity to wrap
     */
    public DisplayEntityHelper(T base) {
        super(base);
    }

    /**
     * Returns the display entity's x position interpolated at {@link #getLerpProgress(double)}.
     *
     * @return the interpolated x of the display entity
     * @since 1.9.1
     */
    public double getLerpTargetX() {
        return base.getPosition(getLerpProgress(0)).x;
    }

    /**
     * Returns the display entity's y position interpolated at {@link #getLerpProgress(double)}.
     *
     * @return the interpolated y of the display entity
     * @since 1.9.1
     */
    public double getLerpTargetY() {
        return base.getPosition(getLerpProgress(0)).y;
    }

    /**
     * Returns the display entity's z position interpolated at {@link #getLerpProgress(double)}.
     *
     * @return the interpolated z of the display entity
     * @since 1.9.1
     */
    public double getLerpTargetZ() {
        return base.getPosition(getLerpProgress(0)).z;
    }

    /**
     * Returns the display entity's pitch interpolated at {@link #getLerpProgress(double)}.
     *
     * @return the interpolated pitch of the display entity
     * @since 1.9.1
     */
    public float getLerpTargetPitch() {
        return base.getXRot(getLerpProgress(0));
    }

    /**
     * Returns the display entity's yaw interpolated at {@link #getLerpProgress(double)}.
     *
     * @return the interpolated yaw of the display entity
     * @since 1.9.1
     */
    public float getLerpTargetYaw() {
        return base.getYRot(getLerpProgress(0));
    }

    /**
     * Returns the display's culling box at its current position, sized by its display width and height.
     * Content transformations are excluded. A zero width or height disables culling but leaves
     * that dimension of the returned box collapsed.
     *
     * @return the culling box, from its minimum corner to its maximum corner
     * @since 1.9.1
     */
    public Vec3D getVisibilityBoundingBox() {
        //? if >=26.3 {
        /*AABB box = base.getInterpolatedBoundingBox(1.0F);
        *///?} else {
        AABB box = base.getBoundingBoxForCulling();
        //?}
        return new Vec3D(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    /**
     * Returns the camera-facing constraint: {@code "fixed"}, {@code "vertical"},
     * {@code "horizontal"}, or {@code "center"}. An unset constraint reports {@code "fixed"};
     * this is separate from the entity's yaw.
     *
     * @return "fixed", "vertical", "horizontal" or "center"
     * @since 1.9.1
     */
    public String getBillboardMode() {
        return ((MixinDisplayEntity) base).callGetBillboardMode().getSerializedName();
    }

    /**
     * Returns the larger of the sky and block levels in the brightness override, or {@code 0}
     * when no override is set. Consequently, {@code 0} does not distinguish an unset override
     * from an override whose levels are both zero.
     *
     * @return the larger of the two light levels, from 0 to 15, or 0 with no override
     * @since 1.9.1
     */
    public int getBrightness() {
        Brightness bri = ((MixinDisplayEntity) base).callGetBrightnessUnpacked();
        return bri == null ? 0 : Math.max(bri.sky(), bri.block());
    }

    /**
     * Returns the sky-light component of the brightness override, or {@code 0} when unset.
     *
     * @return the sky light level of the override, from 0 to 15, or 0 with no override
     * @since 1.9.1
     */
    public int getSkyBrightness() {
        Brightness bri = ((MixinDisplayEntity) base).callGetBrightnessUnpacked();
        return bri == null ? 0 : bri.sky();
    }

    /**
     * Returns the block-light component of the brightness override, or {@code 0} when unset.
     *
     * @return the block light level of the override, from 0 to 15, or 0 with no override
     * @since 1.9.1
     */
    public int getBlockBrightness() {
        Brightness bri = ((MixinDisplayEntity) base).callGetBrightnessUnpacked();
        return bri == null ? 0 : bri.block();
    }

    /**
     * Returns the display's render-distance multiplier; the default is {@code 1.0}. The game
     * tests render distance against {@code viewRange * 64 * viewScale} blocks; this does not
     * change the display's size.
     *
     * @return the view range multiplier, 1.0 by default
     * @since 1.9.1
     */
    public float getViewRange() {
        return ((MixinDisplayEntity) base).callGetViewRange();
    }

    /**
     * Returns the display shadow's radius; the default is {@code 0.0}. Its strength is reported
     * separately by {@link #getShadowStrength() getShadowStrength()}.
     *
     * @return the shadow radius, 0.0 by default
     * @since 1.9.1
     */
    public float getShadowRadius() {
        return ((MixinDisplayEntity) base).callGetShadowRadius();
    }

    /**
     * Returns the display shadow's strength; the default is {@code 1.0}. Its radius is reported
     * separately by {@link #getShadowRadius() getShadowRadius()}.
     *
     * @return the shadow strength, 1.0 by default
     * @since 1.9.1
     */
    public float getShadowStrength() {
        return ((MixinDisplayEntity) base).callGetShadowStrength();
    }

    /**
     * Returns the width used to construct the display's culling box; zero by default. It is a
     * full extent on each horizontal axis, and a zero width disables culling.
     *
     * @return the display width, 0.0 by default
     * @since 1.9.1
     */
    public float getDisplayWidth() {
        return ((MixinDisplayEntity) base).callGetDisplayWidth();
    }

    /**
     * Returns the display's glow-colour override, or {@code -1} when unset. The inherited
     * {@link EntityHelper#getGlowingColor() getGlowingColor()} reports the entity's team colour;
     * for a display, Minecraft uses this override when determining its outline colour.
     *
     * @return the packed glow colour, or -1 if there is no override
     * @since 1.9.1
     */
    public int getGlowColorOverride() {
        return ((MixinDisplayEntity) base).callGetGlowColorOverride();
    }

    /**
     * Returns the clamped teleport interpolation progress from {@code 0.0} to {@code 1.0}; it
     * is {@code 1.0} when no interpolation duration is set. {@code delta} is added to elapsed
     * ticks before calculating progress, so {@code 0} and {@code 1} sample the current tick's
     * start and end respectively.
     *
     * @param delta the fraction of a tick to add to the elapsed ticks, normally 0 or 1
     * @return the interpolation progress, from 0.0 to 1.0
     * @since 1.9.1
     */
    public float getLerpProgress(double delta) {
        return base.calculateInterpolationProgress((float) delta);
    }

    /**
     * Returns the height used to construct the display's culling box, measured upward from its
     * position; zero by default. A zero height disables culling.
     *
     * @return the display height, 0.0 by default
     * @since 1.9.1
     */
    public float getDisplayHeight() {
        return ((MixinDisplayEntity) base).callGetDisplayHeight();
    }

}
