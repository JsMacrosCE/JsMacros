package com.jsmacrosce.jsmacros.client.api.helper.world;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * One of the six block-facing directions: down, up, north, south, west or east. A script gets one
 * of these from whatever already knows which way something is pointing, such as
 * {@link HitResultHelper.Block#getSide()} for the face a block was clicked on, or a facing property
 * of a block state read through {@link BlockStateHelper#getUniversal()}.<br>
 * The useful thing about a direction is that it can be turned. {@link #getOpposite()},
 * {@link #getLeft()} and {@link #getRight()} each hand back a new direction and leave this one
 * alone, so walking a ring of sides is a loop that reassigns rather than one that mutates.
 * {@link #getVector()} turns a direction into the one block step it stands for, which is what a
 * neighbour walk wants.<br>
 * The two quarter turns only work on a horizontal direction. Up and down have no side to turn
 * towards, so {@link #getLeft()} and {@link #getRight()} raise rather than returning anything, and
 * {@link #isHorizontal()} is the cheap way to stay out of that.
 * example:
 * <pre>
 * // whichever way the player is looking
 * const player = Player.getPlayer();
 * const facing = player.getFacingDirection();
 * Chat.log(`${facing.getName()} is on the ${facing.getAxis()} axis at yaw ${facing.getYaw()}`);
 *
 * // turning a direction gives a new one, never a change to the old
 * const behind = facing.getOpposite();
 * Chat.log(`${facing.getName()} is ahead, ${behind.getName()} is behind`);
 *
 * // and a direction is also the one block step it stands for
 * const step = facing.getVector();
 * Chat.log(`one block that way is ${step.getX()}, ${step.getY()}, ${step.getZ()}`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class DirectionHelper extends BaseHelper<Direction> {

    public DirectionHelper(Direction base) {
        super(base);
    }

    /**
     * The direction's own name, which is the same lowercase word the game uses for it everywhere
     * else: {@code down}, {@code up}, {@code north}, {@code south}, {@code west} or {@code east}.
     * That is also the spelling {@link BlockPosHelper#offset(String)} takes.
     * example:
     * <pre>
     * // the face of the block the player is looking at
     * const hit = Player.detailedRayTraceBlock(5, false);
     * Chat.log(`you are looking at the ${hit.getSide().getName()} face`);
     *
     * // and the same name is what a neighbour walk takes
     * const step = Player.getPlayer().getBlockPos().offset(hit.getSide().getName());
     * Chat.log(`which is the block at ${step}`);
     * </pre>
     *
     * @return the name of this direction.
     * @since 1.8.4
     */
    public String getName() {
        return base.getName();
    }

    /**
     * The axis this direction is aligned to, as the single letter the game uses for it. A horizontal
     * direction is on {@code x} or {@code z} and a vertical one is on {@code y}.
     * example:
     * <pre>
     * const hit = Player.detailedRayTraceBlock(5, false);
     * const side = hit.getSide();
     * if (side.getAxis() === "y") {
     *   Chat.log("that is a floor or a ceiling");
     * } else {
     *   Chat.log(`that is a wall, and it faces ${side.getName()}`);
     * }
     * </pre>
     *
     * @return the name of the axis this direction is aligned to.
     * @since 1.8.4
     */
    public String getAxis() {
        return base.getAxis().getName();
    }

    /**
     * Whether this direction is a vertical one, which is the same question as asking whether
     * {@link #getAxis()} answers {@code y}. Only up and down are.
     * example:
     * <pre>
     * // a block state whose facing is a real direction, such as a stair
     * const state = Client.getRegistryManager().getBlockState("minecraft:oak_stairs");
     * const facing = state.getUniversal().getHorizontalFacing();
     * if (facing.isVertical()) {
     *   Chat.log(`${facing.getName()} points straight up or down`);
     * } else {
     *   Chat.log(`${facing.getName()} is one of the four sides`);
     * }
     * </pre>
     *
     * @return {@code true} if this direction is vertical, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isVertical() {
        return base.getAxis().isVertical();
    }

    /**
     * Whether this direction is a horizontal one. The four that are make up the ring that
     * {@link #getLeft()} and {@link #getRight()} walk, and the two that are not are the only ones
     * for which {@link #getPitch()} is not flat.
     * example:
     * <pre>
     * // walk the four horizontal sides, which is what a quarter turn does
     * let dir = Client.getRegistryManager().getBlockState("minecraft:oak_stairs")
     *   .getUniversal().getHorizontalFacing();
     * for (let i = 0; i !== 4; i += 1) {
     *   Chat.log(`${i}: ${dir.getName()}, axis ${dir.getAxis()}, flat ${dir.getPitch() === 0}`);
     *   dir = dir.getRight();
     * }
     * </pre>
     *
     * @return {@code true} if this direction is horizontal, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isHorizontal() {
        return base.getAxis().isHorizontal();
    }

    /**
     * Whether this direction runs towards increasing coordinates on its own axis, which is the same
     * question as whether {@link #getVector()} has a positive number in it. Two of the six answer
     * this and four do not.
     * example:
     * <pre>
     * // which way is positive on each axis, from the four horizontal sides
     * let dir = Client.getRegistryManager().getBlockState("minecraft:oak_stairs")
     *   .getUniversal().getHorizontalFacing();
     * for (let i = 0; i !== 4; i += 1) {
     *   const step = dir.getVector();
     *   Chat.log(`${dir.getName()} on ${dir.getAxis()} is positive: ${dir.isTowardsPositive()}, step ${step}`);
     *   dir = dir.getRight();
     * }
     * </pre>
     *
     * @return {@code true} if this direction is pointing in a positive direction, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isTowardsPositive() {
        return base.getAxisDirection().getStep() == 1;
    }

    /**
     * The yaw this direction stands for, in degrees, which for a horizontal one is the heading it
     * faces. Up and down have no heading of their own in the game and both report the same value,
     * so this is only worth comparing between horizontal directions.
     * example:
     * <pre>
     * // the yaw of each of the four sides, so the four headings can be seen at once
     * let dir = Client.getRegistryManager().getBlockState("minecraft:oak_stairs")
     *   .getUniversal().getHorizontalFacing();
     * for (let i = 0; i !== 4; i += 1) {
     *   Chat.log(`${dir.getName()} is yaw ${dir.getYaw()}`);
     *   dir = dir.getRight();
     * }
     *
     * // how far the player is turned from the way a block faces
     * const yaw = Player.getPlayer().getYaw();
     * const east = Client.getRegistryManager().getBlockState("minecraft:oak_stairs", "[facing=east]")
     *   .getUniversal().getHorizontalFacing();
     * Chat.log(`${east.getName()} is yaw ${east.getYaw()}, and the player is ${yaw}`);
     * Chat.log(`it points the way the player looks: ${east.pointsTo(yaw)}`);
     * </pre>
     *
     * @return the yaw of this direction.
     * @since 1.8.4
     */
    public float getYaw() {
        return base.toYRot();
    }

    /**
     * The pitch this direction stands for, in degrees. It is flat, meaning zero, for a horizontal
     * direction, and a quarter turn either way for a vertical one.
     * example:
     * <pre>
     * // a furnace faces one of the six, so its pitch shows the difference
     * const furnace = Client.getRegistryManager().getBlockState("minecraft:furnace")
     *   .getUniversal().getFacing();
     * Chat.log(`${furnace.getName()} has pitch ${furnace.getPitch()}`);
     * if (furnace.getPitch() === 0) {
     *   Chat.log("so it is turned to a side");
     * }
     * </pre>
     *
     * @return the pitch of this direction.
     * @since 1.8.4
     */
    public float getPitch() {
        if (isHorizontal()) {
            return 0;
        } else {
            return base.getStepY() * 90;
        }
    }

    /**
     * The direction pointing the other way. This works for all six, and is the only turn that does:
     * for a vertical one it gives the other vertical direction.
     * example:
     * <pre>
     * // the face of a block opposite the one that was clicked
     * const hit = Player.detailedRayTraceBlock(5, false);
     * const side = hit.getSide();
     * Chat.log(`clicked ${side.getName()}, far side is ${side.getOpposite().getName()}`);
     *
     * // and for the six directions, opposite is its own inverse
     * Chat.log(`${side.getOpposite().getOpposite().getName()}`);
     * </pre>
     *
     * @return the opposite direction.
     * @since 1.8.4
     */
    public DirectionHelper getOpposite() {
        return new DirectionHelper(base.getOpposite());
    }

    /**
     * The direction a quarter turn anticlockwise from this one. Only a horizontal direction can be
     * turned this way; up and down have no side to turn towards and raise instead.
     * example:
     * <pre>
     * // walk the four sides anticlockwise, starting from a horizontal one
     * let dir = Client.getRegistryManager().getBlockState("minecraft:oak_stairs")
     *   .getUniversal().getHorizontalFacing();
     * for (let i = 0; i !== 4; i += 1) {
     *   Chat.log(dir.getName());
     *   dir = dir.getLeft();
     * }
     *
     * // a vertical direction cannot be turned this way
     * const up = Client.getRegistryManager().getBlockState("minecraft:piston", "[facing=up]")
     *   .getUniversal().getFacing();
     * if (up.isVertical()) {
     *   try {
     *     up.getLeft();
     *   } catch (e) {
     *     Chat.log(`${up.getName()} has no anticlockwise side`);
     *   }
     * }
     * </pre>
     *
     * @return the direction to the left.
     * @throws IllegalStateException if this direction is vertical, since there is no side to turn
     *                               towards.
     * @since 1.8.4
     */
    public DirectionHelper getLeft() {
        return new DirectionHelper(base.getCounterClockWise());
    }

    /**
     * The direction a quarter turn clockwise from this one, the mirror of {@link #getLeft()}. Like
     * that one, it only works on a horizontal direction.
     * example:
     * <pre>
     * // the same four sides, the other way round
     * let dir = Client.getRegistryManager().getBlockState("minecraft:oak_stairs")
     *   .getUniversal().getHorizontalFacing();
     * for (let i = 0; i !== 4; i += 1) {
     *   Chat.log(dir.getName());
     *   dir = dir.getRight();
     * }
     * </pre>
     *
     * @return the direction to the right.
     * @throws IllegalStateException if this direction is vertical, since there is no side to turn
     *                               towards.
     * @since 1.8.4
     */
    public DirectionHelper getRight() {
        return new DirectionHelper(base.getClockWise());
    }

    /**
     * This direction as the one block step it stands for: one of the three coordinates is
     * {@code 1} or {@code -1} and the other two are zero.
     * example:
     * <pre>
     * // the block one step that way from the player
     * const player = Player.getPlayer();
     * const step = player.getFacingDirection().getVector();
     * const neighbour = player.getBlockPos().offset(step.getX(), step.getY(), step.getZ());
     * Chat.log(`that is the block at ${neighbour}`);
     *
     * // the six names are what a block state property of the same name holds, so a
     * // facing property can be compared against a direction without converting anything
     * const stairs = Client.getRegistryManager().getBlockState("minecraft:oak_stairs", "[facing=east]")
     *   .getUniversal();
     * Chat.log(`the stair faces ${stairs.getHorizontalFacing().getName()}`);
     * </pre>
     *
     * @return the direction as a directional vector.
     * @since 1.8.4
     */
    public Pos3D getVector() {
        Vec3i vec = base.getUnitVec3i();
        return new Pos3D(vec.getX(), vec.getY(), vec.getZ());
    }

    /**
     * Whether a yaw points this way more than it points any other one. That is a question about
     * heading, so it is only meaningful for a horizontal direction: up and down have no heading and
     * this never answers true for either of them.
     * example:
     * <pre>
     * // of the four sides, which one the player is turned towards
     * const player = Player.getPlayer();
     * const yaw = player.getYaw();
     * let dir = Client.getRegistryManager().getBlockState("minecraft:oak_stairs")
     *   .getUniversal().getHorizontalFacing();
     * let best = null;
     * for (let i = 0; i !== 4; i += 1) {
     *   if (dir.pointsTo(yaw)) {
     *     best = dir;
     *   }
     *   dir = dir.getRight();
     * }
     * Chat.log(`facing ${best === null ? "nothing in particular" : best.getName()}`);
     * </pre>
     *
     * @param yaw the yaw to check
     * @return {@code true} if the yaw is facing this direction more than any other one,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean pointsTo(double yaw) {
        return base.isFacingAngle((float) yaw);
    }

    @Override
    public String toString() {
        return String.format("DirectionHelper:{\"name\": \"%s\", \"yaw\": %f, \"pitch\": %f}", getName(), getYaw(), getPitch());
    }

}
