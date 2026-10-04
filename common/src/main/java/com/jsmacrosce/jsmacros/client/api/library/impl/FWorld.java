package com.jsmacrosce.jsmacros.client.api.library.impl;

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
import org.apache.logging.log4j.core.jmx.Server;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletReplaceParams;
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
 * "World" here is the world the client currently has loaded, which means the singleplayer world
 * being played or the one on the server, and the client only knows about the parts of it the
 * server has sent. That one fact explains most of what is surprising here: a chunk the client
 * has not been sent has no blocks in it, an entity outside the render distance is not found, and
 * anything read from a block the client does not have comes back as {@code null} rather than as
 * air. {@link #isWorldLoaded()} is the test for there being a world at all, and everything that
 * can fail returns {@code null} or a sentinel rather than raising, so a script that reads it
 * should check.<br>
 * The library splits into what it can ask and what it can do. The readers are the environment
 * ones, {@link #getTime()}, {@link #getTimeOfDay()}, {@link #getBiome()},
 * {@link #getDimension()}, {@link #getDifficulty()}, {@link #getMoonPhase()} and the weather
 * calls, which describe the world; the player ones, {@link #getLoadedPlayers()} and
 * {@link #getPlayers()}, which are two different sets; and the lookups, {@link #getBlock(int,
 * int, int)}, {@link #getChunk(int, int)}, {@link #getEntities()} and the biome and scoreboard
 * calls. The writers are the effects: {@link #playSound(String, double, double, double, double,
 * double)}, {@link #spawnParticle(String, double, double, double, int)} and
 * {@link #playSoundFile(String, double)}.<br>
 * The searching is its own group and is the most capable part. {@link #getWorldScanner()} builds
 * a scanner through a chainable filter, {@link #findBlocksMatching} are shortcuts for the
 * common cases, and {@link #iterateSphere(BlockPosHelper, int, MethodWrapper)} and
 * {@link #iterateBox(BlockPosHelper, BlockPosHelper, MethodWrapper)} walk a region and call back
 * for each block rather than building a list. A search only sees loaded chunks, so a wide
 * {@code chunkrange} is a lot of work for what is usually a small answer.
 * <br>
 * On coordinates: block positions here are block coordinates, so a whole number naming one
 * block, and chunk coordinates are the position divided by sixteen, which is what
 * {@link #getChunk(int, int)} takes rather than an absolute position. The
 * {@code findBlocksMatching} overloads differ on this point, and the parameter names say which:
 * some take a centre position and some take a centre chunk.
 * example:
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
     * the most recent server ticks per second reading, as a rate rather than a time.
     * <p>
     * This is worked out from the gap between the server's time updates, so it is the server's
     * own rate and not the client's, and it is a reading of one interval rather than a smoothed
     * figure. Twenty is the healthy value: a number above it means the server has time to
     * spare, below it that it is behind. A server that sends no time updates at all leaves this
     * at the twenty it starts at rather than reporting anything else.
     * <p>
     * Don't modify.
     * @since 1.2.7
     */
    public static double serverInstantTPS = 20;
    /**
     * the server's rate averaged over the last minute.
     * <p>
     * The mean of every instant reading in the last sixty seconds, so it is the figure that
     * moves slowly and the one a lag spike shows up in afterwards. It is the shortest of the
     * three windows this keeps.
     * <p>
     * Don't modify.
     * @since 1.2.7
     */
    public static double server1MAverageTPS = 20;
    /**
     * the server's rate averaged over the last five minutes.
     * <p>
     * This is built from the one minute average rather than from the instant readings, so it is
     * a mean of a mean and reacts more slowly than {@link #getServer1MAverageTPS()} does. A lag
     * spike takes most of a window to wash out of this one.
     * <p>
     * Don't modify.
     * @since 1.2.7
     */
    public static double server5MAverageTPS = 20;
    /**
     * the server's rate averaged over the last fifteen minutes.
     * <p>
     * The longest of the three windows and the slowest to react, which makes it the one to read
     * when a script wants to know whether a server is healthy across a session rather than
     * whether it is healthy right now.
     * <p>
     * Don't modify.
     * @since 1.2.7
     */
    public static double server15MAverageTPS = 20;

    public FWorld(Core<?, ?> runner) {
        super(runner);
    }

    /**
     * returns whether a world is currently loaded
     * <p>
     * This is the guard nearly everything else in this library wants in front of it. It is
     * {@code false} at the title screen, in any menu, and while a world is being swapped, and
     * {@code true} from the moment a world is joined until it is left. Most of the calls here
     * return {@code null} or a sentinel rather than raising when there is no world, so this is
     * for a script that would rather not deal with those at all.
     * example:
     * <pre>
     * // the guard a script wants in front of anything that reads the world
     * if (World.isWorldLoaded()) {
     *   Chat.log(`in ${World.getDimension()}`);
     * } else {
     *   Chat.log("not in a world");
     * }
     * </pre>
     *
     * @return {@code true} if a world is currently loaded, {@code false} otherwise
     * @since 1.3.0
     */
    public boolean isWorldLoaded() {
        return mc.level != null;
    }

    /**
     * every player the client has an entity for, which is the ones within the render distance.
     * <p>
     * This is the server's player list as entities, so it holds the players whose position the
     * client has been told about and no others. A player in another dimension is not here, and
     * neither is one further away than the render distance. It is {@code null} with no world
     * loaded.<br>
     * For the tab list itself rather than for who is nearby, {@link #getPlayers()} is the call.
     * example:
     * <pre>
     * const players = World.getLoadedPlayers();
     * if (players !== null) {
     *   for (const p of players) {
     *     Chat.log(`${p.getName()} at ${p.getX()}, ${p.getY()}, ${p.getZ()}`);
     *   }
     * }
     * </pre>
     *
     * @return players within render distance, or {@code null} if no world is loaded
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
     * the tab list, which is everybody the server says is online.
     * <p>
     * This is the list the tab list itself is drawn from, so it covers players in other
     * dimensions and players too far away to be loaded as entities. It is a snapshot, taken when
     * it is called, and {@code null} with no world loaded. A player the server has hidden from
     * the tab list is not in it, since the client is never told about that.<br>
     * {@link #getLoadedPlayers()} is the other set, and the two are not the same thing: this is
     * who is online, that is who is nearby.
     * example:
     * <pre>
     * // who is online, and what the server's own ping on them is
     * const online = World.getPlayers();
     * if (online !== null) {
     *   for (const entry of online) {
     *     Chat.log(`${entry.getName()} at ${entry.getPing()}ms`);
     *   }
     * }
     * </pre>
     *
     * @return players on the tablist, or {@code null} if no world is loaded
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
     * one player's tab list entry, by name.
     * <p>
     * This is the same entry {@link #getPlayers()} hands back, found by name, and it carries
     * the tab list's own view of a player: their name, the gamemode the server says they are in,
     * and the ping the server has measured. It is {@code null} for a name that is not online
     * and with no world loaded, and the name has to match what the server reports rather than
     * whatever case the script has, so a lookup by a differently cased name can miss.<br>
     * The ping here is the server's number rather than the client's, so it measures the round
     * trip to the server rather than to the player.
     * example:
     * <pre>
     * const entry = World.getPlayerEntry("Notch");
     * if (entry !== null) {
     *   Chat.log(`${entry.getName()} in ${entry.getGamemode()} at ${entry.getPing()}ms`);
     * }
     * </pre>
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
     * the block at these three block coordinates.
     * <p>
     * This is the main way a script reads the world. The three arguments are block coordinates,
     * so they are whole numbers naming one block rather than a point that can sit partway inside
     * one, and the block is the one the game's data holds at that exact cell.<br>
     * A {@code null} here does <em>not</em> mean air. It means the client has no data for that
     * position, which is the case for a chunk it has not been sent, and that is a different
     * thing from a chunk that really is air. A script walking a large area has to treat
     * {@code null} as "do not know" rather than as "nothing here", and it is also {@code null}
     * with no world loaded.<br>
     * The returned helper carries the block's state and its block entity where there is one, so
     * a chest's contents come from here rather than from a separate call.
     * example:
     * <pre>
     * // what is the block at these three block coordinates
     * const block = World.getBlock(0, 64, 0);
     * if (block !== null) {
     *   Chat.log(`${block.getName()}`);
     * } else {
     *   Chat.log("nothing known about that position");
     * }
     * </pre>
     *
     * @param x the x block coordinate
     * @param y the y block coordinate
     * @param z the z block coordinate
     * @return The block at that position, or {@code null} if the client has no data for it
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
     * the block a point in the world falls inside.
     * <p>
     * This is the point form of {@link #getBlock(int, int, int)}, and the difference that
     * matters is the rounding: a {@code Pos3D} is a double precision point that can sit partway
     * inside a block, and this rounds each axis down to the block below it. So {@code (0.5, 64,
     * -0.5)} is the block at {@code (0, 64, -1)}, and rounding down rather than to the nearest
     * is what makes a point sitting on the lower edge of a block belong to that block.<br>
     * A {@code null} result means the same as it does for the three argument form: the client
     * has no data there, which is not the same as air.
     * example:
     * <pre>
     * // the block a fractional point is in
     * const point = PositionCommon.createPos(0.5, 64.9, -0.5);
     * const block = World.getBlock(point);
     * if (block !== null) {
     *   Chat.log(`that point is in ${block.getName()}`);
     * }
     * </pre>
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
     * the block at a block position.
     * <p>
     * This is the block position form of {@link #getBlock(int, int, int)}, and since a
     * {@code BlockPosHelper} is already whole numbers naming one block there is nothing to round
     * here; this is the same call with the position passed as an object, which is what a script
     * has when the position came from a block or an entity rather than being computed.<br>
     * A {@code null} result means the client has no data there, which is not the same as air.
     * example:
     * <pre>
     * // what is above the block the player is standing on
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const above = World.getBlock(player.getBlockPos().up());
     *   if (above !== null) {
     *     Chat.log(`headroom is ${above.getName()}`);
     *   }
     * }
     * </pre>
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
     * The x and z position of the chunk can be calculated by the following formula: xChunk =
     * x >> 4; zChunk = z >> 4;
     * <p>
     * The two arguments are <em>chunk</em> coordinates, not block coordinates, so they are the
     * block position divided by sixteen and shifted right. A block at {@code 100} is in chunk
     * {@code 6}, and the sixteen blocks in a chunk run from {@code 96} to {@code 111}. The shift
     * rather than a division is what makes negative positions work, so {@code -1} is chunk
     * {@code -1} and not chunk {@code 0}.<br>
     * The result is {@code null} with no world loaded, and asking for a chunk the client has
     * not been sent is where this gets expensive rather than where it fails: the game will
     * build the chunk rather than hand back nothing, which is work and memory for a chunk the
     * server may not even agree on. {@link #isChunkLoaded(int, int)} is the cheap check to do
     * first.
     * example:
     * <pre>
     * // the chunk the player is standing in, worked out from their position
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const x = Math.floor(player.getX()) >> 4;
     *   const z = Math.floor(player.getZ()) >> 4;
     *   if (World.isChunkLoaded(x, z)) {
     *     const chunk = World.getChunk(x, z);
     *     if (chunk !== null) {
     *       Chat.log(`chunk ${x}, ${z} is loaded`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @param x the x coordinate of the chunk, not the absolute position
     * @param z the z coordinate of the chunk, not the absolute position
     * @return ChunkHelper for the chunk coordinates {@link ChunkHelper}, or {@code null} if no world is loaded
     * @since 1.8.4
     */
    @Nullable
    public ChunkHelper getChunk(int x, int z) {
        ClientLevel world = mc.level;
        if (world == null) return null;
        return new ChunkHelper(world.getChunk(x, z));
    }

    /**
     * Usage: <br>
     * This will return all blocks that are facing south, don't require a tool to break,
     * have a hardness of 10 or less and whose name contains either chest or barrel.
     * <pre>
     * World.getWorldScanner()
     *     .withBlockFilter("getHardness").is(">=", 10)
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
     * a slow one makes the scan slow. They also run off the main thread, so a filter that
     * touches the world is not safe.
     * example:
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
     * every block of one kind in a square of chunks around a given chunk.
     * <p>
     * This is the shortcut form of the scanner, for the common case of looking for one block id
     * in a region. The centre is in <em>chunk</em> coordinates, the same divided by sixteen
     * {@link #getChunk(int, int)} takes, and the range is how far out in chunks to go, so a
     * range of {@code 1} is the nine chunks around the centre.<br>
     * The search only sees chunks the client has been sent, and it is a full walk of every block
     * in range, so a wide range is a lot of work. The result is every matching position rather
     * than a count, and it is {@code null} with no world loaded.
     * example:
     * <pre>
     * // gold ore in the nine chunks around the player's own chunk
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const cx = Math.floor(player.getX()) >> 4;
     *   const cz = Math.floor(player.getZ()) >> 4;
     *   const found = World.findBlocksMatching(cx, cz, "gold_ore", 1);
     *   if (found !== null) {
     *     Chat.log(`${found.size()} gold ore in range`);
     *   }
     * }
     * </pre>
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
     * every block of one kind in a square of chunks around the player.
     * <p>
     * The same as {@link #findBlocksMatching(int, int, String, int)} with the centre worked
     * out from where the player is, which is what a script almost always wants. The result is
     * {@code null} when there is no world or no player, so a script that has not already
     * checked wants to.
     * <p>
     * Only loaded chunks are searched, and a wide range is a full walk of every block in it.
     * example:
     * <pre>
     * // diamond ore in the chunks around the player
     * const found = World.findBlocksMatching("diamond_ore", 4);
     * if (found !== null) {
     *   Chat.log(`${found.size()} found`);
     * }
     * </pre>
     *
     * @param id the block id to look for, with or without the minecraft namespace
     * @param chunkrange how many chunks out from the player's chunk to search
     * @return the positions of every matching block, or {@code null} if there is no world
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
     * every block of any of several kinds in a square of chunks around the player.
     * <p>
     * This is {@link #findBlocksMatching(String, int)} for more than one id, and the ids are
     * matched as a set, so a block that is any of them is kept and the list comes back in no
     * particular order. Duplicates in the array do not produce duplicate results.
     * <p>
     * The result is {@code null} when there is no world or no player, and only loaded chunks
     * are searched.
     * example:
     * <pre>
     * // any of the three ore kinds, in the chunks around the player
     * const found = World.findBlocksMatching(["diamond_ore", "gold_ore", "iron_ore"], 4);
     * if (found !== null) {
     *   Chat.log(`${found.size()} ore blocks in range`);
     * }
     * </pre>
     *
     * @param ids the block ids to look for, with or without the minecraft namespace
     * @param chunkrange how many chunks out from the player's chunk to search
     * @return the positions of every matching block, or {@code null} if there is no world
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
     * every block of any of several kinds in a square of chunks around a given chunk.
     * <p>
     * This is {@link #findBlocksMatching(int, int, String, int)} for more than one id, and the
     * ids are matched as a set, so a block that is any of them is kept and the list comes back
     * in no particular order. Duplicates in the array do not produce duplicate results.
     * example:
     * <pre>
     * // any ore at all, in the nine chunks around the player's own chunk
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const cx = Math.floor(player.getX()) >> 4;
     *   const cz = Math.floor(player.getZ()) >> 4;
     *   const found = World.findBlocksMatching(cx, cz, ["diamond_ore", "gold_ore"], 1);
     *   if (found !== null) {
     *     Chat.log(`${found.size()} ore blocks in range`);
     *   }
     * }
     * </pre>
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
     * every block in a square of chunks around the player that two filters keep.
     * <p>
     * This is the filter form of the search, and the shape a script wants when the id alone is
     * not enough, for instance when it wants a chest but not a trapped one. The block filter
     * must be given and cannot be {@code null}; the state filter is optional and can be, in
     * which case only the first filter applies.<br>
     * The filters are called once per block in range rather than once per match, so a slow one
     * makes the search slow. The result is {@code null} when there is no player, which is what
     * this form is checked on rather than on the world.
     * example:
     * <pre class="language-typescript">
     * // chests in the player's chunks, but not barrels
     * const found = World.findBlocksMatching(
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() === "minecraft:chest";
     *   }),
     *   null,
     *   2
     * );
     * if (found !== null) {
     *   Chat.log(`${found.size()} chests in range`);
     * }
     * </pre>
     *
     * @param blockFilter called for each block, keeping the ones it returns true for
     * @param stateFilter called for the state of each block the first filter kept, or
     *                    {@code null} to keep all of those
     * @param chunkrange how many chunks out from the player's chunk to search
     * @return the positions of every matching block, or {@code null} if there is no player
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
     * every block in a square of chunks around a given chunk that two filters keep.
     * <p>
     * This is {@link #findBlocksMatching(MethodWrapper, MethodWrapper, int)} with the centre
     * given rather than taken from the player, and it is what a script searches from a place it
     * is not standing in. The centre is in <em>chunk</em> coordinates. The block filter must be
     * given and the state filter is optional.
     * <p>
     * The filters are called once per block in range rather than once per match, so a slow one
     * makes the search slow. The result is {@code null} with no world loaded.
     * example:
     * <pre class="language-typescript">
     * // leaves in the nine chunks around a chunk, for a macro that clears them
     * const found = World.findBlocksMatching(
     *   0, 0,
     *   JavaWrapper.methodToJava(function (block: BlockHelper) {
     *     return block.getId() === "minecraft:oak_leaves";
     *   }),
     *   null,
     *   1
     * );
     * if (found !== null) {
     *   Chat.log(`${found.size()} leaf blocks near the origin`);
     * }
     * </pre>
     *
     * @param chunkX the x coordinate of the centre chunk, not the block position
     * @param chunkZ the z coordinate of the centre chunk, not the block position
     * @param blockFilter called for each block, keeping the ones it returns true for
     * @param stateFilter called for the state of each block the first filter kept, or
     *                    {@code null} to keep all of those
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
     * per-block work that costs, and it runs off the main thread.
     * example:
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
     * walk a sphere of block positions around a centre, with a choice about air.
     * <p>
     * The shape is a sphere measured in whole blocks, so the callback is made for every
     * position inside the radius rather than for a surface, and the vertical extent is clamped
     * to the world's own build limits. With {@code ignoreAir} left on, the callback only sees
     * real blocks; turning it off calls it for every position in the sphere including air,
     * which is how a script counts empty space.<br>
     * A negative radius raises rather than doing nothing, and this is one of the few calls here
     * that does raise rather than returning a sentinel.
     * example:
     * <pre>
     * // how much of a bubble is air
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   let air = 0;
     *   World.iterateSphere(player.getBlockPos(), 3, false, JavaWrapper.methodToJava(function (block) {
     *     if (block.getBlockStateHelper().isAir()) air++;
     *   }));
     *   Chat.log(`${air} air blocks within three blocks`);
     * }
     * </pre>
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
     * The callback is called once per block position that is not air, off the main thread.
     * example:
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
     * walk a cuboid of block positions, with a choice about air.
     * <p>
     * As {@link #iterateBox(BlockPosHelper, BlockPosHelper, MethodWrapper)}, but with the
     * choice of seeing air as well. Turning {@code ignoreAir} off calls the callback for every
     * position in the box, empty ones included, which is how a script measures a volume rather
     * than what is in it. The y of both corners is clamped to the world's own build limits.
     * example:
     * <pre>
     * // how much of a ten by ten by ten volume is air
     * let air = 0;
     * World.iterateBox(
     *   PositionCommon.createBlockPos(0, 0, 0),
     *   PositionCommon.createBlockPos(9, 9, 9),
     *   false,
     *   JavaWrapper.methodToJava(function (block) {
     *     if (block.getBlockStateHelper().isAir()) air++;
     *   })
     * );
     * Chat.log(`${air} of 1000 blocks are air`);
     * </pre>
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
     * @return a helper for the scoreboards provided to the client.
     * <p>
     * This is the sidebar and the list below the player's list, and it is the server's own
     * scoreboard as the client has it, so it follows whatever the server sends and changes
     * whenever the server updates it. The helper covers the objectives, the slots they are in
     * and the teams and their colours, and it is {@code null} with no world loaded.
     * example:
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
     * @since 1.2.9
     */
    @Nullable
    public ScoreboardsHelper getScoreboards() {
        ClientLevel world = mc.level;
        if (world == null) return null;
        return new ScoreboardsHelper(world.getScoreboard());
    }

    /**
     * @return all entities in the render distance.
     * <p>
     * Everything the client has been told about and is currently keeping: mobs, items, arrows,
     * players, everything. That means it is bounded by the render distance rather than by the
     * world, and it changes from tick to tick, so the list is a snapshot and a second call
     * gives a different answer. It is {@code null} with no world loaded.
     * <p>
     * The narrower forms are usually the better choice: {@link #getEntities(String...)} by type,
     * {@link #getEntities(double)} by distance from the player, and the two together. A filter
     * function is the most flexible of all, through {@link #getEntities(MethodWrapper)}.
     * example:
     * <pre>
     * const entities = World.getEntities();
     * if (entities !== null) {
     *   Chat.log(`${entities.size()} entities within the render distance`);
     * }
     * </pre>
     *
     */
    @Nullable
    public List<EntityHelper<?>> getEntities() {
        return getEntitiesInternal(entity -> true);
    }

    /**
     * every entity of the given types, within the render distance.
     * <p>
     * The types are ids with or without the minecraft namespace, and several can be given at
     * once, in which case the result is the union rather than an intersection. The list is
     * still bounded by the render distance and is a snapshot, and it is {@code null} with no
     * world loaded.<br>
     * This is the form to use when a script knows what it is looking for, since the game
     * filters by type and the script does not have to.
     * example:
     * <pre>
     * // every sheep and cow in the render distance
     * const mobs = World.getEntities("sheep", "cow");
     * if (mobs !== null) {
     *   for (const mob of mobs) {
     *     Chat.log(`${mob.getName()} at ${mob.getBlockPos()}`);
     *   }
     * }
     * </pre>
     *
     * @param types the entity types to consider, with or without the minecraft namespace
     * @return all entities in the render distance, that match the specified entity type.
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
     * every entity within a distance of the player.
     * <p>
     * The distance is measured from the player and the entities within the render distance are
     * the ones considered, so this narrows {@link #getEntities()} rather than reaching further
     * than it. The boundary is inclusive, so an entity exactly at the distance is in the
     * result.<br>
     * The result is {@code null} when there is no world or no player, and is a snapshot that
     * changes from tick to tick.
     * example:
     * <pre>
     * // anything at all within ten blocks of the player
     * const near = World.getEntities(10);
     * if (near !== null) {
     *   for (const entity of near) {
     *     Chat.log(entity.getName());
     *   }
     * }
     * </pre>
     *
     * @param distance the maximum distance to search for entities
     * @return a list of entities within the specified distance to the player.
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
     * every entity of the given types within a distance of the player.
     * <p>
     * This is {@link #getEntities(double)} and {@link #getEntities(String...)} together, and it
     * is the form a script usually wants: the game does both of the narrowing, so the result is
     * small and the script does not walk a list it has no interest in. The distance is
     * inclusive and measured from the player.<br>
     * The result is {@code null} when there is no world or no player, and is a snapshot that
     * changes from tick to tick.
     * example:
     * <pre>
     * // hostile mobs within twenty blocks, which is what a combat macro wants
     * const mobs = World.getEntities(20, "zombie", "skeleton", "creeper");
     * if (mobs !== null) {
     *   Chat.log(`${mobs.size()} hostile mobs close by`);
     * }
     * </pre>
     *
     * @param distance the maximum distance to search for entities
     * @param types    the entity types to consider, with or without the minecraft namespace
     * @return a list of entities within the specified distance to the player, that match the specified entity type.
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
     * every entity a filter function keeps, within the render distance.
     * <p>
     * This is the flexible form: the filter is called once for every entity the client has and
     * the ones it returns {@code true} for are kept, so a script can express anything the
     * narrower calls do not, at the cost of the game handing it the whole list first. It is
     * {@code null} with no world loaded.
     * <p>
     * The entity handed to the filter is a wrapper rather than the game's own object, so the
     * filter reads through the helper methods rather than reaching into raw fields.
     * example:
     * <pre class="language-typescript">
     * // every entity that is a mob rather than an item, a block or a player
     * const mobs = World.getEntities(JavaWrapper.methodToJava(function (entity: EntityHelper) {
     *   return entity.asLiving() !== null;
     * }));
     * if (mobs !== null) {
     *   Chat.log(`${mobs.size()} living entities within the render distance`);
     * }
     * </pre>
     *
     * @param filter the entity filter
     * @return a list of entities that match the specified filter.
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
     * raytrace between two points returning the first block hit.
     * <p>
     * The two triples are the start and the end of a line through the world, and the first
     * block along it is what comes back. Nothing is hit gives {@code null}, and a line that
     * starts inside a block does not count that block, since the trace is between the two
     * points rather than around the start.<br>
     * The {@code fluid} argument decides whether a liquid counts as something to hit: with it
     * {@code false} the line passes through water and lava, and with it {@code true} a liquid
     * in the way is what is returned. There is no partial tick and no shape test, so this is a
     * plain line rather than a swept player box, which is what
     * {@link FPlayer#detailedRayTraceBlock(double, boolean)} is for when a line is not enough.
     * example:
     * <pre>
     * // what is on the line from the player's eyes to a point ten blocks in front
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const eyes = player.getEyePos();
     *   const end = eyes.add(10, 0, 0);
     *   const hit = World.rayTraceBlock(
     *     eyes.getX(), eyes.getY(), eyes.getZ(),
     *     end.getX(), end.getY(), end.getZ(),
     *     false
     *   );
     *   if (hit !== null) {
     *     Chat.log(`the line hits ${hit.getName()}`);
     *   }
     * }
     * </pre>
     *
     * @param x1 the x coordinate of the start of the line
     * @param y1 the y coordinate of the start of the line
     * @param z1 the z coordinate of the start of the line
     * @param x2 the x coordinate of the end of the line
     * @param y2 the y coordinate of the end of the line
     * @param z2 the z coordinate of the end of the line
     * @param fluid whether a liquid in the way counts as the hit
     * @return the first block along the line, or {@code null} if the line hits nothing
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
     * raytrace between two points returning the first entity hit.
     * <p>
     * The two triples are the start and the end of a line through the world, and this finds
     * which living entity along it the line passes through, as opposed to a line that only goes
     * as far as the first thing it meets. The entity's own hitbox is what is tested, not a
     * point, so a line that clips a corner counts.<br>
     * Two things are worth knowing. Only living entities are considered, so an item, an arrow or
     * a boat on the line is not found. And of the entities the line does pass through, the one
     * returned is the one nearest the player rather than the one nearest the start of the
     * line, so the two are not always the same. It is {@code null} with no world loaded, and
     * needs a player for that comparison, so it also gives {@code null} with no player.
     * example:
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
     * note that some server might utilize dimension identifiers for mods to distinguish between worlds.
     * <p>
     * The identifier is the dimension's own, so the overworld, the nether and the end each have
     * one, and a mod that adds its own has its own as well. It follows the server rather than
     * the client's idea of a dimension type, so two servers can be on the same dimension and
     * still differ here.<br>
     * It is {@code null} with no world loaded, so a script reading it wants the usual guard.
     * example:
     * <pre>
     * if (World.isWorldLoaded()) {
     *   Chat.log(`in ${World.getDimension()}`);
     * }
     * </pre>
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
     * the biome the player is standing in.
     * <p>
     * This is the biome at the player's own position, so it is the one the game shows for them
     * and it is what a script wants for "where am I". It is {@code null} with no world or no
     * player, and for a biome at a place rather than at the player,
     * {@link #getBiomeAt(int, int, int)} is the call.
     * example:
     * <pre>
     * const biome = World.getBiome();
     * if (biome !== null) {
     *   Chat.log(`standing in ${biome}`);
     * }
     * </pre>
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
     * ticks processed since world was started.
     * <p>
     * This is the game's own clock, counting up from the moment the world was joined, and it
     * does not care about day or night at all; {@link #getTimeOfDay()} is the one that cycles.
     * A server that does not send its time leaves this at whatever it was.<br>
     * The sentinel for no world is {@code -1}, which is a value the clock can never actually
     * reach, so a script can test for it without ambiguity.
     * example:
     * <pre>
     * const time = World.getTime();
     * Chat.log(time === -1 ? "not in a world" : `${time} ticks since the world started`);
     * </pre>
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
     * ticks passed since world was started INCLUDING those skipped when nights were cut short with sleeping.
     *
     * @return the current dimension's time-of-day ticks, or {@code -1} if no world is loaded.
     * On 26.1+, dimensions with a default clock use it; dimensions without one use the overworld clock.
     * <p>
     * A full cycle is 24000 ticks, which is twenty minutes, and the phases within it are
     * 0 to 1000 for sunrise, 1000 to 11000 for day, 11000 to 13000 for sunset, 13000 to 23000
     * for night and 23000 to 24000 for the rest of the night. It counts what has been slept
     * past as well, so it jumps forward when a night is cut short, which is the difference from
     * {@link #getTime()}.
     * example:
     * <pre>
     * const dayTime = World.getTimeOfDay() % 24000;
     * Chat.log(`${dayTime} ticks into the day, which is ${Math.floor(dayTime / 1000)}`);
     * </pre>
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

    /**
     * Returns the data-driven timelines active in the current dimension.
     * <p>
     * In 26.1, this returns Mojang {@code Holder<Timeline>} objects directly. Each
     * timeline supplies {@code getCurrentTicks}, {@code getTotalTicks}, and
     * {@code getPeriodCount}; pass {@code World.getClockManager()} to those
     * methods. The returned holders retain their registered timeline IDs.
     * <pre>
     * const clockManager = World.getClockManager();
     * for (const timelineHolder of World.getTimelines()) {
     *     const timeline = timelineHolder.value();
     *     Chat.log(`${timelineHolder.getRegisteredName()}: ${timeline.getCurrentTicks(clockManager)}`);
     * }
     * </pre>
     *
     * @return a snapshot of the current dimension's active timeline holders, or an empty list before 26.1 or when no world is loaded.
     * @since 2.0.0
     */
    public List<?> getTimelines() {
        // TODO: When the docgen system is reworked, implement a proper way to version functions and their returns so we can mitigate cross-version docgen issues.
        ClientLevel world = mc.level;
        if (world == null) return Collections.emptyList();
        //? if >=26.1 {
        /*return world.dimensionType().timelines().stream().toList();
        *///? } else {
        return Collections.emptyList();
        //? }
    }

    /**
     * Returns the current world's Mojang {@code ClockManager}.
     * <p>
     * Use this with the raw timelines returned by {@code World.getTimelines()} to read
     * their current, total, and period-count tick values. It is available only on
     * 26.1; earlier targets and an unloaded world return {@code null}.
     *
     * @return the active Mojang clock manager, or {@code null} when unavailable.
     * @since 2.0.0
     */
    @Nullable
    public Object getClockManager() {
        ClientLevel world = mc.level;
        if (world == null) return null;
        //? if >=26.1 {
        /*return world.clockManager();
        *///? } else {
        return null;
        //? }
    }

    /**
     * @return {@code true} if it is daytime, {@code false} otherwise.
     * <p>
     * This is the game's own test rather than a calculation from the clock, so it agrees with
     * what the player sees: it accounts for the weather, for being under water, and for the
     * light level, all of which a time-of-day calculation would miss. It is {@code false} with
     * no world loaded, which is the same as nighttime and is what a script has to watch for.
     * example:
     * <pre>
     * if (World.isWorldLoaded()) {
     *   Chat.log(World.isDay() ? "it is day" : "it is night");
     * }
     * </pre>
     *
     * @since 1.8.4
     */
    public boolean isDay() {
        ClientLevel world = mc.level;
        if (world == null) return false;
        return world.isBrightOutside();
    }

    /**
     * @return {@code true} if it is nighttime, {@code false} otherwise.
     * <p>
     * The counterpart to {@link #isDay()} and on the same terms: it is the game's own test, so
     * it accounts for the weather and the light level, and it is {@code false} with no world
     * loaded. The two are not quite opposites in every case, since a dimension can be neither
     * bright outside nor dark outside.
     * example:
     * <pre>
     * if (World.isWorldLoaded()) {
     *   Chat.log(World.isNight() ? "it is night" : "it is not night");
     * }
     * </pre>
     *
     * @since 1.8.4
     */
    public boolean isNight() {
        ClientLevel world = mc.level;
        if (world == null) return false;
        return world.isDarkOutside();
    }

    /**
     * @return {@code true} if it is raining, {@code false} otherwise.
     * <p>
     * This is whether it is raining where the player is, not whether the weather is set to
     * rain: rain is local to a biome, so a plains can be raining while a desert next to it is
     * not, and this answers for the player's own position. It does not care whether the player
     * can see the sky, so it can be {@code true} while the player is under a roof.<br>
     * It is {@code false} with no world loaded.
     * example:
     * <pre>
     * if (World.isWorldLoaded()) {
     *   if (World.isRaining()) {
     *     Chat.log(`raining here, ${World.isThundering() ? "with thunder" : "without thunder"}`);
     *   }
     * }
     * </pre>
     *
     * @since 1.8.4
     */
    public boolean isRaining() {
        ClientLevel world = mc.level;
        if (world == null) return false;
        return world.isRaining();
    }

    /**
     * @return {@code true} if it is thundering, {@code false} otherwise.
     * <p>
     * Whether there is a storm where the player is, on the same local terms as
     * {@link #isRaining()}. A storm implies rain, so this is never {@code true} where
     * {@code isRaining()} is {@code false}, and it is {@code false} with no world loaded.
     * example:
     * <pre>
     * if (World.isWorldLoaded()) {
     *   if (World.isThundering()) {
     *     Chat.log("a storm overhead, so lightning can strike here");
     *   }
     * }
     * </pre>
     *
     * @since 1.8.4
     */
    public boolean isThundering() {
        ClientLevel world = mc.level;
        if (world == null) return false;
        return world.isThundering();
    }

    /**
     * @return an identifier for the loaded world that is based on the world's name or server ip and
     * thus most likely unique enough to identify a specific world, or
     * {@code "UNKNOWN_NAME"} if no world was found.
     * <p>
     * This is built from what kind of world is open rather than from the world itself, and the
     * shape depends on that. A singleplayer world is {@code LOCAL_} followed by the save's
     * folder name, a realm is {@code REALM_} followed by the realm's name, a LAN game is
     * {@code LAN_} followed by the entry's name, and an ordinary server is its address with
     * the default port stripped and any remaining colon turned into an underscore, so the
     * result has no colon in it and can be used in a file name.<br>
     * Nothing open gives {@code "UNKNOWN_NAME"}, which is a string rather than {@code null},
     * so this never needs a check.
     * example:
     * <pre>
     * // a name to key saved data by, which is stable for a given world
     * const key = World.getWorldIdentifier();
     * Chat.log(`working in ${key}`);
     * </pre>
     *
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
     * where the player respawns.
     * <p>
     * This is the position the player appears at when they die and the respawn button is used,
     * not the block they are standing on, and it is the server's respawn point rather than
     * anything worked out from the world. A bed is what sets it, so it follows the bed the
     * player last slept in and it changes when they sleep somewhere else.<br>
     * It is {@code null} with no world loaded. On 1.21.9 and later it is the player's own
     * respawn position, while earlier targets report the world's shared spawn instead, so the
     * two can differ for a player who has slept in a bed.
     * example:
     * <pre>
     * const respawn = World.getRespawnPos();
     * if (respawn !== null) {
     *   Chat.log(`respawning at ${respawn.getX()}, ${respawn.getY()}, ${respawn.getZ()}`);
     * }
     * </pre>
     *
     * @return respawn position, or {@code null} if no world is loaded
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
     * @return world difficulty as an {@link Integer Integer}. {@code -1} if world is not loaded.
     * <p>
     * The number is the game's own id rather than a name, running from zero for peaceful
     * through one for easy, two for normal and three for hard, and a server or a command can
     * set it to anything. It is the client's view of the setting, which follows the server.
     * example:
     * <pre>
     * const difficulty = World.getDifficulty();
     * if (difficulty >= 3) {
     *   Chat.log("this is a hard world, watch out");
     * }
     * </pre>
     *
     * @since 1.2.6
     */
    public int getDifficulty() {
        ClientLevel world = mc.level;
        if (world == null) return -1;
        return world.getDifficulty().getId();
    }

    /**
     * @return moon phase as an {@link Integer Integer}. {@code -1} if world is not loaded.
     * <p>
     * The number is the moon phase index, running from zero for the full moon through the
     * eight phases, and it changes over several nights rather than every one. The ordering is
     * the game's own, so zero is the full moon rather than the new one, and a script that wants
     * the new moon should test for phase four rather than for zero.
     * example:
     * <pre>
     * const phase = World.getMoonPhase();
     * if (phase === 4) {
     *   Chat.log("new moon, mobs will spawn");
     * }
     * </pre>
     *
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
     * @return sky light as an {@link Integer Integer}. {@code -1} if world is not loaded.
     * <p>
     * The value is the light level at that block from the sky, running from zero to fifteen,
     * and it is what the game uses to decide the block light a mob spawns at and how a light
     * sensitive block behaves. It is a level rather than a brightness, so a full fifteen is
     * daylight and a zero is pitch dark regardless of the block time.<br>
     * The three arguments are block coordinates, and a position the client has no data for gives
     * a level rather than a sentinel, so this is one of the few reads here that does not hand
     * back {@code null} for an unloaded chunk.
     * example:
     * <pre>
     * // how bright is it at the player's feet
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const pos = player.getBlockPos();
     *   Chat.log(`sky light ${World.getSkyLight(pos.getX(), pos.getY(), pos.getZ())}`);
     * }
     * </pre>
     *
     * @param x the x block coordinate
     * @param y the y block coordinate
     * @param z the z block coordinate
     * @since 1.1.2
     */
    public int getSkyLight(int x, int y, int z) {
        ClientLevel world = mc.level;
        if (world == null) return -1;
        return world.getBrightness(LightLayer.SKY, new BlockPos(x, y, z));
    }

    /**
     * @return block light as an {@link Integer Integer}. {@code -1} if world is not loaded.
     * <p>
     * The value is the light level at that block from blocks rather than from the sky, again
     * from zero to fifteen, and it is what a torch or a glowstone contributes. The two light
     * readings are independent and both are wanted: the game uses the larger of them for most
     * purposes, so a dark cave with a torch is dark by sky light and lit by block light.
     * <br>
     * The three arguments are block coordinates, and this is also {@code -1} with no world
     * loaded.
     * example:
     * <pre>
     * // a torch-lit check, for a macro that wants to know where it can build
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const pos = player.getBlockPos();
     *   const lit = World.getBlockLight(pos.getX(), pos.getY(), pos.getZ());
     *   Chat.log(lit > 0 ? "there is a light here" : "no light here");
     * }
     * </pre>
     *
     * @param x the x block coordinate
     * @param y the y block coordinate
     * @param z the z block coordinate
     * @since 1.1.2
     */
    public int getBlockLight(int x, int y, int z) {
        ClientLevel world = mc.level;
        if (world == null) return -1;
        return world.getBrightness(LightLayer.BLOCK, new BlockPos(x, y, z));
    }

    /**
     * plays a sound file using javax's sound stuff.
     * <p>
     * This plays an audio file through the Java sound system rather than through the game, so
     * it is the way to play something the game has no sound event for. The file is looked for
     * relative to the profile's macro folder rather than to the game's working directory, and
     * it has to be in a format the Java sound library understands.<br>
     * The volume is a fraction of full scale, so a value of one is as loud as the system will
     * go and a value of half is half that. The clip is started before this returns and is
     * closed by itself when it finishes, and it is handed back so a script can stop it early or
     * check on it. A file that cannot be read or a format that is not supported raises rather
     * than being silent.
     * example:
     * <pre>
     * // play a sound out of the macro folder, a third as loud as it will go
     * const clip = World.playSoundFile("sounds/beep.wav", 0.33);
     * // and let it finish on its own
     * Chat.log("played");
     * </pre>
     *
     * @param file the file to play, relative to the macro folder
     * @param volume the volume as a fraction of full scale, where one is as loud as possible
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
     * play a game sound, at its normal volume and pitch.
     * <p>
     * This is the short form of the sound calls and the one a script usually wants: the sound
     * plays as the game would play it, with no volume or pitch adjustment and no position, so
     * it is heard at full volume wherever the player is rather than fading with distance. For
     * a sound in the world at a place, {@link #playSound(String, double, double, double,
     * double, double)} is the form.<br>
     * The id is a sound event id with or without the minecraft namespace, and one the game does
     * not know is not an error: it plays as silence. Nothing here reaches the server, so this
     * is a local sound.
     * example:
     * <pre>
     * World.playSound("minecraft:entity.experience_orb.pickup");
     * </pre>
     *
     * @param id the sound event id, with or without the minecraft namespace
     * @see FWorld#playSound(String, double, double, double, double, double)
     * @since 1.1.7
     */
    @DocletReplaceParams("id: SoundId")
    public void playSound(String id) {
        playSound(id, 1F);
    }

    /**
     * play a game sound at a chosen volume.
     * <p>
     * As {@link #playSound(String)}, with the volume as a fraction of what the game would use,
     * so one is normal and a larger number is louder than the game would play it. There is no
     * position, so the sound does not fade with distance.
     * <br>
     * The id is a sound event id with or without the minecraft namespace, and one the game does
     * not know plays as silence.
     * example:
     * <pre>
     * World.playSound("minecraft:block.note_block.bell", 0.5);
     * </pre>
     *
     * @param id the sound event id, with or without the minecraft namespace
     * @param volume the volume as a fraction of the game's own, where one is normal
     * @see FWorld#playSound(String, double, double, double, double, double)
     * @since 1.1.7
     */
    @DocletReplaceParams("id: SoundId, volume: double")
    public void playSound(String id, double volume) {
        playSound(id, volume, 0.25F);
    }

    /**
     * play a game sound at a volume and pitch, with no position.
     * <p>
     * The volume is a fraction of what the game would use and the pitch is a multiplier on
     * the sound's own pitch, so one is the original and a value of two plays it an octave up
     * in feel if not quite in pitch. The default pitch here is a quarter, which is a noticeably
     * low version of the sound, so a script that wants the sound as the game has it should
     * pass one rather than relying on the two argument form.<br>
     * The sound is played through the game's own sound manager as a UI sound, so it is heard at
     * the given volume wherever the player is rather than fading with distance, and it reaches
     * no other player.
     * example:
     * <pre>
     * // a sound at the game's own pitch, a bit quieter
     * World.playSound("block.note_block.pling", 0.7, 1.0);
     * </pre>
     *
     * @param id the sound event id, with or without the minecraft namespace
     * @param volume the volume as a fraction of the game's own, where one is normal
     * @param pitch the pitch as a multiplier on the sound's own, where one is the original
     * @see FWorld#playSound(String, double, double, double, double, double)
     * @since 1.1.7
     */
    @DocletReplaceParams("id: CanOmitNamespace<SoundId>, volume: double, pitch: double")
    public void playSound(String id, double volume, double pitch) {
        SoundEvent sound = SoundEvent.createVariableRangeEvent(ResourceLocation.parse(id));
        assert sound != null;
        mc.execute(() -> mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, (float) pitch, (float) volume)));
    }

    /**
     * plays a minecraft sound using the internal system.
     * <p>
     * This is the positional form: the sound is played in the world at the three coordinates
     * and fades with the distance from there, so it is the one to use for a sound a player
     * ought to hear coming from a place. The coordinates are block positions and can be
     * anywhere in the dimension, loaded or not.<br>
     * The volume is a fraction of what the game would use and the pitch is a multiplier on the
     * sound's own. The sound is played on the master category, so it is not muffled by the
     * player's own settings, and it reaches no other player since it is made up locally.
     * example:
     * <pre>
     * // a sound coming from a block in the world
     * World.playSound("block.anvil_land", 1.0, 1.0, 0, 65, 0);
     * </pre>
     *
     * @param id the sound event id, with or without the minecraft namespace
     * @param volume the volume as a fraction of the game's own, where one is normal
     * @param pitch the pitch as a multiplier on the sound's own, where one is the original
     * @param x the x block coordinate to play it at
     * @param y the y block coordinate to play it at
     * @param z the z block coordinate to play it at
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
     * @return a map of boss bars by the boss bar's UUID.
     * <p>
     * This is the bars the server has sent, so a boss fight's bar appears while the fight is on
     * and the map is empty otherwise. The key is the bar's own identifier as a string, which
     * is what to keep in order to find the same bar again across calls, and the value carries
     * the name, the fill percentage, the colour and the style.<br>
     * The map is a fresh snapshot, so it is a good idea to read the percentage from the same
     * object rather than looking the bar up again, since the two are not read at the same
     * instant. This is never {@code null}, unlike most of the readers here.
     * example:
     * <pre>
     * const bars = World.getBossBars();
     * for (const uuid of Object.keys(bars)) {
     *   const bar = bars.get(uuid);
     *   if (bar) {
     *     Chat.log(`${bar.getName()} at ${bar.getPercent()} percent`);
     *   }
     * }
     * </pre>
     *
     * @since 1.2.1
     */
    public Map<String, BossBarHelper> getBossBars() {
        assert mc.gui != null;
        Map<UUID, LerpingBossEvent> bars = ImmutableMap.copyOf(mc.gui.getBossOverlay().events);
        Map<String, BossBarHelper> out = new HashMap<>();
        for (Map.Entry<UUID, LerpingBossEvent> e : ImmutableList.copyOf(bars.entrySet())) {
            out.put(e.getKey().toString(), new BossBarHelper(e.getValue()));
        }
        return out;
    }

    /**
     * Check whether a chunk is within the render distance and loaded.
     * <p>
     * Both arguments are chunk coordinates, the same divided by sixteen
     * {@link #getChunk(int, int)} takes, and this answers whether the client has been sent that
     * chunk. It is the cheap check to do before anything that walks a region, since a search
     * over a chunk that is not loaded sees nothing and a lookup that forces one is work the
     * server may not agree with.<br>
     * A chunk within the render distance but not yet sent counts as not loaded here, and so
     * does the whole question with no world loaded, since that is the same answer a script
     * wants to act on.
     * example:
     * <pre>
     * // only search the chunks the client actually has
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const cx = Math.floor(player.getX()) >> 4;
     *   const cz = Math.floor(player.getZ()) >> 4;
     *   Chat.log(World.isChunkLoaded(cx, cz) ? "here is loaded" : "here is not loaded");
     * }
     * </pre>
     *
     * @param chunkX the x coordinate of the chunk, not the absolute position
     * @param chunkZ the z coordinate of the chunk, not the absolute position
     * @return {@code true} if the chunk is within the render distance and has been sent
     * @since 1.2.2
     */
    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        ClientLevel world = mc.level;
        if (world == null) return false;
        return world.getChunkSource().hasChunk(chunkX, chunkZ);
    }

    /**
     * @return the current server address as a string ({@code server.address/server.ip:port}).
     * <p>
     * This is the address the client is actually talking to, as the network connection has it,
     * which is not always the address that was typed: a hostname that resolves gives the
     * resolved address, and a proxy or a redirect gives whatever was connected to. That makes
     * it the honest answer to "where am I really connected" rather than "what did the player
     * type".<br>
     * It is {@code null} with no world loaded and with nothing connected, so a singleplayer
     * world gives {@code null} even though a world is open.
     * example:
     * <pre>
     * const address = World.getCurrentServerAddress();
     * if (address !== null) {
     *   Chat.log(`connected to ${address}`);
     * }
     * </pre>
     *
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
     * @return biome at specified location, only works if the block/chunk is loaded.
     * <p>
     * The two arguments are the x and z of a column, and the y is <em>not</em> chosen by the
     * caller: this reads the biome at {@code y = 10}, a hardcoded constant well down in the
     * stone rather than at sea level, which is roughly y=63 in the overworld. That is a poor
     * answer almost everywhere, and a very poor one in a dimension whose biomes vary with
     * height or in a cave. A script that cares about the biome at a particular height wants
     * {@link #getBiomeAt(int, int, int)} instead.
     * <p>
     * A position the client has no data for gives a biome rather than {@code null}, and
     * {@code null} is only for no world loaded.
     * example:
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
     * @since 1.2.2 [Citation Needed]
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
     * @return biome at specified location, only works if the block/chunk is loaded.
     * <p>
     * This is the full form and the one to use when the height matters, since the y is the
     * caller's choice rather than being pinned to the fixed {@code y = 10} that
     * {@link #getBiomeAt(int, int)} uses, which is deep underground rather than at sea level.
     * That matters in a cave, in a mountain biome whose bands change with height, and in any
     * dimension where height is part of the answer.
     * <p>
     * A position the client has no data for gives a biome rather than {@code null}, and
     * {@code null} is only for no world loaded.
     * example:
     * <pre>
     * // the biome at the player's own feet, which is the full form of what the short one guesses
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   Chat.log(`biome at these feet is ${World.getBiomeAt(Math.floor(player.getX()), Math.floor(player.getY()), Math.floor(player.getZ()))}`);
     * }
     * </pre>
     *
     * @param x the x block coordinate
     * @param y the y block coordinate
     * @param z the z block coordinate
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
     * @return best attempt to measure and give the server tps with various timings.
     * <p>
     * This is the four readings in one string: the instant figure, then the one, five and
     * fifteen minute averages. It is a best attempt rather than a measurement the server
     * reported, since it is worked out from the gaps between the server's time updates, so a
     * server that sends those irregularly gives a reading that is rough. Twenty is healthy and
     * a figure well below it means the server is struggling.<br>
     * None of the four is reset on a world change, and all of them start at twenty, so a script
     * that has just joined a server will see twenty rather than a measurement.
     * example:
     * <pre>
     * // the whole readout in one line
     * Chat.log(`server tps: ${World.getServerTPS()}`);
     * </pre>
     *
     * @since 1.2.7
     */
    @DocletReplaceReturn("`${number}, 1M: ${number}, 5M: ${number}, 15M: ${number}`")
    public String getServerTPS() {
        return String.format("%.2f, 1M: %.1f, 5M: %.1f, 15M: %.1f", serverInstantTPS, server1MAverageTPS, server5MAverageTPS, server15MAverageTPS);
    }

    /**
     * @return text helper for the top part of the tab list (above the players)
     * <p>
     * This is the line a server draws above the player list, and it is whatever the server most
     * recently sent rather than something a script has any say over, so a server that sends
     * none leaves it {@code null}. The result is a {@code TextHelper}, so the server's own
     * formatting and any hover or click actions it put in survive.
     * example:
     * <pre>
     * const header = World.getTabListHeader();
     * if (header !== null) {
     *   Chat.log(`the server says: ${header}`);
     * }
     * </pre>
     *
     * @since 1.3.1
     */
    @Nullable
    public TextHelper getTabListHeader() {
        return TextHelper.wrap(((IPlayerListHud) mc.gui.getTabList()).jsmacros_getHeader());
    }

    /**
     * @return text helper for the bottom part of the tab list (below the players)
     * <p>
     * The counterpart to {@link #getTabListHeader()}, and on the same terms: it is whatever the
     * server most recently sent for the line under the player list, and a server that sends
     * none leaves it {@code null}.
     * example:
     * <pre>
     * const footer = World.getTabListFooter();
     * if (footer !== null) {
     *   Chat.log(`the footer says: ${footer}`);
     * }
     * </pre>
     *
     * @since 1.3.1
     */
    @Nullable
    public TextHelper getTabListFooter() {
        return TextHelper.wrap(((IPlayerListHud) mc.gui.getTabList()).jsmacros_getFooter());
    }

    /**
     * Summons the amount of particles at the desired position.
     * <p>
     * This is the short form, and the particles are spread over roughly a tenth of a block in
     * each direction rather than all landing on one point, which is what the default deltas
     * here are. The count is a maximum rather than an exact number, since the game decides
     * how many actually spawn.
     * <p>
     * The particles are made up on the client and reach no other player, so this is something
     * the script's own player sees and nobody else does. An id the game does not know falls
     * back to a generic puff rather than nothing.
     * example:
     * <pre>
     * // a puff of particles at a point in the world
     * World.spawnParticle("minecraft:flame", 0, 65, 0, 20);
     * </pre>
     *
     * @param id    the particle id
     * @param x     the x position to spawn the particle
     * @param y     the y position to spawn the particle
     * @param z     the z position to spawn the particle
     * @param count the amount of particles to spawn
     * @since 1.8.4
     */
    @DocletReplaceParams("id: ParticleId, x: double, y: double, z: double, count: int")
    public void spawnParticle(String id, double x, double y, double z, int count) {
        spawnParticle(id, x, y, z, 0.1, 0.1, 0.1, 1, count, true);
    }

    /**
     * Summons the amount of particles at the desired position with some variation of delta and the
     * given speed.
     * <p>
     * The three deltas are the spread in each direction, so they set how wide an area the
     * particles are scattered over rather than moving each one, and the speed scales them. A
     * delta of zero in every direction puts them all on the same point, which is how a single
     * precise particle rather than a cloud is made.
     * <p>
     * The {@code force} argument is what decides whether a particle further than the game's own
     * render distance for particles is shown; with it on, the particle is shown at any distance
     * the player can see, and with it off the usual limit applies. It does not reach the
     * server, so this is a local effect that no other player sees, and an id the game does not
     * know falls back to a generic puff. Nothing happens at all with no player.
     * example:
     * <pre>
     * // a tight burst of flame, all on one point, shown at any distance
     * World.spawnParticle("flame", 0, 65, 0, 0, 0, 0, 1, 10, true);
     *
     * // and a wide cloud of it, spread over four blocks
     * World.spawnParticle("flame", 0, 65, 0, 2, 2, 2, 0.1, 100, false);
     * </pre>
     *
     * @param id     the particle id
     * @param x      the x position to spawn the particle
     * @param y      the y position to spawn the particle
     * @param z      the z position to spawn the particle
     * @param deltaX the x variation of the particle
     * @param deltaY the y variation of the particle
     * @param deltaZ the z variation of the particle
     * @param speed  the speed of the particle
     * @param count  the number of particles to spawn
     * @param force  whether to show the particle if it's more than 32 blocks away
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
     * @return the raw minecraft world.
     * <p>
     * This is the game's own level object rather than a wrapper, so it is where the parts of
     * the world nothing here wraps are reached, at the cost of the names being the
     * intermediary mapped ones and most of the state on it being {@code null} outside a world.
     * <br>
     * Like {@link FClient#getMinecraft()}, the shipped TypeScript definitions describe the JDK
     * and not the game, so this comes back typed as untyped and a value taken from it has to
     * be cast before it is passed anywhere. It is {@code null} with no world loaded.
     * example:
     * <pre>
     * // the raw level, for something nothing here wraps
     * const level = World.getRaw();
     * if (level !== null) {
     *   Chat.log(`the level's time is ${level.getGameTime()}`);
     * }
     * </pre>
     *
     * @since 1.9.1
     */
    @Nullable
    public ClientLevel getRaw() {
        return mc.level;
    }

    /**
     * @return best attempt to measure and give the server tps.
     * <p>
     * The single most recent reading, worked out from the gap between the last two of the
     * server's time updates. It is the number that moves and the one to watch for a lag spike,
     * and it is noisy for that reason: a single long frame moves it sharply. The three
     * averaged forms are the ones to read when a script wants a trend.
     * <p>
     * It starts at twenty and is not reset on a world change, so a script that has just joined
     * sees twenty rather than a measurement, and a server that sends no time updates at all
     * leaves it there.
     * example:
     * <pre>
     * // watch the instantaneous figure, which is the one that spikes
     * // twenty is healthy, so a reading under it means the server is behind
     * Chat.log(`server right now: ${World.getServerInstantTPS()} tps`);
     * </pre>
     *
     * @since 1.2.7
     */
    public double getServerInstantTPS() {
        return serverInstantTPS;
    }

    /**
     * @return best attempt to measure and give the server tps over the previous 1 minute average.
     * <p>
     * The mean of every instant reading in the last sixty seconds, so it is the shortest of
     * the three windows and the first to show a lag spike. It is the figure to read when a
     * script wants to know whether a server is healthy over the last little while without
     * being distracted by one bad frame.
     * example:
     * <pre>
     * // a one minute view, for a macro that wants a stable number
     * Chat.log(`server at ${World.getServer1MAverageTPS()} tps over the last minute`);
     * </pre>
     *
     * @since 1.2.7
     */
    public double getServer1MAverageTPS() {
        return server1MAverageTPS;
    }

    /**
     * @return best attempt to measure and give the server tps over the previous 5 minute average.
     * <p>
     * Built from the one minute average rather than from the instant readings, so it reacts
     * more slowly than {@link #getServer1MAverageTPS()} and a lag spike takes most of a window
     * to wash out of it. That is the point of having it: a server that has been struggling for
     * a while looks healthy on the one minute figure and not on this one.
     * example:
     * <pre>
     * // a five minute view, which a short spike will not upset
     * Chat.log(`server at ${World.getServer5MAverageTPS()} tps over the last five minutes`);
     * </pre>
     *
     * @since 1.2.7
     */
    public double getServer5MAverageTPS() {
        return server5MAverageTPS;
    }

    /**
     * @return best attempt to measure and give the server tps over the previous 15 minute average.
     * <p>
     * The longest of the three windows and the slowest to react, so it is the figure to read
     * when a script wants to know whether a server has been healthy across a whole session.
     * The window has to have partly filled before it means anything, so early in a session it
     * is close to twenty however the server is actually doing.
     * example:
     * <pre>
     * // a fifteen minute view, for a macro that runs a long way
     * // it sits near twenty while the server is healthy and only drifts down when it struggles
     * Chat.log(`server at ${World.getServer15MAverageTPS()} tps over the last fifteen minutes`);
     * </pre>
     *
     * @since 1.2.7
     */
    public double getServer15MAverageTPS() {
        return server15MAverageTPS;
    }

}
