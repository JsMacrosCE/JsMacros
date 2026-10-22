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
 * Helper for ClientPlayerInteractionManager
 * it accesses interaction manager from {@code mc} instead of {@code base}, to avoid issues
 * <p>
 * Everything the player does to the world goes through one object — breaking a block,
 * attacking, using an item on something, right-clicking — and this is a handle on it. It is
 * reached through {@code Player.getInteractionManager()}, or the shorter
 * {@code Player.interactions()}, and it is {@code null} outside a world.
 * <p>
 * Three things are worth knowing before using it.
 * <br>
 * <b>Targeting.</b> The calls that act on "whatever the player is looking at" use the
 * crosshair, and the game changes what the crosshair is on continuously. The {@code setTarget}
 * family overrides that, so a script can aim at a specific block or entity and have the call
 * act on it instead. An override lasts until it is cleared, so a script that sets one and does
 * not clear it has changed the game for everything after it;
 * {@link #clearTargetOverride()} puts it back and {@link #hasTargetOverride()} says whether
 * one is in place.
 * <br>
 * <b>Waiting.</b> Several calls take an {@code await} argument: with {@code false} they queue
 * the work and return at once, and with {@code true} they block until the server has answered.
 * Waiting is the thing that cannot be done from the main thread, so a call that waits and is
 * made <i>on</i> the main thread throws rather than freezing the game. The two failures are
 * separate and worth keeping apart: that main-thread check raises
 * {@link java.lang.IllegalThreadStateException}, and the {@code throws InterruptedException} on
 * the blocking calls comes from the {@code Semaphore} they wait on, which is not the same thing
 * as refusing the call. Either way the advice is the same: these are the ones to run from a
 * script's own thread, not from inside a main-thread callback.
 * <br>
 * <b>Breaking a block takes time.</b> {@link #breakBlock()} does the whole thing — starts the
 * break, waits for it to finish, and reports what happened — which is why it blocks and why it
 * can end in an interruption rather than a success. {@link #breakBlockAsync} is the version
 * that hands the result to a callback instead of blocking, and is usually the better one in an
 * event.
 * example:
 * <pre>
 * const interactions = Player.interactions();
 * if (interactions === null) { throw new Error("not in a world"); }
 *
 * // act on whatever the player is looking at
 * interactions.breakBlock();
 *
 * // act on a specific block instead, then put the crosshair back
 * interactions.setTarget(10, 64, -10);
 * const result = interactions.breakBlock();
 * interactions.clearTargetOverride();
 * if (result !== null) {
 *   Chat.log(`break finished as ${result.reason}`);
 * }
 *
 * // holding an interact for a number of ticks, and letting go afterwards
 * const left = interactions.holdInteract(20);
 * Chat.log(`${left} ticks were not spent`);
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
     * indicates if the helper should auto update the base manager, default is true<br>
     * when the base doesn't equal to the current manager,<br>
     *  if this is false, raise an error;<br>
     *  else if base is updated, the method works as usual;<br>
     *  else if the method don't need manager or network interaction, work as usual with old manager;<br>
     *  else the method does nothing
     */
    public boolean autoUpdateBase = true;

    public InteractionManagerHelper(MultiPlayerGameMode base) {
        super(base);
    }

    /**
     * checks if the base matches the current manager
     * @param update true if the base should be updated. otherwise it'll raise an error if it's not up-to-date
     * @return true if base is available
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
     * @return the player's current gamemode.
     * @since 1.9.0
     */
    @DocletReplaceReturn("Gamemode")
    public String getGameMode() {
        checkBase(autoUpdateBase);
        return base.getPlayerMode().getName();
    }

    /**
     * @param gameMode possible values are survival, creative, adventure, spectator (case insensitive)
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
     * sets crosshair target to a block
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper setTarget(int x, int y, int z) {
        setTarget(x, y, z, 0);
        return this;
    }

    /**
     * sets crosshair target to a block
     * <p>
     * This is an override, not a read: it tells the game that the calls acting on "whatever the
     * player is looking at" should act on this block instead. It stays in place until
     * {@link #clearTargetOverride()} puts it back, so a script that sets one and does not clear
     * it has changed the game for everything that runs after it.
     * <br>
     * There are seven forms, and this is one of the six block-position ones. A block position is
     * given either as {@code x}, {@code y} and {@code z} ints or as a {@link BlockPosHelper} a
     * script already has; each of those two can then be paired with the direction as a string
     * like this one, as an {@code int} that is the game's 3D data value, or left out. The
     * direction is which face of the block the call acts on, and the no-direction form uses the
     * down face. The seventh form takes a {@code EntityHelper} and targets an entity instead of
     * a block.
     * example:
     * <pre>
     * const interactions = Player.interactions();
     * if (interactions === null) { throw new Error("not in a world"); }
     *
     * // act on the top face of a specific block, then put the crosshair back
     * interactions.setTarget(10, 64, -10, "up");
     * interactions.interactBlock(10, 64, -10, "up", false);
     * interactions.clearTargetOverride();
     * </pre>
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
     * sets crosshair target to a block
     * @return self for chaining
     * @since 1.9.0
     */
    @DocletReplaceParams("x: int, y: int, z: int, direction: Hexit")
    public InteractionManagerHelper setTarget(int x, int y, int z, int direction) {
        InteractionProxy.Target.setTargetBlock(new BlockPos(x, y, z), Direction.from3DDataValue(direction));
        return this;
    }

    /**
     * sets crosshair target to a block
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper setTarget(BlockPosHelper pos) {
        setTarget(pos, 0);
        return this;
    }

    /**
     * sets crosshair target to a block
     * @return self for chaining
     * @since 1.9.0
     */
    @DocletReplaceParams("bpos: BlockPosHelper, direction: Direction")
    public InteractionManagerHelper setTarget(BlockPosHelper pos, String direction) {
        InteractionProxy.Target.setTargetBlock(pos.getRaw(), Direction.byName(direction.toLowerCase(Locale.ROOT)));
        return this;
    }

    /**
     * sets crosshair target to a block
     * @return self for chaining
     * @since 1.9.0
     */
    @DocletReplaceParams("bpos: BlockPosHelper, direction: Hexit")
    public InteractionManagerHelper setTarget(BlockPosHelper pos, int direction) {
        InteractionProxy.Target.setTargetBlock(pos.getRaw(), Direction.from3DDataValue(direction));
        return this;
    }

    /**
     * sets crosshair target to an entity
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
     * @return current hitResult
     * @since 1.9.1
     */
    public @Nullable HitResultHelper<?> getTarget() {
        return HitResultHelper.resolve(mc.hitResult);
    }

    /**
     * @return targeted block pos, null if not targeting block
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
     * @return targeted entity, null if not targeting entity
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
     * sets crosshair target to missed (doesn't target anything)
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper setTargetMissed() {
        InteractionProxy.Target.setTargetMissed();
        return this;
    }

    /**
     * @return {@code true} if target have been set by {@code ClientPlayerEntityHelper#setTarget()} or
     *  {@code ClientPlayerEntityHelper#setTargetMissed()}
     * @since 1.9.0
     */
    public boolean hasTargetOverride() {
        return InteractionProxy.Target.hasOverride();
    }

    /**
     * clears target override
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper clearTargetOverride() {
        InteractionProxy.Target.setTarget(null);
        return this;
    }

    /**
     * @param enabled if target overriding should check range. default is {@code true}
     * @param autoClear if override should clear when out of range.
     *                  if {@code false}, target will set to missed if out of range. default is {@code true}
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper setTargetRangeCheck(boolean enabled, boolean autoClear) {
        InteractionProxy.Target.checkDistance = enabled;
        InteractionProxy.Target.clearIfOutOfRange = autoClear;
        return this;
    }

    /**
     * @param enabled if target overriding should check air. default is {@code false}
     * @param autoClear if override should clear when is air.
     *                  if {@code false}, target will set to missed if is air. default is {@code false}
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper setTargetAirCheck(boolean enabled, boolean autoClear) {
        InteractionProxy.Target.checkAir = enabled;
        InteractionProxy.Target.clearIfIsAir = autoClear;
        return this;
    }

    /**
     * this check ignores air. use {@code ClientPlayerEntityHelper#setTargetAirCheck()} to check air.
     * @param enabled if target overriding should check block shape. default is {@code true}
     * @param autoClear if override should clear when shape is empty.
     *                  if {@code false}, target will set to missed if is empty. default is {@code false}
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper setTargetShapeCheck(boolean enabled, boolean autoClear) {
        InteractionProxy.Target.checkShape = enabled;
        InteractionProxy.Target.clearIfEmptyShape = autoClear;
        return this;
    }

    /**
     * resets all range, air and shape check settings to default.
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper resetTargetChecks() {
        InteractionProxy.Target.resetChecks();
        return this;
    }



    /**
     * @since 1.5.0
     */
    public InteractionManagerHelper attack() throws InterruptedException {
        return attack(false);
    }

    /**
     * @param await
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
     * @param entity
     * @since 1.5.0
     */
    public InteractionManagerHelper attack(EntityHelper<?> entity) throws InterruptedException {
        return attack(entity, false);
    }

    /**
     * @param await
     * @param entity
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
     * @param x         the x coordinate to attack
     * @param y         the y coordinate to attack
     * @param z         the z coordinate to attack
     * @param direction possible values are "up", "down", "north", "south", "east", "west"
     * @return self for chaining.
     * @since 1.8.4
     */
    @DocletReplaceParams("x: int, y: int, z: int, direction: Direction")
    public InteractionManagerHelper attack(int x, int y, int z, String direction) throws InterruptedException {
        return attack(x, y, z, direction, false);
    }

    /**
     * @param x
     * @param y
     * @param z
     * @param direction 0-5 in order: [DOWN, UP, NORTH, SOUTH, WEST, EAST];
     * @since 1.5.0
     */
    @DocletReplaceParams("x: int, y: int, z: int, direction: Hexit")
    public InteractionManagerHelper attack(int x, int y, int z, int direction) throws InterruptedException {
        return attack(x, y, z, direction, false);
    }

    /**
     * @param x         the x coordinate to attack
     * @param y         the y coordinate to attack
     * @param z         the z coordinate to attack
     * @param direction possible values are "up", "down", "north", "south", "east", "west"
     * @param await     whether to wait for the attack to finish
     * @return self for chaining.
     * @since 1.8.4
     */
    @DocletReplaceParams("x: int, y: int, z: int, direction: Direction, await: boolean")
    public InteractionManagerHelper attack(int x, int y, int z, String direction, boolean await) throws InterruptedException {
        return attack(x, y, z, Direction.byName(direction.toLowerCase(Locale.ROOT)), await);
    }

    /**
     * @param x
     * @param y
     * @param z
     * @param direction 0-5 in order: [DOWN, UP, NORTH, SOUTH, WEST, EAST];
     * @param await
     * @throws InterruptedException
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
     * breaks a block, will wait till it's done<br>
     * you can use {@code ClientPlayerEntityHelper#setTarget()} to specify which block to break
     * @return result, or null if interaction manager is unavailable
     * @see InteractionManagerHelper#setTarget(int, int, int, String)
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
     * breaks a block, will wait till it's done<br>
     * this is the same as pointing the crosshair at the block, breaking what is targeted, and
     * then putting the crosshair back:
     * <pre>
     * const interactions = Player.interactions();
     * if (interactions === null) { throw new Error("not in a world"); }
     *
     * interactions.setTarget(10, 64, -10);
     * // only break if the override actually landed on that block, since a
     * // reach check or another script may have moved it in the meantime
     * let res = null;
     * const aimed = interactions.getTargetedBlock();
     * if (aimed !== null) {
     *   const where = aimed.getX() + " " + aimed.getY() + " " + aimed.getZ();
     *   if (where === "10 64 -10") {
     *     res = interactions.breakBlock();
     *   }
     * }
     * interactions.clearTargetOverride();
     * </pre>
     * @return result, or {@code null} if the target moved before the break could start
     * @throws InterruptedException
     * @since 1.9.0
     */
    @Nullable
    public InteractionProxy.Break.BreakBlockResult breakBlock(int x, int y, int z) throws InterruptedException {
        return breakBlock(new BlockPos(x, y, z));
    }

    /**
     * breaks a block, will wait till it's done<br>
     * the same as the three-coordinate form, aimed at a position the script already has:
     * <pre>
     * const interactions = Player.interactions();
     * if (interactions === null) { throw new Error("not in a world"); }
     *
     * const pos = PositionCommon.createBlockPos(10, 64, -10);
     * interactions.setTarget(pos);
     * let res = null;
     * const aimed = interactions.getTargetedBlock();
     * if (aimed !== null) {
     *   const where = aimed.getX() + " " + aimed.getY() + " " + aimed.getZ();
     *   const want = pos.getX() + " " + pos.getY() + " " + pos.getZ();
     *   if (where === want) {
     *     res = interactions.breakBlock();
     *   }
     * }
     * interactions.clearTargetOverride();
     * </pre>
     * @return result, or {@code null} if the target moved before the break could start
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
     * starts breaking a block<br>
     * you can use {@code ClientPlayerEntityHelper#setTarget()} to specify which block to break
     * <p>
     * The non-blocking counterpart to {@link #breakBlock()}: it starts the break and returns,
     * and the result arrives at the callback. That makes it the one to use inside an event,
     * where blocking would hold up the game, and the callback runs on the main thread, so
     * anything it wants to read has to be handed to it rather than reached for.
     * <p>
     * The result is a reason string and, on success, the position that was broken. A
     * {@code SUCCESS} means the client finished the break, not that the server accepted it: a
     * protected block or a lagging server will still refuse it.
     * <p>
     * A {@code null} callback is allowed, and is the way to start a break without caring how it
     * turns out.
     * <br>
     * To break a block at a known position rather than the targeted one, set the target first
     * with {@link #setTarget(int, int, int)} and clear it afterwards.
     * example:
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
     * // and to stop one part way through
     * // interactions.cancelBreakBlock();
     * </pre>
     *
     * @param callback this will mostly be called on main thread!
     *                 Use {@code methodToJavaAsync()} instead of {@code methodToJava()} to avoid errors.
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
     * @since 1.8.0
     */
    public boolean isBreakingBlock() {
        checkBase(autoUpdateBase);
        return base.isDestroying();
    }

    /**
     * @return {@code true} if there's not finished block breaking from {@code ClientPlayerEntityHelper#breakBlock()}
     * @since 1.9.0
     */
    public boolean hasBreakBlockOverride() {
        return InteractionProxy.Break.isBreaking();
    }

    /**
     * cancels breaking block that previously started by {@code ClientPlayerEntityHelper#breakBlock()} or
     *  {@code ClientPlayerEntityHelper#breakBlockAsync()}
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper cancelBreakBlock() {
        InteractionProxy.Break.setOverride(false, "CANCELLED");
        return this;
    }



    /**
     * @since 1.5.0
     */
    public InteractionManagerHelper interact() throws InterruptedException {
        return interact(false);
    }

    /**
     * @param await
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
     * @param entity
     * @param offHand
     * @since 1.5.0, renamed from {@code interact} in 1.6.0
     */
    public InteractionManagerHelper interactEntity(EntityHelper<?> entity, boolean offHand) throws InterruptedException {
        return interactEntity(entity, offHand, false);
    }

    /**
     * @param entity
     * @param offHand
     * @param await
     * @throws InterruptedException
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

    /**
     * @param offHand
     * @since 1.5.0, renamed from {@code interact} in 1.6.0
     */
    public InteractionManagerHelper interactItem(boolean offHand) throws InterruptedException {
        return interactItem(offHand, false);
    }

    /**
     * @param offHand
     * @param await
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
     * @param x         the x coordinate to interact
     * @param y         the y coordinate to interact
     * @param z         the z coordinate to interact
     * @param direction possible values are "up", "down", "north", "south", "east", "west"
     * @return self for chaining.
     * @since 1.8.4
     */
    @DocletReplaceParams("x: int, y: int, z: int, direction: Direction, offHand: boolean")
    public InteractionManagerHelper interactBlock(int x, int y, int z, String direction, boolean offHand) throws InterruptedException {
        return interactBlock(x, y, z, direction, offHand, false);
    }

    /**
     * @param x
     * @param y
     * @param z
     * @param direction 0-5 in order: [DOWN, UP, NORTH, SOUTH, WEST, EAST];
     * @param offHand
     * @since 1.5.0, renamed from {@code interact} in 1.6.0
     */
    @DocletReplaceParams("x: int, y: int, z: int, direction: Hexit, offHand: boolean")
    public InteractionManagerHelper interactBlock(int x, int y, int z, int direction, boolean offHand) throws InterruptedException {
        return interactBlock(x, y, z, direction, offHand, false);
    }

    /**
     * @param x         the x coordinate to interact
     * @param y         the y coordinate to interact
     * @param z         the z coordinate to interact
     * @param direction possible values are "up", "down", "north", "south", "east", "west"
     * @param await     whether to wait for the interaction to complete
     * @return self for chaining.
     * @since 1.8.4
     */
    @DocletReplaceParams("x: int, y: int, z: int, direction: Direction, offHand: boolean, await: boolean")
    public InteractionManagerHelper interactBlock(int x, int y, int z, String direction, boolean offHand, boolean await) throws InterruptedException {
        return interactBlock(x, y, z, Direction.byName(direction.toLowerCase(Locale.ROOT)), offHand, await);
    }

    /**
     * @param x
     * @param y
     * @param z
     * @param direction 0-5 in order: [DOWN, UP, NORTH, SOUTH, WEST, EAST];
     * @param offHand
     * @param await     whether to wait for the interaction to complete
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
     * starts/stops long interact
     * @return self for chaining
     * @since 1.9.0
     */
    public InteractionManagerHelper holdInteract(boolean holding) throws InterruptedException {
        return holdInteract(holding, false);
    }

    /**
     * starts/stops long interact
     * @return self for chaining
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
     * interacts for specified number of ticks
     * <p>
     * This holds the interact down for a number of game ticks, which is what a door or a button
     * needs, and it is a blocking call: the script waits while the ticks go by. It cannot be run
     * on the main thread for the same reason {@link #breakBlock()} cannot.
     * <br>
     * The return value is what was left over, not what was done. It is the full {@code ticks}
     * when the interaction was cut short — the player interrupted it, or the target changed —
     * and {@code 0} when all of them were spent, so a script that cares whether the hold
     * actually lasted has to compare the two.
     * <br>
     * The interaction is released before this returns, so a script does not have to pair it with
     * {@link #holdInteract(boolean)}.
     * example:
     * <pre>
     * const interactions = Player.interactions();
     * if (interactions === null) { throw new Error("not in a world"); }
     *
     * // hold for one second, which is twenty ticks
     * const left = interactions.holdInteract(20);
     * if (left !== 0) {
     *   Chat.log(`the hold was cut short with ${left} ticks left`);
     * }
     * </pre>
     *
     * @return remaining ticks if the interaction was interrupted
     * @throws InterruptedException
     * @since 1.9.0
     */
    public int holdInteract(int ticks) throws InterruptedException {
        return holdInteract(ticks, true);
    }

    /**
     * interacts for specified number of ticks
     * @param stopOnPause if {@code false}, this interaction will not return when interrupted by pause.
     *                    the timer will not decrease, meaning it'll continue right after unpause and interact exact amount of ticks.
     * @return remaining ticks if the interaction was interrupted
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
     * @return {@code true} if interaction from {@code ClientPlayerEntityHelper#holdInteract()} is active
     * @since 1.9.0
     */
    public boolean hasInteractOverride() {
        return InteractionProxy.Interact.isInteracting();
    }

}
