package com.jsmacrosce.jsmacros.client.api.library.impl;

import net.minecraft.world.phys.Vec3;

import com.jsmacrosce.jsmacros.api.math.Pos2D;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.api.math.Vec2D;
import com.jsmacrosce.jsmacros.api.math.Vec3D;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;
import com.jsmacrosce.jsmacros.core.library.Library;

/**
 * Factories for the four geometry types the rest of the API is spoken in, and nothing else. There
 * is no state here and no way to observe a world, so every one of these is a plain constructor
 * call under a friendlier name.<br>
 * The pairs to keep straight are {@link Pos3D} against {@link Pos2D}, and {@link Vec3D} against
 * {@link Vec2D}. A {@code Pos} is a single point and carries only coordinates; a {@code Vec} is a
 * line segment and carries the coordinates of <em>both</em> its ends, so a vector can report a
 * length, a direction, and the angle of that direction. {@link #createVec(double, double, double,
 * double, double, double)} gives you the three dimensional one, {@link #createVec(double, double,
 * double, double)} the flat one. {@code Pos3D} extends {@code Pos2D}, so a point is also a
 * two dimensional point and the flat arithmetic still applies to it, but a vector is a different
 * animal and cannot stand in for a point.<br>
 * The one factory that is not just coordinates is {@code createLookingVector}, which is the
 * convenient way to get a unit length segment pointing where something is looking. There are two
 * overloads, one that takes an entity and asks it, and one that takes a yaw and pitch pair and
 * works them out.<br>
 * The distinction that catches people out is that coordinates here are not blocks. A {@code Pos}
 * is a point in the world at double precision, so {@code 0.5} is half a block along, while
 * {@link BlockPosHelper} built by {@link #createBlockPos(int, int, int)} is a whole block
 * coordinate and is what the block lookups take. The two are different types on purpose, and
 * converting between them is done by the type itself rather than by hand.
 * example:
 * <pre>
 * // a point in the world, at double precision, so this is half a block along x
 * const here = PositionCommon.createPos(0.5, 64, 0.5);
 * // the block that point is inside of, which is the one the block lookups want
 * const block = PositionCommon.createBlockPos(0, 64, 0);
 * const at = World.getBlock(block);
 * if (at !== null) {
 *   Chat.log(`in ${block.getX()}, ${block.getY()}, ${block.getZ()}: ${at.getName()}`);
 * }
 *
 * // a segment between two points, which can report its own length
 * const span = PositionCommon.createVec(0, 64, 0, 3, 64, 4);
 * Chat.log(`that is ${span.getMagnitude()} blocks away`);
 *
 * // a flat pair, for anything that is only two axes like a GUI or a map
 * const corner = PositionCommon.createPos(0, 0);
 * const otherCorner = PositionCommon.createPos(3, 4);
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.6.3
 */
@Library("PositionCommon")
@SuppressWarnings("unused")
public class FPositionCommon extends BaseLibrary {

    public FPositionCommon(Core<?, ?> runner) {
        super(runner);
    }

    /**
     * create a new vector object
     * <p>
     * The two triples are the start and the end of the segment, not a minimum and a maximum, so
     * the two ends can be given in either order and reversing them gives a vector running the
     * other way. This is the three dimensional form; {@link #createVec(double, double, double,
     * double)} is the flat one.
     * example:
     * <pre>
     * // a segment from one corner of a build to the opposite corner
     * const diagonal = PositionCommon.createVec(0, 64, 0, 3, 67, 4);
     * // what the game would call the angles of that direction
     * Chat.log(`yaw ${diagonal.getYaw()}, pitch ${diagonal.getPitch()}`);
     * // and how far it is, which is what a reach check wants
     * Chat.log(`${diagonal.getMagnitude()} blocks`);
     * </pre>
     *
     * @param x1 the x coordinate of the start of the segment
     * @param y1 the y coordinate of the start of the segment
     * @param z1 the z coordinate of the start of the segment
     * @param x2 the x coordinate of the end of the segment
     * @param y2 the y coordinate of the end of the segment
     * @param z2 the z coordinate of the end of the segment
     * @return a new three dimensional vector running from the first point to the second
     * @since 1.6.3
     */
    public Vec3D createVec(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new Vec3D(x1, y1, z1, x2, y2, z2);
    }

    /**
     * the direction an entity is looking, as a segment from the origin.
     * <p>
     * The start is the world origin and the end is one block out along the entity's look vector,
     * so the result is always of length {@code 1} whatever the entity is. That makes it a unit
     * direction rather than a position, and it is what a trace or a projection wants. The angles
     * are read straight off the entity, so they follow whatever the entity is looking at, which
     * for a player means its interpolated view angles rather than its nominal rotation.
     * <p>
     * For the angles themselves rather than a direction, {@link #createLookingVector(double,
     * double)} takes them as arguments.
     * example:
     * <pre>
     * // null outside a world, so there is nothing to take a look direction from there
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const look = PositionCommon.createLookingVector(player);
     *   // unit length by construction, and pointing where the player points
     *   const fiveBlocksOut = look.getEnd().scale(5);
     *   Chat.log(`${fiveBlocksOut} is straight ahead`);
     * }
     * </pre>
     *
     * @param entity the entity to read the look direction off
     * @return a unit length vector from the origin in the direction the entity is looking
     * @since 1.8.4
     */
    public Vec3D createLookingVector(EntityHelper<?> entity) {
        Vec3 rotation = entity.getRaw().getLookAngle();
        return new Vec3D(0, 0, 0, rotation.x, rotation.y, rotation.z);
    }

