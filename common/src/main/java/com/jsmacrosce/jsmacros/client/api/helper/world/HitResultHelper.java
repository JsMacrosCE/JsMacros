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
 * What the crosshair is currently on. A script gets one from
 * {@link com.jsmacrosce.jsmacros.client.api.helper.InteractionManagerHelper#getTarget()}, which is
 * the live crosshair target, or builds one with {@link #resolve(HitResult)}.<br>
 * The class is abstract and the two things it can be are subclasses: {@link Block} for a block the
 * crosshair is on, and {@link Entity} for an entity it is on. This base class alone knows only
 * where the hit landed, which is what both of them share.<br>
 * Because the concrete kind is not in the static type, the two {@code as} methods are how a script
 * asks what it actually got rather than testing with {@code instanceof}: {@link #asBlock()} answers
 * {@code null} unless the target really is a block, and {@link #asEntity()} likewise.
 * example:
 * <pre>
 * const interactions = Player.interactions();
 * const target = interactions === null ? null : interactions.getTarget();
 * if (target === null) {
 *   // nothing under the crosshair at all
 * } else if (target.asEntity() !== null) {
 *   const entity = target.asEntity().getEntity();
 *   Chat.log(`the crosshair is on ${entity.getName()} at ${target.getPos()}`);
 * } else if (target.asBlock() !== null) {
 *   const block = target.asBlock();
 *   Chat.log(`the crosshair is on the ${block.getSide().getName()} face of ${block.getBlockPos()}`);
 * }
 *
 * // the point where the ray landed is on all three kinds
 * if (target !== null) { Chat.log(target.getPos()); }
 * </pre>
 *
 * @author aMelonRind
 * @since 1.9.1
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class HitResultHelper<T extends HitResult> extends BaseHelper<T> {

    /**
     * Wraps a raw hit result, picking the subclass that matches what was actually hit: a block for
     * both a block hit and a miss, and an entity for an entity hit. A raw hit result of any other
     * kind comes back as the plain base class, which knows the position and nothing else.<br>
     * {@code null} in gives {@code null} out, so a script with no target does not have to check
     * before calling.
     * example:
     * <pre>
     * const HitResultHelper =
     *   Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.HitResultHelper");
     * // the raw object the game keeps for the crosshair, which is what resolve takes
     * const target = HitResultHelper.resolve(Client.getMinecraft().hitResult);
     * if (target !== null) {
     *   Chat.log(target);
     * }
     * </pre>
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
     * The exact point the ray landed on, kept at full precision rather than rounded to a block. This
     * is on all three kinds of target, which makes it the one thing that can be read without first
     * working out what was hit.
     * example:
     * <pre>
     * // the landing point, and which block it falls inside
     * const interactions = Player.interactions();
     * const target = interactions === null ? null : interactions.getTarget();
     * if (target !== null) {
     *   const at = target.getPos();
     *   const block = World.getBlock(at);
     *   if (block !== null) {
     *     Chat.log(`landed in ${block.getId()} at ${block.getBlockPos()}`);
     *   }
     * }
     * </pre>
     *
     * @return where the hit landed, as a point in the world.
     * @since 1.9.1
     */
    public Pos3D getPos() {
        var pos = base.getLocation();
        return new Pos3D(pos.x, pos.y, pos.z);
    }

    /**
     * This target as a block target, or {@code null} if it is not one. A plain base-class target and
     * an entity target both answer {@code null} here, so this is the test for "was it a block".
     * example:
     * <pre>
     * const interactions = Player.interactions();
     * const target = interactions === null ? null : interactions.getTarget();
     * const block = target === null ? null : target.asBlock();
     * if (block !== null) {
     *   Chat.log(`block ${block.getBlockPos()}, side ${block.getSide().getName()}`);
     * } else if (target !== null) {
     *   Chat.log("not a block");
     * }
     * </pre>
     *
     * @return this target as a {@link Block}, or {@code null} if it is not a block target.
     * @since 1.9.1
     */
    @Nullable
    public Block asBlock() {
        return null;
    }

    /**
     * This target as an entity target, or {@code null} if it is not one.
     * example:
     * <pre>
     * const interactions = Player.interactions();
     * const target = interactions === null ? null : interactions.getTarget();
     * const hit = target === null ? null : target.asEntity();
     * if (hit !== null) {
     *   const entity = hit.getEntity();
     *   Chat.log(`${entity.getType()} is in the crosshair`);
     *   if (entity.isAlive()) {
     *     Chat.log("and still alive");
     *   }
     * }
     * </pre>
     *
     * @return this target as an {@link Entity}, or {@code null} if it is not an entity target.
     * @since 1.9.1
     */
    @Nullable
    public Entity asEntity() {
        return null;
    }

    @Override
    public String toString() {
        return String.format("HitResultHelper:{\"pos\": %s}", getPos());
    }

    /**
     * A target that is a block, which is also what a miss comes back as: a miss has a position and a
     * side but no block that was really reached, and {@link HitResultHelper.Block#isMissed()} is what tells the two
     * apart.
     * example:
     * <pre>
     * // the block under the crosshair, and whether the ray actually reached it
     * const interactions = Player.interactions();
     * const target = interactions === null ? null : interactions.getTarget();
     * const block = target === null ? null : target.asBlock();
     * if (block !== null) {
     *   if (block.isMissed()) {
     *     Chat.log("the ray went past everything");
     *   } else {
     *     const hit = World.getBlock(block.getBlockPos());
     *     if (hit !== null) {
     *       Chat.log(`${hit.getId()} on its ${block.getSide().getName()} face`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @author aMelonRind
     * @since 1.9.1
     */
    public static class Block extends HitResultHelper<BlockHitResult> {

        public Block(BlockHitResult base) {
            super(base);
        }

        /**
         * The face of the block the ray came in on, which is the side of the block opposite the
         * direction the ray was travelling in. For a miss this is still a direction, so it is not a
         * sign that anything was actually reached.
         * example:
         * <pre>
         * // place a block on the face that was clicked, which is what a build macro wants
         * const interactions = Player.interactions();
     * const block = interactions === null ? null : interactions.getTarget().asBlock();
         * if (block !== null) {
         *   const side = block.getSide();
         *   Chat.log(`clicked the ${side.getName()} face`);
         *   if (side.isHorizontal()) {
         *     const neighbour = block.getBlockPos().offset(side.getOpposite().getName());
         *     Chat.log(`a block there would be ${neighbour}`);
         *   }
         * }
         * </pre>
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
         * The block the ray landed on, which for a miss is the block the ray started inside rather
         * than one that was reached.
         * example:
         * <pre>
         * // the block under the crosshair and the one above it
         * const interactions = Player.interactions();
     * const block = interactions === null ? null : interactions.getTarget().asBlock();
         * if (block !== null) {
         *   const here = block.getBlockPos();
         *   Chat.log(`clicked ${here}, and just above it is ${here.up()}`);
         *   const above = World.getBlock(here.up());
         *   if (above !== null) {
         *     Chat.log(`which is ${above.getId()}`);
         *   }
         * }
         * </pre>
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
         * Whether the ray reached nothing at all. The game hands back a block-shaped hit for a miss
         * so that the crosshair has something to draw against, and this is what separates that from
         * a real hit.
         * example:
         * <pre>
         * // only build against something that was really reached
         * const interactions = Player.interactions();
     * const block = interactions === null ? null : interactions.getTarget().asBlock();
         * if (block !== null) {
         *   if (block.isMissed()) {
         *     Chat.log("pointing at nothing, not building");
         *   } else {
         *     Chat.log(`about to build at ${block.getBlockPos()}`);
         *   }
         * }
         * </pre>
         *
         * @return {@code true} if this hit is a miss rather than a block that was reached.
         * @since 1.9.1
         */
        public boolean isMissed() {
            return base.getType() == HitResult.Type.MISS;
        }

        /**
         * Whether the ray started inside the block it is reporting. That is what happens when the
         * crosshair is inside the player, so this is the test for a hit the player is already
         * standing in rather than one that was reached from outside.
         * example:
         * <pre>
         * // a hit the player is inside is not a block that was reached
         * const interactions = Player.interactions();
     * const block = interactions === null ? null : interactions.getTarget().asBlock();
         * if (block !== null) {
         *   if (block.isInsideBlock()) {
         *     Chat.log("the crosshair is inside a block");
         *   } else {
         *     Chat.log(`looking at ${block.getBlockPos()}`);
         *   }
         * }
         * </pre>
         *
         * @return {@code true} if the ray started inside the block it landed on.
         * @since 1.9.1
         */
        public boolean isInsideBlock() {
            return base.isInside();
        }

        @Override
        public Block asBlock() {
            return this;
        }

        @Override
        public String toString() {
            return String.format("HitResultHelper$Block:{\"pos\": %s, \"side\": %s, \"blockPos\": %s, \"missed\": %s, \"insideBlock\": %s}", getPos(), base.getDirection(), getBlockPos(), isMissed(), isInsideBlock());
        }

    }

    /**
     * A target that is an entity, which carries the entity itself and the point on it that was hit.
     * {@link #getPos()} here is the point on the entity rather than a point in a block, so it moves
     * with the entity between the hit and the read.
     * example:
     * <pre>
     * const interactions = Player.interactions();
     * const hit = interactions === null ? null : interactions.getTarget().asEntity();
     * if (hit !== null) {
     *   const entity = hit.getEntity();
     *   Chat.log(`${entity.getType()} hit at ${hit.getPos()}`);
     *   Chat.log(`it is standing at ${entity.getPos()}, so that is on its near side`);
     * }
     * </pre>
     *
     * @author aMelonRind
     * @since 1.9.1
     */
    public static class Entity extends HitResultHelper<EntityHitResult> {

        public Entity(EntityHitResult base) {
            super(base);
        }

        /**
         * The entity that was hit, wrapped in the entity helper for whatever it is, so a mob gives
         * back the mob-specific helper and anything else gives back the general one.
         * example:
         * <pre>
         * // read the entity under the crosshair and act on what it is
         * const interactions = Player.interactions();
     * const hit = interactions === null ? null : interactions.getTarget().asEntity();
         * if (hit !== null) {
         *   const entity = hit.getEntity();
         *   Chat.log(`${entity.getType()} named ${entity.getName()}`);
         *   Chat.log(`at ${entity.getBlockPos()}, uuid ${entity.getUUID()}`);
         *   if (entity.isAlive()) {
         *     Chat.log("still alive");
         *   }
         * }
         * </pre>
         *
         * @return the entity that was hit.
         * @since 1.9.1
         */
        public EntityHelper<?> getEntity() {
            return EntityHelper.create(base.getEntity());
        }

        @Override
        public Entity asEntity() {
            return this;
        }

        @Override
        public String toString() {
            return String.format("HitResultHelper:{\"pos\": %s, \"entity\": %s}", getPos(), getEntity().getType());
        }

    }

}
