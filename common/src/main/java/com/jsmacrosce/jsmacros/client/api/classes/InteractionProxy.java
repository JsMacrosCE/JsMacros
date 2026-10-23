package com.jsmacrosce.jsmacros.client.api.classes;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.InteractionManagerHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Internal client-side proxy for crosshair-target, block-breaking, and held-interaction overrides.
 * Scripts access these overrides through the player's interaction manager:
 * <pre>
 * const interactions = Player.getInteractionManager();
 * if (interactions !== null && interactions.hasTargetOverride()) {
 *   interactions.clearTargetOverride();
 * }
 * </pre>
 * @author aMelonRind
 * @since 1.9.0
 */
public class InteractionProxy {
    private static final Minecraft mc = Minecraft.getInstance();

    /**
     * Restores target-check defaults and clears the target, break, and held-interaction overrides.
     * Pending break callbacks receive {@code RESET} with no position; client-side block breaking
     * is stopped unless the attack key is held. The game also calls this on world join and disconnect.
     *
     * @since 1.9.0
     */
    public static void reset() {
        Target.resetChecks();
        Target.setTarget(null);
        Break.setOverride(false, "RESET");
        Interact.setOverride(false);
    }

    /**
     * Manages the shared client-side crosshair-target override maintained by client hooks; it is not
     * per-player and does not grant server reach. Scripts normally use the player's interaction
     * manager:
     * <pre>
     * const interactions = Player.getInteractionManager();
     * if (interactions !== null && interactions.hasTargetOverride()) {
     *   interactions.clearTargetOverride();
     * }
     * </pre>
     * Target checks may clear an override or render a miss while retaining the stored target.
     * Clearing it can end a pending break override and run callbacks on the caller's thread.
     */
    public static class Target {
        private static final BlockHitResult MISSED = BlockHitResult.miss(Vec3.ZERO, Direction.DOWN, BlockPos.ZERO);

        @Nullable
        private static HitResult override = null;
        @Nullable
        private static Entity overrideEntity = null;
        /** Enables the interaction-range check during target updates; defaults to true. Assignment alone does not recheck a target. */
        public static boolean checkDistance = true;
        /** When distance checking is enabled, clears an out-of-range target if true; otherwise an update renders a miss but retains it. Defaults to true. */
        public static boolean clearIfOutOfRange = true;
        /** Enables checking whether a block target's state is air during target updates; defaults to false. */
        public static boolean checkAir = false;
        /** When air checking is enabled, clears an air-block target if true; otherwise an update renders a miss but retains it. Defaults to false. */
        public static boolean clearIfIsAir = false;
        /** Enables checking whether a non-air block target has an empty block shape during target updates; defaults to true. */
        public static boolean checkShape = true;
        /** When shape checking is enabled, clears an empty-shape target if true; otherwise an update renders a miss but retains it. Defaults to false. */
        public static boolean clearIfEmptyShape = false;

        /** Restores range checking and out-of-range clearing to true, air checking and air clearing to false, and shape checking to true with empty-shape clearing false. Does not clear or immediately revalidate the target. */
        public static void resetChecks() {
            Target.checkDistance = true;
            Target.clearIfOutOfRange = true;
            Target.checkAir = false;
            Target.clearIfIsAir = false;
            Target.checkShape = true;
            Target.clearIfEmptyShape = false;
        }

        /**
         * Sets a block target at the block's center; a null position clears the target. The direction
         * is used only for a non-null position. Delegates to {@code setTarget}, including immediate
         * checks and break revalidation that can end a pending break override and run callbacks on the caller's thread.
         * @param pos block position, or null to clear the target
         * @param direction face recorded in the constructed block hit result; ignored when {@code pos} is null
         */
        public static void setTargetBlock(@Nullable BlockPos pos, Direction direction) {
            setTarget(pos == null ? null : new BlockHitResult(Vec3.atCenterOf(pos), direction, pos, false));
        }

        /**
         * Stores the supplied target reference and immediately applies the current checks and break-state
         * update at interpolation fraction zero. A check may clear it or render a miss while retaining it.
         * Break revalidation can end a pending break override and run callbacks on the caller's thread,
         * even for a non-null target. Clearing does not itself restore a normal crosshair raycast.
         * @param value target override, or null to clear it
         */
        public static void setTarget(@Nullable HitResult value) {
            synchronized (Target.class) {
                override = value;
                if (value != null && value.getType() == HitResult.Type.ENTITY) overrideEntity = ((EntityHitResult) value).getEntity();
                else overrideEntity = null;
                onUpdate(0f);
                Break.isBreaking();
            }
        }

