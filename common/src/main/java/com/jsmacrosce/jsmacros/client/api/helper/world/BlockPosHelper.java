package com.jsmacrosce.jsmacros.client.api.helper.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * A block position with integer world coordinates. Movement methods return new positions and leave
 * this position unchanged; directional methods correspond to {@link #offset(String, int)}.
 * <pre>
 * const pos = PositionCommon.createBlockPos(10, 64, -3);
 * Chat.log(`start ${pos.getX()}, ${pos.getY()}, ${pos.getZ()}`);
 *
 * const up = pos.up();
 * const upThree = pos.up(3);
 * Chat.log(`${up} and ${upThree}, while pos is still ${pos}`);
 *
 * Chat.log(`${pos.north(2)} equals ${pos.offset("north", 2)}`);   // true
 *
 * Chat.log(pos.toPos3D());
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.2.6
 */
@SuppressWarnings("unused")
public class BlockPosHelper extends BaseHelper<BlockPos> {

    /**
     * Wraps the supplied block position, retaining it as the wrapped value.
     * @param b position to wrap
     */
    public BlockPosHelper(BlockPos b) {
        super(b);
    }

    /**
     * Wraps a newly constructed block position from the supplied block coordinates.
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     */
    public BlockPosHelper(int x, int y, int z) {
        super(new BlockPos(x, y, z));
    }

    /**
     * Returns the {@code x} coordinate in blocks.
     * @return the {@code x} coordinate.
     * @since 1.2.6
     */
    public int getX() {
        return base.getX();
    }

    /**
     * Returns the {@code y} coordinate in blocks.
     * @return the {@code y} coordinate.
     * @since 1.2.6
     */
    public int getY() {
        return base.getY();
    }

    /**
     * Returns the {@code z} coordinate in blocks.
     * @return the {@code z} coordinate.
     * @since 1.2.6
     */
    public int getZ() {
        return base.getZ();
    }

    /**
     * Returns the position one block above this one, equivalent to {@code offset("up")}.
     * @return the position above this one.
     * @since 1.6.5
     */
    public BlockPosHelper up() {
        return new BlockPosHelper(getX(), getY() + 1, getZ());
    }

    /**
     * Returns a position moved by {@code distance} blocks upward; negative values move downward.
     * @param distance the signed distance to move
     * @return the resulting position.
     * @since 1.6.5
     */
    public BlockPosHelper up(int distance) {
        return new BlockPosHelper(getX(), getY() + distance, getZ());
    }

    /**
     * Returns the position one block below this one, equivalent to {@code offset("down")}.
     * @return the position below this one.
     * @since 1.6.5
     */
    public BlockPosHelper down() {
        return new BlockPosHelper(getX(), getY() - 1, getZ());
    }

    /**
     * Returns a position moved by {@code distance} blocks downward; negative values move upward.
     * @param distance the signed distance to move
     * @return the resulting position.
     * @since 1.6.5
     */
    public BlockPosHelper down(int distance) {
        return new BlockPosHelper(getX(), getY() - distance, getZ());
    }

    /**
     * Returns the position one block north, equivalent to {@code offset("north")}. North is toward
     * decreasing {@code z}.
     * @return the position to the north.
     * @since 1.6.5
     */
    public BlockPosHelper north() {
        return new BlockPosHelper(getX(), getY(), getZ() - 1);
    }

    /**
     * Returns a position moved {@code distance} blocks north (toward decreasing {@code z}).
     * @param distance the distance to move north
     * @return the resulting position.
     * @since 1.6.5
     */
    public BlockPosHelper north(int distance) {
        return new BlockPosHelper(getX(), getY(), getZ() - distance);
    }

    /**
     * Returns the adjacent block to the south (increasing {@code z}); equivalent to
     * {@code offset("south")}.
     *
     * @return the block to the south.
     * @since 1.6.5
     */
    public BlockPosHelper south() {
        return new BlockPosHelper(getX(), getY(), getZ() + 1);
    }

    /**
     * Returns a position moved by {@code distance} blocks to the south (increasing {@code z}).
     *
     * @param distance the distance to move south
     * @return the position at the requested signed distance south of this one.
     * @since 1.6.5
     */
    public BlockPosHelper south(int distance) {
        return new BlockPosHelper(getX(), getY(), getZ() + distance);
    }

    /**
     * Returns the adjacent block to the east (increasing {@code x}); equivalent to
     * {@code offset("east")}.
     *
     * @return the block to the east.
     * @since 1.6.5
     */
    public BlockPosHelper east() {
        return new BlockPosHelper(getX() + 1, getY(), getZ());
    }

    /**
     * Returns a position moved by {@code distance} blocks to the east (increasing {@code x}).
     *
     * @param distance the distance to move east
     * @return the position at the requested signed distance east of this one.
     * @since 1.6.5
     */
    public BlockPosHelper east(int distance) {
        return new BlockPosHelper(getX() + distance, getY(), getZ());
    }

    /**
     * Returns the adjacent block to the west (decreasing {@code x}); equivalent to
     * {@code offset("west")}.
     *
     * @return the block to the west.
     * @since 1.6.5
     */
    public BlockPosHelper west() {
        return new BlockPosHelper(getX() - 1, getY(), getZ());
    }

    /**
     * Returns a position moved by {@code distance} blocks to the west (decreasing {@code x}).
     *
     * @param distance the distance to move west
     * @return the position at the requested signed distance west of this one.
     * @since 1.6.5
     */
    public BlockPosHelper west(int distance) {
        return new BlockPosHelper(getX() - distance, getY(), getZ());
    }

    /**
     * Returns the adjacent position in the named direction. The name must be one of
     * {@code down}, {@code up}, {@code north}, {@code south}, {@code west}, or {@code east}.
     *
     * @param direction one of {@code down}, {@code up}, {@code north}, {@code south}, {@code west}
     *                  or {@code east}.
     * @return the block offset by the given direction.
     * @since 1.6.5
     */
    public BlockPosHelper offset(String direction) {
        return new BlockPosHelper(base.relative(Direction.byName(direction)));
    }

    /**
     * Returns a position moved {@code distance} blocks in the named direction; negative distances
     * move in the opposite direction. Direction names are the same as for {@link #offset(String)}.
     *
     * @param direction one of {@code down}, {@code up}, {@code north}, {@code south}, {@code west}
     *                  or {@code east}.
     * @param distance  the distance to move in the given direction
     * @return the position at the requested signed distance in the given direction.
     * @since 1.6.5
     */
    public BlockPosHelper offset(String direction, int distance) {
        return new BlockPosHelper(base.relative(Direction.byName(direction), distance));
    }

    /**
     * Returns the position formed by adding the given coordinate offsets to this position.
     *
     * @param x the x offset
     * @param y the y offset
     * @param z the z offset
     * @return the block offset by the given values.
     * @since 1.8.4
     */
    public BlockPosHelper offset(int x, int y, int z) {
        return new BlockPosHelper(new BlockPos(getX() + x, getY() + y, getZ() + z));
    }

    /**
     * Converts coordinates by integer-dividing {@code x} and {@code z} by eight and leaving
     * {@code y} unchanged. Negative coordinates therefore truncate toward zero; this does not
     * locate or verify a portal.
     *
     * @return the block position converted to the respective nether coordinates.
     * @since 1.8.4
     */
    public BlockPosHelper toNetherCoords() {
        return new BlockPosHelper(getX() / 8, getY(), getZ() / 8);
    }

    /**
     * Converts this position from Nether to Overworld coordinates by multiplying {@code x} and
     * {@code z} by eight and leaving {@code y} unchanged.
     *
     * @return the converted Overworld position.
     * @since 1.8.4
     */
    public BlockPosHelper toOverworldCoords() {
        return new BlockPosHelper(getX() * 8, getY(), getZ() * 8);
    }

    /**
     * Returns the Euclidean distance from this block's center to the entity's position.
     *
     * @param entity the entity to get the distance to
     * @return the distance in blocks.
     * @since 1.8.4
     */
    public double distanceTo(EntityHelper<?> entity) {
        return Math.sqrt(base.distToCenterSqr(entity.getRaw().position()));
    }

    /**
     * Returns the Euclidean distance between the two block positions, using their integer
     * coordinates without centering either block.
     *
     * @param pos the position to get the distance to
     * @return the distance in blocks.
     * @since 1.8.4
     */
    public double distanceTo(BlockPosHelper pos) {
        return Math.sqrt(base.distSqr(pos.base));
    }

    /**
     * Returns the Euclidean distance from this block position's low corner to a point.
     *
     * @param pos the point to measure to
     * @return the distance in blocks.
     * @since 1.8.4
     */
    public double distanceTo(Pos3D pos) {
        return Math.sqrt(base.distToLowCornerSqr(pos.getX(), pos.getY(), pos.getZ()));
    }

    /**
     * Returns the Euclidean distance from this block position's low corner to the given point.
     *
     * @param x the point's x coordinate
     * @param y the point's y coordinate
     * @param z the point's z coordinate
     * @return the distance in blocks.
     * @since 1.8.4
     */
    public double distanceTo(double x, double y, double z) {
        return Math.sqrt(base.distToLowCornerSqr(x, y, z));
    }

    /**
     * Returns this position's integer coordinates as a point at the block's low corner.
     *
     * @return this position represented as a {@link Pos3D}.
     * @since 1.8.4
     */
    public Pos3D toPos3D() {
        return new Pos3D(base.getX(), base.getY(), base.getZ());
    }

    /**
     * Compares this helper with another helper by their wrapped positions; other object types,
     * including {@code null}, are unequal.
     * @param obj object to compare
     * @return {@code true} if {@code obj} is a helper whose wrapped position compares equal
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BlockPosHelper) {
            BlockPosHelper other = (BlockPosHelper) obj;
            return base.equals(other.base);
        }
        return false;
    }

    /**
     * Returns the hash code delegated from the wrapped position.
     * @return the wrapped position's hash code
     */
    @Override
    public int hashCode() {
        return base.hashCode();
    }

    /**
     * Returns a diagnostic string in the format {@code BlockPosHelper:{"x": %d, "y": %d, "z": %d}},
     * with the coordinate getters' values substituted in x/y/z order.
     * @return the formatted diagnostic string
     */
    @Override
    public String toString() {
        return String.format("BlockPosHelper:{\"x\": %d, \"y\": %d, \"z\": %d}", base.getX(), base.getY(), base.getZ());
    }

}
