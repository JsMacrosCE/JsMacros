package com.jsmacrosce.jsmacros.client.api.classes.render.components3d;
import com.jsmacrosce.jsmacros.client.api.classes.render.RenderSink;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.util.ColorUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.client.api.classes.render.Draw3D;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;

import static com.jsmacrosce.jsmacros.client.api.classes.render.components.RenderElement.mc;

/**
 * A {@link TraceLine} whose target follows an entity, with {@code yOffset} added to
 * its interpolated Y coordinate each frame. If the entity is absent, removed, or in
 * a different level from the client, the line stops rendering and is removed from
 * its overlay. {@link #getPos()} remains at the origin until the first render.
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
     * Whether an entity trace line has requested removal; overlays use this class-wide
     * flag to decide whether to scan for lines marked for removal.
     *
     * @since 1.9.0
     */
    public static boolean dirty = false;

    /**
     * The game entity followed by this line, or {@code null} if none was set.
     *
     * @since 1.9.0
     */
    @Nullable
    public Entity entity;
    /**
     * Vertical offset added to the entity's interpolated Y position, in blocks (default
     * {@code 0.5}).
     *
     * @since 1.9.0
     */
    public double yOffset = 0.5;
    /**
     * Whether this line is marked for removal after it cannot render its entity.
     *
     * @since 1.9.0
     */
    public boolean shouldRemove = false;

    /**
     * Initializes an entity-following trace at the origin. A zero-alpha packed color
     * is made opaque only when its RGB bits are nonzero; depth testing is disabled.
     * A non-null helper's raw entity reference is stored without copying, and
     * {@code yOffset} is stored unchanged; a null helper leaves the line without an entity.
     *
     * @param entity helper for the target, or {@code null} to leave the line without an entity
     * @param color packed color; nonzero RGB with a zero alpha byte becomes opaque, while zero-alpha black remains transparent
     * @param yOffset vertical offset added to the target's Y coordinate, in blocks
     */
    public EntityTraceLine(@Nullable EntityHelper<?> entity, int color, double yOffset) {
        super(0, 0, 0, color);
        setEntity(entity);
        setYOffset(yOffset);
    }

    /**
     * Initializes an entity-following trace at the origin with separately supplied
     * alpha and depth testing disabled. Alpha is shifted into the top byte without
     * clamping or range validation. A non-null helper's raw entity reference is stored
     * without copying, and {@code yOffset} is stored unchanged; a null helper leaves
     * the line without an entity.
     *
     * @param entity helper for the target, or {@code null} to leave the line without an entity
     * @param color RGB source; only its low 24 bits are used
     * @param alpha value shifted into the color's top byte
     * @param yOffset vertical offset added to the target's Y coordinate, in blocks
     */
    public EntityTraceLine(@Nullable EntityHelper<?> entity, int color, int alpha, double yOffset) {
        super(0, 0, 0, color, alpha);
        setEntity(entity);
        setYOffset(yOffset);
    }

    /**
     * Initializes an entity-following trace at the origin with separately supplied
     * alpha and the requested depth-testing mode. Alpha is shifted into the top byte
     * without clamping or range validation; {@code alwaysOnTop} controls the inverse
     * internal culling flag and does not ensure visibility. A non-null helper's raw
     * entity reference is stored without copying, and {@code yOffset} is stored
     * unchanged; a null helper leaves the line without an entity.
     *
     * @param entity helper for the target, or {@code null} to leave the line without an entity
     * @param color RGB source; only its low 24 bits are used
     * @param alpha value shifted into the color's top byte
     * @param yOffset vertical offset added to the target's Y coordinate, in blocks
     * @param alwaysOnTop whether to disable depth testing
     */
    public EntityTraceLine(@Nullable EntityHelper<?> entity, int color, int alpha, double yOffset, boolean alwaysOnTop) {
        super(0, 0, 0, color, alpha, alwaysOnTop);
        setEntity(entity);
        setYOffset(yOffset);
    }

    /**
     * Sets the followed entity and clears this line's removal mark. A {@code null}
     * argument leaves both unchanged.
     *
     * @param entity the entity to follow, or {@code null} to leave the current entity unchanged
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
     * Sets the vertical offset added to the entity's interpolated Y coordinate each
     * frame, in blocks.
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
     * Updates the target to the entity's interpolated position plus {@code yOffset},
     * then renders the line. If the entity is absent, removed, or not in the client
     * level, marks the line for removal and returns without rendering. Called by the
     * renderer.
     *
     * @param matrixStack the world transform, already translated so the camera is at the origin
     * @param sink the buffer source the geometry goes into
     * @param tickDelta how far into the current tick this frame is, from 0 to 1
     * @author aMelonRind
     * @since 1.9.0
     */
    @Override
    public void render(PoseStack matrixStack, RenderSink sink, float tickDelta) {
        if (shouldRemove || entity == null || entity.isRemoved() || entity.level() != mc.level) {
            shouldRemove = true;
            dirty = true;
            return;
        }

        Vec3 vec = entity.getPosition(tickDelta);
        setPos(vec.x, vec.y + yOffset, vec.z);
        super.render(matrixStack, sink, tickDelta);
    }

    /**
     * Configures and builds an {@link EntityTraceLine}. Defaults are white, fully
     * opaque, always on top, offset by {@code 0.5} blocks, and no entity; a line with
     * no entity is marked for removal when rendered.
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

        /**
         * Creates a builder with no entity, a {@code 0.5}-block offset, opaque white,
         * and {@code alwaysOnTop} enabled. The parent is stored without validation;
         * it is used only by {@code buildAndAdd()}, so it may be null when building
         * detached lines. Calling {@code buildAndAdd()} with a null parent throws
         * {@code NullPointerException}.
         *
         * @param parent overlay used by {@code buildAndAdd()}, or null for detached builds only
         */
        public Builder(Draw3D parent) {
            this.parent = parent;
        }

        /**
         * Sets the entity for the line; {@code null} leaves the builder without an
         * entity, in which case the line is marked for removal when rendered.
         *
         * @param entity the target entity, or {@code null} for no entity
         * @return self for chaining
         * @since 1.9.0
         */
        public Builder entity(@Nullable EntityHelper<?> entity) {
            this.entity = entity;
            return this;
        }

        /**
         * Returns the helper entity supplied to this builder, or {@code null} if none was set.
         *
         * @return the target entity
         * @since 1.9.0
         */
        @Nullable
        public EntityHelper<?> getEntity() {
            return entity;
        }

        /**
         * Sets how far above the entity's feet the line points, in blocks. The default is {@code 0.5}.
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
         * Returns the y offset currently stored by this builder (default {@code 0.5}).
         *
         * @return the offset of y-axis
         * @since 1.9.0
         */
        public double getYOffset() {
            return yOffset;
        }

        /**
         * Stores the packed colour unchanged and derives alpha from its high byte. If that byte is {@code 0},
         * nonzero RGB is treated as opaque; an all-zero colour remains transparent.
         * <p>
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
         * Sets the packed colour and alpha separately; any alpha byte in {@code color} is not extracted.
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
         * Packs the red, green, and blue components into the colour without changing alpha. Components are not clamped.
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
         * Packs the RGB components into the colour and stores alpha separately; components are not clamped.
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
         * Returns the packed colour currently stored by this builder.
         *
         * @return the color of the line
         * @since 1.9.0
         */
        public int getColor() {
            return color;
        }

        /**
         * Sets the alpha independently of the colour, from {@code 0} (transparent) to
         * {@code 255} (opaque). Values are not clamped.
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
         * Sets whether the built line renders on top of scene geometry (default {@code true});
         * {@code false} enables depth testing so geometry can occlude it.
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
         * Returns the alpha currently stored by this builder (default {@code 255}).
         *
         * @return the alpha value of the line's color
         * @since 1.9.0
         */
        public int getAlpha() {
            return alpha;
        }

        /**
         * Builds the line for {@code entity} and adds it to this builder's overlay.
         * A {@code null} entity results in a line that is marked for removal when rendered.
         *
         * @param entity the target entity
         * @return the build line
         * @since 1.9.0
         */
        public EntityTraceLine buildAndAdd(@Nullable EntityHelper<?> entity) {
            return entity(entity).buildAndAdd();
        }

        /**
         * Builds the line and adds it to this builder's overlay.
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
         * Builds a line for {@code entity} without adding it to an overlay. A {@code null}
         * entity results in a line that is marked for removal when rendered.
         *
         * @param entity the target entity
         * @return the build line
         * @since 1.9.0
         */
        public EntityTraceLine build(@Nullable EntityHelper<?> entity) {
            return entity(entity).build();
        }

        /**
         * Builds a line without adding it to an overlay. Its target starts at the origin
         * and is updated to the entity's interpolated position on its first render.
         *
         * @return the build line
         * @since 1.9.0
         */
        public EntityTraceLine build() {
            return new EntityTraceLine(entity, color, alpha, yOffset, alwaysOnTop);
        }

    }

}