        /** Sets the fixed miss result through {@code setTarget}; it does not request clearing the override.
         * The same checks and break revalidation apply and can run break callbacks on the caller's thread.
         */
        public static void setTargetMissed() {
            setTarget(MISSED);
        }

        /**
         * Reports whether a target override is stored, including a miss or a target currently rendered
         * as a miss by checks. Does not revalidate the target or query the current crosshair hit.
         * @return whether a target override is present
         */
        public static boolean hasOverride() {
            return override != null;
        }

        /**
         * Applies the stored target to the crosshair state after configured checks. A false result can still
         * follow clearing a dead or invalid target; a block check without a level leaves it stored and unapplied.
         * Clearing can end a pending break override and invoke callbacks on this caller's thread.
         * @param tickDelta interpolation fraction used for the range check
         * @return whether the override or a miss was applied, so the normal crosshair pick should be skipped
         */
        public static boolean onUpdate(float tickDelta) {
            boolean cancel = false;
            synchronized (Target.class) {
                if (override != null) {
                    boolean shouldMiss = false;
                    if (overrideEntity != null) {
                        if (!overrideEntity.isAlive()) {
                            setTarget(null);
                            return shouldMiss;
                        }
                    } else if ((checkAir || checkShape) && override.getType() == HitResult.Type.BLOCK) {
                        if (mc.level == null) return shouldMiss;
                        BlockPos pos = ((BlockHitResult) override).getBlockPos();
                        BlockState state = mc.level.getBlockState(pos);
                        if (checkAir && state.isAir()) {
                            if (!clearIfIsAir) shouldMiss = true;
                            else {
                                setTarget(null);
                                return shouldMiss;
                            }
                        }
                        if (checkShape && !state.isAir() && state.getShape(mc.level, pos).isEmpty()) {
                            if (!clearIfEmptyShape) shouldMiss = true;
                            else {
                                setTarget(null);
                                return shouldMiss;
                            }
                        }
                    }
                    if (checkDistance && !isInRange(tickDelta)) {
                        if (!clearIfOutOfRange) shouldMiss = true;
                        else {
                            setTarget(null);
                            return shouldMiss;
                        }
                    }
                    if (override == null) return shouldMiss;
                    cancel = true;
                    if (shouldMiss) {
                        mc.hitResult = MISSED;
                        mc.crosshairPickEntity = null;
                    } else {
                        mc.hitResult = override;
                        mc.crosshairPickEntity = overrideEntity;
                    }
                }
            }
            return cancel;
        }

        /**
         * Tests the stored target against client interaction reach, independently of {@code checkDistance}.
         * Without a stored target, player or game mode this returns false; a miss otherwise passes.
         * A hit location must be strictly within the entity or block reach as appropriate; block targets
         * also pass when the nearest point on their unit block cube is strictly within block reach.
         * This does not check air, shape or visibility, clear the target, or confirm server permission.
         * @param tickDelta interpolation fraction for the player's eye position
         * @return whether the stored target passes the client distance calculation
         */
        public static boolean isInRange(float tickDelta) {
            synchronized (Target.class) {
                if (override == null || mc.player == null || mc.gameMode == null) return false;
                if (override.getType() == HitResult.Type.MISS) return true;

                Vec3 campos = mc.player.getEyePosition(tickDelta);
                double blockReach = mc.player.blockInteractionRange();
                double entityReach = mc.player.entityInteractionRange();
                if (override.getLocation().closerThan(campos, override.getType() == HitResult.Type.ENTITY ? entityReach : blockReach)) return true;

                if (override.getType() != HitResult.Type.BLOCK) return false;
                BlockPos pos = ((BlockHitResult) override).getBlockPos();
                return campos.distanceToSqr(
                        Mth.clamp(campos.x, pos.getX(), pos.getX() + 1),
                        Mth.clamp(campos.y, pos.getY(), pos.getY() + 1),
                        Mth.clamp(campos.z, pos.getZ(), pos.getZ() + 1)
                ) < blockReach * blockReach;
            }
        }

    }

    /**
     * Manages the shared block-breaking override and its pending callbacks through client hooks; it is
     * not a per-instance controller or confirmation of server-side breaking. Scripts normally use the
     * player's interaction manager:
     * <pre>
     * const interactions = Player.getInteractionManager();
     * if (interactions !== null && interactions.hasBreakBlockOverride()) {
     *   interactions.cancelBreakBlock();
     * }
     * </pre>
      * The query can itself end an invalid override and invoke callbacks. Cancelling reports
      * {@code CANCELLED} to pending callbacks synchronously on the caller's thread.
     */
    public static class Break {
        private static boolean override = false;
        private static final List<Consumer<BreakBlockResult>> callbacks = new ArrayList<>();
        @Nullable
        private static BlockPos lastTarget = null;

