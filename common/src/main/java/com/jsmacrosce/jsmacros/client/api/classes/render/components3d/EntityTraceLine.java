package com.jsmacrosce.jsmacros.client.api.classes.render.components3d;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.util.ColorUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.client.api.classes.render.Draw3D;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

import static com.jsmacrosce.jsmacros.client.api.classes.render.components.RenderElement.mc;

/**
 * a {@link TraceLine} that follows an entity rather than a fixed point.
 * <p>
 * Everything a plain trace line does still applies: the line runs from the camera to
 * its target, so what changes is where the target is. This one asks the entity where
 * it is every frame, adds {@code yOffset} to the height, and points there. The line
 * therefore tracks a moving entity without a script having to move it.
 * <p>
 * The entity has to still be in the world the player is in. When it is not, when it
 * has been removed, or when there is no entity at all, the line marks itself for
 * removal and stops drawing, and the next frame the 3D overlay drops it for good. A
 * line built without an entity is removed on its first frame rather than sitting at
 * the origin.
 * <p>
 * Note that {@link #getPos()} is inherited and reports where the line is currently
 * pointing, which for this element is the entity's interpolated position plus the
 * offset. It reads {@code 0, 0, 0} until the first frame has drawn.
 * example:
 * <pre>
 * const player = Player.getPlayer();
 * if (player !== null) {
 *   const draw = Hud.createDraw3D();
 *   // a line from the crosshair to the player, 2.0 blocks above their feet
 *   draw.entityTraceLineBuilder().entity(player).color(0xFFFF0000).yOffset(2.0).buildAndAdd();
 *   draw.register();
 * }
 * </pre>
 *
 * @author aMelonRind
 * @since 1.9.0
 */
@SuppressWarnings("unused")
@DocletCategory("Rendering/Graphics")
public class EntityTraceLine extends TraceLine {
    /**
     * whether anything asked to be removed since the last time the overlays were drawn.
     * <p>
     * This is a flag on the class rather than on an instance, set by a line that has
     * found its entity gone and cleared by each 3D overlay at the start of its draw.
     * It exists so that the common case, where nothing has gone wrong, does not have
     * to walk the whole element list looking for lines to drop. A script has no reason
     * to read or set it.
     *
     * @since 1.9.0
     */
    public static boolean dirty = false;

    /**
     * the raw entity this line follows, or {@code null} if none was ever given.
     * <p>
     * This is the game's own entity object rather than the JsMacros wrapper, so it is
     * not something a script can read positions off. The helper it came from is what
     * a script should be holding; see the builder's {@code entity} for that.
     *
     * @since 1.9.0
     */
    @Nullable
    public Entity entity;
    /**
     * how far above the entity's feet the line points, in blocks. The default of
     * {@code 0.5} is a little under a third of the way up a player-sized entity, and it
     * clears the ground a line at the entity's own position would disappear into.
     *
     * @since 1.9.0
     */
    public double yOffset = 0.5;
    /**
     * whether this line has given up and is waiting to be dropped from its overlay.
     * <p>
     * Set by the line itself the first time it finds the entity gone, and cleared
     * again if a new entity is set on it. An overlay drops every line with this set
     * at the end of the frame it notices.
     *
     * @since 1.9.0
     */
    public boolean shouldRemove = false;

    public EntityTraceLine(@Nullable EntityHelper<?> entity, int color, double yOffset) {
        super(0, 0, 0, color);
        setEntity(entity);
        setYOffset(yOffset);
    }

    public EntityTraceLine(@Nullable EntityHelper<?> entity, int color, int alpha, double yOffset) {
        super(0, 0, 0, color, alpha);
        setEntity(entity);
        setYOffset(yOffset);
    }

    public EntityTraceLine(@Nullable EntityHelper<?> entity, int color, int alpha, double yOffset, boolean alwaysOnTop) {
        super(0, 0, 0, color, alpha, alwaysOnTop);
        setEntity(entity);
        setYOffset(yOffset);
    }