    /**
     * the direction a yaw and pitch pair points in, as a segment from the origin.
     * <p>
     * Note the order: <em>yaw first, pitch second</em>. This is the reverse of the vanilla
     * {@code Vec3.directionFromRotation} call underneath, which takes pitch first, and the two are
     * deliberately not the same order so that a script reading it lines up with
     * {@code Entity.getYaw} and {@code Entity.getPitch}. The angles are the same ones the game
     * uses for a view: yaw is the compass bearing in degrees, where positive is south and it
     * grows clockwise, and pitch is the vertical angle in degrees, where negative looks up.<br>
     * The result starts at the origin and ends one block out along that direction, so it is
     * always of length {@code 1}. Scale the end to get a point in the world, or hand the angles
     * themselves to something that only needs a bearing.
     * example:
     * <pre>
     * // the same two angles a player is looking with, in the same order
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const look = PositionCommon.createLookingVector(player.getYaw(), player.getPitch());
     *   // one block out is the direction itself, ten blocks out is a point in the world
     *   const ahead = look.getEnd().scale(10);
     *   Chat.log(`ten blocks ahead is ${ahead}`);
     *   // and read the angles straight back off the segment that was built from them
     *   Chat.log(`yaw ${look.getYaw()}, pitch ${look.getPitch()}`);
     * }
     * </pre>
     *
     * @param yaw the horizontal angle in degrees, where positive is south and it grows clockwise
     * @param pitch the vertical angle in degrees, where negative looks up
     * @return a unit length vector from the origin pointing along the given angles
     * @since 1.8.4
     */
    public Vec3D createLookingVector(double yaw, double pitch) {
        Vec3 rotation = Vec3.directionFromRotation((float) pitch, (float) yaw);
        return new Vec3D(0, 0, 0, rotation.x, rotation.y, rotation.z);
    }

    /**
     * create a new flat vector object.
     * <p>
     * Same idea as the three dimensional form, with the z axis left out. The two pairs are the
     * start and the end of the segment, and the result reports its length, its direction, and the
     * yaw and pitch of that direction as if the segment lay in the x and y plane.
     * example:
     * <pre>
     * // the span of a GUI element, or of a two axis screen
     * const width = PositionCommon.createVec(0, 0, 200, 0);
     * Chat.log(`${width.getMagnitude()} pixels wide`);
     * </pre>
     *
     * @param x1 the x coordinate of the start of the segment
     * @param y1 the y coordinate of the start of the segment
     * @param x2 the x coordinate of the end of the segment
     * @param y2 the y coordinate of the end of the segment
     * @return a new two dimensional vector running from the first point to the second
     * @since 1.6.3
     */
    public Vec2D createVec(double x1, double y1, double x2, double y2) {
        return new Vec2D(x1, y1, x2, y2);
    }

    /**
     * a point in the world.
     * <p>
     * This is a position, not a block: the coordinates are kept at double precision and are not
     * rounded, so {@code createPos(0.5, 64, 0.5)} is half a block along x and z. The block that
     * point falls inside is what {@link FWorld#getBlock(Pos3D)} works out, and
     * {@link #createBlockPos(int, int, int)} is how you name that block directly.
     * example:
     * <pre>
     * // a point, and then the block it lands in
     * const feet = PositionCommon.createPos(0.5, 64, 0.5);
     * const underneath = World.getBlock(feet);
     * if (underneath !== null) {
     *   Chat.log(`standing on ${underneath.getName()}`);
     * }
     * </pre>
     *
     * @param x the x coordinate of the point
     * @param y the y coordinate of the point
     * @param z the z coordinate of the point
     * @return a new three dimensional position at those coordinates
     * @since 1.6.3
     */
    public Pos3D createPos(double x, double y, double z) {
        return new Pos3D(x, y, z);
    }

    /**
     * a flat point.
     * <p>
     * The two axis version of {@link #createPos(double, double, double)}, for the many things
     * that have no depth to speak of: a GUI, a map, a two dimensional screen.
     * example:
     * <pre>
     * // the top left and bottom right of a rectangle on a flat surface
     * const topLeft = PositionCommon.createPos(0, 0);
     * const bottomRight = PositionCommon.createPos(64, 32);
     * // which can also be handed over as a segment between them
     * const across = topLeft.toVector(bottomRight);
     * Chat.log(`${across.getMagnitude()} across`);
     * </pre>
     *
     * @param x the x coordinate of the point
     * @param y the y coordinate of the point
     * @return a new two dimensional position at those coordinates
     * @since 1.6.3
     */
    public Pos2D createPos(double x, double y) {
        return new Pos2D(x, y);
    }

    /**
     * a whole block, by its block coordinates.
     * <p>
     * This is the other kind of position to {@link #createPos(double, double, double)}: block
     * coordinates are whole numbers naming one block, not a point that can sit partway inside
     * one, and the type is what the block lookups such as {@link FWorld#getBlock(BlockPosHelper)}
     * and the world scanning calls take. Given a fractional point, round it down rather than to
     * the nearest block, which is what {@link FWorld#getBlock(Pos3D)} does for you.
     * example:
     * <pre>
     * // a block by name, then the one above it, and a look at what is in each
     * const base = PositionCommon.createBlockPos(0, 64, 0);
     * for (const pos of [base, base.up()]) {
     *   const block = World.getBlock(pos);
     *   Chat.log(`${pos.getX()}, ${pos.getY()}, ${pos.getZ()}: ${block === null ? "unloaded" : block.getName()}`);
     * }
     * </pre>
     *
     * @param x the x position of the block
     * @param y the y position of the block
     * @param z the z position of the block
     * @return a {@link BlockPosHelper} for the given coordinates.
     * @since 1.8.4
     */
    public BlockPosHelper createBlockPos(int x, int y, int z) {
        return new BlockPosHelper(x, y, z);
    }

}
