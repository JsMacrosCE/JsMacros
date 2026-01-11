package com.jsmacrosce.jsmacros.client.api.helper.world;

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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class ChunkHelper extends BaseHelper<ChunkAccess> {

    public ChunkHelper(ChunkAccess base) {
        super(base);
    }

    /**
     * @return the first block (0 0 0 coordinate) of this chunk.
     * @since 1.8.4
     */
    public BlockPosHelper getStartingBlock() {
        return new BlockPosHelper(base.getPos().getBlockAt(0, 0, 0));
    }

    /**
     * The coordinates are relative to the starting chunk position, see
     * {@link #getStartingBlock()}.
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
     * @return the maximum height of this chunk.
     * @since 1.8.4
     */
    public int getMaxBuildHeight() {
        return base.getMaxY();
    }

    /**
     * @return the minimum height of this chunk.
     * @since 1.8.4
     */
    public int getMinBuildHeight() {
        return base.getMinY();
    }

    /**
     * @return the height of this chunk.
     * @since 1.8.4
     */
    public int getHeight() {
        return base.getHeight();
    }

    /**
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
     * @return the {@code x} coordinate (not the world coordinate) of this chunk.
     * @since 1.8.4
     */
    public int getChunkX() {
        return base.getPos().x;
    }

    /**
     * @return the {@code z} coordinate (not the world coordinate) of this chunk.
     * @since 1.8.4
     */
    public int getChunkZ() {
        return base.getPos().z;
    }

    /**
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
     * @return all entities inside this chunk.
     * @since 1.8.4
     */
    public List<? extends EntityHelper<?>> getEntities() {
        return StreamSupport.stream(Minecraft.getInstance().level.entitiesForRendering().spliterator(), false).
                filter(entity -> entity.chunkPosition().equals(base.getPos())).map(EntityHelper::create).collect(Collectors.toList());
    }

    /**
     * @return all tile entity positions inside this chunk.
     * @since 1.8.4
     */
    public List<BlockPosHelper> getTileEntities() {
        return base.getBlockEntitiesPos().stream().map(BlockPosHelper::new).collect(Collectors.toList());
    }

    /**
     * @param includeAir whether to include air blocks or not
     * @param callback   the callback function
     * @return self for chaining.
     * @since 1.8.4
     */
    public ChunkHelper forEach(boolean includeAir, MethodWrapper<BlockDataHelper, ?, ?, ?> callback) {
        // Maybe adapt this to the WorldScanner way?
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = base.getMinY(); y < base.getMaxY(); y++) {
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
     * @return a map of the raw heightmap data.
     * @since 1.8.4
     */
    public Collection<Map.Entry<Heightmap.Types, Heightmap>> getHeightmaps() {
        return base.getHeightmaps();
    }

    /**
     * @return the raw surface heightmap.
     * @since 1.8.4
     */
    public Heightmap getSurfaceHeightmap() {
        return base.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE);
    }

    /**
     * @return the raw ocean floor heightmap.
     * @since 1.8.4
     */
    public Heightmap getOceanFloorHeightmap() {
        return base.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR);
    }

    /**
     * @return the raw motion blocking heightmap.
     * @since 1.8.4
     */
    public Heightmap getMotionBlockingHeightmap() {
        return base.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING);
    }

    /**
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
