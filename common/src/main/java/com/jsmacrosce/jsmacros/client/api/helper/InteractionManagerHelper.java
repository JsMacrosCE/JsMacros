package com.jsmacrosce.jsmacros.client.api.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletDeclareType;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.access.IClientPlayerInteractionManager;
import com.jsmacrosce.jsmacros.client.api.classes.InteractionProxy;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.HitResultHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.api.library.impl.FClient;
import com.jsmacrosce.jsmacros.client.util.InteractionCompat;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.Locale;
import java.util.concurrent.Semaphore;

/**
 * Controls client-side player interactions with the world through the current interaction manager.
 * <p>
 * Obtain it with {@code Player.interactions()}, which is {@code null} when no interaction
 * manager is available. Block and entity target overrides affect operations that use the
 * crosshair until cleared; range and block checks may also clear or turn an override into a
 * miss.
 * <p>
 * An {@code await} option waits for the queued client-thread invocation when called from outside
 * the client thread and its joined-thread stack; it does not wait for server confirmation.
 * Calls on that stack execute directly. {@link #breakBlock()} rejects waiting on that stack
 * after checking for an immediate result; timed {@link #holdInteract(int)} calls reject it
 * before enabling the hold. Waiting can be interrupted; interruption during a timed hold
 * can leave the interact override enabled.
 * <pre>
 * const interactions = Player.interactions();
 * if (interactions === null) { throw new Error("not in a world"); }
 *
 * interactions.breakBlockAsync(JavaWrapper.methodToJava(function (result) {
 *   Chat.log(`break finished as ${result.reason}`);
 * }));
 * </pre>
 *
 * @author aMelonRind
 * @since 1.9.0
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
@DocletCategory("Misc Helpers")
public class InteractionManagerHelper extends BaseHelper<MultiPlayerGameMode> {
    protected final Minecraft mc = Minecraft.getInstance();

    /**
     * Whether methods refresh this helper's wrapped manager when it differs from the client's
     * current manager. Defaults to {@code true}; when disabled, a mismatch throws
     * {@link RuntimeException}. If no replacement manager exists, updating fails and retains
     * the wrapped manager. See {@link #checkBase(boolean)} for the exact result.
     */
    public boolean autoUpdateBase = true;

    /**
     * Wraps the supplied interaction manager as this helper's initial reference.
     * @param base initial interaction manager to wrap
     */
    public InteractionManagerHelper(MultiPlayerGameMode base) {
        super(base);
    }

    /**
     * Checks whether the wrapped manager is current, optionally replacing it with the client's current manager.
     * Matching references return {@code true}, including when both are {@code null}.
     * A mismatch with {@code update == false} throws {@link RuntimeException}.
     * @param update whether to replace a stale manager with a non-null current manager
     * @return {@code true} if the references match or replacement succeeds; {@code false} if
     *         replacement was requested but the current manager is null, leaving the wrapped manager unchanged
     */
    public boolean checkBase(boolean update) {
        if (mc.gameMode == base) return true;
        if (update) {
            if (mc.gameMode != null) {
                base = mc.gameMode;
                return true;
            } else return false;
        } else {
            throw new RuntimeException("Wrapped interaction manager doesn't match the current one in client");
        }
    }

    /**
     * @return the wrapped manager's current game mode
     * @since 1.9.0
     */
    @DocletReplaceReturn("Gamemode")
    public String getGameMode() {
        checkBase(autoUpdateBase);
        return base.getPlayerMode().getName();
    }

    /**
     * Sets the client's local game mode; it does not change the server's game mode.
     * @param gameMode game mode name; recognized names are case-insensitive, and unrecognized names retain the current mode
     * @return self for chaining
     * @since 1.9.0
     */
    @DocletReplaceParams("gameMode: Gamemode")
    public InteractionManagerHelper setGameMode(String gameMode) {
        checkBase(autoUpdateBase);
        base.setLocalMode(GameType.byName(gameMode.toLowerCase(Locale.ROOT), base.getPlayerMode()));
        return this;
    }

    /**
     * Overrides the crosshair hit result with the block at these coordinates and its down face.
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper setTarget(int x, int y, int z) {
        setTarget(x, y, z, 0);
        return this;
    }

    /**
     * Overrides the crosshair hit result with the block at these coordinates and the named face.
     * <p>
     * This is an override, not a read: it tells the game that the calls acting on "whatever the
     * player is looking at" should act on this block instead. It stays in place until
     * {@link #clearTargetOverride()} puts it back, so a script that sets one and does not clear
     * it has changed the game for everything that runs after it.
     * <br>
     * The override remains until cleared or removed by configured target checks. The other overloads
     * accept a {@link BlockPosHelper}, a 3D face data value, or an entity.
      * <pre>
     * const interactions = Player.interactions();
     * if (interactions === null) { throw new Error("not in a world"); }
     *
     * // act on the top face of a specific block, then put the crosshair back
     * interactions.setTarget(10, 64, -10, "up");
     * interactions.interactBlock(10, 64, -10, "up", false);
     * interactions.clearTargetOverride();
      * </pre>
      * @param x block x coordinate
      * @param y block y coordinate
      * @param z block z coordinate
      * @param direction block face name: {@code up}, {@code down}, {@code north}, {@code south}, {@code east}, or {@code west}
      * @return self for chaining
     * @since 1.9.0
     */
    @DocletReplaceParams("x: int, y: int, z: int, direction: Direction")
    @DocletDeclareType(name = "Direction", type = "'up' | 'down' | 'north' | 'south' | 'east' | 'west'")
    public InteractionManagerHelper setTarget(int x, int y, int z, String direction) {
        InteractionProxy.Target.setTargetBlock(new BlockPos(x, y, z), Direction.byName(direction.toLowerCase(Locale.ROOT)));
        return this;
    }

    /**
     * Overrides the crosshair hit result with the block at these coordinates and the face identified by its 3D data value.
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @param direction face's 3D data value (0-5: down, up, north, south, west, east)
     * @return self for chaining
     * @since 1.9.0
     */
    @DocletReplaceParams("x: int, y: int, z: int, direction: Hexit")
    public InteractionManagerHelper setTarget(int x, int y, int z, int direction) {
        InteractionProxy.Target.setTargetBlock(new BlockPos(x, y, z), Direction.from3DDataValue(direction));
        return this;
    }

    /**
     * Overrides the crosshair hit result with the block at {@code pos} and its down face.
     * @param pos block position
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper setTarget(BlockPosHelper pos) {
        setTarget(pos, 0);
        return this;
    }

    /**
     * Overrides the crosshair hit result with the block at {@code pos} and the named face.
     * @param pos block position
     * @param direction block face name: {@code up}, {@code down}, {@code north}, {@code south}, {@code east}, or {@code west}
     * @return self for chaining
     * @since 1.9.0
     */
    @DocletReplaceParams("pos: BlockPosHelper, direction: Direction")
    public InteractionManagerHelper setTarget(BlockPosHelper pos, String direction) {
        InteractionProxy.Target.setTargetBlock(pos.getRaw(), Direction.byName(direction.toLowerCase(Locale.ROOT)));
        return this;
    }

    /**
     * Overrides the crosshair hit result with the block at {@code pos} and the face identified by its 3D data value.
     * @param pos block position
     * @param direction face's 3D data value (0-5: down, up, north, south, west, east)
     * @return self for chaining
     * @since 1.9.0
     */
    @DocletReplaceParams("pos: BlockPosHelper, direction: Hexit")
    public InteractionManagerHelper setTarget(BlockPosHelper pos, int direction) {
        InteractionProxy.Target.setTargetBlock(pos.getRaw(), Direction.from3DDataValue(direction));
        return this;
    }

    /**
     * Overrides the crosshair hit result with the entity. Throws an {@code AssertionError} if the entity is not pickable or is the local player.
     * @param entity entity to target
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper setTarget(EntityHelper<?> entity) {
        if (!entity.getRaw().isPickable()) throw new AssertionError(String.format("Can't target not-hittable entity! (%s)", entity.getType()));
        if (entity.getRaw() == mc.player) throw new AssertionError("Can't target self!");
        InteractionProxy.Target.setTarget(new EntityHitResult(entity.getRaw()));
        return this;
    }

    /**
     * @return the current crosshair hit result, or {@code null} if none is available
     * @since 1.9.1
     */
    public @Nullable HitResultHelper<?> getTarget() {
        return HitResultHelper.resolve(mc.hitResult);
    }

    /**
     * @return the targeted block position, or {@code null} when the current hit result is not a block hit
     * @since 1.9.0
     */
    @Nullable
    public BlockPosHelper getTargetedBlock() {
        HitResult target = mc.hitResult;
        if (target != null && target.getType() == HitResult.Type.BLOCK) {
            return new BlockPosHelper(((BlockHitResult) target).getBlockPos());
        }
        return null;
    }

    /**
     * @return the entity currently under the crosshair, or {@code null} when there is no crosshair entity
     * @since 1.9.0
     */
    @Nullable
    public EntityHelper<?> getTargetedEntity() {
        if (mc.crosshairPickEntity != null) {
            return EntityHelper.create(mc.crosshairPickEntity);
        }
        return null;
    }

    /**
     * Overrides the crosshair hit result with a miss.
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper setTargetMissed() {
        InteractionProxy.Target.setTargetMissed();
        return this;
    }

    /**
     * @return whether a crosshair target override is set, including a miss override
     * @since 1.9.0
     */
    public boolean hasTargetOverride() {
        return InteractionProxy.Target.hasOverride();
    }

    /**
     * Clears the crosshair target override.
     * Revalidating the pending break override can end it and invoke its callbacks on the caller's thread.
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper clearTargetOverride() {
        InteractionProxy.Target.setTarget(null);
        return this;
    }

    /**
     * @param enabled whether to check override range; defaults to {@code true}
     * @param autoClear whether an out-of-range override is cleared; if {@code false}, it becomes a miss
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper setTargetRangeCheck(boolean enabled, boolean autoClear) {
        InteractionProxy.Target.checkDistance = enabled;
        InteractionProxy.Target.clearIfOutOfRange = autoClear;
        return this;
    }

    /**
     * @param enabled whether to check if a block target is air; defaults to {@code false}
     * @param autoClear whether an air-block override is cleared; if {@code false}, it becomes a miss
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper setTargetAirCheck(boolean enabled, boolean autoClear) {
        InteractionProxy.Target.checkAir = enabled;
        InteractionProxy.Target.clearIfIsAir = autoClear;
        return this;
    }

    /**
     * Controls whether block-target overrides are rejected when the block's collision shape is empty.
     * Air blocks are excluded from this check; use {@link #setTargetAirCheck(boolean, boolean)} to check air.
     * @param enabled whether to check for an empty block shape; defaults to {@code true}
     * @param autoClear whether an override with an empty shape is cleared; otherwise it becomes a miss. Defaults to {@code false}
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper setTargetShapeCheck(boolean enabled, boolean autoClear) {
        InteractionProxy.Target.checkShape = enabled;
        InteractionProxy.Target.clearIfEmptyShape = autoClear;
        return this;
    }

    /**
     * Resets the checks to defaults: range checking enabled and clears out-of-range targets;
     * air checking disabled and retains air targets; shape checking enabled and turns empty
     * shapes into misses.
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper resetTargetChecks() {
        InteractionProxy.Target.resetChecks();
        return this;
    }



    /**
     * @return self for chaining
     * @since 1.5.0
     */
    public InteractionManagerHelper attack() throws InterruptedException {
        return attack(false);
    }

    /**
     * @param await whether to wait for the queued client-thread attack call to finish when called outside the client thread and its joined-thread stack; calls on that stack execute directly. This does not wait for server confirmation
     * @return self for chaining
     * @since 1.6.0
     */
    public InteractionManagerHelper attack(boolean await) throws InterruptedException {
        boolean joinedMain = JsMacrosClient.clientCore.profile.checkJoinedThreadStack();
        if (joinedMain) {
            mc.startAttack();
        } else {
            Semaphore wait = new Semaphore(await ? 0 : 1);
            mc.execute(() -> {
                mc.startAttack();
                wait.release();
            });
            wait.acquire();
        }
        return this;
    }

    /**
     * Attacks the entity and swings the main-hand item. Does nothing if the interaction manager is unavailable.
     * @param entity entity to attack; targeting the local player throws {@code AssertionError}
     * @return self for chaining
     * @since 1.5.0
     */
    public InteractionManagerHelper attack(EntityHelper<?> entity) throws InterruptedException {
        return attack(entity, false);
    }

    /**
     * Attacks the entity and swings the main-hand item. Does nothing if the interaction manager is unavailable.
     * @param await whether to wait for the queued attack and swing to finish when called outside the client thread and its joined-thread stack; calls on that stack execute directly. This does not wait for server confirmation
     * @param entity entity to attack; targeting the local player throws {@code AssertionError}
     * @return self for chaining
     * @since 1.6.0
     */
    public InteractionManagerHelper attack(EntityHelper<?> entity, boolean await) throws InterruptedException {
        if (!checkBase(autoUpdateBase)) return this;
        boolean joinedMain = JsMacrosClient.clientCore.profile.checkJoinedThreadStack();
        if (entity.getRaw() == mc.player) {
            throw new AssertionError("Can't interact with self!");
        }
        if (joinedMain) {
            base.attack(mc.player, entity.getRaw());
            assert mc.player != null;
            mc.player.swing(InteractionHand.MAIN_HAND);
        } else {
            Semaphore wait = new Semaphore(await ? 0 : 1);
            mc.execute(() -> {
                base.attack(mc.player, entity.getRaw());
                assert mc.player != null;
                mc.player.swing(InteractionHand.MAIN_HAND);
                wait.release();
            });
            wait.acquire();
        }
        return this;
    }

    /**
     * Starts breaking the block at the given block coordinates and swings the main-hand item.
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @param direction block face name: {@code up}, {@code down}, {@code north}, {@code south}, {@code east}, or {@code west}
     * @return self for chaining.
     * @since 1.8.4
     */
    @DocletReplaceParams("x: int, y: int, z: int, direction: Direction")
    public InteractionManagerHelper attack(int x, int y, int z, String direction) throws InterruptedException {
        return attack(x, y, z, direction, false);
    }

    /**
     * Starts breaking the block at the given block coordinates and swings the main-hand item.
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @param direction face's 3D data value (0-5: down, up, north, south, west, east)
     * @return self for chaining
     * @since 1.5.0
     */
    @DocletReplaceParams("x: int, y: int, z: int, direction: Hexit")
    public InteractionManagerHelper attack(int x, int y, int z, int direction) throws InterruptedException {
        return attack(x, y, z, direction, false);
    }

    /**
     * Starts breaking the block at the given block coordinates and swings the main-hand item.
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @param direction block face name: {@code up}, {@code down}, {@code north}, {@code south}, {@code east}, or {@code west}
     * @param await whether to wait for the queued block-breaking call and swing to finish when called outside the client thread and its joined-thread stack; calls on that stack execute directly. This does not wait for server confirmation
     * @return self for chaining.
     * @since 1.8.4
     */
    @DocletReplaceParams("x: int, y: int, z: int, direction: Direction, await: boolean")
    public InteractionManagerHelper attack(int x, int y, int z, String direction, boolean await) throws InterruptedException {
        return attack(x, y, z, Direction.byName(direction.toLowerCase(Locale.ROOT)), await);
    }

    /**
     * Starts breaking the block at the given block coordinates and swings the main-hand item.
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @param direction face's 3D data value (0-5: down, up, north, south, west, east)
     * @param await whether to wait for the queued block-breaking call and swing to finish when called outside the client thread and its joined-thread stack; calls on that stack execute directly. This does not wait for server confirmation
     * @return self for chaining
     * @since 1.6.0
     */
    @DocletReplaceParams("x: int, y: int, z: int, direction: Hexit, await: boolean")
    public InteractionManagerHelper attack(int x, int y, int z, int direction, boolean await) throws InterruptedException {
        return attack(x, y, z, Direction.from3DDataValue(direction), await);
    }

    private InteractionManagerHelper attack(int x, int y, int z, Direction direction, boolean await) throws InterruptedException {
        if (!checkBase(autoUpdateBase)) return this;
        boolean joinedMain = JsMacrosClient.clientCore.profile.checkJoinedThreadStack();
        if (joinedMain) {
            base.startDestroyBlock(new BlockPos(x, y, z), direction);
            assert mc.player != null;
            mc.player.swing(InteractionHand.MAIN_HAND);
        } else {
            Semaphore wait = new Semaphore(await ? 0 : 1);
            mc.execute(() -> {
                base.startDestroyBlock(new BlockPos(x, y, z), direction);
                assert mc.player != null;
                mc.player.swing(InteractionHand.MAIN_HAND);
                wait.release();
            });
            wait.acquire();
        }
        return this;
    }

    /**
     * Breaks the currently targeted block and waits for the break result.
     * An immediate result is checked first; otherwise, calling on the client thread or a thread
     * joined to it throws {@link IllegalThreadStateException}. No targeted block is reported by
     * a callback outcome such as {@code NO_TARGET}, not by returning {@code null}.
     * @return the reported break result, or {@code UNAVAILABLE} when the immediate check finds no interaction manager
     * @throws InterruptedException
     * @since 1.9.0
     */
    @Nullable
    public InteractionProxy.Break.BreakBlockResult breakBlock() throws InterruptedException {
        InteractionProxy.Break.BreakBlockResult insta = checkInstaBreak();
        if (insta != null) return insta;
        if (JsMacrosClient.clientCore.profile.checkJoinedThreadStack()) {
            throw new IllegalThreadStateException("Attempted to wait on a thread that is currently joined to main!");
        }

        final InteractionProxy.Break.BreakBlockResult[] ret = {null};
        Semaphore wait = new Semaphore(0);

        InteractionProxy.Break.addCallback(res -> {
            ret[0] = res;
            wait.release();
        }, true);
        preBreakBlock();

        wait.acquire();
        return ret[0];
    }

    /**
     * Attempts to break the block at the given position, temporarily overriding the target when needed.
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @return the break result, or {@code null} if the position cannot be targeted
     * @throws InterruptedException
     * @since 1.9.0
     */
    @Nullable
    public InteractionProxy.Break.BreakBlockResult breakBlock(int x, int y, int z) throws InterruptedException {
        return breakBlock(new BlockPos(x, y, z));
    }

    /**
     * Attempts to break the block at {@code pos}, temporarily overriding the target when needed.
     * @param pos position to break
     * @return the break result, or {@code null} if the position cannot be targeted
     * @throws InterruptedException
     * @since 1.9.0
     */
    @Nullable
    public InteractionProxy.Break.BreakBlockResult breakBlock(BlockPosHelper pos) throws InterruptedException {
        return breakBlock(pos.getRaw());
    }

    @Nullable
    private InteractionProxy.Break.BreakBlockResult breakBlock(BlockPos pos) throws InterruptedException {
        InteractionProxy.Break.BreakBlockResult insta = checkInstaBreak(pos);
        if (insta != null) return insta;
        InteractionProxy.Target.setTargetBlock(pos, Direction.DOWN);
        InteractionProxy.Break.BreakBlockResult res = null;
        BlockPosHelper pos2 = getTargetedBlock();
        if (pos2 != null && pos2.getRaw().equals(pos)) res = breakBlock();
        clearTargetOverride();
        return res;
    }

    /**
     * Starts breaking the currently targeted block and delivers the outcome to {@code callback}.
     * A null callback starts the break without registering for its outcome. {@code SUCCESS}
     * means the client completed the break; it does not guarantee server acceptance.
     * Immediate-result callbacks are submitted to the client thread, and normal game-driven
     * completion runs there. Cancellation, target clearing, reset, or a break-override query can
     * instead invoke pending callbacks synchronously on the calling thread.
     * <pre>
     * const interactions = Player.interactions();
     * if (interactions === null) { throw new Error("not in a world"); }
     *
     * // start it and carry on, with the outcome arriving later
     * interactions.breakBlockAsync(JavaWrapper.methodToJava(function (result) {
     *   if (result !== null) {
     *     Chat.log(`break finished as ${result.reason}`);
     *   }
     * }));
     *
     * </pre>
     *
     * @param callback receives the break outcome
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper breakBlockAsync(@Nullable MethodWrapper<InteractionProxy.Break.BreakBlockResult, Object, ?, ?> callback) throws InterruptedException {
        InteractionProxy.Break.BreakBlockResult insta = checkInstaBreak();
        if (insta != null) {
            if (callback != null) mc.execute(() -> callback.accept(insta));
            return this;
        }
        InteractionProxy.Break.addCallback(callback, true);
        preBreakBlock();
        return this;
    }

    @Nullable
    private InteractionProxy.Break.BreakBlockResult checkInstaBreak() throws InterruptedException {
        HitResult target = mc.hitResult;
        if (target == null || target.getType() != HitResult.Type.BLOCK) return null;
        return checkInstaBreak(((BlockHitResult) target).getBlockPos());
    }

    @Nullable
    private InteractionProxy.Break.BreakBlockResult checkInstaBreak(BlockPos pos) throws InterruptedException {
        if (!checkBase(autoUpdateBase)) return InteractionProxy.Break.BreakBlockResult.UNAVAILABLE;
        if (mc.level == null || mc.player == null
        ||  ((IClientPlayerInteractionManager) base).jsmacros_getBlockBreakingCooldown() != 0
        ||  mc.level.getBlockState(pos).getDestroyProgress(mc.player, mc.player.level(), pos) < 1.0F
        ) return null;
        int side = 0;
        HitResult target = mc.hitResult;
        if (target != null && target.getType() == HitResult.Type.BLOCK) {
            side = ((BlockHitResult) target).getDirection().get3DDataValue();
        }
        attack(pos.getX(), pos.getY(), pos.getZ(), side, true);
        return new InteractionProxy.Break.BreakBlockResult("SUCCESS", new BlockPosHelper(pos));
    }

    private void preBreakBlock() throws InterruptedException {
        if (((IClientPlayerInteractionManager) base).jsmacros_getBlockBreakingCooldown() == 0) {
            HitResult target = mc.hitResult;
            if (target == null || target.getType() != HitResult.Type.BLOCK) return;
            BlockPos pos = ((BlockHitResult) target).getBlockPos();
            attack(pos.getX(), pos.getY(), pos.getZ(), ((BlockHitResult) target).getDirection(), true);
        }
    }

    /**
     * @return whether the current interaction manager is destroying a block
     * @since 1.8.0
     */
    public boolean isBreakingBlock() {
        checkBase(autoUpdateBase);
        return base.isDestroying();
    }

    /**
     * Checks whether a break override is active. This can terminate an invalid override and
     * dispatch pending callbacks synchronously on the caller's thread.
     * @return {@code true} while a valid break override remains active
     * @since 1.9.0
     */
    public boolean hasBreakBlockOverride() {
        return InteractionProxy.Break.isBreaking();
    }

    /**
     * Cancels the active block-break override and reports {@code CANCELLED} to registered callbacks.
     * Callbacks run synchronously on the caller's thread.
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper cancelBreakBlock() {
        InteractionProxy.Break.setOverride(false, "CANCELLED");
        return this;
    }



    /** Starts using the main-hand item.
     * @return self for chaining
     * @since 1.5.0
     */
    public InteractionManagerHelper interact() throws InterruptedException {
        return interact(false);
    }

    /**
     * @param await whether to wait for the queued client-thread invocation; calls on the client or
     *              joined-thread stack execute directly, and no server confirmation is awaited
     * @return self for chaining
     * @since 1.6.0
     */
    public InteractionManagerHelper interact(boolean await) throws InterruptedException {
        boolean joinedMain = JsMacrosClient.clientCore.profile.checkJoinedThreadStack();
        if (joinedMain) {
           mc.startUseItem();
        } else {
            Semaphore wait = new Semaphore(await ? 0 : 1);
            mc.execute(() -> {
                mc.startUseItem();
                wait.release();
            });
            wait.acquire();
        }
        return this;
    }

    /**
     * Interacts with {@code entity} using the selected hand.
     * @param entity entity to interact with; the local player's entity causes {@code AssertionError}
     * @param offHand whether to use the off hand instead of the main hand
     * @return self for chaining
     * @since 1.5.0, renamed from {@code interact} in 1.6.0
     */
    public InteractionManagerHelper interactEntity(EntityHelper<?> entity, boolean offHand) throws InterruptedException {
        return interactEntity(entity, offHand, false);
    }

    /** Interacts with an entity using the selected hand.
     * @param entity entity to interact with; the player's own entity causes an {@link AssertionError}
     * @param offHand whether to use the off hand instead of the main hand
     * @param await whether to wait for the queued client-thread task to run; it does not wait for server confirmation
     * @return self for chaining
     * @since 1.6.0
     */
    public InteractionManagerHelper interactEntity(EntityHelper<?> entity, boolean offHand, boolean await) throws InterruptedException {
        if (!checkBase(autoUpdateBase)) return this;
        if (entity.getRaw() == mc.player) {
            throw new AssertionError("Can't interact with self!");
        }
        InteractionHand hand = offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        boolean joinedMain = JsMacrosClient.clientCore.profile.checkJoinedThreadStack();
        if (joinedMain) {
            InteractionResult result = InteractionCompat.interact(base, mc.player, entity.getRaw(), hand);
            assert mc.player != null;
            if (result.consumesAction()) {
                mc.player.swing(hand);
            }
        } else {
            Semaphore wait = new Semaphore(await ? 0 : 1);
            mc.execute(() -> {
                InteractionResult result = InteractionCompat.interact(base, mc.player, entity.getRaw(), hand);
                assert mc.player != null;
                if (result.consumesAction()) {
                    mc.player.swing(hand);
                }
                wait.release();
            });
            wait.acquire();
        }
        return this;
    }

    /** Uses the selected hand's held item.
     * @param offHand whether to use the off hand instead of the main hand
     * @return self for chaining
     * @since 1.5.0, renamed from {@code interact} in 1.6.0
     */
    public InteractionManagerHelper interactItem(boolean offHand) throws InterruptedException {
        return interactItem(offHand, false);
    }

    /** Uses the selected hand's held item.
     * @param offHand whether to use the off hand instead of the main hand
     * @param await whether to wait for the queued client-thread task to run; it does not wait for server confirmation
     * @return self for chaining
     * @since 1.6.0
     */
    public InteractionManagerHelper interactItem(boolean offHand, boolean await) throws InterruptedException {
        if (!checkBase(autoUpdateBase)) return this;
        InteractionHand hand = offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        boolean joinedMain = JsMacrosClient.clientCore.profile.checkJoinedThreadStack();
        if (joinedMain) {
            InteractionResult result = base.useItem(mc.player, hand);
            assert mc.player != null;
            if (result.consumesAction()) {
                mc.player.swing(hand);
            }
        } else {
            Semaphore wait = new Semaphore(await ? 0 : 1);
            mc.execute(() -> {
                InteractionResult result = base.useItem(mc.player, hand);
                assert mc.player != null;
                if (result.consumesAction()) {
                    mc.player.swing(hand);
                }
                wait.release();
            });
            wait.acquire();
        }
        return this;
    }

    /**
     * Interacts with the block at the given coordinates, using the face named by {@code direction}.
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @param direction face name: {@code up}, {@code down}, {@code north}, {@code south}, {@code east}, or {@code west}
     * @param offHand whether to use the off hand instead of the main hand
     * @return this helper for chaining
     * @since 1.8.4
     */
    @DocletReplaceParams("x: int, y: int, z: int, direction: Direction, offHand: boolean")
    public InteractionManagerHelper interactBlock(int x, int y, int z, String direction, boolean offHand) throws InterruptedException {
        return interactBlock(x, y, z, direction, offHand, false);
    }

    /**
     * Interacts with the block at the given coordinates, using the face selected by its 3D data value.
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @param direction face data value: {@code 0} down, {@code 1} up, {@code 2} north, {@code 3} south, {@code 4} west, {@code 5} east
     * @param offHand whether to use the off hand instead of the main hand
     * @return self for chaining
     * @since 1.5.0, renamed from {@code interact} in 1.6.0
     */
    @DocletReplaceParams("x: int, y: int, z: int, direction: Hexit, offHand: boolean")
    public InteractionManagerHelper interactBlock(int x, int y, int z, int direction, boolean offHand) throws InterruptedException {
        return interactBlock(x, y, z, direction, offHand, false);
    }

    /**
     * Interacts with the block at the given coordinates, using the face named by {@code direction}.
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @param direction face name: {@code up}, {@code down}, {@code north}, {@code south}, {@code east}, or {@code west}
     * @param offHand whether to use the off hand instead of the main hand
     * @param await whether to wait for the queued client-thread task to run; it does not wait for server confirmation
     * @return this helper for chaining
     * @since 1.8.4
     */
    @DocletReplaceParams("x: int, y: int, z: int, direction: Direction, offHand: boolean, await: boolean")
    public InteractionManagerHelper interactBlock(int x, int y, int z, String direction, boolean offHand, boolean await) throws InterruptedException {
        return interactBlock(x, y, z, Direction.byName(direction.toLowerCase(Locale.ROOT)), offHand, await);
    }

    /**
     * Interacts with the block at the given coordinates, using the face selected by its 3D data value.
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @param direction face data value: {@code 0} down, {@code 1} up, {@code 2} north, {@code 3} south, {@code 4} west, {@code 5} east
     * @param offHand whether to use the off hand instead of the main hand
     * @param await whether to wait for the queued client-thread task to run; it does not wait for server confirmation
     * @return this helper for chaining
     * @since 1.5.0, renamed from {@code interact} in 1.6.0
     */
    @DocletReplaceParams("x: int, y: int, z: int, direction: Hexit, offHand: boolean, await: boolean")
    public InteractionManagerHelper interactBlock(int x, int y, int z, int direction, boolean offHand, boolean await) throws InterruptedException {
        return interactBlock(x, y, z, Direction.from3DDataValue(direction), offHand, await);
    }

    private InteractionManagerHelper interactBlock(int x, int y, int z, Direction direction, boolean offHand, boolean await) throws InterruptedException {
        if (!checkBase(autoUpdateBase)) return this;
        InteractionHand hand = offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        boolean joinedMain = JsMacrosClient.clientCore.profile.checkJoinedThreadStack();
        if (joinedMain) {
            InteractionResult result = base.useItemOn(mc.player, hand,
                    new BlockHitResult(new Vec3(x, y, z), direction, new BlockPos(x, y, z), false)
            );
            assert mc.player != null;
            if (result.consumesAction()) {
                mc.player.swing(hand);
            }
        } else {
            Semaphore wait = new Semaphore(await ? 0 : 1);
            mc.execute(() -> {
                InteractionResult result = base.useItemOn(mc.player, hand,
                        new BlockHitResult(new Vec3(x, y, z), direction, new BlockPos(x, y, z), false)
                );
                assert mc.player != null;
                if (result.consumesAction()) {
                    mc.player.swing(hand);
                }
                wait.release();
            });
            wait.acquire();
        }
        return this;
    }

    /**
     * Starts or stops the continuous interact override.
     * @param holding whether to enable or disable the override
     * @return this helper for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper holdInteract(boolean holding) throws InterruptedException {
        return holdInteract(holding, false);
    }

    /**
     * Starts or stops the continuous interact override.
     * Disabling runs directly on the caller's thread. Enabling runs directly on the client or
     * joined-thread stack, otherwise it is queued on the client thread.
     * @param holding whether to enable or disable the override
     * @param awaitFirstClick whether to wait for queued enabling work, not server confirmation; ignored when disabling
     * @return this helper for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper holdInteract(boolean holding, boolean awaitFirstClick) throws InterruptedException {
        if (!holding) {
            InteractionProxy.Interact.setOverride(false);
            return this;
        }
        boolean joinedMain = JsMacrosClient.clientCore.profile.checkJoinedThreadStack();
        if (joinedMain) {
            InteractionProxy.Interact.setOverride(true);
        } else {
            Semaphore wait = new Semaphore(awaitFirstClick ? 0 : 1);
            mc.execute(() -> {
                InteractionProxy.Interact.setOverride(true);
                wait.release();
            });
            wait.acquire();
        }
        return this;
    }

    /**
     * Holds interact for up to the specified number of unpaused game ticks, stopping when paused
     * or when the override stops. Calling on the client or joined-thread stack throws
     * {@link IllegalThreadStateException}. The hold is released on normal return; interruption
     * while waiting can leave it active.
     * @param ticks number of ticks to hold
     * @return ticks not yet elapsed; nonzero if the hold was stopped by pausing with {@code stopOnPause} enabled or if the override stopped, and {@code 0} when the full duration elapsed
     * @throws InterruptedException
     * @since 1.9.0
     */
    public int holdInteract(int ticks) throws InterruptedException {
        return holdInteract(ticks, true);
    }

    /**
     * Holds interact for up to the specified number of unpaused game ticks. Calling on the client
     * or joined-thread stack throws {@link IllegalThreadStateException}. The hold is released on
     * normal return; interruption while waiting can leave it active.
     * @param ticks number of unpaused ticks to hold
     * @param stopOnPause whether to stop when the game is paused; if {@code false}, paused ticks do not count and the hold resumes after unpausing
     * @return ticks remaining; nonzero if paused with {@code stopOnPause} enabled or if the override stopped, and {@code 0} when the full duration elapsed
     * @throws InterruptedException
     * @since 1.9.0
     */
    public int holdInteract(int ticks, boolean stopOnPause) throws InterruptedException {
        if (JsMacrosClient.clientCore.profile.checkJoinedThreadStack()) {
            throw new IllegalThreadStateException("Attempted to wait on a thread that is currently joined to main!");
        }

        holdInteract(true, true);
        while (ticks > 0) {
            FClient.tickSynchronizer.waitTick();
            if (!InteractionProxy.Interact.isInteracting()) break;
            if (!mc.isPaused()) ticks--;
            else if (stopOnPause) break;
        }
        holdInteract(false);
        return ticks;
    }

    /**
     * @return whether the hold-interact override is active
     * @since 1.9.0
     */
    public boolean hasInteractOverride() {
        return InteractionProxy.Interact.isInteracting();
    }

}
