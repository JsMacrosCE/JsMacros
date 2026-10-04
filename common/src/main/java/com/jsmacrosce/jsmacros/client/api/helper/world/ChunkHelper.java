package com.jsmacrosce.jsmacros.client.api.helper.world;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;
import com.jsmacrosce.jsmacros.util.ChunkPosCompat;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * A sixteen by sixteen column of the world, and everything that can be asked of one without leaving
 * it. A script gets one from {@code World.getChunk(x, z)}, which takes chunk coordinates rather
 * than a block position, and from the chunk events.<br>
 * The thing to know before using any of this is that a chunk has two coordinate systems, and they
 * are not interchangeable. {@link #getChunkX()} and {@link #getChunkZ()} name the chunk itself,
 * while everything else on this class is relative to the chunk's own corner: {@link #getOffsetBlock(int, int, int)}
 * takes offsets from zero to fifteen, not world coordinates.
 * example:
 * <pre>
 * const player = Player.getPlayer();
 * const chunk = World.getChunk(Math.floor(player.getX() / 16), Math.floor(player.getZ() / 16));
 * if (chunk !== null) {
 *   Chat.log(`chunk ${chunk.getChunkX()}, ${chunk.getChunkZ()}`);
 *   Chat.log(`its low corner is ${chunk.getStartingBlock()}`);
 *   Chat.log(`it runs from y ${chunk.getMinBuildHeight()} to ${chunk.getMaxBuildHeight()}`);
 *
 *   // offsets are relative to the chunk, not to the world
 *   const corner = chunk.getOffsetBlock(0, chunk.getMaxBuildHeight(), 0);
 *   const block = World.getBlock(corner);
 *   if (block !== null) {
 *     Chat.log(`the top of the corner column is ${block.getId()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class ChunkHelper extends BaseHelper<ChunkAccess> {

    public ChunkHelper(ChunkAccess base) {
        super(base);
    }

    /**
     * The world position of the chunk's own {@code (0, 0, 0)}, which is the corner every offset on
     * this class is measured from. This is the block {@link #getOffsetBlock(int, int, int)} reaches
     * with all three offsets at zero.
     * example:
     * <pre>
     * const chunk = World.getChunk(0, 0);
     * if (chunk !== null) {
     *   const corner = chunk.getStartingBlock();
     *   Chat.log(`chunk ${chunk.getChunkX()}, ${chunk.getChunkZ()} starts at ${corner}`);
     *   // a full chunk is sixteen blocks along x and z
     *   Chat.log(`and ends at ${corner.east(15).south(15)}`);
     * }
     * </pre>
     *
     * @return the first block (0 0 0 coordinate) of this chunk.
     * @since 1.8.4
     */
    public BlockPosHelper getStartingBlock() {
        return new BlockPosHelper(base.getPos().getBlockAt(0, 0, 0));
    }

    /**
     * A block in this chunk, named by its offset from the chunk's own corner. The {@code x} and
     * {@code z} offsets run from zero to fifteen and the {@code y} offset is an absolute height
     * rather than an offset, since a chunk reaches all the way from the bottom of the world to the
     * top. Passing offsets outside that gives a position that is in a different chunk.
     * example:
     * <pre>
     * const chunk = World.getChunk(0, 0);
     * if (chunk !== null) {
     *   // the middle of the chunk, at sea level
     *   const middle = chunk.getOffsetBlock(8, 64, 8);
     *   const block = World.getBlock(middle);
     *   if (block !== null) {
     *     Chat.log(`the middle of the chunk holds ${block.getId()}`);
     *   }
     *
     *   // the four corners, all at the bottom of the world
     *   for (const [x, z] of [[0, 0], [15, 0], [0, 15], [15, 15]]) {
     *     Chat.log(`corner ${x}, ${z} is ${chunk.getOffsetBlock(x, chunk.getMinBuildHeight(), z)}`);
     *   }
     * }
     * </pre>
     *
     * @param xOffset the xOffset offset
     * @param y       the actual y coordinate
     * @param zOffset the zOffset offset
     * @return the block offset from the starting block of this chunk by xOffset y zOffset.
     * @since 1.8.4
     */
    public BlockPosHelper getOffsetBlock(int xOffset, int y, int zOffset) {
        return new BlockPosHelper(base.getPos().getBlockAt(xOffset, y, zOffset));
    }

    /**
     * The highest {@code y} a block can be at in this chunk, counted as a position rather than a
     * count. This is the top of the world and is the same for every chunk, so it is really a
     * question about the world rather than about the chunk it was asked of.
     * example:
     * <pre>
     * const chunk = World.getChunk(0, 0);
     * if (chunk !== null) {
     *   // the top of the world, and one below it
     *   const top = chunk.getMaxBuildHeight();
     *   Chat.log(`${top} and ${top - 1}`);
     *   // the difference between the two ends is the height
     *   Chat.log(`and the chunk is ${chunk.getHeight()} tall`);
     * }
     * </pre>
     *
     * @return the maximum height of this chunk.
     * @since 1.8.4
     */
    public int getMaxBuildHeight() {
        return base.getMaxY();
    }

    /**
     * The lowest {@code y} a block can be at in this chunk, which can be below zero on a world with
     * a deep dark world generation setting. Like {@link #getMaxBuildHeight()} this is a property of
     * the world rather than of this chunk.
     * example:
     * <pre>
     * const chunk = World.getChunk(0, 0);
     * if (chunk !== null) {
     *   Chat.log(`the world starts at y ${chunk.getMinBuildHeight()}`);
     *   // the very bottom of the chunk
     *   const floor = chunk.getOffsetBlock(0, chunk.getMinBuildHeight(), 0);
     *   const block = World.getBlock(floor);
     *   if (block !== null) {
     *     Chat.log(`and there is ${block.getId()} there`);
     *   }
     * }
     * </pre>
     *
     * @return the minimum height of this chunk.
     * @since 1.8.4
     */
    public int getMinBuildHeight() {
        return base.getMinY();
    }

    /**
     * How many blocks tall the world is, which is the distance from
     * {@link #getMaxBuildHeight()} to {@link #getMinBuildHeight()} with one added for the top
     * block being counted as well.
     * example:
     * <pre>
     * const chunk = World.getChunk(0, 0);
     * if (chunk !== null) {
     *   const min = chunk.getMinBuildHeight();
     *   const max = chunk.getMaxBuildHeight();
     *   Chat.log(`${max - min + 1} tall, and getHeight says ${chunk.getHeight()}`);
     * }
     * </pre>
     *
     * @return the height of this chunk.
     * @since 1.8.4
     */
    public int getHeight() {
        return base.getHeight();
    }

    /**
     * The first free {@code y} above the tallest block in a column of this chunk, as the heightmap
     * named sees it. Which blocks count as tall depends on the heightmap: the surface one counts
     * anything that is not air, while the motion blocking ones count what is in the way, and the
     * no-leaves one leaves leaves out.<br>
     * The {@code x} and {@code z} are offsets from the chunk's corner, not world coordinates, and
     * the answer is the free space above rather than the block itself, so a solid column with its
     * top at sixty-four answers sixty-five.
     * <p>
     * A heightmap that has never been filled in answers the bottom of the world for every column,
     * so this is only worth reading from a heightmap the chunk has actually been scanned into.
     * example:
     * <pre>
     * const chunk = World.getChunk(0, 0);
     * if (chunk !== null) {
     *   const surface = chunk.getSurfaceHeightmap();
     *   for (let z = 0; z !== 16; z += 4) {
     *     const line = [];
     *     for (let x = 0; x !== 16; x += 4) {
     *       line.push(`${x},${z}: ${chunk.getTopYAt(x, z, surface)}`);
     *     }
     *     Chat.log(line.join("  "));
     *   }
     * }
     * </pre>
     *
     * @param xOffset   the xOffset coordinate
     * @param zOffset   the zOffset coordinate
     * @param heightmap the heightmap to use
     * @return the maximum {@code y} position of all blocks inside this chunk.
     * @since 1.8.4
     */
    public int getTopYAt(int xOffset, int zOffset, Heightmap heightmap) {
        return heightmap.getFirstAvailable(xOffset, zOffset);
    }

    /**
     * The {@code x} of the chunk itself, which is the world {@code x} divided by sixteen rounded
     * down. This is the number {@code World.getChunk} takes, and it is not a world coordinate.
     * example:
     * <pre>
     * // which chunk the player is standing in
     * const player = Player.getPlayer();
     * const chunk = World.getChunk(Math.floor(player.getX() / 16), Math.floor(player.getZ() / 16));
     * if (chunk !== null) {
     *   Chat.log(`chunk ${chunk.getChunkX()}, ${chunk.getChunkZ()}`);
     *   // and the world position that works out to
     *   Chat.log(`which covers ${chunk.getStartingBlock()}`);
     * }
     * </pre>
     *
     * @return the {@code x} coordinate (not the world coordinate) of this chunk.
     * @since 1.8.4
     */
    public int getChunkX() {
        return ChunkPosCompat.x(base.getPos());
    }

    /**
     * The {@code z} of the chunk itself, which is the world {@code z} divided by sixteen rounded
     * down.
     * example:
     * <pre>
     * // a chunk position pair, and the block it starts at
     * const chunk = World.getChunk(-3, 7);
     * if (chunk !== null) {
     *   Chat.log(`${chunk.getChunkX()}, ${chunk.getChunkZ()} starts at ${chunk.getStartingBlock()}`);
     * }
     * </pre>
     *
     * @return the {@code z} coordinate (not the world coordinate) of this chunk.
     * @since 1.8.4
     */
    public int getChunkZ() {
        return ChunkPosCompat.z(base.getPos());
    }

    /**
     * The biome at a point in this chunk, as the biome's registry id. The {@code x} and {@code z} are
     * offsets from the chunk's corner, while the {@code y} is an absolute height, since a biome
     * depends on it: a cave and the surface above it can be different biomes.
     * <p>
     * The biome is read out of the world that is loaded rather than out of this chunk, so the
     * answer is the live one.
     * example:
     * <pre>
     * const chunk = World.getChunk(0, 0);
     * if (chunk !== null) {
     *   // the surface and the bottom of the same column
     *   Chat.log(`at the top: ${chunk.getBiome(8, chunk.getMaxBuildHeight() - 1, 8)}`);
     *   Chat.log(`at the bottom: ${chunk.getBiome(8, chunk.getMinBuildHeight(), 8)}`);
     * }
     * </pre>
     *
     * @param xOffset the x offset
     * @param y       the y coordinate
     * @param zOffset the z offset
     * @return the biome at the given position.
     * @since 1.8.4
     */
    @DocletReplaceReturn("Biome")
    public String getBiome(int xOffset, int y, int zOffset) {
        return Minecraft.getInstance().level.registryAccess().lookupOrThrow(Registries.BIOME).getKey(Minecraft.getInstance().level.getBiome(base.getPos().getBlockAt(xOffset, y, zOffset)).value()).toString();
    }

    /**
     * How much time players have spent in this chunk, counted up while somebody is in it. The game
     * uses it to decide how hard the chunk is, and a chunk nobody has visited stays at zero.<br>
     * The game is what moves this counter, and it moves it for the chunks players are in, so a
     * chunk nobody has been near stays where it was.
     * example:
     * <pre>
     * // the chunks around the player, and how much time each has seen
     * const player = Player.getPlayer();
     * const cx = Math.floor(player.getX() / 16);
     * const cz = Math.floor(player.getZ() / 16);
     * for (let dx = -1; dx !== 2; dx += 1) {
     *   for (let dz = -1; dz !== 2; dz += 1) {
     *     const chunk = World.getChunk(cx + dx, cz + dz);
     *     if (chunk !== null) {
     *       Chat.log(`${chunk.getChunkX()}, ${chunk.getChunkZ()}: ${chunk.getInhabitedTime()} ticks`);
     *     }
     *   }
     * }
     * </pre>
     *
     * With an increasing inhabited time, the local difficulty increases and stronger mobs will
     * spawn. Because the time is cumulative, the more players are in the chunk, the faster the time
     * will increase.
     *
     * @return the cumulative time players have spent inside this chunk.
     * @since 1.8.4
     */
    public long getInhabitedTime() {
        return base.getInhabitedTime();
    }

    /**
     * The entities in this chunk right now, read out of the world rather than out of the chunk
     * itself, so this is a snapshot of what the client currently knows about. An entity is counted
     * by the chunk it is standing in, so one that straddles a border is only in one of them.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * const chunk = World.getChunk(Math.floor(player.getX() / 16), Math.floor(player.getZ() / 16));
     * if (chunk !== null) {
     *   const entities = chunk.getEntities();
     *   Chat.log(`${entities.size()} entities in this chunk`);
     *   for (let i = 0; i !== entities.size(); i += 1) {
     *     const e = entities.get(i);
     *     Chat.log(`  ${e.getType()} at ${e.getPos()}`);
     *   }
     * }
     * </pre>
     *
     * @return all entities inside this chunk.
     * @since 1.8.4
     */
    public List<? extends EntityHelper<?>> getEntities() {
        return StreamSupport.stream(Minecraft.getInstance().level.entitiesForRendering().spliterator(), false).
                filter(entity -> entity.chunkPosition().equals(base.getPos())).map(EntityHelper::create).collect(Collectors.toList());
    }

    /**
     * The positions of the block entities in this chunk: a chest, a sign, a spawner, anything that
     * keeps data of its own. The list is of positions rather than of blocks, so what each one holds
     * is a further lookup.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * const chunk = World.getChunk(Math.floor(player.getX() / 16), Math.floor(player.getZ() / 16));
     * if (chunk !== null) {
     *   const spots = chunk.getTileEntities();
     *   Chat.log(`${spots.size()} block entities in this chunk`);
     *   for (let i = 0; i !== spots.size(); i += 1) {
     *     const block = World.getBlock(spots.get(i));
     *     if (block !== null) {
     *       Chat.log(`  ${block.getId()} at ${spots.get(i)}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return all tile entity positions inside this chunk.
     * @since 1.8.4
     */
    public List<BlockPosHelper> getTileEntities() {
        return base.getBlockEntitiesPos().stream().map(BlockPosHelper::new).collect(Collectors.toList());
    }

    /**
     * Runs a callback over every block in the chunk and gives the chunk back so calls can be
     * chained. The order is by {@code x}, then {@code z}, then {@code y}, from
     * {@link #getMinBuildHeight()} through {@link #getMaxBuildHeight()} inclusively.
     * The {@code includeAir} flag is what keeps the walk affordable, since air is
     * most of a chunk.<br>
     * The callback receives a {@link BlockDataHelper}, so the block, its position and its block
     * entity data are all reachable from what it is given. Nothing here stops the walk early: a
     * callback that has found what it wanted still has to return, and the whole chunk is read
     * either way.
     * example:
     * <pre>
     * // count the blocks of one kind in the chunk, skipping air
     * const player = Player.getPlayer();
     * const chunk = World.getChunk(Math.floor(player.getX() / 16), Math.floor(player.getZ() / 16));
     * let ores = 0;
     * if (chunk !== null) {
     *   chunk.forEach(false, JavaWrapper.methodToJava(function (block) {
     *     if (block.getId() === "minecraft:diamond_ore") {
     *       ores += 1;
     *     }
     *   }));
     *   Chat.log(`${ores} diamond ore in this chunk`);
     * }
     * </pre>
     *
     * @param includeAir whether to include air blocks or not
     * @param callback   the callback function
     * @return self for chaining.
     * @since 1.8.4
     */
    public ChunkHelper forEach(boolean includeAir, MethodWrapper<BlockDataHelper, ?, ?, ?> callback) {
        // Maybe adapt this to the WorldScanner way?
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = base.getMinY(); y <= base.getMaxY(); y++) {
                    BlockPos pos = base.getPos().getBlockAt(x, y, z);
                    BlockState state = base.getBlockState(pos);
                    if (!includeAir && state.isAir()) {
                        continue;
                    }
                    callback.accept(new BlockDataHelper(state, base.getBlockEntity(pos), pos));
                }
            }
        }
        return this;
    }

    /**
     * Whether the chunk holds at least one of the named blocks, as far as the chunk's own block
     * data can say. This is the cheap one: it walks the chunk's block storage rather than building a
     * helper for every block, and it stops at the first match, so it is the call to use as a test
     * before doing anything expensive.
     * <p>
     * The ids may be written with or without the {@code minecraft} namespace. One that is well
     * formed but names no block resolves to the air block, and that never matches, while one that is
     * not a well formed id at all is an error.
     * example:
     * <pre>
     * // is it worth scanning this chunk at all
     * const player = Player.getPlayer();
     * const chunk = World.getChunk(Math.floor(player.getX() / 16), Math.floor(player.getZ() / 16));
     * if (chunk !== null) {
     *   if (!chunk.containsAny("diamond_ore", "deepslate_diamond_ore")) {
     *     Chat.log("no diamonds in this chunk");
     *   } else {
     *     Chat.log("worth looking at");
     *   }
     * }
     * </pre>
     *
     * @param blocks the blocks to search for
     * @return {@code true} if this chunk contains at least one of the specified blocks,
     * {@code false} otherwise.
     * @since 1.8.4
     */
    @DocletReplaceParams("...blocks: JavaVarArgs<CanOmitNamespace<BlockId>>")
    public boolean containsAny(String... blocks) {
        // Don't use section.hasAny because it will take some time to update the block palette
        Set<Block> filterBlocks = Arrays.stream(blocks).map(ResourceLocation::parse).map(BuiltInRegistries.BLOCK::getValue).collect(Collectors.toSet());
        for (LevelChunkSection section : base.getSections()) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 16; y++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (filterBlocks.contains(state)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Whether the chunk holds every one of the named blocks. It walks the chunk's block storage and
     * drops each block off the list as it finds it, so it answers true as soon as the list empties
     * and answers false only once the whole chunk has been read.
     * <p>
     * The ids may be written with or without the {@code minecraft} namespace. One that is well
     * formed but names no block resolves to the air block, which any chunk satisfies because it has
     * air in it, so a mistyped id makes the answer true for the wrong reason rather than false.
     * Reading {@link BlockDataHelper#getId()} off a block is the way to check what an id really
     * names.
     * example:
     * <pre>
     * // a chunk with every one of a set of blocks in it
     * const chunk = World.getChunk(0, 0);
     * if (chunk !== null) {
     *   const wanted = ["stone", "dirt", "grass_block"];
     *   if (chunk.containsAll(wanted)) {
     *     Chat.log("all of them are here");
     *   } else if (chunk.containsAny(wanted)) {
     *     Chat.log("some of them are here");
     *   } else {
     *     Chat.log("none of them are here");
     *   }
     * }
     * </pre>
     *
     * @param blocks the blocks to search for
     * @return {@code true} if the chunk contains all the specified blocks, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    @DocletReplaceParams("...blocks: JavaVarArgs<CanOmitNamespace<BlockId>>")
    public boolean containsAll(String... blocks) {
        // Don't use section.hasAny because it will take some time to update the block palette
        Set<Block> filterBlocks = Arrays.stream(blocks).map(ResourceLocation::parse).map(BuiltInRegistries.BLOCK::getValue).collect(Collectors.toSet());
        for (LevelChunkSection section : base.getSections()) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 16; y++) {
                        BlockState state = section.getBlockState(x, y, z);
                        filterBlocks.remove(state.getBlock());
                        if (filterBlocks.isEmpty()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * The heightmaps this chunk holds, each paired with the kind it is. Only the kinds the chunk has
     * actually been scanned for are in here, so a chunk that has not been worked out may have none
     * at all, and the collection is a read-only view rather than a copy.
     * example:
     * <pre>
     * const chunk = World.getChunk(0, 0);
     * if (chunk !== null) {
     *   const maps = chunk.getHeightmaps();
     *   Chat.log(`${maps.size()} heightmaps on this chunk`);
     *   for (const entry of maps) {
     *     Chat.log(`  one of them: ${entry.getKey()}`);
     *   }
     * }
     * </pre>
     *
     * @return a map of the raw heightmap data.
     * @since 1.8.4
     */
    public Collection<Map.Entry<Heightmap.Types, Heightmap>> getHeightmaps() {
        return base.getHeightmaps();
    }

    /**
     * The chunk's surface heightmap, which counts any block that is not air. This creates the
     * heightmap if the chunk does not have it yet, and a freshly created one has never been filled
     * in, so it answers the bottom of the world for every column until the game scans it.
     * <p>
     * Feed it to {@link #getTopYAt(int, int, Heightmap)} to read a column out of it.
     * example:
     * <pre>
     * const chunk = World.getChunk(0, 0);
     * if (chunk !== null) {
     *   const surface = chunk.getSurfaceHeightmap();
     *   // the highest non-air block in the middle column, and the first free space above it
     *   const free = chunk.getTopYAt(8, 8, surface);
     *   const block = World.getBlock(chunk.getOffsetBlock(8, free - 1, 8));
     *   if (block !== null) {
     *     Chat.log(`top of the middle column is ${block.getId()} at y ${free - 1}`);
     *   }
     * }
     * </pre>
     *
     * @return the raw surface heightmap.
     * @since 1.8.4
     */
    public Heightmap getSurfaceHeightmap() {
        return base.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE);
    }

    /**
     * The chunk's ocean floor heightmap, which counts what is in the way rather than what is not
     * air, so it ignores a block that something can stand through. This creates the heightmap if the
     * chunk does not have it yet, and one that has never been filled in answers the bottom of the
     * world for every column.
     * example:
     * <pre>
     * // a terrain profile across the chunk, ignoring anything passable
     * const chunk = World.getChunk(0, 0);
     * if (chunk !== null) {
     *   const floor = chunk.getOceanFloorHeightmap();
     *   const profile = [];
     *   for (let x = 0; x !== 16; x += 1) {
     *     profile.push(chunk.getTopYAt(x, 8, floor));
     *   }
     *   Chat.log(`floor profile: ${profile.join(", ")}`);
     * }
     * </pre>
     *
     * @return the raw ocean floor heightmap.
     * @since 1.8.4
     */
    public Heightmap getOceanFloorHeightmap() {
        return base.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR);
    }

    /**
     * The chunk's motion blocking heightmap, which counts what is in the way and also counts any
     * fluid, so water has a surface here. This creates the heightmap if the chunk does not have it
     * yet, and one that has never been filled in answers the bottom of the world for every column.
     * example:
     * <pre>
     * // where something would land if it fell into this chunk
     * const chunk = World.getChunk(0, 0);
     * if (chunk !== null) {
     *   const blocking = chunk.getMotionBlockingHeightmap();
     *   for (let z = 0; z !== 16; z += 4) {
     *     Chat.log(`column ${z} lands at y ${chunk.getTopYAt(8, z, blocking)}`);
     *   }
     * }
     * </pre>
     *
     * @return the raw motion blocking heightmap.
     * @since 1.8.4
     */
    public Heightmap getMotionBlockingHeightmap() {
        return base.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING);
    }

    /**
     * The chunk's motion blocking heightmap with leaves left out, so a tree canopy does not count as
     * ground. This creates the heightmap if the chunk does not have it yet, and one that has never
     * been filled in answers the bottom of the world for every column.
     * example:
     * <pre>
     * // the ground under a forest rather than the top of the trees
     * const chunk = World.getChunk(0, 0);
     * if (chunk !== null) {
     *   const noLeaves = chunk.getMotionBlockingNoLeavesHeightmap();
     *   const withLeaves = chunk.getMotionBlockingHeightmap();
     *   for (let z = 0; z !== 16; z += 8) {
     *     Chat.log(`at ${z}: ground ${chunk.getTopYAt(8, z, noLeaves)}, `
     *       + `canopy ${chunk.getTopYAt(8, z, withLeaves)}`);
     *   }
     * }
     * </pre>
     *
     * @return the raw motion blocking heightmap without leaves.
     * @since 1.8.4
     */
    public Heightmap getMotionBlockingNoLeavesHeightmap() {
        return base.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
    }

    @Override
    public String toString() {
        return String.format("ChunkHelper:{\"x\": %d, \"z\": %d}", getChunkX(), getChunkZ());
    }

}