    /**
     * points this line at an entity from now on.
     * <p>
     * The entity is unwrapped to the game's own object and stored, and the line is
     * brought back from having given up, so a line that had already marked itself for
     * removal starts drawing again once it has something to point at.
     * <p>
     * A {@code null} argument does nothing at all and the line is left pointing at
     * whatever it was before. That is worth knowing, because a line that was built
     * without an entity is removed on its first frame and there is no way back from
     * here other than giving it a real one.
     * example:
     <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const draw = Hud.createDraw3D();
     *   const line = draw.entityTraceLineBuilder().color(0xFFFF0000).buildAndAdd();
     *   // decided later, once there is something to point at
     *   line.setEntity(player);
     *   draw.register();
     * }
     * </pre>
     *
     * @param entity the entity to follow, or {@code null} to leave the line as it is
     * @return self for chaining
     * @since 1.9.0
     */
    public EntityTraceLine setEntity(@Nullable EntityHelper<?> entity) {
        if (entity == null) return this;
        this.entity = entity.getRaw();
        shouldRemove = false;
        return this;
    }

    /**
     * how far above the entity's feet this line points, in blocks.
     * <p>
     * The offset is added to the entity's interpolated height every frame, so it moves
     * with the entity rather than being applied once. Zero aims at the entity's own
     * position, which for most entities is inside the ground.
     * example:
     <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const draw = Hud.createDraw3D();
     *   // aim at the player's eyes rather than their feet
     *   draw.addEntityTraceLine(player, 0xFFFF0000, 255, player.getEyeHeight());
     *   draw.register();
     * }
     * </pre>
     *
     * @param yOffset the offset above the entity's feet, in blocks
     * @return self for chaining
     * @since 1.9.0
     */
    public EntityTraceLine setYOffset(double yOffset) {
        this.yOffset = yOffset;
        return this;
    }

    /**
     * points the line at the entity and draws it.
     * <p>
     * The entity's interpolated position plus {@code yOffset} becomes the target, and
     * the start of the line is the camera as a plain trace line does it. When there is
     * no usable entity the line is not drawn at all: it marks itself for removal, sets
     * the class-wide flag so the overlays know to look for lines like it, and returns
     * before drawing anything.
     * <p>
     * This is called by the renderer once a frame. There is no reason for a script to
     * call it.
     *
     * @param matrixStack the world transform, already translated so the camera is at the origin
     * @param consumers the buffer source the geometry goes into
     * @param tickDelta how far into the current tick this frame is, from 0 to 1
     * @author aMelonRind
     * @since 1.9.0
     */
    @Override
    public void render(PoseStack matrixStack, MultiBufferSource consumers, float tickDelta) {
        if (shouldRemove || entity == null || entity.isRemoved() || entity.level() != mc.level) {
            shouldRemove = true;
            dirty = true;
            return;
        }

        Vec3 vec = entity.getPosition(tickDelta);
        setPos(vec.x, vec.y + yOffset, vec.z);
        super.render(matrixStack, consumers, tickDelta);
    }

    /**
     * collects the values for an {@link EntityTraceLine}, then builds it.
     * <p>
     * This is the same shape as the other 3D builders: a chain of setters, then
     * {@link #build()} to get the line without putting it anywhere, or
     * {@link #buildAndAdd()} to get it and add it to the overlay it was made from.
     * The defaults are a white line, fully opaque, always on top, aimed half a block
     * above the entity, and <em>no entity at all</em>, so a line built without naming
     * one is removed on its first frame.
     * <p>
     * The colour setters keep the colour and the alpha apart internally, which is why
     * there is a separate {@link #alpha(int)}: {@link #color(int)} taken on its own
     * picks the alpha up out of the colour it is given.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const draw = Hud.createDraw3D();
     *   draw.entityTraceLineBuilder()
     *      .entity(player)
     *      .color(0xFF00FF00, 128)
     *      .yOffset(1.5)
     *      .alwaysOnTop(false)
     *      .buildAndAdd();
     *   draw.register();
     * }
     * </pre>
     *
     * @author aMelonRind
     * @since 1.9.0
     */
    @DocletCategory("Rendering/Graphics")
    public static class Builder {
        private final Draw3D parent;

