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
     * @return the Inventory handler
     * @see Inventory
     */
    public Inventory<?> openInventory() {
        assert mc.player != null && mc.player.getInventory() != null;
        return Inventory.create();
    }

    /**
     * @return the player entity wrapper.
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
     * @since 1.9.0
     */
    @Nullable
    public InteractionManagerHelper getInteractionManager() {
        return mc.gameMode == null ? null : new InteractionManagerHelper(mc.gameMode);
    }

    /**
     * alias for {@link FPlayer#getInteractionManager()}
     * @since 1.9.0
     */
    @Nullable
    public InteractionManagerHelper interactions() {
        return getInteractionManager();
    }

    /**
     * @return the player's current gamemode.
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
     * @since 1.8.4
     */
    @DocletReplaceParams("gameMode: Gamemode")
    public void setGameMode(String gameMode) {
        assert mc.gameMode != null;
        mc.gameMode.setLocalMode(GameType.byName(gameMode.toLowerCase(Locale.ROOT), mc.gameMode.getPlayerMode()));
    }

    /**
     * @param distance
     * @param fluid
     * @return the block/liquid the player is currently looking at.
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
     * @return the raycast result.
     * @since 1.9.1
     */
    public HitResultHelper.Block detailedRayTraceBlock(double distance, boolean fluid) {
        assert mc.level != null;
        assert mc.player != null;
        return new HitResultHelper.Block((BlockHitResult) mc.player.pick(distance, 0, fluid));
    }

    /**
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
     * @param distance
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
     * @param l1
     * @param l2
     * @param l3
     * @param l4
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
     * @param index the index of the message. should be in between 0 and 3
     * @param message the message to write
     * @return of success (sign screen is open).
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
     * @param folder
     * @param callback calls your method as a {@link Consumer}&lt;{@link TextHelper}&gt;
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
     *
     * @param folder
     * @param file
     * @param callback calls your method as a {@link Consumer}&lt;{@link TextHelper}&gt;
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
     * @param folder   the folder to save the screenshot to, relative to the macro folder
     * @param width    the width of the panorama
     * @param height   the height of the panorama
     * @param callback calls your method as a {@link Consumer}&lt;{@link TextHelper}&gt;
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

    public StatsHelper getStatistics() {
        assert mc.player != null;
        return new StatsHelper(mc.player.getStats());
    }

    /**
     * @return the current reach distance of the player.
     * @since 1.8.4
     */
    public double getReach() {
        assert mc.player != null;
        return mc.player.blockInteractionRange();
    }

    /**
     * Creates a new PlayerInput object.
     *
     * @see PlayerInput
     * @since 1.4.0
     */
    public PlayerInput createPlayerInput() {
        return new PlayerInput();
    }

    /**
     * Creates a new PlayerInput object.
     *
     * @see PlayerInput
     * @since 1.4.0
     */
    public PlayerInput createPlayerInput(double movementForward, double movementSideways, double yaw) {
        return new PlayerInput((float) movementForward, (float) movementSideways, (float) yaw);
    }

    /**
     * Creates a new PlayerInput object.
     *
     * @see PlayerInput
     * @since 1.4.0
     */
    public PlayerInput createPlayerInput(double movementForward, double yaw, boolean jumping, boolean sprinting) {
        return new PlayerInput((float) movementForward, (float) yaw, jumping, sprinting);
    }

    /**
     * Creates a new PlayerInput object.
     *
     * @param movementForward  1 = forward input (W); 0 = no input; -1 = backward input (S)
     * @param movementSideways 1 = left input (A); 0 = no input; -1 = right input (D)
     * @param yaw              yaw of the player
     * @param pitch            pitch of the player
     * @param jumping          jump input
     * @param sneaking         sneak input
     * @param sprinting        sprint input
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
     *
     * @param csv CSV string to be parsed
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
     *
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
     *
     * @param inputs the PlayerInputs to be executed
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
     *
     * @see MovementQueue
     * @since 1.4.0
     */
    public void clearInputs() {
        MovementQueue.clear();
    }

    public void setDrawPredictions(boolean val) {
        MovementQueue.setDrawPredictions(val);
    }

    /**
     * Predicts where one tick with a {@code PlayerInput} as input would lead to.
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
     *
     * @param inputs the PlayerInputs for each tick for the prediction
     * @return the position after each input
     * @see #predictInputs(PlayerInput[], boolean)
     * @since 1.4.0
     */
    public List<Pos3D> predictInputs(PlayerInput[] inputs) {
        return predictInputs(inputs, false);
    }

    /**
     * @return
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
     *
     * @param inputs the PlayerInputs for each tick for the prediction
     * @param draw   whether to visualize the result or not
     * @return the position after each input
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
     *
     * @param yaw the relative yaw for the player
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
     *
     * @param yaw the relative yaw for the player
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
     *
     * @param yaw the relative yaw for the player
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
     *
     * @param yaw the relative yaw for the player
     * @since 1.4.2
     */
    public void moveStrafeRight(double yaw) {
        PlayerInput input = new PlayerInput();
        input.movementSideways = -1.0F;
        input.yaw = (float) (getPlayer().getYaw() + yaw);
        addInput(input);
    }

}
