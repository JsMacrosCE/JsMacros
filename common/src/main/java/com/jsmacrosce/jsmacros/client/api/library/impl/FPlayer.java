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

import com.jsmacrosce.doclet.DocletIgnore;
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
 * The library acts on the local player. Many methods require a loaded world; where a method
 * relies on the player or interaction manager, it may assert that the corresponding game object
 * exists rather than return an empty result. {@link FWorld#isWorldLoaded()} can be used to check
 * world availability. Inputs created by this library are values, not immediate actions: queue
 * them with {@link #addInput(PlayerInput)} or {@link #addInputs(PlayerInput[])} to apply one per
 * tick.
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
 *   // queue forward movement for one second of game time
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

    /**
     * Initializes the Player library with its associated core. Screenshot and panorama operations
     * use that core's configured macro folder as the base for their output paths.
     *
     * @param runner core associated with this library
     */
    public FPlayer(Core<?, ?> runner) {
        super(runner);
    }

    /**
     * Returns a handler for the local player's inventory. Inventory operations through the
     * handler act on the live inventory.
     *
     * @return the local player's inventory handler
     * @see Inventory
     */
    public Inventory<?> openInventory() {
        assert mc.player != null && mc.player.getInventory() != null;
        return Inventory.create();
    }

    /**
     * Returns a wrapper for the local player, or {@code null} when no local player exists.
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
     * Returns a handler for client interactions with the world, or {@code null} when no game
     * mode interaction manager exists.
     *
     * @return a handle on the interaction manager, or {@code null} when there is no world
     * @since 1.9.0
     */
    @Nullable
    public InteractionManagerHelper getInteractionManager() {
        return mc.gameMode == null ? null : new InteractionManagerHelper(mc.gameMode);
    }

    /**
     * Alias for {@link #getInteractionManager()}.
     *
     * @return a handle on the interaction manager, or {@code null} when none exists
     * @since 1.9.0
     */
    @Nullable
    public InteractionManagerHelper interactions() {
        return getInteractionManager();
    }

    /**
     * Returns the client's current gamemode name in lowercase.
     *
     * @return the client's current gamemode
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
     * Sets the local client's gamemode; the server may subsequently override it.
     *
     * @param gameMode gamemode name; names are case-insensitive and unrecognized names leave the current mode unchanged
     *
     * @since 1.8.4
     */
    @DocletReplaceParams("gameMode: Gamemode")
    public void setGameMode(String gameMode) {
        assert mc.gameMode != null;
        mc.gameMode.setLocalMode(GameType.byName(gameMode.toLowerCase(Locale.ROOT), mc.gameMode.getPlayerMode()));
    }

    /**
     * Traces from the player's view and returns the hit block, or {@code null} for a miss or
     * void-air hit. The {@code fluid} argument controls whether fluids are considered by the
     * trace; this result is independent of any target set on the interaction manager.
     *
     * @param distance how far to trace, in blocks
     * @param fluid    whether the trace considers fluids
     * @return the hit block, or {@code null} for a miss or void air
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
     * Returns the block raycast result, including its hit position and side. A miss is represented
     * by a result whose type is a miss, not by {@code null}.
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
     * Returns the entity stored as the current crosshair pick, or {@code null} if there is none.
     * This reads the rendered pick rather than tracing; use {@link #rayTraceEntity(int)} for a fresh trace.
     *
     * @return the entity the camera is currently looking at. can be affected by {@link InteractionManagerHelper#setTarget(EntityHelper)}
     * @see EntityHelper
     * @deprecated use {@link FPlayer#rayTraceEntity(int)} or {@link InteractionManagerHelper#getTargetedEntity()} instead
     * @since 1.0.5
     */
    @Deprecated
    @DocletIgnore
    @Nullable
    public EntityHelper<?> rayTraceEntity() {
        if (mc.crosshairPickEntity != null) {
            return EntityHelper.create(mc.crosshairPickEntity);
        } else {
            return null;
        }
    }

    /**
     * Traces from the player's eyes along their view and returns the first targeted entity, or
     * {@code null} if there is no entity within range. The distance is in blocks. This performs
     * a fresh trace; {@link InteractionManagerHelper#getTargetedEntity()} returns the existing
     * crosshair pick instead.
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
     * Writes up to four lines, top to bottom, to the open sign editor. A {@code null} line is left
     * unchanged; returns {@code false} if no sign editor is open.
     *
     * @param l1 the first line of the sign, or {@code null} to leave it unchanged
     * @param l2 the second line of the sign, or {@code null} to leave it unchanged
     * @param l3 the third line of the sign, or {@code null} to leave it unchanged
     * @param l4 the fourth line of the sign, or {@code null} to leave it unchanged
     * @return whether a sign editor is open
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
     * Writes one line to the open sign editor. The index is zero-based from the top and must be
     * from 0 through 3; it is checked even when no sign editor is open. Unlike the four-line
     * overload, this method does not accept {@code null} to leave a line unchanged.
     *
     * @param index zero-based sign line, from 0 through 3
     * @param message the message to write
     * @return whether a sign editor is open
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
     * Captures the current render target to a game-named file under the macro folder. The callback,
     * if supplied, receives the game's result as text.
     *
     * @param folder the folder to save into, relative to the macro folder
     * @param callback called with the game's result as text, or {@code null} for no callback
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
     * Captures the current render target to the specified file under the macro folder. The callback,
     * if supplied, receives the game's result as text.
     *
     * @param folder the folder to save into, relative to the macro folder
     * @param file the file name to save under, within that folder
     * @param callback called with the game's result as text, or {@code null} for no callback
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
     * Captures a panorama of the current world.
     * <p>
     * The image is saved under {@code folder}, relative to the profile's macro folder, with a
     * game-chosen name. On Minecraft 1.21.8, {@code width} and {@code height} are ignored. The
     * optional callback receives the message returned by the game's capture method.
     *
     * @param folder   the folder to save the panorama to, relative to the macro folder
     * @param width    retained for compatibility; ignored on Minecraft 1.21.8
     * @param height   retained for compatibility; ignored on Minecraft 1.21.8
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
     * Returns a helper over the local player's statistics counters. A player must be present.
     *
     * @return a handle on the player's statistics
     * @since 2.0.0
     */
    public StatsHelper getStatistics() {
        assert mc.player != null;
        return new StatsHelper(mc.player.getStats());
    }

    /**
     * Returns the local player's current block interaction range, in blocks. A player must be present.
     *
     * @return the block interaction range
     * @since 1.8.4
     */
    public double getReach() {
        assert mc.player != null;
        return mc.player.blockInteractionRange();
    }

    /**
     * Creates an input with zero movement and angles and all buttons false. It has no effect until
     * queued with {@link #addInput(PlayerInput)}.
     *
     * @return a new empty PlayerInput
     * @see PlayerInput
     * @since 1.4.0
     */
    public PlayerInput createPlayerInput() {
        return new PlayerInput();
    }

    /**
     * Creates an input with the supplied movement axes and absolute yaw in degrees; pitch and
     * buttons default to zero/false. It has no effect until queued with {@link #addInput(PlayerInput)}.
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
     * Creates an input with forward movement, absolute yaw in degrees, and jump/sprint buttons.
     * Sideways movement and pitch are zero, and sneaking is false. It has no effect until queued
     * with {@link #addInput(PlayerInput)}.
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
     * Creates an input with both movement axes, yaw and pitch in degrees, and all three buttons
     * explicitly set. It has no effect until queued with {@link #addInput(PlayerInput)}.
     *
     * @param movementForward  1 = forward input (W); 0 = no input; -1 = backward input (S)
     * @param movementSideways 1 = left input (A); 0 = no input; -1 = right input (D)
     * @param yaw              yaw of the player
     * @param pitch            pitch of the player
     * @param jumping          jump input
     * @param sneaking         sneak input
     * @param sprinting        sprint input
     * @return an input with the supplied values
     * @since 1.4.0
     */
    public PlayerInput createPlayerInput(double movementForward, double movementSideways, double yaw, double pitch, boolean jumping, boolean sneaking, boolean sprinting) {
        return new PlayerInput(movementForward, movementSideways, yaw, pitch, jumping, sneaking, sprinting);
    }

    /**
     * Parses CSV rows into inputs using case-sensitive field-name headers. Rows with a different
     * number of columns than the header are skipped; omitted fields retain their defaults. Spaces
     * are removed, quoted values are unsupported, and headers naming no field or a private field
     * throw. Invalid numeric values also throw; boolean values use {@link Boolean#valueOf(String)}
     * semantics.
     *
     * @param csv CSV string to be parsed, one input per row under a header row
     * @return parsed inputs in row order; they are not queued for playback. Pass them to
     *         {@link #addInputs(PlayerInput[])} (or individually to {@link #addInput(PlayerInput)}) to queue them
     * @throws NoSuchFieldException if a header names no field on {@link PlayerInput}
     * @throws IllegalAccessException if a header names a private field on {@link PlayerInput}
     * @see PlayerInput#PlayerInput(float, float, float, float, boolean, boolean, boolean)
     * @since 1.4.0
     */
    public List<PlayerInput> createPlayerInputsFromCsv(String csv) throws NoSuchFieldException, IllegalAccessException {
        return PlayerInput.fromCsv(csv);
    }

    /**
     * Parses a complete JSON object into one input. Keys matching input fields are case-sensitive;
     * omitted fields retain defaults and unknown keys are ignored.
     *
     * @param json JSON string to be parsed
     * @return the parsed input
     * @see #createPlayerInputsFromCsv(String)
     * @since 1.4.0
     */
    public PlayerInput createPlayerInputsFromJson(String json) {
        return PlayerInput.fromJson(json);
    }

    /**
     * Captures the local player's movement key states, current yaw and pitch, jump and sneak key
     * states, and sprinting state in a new input.
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
     * Adds a copy of an input to the movement queue for playback.
     *
     * @param input the PlayerInput to be executed
     * @see MovementQueue
     * @since 1.4.0
     */
    public void addInput(PlayerInput input) {
        MovementQueue.append(input, mc.player);
    }

    /**
     * Adds a copy of each input to the movement queue in array order.
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
     * Removes queued inputs and predictions. If prediction drawing is enabled, its empty overlay
     * is replaced while remaining registered.
     *
     * @see MovementQueue
     * @since 1.4.0
     */
    public void clearInputs() {
        MovementQueue.clear();
    }

    /**
     * Enables or disables drawing predicted positions for queued movement inputs.
     *
     * @param val {@code true} to show the predicted positions, {@code false} to hide them
     * @since 2.0.0
     */
    public void setDrawPredictions(boolean val) {
        MovementQueue.setDrawPredictions(val);
    }

    /**
     * Predicts the position after one tick of the input without drawing a marker or moving the player.
     *
     * @param input the PlayerInput for the prediction
     * @return the predicted position after one tick
     * @since 1.4.0
     */
    public Pos3D predictInput(PlayerInput input) {
        return predictInput(input, false);
    }

    /**
     * Predicts the position after one tick, optionally adding an orange marker to the 3D overlay. The real player is not moved.
     * <p>
     * Markers remain in the overlay until it is cleared with {@link FHud#clearDraw3Ds()}.
     *
     * @param input the PlayerInput for the prediction
     * @param draw  whether to visualize the result or not
     * @return the predicted position after one tick
     * @since 1.4.0
     */
    public Pos3D predictInput(PlayerInput input, boolean draw) {
        return predictInputs(new PlayerInput[]{input}, draw).get(0);
    }

    /**
     * Predicts the positions resulting from applying inputs in order, without drawing or moving the player. Prediction starts at the player's current state.
     *
     * @param inputs the PlayerInputs for each tick for the prediction
     * @return one predicted position per input, in input order
     * @since 1.4.0
     */
    public List<Pos3D> predictInputs(PlayerInput[] inputs) {
        return predictInputs(inputs, false);
    }

    /**
     * Reports the interaction manager's current block-destruction flag. Use {@code Player.getInteractionManager().isBreakingBlock()} instead.
     *
     * @return {@code true} while block destruction is active
     * @since 1.8.0
     * @deprecated use {@code Player.getInteractionManager().isBreakingBlock()} instead
     */
    @Deprecated
    @DocletIgnore
    public boolean isBreakingBlock() {
        assert mc.gameMode != null;
        return mc.gameMode.isDestroying();
    }

    /**
     * Predicts positions by applying inputs in order from the player's current state, optionally adding an orange marker for each position. The real player is not moved.
     * <p>
     * Markers remain in the overlay until it is cleared with {@link FHud#clearDraw3Ds()}.
     *
     * @param inputs the PlayerInputs for each tick for the prediction
     * @param draw   whether to visualize the result or not
     * @return one predicted position per input, in input order
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
     * Queues one tick of forward movement using a yaw offset in degrees from the player's current yaw; positive offsets turn right.
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
     * Queues one tick of backward movement using a yaw offset in degrees from the player's current yaw; positive offsets turn right.
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
     * Queues one tick of leftward strafe using a yaw offset in degrees from the player's current yaw; positive offsets turn right.
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
     * Queues one tick of rightward strafe using a yaw offset in degrees from the player's current yaw; positive offsets turn right.
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