        /** Enables or disables the override, resetting its tracked target either way. Disabling may stop
         * client-side breaking and dispatch pending callbacks with a null reason and position, even if already disabled.
         * Runs directly on the caller; enabling does not start a block-break action.
         * @param value whether to enable the override
         */
        public static void setOverride(boolean value) {
            setOverride(value, null, null);
        }

        /** Enables or disables the override, resetting its tracked target either way. The reason is sent
         * to callbacks only on disable, with a null position, even if already disabled. Disabling calls
         * {@code stopDestroyBlock()} only when a game mode exists and the attack key is not down.
         * Runs directly on the caller; enabling does not start a block-break action.
         * @param value whether to enable the override
         * @param reason reported reason when disabling, or null; ignored when enabling
         */
        public static void setOverride(boolean value, @Nullable String reason) {
            setOverride(value, reason, null);
        }

        private static void setOverride(boolean value, @Nullable String reason, @Nullable BlockPos pos) {
            lastTarget = null;
            override = value;
            if (!value) {
                if (mc.gameMode != null && !mc.options.keyAttack.isDown()) mc.gameMode.stopDestroyBlock();
                runCallback(reason, pos);
            }
        }

        /** Queues a non-null outcome callback and optionally enables the override. Enabling resets the
         * tracked target; it does not start a block-break action. A false {@code breaking} value leaves
         * the current override unchanged. Callbacks run on the dispatching caller's thread while the callback
          * monitor is held. Each callback's {@code Exception} is printed and iteration continues; the queue
          * is cleared after normal iteration. Reentrant queue mutation or an {@code Error} can prevent completion.
         * @param callback consumer for a later outcome, or null to add none
         * @param breaking whether to enable the override
         */
        public static void addCallback(Consumer<BreakBlockResult> callback, boolean breaking) {
            synchronized (callbacks) {
                if (callback != null) callbacks.add(callback);
                if (breaking) setOverride(true);
            }
        }

        private static void runCallback(@Nullable String reason, @Nullable BlockPos pos) {
            if (callbacks.isEmpty()) return;
            runCallback(new BreakBlockResult(reason, pos == null ? null : new BlockPosHelper(pos)));
        }