        @Nullable
        private EntityHelper<?> entity = null;
        private double yOffset = 0.5;
        private int color = 0xFFFFFF;
        private int alpha = 0xFF;
        private boolean alwaysOnTop = true;

        public Builder(Draw3D parent) {
            this.parent = parent;
        }

        /**
         * the entity the line will point at, which may be none.
         * <p>
         * Nothing is checked here, so a line can be built around no entity at all and
         * will be dropped on its first frame rather than pointing at the origin.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * const builder = draw.entityTraceLineBuilder();
         * const player = Player.getPlayer();
         * if (player !== null) {
         *   builder.entity(player);
         * }
         * // a line with no entity is removed on its first frame
         * builder.buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param entity the target entity
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder entity(@Nullable EntityHelper<?> entity) {
            this.entity = entity;
            return this;
        }

        /**
         * the entity this builder was given, or {@code null} if it has none.
         * <p>
         * This is the helper as it was handed over, not the unwrapped object the
         * built line holds, so it is the one a script can still read positions off.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().entityTraceLineBuilder();
         * const player = Player.getPlayer();
         * if (player !== null) {
         *   builder.entity(player);
         * }
         * Chat.log(builder.getEntity() === null ? "no target" : "has a target");
         * </pre>
         *
         * @return the target entity
         * @since 1.9.0
         */
        @Nullable
        public EntityHelper<?> getEntity() {
            return entity;
        }

        /**
         * how far above the entity's feet the built line will point, in blocks.
         * <p>
         * This is only the value being collected; the built line reads it every frame,
         * so the offset follows the entity rather than being applied once. The default
         * is {@code 0.5}.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().entityTraceLineBuilder();
         * builder.yOffset(3);
         * // the builder still has 0.5 until something is set
         * Chat.log(`y offset is now ${builder.getYOffset()}`);
         * </pre>
         *
         * @param yOffset the offset of y-axis
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder yOffset(double yOffset) {
            this.yOffset = yOffset;
            return this;
        }

        /**
         * the y offset collected so far, which is {@code 0.5} until one is set.
         * <p>
         * This is the builder's own value, not the built line's, so it says nothing
         * about a line that has already been made.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().entityTraceLineBuilder();
         * Chat.log(`default y offset is ${builder.getYOffset()}`);
         * </pre>
         *
         * @return the offset of y-axis
         * @since 1.9.0
         */
        public double getYOffset() {
            return yOffset;
        }

