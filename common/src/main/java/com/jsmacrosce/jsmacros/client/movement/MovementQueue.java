package com.jsmacrosce.jsmacros.client.movement;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import com.jsmacrosce.jsmacros.api.PlayerInput;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.api.classes.render.Draw3D;
import com.jsmacrosce.jsmacros.client.api.library.impl.FHud;

import java.util.ArrayList;
import java.util.List;

import static com.jsmacrosce.jsmacros.client.JsMacros.LOGGER;

/**
 * Shared queue and prediction overlay used to play back queued player inputs.
 * Scripts normally use {@code Player.addInput}, {@code Player.addInputs},
 * {@code Player.clearInputs}, and {@code Player.setDrawPredictions}; this support
 * class is not a per-player queue.
 * <pre>
 * if (Player.getPlayer() !== null) {
 *   Player.setDrawPredictions(true);
 *   Player.addInput(Player.getCurrentPlayerInput());
 * }
 * </pre>
 */
public class MovementQueue {
    private static final List<PlayerInput> queue = new ArrayList<>();
    private static final List<Vec3> predictions = new ArrayList<>();
    /** Current prediction and error-point overlay; {@code clear()} replaces this reference. */
    public static Draw3D predPoints = new Draw3D();
    private static LocalPlayer player;
    private static int queuePos = 0;
    private static boolean reCalcPredictions;

    private static boolean doDrawPredictions = false;

    /**
     * Updates predictions for the next queued input and returns that stored input.
     * This method does not apply the input to the player. Prediction markers may be
     * appended even while the overlay is unregistered for drawing.
     * When the queue is exhausted, returns {@code null} without updating the player
     * reference or prediction state. The queue position advances only after prediction
     * work completes; consumed entries remain queued until {@code clear()}.
     *
     * @param newPlayer non-null local player used for prediction updates when input remains; ignored when exhausted
     * @return the next stored input, or {@code null} when the queue is exhausted
     */
    public synchronized static PlayerInput tick(LocalPlayer newPlayer) {
        if (queuePos == queue.size()) {
            return null;
        }

        player = newPlayer;

        if (predictions.size() == queue.size() - queuePos + 1 && queuePos != 0) {
            Vec3 firstPrediction = predictions.getFirst();
            Vec3 diff = new Vec3(
                    player.getX() - firstPrediction.x(),
                    player.getY() - firstPrediction.y(),
                    player.getZ() - firstPrediction.z());
            if (diff.length() > 0.01D) {
                LOGGER.debug("Pred of by x={}, y={}, z={}", diff.x(), diff.y(), diff.z());
                LOGGER.debug("Player pos x={}, y={}, z={}", player.getX(), player.getY(), player.getZ());
                predPoints.addPoint(player.getX(), player.getY(), player.getZ(), 0.02, 0xFFde070a);
                reCalcPredictions = true;
            } else {
                LOGGER.debug("No Diff");
                predictions.removeFirst();
            }
        } else {
            LOGGER.debug("No Pred");
            reCalcPredictions = true;
        }

        if (reCalcPredictions) {
            calcPredictions();
            drawPredictions();
            reCalcPredictions = false;
        }

        if (!predictions.isEmpty()) {
            Vec3 firstPrediction = predictions.getFirst();
            LOGGER.debug("Predic pos x={}, y={}, z={}", firstPrediction.x(), firstPrediction.y(), firstPrediction.z());
        }

        queuePos++;
        return queue.get(queuePos - 1);
    }

    private synchronized static void calcPredictions() {
        List<PlayerInput> toCalc = ImmutableList.copyOf(queue);
        toCalc = toCalc.subList(queuePos, toCalc.size());
        predictions.clear();
        MovementDummy dummy = new MovementDummy(player);
        for (PlayerInput input : toCalc) {
            predictions.add(dummy.applyInput(input));
        }
    }

    private synchronized static void drawPredictions() {
        predictions.forEach(point -> predPoints.addPoint(new Pos3D(point.x(), point.y(), point.z()), 0.01, 0xFFffd000));
    }

    /** Copies an input into the queue and schedules its predictions for recalculation.
     * Stores the supplied player without resetting the queue position or replaying consumed inputs.
     * This method is not synchronized with the class monitor used by {@code tick}, {@code clear},
     * and {@code setDrawPredictions}; callers must coordinate concurrent access. The input must not be null.
     *
     * @param input input to copy into the queue
     * @param newPlayer local player used for subsequent prediction updates
     */
    public static void append(PlayerInput input, LocalPlayer newPlayer) {
        reCalcPredictions = true;
        player = newPlayer;
        // We do the clone step here, since somewhere one could maybe change the input
        // and we don't want that to affect us.
        queue.add(input.clone());
    }

    /** Enables or disables registration of the current prediction overlay for drawing.
     * The overlay registration changes only when the enabled state changes; this does
     * not clear points or control whether predictions are generated.
     *
     * @param val whether prediction drawing should be enabled
     */
    public synchronized static void setDrawPredictions(boolean val) {
        if (val ^ doDrawPredictions) {
            doDrawPredictions = val;
            if (doDrawPredictions) {
                FHud.renders.add(predPoints);
            } else {
                FHud.renders.remove(predPoints);
            }
        }
    }

    /**
     * Clears queued inputs and predictions, resets the queue position, and replaces
     * the overlay. The drawing-enabled state is preserved, and the stored player and
     * prediction-recalculation flag are not reset. The old overlay is removed from
     * HUD registration, not emptied; its replacement is a new empty overlay, registered
     * immediately if prediction drawing remains enabled.
     */
    public synchronized static void clear() {
        queue.clear();
        predictions.clear();
        FHud.renders.remove(predPoints);
        predPoints = new Draw3D();
        if (doDrawPredictions) {
            FHud.renders.add(predPoints);
        }
        queuePos = 0;
    }

}
