package com.jsmacrosce.jsmacros.client.api.library.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import org.jetbrains.annotations.Nullable;

import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.api.PlayerInput;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.access.ISignEditScreen;
import com.jsmacrosce.jsmacros.client.api.classes.inventory.Inventory;
import com.jsmacrosce.jsmacros.client.api.helper.InteractionManagerHelper;
import com.jsmacrosce.jsmacros.client.api.helper.StatsHelper;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockDataHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.HitResultHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.ClientPlayerEntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.movement.MovementDummy;
import com.jsmacrosce.jsmacros.client.movement.MovementQueue;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;
import com.jsmacrosce.jsmacros.core.library.Library;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Functions for getting and modifying the player's state.
 * <p>
 * An instance of this class is passed to scripts as the {@code Player} variable.
 * <br>
 * "The player" here is always the local player, and most of this library assumes there is one.
 * Nearly every call here is only meaningful in a world: the entity is {@code null} at the title
 * screen, the interaction manager is {@code null} outside a world, and the checks on them are
 * assertions rather than guards, so on a released game without assertions enabled some of these
 * will raise instead of returning something empty. {@link FWorld#isWorldLoaded()} is the cheap
 * test to put in front of a whole script.<br>
 * The library groups into what it does. {@link #getPlayer()} and
 * {@link #getInteractionManager()} are the two handles everything else hangs off: the first is
 * the player's own body, position, health and effects, the second is how the client interacts
 * with the world, so breaking blocks, attacking and using items go through it. The
 * {@code rayTrace} calls answer what the player is looking at. {@link #createPlayerInput()} and
 * the rest of that family build the synthetic input a movement macro replays, and
 * {@link #predictInput(PlayerInput)} and {@link #predictInputs(PlayerInput[])} say where those
 * inputs would lead without having to send them. {@link #takeScreenshot(String,
 * MethodWrapper)} and {@link #takePanorama(String, int, int, MethodWrapper)} are the capture
 * calls, and {@link #getStatistics()} is the statistics screen's data.
 * <br>
 * A note on the input family, because the names are misleading: {@link #createPlayerInput()} and
 * friends build a value, they do not move anything. Nothing happens until the value is handed to
 * {@link #addInput(PlayerInput)} or {@link #addInputs(PlayerInput[])}, and the queue is worked
 * through one entry per tick afterwards, so several inputs added together play out over several
 * ticks rather than all at once. {@link #moveForward(double)} and its three siblings are the
 * shortcuts for the common cases.
 * example:
 * <pre>
 * // all of this only makes sense in a world
 * if (World.isWorldLoaded()) {
 *   // what is the player standing in
 *   const player = Player.getPlayer();
 *   if (player !== null) {
 *     const under = World.getBlock(player.getBlockPos().down());
 *     if (under !== null) {
 *       Chat.log(`standing in ${under.getName()}`);
 *     }
 *   }
 *
 *   // walk forward for a second of game time
 *   Player.moveForward(0);
 *   Client.waitTick(20);
 *   Player.clearInputs();
 * }
 * </pre>
 *
 * @author Wagyourtail
 */
@Library("Player")
@SuppressWarnings("unused")
public class FPlayer extends BaseLibrary {
    private static final Minecraft mc = Minecraft.getInstance();

    public FPlayer(Core<?, ?> runner) {
        super(runner);
    }

    /**
     * a handle on the player's own inventory, for moving items around.
     * <p>
     * This is the inventory the player carries, not whatever container happens to be open: the
     * container is given its own type by the screen it came from, and this is the player's own
     * either way. The returned helper is a different type for a creative player than for a
     * survival one, so a script that works on both wants to check
     * {@link Inventory#isContainer()} or the container's name before assuming a layout.<br>
     * Nothing here is a copy: dropping, swapping and clicking are real actions that the server
     * hears about.
     * example:
     * <pre>
     * if (World.isWorldLoaded()) {
     *   const inv = Player.openInventory();
     *   const held = inv.getHeld();
     *   if (held !== null) {
     *     Chat.log(`holding ${held.getName()}`);
     *   }
     * }
     * </pre>
     *
     * @return the Inventory handler
     * @see Inventory
     */
    public Inventory<?> openInventory() {
        assert mc.player != null && mc.player.getInventory() != null;
        return Inventory.create();
    }

    /**
     * the local player as an entity, for position, health, effects and the rest.
     * <p>
     * This is the body the game itself is moving, not a copy, so setting a position or an effect
     * through it is a real change and the game will notice on the next tick. It is {@code null}
     * whenever there is no player, which is every screen other than being in a world, so a script
     * that is not already inside a world event wants a check.
     * <p>
     * Almost every change here wants the main thread, so this is a natural place to be inside a
     * {@link FClient#runOnMainThread(MethodWrapper)}.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   Chat.log(`${player.getX()}, ${player.getY()}, ${player.getZ()}`);
     *   Chat.log(`health ${player.getHealth()}`);
     * }
     * </pre>
     *
     * @return the player entity wrapper, or {@code null} when there is no player
     * @see ClientPlayerEntityHelper
     * @since 1.0.3
     */
    @Nullable
    public ClientPlayerEntityHelper<LocalPlayer> getPlayer() {
        if (mc.player == null) {
            return null;
        }
        return new ClientPlayerEntityHelper<>(mc.player);
    }

    /**
     * a handle on how the client interacts with the world.
     * <p>
     * This is the one to reach for anything that touches the world as the player: breaking a
     * block, attacking an entity, using an item, right clicking a block, and the target
     * selection all of those share. It is {@code null} outside a world.
     * <p>
     * Several of its calls take an {@code await} argument, which blocks until the server has
     * answered, and that is the thing that cannot be done from the main thread.
     * example:
     * <pre>
     * const interactions = Player.getInteractionManager();
     * if (interactions !== null) {
     *   // break whatever the player is looking at
     *   interactions.breakBlock();
     * }
     * </pre>
     *
     * @return a handle on the interaction manager, or {@code null} when there is no world
     * @since 1.9.0
     */
    @Nullable
    public InteractionManagerHelper getInteractionManager() {
        return mc.gameMode == null ? null : new InteractionManagerHelper(mc.gameMode);
    }

    /**
     * alias for {@link FPlayer#getInteractionManager()}
     * <p>
     * Exactly the same call, under the shorter name, and like it this is {@code null} outside a
     * world.
     * example:
     * <pre>
     * const interactions = Player.interactions();
     * if (interactions !== null) {
     *   const block = interactions.getTargetedBlock();
     *   if (block !== null) {
     *     Chat.log(`looking at a block, ${block.getX()}, ${block.getY()}, ${block.getZ()}`);
     *   }
     * }
     * </pre>
     *
     * @return a handle on the interaction manager, or {@code null} when there is no world
     * @since 1.9.0
     */
    @Nullable
    public InteractionManagerHelper interactions() {
        return getInteractionManager();
    }

    /**
     * @return the player's current gamemode.
     * <p>
     * The mode is the one the client believes it is in, which is not always the one the server
     * has the player in, and it is a lower case name: {@code survival}, {@code creative},
     * {@code adventure} or {@code spectator}.
     * example:
     * <pre>
     * if (World.isWorldLoaded()) {
     *   Chat.log(`in ${Player.getGameMode()}`);
     * }
     * </pre>
     *
     * @since 1.0.9
     */
    @DocletReplaceReturn("Gamemode")
    public String getGameMode() {
        assert mc.gameMode != null;
        GameType mode = mc.gameMode.getPlayerMode();
        return mode.getName();
    }

    /**
     * @param gameMode possible values are survival, creative, adventure, spectator (case insensitive)
     * <p>
     * This sets the mode on the client only. On a server that does not allow the change the
     * server will put it back on the next packet, so this is a way to ask rather than a way to
     * force, and a name the game does not know leaves the mode as it was.
     * example:
     * <pre>
     * Player.setGameMode("creative");
     * </pre>
     *
     * @since 1.8.4
     */
    @DocletReplaceParams("gameMode: Gamemode")
    public void setGameMode(String gameMode) {
        assert mc.gameMode != null;
        mc.gameMode.setLocalMode(GameType.byName(gameMode.toLowerCase(Locale.ROOT), mc.gameMode.getPlayerMode()));
    }

    /**
     * the block the player is looking at, or {@code null} for nothing.
     * <p>
     * The trace starts at the player's eyes and runs along where they are looking, and the
     * distance is how far it goes. A trace that reaches nothing, or that only reaches a block the
     * game has no data for, gives {@code null} rather than a block, so the result always needs
     * checking.<br>
     * The {@code fluid} argument decides whether a liquid counts as something to hit. With it
     * {@code false} the trace passes through water and lava, and with it {@code true} a liquid
     * in the way is what comes back. This is the block the <em>player</em> is looking at, which
     * is not the same as what the interaction manager has targeted, since
     * {@link InteractionManagerHelper#setTarget(EntityHelper)} and its siblings can override
     * that.
     * example:
     * <pre>
     * // what am I looking at
     * const block = Player.rayTraceBlock(5, false);
     * if (block !== null) {
     *   const pos = block.getBlockPos();
     *   Chat.log(`looking at ${block.getName()} at ${pos.getX()}, ${pos.getY()}, ${pos.getZ()}`);
     * }
     * </pre>
     *
     * @param distance how far to trace, in blocks
     * @param fluid    whether a liquid in the way counts as the hit
     * @return the block/liquid the player is currently looking at, or {@code null} if nothing was hit
     * @see BlockDataHelper
     * @since 1.0.5
     */
    @Nullable
    public BlockDataHelper rayTraceBlock(double distance, boolean fluid) {
        assert mc.level != null;
        assert mc.player != null;
        BlockHitResult h = (BlockHitResult) mc.player.pick(distance, 0, fluid);
        if (h.getType() == HitResult.Type.MISS) {
            return null;
        }
        BlockState b = mc.level.getBlockState(h.getBlockPos());
        BlockEntity t = mc.level.getBlockEntity(h.getBlockPos());
        if (b.getBlock().equals(Blocks.VOID_AIR)) {
            return null;
        }
        return new BlockDataHelper(b, t, h.getBlockPos());
    }

    /**
     * the full result of tracing towards where the player is looking.
     * <p>
     * This is the same trace as {@link #rayTraceBlock(double, boolean)} and takes the same two
     * arguments, but it hands back the whole hit rather than just the block, which is what
     * carries the exact point on the block's face that was hit and the side of it. That is what
     * a placement macro needs and what the simpler call throws away.
     * <p>
     * Unlike the simpler call this never gives {@code null}: a trace that hits nothing comes
     * back as a result whose type is a miss, so the type is what to check.
     * example:
     * <pre>
     * // the exact point on the block face the crosshair is on
     * const hit = Player.detailedRayTraceBlock(5, false);
     * if (!hit.isMissed()) {
     *   const pos = hit.getPos();
     *   const side = hit.getSide();
     *   Chat.log(`hit the ${side} side at ${pos.getX()}, ${pos.getY()}, ${pos.getZ()}`);
     * }
     * </pre>
     *
     * @param distance how far to trace, in blocks
     * @param fluid    whether a liquid in the way counts as the hit
     * @return the raycast result, whose type says whether anything was hit
     * @since 1.9.1
     */
    public HitResultHelper.Block detailedRayTraceBlock(double distance, boolean fluid) {
        assert mc.level != null;
        assert mc.player != null;
        return new HitResultHelper.Block((BlockHitResult) mc.player.pick(distance, 0, fluid));
    }

    /**
     * the entity the camera is currently looking at, whatever the interaction manager thinks.
     * <p>
     * This reads the crosshair pick the game fills in as part of rendering, so it only ever
     * agrees with what the player can see. That is the problem with it: the pick belongs to the
     * <em>camera</em>, so in first person there is often no camera entity to pick from and this
     * gives {@code null} even when the player is plainly looking at a mob. An interaction
     * manager target does not change it, it can only make the two disagree.<br>
     * Use {@link #rayTraceEntity(int)} or {@link InteractionManagerHelper#getTargetedEntity()}
     * instead, both of which trace themselves rather than reading what was drawn.
     * example:
     * <pre>
     * // the crosshair pick, which is null more often than you would expect
     * const picked = Player.rayTraceEntity();
     * if (picked !== null) {
     *   Chat.log(`crosshair is over ${picked.getName()}`);
     * }
     * </pre>
     *
     * @return the entity the camera is currently looking at. can be affected by {@link InteractionManagerHelper#setTarget(EntityHelper)}
     * @see EntityHelper
     * @deprecated use {@link FPlayer#rayTraceEntity(int)} or {@link InteractionManagerHelper#getTargetedEntity()} instead
     * @since 1.0.5
     */
    @Deprecated
    @Nullable
    public EntityHelper<?> rayTraceEntity() {
        if (mc.crosshairPickEntity != null) {
            return EntityHelper.create(mc.crosshairPickEntity);
        } else {
            return null;
        }
    }

    /**
     * the entity the player is looking at, traced fresh, or {@code null} for nothing.
     * <p>
     * The trace starts at the player's eyes and runs along where they are looking, and the
     * distance is how far it reaches. This is the form to use: unlike the no argument
     * {@link #rayTraceEntity()} it traces rather than reading what the renderer happened to
     * pick, so it works in first person, and it is not affected by an interaction manager
     * target.<br>
     * A trace that reaches nothing gives {@code null}, and a trace that reaches a block rather
     * than an entity does too.
     * example:
     * <pre>
     * // what mob am I looking at
     * const entity = Player.rayTraceEntity(5);
     * if (entity !== null) {
     *   Chat.log(`looking at ${entity.getName()} at ${entity.getX()}, ${entity.getY()}, ${entity.getZ()}`);
     * }
     * </pre>
     *
     * @param distance how far to trace, in blocks
     * @return entity the player entity is currently looking at (if any).
     * @since 1.8.3
     */
    @Nullable
    public EntityHelper<?> rayTraceEntity(int distance) {
        return DebugRenderer.getTargetedEntity(mc.player, distance).map(EntityHelper::create).orElse(null);
    }

    /**
     * Write to a sign screen if a sign screen is currently open.<br>
     * If the given string is null, the text will remain unchanged.
     * <p>
     * The four arguments are the four lines, top to bottom, and a {@code null} leaves that line
     * as it is rather than clearing it, so a script can fill in only what it wants to change.
     * The return says whether a sign screen was open at all, which is {@code false} when the
     * player is not editing a sign, and that is the only thing the return means: it does not
     * say whether the text was written, and a sign that is not being edited simply has nothing
     * to write to.
     * <p>
     * This is the four at once form; {@link #writeSign(int, String)} writes one line and is
     * usually the better shape for a script that builds the lines in a loop.
     * example:
     * <pre>
     * // fill in a sign the player has open, and hear about it if there is not one
     * if (!Player.writeSign("line one", "line two", null, null)) {
     *   Chat.log("no sign is open");
     * }
     * </pre>
     *
     * @param l1 the first line of the sign, or {@code null} to leave it unchanged
     * @param l2 the second line of the sign, or {@code null} to leave it unchanged
     * @param l3 the third line of the sign, or {@code null} to leave it unchanged
     * @param l4 the fourth line of the sign, or {@code null} to leave it unchanged
     * @return of success (sign screen is open).
     * @since 1.2.2
     */
    public boolean writeSign(@Nullable String l1, @Nullable String l2, @Nullable String l3, @Nullable String l4) {
        if (mc.screen instanceof SignEditScreen screen) {
            if (l1 != null) ((ISignEditScreen) screen).jsmacros_setLine(0, l1);
            if (l2 != null) ((ISignEditScreen) screen).jsmacros_setLine(1, l2);
            if (l3 != null) ((ISignEditScreen) screen).jsmacros_setLine(2, l3);
            if (l4 != null) ((ISignEditScreen) screen).jsmacros_setLine(3, l4);
            return true;
        }
        return false;
    }

    /**
     * Write a line to a sign screen if a sign screen is currently open.
     * <p>
     * This writes one line, counting from zero at the top, and is the shape to use when the
     * lines come from a loop. The index is checked before anything else happens, so an index
     * outside zero to three raises {@link IndexOutOfBoundsException} even when no sign is open.
     * A {@code null} message is not accepted here, unlike in the four argument
     * {@link #writeSign(String, String, String, String)}, so a line that should be left alone
     * has to be skipped rather than passed as {@code null}.
     * <p>
     * The return says whether a sign screen was open, and nothing more; a sign that is not
     * being edited means nothing was written and nothing is reported beyond the {@code false}.
     * example:
     * <pre>
     * // write the lines a script has; the index counts from zero at the top
     * const lines = ["shop", "buy", "sell", "hours 9 to 5"];
     * lines.forEach(function (line, index) {
     *   Player.writeSign(index, line);
     * });
     * </pre>
     *
     * @param index the index of the message. should be in between 0 and 3
     * @param message the message to write
     * @return of success (sign screen is open).
     * @throws IndexOutOfBoundsException if the index is outside zero to three
     * @since 2.0.0
     */
    public boolean writeSign(int index, String message) {
        if ((index & ~3) != 0) {
            throw new IndexOutOfBoundsException("Index should be in between 0 and 3!!  provided: " + index);
        }
        if (mc.screen instanceof SignEditScreen screen) {
            ((ISignEditScreen) screen).jsmacros_setLine(index, message);
            return true;
        }
        return false;
    }

    /**
     * take a screenshot and let the game name the file.
     * <p>
     * This captures what is on screen into a file under the folder, and the game picks the name,
     * so a script that takes several cannot collide with itself. The folder is relative to the
     * profile's macro folder, so it is not relative to the game's working directory.
     * <p>
     * The capture happens on the next frame rather than during the call, so the callback runs a
     * little later and is given a message saying where the file went. The callback is optional.
     * example:
     * <pre>
     * // capture, and be told the path afterwards
     * Player.takeScreenshot("screenshots", JavaWrapper.methodToJava(function (text) {
     *   Chat.log(text);
     * }));
     * </pre>
     *
     * @param folder the folder to save into, relative to the macro folder
     * @param callback calls your method as a {@link Consumer}{@code <}{@link TextHelper}{@code >} with
     *                 the message naming the saved file, or {@code null} for none
     * @see #takeScreenshot(String, String, MethodWrapper)
     * @since 1.2.6
     */
    public void takeScreenshot(String folder, @Nullable MethodWrapper<TextHelper, Object, Object, ?> callback) {
        assert folder != null;
        Screenshot.grab(new File(runner.config.macroFolder, folder), mc.getMainRenderTarget(),
                (text) -> {
                    if (callback != null) {
                        callback.accept(TextHelper.wrap(text));
                    }
                });
    }

    /**
     * Take a screenshot and save to a file.
     * <p>
     * {@code file} is the optional one, typescript doesn't like it not being the last one that's optional
     * <p>
     * This is the same capture with a name of the script's choosing, so a script that wants a
     * known filename uses this rather than the shorter form. The folder is relative to the
     * profile's macro folder. The capture happens on the next frame, so the callback runs a
     * little later, and it is optional.
     * example:
     * <pre>
     * // a named capture, for a macro that keeps a fixed set of pictures
     * Player.takeScreenshot("screenshots", "before.png", JavaWrapper.methodToJava(function (text) {
     *   Chat.log(text);
     * }));
     * </pre>
     *
     * @param folder the folder to save into, relative to the macro folder
     * @param file the file name to save under, within that folder
     * @param callback calls your method as a {@link Consumer}{@code <}{@link TextHelper}{@code >} with
     *                 the message naming the saved file, or {@code null} for none
     * @since 1.2.6
     */
    public void takeScreenshot(String folder, String file, @Nullable MethodWrapper<TextHelper, Object, Object, ?> callback) {
        assert folder != null && file != null;
        Screenshot.grab(
                new File(runner.config.macroFolder, folder),
                file,
                mc.getMainRenderTarget(),
                //? if >1.21.5 {
                0,
                //?}
                (text) -> {
                    if (callback != null) {
                        callback.accept(TextHelper.wrap(text));
                    }
                });
    }

    /**
     * take a panorama, which is the world rendered as a strip of cube faces.
     * <p>
     * This is the same picture the panorama button on the world selection screen makes, and it
     * is stitched together from several renders, so it takes noticeably longer than a plain
     * screenshot and holds the game up while it does. The folder is relative to the profile's
     * macro folder, and the game picks the file name.<br>
     * The width and height are only honoured on the older targets, where the panorama was
     * rendered at exactly that size; on 1.21.6 and later the game builds the strip itself and
     * these two arguments are ignored. The callback receives the message naming the file that
     * was written.
     * example:
     * <pre>
     * // a panorama of the current world, for a macro that keeps a record of where it has been
     * Player.takePanorama("panoramas", 512, 256, JavaWrapper.methodToJava(function (text) {
     *   Chat.log(text);
     * }));
     * </pre>
     *
     * @param folder   the folder to save the screenshot to, relative to the macro folder
     * @param width    the width of the panorama, ignored on 1.21.6 and later
     * @param height   the height of the panorama, ignored on 1.21.6 and later
     * @param callback calls your method as a {@link Consumer}{@code <}{@link TextHelper}{@code >} with
     *                 the message naming the saved file, or {@code null} for none
     * @since 1.8.4
     */
    public void takePanorama(String folder, int width, int height, @Nullable MethodWrapper<TextHelper, Object, Object, ?> callback) {
        assert folder != null;
        Component result = mc.grabPanoramixScreenshot(
                new File(runner.config.macroFolder, folder)
                //? if <=1.21.5 {
                /*, width,
                height
                *///?}
        );
        if (callback != null) {
            callback.accept(TextHelper.wrap(result));
        }
    }

    /**
     * the player's statistics, which is what the statistics screen shows.
     * <p>
     * This reads the same counters the game keeps for the advancements and the statistics
     * screen, so a value here is the game's own count rather than something a script has been
     * keeping. The counters are updated by the game, so a value read immediately after an
     * action may not have caught up yet; the game only refreshes them at the end of a tick.
     * <p>
     * This needs a player, so it is only meaningful in a world.
     * example:
     * <pre>
     * if (World.isWorldLoaded()) {
     *   const stats = Player.getStatistics();
     *   Chat.log(`${stats.getBlockMined("minecraft:stone")} stone mined`);
     *   // the whole thing, as the statistics screen would print it
     *   for (const stat of stats.getStatList()) {
     *     Chat.log(`${stat}: ${stats.getFormattedStatValue(stat)}`);
     *   }
     * }
     * </pre>
     *
     * @return a handle on the player's statistics
     * @since 2.0.0
     */
    public StatsHelper getStatistics() {
        assert mc.player != null;
        return new StatsHelper(mc.player.getStats());
    }

    /**
     * how far the player can reach.
     * <p>
     * This is the current block interaction range, which is the game's own value and so moves
     * with whatever has changed it: a server that enforces a shorter reach, a mod, or a
     * creative player's longer one. It is the number an attack or an interaction has to be
     * within, and it is a distance in blocks rather than a reach in the sense of a ray trace
     * against a block face, which is a slightly different test.
     * <p>
     * This needs a player, so it is only meaningful in a world.
     * example:
     * <pre>
     * if (World.isWorldLoaded()) {
     *   Chat.log(`reach is ${Player.getReach()}`);
     * }
     * </pre>
     *
     * @return the current reach distance of the player.
     * @since 1.8.4
     */
    public double getReach() {
        assert mc.player != null;
        return mc.player.blockInteractionRange();
    }

    /**
     * Creates a new PlayerInput object, with everything at zero.
     * <p>
     * This is a value and nothing more: no movement, no buttons, pitch zero. The fields on it
     * are public and writable, so a script normally makes one of these and then assigns to the
     * fields it cares about, which is what the field names in {@link PlayerInput} are for. It
     * does nothing until it is handed to {@link #addInput(PlayerInput)}.<br>
     * A fresh input is also what {@link #moveForward(double)} and its siblings build internally.
     * example:
     * <pre>
     * // one tick of walking forward
     * const input = Player.createPlayerInput();
     * input.movementForward = 1.0;
     * Player.addInput(input);
     * </pre>
     *
     * @return a new empty PlayerInput
     * @see PlayerInput
     * @since 1.4.0
     */
    public PlayerInput createPlayerInput() {
        return new PlayerInput();
    }

    /**
     * Creates a new PlayerInput object from movement and a bearing.
     * <p>
     * This is the form that only moves and turns, and it leaves pitch, jumping, sneaking and
     * sprinting at zero. The movement values are the same -1 to 1 scale the keyboard gives,
     * where positive forward is the W key. The yaw is an absolute bearing in degrees rather
     * than a turn, so it sets where the player is facing rather than how far they turn.
     * <p>
     * The result is a value and does nothing until it is handed to
     * {@link #addInput(PlayerInput)}.
     * example:
     * <pre>
     * // strafe right while facing due south
     * Player.addInput(Player.createPlayerInput(0.0, -1.0, 0.0));
     * </pre>
     *
     * @param movementForward  1 = forward input (W); 0 = no input; -1 = backward input (S)
     * @param movementSideways 1 = left input (A); 0 = no input; -1 = right input (D)
     * @param yaw              yaw of the player, as an absolute bearing in degrees
     * @return a new PlayerInput with the movement and bearing set
     * @see PlayerInput
     * @since 1.4.0
     */
    public PlayerInput createPlayerInput(double movementForward, double movementSideways, double yaw) {
        return new PlayerInput((float) movementForward, (float) movementSideways, (float) yaw);
    }

    /**
     * Creates a new PlayerInput object from forward movement and two buttons.
     * <p>
     * This is the shortest form that sets more than one thing, and it is a shortcut for the full
     * one with the sideways movement left at zero. The yaw is an absolute bearing in degrees.
     * Sneaking is left off, since this form has no argument for it, so a script that wants to
     * sneak wants the full form or wants to assign the field afterwards.
     * <p>
     * The result is a value and does nothing until it is handed to
     * {@link #addInput(PlayerInput)}.
     * example:
     * <pre>
     * // one tick of sprinting forward
     * Player.addInput(Player.createPlayerInput(1.0, 0.0, true, true));
     * </pre>
     *
     * @param movementForward 1 = forward input (W); 0 = no input; -1 = backward input (S)
     * @param yaw             yaw of the player, as an absolute bearing in degrees
     * @param jumping         whether the jump button is held
     * @param sprinting       whether the sprint button is held
     * @return a new PlayerInput with the forward movement, bearing and buttons set
     * @see PlayerInput
     * @since 1.4.0
     */
    public PlayerInput createPlayerInput(double movementForward, double yaw, boolean jumping, boolean sprinting) {
        return new PlayerInput((float) movementForward, (float) yaw, jumping, sprinting);
    }

    /**
     * Creates a new PlayerInput object with everything a tick of input can carry.
     * <p>
     * This is the full form, and the one a script usually ends up at, because the other two
     * are shortcuts into it. Every field of a {@code PlayerInput} is set by it: both movement
     * axes, both angles and all three buttons. The angles are absolute in degrees, with pitch
     * positive looking down, and the movement values are the -1 to 1 scale the keyboard gives.
     * <p>
     * The result is a value and does nothing until it is handed to
     * {@link #addInput(PlayerInput)}, and it is worked through one tick at a time after that.
     * example:
     * <pre>
     * // one tick of sneaking backwards while looking down a little
     * Player.addInput(Player.createPlayerInput(-1.0, 0.0, 0.0, 10.0, false, true, false));
     * </pre>
     *
     * @param movementForward  1 = forward input (W); 0 = no input; -1 = backward input (S)
     * @param movementSideways 1 = left input (A); 0 = no input; -1 = right input (D)
     * @param yaw              yaw of the player
     * @param pitch            pitch of the player
     * @param jumping          jump input
     * @param sneaking         sneak input
     * @param sprinting        sprint input
     * @return a new PlayerInput with every field set
     * @see PlayerInput
     * @since 1.4.0
     */
    public PlayerInput createPlayerInput(double movementForward, double movementSideways, double yaw, double pitch, boolean jumping, boolean sneaking, boolean sprinting) {
        return new PlayerInput(movementForward, movementSideways, yaw, pitch, jumping, sneaking, sprinting);
    }

    /**
     * Parses each row of CSV string into a {@code PlayerInput}.
     * The capitalization of the header matters.<br>
     * About the columns:
     * <ul>
     *   <li> {@code movementForward} and {@code movementSideways} as a number</li>
     *   <li>{@code yaw} and {@code pitch} as an absolute number</li>
     *   <li>{@code jumping}, {@code sneaking} and {@code sprinting} have to be boolean</li>
     * </ul>
     * <p>
     * The separation must be a "," it's a csv...(but spaces don't matter)<br>
     * Quoted values don't work
     * <p>
     * Each row of the string becomes one input, so this is how a long recorded walk is turned
     * back into the sequence of inputs that produced it. The header names the fields rather
     * than a position, so a row may leave one out and it takes its default, but the
     * capitalization has to match exactly.<br>
     * The values are the same as the arguments to
     * {@link #createPlayerInput(double, double, double, double, boolean, boolean, boolean)}:
     * both movements and both angles as numbers, and the three buttons as booleans. Spaces
     * around the commas do not matter, but a quoted value does not work, so a line containing a
     * comma inside a field cannot be written this way.
     * <p>
     * Nothing is queued by this. The inputs are values, and they have to be handed to
     * {@link #addInput(PlayerInput)} one at a time, or to
     * {@link #addInputs(PlayerInput[])} as an array, before the player does anything with them.
     * example:
     * <pre>
     * // replay a recorded walk, which the parser hands back as a Java list
     * const walk = Player.createPlayerInputsFromCsv(
     *   "movementForward,movementSideways,yaw,pitch,jumping,sneaking,sprinting\n" +
     *   "1,0,0,0,true,false,true\n" +
     *   "1,0,0,0,false,false,true"
     * );
     * // it is a Java list, so it is read with get and size rather than indexed
     * let i = 0;
     * while (i !== walk.size()) {
     *   Player.addInput(walk.get(i));
     *   i += 1;
     * }
     * </pre>
     *
     * @param csv CSV string to be parsed, one input per row under a header row
     * @return one PlayerInput per row of the string, in the order they appear
     * @throws NoSuchFieldException if a header names a field the input does not have
     * @throws IllegalAccessException if a field cannot be read
     * @see PlayerInput#PlayerInput(float, float, float, float, boolean, boolean, boolean)
     * @since 1.4.0
     */
    public List<PlayerInput> createPlayerInputsFromCsv(String csv) throws NoSuchFieldException, IllegalAccessException {
        return PlayerInput.fromCsv(csv);
    }

    /**
     * Parses a JSON string into a {@code PlayerInput} Object.
     * For details see {@code PlayerInput.fromCsv()}, on what has to be present.<br>
     * Capitalization of the keys matters.
     * <p>
     * This is the single input form of the same idea, so it takes one object rather than a
     * header and rows, and the keys are the same field names in the same capitalization. A key
     * that names nothing is ignored rather than reported, so a field left out simply comes back
     * at its default, and the enclosing braces have to be in the string. The name is plural
     * but the result is a single input.
     * example:
     * <pre>
     * // one input out of a saved recording, and queue it
     * const step = Player.createPlayerInputsFromJson('{"movementForward": 1, "yaw": 90, "sprinting": true}');
     * Player.addInput(step);
     * </pre>
     *
     * @param json JSON string to be parsed
     * @return The JSON parsed into a {@code PlayerInput}
     * @see #createPlayerInputsFromCsv(String)
     * @since 1.4.0
     */
    public PlayerInput createPlayerInputsFromJson(String json) {
        return PlayerInput.fromJson(json);
    }

    /**
     * Creates a new {@code PlayerInput} object with the current inputs of the player.
     * <p>
     * This snapshots what the player is actually pressing right now, as the same value
     * {@link #addInput(PlayerInput)} takes. It is the way to start a recorded walk from
     * wherever the player happens to be, and the way to check what a script's own movement
     * actually did.<br>
     * The buttons are read as the game holds them, so sneaking and sprinting come from whether
     * the player is in that state rather than from whether the key is down, and the angles are
     * the player's current ones rather than the last input's.
     * example:
     * <pre>
     * // queue twenty more ticks of exactly what the player is doing now
     * const current = Player.getCurrentPlayerInput();
     * for (const step of Array(20).fill(current)) {
     *   Player.addInput(step);
     * }
     * </pre>
     *
     * @return a PlayerInput holding the player's current movement, angles and buttons
     * @see PlayerInput
     * @since 1.4.0
     */
    public PlayerInput getCurrentPlayerInput() {
        assert mc.player != null;
        var plIn = mc.player.input.keyPresses;
        int forward = (plIn.forward() ? 1 : 0) + (plIn.backward() ? -1 : 0);
        int sideways = (plIn.left() ? 1 : 0) + (plIn.right() ? -1 : 0);
        return new PlayerInput(forward, sideways, mc.player.getYRot(), mc.player.getXRot(), mc.player.input.keyPresses.jump(), mc.player.input.keyPresses.shift(), mc.player.isSprinting());
    }

    /**
     * Adds a new {@code PlayerInput} to {@code MovementQueue} to be executed
     * <p>
     * This is where a movement input stops being a value and becomes something the player
     * does. The queue is worked through one entry per client tick, so adding one queues a
     * single tick of movement and adding ten queues ten ticks, and {@link #clearInputs()} empties
     * whatever has not run yet.<br>
     * The input is copied on the way in, so a script can reuse and change the object it passed
     * without disturbing what was queued, and the queue is not affected by anything the script
     * does to the original afterwards.
     * example:
     * <pre>
     * // walk forward, then sprint forward and jump, one tick each
     * Player.addInput(Player.createPlayerInput(1.0, 0.0, 0.0));
     * Player.addInput(Player.createPlayerInput(1.0, 0.0, true, true));
     * </pre>
     *
     * @param input the PlayerInput to be executed
     * @see MovementQueue
     * @since 1.4.0
     */
    public void addInput(PlayerInput input) {
        MovementQueue.append(input, mc.player);
    }

    /**
     * Adds multiple new {@code PlayerInput} to {@code MovementQueue} to be executed
     * <p>
     * This is {@link #addInput(PlayerInput)} in a loop, so a whole recorded walk goes on the
     * queue in one call. The order is kept and each entry is one tick, so the player performs
     * them in the order given, one per tick. The array is a plain one, and a script that wants
     * to keep a library of walks can pass the same array more than once.
     * example:
     * <pre>
     * // queue a sequence: three ticks forward, then three ticks back
     * const forward = Player.createPlayerInput(1.0, 0.0, 0.0);
     * const back = Player.createPlayerInput(-1.0, 0.0, 0.0);
     * Player.addInputs([forward, forward, forward, back, back, back]);
     * </pre>
     *
     * @param inputs the PlayerInputs to be executed, one per tick, in order
     * @see MovementQueue
     * @since 1.4.0
     */
    public void addInputs(PlayerInput[] inputs) {
        for (PlayerInput input : inputs) {
            addInput(input);
        }
    }

    /**
     * Clears all inputs in the {@code MovementQueue}
     * <p>
     * This throws away whatever has been queued and not yet played out, which is how a macro
     * stops early. The current tick is not affected, since it is already running, so a player
     * who is mid-step finishes that step.<br>
     * It also takes the prediction markers down and builds a fresh set, so a script that had
     * {@link #setDrawPredictions(boolean)} on has to turn it on again to keep seeing them.
     * example:
     * <pre>
     * // queue a walk, then change your mind a moment later
     * Player.addInput(Player.createPlayerInput(1.0, 0.0, 0.0));
     * Client.waitTick(5);
     * Player.clearInputs();
     * </pre>
     *
     * @see MovementQueue
     * @since 1.4.0
     */
    public void clearInputs() {
        MovementQueue.clear();
    }

    /**
     * show or hide the markers for where the queued inputs would take the player.
     * <p>
     * The queue works out where each queued input would leave the player before it plays it,
     * and this decides whether those positions are drawn in the world. A small box is put at
     * each predicted position, in a colour that changes once the prediction and the player
     * disagree, so a script can see at a glance that the prediction has drifted.<br>
     * The markers are on the same list a 3D overlay is, so {@link FHud#clearDraw3Ds()} takes
     * them down as well and this has to be called again to get them back. Setting the same
     * value twice does nothing the second time, and clearing the queue builds a fresh set of
     * markers, so a script that clears the queue should follow it with this if the markers are
     * wanted.
     * example:
     * <pre>
     * // turn the prediction markers on, queue a walk, and watch where it would go
     * Player.setDrawPredictions(true);
     * Player.addInput(Player.createPlayerInput(1.0, 0.0, 0.0));
     * Client.waitTick(1);
     * </pre>
     *
     * @param val {@code true} to show the predicted positions, {@code false} to hide them
     * @since 2.0.0
     */
    public void setDrawPredictions(boolean val) {
        MovementQueue.setDrawPredictions(val);
    }

    /**
     * Predicts where one tick with a {@code PlayerInput} as input would lead to.
     * <p>
     * This is the single input form and it draws nothing. The prediction is worked out on a
     * stand in for the player rather than by moving the real one, so calling it changes nothing
     * in the world and the player does not move. The result is the position after that one tick
     * of input, which for a walk is a short step and for a jump is a step upwards.
     * example:
     * <pre>
     * // where would one tick of sprinting forward put me
     * const where = Player.predictInput(Player.createPlayerInput(1.0, 0.0, true, true));
     * Chat.log(`one tick ahead: ${where.getX()}, ${where.getY()}, ${where.getZ()}`);
     * </pre>
     *
     * @param input the PlayerInput for the prediction
     * @return the position after the input
     * @see #predictInput(PlayerInput, boolean)
     * @since 1.4.0
     */
    public Pos3D predictInput(PlayerInput input) {
        return predictInput(input, false);
    }

    /**
     * Predicts where one tick with a {@code PlayerInput} as input would lead to.
     * <p>
     * As the single input form, but with the option of drawing the result. Drawing puts a small
     * orange box in the world at the predicted position through the same 3D overlay the queued
     * movement uses, and it is not removed afterwards, so a script drawing a lot of predictions
     * wants {@link FHud#clearDraw3Ds()} afterwards or wants the draw argument to be {@code
     * false}.<br>
     * The prediction is worked out on a stand in, so the real player does not move either way.
     * example:
     * <pre>
     * // predict a jump and show where it lands
     * const input = Player.createPlayerInput(1.0, 0.0, 0.0, 0.0, true, false, true);
     * const landing = Player.predictInput(input, true);
     * Chat.log(`predicted landing at ${landing.getY()}`);
     * </pre>
     *
     * @param input the PlayerInput for the prediction
     * @param draw  whether to visualize the result or not
     * @return the position after the input
     * @since 1.4.0
     */
    public Pos3D predictInput(PlayerInput input, boolean draw) {
        return predictInputs(new PlayerInput[]{input}, draw).get(0);
    }

    /**
     * Predicts where each {@code PlayerInput} executed in a row would lead
     * without drawing it.
     * <p>
     * This is the sequence form: each input is applied to the position the one before it
     * reached, so the result is the whole path rather than one step of it, and the list comes
     * back with one position per input, in the same order. Nothing is drawn and the real player
     * does not move.<br>
     * The stand in starts from the player's current position, so a prediction of where a
     * recorded walk ends is only as good as the player being in the same place the recording
     * started.
     * example:
     * <pre>
     * // where would ten ticks of walking forward end up
     * const input = Player.createPlayerInput(1.0, 0.0, 0.0);
     * const steps = Array(10).fill(input);
     * const path = Player.predictInputs(steps);
     * const end = path.get(path.size() - 1);
     * Chat.log(`ten ticks ahead: ${end.getX()}, ${end.getY()}, ${end.getZ()}`);
     * </pre>
     *
     * @param inputs the PlayerInputs for each tick for the prediction
     * @return the position after each input, one per input, in order
     * @see #predictInputs(PlayerInput[], boolean)
     * @since 1.4.0
     */
    public List<Pos3D> predictInputs(PlayerInput[] inputs) {
        return predictInputs(inputs, false);
    }

    /**
     * whether the player is currently breaking a block.
     * <p>
     * This is the interaction manager's own flag, so it is {@code true} from the tick the
     * breaking starts until the block is gone or the breaking is cancelled. Use the interaction
     * manager instead: it says the same thing and is where the rest of the interaction calls
     * live.
     * example:
     * <pre>
     * const interactions = Player.getInteractionManager();
     * if (interactions !== null) {
     *   if (interactions.isBreakingBlock()) {
     *     Chat.log("still mining");
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the player is in the middle of breaking a block
     * @since 1.8.0
     * @deprecated use {@code Player.getInteractionManager().isBreakingBlock()} instead
     */
    @Deprecated
    public boolean isBreakingBlock() {
        assert mc.gameMode != null;
        return mc.gameMode.isDestroying();
    }

    /**
     * Predicts where each {@code PlayerInput} executed in a row would lead
     * <p>
     * This is the sequence form with the option of drawing it. Each input is applied to where
     * the one before it reached, so the list comes back with one position per input, and the
     * stand in starts from the player's current position, so the path is a prediction of moving
     * from where the player is now rather than of replaying a recording.<br>
     * Drawing puts a small orange box at each predicted position through the same 3D overlay the
     * queued movement uses, and nothing removes them afterwards, so a script drawing a long
     * sequence wants {@link FHud#clearDraw3Ds()} afterwards.
     * example:
     * <pre>
     * // show where a straight run of ten ticks would go
     * const input = Player.createPlayerInput(1.0, 0.0, 0.0);
     * const steps = Array(10).fill(input);
     * Player.predictInputs(steps, true);
     * Client.waitTick(1);
     * </pre>
     *
     * @param inputs the PlayerInputs for each tick for the prediction
     * @param draw   whether to visualize the result or not
     * @return the position after each input, one per input, in order
     * @since 1.4.0
     */
    public List<Pos3D> predictInputs(PlayerInput[] inputs, boolean draw) {
        assert mc.player != null;
        MovementDummy dummy = new MovementDummy(mc.player);
        List<Pos3D> predictions = new ArrayList<>();
        for (PlayerInput input : inputs) {
            var pos = dummy.applyInput(input);
            predictions.add(new Pos3D(pos.x, pos.y, pos.z));
        }
        if (draw) {
            for (Pos3D point : predictions) {
                MovementQueue.predPoints.addPoint(point, 0.01, 0xff9500);
            }
        }
        return predictions;
    }

    /**
     * Adds a forward movement with a relative yaw value to the MovementQueue.
     * <p>
     * This is the shortcut for one tick of walking forward, and the yaw is a <em>turn</em>
     * rather than a bearing: {@code 0} is the way the player is already facing, {@code 90} is a
     * quarter turn to the right, and a negative value turns left. The player's own current
     * bearing is added to it, so the step is relative to where they are now.<br>
     * Like everything on the queue this is a single tick, and a run of them is a walk, so a
     * script that wants to go somewhere queues several and lets them play out. It needs a
     * player, since the current bearing is read to work the relative one out.
     * example:
     * <pre>
     * // walk forward for a quarter of a second
     * Player.moveForward(0);
     * Client.waitTick(5);
     * Player.clearInputs();
     * </pre>
     *
     * @param yaw the relative yaw for the player, where positive turns right
     * @since 1.4.0
     */
    public void moveForward(double yaw) {
        PlayerInput input = new PlayerInput();
        input.movementForward = 1.0F;
        input.yaw = (float) (getPlayer().getYaw() + yaw);
        addInput(input);
    }

    /**
     * Adds a backward movement with a relative yaw value to the MovementQueue.
     * <p>
     * One tick of walking backwards, with the same relative yaw as
     * {@link #moveForward(double)}: {@code 0} keeps the current bearing and a positive value
     * turns right. Note that the turn is applied while backing up, so a positive value walks
     * backwards towards the player's left rather than their right.
     * example:
     * <pre>
     * // back away from something, keeping the same facing
     * Player.moveBackward(0);
     * Client.waitTick(4);
     * Player.clearInputs();
     * </pre>
     *
     * @param yaw the relative yaw for the player, where positive turns right
     * @since 1.4.0
     */
    public void moveBackward(double yaw) {
        PlayerInput input = new PlayerInput();
        input.movementForward = -1.0F;
        input.yaw = (float) (getPlayer().getYaw() + yaw);
        addInput(input);
    }

    /**
     * Adds sideways movement with a relative yaw value to the MovementQueue.
     * <p>
     * One tick of strafing to the left, which is the A key, and is a side step rather than a
     * turn: the player keeps the same bearing. The yaw is the same relative value as everywhere
     * else, so a non-zero one turns as well as strafing.<br>
     * A single tick of strafing barely moves the player, so a real side step queues several of
     * these in a row.
     * example:
     * <pre>
     * // sidestep to the left without turning
     * Player.moveStrafeLeft(0);
     * Client.waitTick(4);
     * Player.clearInputs();
     * </pre>
     *
     * @param yaw the relative yaw for the player, where positive turns right
     * @since 1.4.2
     */
    public void moveStrafeLeft(double yaw) {
        PlayerInput input = new PlayerInput();
        input.movementSideways = 1.0F;
        input.yaw = (float) (getPlayer().getYaw() + yaw);
        addInput(input);
    }

    /**
     * Adds sideways movement with a relative yaw value to the MovementQueue.
     * <p>
     * One tick of strafing to the right, which is the D key, and like
     * {@link #moveStrafeLeft(double)} it is a side step that keeps the same bearing unless the
     * yaw says otherwise.
     * example:
     * <pre>
     * // sidestep to the right without turning
     * Player.moveStrafeRight(0);
     * Client.waitTick(4);
     * Player.clearInputs();
     * </pre>
     *
     * @param yaw the relative yaw for the player, where positive turns right
     * @since 1.4.2
     */
    public void moveStrafeRight(double yaw) {
        PlayerInput input = new PlayerInput();
        input.movementSideways = -1.0F;
        input.yaw = (float) (getPlayer().getYaw() + yaw);
        addInput(input);
    }

}
