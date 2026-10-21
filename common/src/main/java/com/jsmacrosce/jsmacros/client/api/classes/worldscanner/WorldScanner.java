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
 * Scans chunks and block-coordinate regions for blocks matching optional block and state filters.
 * Filter results are cached per block state and reused by later scans on the same scanner. Scans
 * skip chunks unavailable to the client and do not request them from the server.
 * <p>
 * There are three kinds of search and they differ in the units they take. The chunk searches take a
 * centre in chunk coordinates, the area searches take block coordinates, and the reach searches take
 * a point and a distance. A chunk coordinate is the block coordinate divided by sixteen, and the
 * parameter names say which of the two a method is expecting.
 * <pre>
 * // reuse a scanner so matching states already evaluated are not tested again
 * const scanner = World.getWorldScanner(
 *   JavaWrapper.methodToJava(function (block) {
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
     * Constructs a scanner for {@code world}. Non-null block and state filters are both applied;
     * the block filter runs first, and either may be null to omit that test. With both null, every
     * encountered state matches. Scans may use parallel streams unless a wrapped script filter's
     * context is not multithreaded; this choice is made when the scanner is constructed.
     * <pre class="language-typescript">
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
        this(world, blockFilter, stateFilter, false);
    }

    WorldScanner(Level world, @Nullable Function<BlockHelper, Boolean> blockFilter, @Nullable Function<BlockStateHelper, Boolean> stateFilter, boolean sequential) {
        this.world = world;
        this.useParallelStream = !sequential && isParallelStreamAllowed(blockFilter) && isParallelStreamAllowed(stateFilter);
        this.filter = combineFilter(blockFilter, stateFilter);
        cachedFilterStates = new ConcurrentHashMap<>();
    }

    /**
     * Returns the square of chunk positions centered on the given chunk coordinates, without
     * checking whether those chunks are available. The side length is {@code 2 * chunkrange + 1};
     * positions are ordered by ascending X, then ascending Z.
     * <pre class="language-typescript">
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
     * Scans the square of chunks centered on the player's current chunk, returning matching block
     * positions across each chunk's full height. The side length is {@code 2 * chunkRange + 1};
     * returns an empty list when there is no player. Unavailable chunks are skipped.
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
     * Scans the square of chunks centered on the supplied chunk coordinates and returns matching
     * positions across their full height. The side length is {@code 2 * chunkrange + 1}; unavailable
     * chunks are skipped.
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
     * Scans a block-coordinate cube centered on {@code pos}, including both ends on each axis.
     * Its side length is {@code 2 * range + 1}; the Y bounds are clamped to world height, and a
     * cube entirely outside that height is empty.
     * <pre class="language-typescript">
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
     * Scans a cube centred on the supplied block coordinates, including both ends on each axis.
     * Its side length is {@code 2 * range + 1}; the y range is clamped to the world height.
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
     * Scans the box from the first position, inclusive, to the second, exclusive. An equal pair of
     * coordinates on any axis makes the result empty; the y range is clamped to world height.
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
     * Scans the box from the first coordinate triple, inclusive, to the second, exclusive. Equal
     * coordinates on any axis make the result empty; the y range is clamped to world height.
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
     * Scans the box including both positions on every axis. Equal coordinates describe one block;
     * the y range is clamped to world height. Unlike {@link #scanCubeArea(BlockPosHelper, BlockPosHelper)},
     * the second position is included.
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
     * Scans the box including both coordinate triples on every axis; the y range is clamped to
     * world height. Unlike {@link #scanCubeArea(int, int, int, int, int, int)}, the second triple
     * is included.
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
     * Scans for matching block positions within {@code radius} of {@code (pos.x - 0.5, pos.y - 0.5,
     * pos.z - 0.5)}. Distance is measured to each candidate's integer block position, not its
     * center; the y range is clamped to world height.
     *
     * @param pos    supplies {@code x}, {@code y}, and {@code z}; distance uses each coordinate minus {@code 0.5}
     * @param radius how far out to go
     * @return the positions of matching blocks whose block positions are within the radius
     * @throws IllegalArgumentException if {@code radius} is negative
     * @since 1.9.1
     */
    public List<Pos3D> scanSphereArea(Pos3D pos, double radius) {
        return scanSphereArea(pos.x, pos.y, pos.z, radius);
    }

    /**
     * Scans for matching block positions within {@code radius} of {@code (x - 0.5, y - 0.5, z - 0.5)}.
     * <p>
     * Distance is measured to each candidate's integer block position, not its center; the y range
     * is clamped to world height. This uses the same distance convention as
     * {@link #scanSphereArea(Pos3D, double)}.
     * <pre class="language-typescript">
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
     * @param x      x coordinate; the distance origin uses {@code x - 0.5}
     * @param y      y coordinate; the distance origin uses {@code y - 0.5}
     * @param z      z coordinate; the distance origin uses {@code z - 0.5}
     * @param radius how far out to go
     * @return the positions of matching blocks whose block positions are within the radius
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
     * Scans from the player's eye position using the client's block-interaction reach and the
     * strict shape check. Obstructions are not considered; with no player, returns an empty list.
     *
     * @return matching block positions, or an empty list when there is no player
     * @since 1.9.1
     */
    public List<Pos3D> scanReachable() {
        if (mc.player == null) return new ArrayList<>();
        var eyePos = mc.player.getEyePosition();
        return scanReachable(new Pos3D(eyePos.x, eyePos.y, eyePos.z), getReach(), true);
    }

    /**
     * Scans from the player's eye position using the client's block-interaction reach. Obstructions
     * are not considered; with no player, returns an empty list. With {@code strict} true, the
     * block's shape must be within reach; otherwise its unit block box is tested instead.
     *
     * @param strict whether to require the block's shape to be within reach; false tests its unit block box
     * @return matching block positions, or an empty list when there is no player
     * @since 1.9.1
     */
    public List<Pos3D> scanReachable(boolean strict) {
        if (mc.player == null) return new ArrayList<>();
        var eyePos = mc.player.getEyePosition();
        return scanReachable(new Pos3D(eyePos.x, eyePos.y, eyePos.z), getReach(), strict);
    }

    /**
     * Scans from {@code pos} using the player's block-interaction reach (4.5 when there is no
     * player) and the strict shape check. Obstructions are not considered.
     *
     * @param pos the point to search from
     * @return matching positions whose unit block boxes, and in strict mode shapes, are within reach of {@code pos}
     * @since 1.9.1
     */
    public List<Pos3D> scanReachable(Pos3D pos) {
        return scanReachable(pos, getReach(), true);
    }

    /**
     * Scans from {@code pos} using the supplied reach and strict shape check. Obstructions are not
     * considered; unlike {@link #scanReachable(Pos3D)}, this overload does not obtain reach from the player.
     *
     * @param pos   the point to search from
     * @param reach how far out from {@code pos} to go
     * @return matching positions whose unit block boxes and shapes are within {@code reach} of {@code pos}
     * @since 1.9.1
     */
    public List<Pos3D> scanReachable(Pos3D pos, double reach) {
        return scanReachable(pos, reach, true);
    }

    /**
     * Scans from {@code pos} using {@code reach}; obstructions are not considered. Distance is
     * measured to each unit block box, and when {@code strict} is true the block's shape must also
     * be within reach. A negative reach throws {@link IllegalArgumentException}.
     *
     * @param pos the point to search from
     * @param reach the maximum distance in blocks
     * @param strict whether to require the block's shape to be within reach; false tests its unit block box
     * @return matching positions whose unit block boxes, and in strict mode shapes, are within reach
     * @since 1.9.1
     */
    public List<Pos3D> scanReachable(Pos3D pos, double reach, boolean strict) {
        return scanReachableInternal(new Vec3(pos.x, pos.y, pos.z), reach, strict).collect(Collectors.toList());
    }

    /**
     * Returns the nearest matching block within the player's block interaction range, measured
     * from the player's eyes; obstructions are not considered. Uses the strict shape check and
     * ranks candidates by distance to their block positions, not their centres.
     * <pre class="language-typescript">
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
     * Returns the nearest matching block within the player's block interaction range, measured
     * from the player's eyes; obstructions are not considered. With {@code strict} false, reach
     * and ranking use unit block boxes rather than block shapes.
     * <pre class="language-typescript">
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
     * @param strict whether reach should also be checked against the block's shape
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
     * Returns the nearest matching block within {@code reach} of {@code pos}; obstructions are not
     * considered. Reach is measured to unit block boxes, and with {@code strict} true the block's
     * shape must also be within reach. Candidates are ranked by distance from {@code pos} shifted
     * by {@code (-0.5, -0.5, -0.5)} to their block positions.
     * <pre class="language-typescript">
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
     * @param reach maximum distance in blocks from {@code pos}
     * @param strict whether to require the block's shape to be within reach
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
     * Scans the inclusive block-coordinate bounds, clamping the Y bounds to the world's build height.
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
     * Filters blocks whose unit boxes are within {@code reach} of {@code pos}; when {@code strict}
     * is true, also requires a non-empty block shape within reach.
     *
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
     * Keys are Minecraft block/state strings: {@code ignoreState} uses the block string, while
     * false uses the state string including its properties.
     * <p>
     * Counts cover the chunk's full height. Chunks not available to the client are skipped.
     * <pre class="language-typescript">
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
     * Scans the square from {@code centerX - chunkRange} through {@code centerX + chunkRange} and
     * likewise for Z. Keys follow {@link #getBlocksInChunk(int, int, boolean)}; counts from all
     * available chunks are added together, and unavailable chunks are skipped.
     * <pre class="language-typescript">
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
     * @param chunkRange  non-negative radius in chunks; zero scans only the center chunk
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
     * Returns the number of distinct block states cached after filter evaluation. A state is
     * evaluated at most once per scanner; this is not a count of scanned blocks.
     *
     * @return the number of cached block states
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
