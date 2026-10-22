package com.jsmacrosce.jsmacros.client.api.helper.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * A block position: three whole numbers naming one block in the world, with the smallest
 * coordinates at its low corner. It is a value, not a handle on anything, so every method that
 * moves it gives back a new position and leaves this one alone.<br>
 * A script builds one with
 * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FPositionCommon#createBlockPos(int, int, int)},
 * from a chunk with {@link ChunkHelper#getOffsetBlock(int, int, int)}, or from a point with
 * {@link Pos3D#toRawBlockPos()}, and gets one back from a block with
 * {@link BlockDataHelper#getBlockPos()} or from an entity with {@link EntityHelper#getBlockPos()}.<br>
 * The six single-step methods and {@link #offset(String, int)} all cover the same ground, so pick
 * whichever reads better: {@code pos.up()} and {@code pos.offset("up")} are the same move, and
 * {@code pos.north(3)} and {@code pos.offset("north", 3)} are the same three.
 * example:
 * <pre>
 * const pos = PositionCommon.createBlockPos(10, 64, -3);
 * Chat.log(`start ${pos.getX()}, ${pos.getY()}, ${pos.getZ()}`);
 *
 * // nothing here changes pos itself, every move makes a new one
 * const up = pos.up();
 * const upThree = pos.up(3);
 * Chat.log(`${up} and ${upThree}, while pos is still ${pos}`);
 *
 * // the two spellings of the same move
 * Chat.log(`${pos.north(2)} equals ${pos.offset("north", 2)}`);   // true
 *
 * // and the position as a point rather than a block
 * Chat.log(pos.toPos3D());
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.2.6
 */
@SuppressWarnings("unused")
public class BlockPosHelper extends BaseHelper<BlockPos> {

    public BlockPosHelper(BlockPos b) {
        super(b);
    }

    public BlockPosHelper(int x, int y, int z) {
        super(new BlockPos(x, y, z));
    }

    /**
     * The {@code x} coordinate, counted in blocks from the world origin.
     * example:
     * <pre>
     * const pos = Player.getPlayer().getBlockPos();
     * if (pos.getX() > 1000) {
     *   Chat.log(`a long way east: x ${pos.getX()}`);
     * }
     * </pre>
     *
     * @return the {@code x} value of the block.
     * @since 1.2.6
     */
    public int getX() {
        return base.getX();
    }

    /**
     * The {@code y} coordinate, counted in blocks from the bottom of the world rather than from sea
     * level, so it can be negative.
     * example:
     * <pre>
     * // how far down the player is from the top of the build
     * const pos = Player.getPlayer().getBlockPos();
     * const chunk = World.getChunk(Math.floor(pos.getX() / 16), Math.floor(pos.getZ() / 16));
     * if (chunk !== null) {
     *   Chat.log(`${chunk.getMaxBuildHeight() - pos.getY()} blocks below the top`);
     * }
     * </pre>
     *
     * @return the {@code y} value of the block.
     * @since 1.2.6
     */
    public int getY() {
        return base.getY();
    }

    /**
     * The {@code z} coordinate, counted in blocks from the world origin.
     * example:
     * <pre>
     * const pos = Player.getPlayer().getBlockPos();
     * Chat.log(`z ${pos.getZ()}`);
     * </pre>
     *
     * @return the {@code z} value of the block.
     * @since 1.2.6
     */
    public int getZ() {
        return base.getZ();
    }

    /**
     * The block one above this one, which is the same as {@code offset("up")}.
     * example:
     * <pre>
     * // the block above the player's feet, and what is in it
     * const above = World.getBlock(Player.getPlayer().getBlockPos().up());
     * if (above !== null) {
     *   Chat.log(`directly overhead is ${above.getId()}`);
     * }
     * </pre>
     *
     * @return the block above.
     * @since 1.6.5
     */
    public BlockPosHelper up() {
        return new BlockPosHelper(getX(), getY() + 1, getZ());
    }

    /**
     * The block a number of blocks above this one. A distance of zero gives back a position equal
     * to this one, and a negative one goes the other way.
     * example:
     * <pre>
     * // a column of positions going up from the player
     * const feet = Player.getPlayer().getBlockPos();
     * for (let h = 1; h !== 4; h += 1) {
     *   const at = feet.up(h);
     *   const block = World.getBlock(at);
     *   if (block !== null) {
     *     Chat.log(`${h} up is ${block.getId()}`);
     *   }
     * }
     * </pre>
     *
     * @param distance the distance to move up
     * @return the block n-th block above.
     * @since 1.6.5
     */
    public BlockPosHelper up(int distance) {
        return new BlockPosHelper(getX(), getY() + distance, getZ());
    }

    /**
     * The block one below this one, which is the same as {@code offset("down")}.
     * example:
     * <pre>
     * // the block the player is standing on
     * const ground = World.getBlock(Player.getPlayer().getBlockPos().down());
     * if (ground !== null) {
     *   Chat.log(`standing on ${ground.getId()}`);
     * }
     * </pre>
     *
     * @return the block below.
     * @since 1.6.5
     */
    public BlockPosHelper down() {
        return new BlockPosHelper(getX(), getY() - 1, getZ());
    }

    /**
     * The block a number of blocks below this one.
     * example:
     * <pre>
     * // look for a floor under the player
     * const feet = Player.getPlayer().getBlockPos();
     * for (let d = 1; d !== 21; d += 1) {
     *   const at = feet.down(d);
     *   const block = World.getBlock(at);
     *   if (block !== null) {
     *     Chat.log(`${d} down is ${block.getId()}`);
     *   }
     * }
     * </pre>
     *
     * @param distance the distance to move down
     * @return the block n-th block below.
     * @since 1.6.5
     */
    public BlockPosHelper down(int distance) {
        return new BlockPosHelper(getX(), getY() - distance, getZ());
    }

    /**
     * The block one to the north, which is the same as {@code offset("north")}. North is towards
     * decreasing {@code z}.
     * example:
     * <pre>
     * // the four blocks around the player's feet
     * const feet = Player.getPlayer().getBlockPos();
     * for (const side of ["north", "south", "east", "west"]) {
     *   const at = feet.offset(side);
     *   const block = World.getBlock(at);
     *   if (block !== null) {
     *     Chat.log(`${side} of the player is ${block.getId()}`);
     *   }
     * }
     * </pre>
     *
     * @return the block to the north.
     * @since 1.6.5
     */
    public BlockPosHelper north() {
        return new BlockPosHelper(getX(), getY(), getZ() - 1);
    }

    /**
     * The block a number of blocks to the north.
     * example:
     * <pre>
     * // walk ten blocks north and report what is there
     * const from = Player.getPlayer().getBlockPos();
     * const to = from.north(10);
     * const block = World.getBlock(to);
     * if (block !== null) {
     *   Chat.log(`${to} is ${block.getId()}`);
     * }
     * </pre>
     *
     * @param distance the distance to move north
     * @return the n-th block to the north.
     * @since 1.6.5
     */
    public BlockPosHelper north(int distance) {
        return new BlockPosHelper(getX(), getY(), getZ() - distance);
    }

    /**
     * The block one to the south, which is the same as {@code offset("south")}. South is towards
     * increasing {@code z}.
     * example:
     * <pre>
     * const feet = Player.getPlayer().getBlockPos();
     * Chat.log(`${feet.south()} versus ${feet.north()}`);
     * </pre>
     *
     * @return the block to the south.
     * @since 1.6.5
     */
    public BlockPosHelper south() {
        return new BlockPosHelper(getX(), getY(), getZ() + 1);
    }

    /**
     * The block a number of blocks to the south.
     * example:
     * <pre>
     * // a ring around the player, ten blocks out
     * const feet = Player.getPlayer().getBlockPos();
     * for (const side of ["north", "south", "east", "west"]) {
     *   const at = feet.offset(side, 10);
     *   Chat.log(`${side} ten is ${at}`);
     * }
     * </pre>
     *
     * @param distance the distance to move south
     * @return the n-th block to the south.
     * @since 1.6.5
     */
    public BlockPosHelper south(int distance) {
        return new BlockPosHelper(getX(), getY(), getZ() + distance);
    }

    /**
     * The block one to the east, which is the same as {@code offset("east")}. East is towards
     * increasing {@code x}.
     * example:
     * <pre>
     * const feet = Player.getPlayer().getBlockPos();
     * const at = feet.east();
     * const block = World.getBlock(at);
     * if (block !== null) {
     *   Chat.log(`one east is ${block.getId()}`);
     * }
     * </pre>
     *
     * @return the block to the east.
     * @since 1.6.5
     */
    public BlockPosHelper east() {
        return new BlockPosHelper(getX() + 1, getY(), getZ());
    }

    /**
     * The block a number of blocks to the east.
     * example:
     * <pre>
     * // line the four sides of a build up with markers
     * const from = Player.getPlayer().getBlockPos();
     * for (let d = 1; d !== 5; d += 1) {
     *   Chat.log(`${from.east(d)} and ${from.west(d)}`);
     * }
     * </pre>
     *
     * @param distance the distance to move east
     * @return the n-th block to the east.
     * @since 1.6.5
     */
    public BlockPosHelper east(int distance) {
        return new BlockPosHelper(getX() + distance, getY(), getZ());
    }

    /**
     * The block one to the west, which is the same as {@code offset("west")}. West is towards
     * decreasing {@code x}.
     * example:
     * <pre>
     * const feet = Player.getPlayer().getBlockPos();
     * Chat.log(`${feet.east()} and ${feet.west()} straddle the player`);
     * </pre>
     *
     * @return the block to the west.
     * @since 1.6.5
     */
    public BlockPosHelper west() {
        return new BlockPosHelper(getX() - 1, getY(), getZ());
    }

    /**
     * The block a number of blocks to the west.
     * example:
     * <pre>
     * // the gap between two positions along x
     * const from = Player.getPlayer().getBlockPos();
     * const back = from.west(3);
     * Chat.log(`three blocks west of x ${from.getX()} is x ${back.getX()}`);
     * </pre>
     *
     * @param distance the distance to move west
     * @return the n-th block to the west.
     * @since 1.6.5
     */
    public BlockPosHelper west(int distance) {
        return new BlockPosHelper(getX() - distance, getY(), getZ());
    }

    /**
     * The block one step in the named direction. The name is one of the six the game uses,
     * lowercase: {@code down}, {@code up}, {@code north}, {@code south}, {@code west} or
     * {@code east}.<br>
     * This is the general form of the six single-direction methods and reads well when the
     * direction is already a variable; use {@link #up()} and its siblings when it is not.
     * example:
     * <pre>
     * // the six neighbours of a block, by name
     * const pos = Player.getPlayer().getBlockPos();
     * for (const name of ["down", "up", "north", "south", "west", "east"]) {
     *   Chat.log(`${name} of ${pos} is ${pos.offset(name)}`);
     * }
     * </pre>
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
     * The block a number of steps in the named direction, using the same six names as
     * {@link #offset(String)}. A distance of zero gives back a position equal to this one, and a
     * negative one moves the other way.
     * example:
     * <pre>
     * // a spiral of positions, four steps out then one up each time
     * let pos = Player.getPlayer().getBlockPos();
     * for (let ring = 1; ring !== 4; ring += 1) {
     *   for (const name of ["north", "east", "south", "west"]) {
     *     pos = pos.offset(name, ring);
     *     Chat.log(`ring ${ring} ${name} is ${pos}`);
     *   }
     *   pos = pos.up();
     * }
     * </pre>
     *
     * @param direction one of {@code down}, {@code up}, {@code north}, {@code south}, {@code west}
     *                  or {@code east}.
     * @param distance  the distance to move in the given direction
     * @return the n-th block offset by the given direction.
     * @since 1.6.5
     */
    public BlockPosHelper offset(String direction, int distance) {
        return new BlockPosHelper(base.relative(Direction.byName(direction), distance));
    }

    /**
     * The block reached by adding the three offsets to this one. This is the one to reach for when
     * the movement is a set of numbers rather than a direction, and it is also the one a box walk
     * wants, since a box is two corners and the difference between them.
     * example:
     * <pre>
     * // the block diagonally up and across from the player
     * const feet = Player.getPlayer().getBlockPos();
     * const at = feet.offset(1, 1, 1);
     * const block = World.getBlock(at);
     * if (block !== null) {
     *   Chat.log(`up and across is ${block.getId()} at ${at}`);
     * }
     *
     * // walk a box by moving from one corner to the other
     * const low = feet.offset(-8, 0, -8);
     * const high = feet.offset(8, 4, 8);
     * World.iterateBox(low, high, JavaWrapper.methodToJava(function (b) {
     *   Chat.log(`${b.getBlockPos()} is ${b.getId()}`);
     * }));
     * </pre>
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
     * This position read as a position in the nether, which means dividing {@code x} and {@code z}
     * by eight and leaving {@code y} alone. The arithmetic is done with whole numbers and Java's
     * own rounding for negatives, so a negative coordinate rounds towards zero rather than down.<br>
     * This is the conversion a portal uses, and nothing here knows whether a portal is actually at
     * this position.
     * example:
     * <pre>
     * // the same spot seen from the other dimension
     * const overworld = Player.getPlayer().getBlockPos();
     * const nether = overworld.toNetherCoords();
     * Chat.log(`overworld ${overworld} is nether ${nether}`);
     * Chat.log(`and back again ${nether.toOverworldCoords()}`);
     * </pre>
     *
     * @return the block position converted to the respective nether coordinates.
     * @since 1.8.4
     */
    public BlockPosHelper toNetherCoords() {
        return new BlockPosHelper(getX() / 8, getY(), getZ() / 8);
    }

    /**
     * This position read as a position in the overworld, which means multiplying {@code x} and
     * {@code z} by eight and leaving {@code y} alone.
     * example:
     * <pre>
     * // where a nether coordinate lands in the overworld
     * const nether = Player.getPlayer().getBlockPos().toNetherCoords();
     * Chat.log(`nether ${nether} is overworld ${nether.toOverworldCoords()}`);
     * </pre>
     *
     * @return the block position converted to the respective overworld coordinates.
     * @since 1.8.4
     */
    public BlockPosHelper toOverworldCoords() {
        return new BlockPosHelper(getX() * 8, getY(), getZ() * 8);
    }

    /**
     * How far this block is from an entity, measured to the middle of this block rather than to its
     * corner. That makes it the distance to the block's own centre, so a position compared with
     * itself is not zero unless the entity is exactly there.
     * example:
     * <pre>
     * // which nearby entity is nearest to the block the player is looking at
     * const centre = Player.detailedRayTraceBlock(5, false).getBlockPos();
     * if (centre !== null) {
     *   const nearby = World.getEntities(16);
     *   if (nearby !== null) {
     *     let best = null;
     *     let bestDistance = 0;
     *     for (let i = 0; i !== nearby.size(); i += 1) {
     *       const e = nearby.get(i);
     *       const away = centre.distanceTo(e);
     *       if (best === null) {
     *         best = e;
     *         bestDistance = away;
     *       } else {
     *         // the nearer of the two wins, which Math.min answers without a
     *         // comparison operator
     *         if (Math.min(away, bestDistance) === away) {
     *           best = e;
     *           bestDistance = away;
     *         }
     *       }
     *     }
     *     if (best !== null) {
     *       Chat.log(`nearest is ${best.getType()} at ${bestDistance} blocks`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @param entity the entity to get the distance to
     * @return the distance of this position to the given entity.
     * @since 1.8.4
     */
    public double distanceTo(EntityHelper<?> entity) {
        return Math.sqrt(base.distToCenterSqr(entity.getRaw().position()));
    }

    /**
     * How far this block is from another block, measured corner to corner: the numbers are used as
     * they are, with no half added to either end, so a position compared with itself is zero.
     * example:
     * <pre>
     * // is a chest within reach of where the player is standing
     * const feet = Player.getPlayer().getBlockPos();
     * const chest = feet.offset(3, 0, 0);
     * Chat.log(`${chest} is ${feet.distanceTo(chest)} blocks away`);
     * </pre>
     *
     * @param pos the position to get the distance to
     * @return the distance of this position to the given position.
     * @since 1.8.4
     */
    public double distanceTo(BlockPosHelper pos) {
        return Math.sqrt(base.distSqr(pos.base));
    }

    /**
     * How far this block is from a point, measured from this block's low corner to the point, which
     * is the same corner {@link #distanceTo(BlockPosHelper)} measures from.
     * example:
     * <pre>
     * // how far the block the player stands on is from their own feet
     * const feet = Player.getPlayer().getBlockPos();
     * Chat.log(`${feet.distanceTo(Player.getPlayer().getPos())} blocks to the corner`);
     * </pre>
     *
     * @param pos the position to get the distance to
     * @return the distance of this position to the given position.
     * @since 1.8.4
     */
    public double distanceTo(Pos3D pos) {
        return Math.sqrt(base.distToLowCornerSqr(pos.getX(), pos.getY(), pos.getZ()));
    }

    /**
     * How far this block is from a point given as three numbers, measured from this block's low
     * corner to the point. This is the same measurement as {@link #distanceTo(Pos3D)} without
     * needing a point to be built first.
     * example:
     * <pre>
     * // distance to an exact spot rather than to a block
     * const feet = Player.getPlayer().getBlockPos();
     * Chat.log(`${feet.distanceTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5)} to the middle`);
     * </pre>
     *
     * @param x the x coordinate to get the distance to
     * @param y the y coordinate to get the distance to
     * @param z the z coordinate to get the distance to
     * @return the distance of this position to the given position.
     * @since 1.8.4
     */
    public double distanceTo(double x, double y, double z) {
        return Math.sqrt(base.distToLowCornerSqr(x, y, z));
    }

    /**
     * This position as a point rather than a block, so the same three numbers with none of the
     * block meaning attached. A block position read this way sits on the block's low corner, which is
     * why {@code toPos3D().getX()} is a whole number rather than a half.
     * example:
     * <pre>
     * // the point at the low corner of the block, which World.getBlock takes
     * const pos = Player.getPlayer().getBlockPos();
     * const point = pos.toPos3D();
     * const block = World.getBlock(point);
     * if (block !== null) {
     *   Chat.log(`that point is in ${block.getId()}`);
     * }
     * </pre>
     *
     * @return the {@link Pos3D} representation of this position.
     * @since 1.8.4
     */
    public Pos3D toPos3D() {
        return new Pos3D(base.getX(), base.getY(), base.getZ());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BlockPosHelper) {
            BlockPosHelper other = (BlockPosHelper) obj;
            return base.equals(other.base);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return base.hashCode();
    }

    @Override
    public String toString() {
        return String.format("BlockPosHelper:{\"x\": %d, \"y\": %d, \"z\": %d}", base.getX(), base.getY(), base.getZ());
    }

}
