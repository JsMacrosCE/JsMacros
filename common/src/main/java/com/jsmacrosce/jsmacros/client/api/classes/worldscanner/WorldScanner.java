package com.jsmacrosce.jsmacros.client.api.classes.worldscanner;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.BitStorage;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.access.IPackedIntegerArray;
import com.jsmacrosce.jsmacros.client.access.IPalettedContainer;
import com.jsmacrosce.jsmacros.client.access.IPalettedContainerData;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.util.ChunkPosCompat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class to scan the world for certain blocks. The results of the filters are cached,
 * so it's a good idea to reuse an instance of this if possible.
 * The scanner can either return a list of all block positions or
 * a list of blocks and their respective count for every block / state matching the filters criteria.
 * <p>
 * The cache is per block <i>state</i> rather than per block, so every variant of a block that appears
 * in the area is tested once and then kept, and reusing a scanner is what makes a second scan of the
 * same area cheap. Only chunks the client has been sent are walked, so a wide range is a lot of work
 * for a small answer, and everything here works from the client side alone rather than asking the
 * server.
 * <p>
 * There are three kinds of search and they differ in the units they take. The chunk searches take a
 * centre in chunk coordinates, the area searches take block coordinates, and the reach searches take
 * a point and a distance. A chunk coordinate is the block coordinate divided by sixteen, and the
 * parameter names say which of the two a method is expecting.
 * example:
 * <pre>
 * // a scanner of one kind of block, reused so the second scan is cheap
 * const scanner = World.getWorldScanner(
 *   JavaWrapper.methodToJava(function (block: BlockHelper) {
 *     return block.getId() === "minecraft:diamond_ore";
 *   }),
 *   null
 * );
 * if (scanner !== null) {
 *   // every position in the nine chunks around the player
 *   const found = scanner.scanAroundPlayer(1);
 *   Chat.log(`${found.size()} diamond ore, from ${scanner.getCachedAmount()} states tested`);
 *
 *   // the same scanner, asking for counts rather than positions
 *   const counts = scanner.getBlocksInChunks(0, 0, 0, true);
 *   Chat.log(`${counts.size()} kinds of block in the chunk at the origin`);
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.6.5
 */
@SuppressWarnings("unused")
public class WorldScanner {

    private static final Minecraft mc = Minecraft.getInstance();

    private final Level world;
    private final Map<BlockState, Boolean> cachedFilterStates;

    @Nullable
    private final Function<BlockState, Boolean> filter;

    private final boolean useParallelStream;

    /**
     * Creates a new World scanner with for the given world. It accepts two boolean functions,
     * one for {@link BlockHelper} and the other for {@link BlockStateHelper}.
     * <p>
     * Either function may be {@code null}, which skips that stage: with both null the scanner
     * matches every state, which is every block in the region. When both are given the block filter
     * runs first and the state filter narrows what it kept, so the two are stages rather than
     * alternatives.
     * <p>
     * A filter that is a script function decides whether the scan runs in parallel. A filter handed
     * over as a java wrapper only gets one when the script context it came from is itself
     * multi-threaded, and this is worked out once here rather than per scan.
     * example:
     * <pre>
     * // naming the parameter type is what tells the wrapper what it is being
     * // handed. The two are stages: a block is kept when the block filter and
     * // the state filter both accept it
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() !== "minecraft:dirt";
     *   }),
     *   JavaWrapper.methodToJava(function (state: BlockStateHelper) {
     *     return state.isSolid();
     *   })
     * );
     * if (scanner !== null) {
     *   Chat.log(`${scanner.scanAroundPlayer(2).size()} blocks matched`);
     * }
     * </pre>
     *
     * @param world       the world to scan
     * @param blockFilter a filter method for the blocks, or {@code null} to keep all of them
     * @param stateFilter a filter method for the block states, or {@code null} to keep all of those
     */
    public WorldScanner(Level world, @Nullable Function<BlockHelper, Boolean> blockFilter, @Nullable Function<BlockStateHelper, Boolean> stateFilter) {
        this.world = world;
        this.useParallelStream = isParallelStreamAllowed(blockFilter) && isParallelStreamAllowed(stateFilter);
        this.filter = combineFilter(blockFilter, stateFilter);
        cachedFilterStates = new ConcurrentHashMap<>();
    }

    /**
     * Gets a list of all chunks in the given range around the center chunk.
     * <p>
     * This is only the square of chunk positions, so it reads nothing from the world and returns the
     * same list whatever is loaded. The square is {@code 2 * chunkrange + 1} on a side, so a range of
     * 0 is the one centre chunk and a range of 1 is nine chunks, and the list runs x outer and z
     * inner, both ascending. Nothing here checks whether a chunk exists.
     * example:
     * <pre>
     * // the nine chunks around a chunk, as chunk positions rather than
     * // block ones, which is what the scan methods take
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) { return true; }),
     *   null
     * );
     * if (scanner !== null) {
     *   const chunks = scanner.getChunkRange(0, 0, 1);
     *   Chat.log(`${chunks.size()} chunks`);
     * }
     * </pre>
     *
     * @param centerX    the x coordinate of the center chunk to scan around
     * @param centerZ    the z coordinate of the center chunk to scan around
     * @param chunkrange the range to scan around the center chunk
     * @return a list of the chunk positions in the square, which is {@code 2 * chunkrange + 1}
     *         squared entries
     */
    public List<ChunkPos> getChunkRange(int centerX, int centerZ, int chunkrange) {
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = centerX - chunkrange; x <= centerX + chunkrange; x++) {
            for (int z = centerZ - chunkrange; z <= centerZ + chunkrange; z++) {
                chunks.add(new ChunkPos(x, z));
            }
        }
        return chunks;
    }

    /**
     * Scans all chunks in the given range around the player and returns a list of all block positions, for blocks matching the filter.
     * This will scan in a square with length 2*range + 1. So range = 0 for example will only scan the chunk the player
     * is standing in, while range = 1 will scan in a 3x3 area.
     * <p>
     * With no player this is an empty list rather than a failure, so a script that has not already
     * checked does not have to. Only chunks the client has been sent are walked, and the whole
     * height of each one is.
     * example:
     * <pre>
     * // the nine chunks around the player's own chunk
     * const found = World.findBlocksMatching("diamond_ore", 2);
     * if (found !== null) {
     *   for (const pos of found) {
     *     Chat.log(`diamond ore at ${pos.getX()}, ${pos.getY()}, ${pos.getZ()}`);
     *   }
     * }
     * </pre>
     *
     * @param chunkRange the range to scan around the center chunk
     * @return a list of all matching block positions.
     */
    public List<Pos3D> scanAroundPlayer(int chunkRange) {
        if (mc.player == null) return new ArrayList<>();
        ChunkPos playerPos = mc.player.chunkPosition();
        return scanChunkRange(ChunkPosCompat.x(playerPos), ChunkPosCompat.z(playerPos), chunkRange);
    }

    /**
     * Scans all chunks in the given range around the center chunk and returns a list of all block positions, for blocks matching the filter.
     * This will scan in a square with length 2*range + 1. So range = 0 for example will only scan the specified chunk,
     * while range = 1 will scan in a 3x3 area.
     * <p>
     * The centre is in chunk coordinates, the same divided by sixteen, and only chunks the client
     * has been sent are walked, so a wide range is a lot of work for what is usually a short answer.
     * The y range is the full height of the world and is clamped to it.
     * example:
     * <pre>
     * // the chunks around a chunk of the script's own choosing rather than
     * // the player's, so the search is the same wherever the player is
     * const found = World.findBlocksMatching(0, 0, "gold_ore", 1);
     * if (found !== null) {
     *   Chat.log(`${found.size()} gold ore in range`);
     * }
     * </pre>
     *
     * @param centerX    the x coordinate of the center chunk to scan around
     * @param centerZ    the z coordinate of the center chunk to scan around
     * @param chunkrange the range to scan around the center chunk
     * @return a list of all matching block positions.
     * @throws IllegalArgumentException if {@code chunkrange} is negative
     */
    public List<Pos3D> scanChunkRange(int centerX, int centerZ, int chunkrange) {
        assert world != null;
        if (chunkrange < 0) {
            throw new IllegalArgumentException("chunkrange must be at least 0");
        }
        return scanChunksInternal(getChunkRange(centerX, centerZ, chunkrange));
    }

    /**
     * scan area in blocks
     * <p>
     * This is a cube of {@code 2 * range + 1} blocks on a side centred on the position, and both
     * ends are counted, so a range of 0 is that one block. The y range is clamped to the world's,
     * and a cube entirely above or below it is empty.
     * example:
     * <pre>
     * // the 3x3x3 cube around a block position, both ends counted
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() === "minecraft:chest";
     *   }),
     *   null
     * );
     * if (scanner !== null) {
     *   const centre = PositionCommon.createBlockPos(100, 64, 100);
     *   const found = scanner.scanCubeArea(centre, 1);
     *   Chat.log(`${found.size()} chests in the cube`);
     * }
     * </pre>
     *
     * @param pos   the centre of the cube
     * @param range how many blocks out from the centre to go on each axis
     * @return the positions of every matching block in the cube
     * @throws IllegalArgumentException if {@code range} is negative
     * @since 1.9.1
     */
    public List<Pos3D> scanCubeArea(BlockPosHelper pos, int range) {
        return scanCubeArea(pos.getX(), pos.getY(), pos.getZ(), range);
    }

    /**
     * scan area in blocks
     * <p>
     * This is a cube of {@code 2 * range + 1} blocks on a side centred on the three coordinates,
     * and both ends are counted, so a range of 0 is that one block. The y range is clamped to the
     * world's, and a cube entirely above or below it is empty.
     * example:
     * <pre>
     * // the same cube written as three numbers rather than a position
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() === "minecraft:chest";
     *   }),
     *   null
     * );
     * if (scanner !== null) {
     *   const found = scanner.scanCubeArea(100, 64, 100, 1);
     *   Chat.log(`${found.size()} chests in the cube`);
     * }
     * </pre>
     *
     * @param x     the x coordinate of the centre of the cube
     * @param y     the y coordinate of the centre of the cube
     * @param z     the z coordinate of the centre of the cube
     * @param range how many blocks out from the centre to go on each axis
     * @return the positions of every matching block in the cube
     * @throws IllegalArgumentException if {@code range} is negative
     * @since 1.9.1
     */
    public List<Pos3D> scanCubeArea(int x, int y, int z, int range) {
        if (range < 0) throw new IllegalArgumentException("range cannot be negative!");
        return scanCubeAreaInternal(
                x - range, y - range, z - range,
                x + range, y + range, z + range
        ).collect(Collectors.toList());
    }

    /**
     * scan area in blocks
     * <p>
     * The first position is counted and the second is not, so a box from 0 to 10 on an axis is ten
     * blocks wide, ending at 9. An axis whose two ends are the same is empty whatever the others
     * are, and the y range is clamped to the world's, so a box entirely above or below it is empty
     * too. This differs from the overload taking a range, which is a cube counted on both ends.
     * example:
     * <pre>
     * // a box that includes the first corner and stops short of the second,
     * // so it is 10 blocks on a side here, ending at 9 on each axis
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() === "minecraft:chest";
     *   }),
     *   null
     * );
     * if (scanner !== null) {
     *   const from = PositionCommon.createBlockPos(0, 0, 0);
     *   const to = PositionCommon.createBlockPos(10, 10, 10);
     *   const found = scanner.scanCubeArea(from, to);
     *   Chat.log(`${found.size()} chests in the box`);
     * }
     * </pre>
     *
     * @param pos1 first pos, inclusive
     * @param pos2 second pos, exclusive
     * @return the positions of every matching block in the box, which is empty if any of the three
     *         axes has the same value at both ends
     * @since 1.9.1
     */
    public List<Pos3D> scanCubeArea(BlockPosHelper pos1, BlockPosHelper pos2) {
        return scanCubeArea(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
    }

    /**
     * scan area in blocks
     * <p>
     * The first set of coordinates is counted and the second is not, so a box from 0 to 10 on an
     * axis is ten blocks wide, ending at 9. An axis whose two ends are the same is empty whatever
     * the others are, and the y range is clamped to the world's, so a box entirely above or below it is empty
     * too. This differs from the overload taking a range, which is a cube counted on both ends.
     * example:
     * <pre>
     * // the same ten block box written as six numbers rather than two positions
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() === "minecraft:chest";
     *   }),
     *   null
     * );
     * if (scanner !== null) {
     *   const found = scanner.scanCubeArea(0, 0, 0, 10, 10, 10);
     *   Chat.log(`${found.size()} chests in the box`);
     * }
     * </pre>
     *
     * @param x1 first x coordinate, inclusive
     * @param y1 first y coordinate, inclusive
     * @param z1 first z coordinate, inclusive
     * @param x2 second x coordinate, exclusive
     * @param y2 second y coordinate, exclusive
     * @param z2 second z coordinate, exclusive
     * @return the positions of every matching block in the box, which is empty if any of the three
     *         axes has the same value at both ends
     * @since 1.9.1
     */
    public List<Pos3D> scanCubeArea(int x1, int y1, int z1, int x2, int y2, int z2) {
        if (x1 == x2 || y1 == y2 || z1 == z2) return new ArrayList<>();
        return scanCubeAreaInternal(
                x1, y1, z1,
                x2 - ((x2 - x1) >> 31 | 1),
                y2 - ((y2 - y1) >> 31 | 1),
                z2 - ((z2 - z1) >> 31 | 1)
        ).collect(Collectors.toList());
    }

    /**
     * scan area in blocks
     * <p>
     * Both positions are counted here, which is the one thing that separates this from the
     * {@link #scanCubeArea(BlockPosHelper, BlockPosHelper)} of the same shape, so the same two
     * corners give one more block on each axis: 0 to 10 is eleven blocks here and ten there. The
     * y range is clamped to the world's, so a box entirely above or below it is empty. A single
     * block is the two positions being the same.
     * example:
     * <pre>
     * // the same two corners as the non inclusive form, which makes
     * // this eleven blocks on a side rather than ten
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() === "minecraft:chest";
     *   }),
     *   null
     * );
     * if (scanner !== null) {
     *   const from = PositionCommon.createBlockPos(0, 0, 0);
     *   const to = PositionCommon.createBlockPos(10, 10, 10);
     *   const found = scanner.scanCubeAreaInclusive(from, to);
     *   Chat.log(`${found.size} chests in the closed box`);
     * }
     * </pre>
     *
     * @param pos1 first pos, inclusive
     * @param pos2 second pos, inclusive
     * @return the positions of every matching block in the box
     * @since 1.9.1
     */
    public List<Pos3D> scanCubeAreaInclusive(BlockPosHelper pos1, BlockPosHelper pos2) {
        return scanCubeAreaInclusive(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
    }

    /**
     * scan area in blocks
     * <p>
     * All six coordinates are counted here, so this is
     * {@link #scanCubeAreaInclusive(BlockPosHelper, BlockPosHelper)} written as numbers, and 0 to 10
     * is eleven blocks on an axis rather than the ten the other form gives. The y range is
     * clamped to the world's, so a box entirely above or below it is empty.
     * example:
     * <pre>
     * // the same closed box as the two position form, written as six numbers
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() === "minecraft:chest";
     *   }),
     *   null
     * );
     * if (scanner !== null) {
     *   const found = scanner.scanCubeAreaInclusive(0, 0, 0, 10, 10, 10);
     *   Chat.log(`${found.size} chests in the closed box`);
     * }
     * </pre>
     *
     * @param x1 first x coordinate, inclusive
     * @param y1 first y coordinate, inclusive
     * @param z1 first z coordinate, inclusive
     * @param x2 second x coordinate, inclusive
     * @param y2 second y coordinate, inclusive
     * @param z2 second z coordinate, inclusive
     * @return the positions of every matching block in the box
     * @since 1.9.1
     */
    public List<Pos3D> scanCubeAreaInclusive(int x1, int y1, int z1, int x2, int y2, int z2) {
        return scanCubeAreaInternal(x1, y1, z1, x2, y2, z2).collect(Collectors.toList());
    }

    /**
     * scan area in blocks
     * <p>
     * This is a sphere rather than a cube, and it is worked out in two steps. Below a radius of 48 a
     * cube around the position is walked and each block is then tested against the radius; at 48 and
     * above the chunk sections are walked instead, which is what makes a wide radius affordable. The
     * distance is measured from the centre of the block at the given position, which is half a
     * block back on each axis from the numbers handed in, to the block position of each candidate
     * rather than to its centre. That makes the measure half a block short on each axis from a true
     * centre to centre one, so a block is in when its corner is within the radius rather than when
     * its centre is. The y range is clamped to the world's.
     * example:
     * <pre>
     * // a radius of 20 is under the threshold, so this walks a 41 block cube
     * // and then discards what falls outside the sphere
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() === "minecraft:coal_ore";
     *   }),
     *   null
     * );
     * if (scanner !== null) {
     *   const here = PositionCommon.createPos(0.5, 64, 0.5);
     *   const found = scanner.scanSphereArea(here, 20);
     *   Chat.log(`${found.size()} coal ore within 20 blocks`);
     * }
     * </pre>
     *
     * @param pos    the centre of the sphere
     * @param radius how far out to go
     * @return the positions of every matching block whose centre is within the radius
     * @throws IllegalArgumentException if {@code radius} is negative
     * @since 1.9.1
     */
    public List<Pos3D> scanSphereArea(Pos3D pos, double radius) {
        return scanSphereArea(pos.x, pos.y, pos.z, radius);
    }

    /**
     * scan area in blocks
     * <p>
     * This is the {@link #scanSphereArea(Pos3D, double)} of the same search written as four numbers
     * rather than a position and a radius, and is measured the same way: from the centre of the
     * block at the given numbers to the block position of each candidate rather than to its
     * centre, so the same half a block applies on each axis.
     * example:
     * <pre>
     * // the same sphere written as three numbers and a radius
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() === "minecraft:coal_ore";
     *   }),
     *   null
     * );
     * if (scanner !== null) {
     *   const found = scanner.scanSphereArea(0.5, 64, 0.5, 20);
     *   Chat.log(`${found.size()} coal ore within 20 blocks`);
     * }
     * </pre>
     *
     * @param x      the x coordinate of the centre of the sphere
     * @param y      the y coordinate of the centre of the sphere
     * @param z      the z coordinate of the centre of the sphere
     * @param radius how far out to go
     * @return the positions of every matching block whose centre is within the radius
     * @throws IllegalArgumentException if {@code radius} is negative
     * @since 1.9.1
     */
    public List<Pos3D> scanSphereArea(double x, double y, double z, double radius) {
        if (radius < 0) throw new IllegalArgumentException("radius cannot be negative!");
        double sq = radius * radius;
        Vec3 centered = new Vec3(x - 0.5, y - 0.5, z - 0.5);

        if (radius < 48) return scanCubeAreaInternal(
                (int) Math.floor(x - radius),
                (int) Math.floor(y - radius),
                (int) Math.floor(z - radius),
                (int) Math.floor(x + radius),
                (int) Math.floor(y + radius),
                (int) Math.floor(z + radius)
        ).filter(pos -> centered.distanceToSqr(pos.x, pos.y, pos.z) <= sq).collect(Collectors.toList());

        // skip edge chunk sections because of large radius
        Stream<ChunkPos> stream = ChunkPos.rangeClosed(
                new ChunkPos((int) Math.floor(x - radius) >> 4, (int) Math.floor(z - radius) >> 4),
                new ChunkPos((int) Math.floor(x + radius) >> 4, (int) Math.floor(z + radius) >> 4)
        );
        if (useParallelStream) //noinspection DataFlowIssue
            stream = stream.parallel();
        return stream.flatMap(chunkPos -> {
                    double dx = Mth.clamp(centered.x, chunkPos.getMinBlockX(), chunkPos.getMaxBlockX()) - centered.x;
                    double dz = Mth.clamp(centered.z, chunkPos.getMinBlockZ(), chunkPos.getMaxBlockZ()) - centered.z;
                    double xzDistSq = dx * dx + dz * dz;
                    if (xzDistSq > sq) return null;

                    double ry = Math.sqrt(sq - xzDistSq);
                    return scanChunkInternal(chunkPos, (int) Math.floor(centered.y - ry), (int) Math.floor(centered.y + ry));
                })
                .filter(pos -> centered.distanceToSqr(pos.x, pos.y, pos.z) <= sq)
                .collect(Collectors.toList());
    }

    /**
     * scan around with player pos and player reach.<br>
     * this doesn't filter out positions that has obstacle.
     * <p>
     * This is from the player's eye position to the reach their client reports, which is not always
     * the ordinary 4.5, and it takes the strict form: a block is only in range if the player could
     * actually reach some part of its shape, so a block that is not a full cube and whose shape
     * stops short of the reach is left out. With no player this is an empty list.
     * example:
     * <pre>
     * // blocks of one kind the player could reach right now, which is the
     * // shape checked form and so leaves out a torch on a far wall
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() === "minecraft:stone_button";
     *   }),
     *   null
     * );
     * if (scanner !== null) {
     *   const found = scanner.scanReachable();
     *   Chat.log(`${found.size()} buttons in reach`);
     * }
     * </pre>
     *
     * @since 1.9.1
     */
    public List<Pos3D> scanReachable() {
        if (mc.player == null) return new ArrayList<>();
        var eyePos = mc.player.getEyePosition();
        return scanReachable(new Pos3D(eyePos.x, eyePos.y, eyePos.z), getReach(), true);
    }

    /**
     * scan around with player pos and player reach.<br>
     * this doesn't filter out positions that has obstacle.
     * <p>
     * This is the same search as the no argument form with the strict flag spelled out. A strict
     * scan checks the block's own shape, so a block with no shape at all is left out and one that
     * is not a full cube is only in range where its shape is; a non strict one only asks whether
     * the block's bounding box is within reach, which is a larger answer for anything that is not a
     * full cube. With no player this is an empty list.
     * example:
     * <pre>
     * // false drops the shape check, so this also finds a torch standing on
     * // the far side of a block the player's reach does not quite reach
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() === "minecraft:torch";
     *   }),
     *   null
     * );
     * if (scanner !== null) {
     *   const found = scanner.scanReachable(false);
     *   Chat.log(`${found.size()} torches within reach of the bounding box`);
     * }
     * </pre>
     *
     * @param strict if it should check for block outline instead of full cube, default is true
     * @since 1.9.1
     */
    public List<Pos3D> scanReachable(boolean strict) {
        if (mc.player == null) return new ArrayList<>();
        var eyePos = mc.player.getEyePosition();
        return scanReachable(new Pos3D(eyePos.x, eyePos.y, eyePos.z), getReach(), strict);
    }

    /**
     * scan around with the given pos and player reach.<br>
     * this doesn't filter out positions that has obstacle.
     * <p>
     * This is the same search from a position of the script's choosing rather than the player's
     * eyes. The reach is still the player's, which falls back to 4.5 when there is no player at all,
     * since unlike the overloads that read the eye position this one does not need one. The strict
     * form is used, and the reach is measured to the nearest point of a block's bounding box before
     * the shape is consulted.
     * example:
     * <pre>
     * // a search from a fixed point rather than from the player, so the
     * // answer does not move as the player does
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() === "minecraft:chest";
     *   }),
     *   null
     * );
     * if (scanner !== null) {
     *   const from = PositionCommon.createPos(0.5, 64, 0.5);
     *   const found = scanner.scanReachable(from);
     *   Chat.log(`${found.size()} chests within reach of the origin`);
     * }
     * </pre>
     *
     * @param pos the point to search from
     * @return the positions of every matching block within reach of {@code pos}
     * @since 1.9.1
     */
    public List<Pos3D> scanReachable(Pos3D pos) {
        return scanReachable(pos, getReach(), true);
    }

    /**
     * scan around with the given pos and the given reach.<br>
     * this doesn't filter out positions that has obstacle.
     * <p>
     * This is the {@link #scanReachable(Pos3D)} of the same search with the reach spelled out rather
     * than taken from the player, so it is the form to use for a reach the player's client does not
     * report, such as a reach a tool grants. The strict form is used.
     * example:
     * <pre>
     * // a reach of the script's own choosing, which is how a longer one from
     * // a tool is searched for without depending on the player's client
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() === "minecraft:chest";
     *   }),
     *   null
     * );
     * if (scanner !== null) {
     *   const from = PositionCommon.createPos(0.5, 64, 0.5);
     *   const found = scanner.scanReachable(from, 6.0);
     *   Chat.log(`${found.size()} chests within 6 blocks`);
     * }
     * </pre>
     *
     * @param pos   the point to search from
     * @param reach how far out from {@code pos} to go
     * @return the positions of every matching block within {@code reach} of {@code pos}
     * @since 1.9.1
     */
    public List<Pos3D> scanReachable(Pos3D pos, double reach) {
        return scanReachable(pos, reach, true);
    }

    /**
     * scan around with the given pos and the given reach.<br>
     * this doesn't filter out positions that has obstacle.
     * <p>
     * This is the full form, with every choice left to the script: the point, the reach and whether
     * the block's shape is consulted. A strict scan keeps a block only where its own shape is within
     * reach, which drops a block with an empty shape and narrows one that is not a full cube; a non
     * strict one only asks whether the block's bounding box is within reach. The reach is measured
     * to the nearest point of that box, so a block the player is looking past the edge of still
     * counts when any part of its box is close enough.
     * example:
     * <pre>
     * // the full form, with the strict shape check turned off and a reach
     * // of the script's own choosing
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() === "minecraft:chest";
     *   }),
     *   null
     * );
     * if (scanner !== null) {
     *   const from = PositionCommon.createPos(0.5, 64, 0.5);
     *   const found = scanner.scanReachable(from, 6.0, false);
     *   Chat.log(`${found.size()} chests within 6 blocks by bounding box`);
     * }
     * </pre>
     *
     * @param pos {@code Player.getPlayer().getEyePos()}
     * @param reach {@code Player.getInteractionManager().getReach()}
     * @param strict if it should check for block outline instead of full cube, default is true
     * @return the positions of every matching block within {@code reach} of {@code pos}
     * @throws IllegalArgumentException if {@code reach} is negative
     * @since 1.9.1
     */
    public List<Pos3D> scanReachable(Pos3D pos, double reach, boolean strict) {
        return scanReachableInternal(new Vec3(pos.x, pos.y, pos.z), reach, strict).collect(Collectors.toList());
    }

    /**
     * scan around with player pos and player reach, and return the closest one.<br>
     * this doesn't filter out positions that has obstacle.
     * <p>
     * This is the {@link #scanReachable()} of the same search with only the nearest match kept, so
     * the answer is a single position or nothing. Nothing within reach gives {@code null} rather
     * than an empty list, and so does having no player. The strict form is used, and the ranking is
     * by distance from the centre of the block at the player's eye position to the block position
     * of each candidate, which is half a block short on each axis from a centre to centre measure.
     * Two blocks equally near are decided by the order the scan found them in.
     * example:
     * <pre>
     * // the nearest block of one kind the player could reach, or null when
     * // there is none, which is why the result is checked rather than read
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() === "minecraft:chest";
     *   }),
     *   null
     * );
     * if (scanner !== null) {
     *   const nearest = scanner.scanClosestReachable();
     *   if (nearest !== null) {
     *     Chat.log(`nearest chest at ${nearest.getX()}, ${nearest.getY()}, ${nearest.getZ()}`);
     *   } else {
     *     Chat.log("no chest in reach");
     *   }
     * }
     * </pre>
     *
     * @return the nearest matching block within reach of the player's eyes, or {@code null} if
     *         there is none or there is no player
     * @since 1.9.1
     */
    @Nullable
    public Pos3D scanClosestReachable() {
        if (mc.player == null) return null;
        var eyePos = mc.player.getEyePosition();
        return scanClosestReachable(new Pos3D(eyePos.x, eyePos.y, eyePos.z), getReach(), true);
    }

    /**
     * scan around with player pos and player reach, and return the closest one.<br>
     * this doesn't filter out positions that has obstacle.
     * <p>
     * This is the {@link #scanClosestReachable()} of the same search with the strict flag spelled
     * out. A non strict nearest is the nearest by bounding box, which for a block that is not a full
     * cube can be a different block from the nearest one the player could actually touch.
     * example:
     * <pre>
     * // the nearest by bounding box rather than by shape, which is the
     * // looser of the two and so can name a block out of touch
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() === "minecraft:torch";
     *   }),
     *   null
     * );
     * if (scanner !== null) {
     *   const nearest = scanner.scanClosestReachable(false);
     *   if (nearest !== null) {
     *     Chat.log(`nearest torch at ${nearest.getX()}, ${nearest.getY()}, ${nearest.getZ()}`);
     *   }
     * }
     * </pre>
     *
     * @param strict if it should check for block outline instead of full cube, default is true
     * @return the nearest matching block within reach of the player's eyes, or {@code null} if
     *         there is none or there is no player
     * @since 1.9.1
     */
    @Nullable
    public Pos3D scanClosestReachable(boolean strict) {
        if (mc.player == null) return null;
        var eyePos = mc.player.getEyePosition();
        return scanClosestReachable(new Pos3D(eyePos.x, eyePos.y, eyePos.z), getReach(), strict);
    }

    /**
     * scan around with the given pos and the given reach, and return the closest one.<br>
     * this doesn't filter out positions that has obstacle.
     * <p>
     * This is the full form of the closest search, with the point, the reach and the strict flag
     * all left to the script. The reach test is the one
     * {@link #scanReachable(Pos3D, double, boolean)} does for the same arguments, and only the
     * ranking differs: it is by distance from the centre of the block at {@code pos} to the block
     * position of each candidate rather than to its centre, which is the same half a block short on
     * each axis that the sphere searches measure by. The reach test itself is a true point to box
     * distance, so only the ranking is affected by this.
     * example:
     * <pre>
     * // the full form, from a fixed point with a reach of the script's own
     * // choosing and the shape check turned off
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() === "minecraft:chest";
     *   }),
     *   null
     * );
     * if (scanner !== null) {
     *   const from = PositionCommon.createPos(0.5, 64, 0.5);
     *   const nearest = scanner.scanClosestReachable(from, 6.0, false);
     *   if (nearest !== null) {
     *     Chat.log(`nearest chest at ${nearest.getX()}, ${nearest.getY()}, ${nearest.getZ()}`);
     *   }
     * }
     * </pre>
     *
     * @param pos the point to search from
     * @param reach how far out from {@code pos} to go
     * @param strict if it should check for block outline instead of full cube, default is true
     * @return the nearest matching block within {@code reach} of {@code pos}, or {@code null} if
     *         there is none
     * @throws IllegalArgumentException if {@code reach} is negative
     * @since 1.9.1
     */
    @Nullable
    public Pos3D scanClosestReachable(Pos3D pos, double reach, boolean strict) {
        Vec3 vec = new Vec3(pos.x, pos.y, pos.z);
        Vec3 centered = vec.subtract(0.5, 0.5, 0.5);
        return scanReachableInternal(vec, reach, strict)
                .min(Comparator.comparingDouble(p -> centered.distanceToSqr(p.x, p.y, p.z)))
                .orElse(null);
    }

    private double getReach() {
        return mc.player != null ? mc.player.blockInteractionRange() : 4.5;
    }

    /**
     * all inclusive
     * @since 1.9.1
     */
    private Stream<Pos3D> scanCubeAreaInternal(int x1, int y1, int z1, int x2, int y2, int z2) {
        int worldBottom = world.getMinY();
        int worldTop = world.getHeight() - 1;
        if (Math.min(y1, y2) > worldTop || Math.max(y1, y2) < worldBottom) return Stream.empty();

        y1 = Mth.clamp(y1, worldBottom, worldTop);
        y2 = Mth.clamp(y2, worldBottom, worldTop);
        int dx = (x2 - x1) >> 31 | 1;
        int dy = (y2 - y1) >> 31 | 1;
        int dz = (z2 - z1) >> 31 | 1;

        int size = Math.abs((x2 - x1 + dx) * (y2 - y1 + 1) * (z2 - z1 + 1));
        if (size < 255) { // honestly idk where's the threshold
            if (size == 0) return Stream.empty();
            List<Pos3D> list = new ArrayList<>();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int y = y1; (y < y2 ^ dy == -1) || y == y2; y += dy)
                for (int z = z1; (z < z2 ^ dz == -1) || z == z2; z += dz)
                    for (int x = x1; (x < x2 ^ dx == -1) || x == x2; x += dx) {
                        if (getFilterResult(world.getBlockState(pos.set(x, y, z)))) {
                            list.add(new Pos3D(x, y, z));
                        }
                    }
            return getBestStream(list);
        }

        int minX = dx == 1 ? x1 : x2;
        int minY = dy == 1 ? y1 : y2;
        int minZ = dz == 1 ? z1 : z2;
        int maxX = dx == 1 ? x2 : x1;
        int maxY = dy == 1 ? y2 : y1;
        int maxZ = dz == 1 ? z2 : z1;
        Stream<ChunkPos> stream = ChunkPos.rangeClosed(
                new ChunkPos(x1 >> 4, z1 >> 4),
                new ChunkPos(x2 >> 4, z2 >> 4)
        );
        if (useParallelStream) //noinspection DataFlowIssue
            stream = stream.parallel();
        return stream.flatMap(chunkPos -> scanChunkInternal(chunkPos, minY, maxY))
                .filter(pos ->
                        minX <= pos.x && pos.x <= maxX &&
                        minY <= pos.y && pos.y <= maxY &&
                        minZ <= pos.z && pos.z <= maxZ
                );
    }

    /**
     * @since 1.9.1
     */
    private Stream<Pos3D> scanReachableInternal(Vec3 pos, double reach, boolean strict) {
        if (reach < 0) throw new IllegalArgumentException("reach cannot be negative!");
        double sq = reach * reach;

        Stream<Pos3D> stream = scanCubeAreaInternal(
                (int) Math.floor(pos.x - reach),
                (int) Math.floor(pos.y - reach),
                (int) Math.floor(pos.z - reach),
                (int) Math.floor(pos.x + reach),
                (int) Math.floor(pos.y + reach),
                (int) Math.floor(pos.z + reach)
        ).filter(p -> pos.distanceToSqr(
                Mth.clamp(pos.x, p.x, p.x + 1),
                Mth.clamp(pos.y, p.y, p.y + 1),
                Mth.clamp(pos.z, p.z, p.z + 1)
        ) <= sq);

        return !strict ? stream : stream.filter(p -> {
            BlockPos blockPos = BlockPos.containing(p.x, p.y, p.z);
            VoxelShape vs = world.getBlockState(blockPos).getShape(world, blockPos);
            if (vs.isEmpty()) return false;
            if (Shapes.block().equals(vs)) return true;

            Vec3 relative = pos.subtract(p.x, p.y, p.z);
            boolean[] isInRange = {false};
            vs.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
                if (isInRange[0]) return;
                isInRange[0] = relative.distanceToSqr(
                        Mth.clamp(relative.x, minX, maxX),
                        Mth.clamp(relative.y, minY, maxY),
                        Mth.clamp(relative.z, minZ, maxZ)
                ) <= sq;
            });
            return isInRange[0];
        });
    }

    private List<Pos3D> scanChunksInternal(List<ChunkPos> chunkPositions) {
        assert world != null;
        return getBestStream(chunkPositions).flatMap(this::scanChunkInternal).collect(Collectors.toList());
    }

    private Stream<Pos3D> scanChunkInternal(ChunkPos pos) {
        return scanChunkInternal(pos, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private Stream<Pos3D> scanChunkInternal(ChunkPos pos, int minY, int maxY) {
        int posX = ChunkPosCompat.x(pos);
        int posZ = ChunkPosCompat.z(pos);
        if (!world.hasChunk(posX, posZ)) {
            return Stream.empty();
        }

        long chunkX = (long) posX << 4;
        long chunkZ = (long) posZ << 4;

        List<Pos3D> blocks = new ArrayList<>();

        streamChunkSections(world.getChunk(posX, posZ), minY, maxY, (section, yOffset, isInFilter) -> {
            SimpleBitStorage array = (SimpleBitStorage) ((IPalettedContainer<?>) section.getStates()).jsmacros_getData().jsmacros_getStorage();
            forEach(array, isInFilter, place -> blocks.add(new Pos3D(
                    chunkX + ((place & 255) & 15),
                    yOffset + (place >> 8),
                    chunkZ + ((place & 255) >> 4)
            )));
        });
        return blocks.stream();
    }

    /**
     * Gets the amount of all blocks matching the criteria inside the chunk.
     * <p>
     * The key is the game's own text for the block, which is the id wrapped in {@code Block{...}},
     * so stone is keyed {@code Block{minecraft:stone}} rather than {@code minecraft:stone}. That
     * is the key whether {@code ignoreState} is true or not; the flag only decides whether the
     * state's property list is appended after it in square brackets, so with it false a stair is
     * keyed something like {@code Block{minecraft:oak_stairs}[facing=north,half=top]} and a block
     * with no properties of its own is keyed exactly as it is with the flag true.
     * <p>
     * A chunk the client has not been sent is skipped rather than failing, so this can be an empty
     * map for a chunk that is not loaded, and the counts are of the whole height of that one chunk.
     * example:
     * <pre>
     * // a count of the ores in one chunk. The centre here is a chunk
     * // coordinate, not a block one, so 0 is the chunk at the origin rather
     * // than the block at 0, 0
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId().endsWith("_ore");
     *   }),
     *   null
     * );
     * if (scanner !== null) {
     *   const counts = scanner.getBlocksInChunk(0, 0, true);
     *   for (const key of counts.keySet()) {
     *     Chat.log(`${key}: ${counts.get(key)}`);
     *   }
     * }
     * </pre>
     *
     * @param chunkX      the x coordinate of the chunk to scan
     * @param chunkZ      the z coordinate of the chunk to scan
     * @param ignoreState whether multiple states should be combined to a single block
     * @return a map of all blocks inside the specified chunk and their respective count.
     */
    public Map<String, Integer> getBlocksInChunk(int chunkX, int chunkZ, boolean ignoreState) {
        return getBlocksInChunks(chunkX, chunkZ, 0, ignoreState);
    }

    /**
     * Gets the amount of all blocks matching the criteria inside a square of chunks around the
     * given centre chunk.
     * <p>
     * This is the {@link #getBlocksInChunk(int, int, boolean)} over a square of chunks rather than
     * one, and the key is the same: the game's own text for the block, the id wrapped in
     * {@code Block{...}}, with the state's property list appended in square brackets only when
     * {@code ignoreState} is false. Counts from every chunk are added together, so a block present
     * in three chunks of the square is counted three times. Chunks the client has not been sent are
     * skipped.
     * example:
     * <pre>
     * // a count of the ores in the nine chunks around a chunk. The key
     * // comes from the block itself and has nothing to do with how the
     * // filter was built, so the filter's form is not what is shown here
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId().endsWith("_ore");
     *   }),
     *   null
     * );
     * if (scanner !== null) {
     *   const counts = scanner.getBlocksInChunks(0, 0, 1, true);
     *   Chat.log(`${counts.size()} kinds of ore in the nine chunks`);
     * }
     * </pre>
     *
     * @param centerX     the x coordinate of the center chunk to scan around
     * @param centerZ     the z coordinate of the center chunk to scan around
     * @param chunkRange  the range to scan around the center chunk
     * @param ignoreState whether multiple states should be combined to a single block
     * @return a map of all blocks inside the specified chunks and their respective count.
     * @throws IllegalArgumentException if {@code chunkRange} is negative
     */
    public Map<String, Integer> getBlocksInChunks(int centerX, int centerZ, int chunkRange, boolean ignoreState) {
        assert world != null;
        if (chunkRange < 0) {
            throw new IllegalArgumentException("chunkRange must be at least 0");
        }
        return getBlocksInChunksInternal(getChunkRange(centerX, centerZ, chunkRange), ignoreState);
    }

    private Map<String, Integer> getBlocksInChunksInternal(List<ChunkPos> chunkPositions, boolean ignoreState) {
        Object2IntOpenHashMap<String> result = new Object2IntOpenHashMap<>();

        getBestStream(chunkPositions).flatMap(pos -> {
            int posX = ChunkPosCompat.x(pos);
            int posZ = ChunkPosCompat.z(pos);
            if (!world.getChunkSource().hasChunk(posX, posZ)) {
                return Stream.empty();
            }

            Object2IntOpenHashMap<BlockState> blocks = new Object2IntOpenHashMap<>();

            streamChunkSections(world.getChunk(posX, posZ), (section, yOffset, isInFilter) -> count(section.getStates(), isInFilter, blocks::addTo));
            return blocks.object2IntEntrySet().stream();
        }).forEach(blockStateEntry -> {
            BlockState state = blockStateEntry.getKey();
            result.addTo(ignoreState ? state.getBlock().toString() : state.toString(), blockStateEntry.getIntValue());
        });
        return result;
    }

    private boolean getFilterResult(BlockState state) {
        Boolean v;
        return (v = cachedFilterStates.get(state)) == null ? addCachedState(state) : v;
    }

    private boolean addCachedState(BlockState state) {
        boolean isInFilter = false;

        if (filter != null) {
            isInFilter = filter.apply(state);
        }

        cachedFilterStates.put(state, isInFilter);
        return isInFilter;
    }

    private boolean[] getIncludedFilterIndices(Palette<BlockState> palette) {
        boolean commonBlockFound = false;
        boolean[] isInFilter = new boolean[palette.getSize()];

        for (int i = 0; i < palette.getSize(); i++) {
            BlockState state = palette.valueFor(i);
            if (getFilterResult(state)) {
                isInFilter[i] = true;
                commonBlockFound = true;
            } else {
                isInFilter[i] = false;
            }
        }

        if (!commonBlockFound) {
            return new boolean[0];
        }
        return isInFilter;
    }

    /**
     * Get the amount of cached block states. This will normally be around 200 - 400.
     * <p>
     * A state is cached the first time it is tested against the filter and never tested again, so
     * this is the number of distinct states this scanner has seen rather than a count of the
     * blocks it has walked. It stops going up once every state in the area has been seen, so a
     * second scan of the same area leaves it where it was, and a scanner with no filter at all
     * still reaches a number because the states are cached either way.
     * example:
     * <pre>
     * // scanning the same area twice finds the same blocks the second time,
     * // because the second scan reads the cache rather than the filter
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() === "minecraft:chest";
     *   }),
     *   null
     * );
     * if (scanner !== null) {
     *   const first = scanner.scanAroundPlayer(1);
     *   const seen = scanner.getCachedAmount();
     *   const second = scanner.scanAroundPlayer(1);
     *   Chat.log(`${first.size()} chests, ${seen} states cached, ${second.size} on the second pass`);
     * }
     * </pre>
     *
     * @return the amount of cached block states.
     */
    public int getCachedAmount() {
        return cachedFilterStates.size();
    }

    private <V> Stream<V> getBestStream(List<V> list) {
        if (useParallelStream) {
            return list.stream().parallel();
        } else {
            return list.stream();
        }
    }

    private void streamChunkSections(ChunkAccess chunk, TriConsumer<LevelChunkSection, Integer, boolean[]> consumer) {
        streamChunkSections(chunk, Integer.MIN_VALUE, Integer.MAX_VALUE, consumer);
    }

    private void streamChunkSections(ChunkAccess chunk, int minY, int maxY, TriConsumer<LevelChunkSection, Integer, boolean[]> consumer) {
        int yOffset = chunk.getMinY() - 16;
        minY &= ~15;
        for (LevelChunkSection section : chunk.getSections()) {
            yOffset += 16;
            if (yOffset < minY) continue;
            if (yOffset > maxY) break;
            if (section == null || section.hasOnlyAir()) {
                continue;
            }

            PalettedContainer<BlockState> sectionContainer = section.getStates();
            //this won't work if the PaletteStorage is of the type EmptyPaletteStorage
            if (!(((IPalettedContainer<?>) sectionContainer).jsmacros_getData().jsmacros_getStorage() instanceof SimpleBitStorage)) {
                continue;
            }

            boolean[] isInFilter = getIncludedFilterIndices(((IPalettedContainer<BlockState>) sectionContainer).jsmacros_getData().jsmacros_getPalette());
            if (isInFilter.length == 0) {
                continue;
            }
            consumer.accept(section, yOffset, isInFilter);
        }
    }

    private static boolean isParallelStreamAllowed(Function<?, Boolean> filter) {
        if (filter instanceof MethodWrapper<?, ?, ?, ?> wrapper) {
            if (!wrapper.getCtx().isMultiThreaded()) {
                return false;
            }
        }
        return true;
    }

    private static Function<BlockState, Boolean> combineFilter(Function<BlockHelper, Boolean> blockFilter, Function<BlockStateHelper, Boolean> stateFilter) {
        if (blockFilter != null) {
            if (stateFilter != null) {
                return state -> blockFilter.apply(new BlockHelper(state.getBlock())) && stateFilter.apply(new BlockStateHelper(state));
            } else {
                return state -> blockFilter.apply(new BlockHelper(state.getBlock()));
            }
        } else if (stateFilter != null) {
            return state -> stateFilter.apply(new BlockStateHelper(state));
        } else {
            return null;
        }
    }

    private static void forEach(SimpleBitStorage array, boolean[] isInFilter, IntConsumer action) {
        int counter = 0;

        int elementsPerLong = ((IPackedIntegerArray) array).jsmacros_getElementsPerLong();
        long maxValue = ((IPackedIntegerArray) array).jsmacros_getMaxValue();
        int elementBits = array.getBits();
        int size = array.getSize();

        for (long datum : array.getRaw()) {
            long row = datum;
            if (row == 0) {
                counter += elementsPerLong;
                continue;
            }
            for (int idx = 0; idx < elementsPerLong; idx++) {
                if (isInFilter[(int) (row & maxValue)]) {
                    action.accept(counter);
                }

                row >>= elementBits;
                counter++;
                if (counter >= size) {
                    return;
                }
            }
        }
    }

    private static void count(PalettedContainer<BlockState> container, boolean[] isInFilter, PalettedContainer.CountConsumer<BlockState> counter) {
        IPalettedContainerData<BlockState> data = ((IPalettedContainer<BlockState>) container).jsmacros_getData();
        Palette<BlockState> palette = data.jsmacros_getPalette();
        BitStorage storage = data.jsmacros_getStorage();

        int[] count = new int[palette.getSize()];

        if (palette.getSize() == 1) {
            counter.accept(palette.valueFor(0), storage.getSize());
        } else {
            storage.getAll(key -> count[key]++);
            for (int idx = 0; idx < count.length; idx++) {
                if (isInFilter[idx]) {
                    counter.accept(palette.valueFor(idx), count[idx]);
                }
            }
        }
    }

    @FunctionalInterface
    private interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);

    }

}
