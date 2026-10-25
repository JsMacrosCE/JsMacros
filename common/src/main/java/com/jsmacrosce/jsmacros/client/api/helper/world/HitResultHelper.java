package com.jsmacrosce.jsmacros.client.api.helper.world;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * Wraps a crosshair hit result, with shared access to its hit position and subtype-specific access
 * for block and entity hits. Use {@link com.jsmacrosce.jsmacros.client.api.helper.InteractionManagerHelper#getTarget()}
 * for the current target or {@link #resolve(HitResult)} to wrap a raw result. Misses use {@link Block}.
 * <pre>
 * const interactions = Player.interactions();
 * const target = interactions === null ? null : interactions.getTarget();
 * if (target === null) {
 *   // nothing under the crosshair at all
 * } else if (target instanceof HitResults.Entity) {
 *   const entity = target.getEntity();
 *   Chat.log(`the crosshair is on ${entity.getName()} at ${target.getPos()}`);
 * } else if (target instanceof HitResults.Block) {
 *   const blockPos = target.getBlockPos();
 *   if (blockPos !== null) {
 *     Chat.log(`the crosshair is on the ${target.getSide().getName()} face of ${blockPos}`);
 *   }
 * }
 *
 * // the point where the ray landed is on all three kinds
 * if (target !== null) { Chat.log(target.getPos()); }
 * </pre>
 * @param <T> the Minecraft hit-result type wrapped by this helper
 *
 * @author aMelonRind
 * @since 1.9.1
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class HitResultHelper<T extends HitResult> extends BaseHelper<T> {

    /**
     * Wraps a raw hit result as a {@link Block} for block hits and misses, or an {@link Entity} for
     * entity hits. Returns {@code null} for a null input.
     *
     * @param hr the raw hit result to wrap, or {@code null}.
     * @return the matching subclass of this helper, or {@code null} if {@code hr} was {@code null}.
     * @since 1.9.1
     */
    @Nullable
    public static HitResultHelper<?> resolve(@Nullable HitResult hr) {
        if (hr == null) return null;
        return switch (hr.getType()) {
            case MISS, BLOCK -> new Block((BlockHitResult) hr);
            case ENTITY -> new Entity((EntityHitResult) hr);
            //noinspection UnnecessaryDefault
            default -> new HitResultHelper<>(hr);
        };
    }

    protected HitResultHelper(T base) {
        super(base);
    }

    /**
     * Returns the hit location as a world-space point, without rounding its coordinates.
     *
     * @return where the hit landed, as a point in the world.
     * @since 1.9.1
     */
    public Pos3D getPos() {
        var pos = base.getLocation();
        return new Pos3D(pos.x, pos.y, pos.z);
    }

    /**
     * Returns this target as a block hit or miss, or {@code null} for other hit-result types.
     *
     * @return this target as a {@link Block}, or {@code null} if it is not a block target.
     * @since 1.9.1
     */
    @Nullable
    public Block asBlock() {
        return null;
    }

    /**
     * Returns this target as an entity hit, or {@code null} for other hit-result types.
     *
     * @return this target as an {@link Entity}, or {@code null} if it is not an entity target.
     * @since 1.9.1
     */
    @Nullable
    public Entity asEntity() {
        return null;
    }

    /** Returns a diagnostic string containing the hit position.
     *
     * @return a diagnostic containing the position rendered from {@code getPos()}.
     */
    @Override
    public String toString() {
        return String.format("HitResultHelper:{\"pos\": %s}", getPos());
    }

    /**
     * Represents a block hit or miss; use {@link #isMissed()} to distinguish them.
     * <pre>
     * const interactions = Player.interactions();
     * const target = interactions === null ? null : interactions.getTarget();
     * if (target instanceof HitResults.Block && !target.isMissed()) {
     *   const pos = target.getBlockPos();
     *   if (pos !== null) Chat.log(`hit ${pos}`);
     * }
     * </pre>
     *
     * @author aMelonRind
     * @since 1.9.1
     */
    public static class Block extends HitResultHelper<BlockHitResult> {

        /** Wraps the supplied block hit result, including a result representing a miss.
         *
         * @param base block hit result to wrap
         */
        public Block(BlockHitResult base) {
            super(base);
        }

        /**
         * Returns the hit direction, or {@code null} if the result has no direction. Miss results
         * also carry a direction; this does not imply a block was hit.
         *
         * @return the face the ray came in on, or {@code null} if the hit carries no direction.
         * @since 1.9.1
         */
        @Nullable
        public DirectionHelper getSide() {
            Direction dir = base.getDirection();
            return dir == null ? null : new DirectionHelper(dir);
        }

        /**
         * Returns the position carried by the block hit result. For a miss, this is the position
         * supplied with that miss, not necessarily a block reached by the ray.
         *
         * @return the block that was hit, or {@code null} if the hit carries no position.
         * @since 1.9.1
         */
        @Nullable
        public BlockPosHelper getBlockPos() {
            BlockPos pos = base.getBlockPos();
            return pos == null ? null : new BlockPosHelper(pos);
        }

        /**
         * Returns whether this block hit result is marked as a miss.
         *
         * @return {@code true} if this hit is a miss rather than a block that was reached.
         * @since 1.9.1
         */
        public boolean isMissed() {
            return base.getType() == HitResult.Type.MISS;
        }

        /**
         * Whether the underlying block hit is marked as inside; this is independent of whether the
         * hit is a miss.
         *
         * @return {@code true} if the hit is marked as inside.
         * @since 1.9.1
         */
        public boolean isInsideBlock() {
            return base.isInside();
        }

        /** Returns this block-hit-or-miss helper.
         *
         * @return this block-hit-or-miss helper
         */
        @Override
        public Block asBlock() {
            return this;
        }

        /** Returns a diagnostic containing the hit position, direction, block position, miss flag, and inside flag.
         *
         * @return a diagnostic with those values rendered from the underlying result and helper accessors
         */
        @Override
        public String toString() {
            return String.format("HitResultHelper$Block:{\"pos\": %s, \"side\": %s, \"blockPos\": %s, \"missed\": %s, \"insideBlock\": %s}", getPos(), base.getDirection(), getBlockPos(), isMissed(), isInsideBlock());
        }

    }

    /**
     * A target that carries the entity and the hit location recorded by the hit result.
     * <pre>
     * const interactions = Player.interactions();
     * const target = interactions === null ? null : interactions.getTarget();
     * if (target instanceof HitResults.Entity) {
     *   Chat.log(`the crosshair is on ${target.getEntity().getName()}`);
     * }
     * </pre>
     *
     * @author aMelonRind
     * @since 1.9.1
     */
    public static class Entity extends HitResultHelper<EntityHitResult> {

        /** Wraps the supplied entity hit result.
         *
         * @param base entity hit result to wrap
         */
        public Entity(EntityHitResult base) {
            super(base);
        }

        /**
         * The hit entity wrapped in its matching {@link EntityHelper} subtype.
         *
         * @return the hit entity wrapped in its matching helper subtype.
         * @since 1.9.1
         */
        public EntityHelper<?> getEntity() {
            return EntityHelper.create(base.getEntity());
        }

        /** Returns this entity-hit helper.
         *
         * @return this entity-hit helper
         */
        @Override
        public Entity asEntity() {
            return this;
        }

        /** Returns a diagnostic containing the hit position and entity type identifier.
         *
          * @return a diagnostic with the position and the value from {@code EntityHelper.getType()}
         */
        @Override
        public String toString() {
            return String.format("HitResultHelper:{\"pos\": %s, \"entity\": %s}", getPos(), getEntity().getType());
        }

    }

}