        /**
         * the colour collected so far, as one packed value.
         * <p>
         * The alpha is held separately on the builder and is not part of this number
         * unless it was set through {@link #color(int, int)}, so a colour set with the
         * one argument form reads back with an alpha byte of zero even though the
         * built line comes out fully opaque.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().entityTraceLineBuilder();
         * // a one argument colour carries its own alpha out into the builder's
         * builder.color(0x80FF0000);
         * Chat.log(`colour 0x${builder.getColor().toString(16)}, alpha ${builder.getAlpha()}`);
         * </pre>
         *
         * @param color the color of the line
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder color(int color) {
            this.color = color;
            this.alpha = ColorUtil.fixAlpha(color) >>> 24;
            return this;
        }

        /**
         * the colour and the alpha, given separately.
         * <p>
         * This leaves the colour exactly as given rather than reading an alpha out of
         * it, so a colour written with its own alpha byte and no {@code alpha} call
         * loses that byte and comes out at the alpha given here instead.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().entityTraceLineBuilder();
         * // half a red line: the colour keeps 0x00FF0000 and the alpha is 128
         * builder.color(0xFF0000, 128);
         * Chat.log(`colour 0x${builder.getColor().toString(16)}, alpha ${builder.getAlpha()}`);
         * </pre>
         *
         * @param color the color of the line
         * @param alpha the alpha value of the line's color
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder color(int color, int alpha) {
            this.color = color;
            this.alpha = alpha;
            return this;
        }

        /**
         * the three colour components, leaving the alpha as it was.
         * <p>
         * Nothing is clamped, so a component above 255 spills into the one above it
         * rather than being cut off. The alpha is not touched by this, so it stays
         * whatever the last colour or {@link #alpha(int)} call left it at.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().entityTraceLineBuilder();
         * // an opaque orange, since the alpha default has not been changed
         * builder.color(255, 128, 0);
         * Chat.log(`colour 0x${builder.getColor().toString(16)}, alpha ${builder.getAlpha()}`);
         * </pre>
         *
         * @param r the red component of the color
         * @param g the green component of the color
         * @param b the blue component of the color
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder color(int r, int g, int b) {
            this.color = (r << 16) | (g << 8) | b;
            return this;
        }

        /**
         * the three colour components and the alpha, all given on their own.
         * <p>
         * The same packing as the three argument form, with the alpha kept apart in
         * the builder rather than being packed into the colour. The default colour is
         * {@code 0xFFFFFF} and the default alpha is {@code 0xFF}, so a builder that has
         * had nothing set on it builds a fully opaque white line.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().entityTraceLineBuilder();
         * builder.color(255, 0, 255, 255);
         * Chat.log(`colour 0x${builder.getColor().toString(16)}, alpha ${builder.getAlpha()}`);
         * </pre>
         *
         * @param r the red component of the color
         * @param g the green component of the color
         * @param b the blue component of the color
         * @param a the alpha value of the color
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder color(int r, int g, int b, int a) {
            this.color = (r << 16) | (g << 8) | b;
            this.alpha = a;
            return this;
        }

        /**
         * the colour collected so far, as one packed value.
         * <p>
         * This is the builder's own field rather than anything read back off a built
         * line. A built line's own colour getter masks the alpha out, so a colour set
         * here with an alpha in it reads back differently here than it does there.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().entityTraceLineBuilder();
         * // the default is plain white with no alpha in the number itself
         * Chat.log(`default colour 0x${builder.getColor().toString(16)}`);
         * </pre>
         *
         * @return the color of the line
         * @since 1.9.0
         */
        public int getColor() {
            return color;
        }

        /**
         * the alpha the built line will be drawn at, from 0 to 255.
         * <p>
         * 255 is fully opaque and 0 is invisible. This only sets the value; it does
         * not touch the colour, so the two are set independently and whichever was
         * called last for each one wins.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().entityTraceLineBuilder();
         * builder.color(0xFF0000).alpha(64);
         * Chat.log(`red at alpha ${builder.getAlpha()}`);
         * </pre>
         *
         * @param alpha the alpha value for the line's color
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder alpha(int alpha) {
            this.alpha = alpha;
            return this;
        }

        /**
         * whether the built line draws on top of everything else, including blocks.
         * <p>
         * The default is {@code true}. Turning it off lets terrain hide the line the
         * way it hides anything else in the world, which is usually what a line
         * pointing at something behind a wall should do.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * // a line that terrain can hide, rather than one drawn over everything
         * draw.traceLineBuilder().pos(0, 64, 0).alwaysOnTop(false).buildAndAdd();
         * draw.register();
         * </pre>
         *
         * @param alwaysOnTop whether the line should render on top of everything else.
         * @return self for chaining
         * @since 2.0.0
         */
        public Builder alwaysOnTop(boolean alwaysOnTop) {
            this.alwaysOnTop = alwaysOnTop;
            return this;
        }

