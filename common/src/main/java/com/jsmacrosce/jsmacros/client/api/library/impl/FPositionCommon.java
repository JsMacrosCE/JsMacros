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
 * Creates 2D and 3D positions and segments, plus block positions and look-direction vectors.
 * Positions retain double coordinates; block positions use integer block coordinates. A vector
 * stores its start and end positions, while a position stores one point.
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

    /**
     * Initializes this position library with its associated core.
     * @param runner core associated with this library
     */
    public FPositionCommon(Core<?, ?> runner) {
        super(runner);
    }

    /**
     * Creates a 3D segment from its start coordinates to its end coordinates.
     * <p>
     * The two triples are the start and the end of the segment, not a minimum and a maximum, so
     * the two ends can be given in either order and reversing them gives a vector running the
     * other way. This is the three dimensional form; {@link #createVec(double, double, double,
     * double)} is the flat one.
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
     * Returns the entity's look direction as a segment from the origin.
     * <p>
     * The end coordinates are the entity's raw look-angle vector; no normalization is performed.
     * <p>
     * For the angles themselves rather than a direction, {@link #createLookingVector(double,
     * double)} takes them as arguments.
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const look = PositionCommon.createLookingVector(player);
     *   // the endpoint is the player's raw look-angle vector
     *   const fiveBlocksOut = look.getEnd().scale(5);
     *   Chat.log(`${fiveBlocksOut} is straight ahead`);
     * }
     * </pre>
     *
     * @param entity the entity to read the look direction off
     * @return the entity's raw look-angle vector from the origin
     * @since 1.8.4
     */
    public Vec3D createLookingVector(EntityHelper<?> entity) {
        Vec3 rotation = entity.getRaw().getLookAngle();
        return new Vec3D(0, 0, 0, rotation.x, rotation.y, rotation.z);
    }

    /**
     * Returns the direction for the supplied yaw and pitch as a segment from the origin.
     * <p>
     * Arguments are in yaw-then-pitch order, both in degrees; vanilla's conversion takes them in
     * pitch-then-yaw order. The returned endpoint is the direction vector produced by that
     * conversion.
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
     * @return the vector produced by converting the given angles, from the origin
     * @since 1.8.4
     */
    public Vec3D createLookingVector(double yaw, double pitch) {
        Vec3 rotation = Vec3.directionFromRotation((float) pitch, (float) yaw);
        return new Vec3D(0, 0, 0, rotation.x, rotation.y, rotation.z);
    }

    /**
     * Creates a 2D segment from its start coordinates to its end coordinates.
     * <p>
     * The two coordinate pairs are the start and end; there is no z coordinate.
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
     * Creates a 3D position from double-precision coordinates.
     * <p>
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
     * Creates a 2D position from its x and y coordinates.
     * <p>
     * Unlike {@link #createPos(double, double, double)}, this position has no z coordinate.
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
     * Creates a block position from integer block coordinates.
     * <p>
     * Unlike {@link #createPos(double, double, double)}, this names a whole block rather than a
     * point that may lie within one. For a fractional point, {@link FWorld#getBlock(Pos3D)} floors
     * each coordinate to find its containing block.
     *
     * @param x the block's x coordinate
     * @param y the block's y coordinate
     * @param z the block's z coordinate
     * @return the block position at those coordinates
     * @since 1.8.4
     */
    public BlockPosHelper createBlockPos(int x, int y, int z) {
        return new BlockPosHelper(x, y, z);
    }

}
