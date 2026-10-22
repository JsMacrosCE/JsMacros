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
 * Provides access to a chunk. {@code World.getChunk(x, z)} takes chunk coordinates, while
 * {@link #getOffsetBlock(int, int, int)} uses local {@code x}/{@code z} offsets and absolute
 * {@code y} coordinates.
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

    /** Wraps the supplied chunk access reference without loading or looking up a chunk.
     * @param base raw chunk access to wrap
     */
    public ChunkHelper(ChunkAccess base) {
        super(base);
    }

    /**
     * Returns the world position of this chunk's local origin.
     * @return the block position at local {@code (0, 0, 0)}.
     * @since 1.8.4
     */
    public BlockPosHelper getStartingBlock() {
        return new BlockPosHelper(base.getPos().getBlockAt(0, 0, 0));
    }

    /**
     * Returns a block position from local {@code x}/{@code z} offsets and an absolute {@code y}.
     * Offsets outside {@code 0..15} on {@code x} or {@code z} produce positions in another chunk.
     * @param xOffset local x coordinate (normally {@code 0..15})
     * @param y       absolute y coordinate
     * @param zOffset local z coordinate (normally {@code 0..15})
     * @return the resulting world block position.
     * @since 1.8.4
     */
    public BlockPosHelper getOffsetBlock(int xOffset, int y, int zOffset) {
        return new BlockPosHelper(base.getPos().getBlockAt(xOffset, y, zOffset));
    }

    /**
     * Returns the chunk's upper build-height boundary.
     * @return the exclusive upper y boundary.
     * @since 1.8.4
     */
    public int getMaxBuildHeight() {
        return base.getMaxY();
    }

    /**
     * Returns the chunk's lower build-height boundary.
     * @return the inclusive lower y boundary.
     * @since 1.8.4
     */
    public int getMinBuildHeight() {
        return base.getMinY();
    }

    /**
     * Returns the chunk's build height in blocks.
     * @return the number of blocks between the lower and upper build-height boundaries.
     * @since 1.8.4
     */
    public int getHeight() {
        return base.getHeight();
    }

    /**
     * Returns the first available y value in the given heightmap column; x and z are local offsets.
     * This is the free space above the heightmap's highest counted block, not that block's y value.
     * @param xOffset   local x coordinate
     * @param zOffset   local z coordinate
     * @param heightmap heightmap to query
     * @return the first available y value for the column.
     * @since 1.8.4
     */
    public int getTopYAt(int xOffset, int zOffset, Heightmap heightmap) {
        return heightmap.getFirstAvailable(xOffset, zOffset);
    }

    /**
     * Returns this chunk's x coordinate in chunk units, as accepted by {@code World.getChunk}.
     * @return the chunk x coordinate.
     * @since 1.8.4
     */
    public int getChunkX() {
        return ChunkPosCompat.x(base.getPos());
    }

    /**
     * Returns this chunk's z coordinate in chunk units, as accepted by {@code World.getChunk}.
     * @return the chunk z coordinate.
     * @since 1.8.4
     */
    public int getChunkZ() {
        return ChunkPosCompat.z(base.getPos());
    }

    /**
     * Returns the registry id of the biome at a position in the loaded world. X and z are local
     * offsets from this chunk's origin; y is an absolute coordinate.
     * @param xOffset local x coordinate
     * @param y       absolute y coordinate
     * @param zOffset local z coordinate
     * @return the biome registry id.
     * @since 1.8.4
     */
    @DocletReplaceReturn("Biome")
    public String getBiome(int xOffset, int y, int zOffset) {
        return Minecraft.getInstance().level.registryAccess().lookupOrThrow(Registries.BIOME).getKey(Minecraft.getInstance().level.getBiome(base.getPos().getBlockAt(xOffset, y, zOffset)).value()).toString();
    }

    /**
     * Returns the chunk's cumulative inhabited time in ticks. The counter is maintained by the
     * game; reading it here does not advance it.
     *
     * @return the cumulative inhabited time, in ticks.
     * @since 1.8.4
     */
    public long getInhabitedTime() {
        return base.getInhabitedTime();
    }

    /**
     * Returns entities currently exposed by the client rendering iterable whose chunk position
     * equals this chunk's position. This is a newly collected list, not the chunk's entity storage.
     *
     * @return the collected entities whose chunk position matches this chunk.
     * @since 1.8.4
     */
    public List<? extends EntityHelper<?>> getEntities() {
        return StreamSupport.stream(Minecraft.getInstance().level.entitiesForRendering().spliterator(), false).
                filter(entity -> entity.chunkPosition().equals(base.getPos())).map(EntityHelper::create).collect(Collectors.toList());
    }

    /**
     * Returns the positions of block entities stored in this chunk.
     *
     * @return the collected block-entity positions.
     * @since 1.8.4
     */
    public List<BlockPosHelper> getTileEntities() {
        return base.getBlockEntitiesPos().stream().map(BlockPosHelper::new).collect(Collectors.toList());
    }

    /**
     * Calls {@code callback} for each included block, in local {@code x}, then {@code z}, then
     * ascending absolute {@code y} order. The y range includes both build-height bounds. The walk
     * always visits the full chunk; the callback cannot stop it early.
     *
     * @param includeAir whether to call the callback for air blocks
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
     * Tests whether the chunk contains any requested block. Block ids may omit the
     * {@code minecraft} namespace; an id absent from the registry resolves to air. The current
     * implementation compares each block state with a set of blocks, so ordinary nonempty valid
     * requests do not match and return {@code false}.
     *
     * @param blocks the blocks to search for
     * @return {@code false} for ordinary nonempty valid requests; otherwise, whether a requested
     * block was found.
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
     * Tests whether the chunk contains every requested block. Block ids may omit the
     * {@code minecraft} namespace. An id absent from the registry resolves to air, which can make
     * the result true for an unintended reason. An empty argument list also returns true.
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
     * Returns the heightmaps currently stored for this chunk as type/map entries. The returned
     * collection is unmodifiable; it reflects only maps currently present.
     *
     * @return a map of the raw heightmap data.
     * @since 1.8.4
     */
    public Collection<Map.Entry<Heightmap.Types, Heightmap>> getHeightmaps() {
        return base.getHeightmaps();
    }

    /**
     * Returns the {@code WORLD_SURFACE} heightmap, counting non-air blocks. Creates an unprimed
     * map when none exists; until populated, its columns return the chunk's minimum build height.
     * Use {@link #getTopYAt(int, int, Heightmap)} to query a column.
     *
     * @return the raw surface heightmap.
     * @since 1.8.4
     */
    public Heightmap getSurfaceHeightmap() {
        return base.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE);
    }

    /**
     * Returns the chunk's ocean-floor heightmap, which counts blocks that block motion. A new map
     * is created if needed; until populated, its columns return the chunk's minimum build height.
     *
     * @return the raw ocean floor heightmap.
     * @since 1.8.4
     */
    public Heightmap getOceanFloorHeightmap() {
        return base.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR);
    }

    /**
     * Returns the chunk's motion-blocking heightmap, which counts blocks that block motion or
     * contain fluid. A new map is created if needed; until populated, its columns return the chunk's
     * minimum build height.
     *
     * @return the raw motion blocking heightmap.
     * @since 1.8.4
     */
    public Heightmap getMotionBlockingHeightmap() {
        return base.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING);
    }

    /**
     * Returns the chunk's motion-blocking heightmap, excluding leaves blocks. A new map is created
     * if needed; until populated, its columns return the chunk's minimum build height.
     *
     * @return the raw motion blocking heightmap without leaves.
     * @since 1.8.4
     */
    public Heightmap getMotionBlockingNoLeavesHeightmap() {
        return base.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
    }

    /** Returns a diagnostic string containing this chunk's x and z chunk coordinates.
     * @return the diagnostic representation of this wrapped chunk, including its x and z chunk coordinates.
     */
    @Override
    public String toString() {
        return String.format("ChunkHelper:{\"x\": %d, \"z\": %d}", getChunkX(), getChunkZ());
    }

}