        /**
         * the alpha collected so far, as a number from 0 to 255.
         * <p>
         * This starts at {@code 255} and is overwritten by any of the {@code color}
         * setters, so a colour set with the one argument form brings its own alpha
         * along rather than leaving this at its default.
         * example:
         * <pre>
         * const builder = Hud.createDraw3D().entityTraceLineBuilder();
         * builder.alpha(64);
         * Chat.log(`half transparent: ${builder.getAlpha()}`);
         * </pre>
         *
         * @return the alpha value of the line's color
         * @since 1.9.0
         */
        public int getAlpha() {
            return alpha;
        }

        /**
         * builds the line and adds it to the overlay, naming the entity in the call.
         * <p>
         * The same as setting the entity and then calling {@link #buildAndAdd()}, and
         * with the same caveat: a {@code null} here leaves the built line with no
         * entity, and it is removed on its first frame.
         * example:
         * <pre>
         * const player = Player.getPlayer();
         * if (player !== null) {
         *   const draw = Hud.createDraw3D();
         *   draw.entityTraceLineBuilder().buildAndAdd(player);
         *   draw.register();
         * }
         * </pre>
         *
         * @param entity the target entity
         * @return the build line
         * @since 1.9.0
         */
        public EntityTraceLine buildAndAdd(@Nullable EntityHelper<?> entity) {
            return entity(entity).buildAndAdd();
        }

        /**
         * builds the line and adds it to the overlay this builder came from.
         * <p>
         * The line goes into the same list any other trace line does, so
         * {@code Draw3D.getTraceLines()} reports it alongside the plain ones and
         * {@code removeTraceLine} takes it off again.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * const player = Player.getPlayer();
         * const builder = draw.entityTraceLineBuilder().color(0xFFFF0000);
         * if (player !== null) {
         *   builder.entity(player);
         * }
         * const line = builder.buildAndAdd();
         * draw.register();
         * Chat.log(`${draw.getTraceLines().size()} trace lines, newest is ours`);
         * </pre>
         *
         * @return the build line
         * @since 1.9.0
         */
        public EntityTraceLine buildAndAdd() {
            EntityTraceLine line = build();
            parent.addTraceLine(line);
            return line;
        }

        /**
         * builds the line without adding it anywhere, naming the entity in the call.
         * <p>
         * Same as setting the entity and then calling {@link #build()}. A line built
         * this way draws nothing until it is added to an overlay.
         * example:
         * <pre>
         * const player = Player.getPlayer();
         * if (player !== null) {
         *   const draw = Hud.createDraw3D();
         *   const line = draw.entityTraceLineBuilder().build(player);
         *   // built but not added, so it is not in the overlay yet
         *   draw.addTraceLine(line);
         *   draw.register();
         * }
         * </pre>
         *
         * @param entity the target entity
         * @return the build line
         * @since 1.9.0
         */
        public EntityTraceLine build(@Nullable EntityHelper<?> entity) {
            return entity(entity).build();
        }

        /**
         * Builds the line from the given values
         * <p>
         * The line this makes is not put anywhere, so it draws nothing until it is
         * added to an overlay. It also starts with its target at {@code 0, 0, 0} and
         * the entity it was given, and only finds the entity's real position on its
         * first drawn frame, so reading the target straight after building gives the
         * origin rather than where the entity is.
         * example:
         * <pre>
         * const draw = Hud.createDraw3D();
         * const player = Player.getPlayer();
         * if (player !== null) {
         *   const line = draw.entityTraceLineBuilder().entity(player).build();
         *   // the target is the origin until the line has been drawn once
         *   Chat.log(`target before drawing: ${line.getPos()}`);
         *   draw.addTraceLine(line);
         *   draw.register();
         * }
         * </pre>
         *
         * @return the build line
         * @since 1.9.0
         */
        public EntityTraceLine build() {
            return new EntityTraceLine(entity, color, alpha, yOffset, alwaysOnTop);
        }

    }

}