        private static void runCallback(BreakBlockResult result) {
            synchronized (callbacks) {
                if (!callbacks.isEmpty()) {
                    callbacks.forEach(cb -> {
                        try {
                            cb.accept(result);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    callbacks.clear();
                }
            }
        }

        /** Validates the override against the current level, crosshair block, and tracked position; failed
         * checks can disable it and dispatch callbacks on this caller's thread. This does not query native
         * destruction state or confirm server acceptance. The first valid block target becomes the tracked
         * position; a changed position, a missing block target or air can end the override.
         * @return whether the override remains active after these checks
         */
        public static boolean isBreaking() {
            if (mc.level == null) setOverride(false, "RESET");
            synchronized (callbacks) {
                if (!override) {
                    if (!callbacks.isEmpty()) runCallback("NO_OVERRIDE", null);
                    return false;
                }
            }
            if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) {
                setOverride(false, lastTarget == null ? "NO_TARGET" : "TARGET_LOST");
                return false;
            }
            if (lastTarget == null) {
                lastTarget = ((BlockHitResult) mc.hitResult).getBlockPos();
            } else if (!((BlockHitResult) mc.hitResult).getBlockPos().equals(lastTarget)) {
                setOverride(false, "TARGET_CHANGE");
                return false;
            }
            if (mc.level.getBlockState(lastTarget).isAir()) {
                setOverride(false, "IS_AIR");
                return false;
            }
            return true;
        }

        /** Reports the client block-destruction call result to the active override. A true result while active
         * disables the override and reports {@code SUCCESS} with the position to callbacks on this caller's
         * thread. Otherwise this does nothing; {@code SUCCESS} does not confirm server acceptance.
         * @param pos position reported as broken
         * @param ret result of the client destruction call; false leaves the override unchanged
         */
        public static void onBreakBlock(BlockPos pos, boolean ret) {
            if (override && ret) setOverride(false, "SUCCESS", pos);
        }

        /** Carries an outcome reason and optional block position reported by the breaking override.
         * These are proxy-reported results, not proof of server acceptance. Scripts receive them through
         * the interaction manager's break callback:
         * <pre>
         * const interactions = Player.getInteractionManager();
         * if (interactions !== null) {
         *   interactions.breakBlockAsync(JavaWrapper.methodToJava(function (result) {
         *     if (result !== null) Chat.log(result.reason);
         *   }));
         * }
         * </pre>
         */
        public static class BreakBlockResult {
            /** Sentinel returned by the helper's immediate check when the interaction manager is absent; its reason is {@code UNAVAILABLE} and position is null. Not a proxy callback outcome. */
            public static final BreakBlockResult UNAVAILABLE = new BreakBlockResult("UNAVAILABLE", null);
            /**
             * Outcome reported for a block-break override. Proxy callbacks use {@code SUCCESS},
             * {@code NOT_BREAKING}, {@code RESET}, {@code NO_OVERRIDE}, {@code IS_AIR},
             * {@code NO_TARGET}, {@code TARGET_LOST}, or {@code TARGET_CHANGE}. {@code SUCCESS}
             * includes the broken position; other callback results have no position. The static
             * {@code UNAVAILABLE} value is a sentinel with no position, not a callback outcome.
             * A null reason is possible when constructed outside the proxy.
             */
            @DocletReplaceReturn("BreakBlockResult$Reason | null")
            @DocletDeclareType(name = "BreakBlockResult$Reason", type = "'SUCCESS' | 'CANCELLED' | 'INTERRUPTED' | 'NOT_BREAKING' | 'RESET' | 'NO_OVERRIDE' | 'IS_AIR' | 'NO_TARGET' | 'TARGET_LOST' | 'TARGET_CHANGE' | 'UNAVAILABLE'")
            @Nullable
            public final String reason;
            /** Position reported with the result, or null when none was supplied. Proxy callbacks supply a position for {@code SUCCESS}; manually constructed results may supply any position or null. */
            @Nullable
            public final BlockPosHelper pos;

            /** Stores the supplied nullable outcome reason and position without validation or copying.
             * @param reason diagnostic outcome reason, or null
             * @param pos optional block position, or null
             */
            public BreakBlockResult(@Nullable String reason, @Nullable BlockPosHelper pos) {
                this.reason = reason;
                this.pos = pos;
            }

            /**
             * Returns a diagnostic representation containing the reason and position representations.
             * The output is not guaranteed to be valid JSON or a stable parse format; a non-null
             * reason is quoted without escaping. Null reason or position references are printed as {@code null}.
             * This method does not run a break callback.
             * @return diagnostic text containing the reason and position representations
             */
            @Override
            public String toString() {
                return "BreakBlockResult:{\"reason\": "
                        + (reason == null ? "null" : "\"" + reason + "\"")
                        + ", \"pos\": "
                        + (pos == null ? "null" : pos.toString())
                        + "}";
            }

        }

    }

    /**
     * Manages the shared held-use override maintained by client hooks; it is neither a physical-key
     * query nor a per-player interaction controller. Scripts normally access it through the player
     * interaction manager:
     * <pre>
     * const interactions = Player.getInteractionManager();
     * if (interactions !== null && interactions.hasInteractOverride()) {
     *   interactions.holdInteract(false);
     * }
     * </pre>
     * Disabling requests a later release check; it does not guarantee a server-side cancellation.
     */
    public static class Interact {
        private static boolean override = false;
        private static boolean releaseCheck = false;

        /**
         * Sets the held-use override directly on the caller. Enabling clears a pending release check
         * and calls {@code mc.startUseItem()} on every call. Disabling an active override records a
         * deferred release check; it does not release the item here. Prefer the interaction-manager
         * helper for normal script use.
         * @param value whether to enable the override; {@code false} requests a later release check
         * when disabling an active override
         */
        public static void setOverride(boolean value) {
            if (override && !value) releaseCheck = true;
            else if (value) releaseCheck = false;
            override = value;
            if (value) mc.startUseItem();
        }

        /**
         * Reports whether the held-use override flag is enabled; this does not query key or item use.
         * @return whether the held-use override is currently enabled
         */
        public static boolean isInteracting() {
            return override;
        }

        /**
         * Maintains held-use state from client hooks. With no player it returns without changing state.
         * A down use key clears the override flag without requesting a release check. When the override
         * remains enabled, cooldown is exactly zero and the player is not using an item, this calls
         * {@code mc.startUseItem()}. Otherwise, a pending release check calls
         * {@code mc.gameMode.releaseUsingItem(mc.player)} only when the game mode exists, the player
         * is using an item and the use key is not down; the pending check is then cleared even if those
         * conditions are unmet. A call failure can prevent that final clearing.
         * @param cooldown current use-item delay; a new use attempt requires exactly zero
         */
        public static void ensureInteracting(int cooldown) {
            if (mc.player == null) return;
            if (mc.options.keyUse.isDown()) override = false;
            if (isInteracting() && cooldown == 0 && !mc.player.isUsingItem()) mc.startUseItem();
            else if (releaseCheck) {
                if (mc.gameMode != null && mc.player.isUsingItem() && !mc.options.keyUse.isDown()) mc.gameMode.releaseUsingItem(mc.player);
                releaseCheck = false;
            }
        }

    }

}
