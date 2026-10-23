package com.jsmacrosce.jsmacros.client.api.library.impl;

import com.jsmacrosce.jsmacros.client.util.GuiCompat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=26.1 {
/*import net.minecraft.core.Holder;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.timeline.Timeline;
*///?}
import org.apache.logging.log4j.core.jmx.Server;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReadOnly;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.doclet.DocletReplaceTypeParams;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.JsMacros;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.access.IPlayerListHud;
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.WorldScanner;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.WorldScannerBuilder;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.*;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.BossBarHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.PlayerEntityHelper;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;
import com.jsmacrosce.jsmacros.core.library.Library;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Functions for getting and using world data.
 * <p>
 * An instance of this class is passed to scripts as the {@code World} variable.
 * <br>
 * The client-side {@code World} library exposes the currently loaded world. Results reflect
 * client state, and methods document their own null or sentinel behavior. Block-search methods
 * inspect loaded chunks; overloads distinguish chunk coordinates from block positions.
 * <pre>
 * // the world has to be loaded before anything here says anything useful
 * if (World.isWorldLoaded()) {
 *   // what is the time and where are we
 *   Chat.log(`${World.getDimension()} at ${World.getTime()}, biome ${World.getBiome()}`);
 *
 *   // what is under the player
 *   const player = Player.getPlayer();
 *   if (player !== null) {
 *     const block = World.getBlock(player.getBlockPos().down());
 *     if (block !== null) {
 *       Chat.log(`standing on ${block.getName()}`);
 *     }
 *   }
 *
 *   // find diamond ore in the loaded chunks around the player
 *   const found = World.findBlocksMatching("diamond_ore", 4);
 *   if (found !== null) {
 *     for (const pos of found) {
 *       Chat.log(`diamond ore at ${pos.getX()}, ${pos.getY()}, ${pos.getZ()}`);
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Wagyourtail
 */
@Library("World")
@SuppressWarnings("unused")
public class FWorld extends BaseLibrary {

    private static final Minecraft mc = Minecraft.getInstance();
    /**
     * The latest server TPS estimate, calculated from server time updates.
     * Initialized to 20; retains the last estimate until another update is measured.
     *
     * @since 1.2.7
     */
    @DocletReadOnly
    public static double serverInstantTPS = 20;
    /**
     * The server's estimated TPS averaged over the last minute.
     *
     * @since 1.2.7
     */
    @DocletReadOnly
    public static double server1MAverageTPS = 20;
    /**
     * The server's estimated TPS averaged over the last five minutes, using one-minute averages.
     *
     * @since 1.2.7
     */
    @DocletReadOnly
    public static double server5MAverageTPS = 20;
    /**
     * The server's estimated TPS averaged over the last fifteen minutes, using five-minute averages.
     *
     * @since 1.2.7
     */
    @DocletReadOnly
    public static double server15MAverageTPS = 20;

    /**
     * Initializes this world library with its associated core.
     * @param runner core associated with this library
     */
    public FWorld(Core<?, ?> runner) {
        super(runner);
    }

    /**
     * Returns whether the client currently has a world loaded.
     *
     * @return {@code true} if a world is currently loaded, {@code false} otherwise
     * @since 1.3.0
     */
    public boolean isWorldLoaded() {
        return mc.level != null;
    }

    /**
     * Returns wrappers for player entities currently present in the client world. Unlike
     * {@link #getPlayers()}, this does not include tab-list entries for players without a
     * client-side entity.
     *
     * @return current client-world player entities, or {@code null} if no world is loaded
     */
    @Nullable
    public List<PlayerEntityHelper<Player>> getLoadedPlayers() {
        ClientLevel world = mc.level;
        if (world == null) return null;
        List<PlayerEntityHelper<Player>> players = new ArrayList<>();
        for (AbstractClientPlayer p : ImmutableList.copyOf(world.players())) {
            players.add(new PlayerEntityHelper<>(p));
        }
        return players;
    }

    /**
     * Returns a snapshot of the connection's online-player entries. Unlike
     * {@link #getLoadedPlayers()}, entries need not have a client-side entity; returns
     * {@code null} when there is no connection.
     *
     * @return online-player entries, or {@code null} if no connection is available
     */
    @Nullable
    public List<PlayerListEntryHelper> getPlayers() {
        ClientPacketListener handler = mc.getConnection();
        if (handler == null) return null;
        List<PlayerListEntryHelper> players = new ArrayList<>();
        for (PlayerInfo p : ImmutableList.copyOf(handler.getOnlinePlayers())) {
            players.add(new PlayerListEntryHelper(p));
        }
        return players;
    }

    /**
     * Returns the matching online-player entry, or {@code null} when there is no connection
     * or no matching name. See {@link #getPlayers()} for the entry list.
     *
     * @param name the name of the player to get the entry for
     * @return player entry for the given player's name or {@code null} if not found.
     * @since 1.8.4
     */
    @Nullable
    public PlayerListEntryHelper getPlayerEntry(String name) {
        ClientPacketListener handler = mc.getConnection();
        if (handler == null) return null;
        PlayerInfo entry = handler.getPlayerInfo(name);
        return entry != null ? new PlayerListEntryHelper(entry) : null;
    }

    /**
     * Returns the block data at block coordinates. A {@code null} result means the position
     * resolves to void air or no world is loaded; ordinary air still returns a helper.
     *
     * @param x the x block coordinate
     * @param y the y block coordinate
     * @param z the z block coordinate
     * @return the block data, or {@code null} for void air or no loaded world
     */
    @Nullable
    public BlockDataHelper getBlock(int x, int y, int z) {
        ClientLevel world = mc.level;
        if (world == null) return null;
        BlockPos bp = new BlockPos(x, y, z);
        BlockState b = world.getBlockState(bp);
        BlockEntity t = world.getBlockEntity(bp);
        if (b.getBlock().equals(Blocks.VOID_AIR)) {
            return null;
        }
        return new BlockDataHelper(b, t, bp);
    }

    /**
     * Returns the block containing {@code pos}, flooring each coordinate before lookup.
     * A {@code null} result has the same meaning as in {@link #getBlock(int, int, int)}.
     *
     * @param pos the point in the world, whose coordinates are rounded down to a block
     * @return The block that point falls inside, or {@code null} if the client has no data for it
     * @since 1.8.4
     */
    @Nullable
    public BlockDataHelper getBlock(Pos3D pos) {
        return getBlock((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));
    }

    /**
     * Returns the block data at {@code pos}; a {@code null} result has the same meaning as in
     * {@link #getBlock(int, int, int)}.
     *
     * @param pos the block position to look up
     * @return The block at that position, or {@code null} if the client has no data for it
     * @since 1.8.4
     */
    @Nullable
    public BlockDataHelper getBlock(BlockPosHelper pos) {
        return getBlock(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Returns the chunk at chunk coordinates {@code x}, {@code z}; these are not block coordinates.
     * Block coordinates map to chunks by shifting right four bits: {@code 100 >> 4} is chunk
     * {@code 6} (blocks {@code 96}–{@code 111}), while {@code -1 >> 4} is chunk {@code -1}.
     * The result is {@code null} if no world is loaded. A missing client chunk produces a
     * helper wrapping the client's empty-chunk placeholder, not generated terrain; check
     * {@link #isChunkLoaded(int, int)} first when you need an already-loaded chunk.
     * <pre>
     * // Convert block coordinates to chunk coordinates, including for negative positions.
     * const x = -1 >> 4;
     * const z = 100 >> 4;
     * if (World.isChunkLoaded(x, z)) {
     *   const chunk = World.getChunk(x, z);
     *   if (chunk !== null) Chat.log(`chunk ${x}, ${z} is available`);
     * }
     * </pre>
     *
     * @param x the x coordinate of the chunk, not the absolute position
     * @param z the z coordinate of the chunk, not the absolute position
     * @return the chunk helper, or {@code null} if no world is loaded
     * @since 1.8.4
     */
    @Nullable
    public ChunkHelper getChunk(int x, int z) {
        ClientLevel world = mc.level;
        if (world == null) return null;
        return new ChunkHelper(world.getChunk(x, z));
    }

    /**
     * Returns a builder for a world scanner. The example builds a scanner matching blocks
     * facing south that do not require a tool, have hardness at most {@code 10}, and whose
     * string representation contains {@code chest} or {@code barrel}; building does not scan.
     * <pre>
     * World.getWorldScanner()
     *     .withBlockFilter("getHardness").is("<=", 10)
     *     .andStringBlockFilter().contains("chest", "barrel")
     *     .withStringStateFilter().contains("facing=south")
     *     .andStateFilter("isToolRequired").is(false)
     *     .build()
     * </pre>
     *
     * @return a builder to create a WorldScanner.
     * @since 1.6.5
     */
    @DocletReplaceReturn("TypedWorldScannerBuilder.Initial")
    public WorldScannerBuilder getWorldScanner() {
        return new WorldScannerBuilder();
    }

    /**
     * a scanner over the current world, from two filter functions.
     * <p>
     * This is the free form of the scanner {@link #getWorldScanner()} builds, and it is the one
     * to reach for when a filter is a real computation rather than a chain of
     * {@code equals} and {@code contains} checks. The block filter is called for every block in
     * range and keeps the ones it returns {@code true} for; the state filter is called for the
     * state of each of those and can narrow it further, and either may be {@code null} to skip
     * that stage.<br>
     * The filters are called a great many times, once per block rather than once per match, so
     * a slow one makes the scan slow. The scanner uses parallel streams only when both
     * filters support parallel execution; those filters must not access the world unsafely.
     * <pre class="language-typescript">
     * // every loaded block that is solid and is not dirt. naming the parameter type
     * // is what tells the wrapper what it is being handed
     * const scanner = World.getWorldScanner(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() !== "minecraft:dirt";
     *   }),
     *   JavaWrapper.methodToJava(function (state: BlockStateHelper) {
     *     return state.isSolid();
     *   })
     * );
     * if (scanner !== null) {
     *   const found = scanner.scanAroundPlayer(2);
     *   Chat.log(`${found.size()} blocks matched`);
     * }
     * </pre>
     *
     * @param blockFilter called for each block, keeping the ones it returns true for, or
     *                    {@code null} to keep all of them
     * @param stateFilter called for the state of each block the first filter kept, keeping the
     *                    ones it returns true for, or {@code null} to keep all of those
     * @return a scanner for the current world, or {@code null} if no world is loaded
     * @since 1.6.5
     */
    @Nullable
    public WorldScanner getWorldScanner(@Nullable MethodWrapper<BlockHelper, Object, Boolean, ?> blockFilter, @Nullable MethodWrapper<BlockStateHelper, Object, Boolean, ?> stateFilter) {
        ClientLevel world = mc.level;
        if (world == null) return null;
        return new WorldScanner(world, blockFilter, stateFilter);
    }

    /**
     * Finds positions of a block ID in the square chunk range centered at {@code centerX, centerZ}.
     * These are chunk coordinates; a range of {@code 1} covers a 3-by-3 chunk square. Only
     * loaded chunks are scanned, examining their block positions; large ranges can be expensive.
     * IDs may omit the {@code minecraft:} namespace. A negative range throws
     * {@link IllegalArgumentException}.
     *
     * @param centerX the x coordinate of the centre chunk, not the block position
     * @param centerZ the z coordinate of the centre chunk, not the block position
     * @param id the block id to look for, with or without the minecraft namespace
     * @param chunkrange how many chunks out from the centre to search, so one is a three by three
     * @return the positions of every matching block, or {@code null} if no world is loaded
     * @since 1.6.4
     */
    @Nullable
    @DocletReplaceParams("centerX: int, centerZ: int, id: CanOmitNamespace<BlockId>, chunkrange: int")
    public List<Pos3D> findBlocksMatching(int centerX, int centerZ, String id, int chunkrange) {
        ClientLevel world = mc.level;
        if (world == null) return null;
        String finalId = RegistryHelper.parseNameSpace(id);
        return new WorldScanner(world, block -> BuiltInRegistries.BLOCK.getKey(block.getRaw()).toString().equals(finalId), null).scanChunkRange(centerX, centerZ, chunkrange);
    }

    /**
     * Finds positions of a block ID in the square chunk range centered on the player. Returns
     * {@code null} if there is no world or player. IDs may omit the {@code minecraft:} namespace;
     * only loaded chunks are scanned. A negative range throws {@link IllegalArgumentException}.
     *
     * @param id the block id to look for, with or without the minecraft namespace
     * @param chunkrange how many chunks out from the player's chunk to search
     * @return the positions of every matching block, or {@code null} if there is no world or player
     * @since 1.6.4
     */
    @Nullable
    @DocletReplaceParams("id: CanOmitNamespace<BlockId>, chunkrange: int")
    public List<Pos3D> findBlocksMatching(String id, int chunkrange) {
        ClientLevel world = mc.level;
        LocalPlayer player = mc.player;
        if (world == null || player == null) return null;
        String finalId = RegistryHelper.parseNameSpace(id);
        int playerChunkX = player.getBlockX() >> 4;
        int playerChunkZ = player.getBlockZ() >> 4;
        return new WorldScanner(world, block -> BuiltInRegistries.BLOCK.getKey(block.getRaw()).toString().equals(finalId), null).scanChunkRange(playerChunkX, playerChunkZ, chunkrange);
    }

    /**
     * Finds positions whose block ID matches any ID in {@code ids}, centered on the player.
     * IDs may omit the {@code minecraft:} namespace; duplicates do not duplicate results.
     * Returns {@code null} if there is no world or player. Only loaded chunks are scanned; a
     * negative range throws {@link IllegalArgumentException}.
     *
     * @param ids the block ids to look for, with or without the minecraft namespace
     * @param chunkrange how many chunks out from the player's chunk to search
     * @return matching positions, or {@code null} if there is no world or player
     * @since 1.6.4
     */
    @Nullable
    @DocletReplaceParams("ids: CanOmitNamespace<BlockId>[], chunkrange: int")
    public List<Pos3D> findBlocksMatching(String[] ids, int chunkrange) {
        ClientLevel world = mc.level;
        LocalPlayer player = mc.player;
        if (world == null || player == null) return null;
        int playerChunkX = player.getBlockX() >> 4;
        int playerChunkZ = player.getBlockZ() >> 4;
        Set<String> ids2 = Arrays.stream(ids).map(RegistryHelper::parseNameSpace).collect(Collectors.toUnmodifiableSet());
        return new WorldScanner(world, block -> ids2.contains(BuiltInRegistries.BLOCK.getKey(block.getRaw()).toString()), null).scanChunkRange(playerChunkX, playerChunkZ, chunkrange);
    }

    /**
     * Finds positions whose block ID matches any ID in {@code ids}, centered at chunk
     * coordinates {@code centerX, centerZ}. IDs may omit the namespace; duplicates do not
     * duplicate results. Only loaded chunks are scanned; a negative range throws
     * {@link IllegalArgumentException}.
     *
     * @param centerX the x coordinate of the centre chunk, not the block position
     * @param centerZ the z coordinate of the centre chunk, not the block position
     * @param ids the block ids to look for, with or without the minecraft namespace
     * @param chunkrange how many chunks out from the centre to search
     * @return the positions of every matching block, or {@code null} if no world is loaded
     * @since 1.6.4
     */
    @Nullable
    @DocletReplaceParams("centerX: int, centerZ: int, ids: CanOmitNamespace<BlockId>[], chunkrange: int")
    public List<Pos3D> findBlocksMatching(int centerX, int centerZ, String[] ids, int chunkrange) {
        ClientLevel world = mc.level;
        if (world == null) return null;
        Set<String> ids2 = Arrays.stream(ids).map(RegistryHelper::parseNameSpace).collect(Collectors.toUnmodifiableSet());
        return new WorldScanner(world, block -> ids2.contains(BuiltInRegistries.BLOCK.getKey(block.getRaw()).toString()), null).scanChunkRange(centerX, centerZ, chunkrange);
    }

    /**
     * Searches around the player using a required block predicate and an optional state
     * predicate. The callbacks receive {@link BlockHelper} and {@link BlockStateHelper},
     * respectively; the state predicate runs only after the block predicate accepts a block.
     * Pass {@code null} to omit the state stage. Results are cached by block state, so callbacks
     * need not run once per position. A negative range throws {@link IllegalArgumentException};
     * returns {@code null} if there is no world or player.
     * <pre>
     * const found = World.findBlocksMatching(
     *   JavaWrapper.methodToJava(function (block) {
     *     return block.getId() === "minecraft:chest";
     *   }),
     *   null,
     *   1
     * );
     * </pre>
     *
     * @param blockFilter required predicate selecting blocks; results may be reused for matching states
     * @param stateFilter optional predicate selecting states accepted by the block filter, or {@code null}
     * @param chunkrange how many chunks out from the player's chunk to search
     * @return the positions of every matching block, or {@code null} if there is no player or world
     * @throws IllegalArgumentException if the block filter is {@code null}
     * @since 1.6.4
     */
    @Nullable
    public List<Pos3D> findBlocksMatching(MethodWrapper<BlockHelper, Object, Boolean, ?> blockFilter, @Nullable MethodWrapper<BlockStateHelper, Object, Boolean, ?> stateFilter, int chunkrange) {
        if (blockFilter == null) {
            throw new IllegalArgumentException("idFilter cannot be null");
        }
        LocalPlayer player = mc.player;
        if (player == null) return null;
        int playerChunkX = player.getBlockX() >> 4;
        int playerChunkZ = player.getBlockZ() >> 4;
        return findBlocksMatching(playerChunkX, playerChunkZ, blockFilter, stateFilter, chunkrange);
    }

    /**
     * Searches around chunk coordinates {@code chunkX, chunkZ} using a required block predicate
     * and an optional state predicate. The callbacks receive {@link BlockHelper} and
     * {@link BlockStateHelper}; the state predicate runs only after the block predicate accepts
     * a block. Pass {@code null} to omit that stage. Results are cached by block state. Returns
     * {@code null} if no world is loaded; a negative range throws {@link IllegalArgumentException}.
     *
     * @param chunkX the x coordinate of the centre chunk, not the block position
     * @param chunkZ the z coordinate of the centre chunk, not the block position
     * @param blockFilter required predicate selecting blocks; results may be reused for matching states
     * @param stateFilter optional predicate selecting states accepted by the block filter, or {@code null}
     * @param chunkrange how many chunks out from the centre to search
     * @return the positions of every matching block, or {@code null} if no world is loaded
     * @throws IllegalArgumentException if the block filter is {@code null}
     * @since 1.6.4
     */
    @Nullable
    public List<Pos3D> findBlocksMatching(int chunkX, int chunkZ, MethodWrapper<BlockHelper, Object, Boolean, ?> blockFilter, @Nullable MethodWrapper<BlockStateHelper, Object, Boolean, ?> stateFilter, int chunkrange) {
        if (blockFilter == null) {
            throw new IllegalArgumentException("block filter cannot be null");
        }
        ClientLevel world = mc.level;
        if (world == null) return null;
        return new WorldScanner(world, blockFilter, stateFilter).scanChunkRange(chunkX, chunkZ, chunkrange);
    }

    /**
     * By default, air blocks are ignored and the callback is only called for real blocks.
     * <p>
     * This walks a sphere of block positions around a centre and calls back for each one that
     * is not air. The shape is a sphere measured in whole blocks, so the callback is made for
     * every position inside the radius rather than for a sphere surface, and the vertical
     * extent is clamped to the world's own build limits so the walk does not run off the top or
     * the bottom.<br>
     * This is the form for "everything solid near here" where the answer is short and a list
     * would be wasteful. The callback is called once per block position, so it is the
     * per-block work that costs. Callbacks run synchronously on the calling thread.
     * <pre>
     * // count the solid blocks in a five block bubble around the player
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   let count = 0;
     *   World.iterateSphere(player.getBlockPos(), 5, JavaWrapper.methodToJava(function (block) {
     *     if (block.getBlockStateHelper().isSolid()) count++;
     *   }));
     *   Chat.log(`${count} solid blocks within five blocks`);
     * }
     * </pre>
     *
     * @param pos      the center position
     * @param radius   the radius to scan
     * @param callback the callback to call for each block
     * @since 1.8.4
     */
    public void iterateSphere(BlockPosHelper pos, int radius, MethodWrapper<BlockDataHelper, ?, ?, ?> callback) {
        iterateSphere(pos, radius, true, callback);
    }

    /**
     * Visits positions accepted by a squared-distance test around {@code pos}. Bounds and the
     * distance test use integer arithmetic, which can overflow for large radii or coordinates.
     * The current y bounds use {@code clamp(minY, centerY - radius, height)} and
     * {@code clamp(minY, centerY + radius, height)}; they do not guarantee positions within the
     * world's build-height limits. With {@code ignoreAir} true, air positions are skipped.
     * The callback runs synchronously on the calling thread; with no world loaded there are
     * no calls. A negative radius throws {@link IllegalArgumentException}.
     *
     * @param pos       the center position
     * @param radius    the radius to scan, which must not be negative
     * @param ignoreAir whether to ignore air blocks
     * @param callback  the callback to call for each block
     * @throws IllegalArgumentException if the radius is negative
     * @since 1.8.4
     */
    public void iterateSphere(BlockPosHelper pos, int radius, boolean ignoreAir, MethodWrapper<BlockDataHelper, ?, ?, ?> callback) {
        if (radius < 0) {
            throw new IllegalArgumentException("radius cannot be negative");
        }
        ClientLevel world = mc.level;
        if (world == null) return;
        int xStart = pos.getX() - radius;
        int yStart = Mth.clamp(world.getMinY(), pos.getY() - radius, world.getHeight());
        int zStart = pos.getZ() - radius;
        int xEnd = pos.getX() + radius;
        int yEnd = Mth.clamp(world.getMinY(), pos.getY() + radius, world.getHeight());
        int zEnd = pos.getZ() + radius;
        int radiusSq = radius * radius;

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        for (int x = xStart; x <= xEnd; x++) {
            int dx = x - pos.getX();
            for (int y = yStart; y <= yEnd; y++) {
                int dy = y - pos.getY();
                for (int z = zStart; z <= zEnd; z++) {
                    int dz = z - pos.getZ();
                    blockPos.set(x, y, z);
                    BlockState state = world.getBlockState(blockPos);
                    if (ignoreAir && state.isAir()) {
                        continue;
                    }
                    if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                        callback.accept(new BlockDataHelper(state, world.getBlockEntity(blockPos), blockPos));
                    }
                }
            }
        }
    }

    /**
     * walk a cuboid of block positions, ignoring air.
     * <p>
     * This is the box shaped counterpart to {@link #iterateSphere(BlockPosHelper, int,
     * MethodWrapper)}, and it includes both corners, so a box from one position to the next
     * covers every position between them as well. The two corners are given in either order
     * and the walk covers the space between them, so it does not matter which is which.<br>
     * The callback is called synchronously on the calling thread for each non-air position.
     * <pre>
     * // what is in the six blocks directly above the player
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const base = player.getBlockPos();
     *   World.iterateBox(base.up(), base.up(6), JavaWrapper.methodToJava(function (block) {
     *     Chat.log(block.getName());
     *   }));
     * }
     * </pre>
     *
     * @param pos1     the first position
     * @param pos2     the second position
     * @param callback the callback to call for each block
     * @since 1.8.4
     */
    public void iterateBox(BlockPosHelper pos1, BlockPosHelper pos2, MethodWrapper<BlockDataHelper, ?, ?, ?> callback) {
        iterateBox(pos1, pos2, true, callback);
    }

    /**
     * Visits positions between the corners, including air only when {@code ignoreAir} is false.
     * The callback runs synchronously on the calling thread; with no world loaded there are no
     * calls. Each corner's y is computed as {@code clamp(minY, cornerY, height)} in the current
     * implementation, not as a clamp of {@code cornerY} to the build-height range; visited
     * positions are not guaranteed to lie within that range.
     *
     * @param pos1      the first position
     * @param pos2      the second position
     * @param ignoreAir whether to ignore air blocks
     * @param callback  the callback to call for each block
     * @since 1.8.4
     */
    public void iterateBox(BlockPosHelper pos1, BlockPosHelper pos2, boolean ignoreAir, MethodWrapper<BlockDataHelper, ?, ?, ?> callback) {
        ClientLevel world = mc.level;
        if (world == null) return;
        BlockPos.betweenClosedStream(pos1.getRaw().atY(Mth.clamp(world.getMinY(), pos1.getY(), world.getHeight())), pos2.getRaw().atY(Mth.clamp(world.getMinY(), pos2.getY(), world.getHeight()))).forEach(bp -> {
            BlockState state = world.getBlockState(bp);
            if (ignoreAir && state.isAir()) {
                return;
            }
            callback.accept(new BlockDataHelper(state, world.getBlockEntity(bp), bp));
        });
    }

    /**
     * Returns the current client world's scoreboard, including its objectives, display slots,
     * and teams. The client state reflects scoreboard updates it has received; the result is
     * {@code null} when no world is loaded.
     * <pre>
     * const boards = World.getScoreboards();
     * if (boards !== null) {
     *   const current = boards.getCurrentScoreboard();
     *   if (current !== null) {
     *     Chat.log(`sidebar objective is ${current.getName()}`);
     *   }
     * }
     * </pre>
     *
     * @return the current scoreboard, or {@code null} if no world is loaded
     * @since 1.2.9
     */
    @Nullable
    public ScoreboardsHelper getScoreboards() {
        ClientLevel world = mc.level;
        if (world == null) return null;
        return new ScoreboardsHelper(world.getScoreboard());
    }

    /**
     * Returns a snapshot of entities currently available to the client; it is {@code null} when
     * no world is loaded. Use {@link #getEntities(String...)} to filter by type,
     * {@link #getEntities(double)} to filter by player distance, or
     * {@link #getEntities(MethodWrapper)} for a custom filter.
     * <pre>
     * const entities = World.getEntities();
     * if (entities !== null) {
     *   Chat.log(`${entities.size()} entities available to the client`);
     * }
     * </pre>
     *
     * @return a snapshot of available entities, or {@code null} if no world is loaded
     */
    @Nullable
    public List<EntityHelper<?>> getEntities() {
        return getEntitiesInternal(entity -> true);
    }

    /**
     * Returns entities from the client's rendering collection whose type matches any supplied
     * ID. IDs may omit the {@code minecraft:} namespace; returns {@code null} without a world.
     *
     * @param types the entity types to consider, with or without the minecraft namespace
     * @return matching entities in the client's rendering collection, or {@code null} if no world is loaded
     * @since 1.8.4
     */
    @Nullable
    @DocletReplaceTypeParams("E extends CanOmitNamespace<EntityId>")
    @DocletReplaceParams("...types: JavaVarArgs<E>")
    @DocletReplaceReturn("JavaList<EntityTypeFromId<E>> | null")
    public List<EntityHelper<?>> getEntities(String... types) {
        Set<String> uniqueTypes = Arrays.stream(types).map(RegistryHelper::parseNameSpace).collect(Collectors.toUnmodifiableSet());
        Predicate<Entity> typePredicate = entity -> uniqueTypes.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        return getEntitiesInternal(typePredicate);
    }

    /**
     * Returns entities from the client's rendering collection whose distance from the player
     * is at most {@code distance}; negative values match none. Returns {@code null} if no player
     * or world is available.
     *
     * @param distance the maximum distance to search for entities
     * @return matching entities in the client's rendering collection, or {@code null} if no player or world is available
     * @since 1.8.4
     */
    @Nullable
    public List<EntityHelper<?>> getEntities(double distance) {
        LocalPlayer player = mc.player;
        if (player == null) return null;
        Predicate<Entity> distancePredicate = e -> e.distanceTo(player) <= distance;
        return getEntitiesInternal(distancePredicate);
    }

    /**
     * Returns rendering-collection entities that match any supplied type ID and whose distance
     * from the player is at most {@code distance}. IDs may omit the namespace. Returns
     * {@code null} if no player or world is available.
     *
     * @param distance the maximum distance to search for entities
     * @param types    the entity types to consider, with or without the minecraft namespace
     * @return matching entities in the client's rendering collection, or {@code null} if no player or world is available
     * @since 1.8.4
     */
    @Nullable
    @DocletReplaceTypeParams("E extends CanOmitNamespace<EntityId>")
    @DocletReplaceParams("distance: double, ...types: JavaVarArgs<E>")
    @DocletReplaceReturn("JavaList<EntityTypeFromId<E>> | null")
    public List<EntityHelper<?>> getEntities(double distance, String... types) {
        LocalPlayer player = mc.player;
        if (player == null) return null;
        Set<String> uniqueTypes = Arrays.stream(types).map(RegistryHelper::parseNameSpace).collect(Collectors.toUnmodifiableSet());
        Predicate<Entity> distancePredicate = e -> e.distanceTo(player) <= distance;
        Predicate<Entity> typePredicate = entity -> uniqueTypes.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        return getEntitiesInternal(distancePredicate.and(typePredicate));
    }

    /**
     * Applies {@code filter} to wrappers for entities in the client's rendering collection and
     * returns those accepted. Returns {@code null} without a world. The result list is newly
     * assembled, but its helpers wrap live entities.
     *
     * @param filter the entity filter
     * @return matching entities, or {@code null} if no world is loaded
     * @since 1.8.4
     */
    @Nullable
    public List<EntityHelper<?>> getEntities(MethodWrapper<EntityHelper<?>, ?, ?, ?> filter) {
        ClientLevel world = mc.level;
        if (world == null) return null;
        List<EntityHelper<?>> entities = new ArrayList<>();
        for (Entity e : tryCopyEntities(world, 0)) {
            EntityHelper<?> entity = EntityHelper.create(e);
            if (filter.test(entity)) {
                entities.add(entity);
            }
        }
        return entities;
    }

    @Nullable
    private List<EntityHelper<?>> getEntitiesInternal(Predicate<Entity> filter) {
        ClientLevel world = mc.level;
        if (world == null) return null;
        List<EntityHelper<?>> entities = new ArrayList<>();
        for (Entity e : tryCopyEntities(world, 0)) {
            if (filter.test(e)) {
                entities.add(EntityHelper.create(e));
            }
        }
        return entities;
    }

    /**
     * @param tried always input 0 please. don't want to create an overload for private method.
     */
    private List<Entity> tryCopyEntities(ClientLevel world, int tried) {
        try {
            List<Entity> list = ImmutableList.copyOf(world.entitiesForRendering());
            if (tried != 0) {
                JsMacros.LOGGER.warn("FWorld.tryCopyEntities() did {} tries", tried + 1);
            }
            return list;
        } catch (NullPointerException e) {
            if (tried > 40) throw e; // give up, I wouldn't imagine that it exceeds 40 times
            return tryCopyEntities(world, tried + 1);
        }
    }

    /**
     * Clips from the start point to the end point against block collider shapes. {@code fluid}
     * selects whether fluids are considered; a miss returns {@code null}. Returns {@code null}
     * if either the world or local player is unavailable (the player supplies the clip context).
     *
     * @param x1 the x coordinate of the start of the line
     * @param y1 the y coordinate of the start of the line
     * @param z1 the z coordinate of the start of the line
     * @param x2 the x coordinate of the end of the line
     * @param y2 the y coordinate of the end of the line
     * @param z2 the z coordinate of the end of the line
     * @param fluid whether a liquid in the way counts as the hit
     * @return block data at the first collider or selected fluid hit, or {@code null} on a miss or without a world/player
     * @since 1.6.5
     */
    @Nullable
    public BlockDataHelper rayTraceBlock(double x1, double y1, double z1, double x2, double y2, double z2, boolean fluid) {
        ClientLevel world = mc.level;
        LocalPlayer player = mc.player;
        if (world == null || player == null) return null;
        BlockHitResult result = world.clip(new ClipContext(new Vec3(x1, y1, z1), new Vec3(x2, y2, z2), ClipContext.Block.COLLIDER, fluid ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, player));
        if (result.getType() != BlockHitResult.Type.MISS) {
            return new BlockDataHelper(world.getBlockState(result.getBlockPos()), world.getBlockEntity(result.getBlockPos()), result.getBlockPos());
        }
        return null;
    }

    /**
     * Returns the living entity whose hitbox intersects the segment and is nearest the player;
     * this need not be the first entity from the segment's start. Non-living entities are ignored.
     * Returns {@code null} if no world is loaded or no eligible entity intersects the segment.
     * Requires a local player for the distance comparison when a world is loaded.
     * <pre>
     * // is there a mob between the player and a point ten blocks ahead?
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const eyes = player.getEyePos();
     *   const end = eyes.add(10, 0, 0);
     *   const mob = World.rayTraceEntity(
     *     eyes.getX(), eyes.getY(), eyes.getZ(),
     *     end.getX(), end.getY(), end.getZ()
     *   );
     *   Chat.log(mob === null ? "nothing in the way" : `${mob.getName()} is in the way`);
     * }
     * </pre>
     *
     * @param x1 the x coordinate of the start of the line
     * @param y1 the y coordinate of the start of the line
     * @param z1 the z coordinate of the start of the line
     * @param x2 the x coordinate of the end of the line
     * @param y2 the y coordinate of the end of the line
     * @param z2 the z coordinate of the end of the line
     * @return a living entity whose hitbox the line passes through, or {@code null} if none does
     * @since 1.8.3
     */
    @Nullable
    public EntityHelper<?> rayTraceEntity(double x1, double y1, double z1, double x2, double y2, double z2) {
        ClientLevel level = mc.level;
        if (level == null) return null;
        TargetingConditions target = TargetingConditions.forNonCombat();
        target.selector((e, w) -> e.getBoundingBox().clip(new Vec3(x1, y1, z1), new Vec3(x2, y2, z2)).isPresent());
        List<LivingEntity> entities = (List) StreamSupport.stream(level.entitiesForRendering().spliterator(), false).filter(e -> e instanceof LivingEntity).collect(Collectors.toList());
        LivingEntity closest = null;
        double distance = -1;
        Player tester = mc.player;
        for (LivingEntity e : entities) {
            if (target.test(null, tester, e)) {
                double d = e.distanceToSqr(tester);
                if (distance == -1 || d < distance) {
                    closest = e;
                    distance = d;
                }
            }
        }
        if (closest != null) {
            return EntityHelper.create(closest);
        }
        return null;
    }

    /**
     * Returns the current dimension identifier as a string, or {@code null} if no world is loaded.
     *
     * @return the current dimension, or {@code null} if no world is loaded
     * @since 1.1.2
     */
    @Nullable
    @DocletReplaceReturn("Dimension | null")
    public String getDimension() {
        ClientLevel world = mc.level;
        if (world == null) return null;
        return world.dimension()
                //? if >=1.21.11 {
                /*.identifier()
                *///? } else {
                .location()
                //? }
                .toString();
    }

    /**
     * Returns the registry ID of the biome at the player's block position, or {@code null} if
     * there is no world, player, or registry ID.
     *
     * @return the current biome, or {@code null} if no world is loaded
     * @since 1.1.5
     */
    @Nullable
    @DocletReplaceReturn("Biome | null")
    public String getBiome() {
        ClientLevel world = mc.level;
        LocalPlayer player = mc.player;
        if (world == null || player == null) return null;
        ResourceLocation id = world.registryAccess().lookupOrThrow(Registries.BIOME).getKey(world.getBiome(player.blockPosition()).value());
        return id == null ? null : id.toString();
    }

    /**
     * Returns the client world's game-time counter, distinct from the day-time clock returned
     * by {@link #getTimeOfDay()}. Returns {@code -1} when no world is loaded.
     *
     * @return the current world time. {@code -1} if world is not loaded.
     * @since 1.1.5
     */
    public long getTime() {
        ClientLevel world = mc.level;
        if (world == null) return -1;
        return world.getGameTime();
    }

    /**
     * Returns the current dimension's time-of-day ticks, including time skipped by sleeping.
     * On 26.1+, dimensions with a default clock use it; dimensions without one use the overworld clock.
     * Unlike {@link #getTime()}, this value follows the day-time clock rather than elapsed game ticks.
     *
     * @return time-of-day ticks, or {@code -1} if no world is loaded
     * @since 1.1.5
     */
    public long getTimeOfDay() {
        ClientLevel world = mc.level;
        if (world == null) return -1;
        //? if >=26.1 {
        /*return world.dimensionType().defaultClock().isPresent()
                ? world.getDefaultClockTime()
                : world.getOverworldClockTime();
        *///? } else {
        return world.getDayTime();
        //?}
    }

    //? if >=26.1 {
    /*/^*
     * Returns the data-driven timelines active in the current dimension.
     * <p>
     * These are Mojang {@code Holder<Timeline>} objects. Each timeline supplies
     * {@code getCurrentTicks}, {@code getTotalTicks}, and {@code getPeriodCount}; pass
     * {@code World.getClockManager()} to those methods. The returned holders retain their
     * registered timeline IDs.
     * <pre>
     * const clockManager = World.getClockManager();
     * for (const timelineHolder of World.getTimelines()) {
     *     const timeline = timelineHolder.value();
     *     Chat.log(`${timelineHolder.getRegisteredName()}: ${timeline.getCurrentTicks(clockManager)}`);
     * }
     * </pre>
     *
     * @return a snapshot of the current dimension's active timeline holders, or an empty list when no world is loaded.
     * @since 2.0.0
     ^/
    public List<Holder<Timeline>> getTimelines() {
        ClientLevel world = mc.level;
        if (world == null) return Collections.emptyList();
        return world.dimensionType().timelines().stream().toList();
    }

    /^*
     * Returns the current world's Mojang {@code ClockManager}.
     * <p>
     * Use this with the raw timelines returned by {@code World.getTimelines()} to read
     * their current, total, and period-count tick values. It is {@code null} when no world
     * is loaded.
     *
     * @return the active Mojang clock manager, or {@code null} when unavailable.
     * @since 2.0.0
     ^/
    @Nullable
    public ClockManager getClockManager() {
        ClientLevel world = mc.level;
        if (world == null) return null;
        return world.clockManager();
    }
    *///?}

    /**
     * Tests the world's outside brightness, based on world time and weather rather than block
     * light or whether the player is underwater. With no world loaded it returns {@code false}.
     * In fixed-time dimensions both this and {@link #isNight()} return {@code false}.
     *
     * @return {@code true} when the world's outside-brightness test passes; {@code false} otherwise
     * @since 1.8.4
     */
    public boolean isDay() {
        ClientLevel world = mc.level;
        if (world == null) return false;
        return world.isBrightOutside();
    }

    /**
     * Uses the world's outside-darkness test, which considers time and weather rather than
     * block light or whether the player is underwater. In fixed-time dimensions both
     * {@link #isDay()} and this method return {@code false}; with no world loaded this also returns {@code false}.
     *
     * @return {@code true} when the world's outside-darkness test passes; {@code false} otherwise, including with no world loaded
     * @since 1.8.4
     */
    public boolean isNight() {
        ClientLevel world = mc.level;
        if (world == null) return false;
        return world.isDarkOutside();
    }

    /**
     * Returns whether the client world's global rain state is active; this is not a local
     * player-position or sky-visibility test.
     *
     * @return {@code true} if global rain is active, {@code false} otherwise, including with no world loaded
     * @since 1.8.4
     */
    public boolean isRaining() {
        ClientLevel world = mc.level;
        if (world == null) return false;
        return world.isRaining();
    }

    /**
     * Returns whether the client world's global thunder state is active, not whether there is
     * thunder at the player's position. The global thunder level is derived from the rain level,
     * so {@code true} implies {@link #isRaining()} is also {@code true}.
     *
     * @return {@code true} if global thunder is active, {@code false} otherwise, including with no world loaded
     * @since 1.8.4
     */
    public boolean isThundering() {
        ClientLevel world = mc.level;
        if (world == null) return false;
        return world.isThundering();
    }

    /**
     * Builds a name-based identifier for the current world; it is not guaranteed unique or stable.
     * Singleplayer uses {@code LOCAL_} followed by the save folder name; realms and LAN games
     * use {@code REALM_} or {@code LAN_} followed by the server entry's name. Other servers use
     * their address with every {@code :25565} substring removed and remaining colons replaced
     * by underscores.
     *
     * @return the derived identifier, or {@code "UNKNOWN_NAME"} if no world or server was found
     * @since 1.8.4
     */
    public String getWorldIdentifier() {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server != null) {
            return "LOCAL_" + server.getWorldPath(LevelResource.ROOT).normalize().getFileName();
        }
        ServerData multiplayerServer = mc.getCurrentServer();
        if (multiplayerServer != null) {
            if (multiplayerServer.isRealm()) {
                return "REALM_" + multiplayerServer.name;
            }
            if (multiplayerServer.isLan()) {
                return "LAN_" + multiplayerServer.name;
            }
            return multiplayerServer.ip.replace(":25565", "").replace(":", "_");
        }
        return "UNKNOWN_NAME";
    }

    /**
     * Returns respawn-related position data from the client level; it does not predict the
     * eventual respawn outcome. Before 1.21.9 this is the level's shared spawn position; on
     * 1.21.9 and later it is the position in the level's respawn data, not a personal bed lookup. The result is
     * {@code null} if no world is loaded.
     *
     * @return the client-level spawn or respawn-data position, or {@code null} if no world is loaded
     * @since 1.2.6
     */
    @Nullable
    public BlockPosHelper getRespawnPos() {
        ClientLevel world = mc.level;
        if (world == null) return null;
        //? if >1.21.8 {
        /*return new BlockPosHelper(world.getRespawnData().pos());
        *///?} else {
        return new BlockPosHelper(world.getSharedSpawnPos());
        //?}
    }

    /**
     * Returns the client world's difficulty ID.
     *
     * @return 0 for peaceful, 1 for easy, 2 for normal, 3 for hard, or {@code -1} if no world is loaded
     * @since 1.2.6
     */
    public int getDifficulty() {
        ClientLevel world = mc.level;
        if (world == null) return -1;
        return world.getDifficulty().getId();
    }

    /**
     * Returns the moon phase index from {@code 0} through {@code 7}; {@code 0} is full and
     * {@code 4} is new. The phase advances once per 24,000 clock ticks.
     *
     * @return the moon phase index, or {@code -1} if no world is loaded
     * @since 1.2.6
     */
    public int getMoonPhase() {
        ClientLevel world = mc.level;
        if (world == null) return -1;
        //? if >=26.1 {
        /*return (int) Math.floorMod(
            world.getOverworldClockTime() / net.minecraft.world.level.MoonPhase.PHASE_LENGTH,
            (long) net.minecraft.world.level.MoonPhase.COUNT
        );
        *///? } else if >=1.21.11 {
        /*return (int) (world.getDayTime() / 24000L % 8L + 8L) % 8;
        *///? } else {
        return world.getMoonPhase();
        //? }
    }

    /**
     * Returns sky light at the block coordinates. This layer is independent of block light and
     * does not account for sky-darkening effects of time or weather; a position in an unloaded
     * chunk may still return a level.
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const pos = player.getBlockPos();
     *   const sky = World.getSkyLight(pos.getX(), pos.getY(), pos.getZ());
     *   const block = World.getBlockLight(pos.getX(), pos.getY(), pos.getZ());
     *   Chat.log(`sky light ${sky}, block light ${block}`);
     * }
     * </pre>
     *
     * @param x the x block coordinate
     * @param y the y block coordinate
     * @param z the z block coordinate
     * @return the sky-light level from {@code 0} to {@code 15}, or {@code -1} if no world is loaded
     * @since 1.1.2
     */
    public int getSkyLight(int x, int y, int z) {
        ClientLevel world = mc.level;
        if (world == null) return -1;
        return world.getBrightness(LightLayer.SKY, new BlockPos(x, y, z));
    }

    /**
     * Returns block-emitted light at the block coordinates, independently of sky light. This
     * value alone does not determine the game's lighting or spawning rules.
     *
     * @param x the x block coordinate
     * @param y the y block coordinate
     * @param z the z block coordinate
     * @return the block-light level from {@code 0} to {@code 15}, or {@code -1} if no world is loaded
     * @since 1.1.2
     */
    public int getBlockLight(int x, int y, int z) {
        ClientLevel world = mc.level;
        if (world == null) return -1;
        return world.getBrightness(LightLayer.BLOCK, new BlockPos(x, y, z));
    }

    /**
     * Opens and starts a Java Sound clip from a file relative to the macro folder. {@code volume}
     * is mapped to the clip's master-gain range; the returned clip can be stopped by the caller
     * and is closed on its stop event. File and Java Sound operations may fail with the declared
     * exceptions; actual audibility depends on the host audio system.
     *
     * @param file the file to play, relative to the macro folder
     * @param volume position in the clip's master-gain range: {@code 0} selects its minimum and {@code 1} its maximum
     * @return the started clip, which the caller may stop
     * @throws LineUnavailableException if the system audio line cannot be obtained
     * @throws IOException if the file cannot be read
     * @throws UnsupportedAudioFileException if the file is not in a supported format
     * @since 1.1.7
     */
    public Clip playSoundFile(String file, double volume) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
        Clip clip = AudioSystem.getClip();
        clip.open(AudioSystem.getAudioInputStream(new File(JsMacrosClient.clientCore.config.macroFolder, file)));
        FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        double min = gainControl.getMinimum();
        double range = gainControl.getMaximum() - min;
        float gain = (float) ((range * volume) + min);
        gainControl.setValue(gain);
        clip.addLineListener(event -> {
            if (event.getType().equals(LineEvent.Type.STOP)) {
                clip.close();
            }
        });
        clip.start();
        return clip;
    }

    /**
     * Plays a local UI sound at volume {@code 1} and pitch {@code 0.25}. The ID is parsed as a
     * resource location; playback is submitted to the client sound manager. For positional
     * playback, use {@link #playSound(String, double, double, double, double, double)}.
     *
     * @param id the sound event id, with or without the minecraft namespace
     * @since 1.1.7
     */
    @DocletReplaceParams("id: SoundId")
    public void playSound(String id) {
        playSound(id, 1F);
    }

    /**
     * Plays a local UI sound at the supplied volume and pitch {@code 0.25}. The ID is parsed as
     * a resource location; playback is submitted to the client sound manager.
     * Use {@link #playSound(String, double, double, double, double, double)} for positional playback.
     *
     * @param id the sound event id, with or without the minecraft namespace
     * @param volume the volume value passed to Minecraft's sound manager
     * @since 1.1.7
     */
    @DocletReplaceParams("id: SoundId, volume: double")
    public void playSound(String id, double volume) {
        playSound(id, volume, 0.25F);
    }

    /**
     * Plays a local UI sound with volume and pitch converted to {@code float}. The two-argument
     * overload defaults pitch to {@code 0.25}; pass a pitch explicitly to choose another value.
     * Use {@link #playSound(String, double, double, double, double, double)} for positional playback.
     *
     * @param id the sound event id, with or without the minecraft namespace
     * @param volume the volume value passed to Minecraft's sound manager
     * @param pitch the pitch value passed to Minecraft's sound manager
     * @since 1.1.7
     */
    @DocletReplaceParams("id: CanOmitNamespace<SoundId>, volume: double, pitch: double")
    public void playSound(String id, double volume, double pitch) {
        SoundEvent sound = SoundEvent.createVariableRangeEvent(ResourceLocation.parse(id));
        assert sound != null;
        mc.execute(() -> mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, (float) pitch, (float) volume)));
    }

    /**
     * Plays a local sound at the supplied coordinates using the master category. Volume and
     * pitch are converted to {@code float}; with no world loaded, this method does nothing.
     * The sound is not sent to the server.
     *
     * @param id the sound event id, with or without the minecraft namespace
     * @param volume the volume value passed to Minecraft's local-sound API
     * @param pitch the pitch value passed to Minecraft's local-sound API
     * @param x the x world coordinate in blocks
     * @param y the y world coordinate in blocks
     * @param z the z world coordinate in blocks
     * @since 1.1.7
     */
    @DocletReplaceParams("id: CanOmitNamespace<SoundId>, volume: double, pitch: double, x: double, y: double, z: double")
    public void playSound(String id, double volume, double pitch, double x, double y, double z) {
        ClientLevel world = mc.level;
        if (world == null) return;
        SoundEvent sound = SoundEvent.createVariableRangeEvent(ResourceLocation.parse(id));
        assert sound != null;
        mc.execute(() -> world.playLocalSound(x, y, z, sound, SoundSource.MASTER, (float) volume, (float) pitch, true));
    }

    /**
     * Copies the client boss overlay's bars into a map keyed by UUID strings.
     * Bars are not restricted to boss fights. The map is a fresh snapshot, but its helpers
     * wrap the existing bar objects rather than frozen copies of their state.
     *
     * @return a non-null map of boss-bar helpers, empty when the overlay has no bars
     * @since 1.2.1
     */
    public Map<String, BossBarHelper> getBossBars() {
        assert mc.gui != null;
        Map<UUID, LerpingBossEvent> bars = ImmutableMap.copyOf(GuiCompat.hud(mc).getBossOverlay().events);
        Map<String, BossBarHelper> out = new HashMap<>();
        for (Map.Entry<UUID, LerpingBossEvent> e : ImmutableList.copyOf(bars.entrySet())) {
            out.put(e.getKey().toString(), new BossBarHelper(e.getValue()));
        }
        return out;
    }

    /**
     * Checks whether the client chunk source currently has the given chunk.
     *
     * @param chunkX the x coordinate of the chunk, not the absolute position
     * @param chunkZ the z coordinate of the chunk, not the absolute position
     * @return {@code true} if the client chunk source has the chunk; {@code false} if no world is loaded
     * @since 1.2.2
     */
    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        ClientLevel world = mc.level;
        if (world == null) return false;
        return world.getChunkSource().hasChunk(chunkX, chunkZ);
    }

    /**
     * Returns the remote address recorded by the current network connection; its string format
     * depends on that address and is not necessarily the hostname entered by the player.
     * <pre>
     * const address = World.getCurrentServerAddress();
     * if (address !== null) {
     *   Chat.log(`connected to ${address}`);
     * }
     * </pre>
     *
     * @return the connection's remote address string, or {@code null} if no connection exists
     * @since 1.2.2
     */
    @Nullable
    public String getCurrentServerAddress() {
        ClientPacketListener h = mc.getConnection();
        if (h == null) {
            return null;
        }
        Connection c = h.getConnection();
        if (c == null) {
            return null;
        }
        return c.getRemoteAddress().toString();
    }

    /**
     * Looks up the biome at {@code (x, 10, z)}; use {@link #getBiomeAt(int, int, int)} when the
     * vertical coordinate matters. The lookup does not require a loaded chunk, but the result
     * can be {@code null} if no world is loaded or the biome has no registered key.
     * <pre>
     * // what biome is this column in
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   Chat.log(`column biome is ${World.getBiomeAt(Math.floor(player.getX()), Math.floor(player.getZ()))}`);
     * }
     * </pre>
     *
     * @param x the x block coordinate
     * @param z the z block coordinate
     * @return the biome ID at the sampled position, or {@code null} if unavailable
     * @since 1.2.3
     */
    @Nullable
    @DocletReplaceReturn("Biome | null")
    public String getBiomeAt(int x, int z) {
        ClientLevel world = mc.level;
        if (world == null) return null;
        ResourceLocation id = world.registryAccess().lookupOrThrow(Registries.BIOME).getKey(world.getBiome(new BlockPos(x, 10, z)).value());
        return id == null ? null : id.toString();
    }

    /**
     * Looks up the biome at the given block coordinates, including the supplied vertical
     * coordinate (unlike {@link #getBiomeAt(int, int)}). The lookup does not require a loaded
     * chunk, but the result can be {@code null} if no world is loaded or the biome has no
     * registered key.
     *
     * @param x the x block coordinate
     * @param y the y block coordinate
     * @param z the z block coordinate
     * @return the biome ID at the sampled position, or {@code null} if unavailable
     * @since 1.8.4
     */
    @Nullable
    @DocletReplaceReturn("Biome | null")
    public String getBiomeAt(int x, int y, int z) {
        ClientLevel world = mc.level;
        if (world == null) return null;
        ResourceLocation id = world.registryAccess().lookupOrThrow(Registries.BIOME).getKey(world.getBiome(new BlockPos(x, y, z)).value());
        return id == null ? null : id.toString();
    }

    /**
     * Formats the latest server TPS estimate and the one-, five- and fifteen-minute averages.
     * Estimates use server time updates, not a server-reported TPS measurement. The one-minute
     * value averages retained instant estimates; the five-minute value averages one-minute
     * averages sampled at intervals exceeding one minute, and the fifteen-minute value averages
     * five-minute averages sampled at intervals exceeding five minutes. Each update removes at
     * most one expired sample from each window, so older samples can remain after a long gap.
     * Values start at 20 and retain their last reading across world changes. Login clears the
     * sample windows but does not reset the published values; a later time update recalculates
     * them using only samples accumulated since login. Longer averages represent their full
     * periods only after enough new samples accumulate.
     *
     * <pre>
     * Chat.log(`server tps: ${World.getServerTPS()}`);
     * </pre>
     *
     * @return the instant, one-, five- and fifteen-minute estimates in one string
     * @since 1.2.7
     */
    @DocletReplaceReturn("`${number}, 1M: ${number}, 5M: ${number}, 15M: ${number}`")
    public String getServerTPS() {
        return String.format("%.2f, 1M: %.1f, 5M: %.1f, 15M: %.1f", serverInstantTPS, server1MAverageTPS, server5MAverageTPS, server15MAverageTPS);
    }

    /**
     * Returns the current tab-list header, normally supplied by the server, as a {@link TextHelper}.
     * Retains its rich formatting and hover/click data, without guaranteeing those actions run.
     * <pre>
     * const header = World.getTabListHeader();
     * if (header !== null) {
     *   Chat.log(`tab-list header: ${header}`);
     * }
     * </pre>
     *
     * @return the header, or {@code null} if absent
     * @since 1.3.1
     */
    @Nullable
    public TextHelper getTabListHeader() {
        return TextHelper.wrap(((IPlayerListHud) GuiCompat.hud(mc).getTabList()).jsmacros_getHeader());
    }

    /**
     * Returns the current tab-list footer, normally supplied by the server, as a {@link TextHelper}.
     * Retains its rich formatting and hover/click data, without guaranteeing those actions run.
     * Use {@link #getTabListHeader()} for the corresponding header.
     *
     * @return the footer, or {@code null} if absent
     * @since 1.3.1
     */
    @Nullable
    public TextHelper getTabListFooter() {
        return TextHelper.wrap(((IPlayerListHud) GuiCompat.hud(mc).getTabList()).jsmacros_getFooter());
    }

    /**
     * Spawns particles locally using deltas of {@code 0.1} on each axis, speed {@code 1}, and
     * the particle-limiter override enabled. Positive counts use Gaussian position offsets,
     * not a bounded tenth-block box. Nothing happens without a player.
     * See {@link #spawnParticle(String, double, double, double, double, double, double, double, int, boolean)}
     * for supported IDs, count semantics, and visibility limitations.
     * <pre>
     * // a puff of particles at a point in the world
     * World.spawnParticle("minecraft:flame", 0, 65, 0, 20);
     * </pre>
     *
     * @param id    a simple particle registry ID; the namespace defaults to {@code minecraft}
     * @param x     the x world coordinate in blocks
     * @param y     the y world coordinate in blocks
     * @param z     the z world coordinate in blocks
     * @param count positive values attempt that many particles; zero attempts one; negative values attempt none
     * @since 1.8.4
     */
    @DocletReplaceParams("id: ParticleId, x: double, y: double, z: double, count: int")
    public void spawnParticle(String id, double x, double y, double z, int count) {
        spawnParticle(id, x, y, z, 0.1, 0.1, 0.1, 1, count, true);
    }

    /**
     * Schedules particle spawning on the local client; no particles are sent to the server or
     * other players. Nothing happens without a player.
     * <p>
     * For positive counts, each attempt uses Gaussian position offsets scaled by the deltas
     * and independent Gaussian velocity components scaled by {@code speed}. Zero deltas keep
     * the initial positions at the supplied coordinates; they do not eliminate velocity.
     * A zero count instead attempts one particle at the exact position with velocity components
     * {@code deltaX * speed}, {@code deltaY * speed}, and {@code deltaZ * speed}.
     * <p>
     * The ID is a registry identifier, not particle-option data. Only simple particle types
     * are supported; parameterized types such as {@code minecraft:dust} cause a
     * {@link ClassCastException}. A missing registry value falls back to {@code CLOUD}.
     * The {@code force} argument overrides the particle limiter, while the packet's
     * {@code alwaysShow} flag is always enabled. Neither guarantees visibility at arbitrary
     * distances or that every attempted particle will render.
     * <pre>
     * // a tight burst of flame with the particle-limiter override enabled
     * World.spawnParticle("flame", 0, 65, 0, 0, 0, 0, 1, 10, true);
     *
     * // and a wider Gaussian spread, not a bounded four-block box
     * World.spawnParticle("flame", 0, 65, 0, 2, 2, 2, 0.1, 100, false);
     * </pre>
     *
     * @param id     a simple particle registry ID; the namespace defaults to {@code minecraft}
     * @param x      the x world coordinate in blocks
     * @param y      the y world coordinate in blocks
     * @param z      the z world coordinate in blocks
     * @param deltaX the x position-offset scale for positive counts, or x velocity factor for zero
     * @param deltaY the y position-offset scale for positive counts, or y velocity factor for zero
     * @param deltaZ the z position-offset scale for positive counts, or z velocity factor for zero
     * @param speed  the velocity scale; it does not scale position offsets
     * @param count  positive values attempt that many particles; zero attempts one; negative values attempt none
     * @param force  whether to set the particle-limiter override flag
     * @since 1.8.4
     */
    @DocletReplaceParams("id: CanOmitNamespace<ParticleId>, x: double, y: double, z: double, deltaX: double, deltaY: double, deltaZ: double, speed: double, count: int, force: boolean")
    public void spawnParticle(String id, double x, double y, double z, double deltaX, double deltaY, double deltaZ, double speed, int count, boolean force) {
        LocalPlayer player = mc.player;
        if (player == null) return;
        ParticleOptions particle = (ParticleOptions) BuiltInRegistries.PARTICLE_TYPE.getValue(RegistryHelper.parseIdentifier(id));
        particle = particle != null ? particle : ParticleTypes.CLOUD;

        ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(particle, force, true, x, y, z, (float) deltaX, (float) deltaY, (float) deltaZ, (float) speed, count);
        mc.execute(() -> player.connection.handleParticleEvent(packet));
    }

    /**
     * Returns the raw Minecraft client level rather than a JsMacros wrapper. Its member names
     * depend on the loader and Minecraft target, and shipped typings do not model those game
     * members; see {@link FClient#getMinecraft()} for the related raw Minecraft access point.
     *
     * @return the raw client level, or {@code null} if no world is loaded
     * @since 1.9.1
     */
    @Nullable
    public ClientLevel getRaw() {
        return mc.level;
    }

    /**
     * Returns the latest server TPS estimate, calculated from server time updates.
     * Initialized to 20 and not reset on world changes.
     *
     * @return the latest server TPS estimate
     * @since 1.2.7
     */
    public double getServerInstantTPS() {
        return serverInstantTPS;
    }

    /**
     * Returns the server's estimated TPS averaged over the last minute.
     *
     * @return the one-minute average of instant TPS estimates
     * @since 1.2.7
     */
    public double getServer1MAverageTPS() {
        return server1MAverageTPS;
    }

    /**
     * Returns the server's estimated TPS averaged over the last five minutes.
     *
     * @return the five-minute average of one-minute TPS averages
     * @since 1.2.7
     */
    public double getServer5MAverageTPS() {
        return server5MAverageTPS;
    }

    /**
     * Returns the server's estimated TPS averaged over the last fifteen minutes.
     *
     * @return the fifteen-minute average of five-minute TPS averages
     * @since 1.2.7
     */
    public double getServer15MAverageTPS() {
        return server15MAverageTPS;
    }

}
