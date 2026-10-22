package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display;

import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.AABB;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.api.math.Vec3D;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinDisplayEntity;

/**
 * the part of a display entity that is the same whichever thing is being shown, which is the
 * positioning, the facing rule, the light and the shadow. A display entity is a marker in the
 * world that draws a block, an item or a piece of text at an offset, a scale and an angle; what
 * it draws is what the three classes below this one add, and everything here is shared.
 * <p>
 * A display entity has no collision and cannot be hurt, and it has two positions rather than
 * one. The entity itself is moved instantly to wherever it is told to go, and
 * {@link #getLerpProgress(double) getLerpProgress()} reports how far along a {@code teleport_duration} it is. The five {@code getLerpTarget} calls use that progress with no partial tick of their own, so they read where the display has got to at the start of the current tick. On top of that the drawn thing sits at its own translation, scale and rotation
 * inside the display, and none of that is reachable from here.
 * <p>
 * Everything here is a read of the entity data the client was last sent, so it is what the
 * server has said rather than a decision the client made. The exception is the interpolation
 * progress, which only ever moves on the client: a display with no {@code interpolation_duration}
 * set answers {@code 1.0} on every call, and so does one on a dedicated server, where the
 * duration is never stored in the first place.
 * example:
 * <pre>
 * const DisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.DisplayEntityHelper");
 * const displays = World.getEntities(64, "text_display", "block_display", "item_display");
 * if (displays !== null) {
 *   for (const entity of displays) {
 *     const display = DisplayEntityHelper.class.cast(entity);
 *     // the box the client is using to decide whether to draw it at all
 *     const box = display.getVisibilityBoundingBox();
 *     Chat.log(`${entity.getType()} at ${entity.getPos()}, culled against ${box.getDeltaX()} by ${box.getDeltaY()}`);
 *   }
 * }
 * </pre>
 *
 * @author aMelonRind
 * @since 1.9.1
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class DisplayEntityHelper<T extends Display> extends EntityHelper<T> {

    public DisplayEntityHelper(T base) {
        super(base);
    }

    /**
     * the horizontal x of the display along its own teleport, which is the line between where
     * it was last tick and where the entity now is.
     * <p>
     * This is the whole entity's position, not the drawn thing's, so a display showing a block
     * with a translation on it puts the drawn block somewhere else entirely. What is being
     * reported is the lerp of last tick's x and the current x at
     * {@link #getLerpProgress(double) the display's own progress}, so a display that is not
     * interpolating answers its current x and one that has just started answering is still
     * somewhere between the two.
     * example:
     * <pre>
     * const DisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.DisplayEntityHelper");
     * const displays = World.getEntities(64, "block_display");
     * if (displays !== null) {
     *   for (const entity of displays) {
     *     const display = DisplayEntityHelper.class.cast(entity);
     *     // how far along the teleport it is, out of 0 to 1
     *     Chat.log(`progress ${display.getLerpProgress(0).toFixed(2)}, x ${display.getLerpTargetX().toFixed(2)}`);
     *   }
     * }
     * </pre>
     *
     * @return the interpolated x of the display entity
     * @since 1.9.1
     */
    public double getLerpTargetX() {
        return base.getPosition(getLerpProgress(0)).x;
    }

    /**
     * the height of the display along its own teleport, which is the lerp of last tick's y and
     * the current y at the display's interpolation progress.
     * <p>
     * This is the entity's own y, which is the point the drawn block, item or text is placed
     * at, rather than the middle of what is drawn and rather than the eye height the shared
     * entity helpers report.
     * example:
     * <pre>
     * const DisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.DisplayEntityHelper");
     * const displays = World.getEntities(64, "text_display");
     * if (displays !== null) {
     *   for (const entity of displays) {
     *     const display = DisplayEntityHelper.class.cast(entity);
     *     // the bottom of the display, not its middle
     *     Chat.log(`floating at y ${display.getLerpTargetY().toFixed(2)}`);
     *   }
     * }
     * </pre>
     *
     * @return the interpolated y of the display entity
     * @since 1.9.1
     */
    public double getLerpTargetY() {
        return base.getPosition(getLerpProgress(0)).y;
    }

    /**
     * the horizontal z of the display along its own teleport, which is the lerp of last tick's z
     * and the current z at the display's interpolation progress.
     * <p>
     * The counterpart to {@link #getLerpTargetX() getLerpTargetX()} on the other horizontal
     * axis, and the same entity position rather than the position of the block, item or text
     * being drawn.
     * example:
     * <pre>
     * const DisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.DisplayEntityHelper");
     * const displays = World.getEntities(64, "text_display");
     * if (displays !== null) {
     *   for (const entity of displays) {
     *     const display = DisplayEntityHelper.class.cast(entity);
     *     Chat.log(`floating at z ${display.getLerpTargetZ().toFixed(2)}`);
     *   }
     * }
     * </pre>
     *
     * @return the interpolated z of the display entity
     * @since 1.9.1
     */
    public double getLerpTargetZ() {
        return base.getPosition(getLerpProgress(0)).z;
    }

    /**
     * the display's pitch along its own teleport, which is the lerp of last tick's pitch and the
     * current one at the display's interpolation progress.
     * <p>
     * A positive pitch looks down, the same way it does everywhere else in the game. This is
     * the entity's own pitch and is independent of the left and right rotations inside the
     * display's transformation, which tilt whatever it is drawing instead.
     * example:
     * <pre>
     * const DisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.DisplayEntityHelper");
     * const displays = World.getEntities(64, "text_display");
     * if (displays !== null) {
     *   for (const entity of displays) {
     *     const display = DisplayEntityHelper.class.cast(entity);
     *     // pitch is the nod up and down, yaw is the turn
     *     Chat.log(`pitch ${display.getLerpTargetPitch().toFixed(1)}, yaw ${display.getLerpTargetYaw().toFixed(1)}`);
     *   }
     * }
     * </pre>
     *
     * @return the interpolated pitch of the display entity
     * @since 1.9.1
     */
    public float getLerpTargetPitch() {
        return base.getXRot(getLerpProgress(0));
    }

    /**
     * the display's yaw along its own teleport, which is the lerp of last tick's yaw and the
     * current one at the display's interpolation progress.
     * <p>
     * This is the turn of the entity, the same value {@link #getYaw() getYaw()} reports without
     * the interpolation. It is worth having separately because the two can disagree by a long
     * way while a display is in the middle of a teleport.
     * example:
     * <pre>
     * const DisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.DisplayEntityHelper");
     * const displays = World.getEntities(64, "text_display");
     * if (displays !== null) {
     *   for (const entity of displays) {
     *     const display = DisplayEntityHelper.class.cast(entity);
     *     // the yaw the display is actually drawn at, mid teleport
     *     Chat.log(`drawn at yaw ${display.getLerpTargetYaw().toFixed(1)}`);
     *   }
     * }
     * </pre>
     *
     * @return the interpolated yaw of the display entity
     * @since 1.9.1
     */
    public float getLerpTargetYaw() {
        return base.getYRot(getLerpProgress(0));
    }

    /**
     * the box the client would use to decide whether the display is close enough to draw, as
     * a segment whose start is the minimum corner and whose end is the maximum corner.
     * <p>
     * This is not the box a ray cast hits. It is built from the entity's position and its width
     * and height, with the width used as a half extent on x and z and the height as the distance from the entity's y up to the top, and it is rebuilt whenever the width or the height changes and whenever the entity moves. The drawn block, item or text lives inside the display's own
     * transformation and is not taken into account, so a display with a zero width and height
     * has a collapsed box here even though it can be showing something.
     * <p>
     * Since it is a {@link Vec3D} rather than a box type, {@code x1} to {@code z1} are the
     * minimum corner and {@code x2} to {@code z2} the maximum, and the differences between
     * them are the size of the box.
     * example:
     * <pre>
     * const DisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.DisplayEntityHelper");
     * const displays = World.getEntities(64, "block_display");
     * if (displays !== null) {
     *   for (const entity of displays) {
     *     const display = DisplayEntityHelper.class.cast(entity);
     *     const box = display.getVisibilityBoundingBox();
     *     // a zero width or height collapses this box, and exempts
     // the display from culling rather than hiding it
     *     Chat.log(`culling box ${box.getDeltaX()} by ${box.getDeltaY()} by ${box.getDeltaZ()}`);
     *   }
     * }
     * </pre>
     *
     * @return the culling box, from its minimum corner to its maximum corner
     * @since 1.9.1
     */
    public Vec3D getVisibilityBoundingBox() {
        AABB box = base.getBoundingBoxForCulling();
        return new Vec3D(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    /**
     * how the display turns to face the camera, which is the rule that makes a hologram or a
     * floating name track the viewer.
     * <p>
     * The value is the id the game writes to the entity data, so it is one of {@code "fixed"}, {@code "vertical"}, {@code "horizontal"} or {@code "center"}, and {@code "fixed"} is what a display with no billboard set reports. It is one of four settings rather than an angle: the entity's own yaw is a separate number, which {@link #getYaw() getYaw()} reports, and the two are not the same thing.
     * example:
     * <pre>
     * const DisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.DisplayEntityHelper");
     * const displays = World.getEntities(64, "text_display");
     * if (displays !== null) {
     *   for (const entity of displays) {
     *     const display = DisplayEntityHelper.class.cast(entity);
     *     if (display.getBillboardMode() !== "fixed") {
     *       Chat.log(`this one turns to face you: ${display.getBillboardMode()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return "fixed", "vertical", "horizontal" or "center"
     * @since 1.9.1
     */
    public String getBillboardMode() {
        return ((MixinDisplayEntity) base).callGetBillboardMode().getSerializedName();
    }

    /**
     * the brighter of the two light levels the display's brightness override asks for, or
     * {@code 0} when there is no override at all.
     * <p>
     * The override is the {@code brightness} the display was given, which pins it to a fixed
     * light level and stops the game working it out from the blocks around it. A display with
     * none set reports {@code 0} from all three of the brightness calls, which is a real
     * light level rather than a marker, so there is no way to tell an override of {@code 0}
     * from no override by reading this.
     * example:
     * <pre>
     * const DisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.DisplayEntityHelper");
     * const displays = World.getEntities(64, "text_display");
     * if (displays !== null) {
     *   for (const entity of displays) {
     *     const display = DisplayEntityHelper.class.cast(entity);
     *     // 15 on both sides means the display is at full brightness whatever the light is
     *     Chat.log(`brightness ${display.getBrightness()} of 15`);
     *   }
     * }
     * </pre>
     *
     * @return the larger of the two light levels, from 0 to 15, or 0 with no override
     * @since 1.9.1
     */
    public int getBrightness() {
        Brightness bri = ((MixinDisplayEntity) base).callGetBrightnessUnpacked();
        return bri == null ? 0 : Math.max(bri.sky(), bri.block());
    }

    /**
     * the sky half of the display's brightness override, or {@code 0} when there is no override.
     * <p>
     * The override carries two levels rather than one, the sky and the block, and
     * {@link #getBrightness() getBrightness()} reports whichever of the two is higher. Reading
     * this and {@link #getBlockBrightness() getBlockBrightness()} separately is the way to see
     * an override that deliberately makes the two differ.
     * example:
     * <pre>
     * const DisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.DisplayEntityHelper");
     * const displays = World.getEntities(64, "text_display");
     * if (displays !== null) {
     *   for (const entity of displays) {
     *     const display = DisplayEntityHelper.class.cast(entity);
     *     Chat.log(`sky ${display.getSkyBrightness()}, block ${display.getBlockBrightness()}`);
     *   }
     * }
     * </pre>
     *
     * @return the sky light level of the override, from 0 to 15, or 0 with no override
     * @since 1.9.1
     */
    public int getSkyBrightness() {
        Brightness bri = ((MixinDisplayEntity) base).callGetBrightnessUnpacked();
        return bri == null ? 0 : bri.sky();
    }

    /**
     * the block half of the display's brightness override, or {@code 0} when there is no
     * override.
     * <p>
     * The other of the two levels {@link #getSkyBrightness() getSkyBrightness()} reports. The two halves are independent, so a display can be pinned bright on one and dark on the other, and the only way to see them differ is to read both.
     * example:
     * <pre>
     * const DisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.DisplayEntityHelper");
     * const displays = World.getEntities(64, "text_display");
     * if (displays !== null) {
     *   for (const entity of displays) {
     *     const display = DisplayEntityHelper.class.cast(entity);
     *     // a block level of 0 with a sky level of 15 is a display lit only by daylight
     *     if (display.getBlockBrightness() !== display.getSkyBrightness()) {
     *       Chat.log(`lit ${display.getBlockBrightness()} by blocks and ${display.getSkyBrightness()} by the sky`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the block light level of the override, from 0 to 15, or 0 with no override
     * @since 1.9.1
     */
    public int getBlockBrightness() {
        Brightness bri = ((MixinDisplayEntity) base).callGetBrightnessUnpacked();
        return bri == null ? 0 : bri.block();
    }

    /**
     * how far away the display is still drawn, as a multiple of the game's own render distance,
     * and {@code 1.0} is what a display with no view range set reports.
     * <p>
     * The check the game makes is against this value times sixty four, so a view range of {@code 2.0} reaches twice as far as the default and a view range of {@code 0.0} takes the display out of the world entirely. Nothing else uses the number, so this is a distance and not a size: it does not change how big the display is drawn.
     * example:
     * <pre>
     * const DisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.DisplayEntityHelper");
     * const displays = World.getEntities(64, "text_display");
     * if (displays !== null) {
     *   for (const entity of displays) {
     *     const display = DisplayEntityHelper.class.cast(entity);
     *     if (display.getViewRange() > 1.0) {
     *       Chat.log(`this one draws ${display.getViewRange() * 64} blocks out`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the view range multiplier, 1.0 by default
     * @since 1.9.1
     */
    public float getViewRange() {
        return ((MixinDisplayEntity) base).callGetViewRange();
    }

    /**
     * the radius of the shadow the display casts, and {@code 0.0} is what a display with none set reports.
     * <p>
     * This is the size of the shadow rather than how dark it is, which is {@link #getShadowStrength() getShadowStrength()}. The shadow is cast at the display's own position rather than at the block, item or text it is showing, because the transformation placing the drawn thing is applied after the shadow has been drawn, so a display translated off the ground casts its shadow back at the entity.
     * example:
     * <pre>
     * const DisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.DisplayEntityHelper");
     * const displays = World.getEntities(64, "block_display");
     * if (displays !== null) {
     *   for (const entity of displays) {
     *     const display = DisplayEntityHelper.class.cast(entity);
     *     if (display.getShadowRadius() > 0.0) {
     *       Chat.log(`shadow ${display.getShadowRadius()} across at ${display.getShadowStrength()} opacity`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the shadow radius, 0.0 by default
     * @since 1.9.1
     */
    public float getShadowRadius() {
        return ((MixinDisplayEntity) base).callGetShadowRadius();
    }

    /**
     * how dark the display's shadow is, and {@code 1.0} is what a display with none set reports.
     * <p>
     * The other half of the shadow, with {@link #getShadowRadius() getShadowRadius()} giving its size. The two are separate stored numbers rather than one setting, so a display can be given a large radius and a strength of its own.
     * example:
     * <pre>
     * const DisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.DisplayEntityHelper");
     * const displays = World.getEntities(64, "block_display");
     * if (displays !== null) {
     *   for (const entity of displays) {
     *     const display = DisplayEntityHelper.class.cast(entity);
     *     // a zero strength with a real radius is a shadow that is there but cannot be seen
     *     if (display.getShadowRadius() > 0.0) {
     *       if (display.getShadowStrength() === 0.0) {
     *         Chat.log(`an invisible shadow under the display at ${entity.getPos()}`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the shadow strength, 1.0 by default
     * @since 1.9.1
     */
    public float getShadowStrength() {
        return ((MixinDisplayEntity) base).callGetShadowStrength();
    }

    /**
     * the display's width, and {@code 0.0} is the default.
     * <p>
     * This is the display's own width rather than the width of the block it is showing, and it
     * is a half extent on the x and z axes, so a width of {@code 1.0} is one block across. It
     * is what {@link #getVisibilityBoundingBox() getVisibilityBoundingBox()} is built from on those two axes. A width of {@code 0.0} exempts the display from culling, which means it is drawn wherever its chunks are loaded rather than making it invisible.
     * example:
     * <pre>
     * const DisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.DisplayEntityHelper");
     * const displays = World.getEntities(64, "block_display");
     * if (displays !== null) {
     *   for (const entity of displays) {
     *     const display = DisplayEntityHelper.class.cast(entity);
     *     // width and height are what the client culls against, not what is drawn
     *     Chat.log(`culled against ${display.getDisplayWidth()} by ${display.getDisplayHeight()}`);
     *   }
     * }
     * </pre>
     *
     * @return the display width, 0.0 by default
     * @since 1.9.1
     */
    public float getDisplayWidth() {
        return ((MixinDisplayEntity) base).callGetDisplayWidth();
    }

    /**
     * the display's own glow colour as a packed ARGB integer, or {@code -1} when it has not been
     * given one.
     * <p>
     * This is the {@code glow_color_override} the display was given, and {@code -1} is the
     * value that means it is unset. The inherited
     * {@link EntityHelper#getGlowingColor() getGlowingColor()} is the one to read for the
     * colour a display is actually drawn with: it answers this override when there is one and
     * falls back to the scoreboard team colour when there is not, so the two disagree for any
     * display that is not overridden and not on a team.
     * <p>
     * A packed colour is {@code 0xAARRGGBB}, so the top byte is the alpha, which is fully opaque
     * at {@code 0xFF}, and {@code -1} is all four bytes set.
     * example:
     * <pre>
     * const DisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.DisplayEntityHelper");
     * const displays = World.getEntities(64, "text_display");
     * if (displays !== null) {
     *   for (const entity of displays) {
     *     const display = DisplayEntityHelper.class.cast(entity);
     *     const override = display.getGlowColorOverride();
     *     if (override !== -1) {
     *       // the override is the whole packed colour, alpha included
     *       Chat.log(`glow colour 0x${override.toString(16).padStart(8, "0")}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the packed glow colour, or -1 if there is no override
     * @since 1.9.1
     */
    public int getGlowColorOverride() {
        return ((MixinDisplayEntity) base).callGetGlowColorOverride();
    }

    /**
     * how far along a teleport the display is, from {@code 0.0} at the start to {@code 1.0} at
     * the end, and {@code 1.0} whenever it is not teleporting.
     * <p>
     * The number is the position's own {@code teleport_duration} measured in ticks, with
     * {@code delta} added to the ticks elapsed so far before the fraction is taken, so passing
     * {@code 0} gives the progress at the start of the current tick and {@code 1} at the end
     * of it. The result is clamped into the range, so a display whose delay has run on answers {@code 1.0} rather than going past it.
     * <p>
     * A display that was never given a {@code teleport_duration} has a duration of zero and
     * answers {@code 1.0} whatever {@code delta} is. The same is true of a display on a
     * dedicated server, since the duration is only stored on the client.
     * <p>
     * The five {@code getLerpTarget} calls on this class are this number fed into the position
     * and rotation lerps, which is where the answer actually shows up.
     * example:
     * <pre>
     * const DisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.DisplayEntityHelper");
     * const displays = World.getEntities(64, "text_display");
     * if (displays !== null) {
     *   for (const entity of displays) {
     *     const display = DisplayEntityHelper.class.cast(entity);
     *     const now = display.getLerpProgress(0);
     *     // anything short of 1.0 means the display is still on its way
     *     if (1.0 > now) {
     *       Chat.log(`mid teleport, ${(now * 100).toFixed(0)} percent of the way to ${display.getLerpTargetX()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @param delta the fraction of a tick to add to the elapsed ticks, normally 0 or 1
     * @return the interpolation progress, from 0.0 to 1.0
     * @since 1.9.1
     */
    public float getLerpProgress(double delta) {
        return base.calculateInterpolationProgress((float) delta);
    }

    /**
     * the display's height, and {@code 0.0} is the default.
     * <p>
     * The counterpart to {@link #getDisplayWidth() getDisplayWidth()} on the y axis, where it
     * is the distance from the display's own y up to the top of the culling box rather than a half extent. A height of {@code 0.0} exempts the display from culling in the same way a width of {@code 0.0} does, and either one on its own is enough to do it.
     * example:
     * <pre>
     * const DisplayEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.DisplayEntityHelper");
     * const displays = World.getEntities(64, "block_display");
     * if (displays !== null) {
     *   for (const entity of displays) {
     *     const display = DisplayEntityHelper.class.cast(entity);
     *     if (display.getDisplayHeight() > 0.0) {
     *       Chat.log(`${display.getDisplayHeight()} blocks of culling box above the display`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the display height, 0.0 by default
     * @since 1.9.1
     */
    public float getDisplayHeight() {
        return ((MixinDisplayEntity) base).callGetDisplayHeight();
    }

}
