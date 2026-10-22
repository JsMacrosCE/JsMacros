package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.api.math.Vec3D;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.access.IItemCooldownEntry;
import com.jsmacrosce.jsmacros.client.access.IItemCooldownManager;
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;
import com.jsmacrosce.jsmacros.client.api.helper.AdvancementManagerHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;
import com.jsmacrosce.jsmacros.client.util.InteractionCompat;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * Provides client-side operations for the local player.
 * <p>
 * Position setters schedule changes on the client thread when called off-thread; their
 * {@code await} overloads wait for the scheduled task to run. Calls from the client
 * thread apply immediately.
 * <pre>
 * const player = Player.getPlayer();
 * if (player !== null) {
 *   // turn to face a point, which is the form that works from any thread
 *   player.lookAt(player.getX() + 10, player.getY(), player.getZ());
 *   // and nudge the player along
 *   player.addPos(0, 1, 0);
 *   Chat.log(`now at ${player.getPos()}`);
 * }
 * </pre>
 * @param <T> the concrete local-player type wrapped by this helper
 *
 * @author Wagyourtail
 * @since 1.0.3
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class ClientPlayerEntityHelper<T extends LocalPlayer> extends PlayerEntityHelper<T> {
    protected final Minecraft mc = Minecraft.getInstance();

    /** Wraps the supplied local player and retains that reference.
     * @param e the local player to wrap
     */
    public ClientPlayerEntityHelper(T e) {
        super(e);
    }

    /**
     * @since 1.8.4
     */
    private ClientPlayerEntityHelper<T> setVelocity(Vec3 velocity) {
        base.setDeltaMovement(velocity);
        return this;
    }

    /**
     * Replaces the player's velocity with the supplied vector.
     *
     * @param velocity the movement to give the player, per tick
     * @return self for chaining.
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> setVelocity(Pos3D velocity) {
        return setVelocity(new Vec3(velocity.x, velocity.y, velocity.z));
    }

    /**
     * Replaces the player's velocity with the supplied components.
     *
     * @param x the movement on the x axis, per tick
     * @param y the movement on the y axis, per tick
     * @param z the movement on the z axis, per tick
     * @return self for chaining.
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> setVelocity(double x, double y, double z) {
        return setVelocity(new Vec3(x, y, z));
    }

    /**
     * @since 1.8.4
     */
    private ClientPlayerEntityHelper<T> addVelocity(Vec3 velocity) {
        base.push(velocity);
        return this;
    }

    /**
     * Adds the supplied vector to the player's current velocity.
     *
     * @param velocity the movement to add, per tick
     * @return self for chaining.
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> addVelocity(Pos3D velocity) {
        return addVelocity(new Vec3(velocity.x, velocity.y, velocity.z));
    }

    /**
     * Adds the supplied components to the player's current velocity.
     *
     * @param x the movement to add on the x axis, per tick
     * @param y the movement to add on the y axis, per tick
     * @param z the movement to add on the z axis, per tick
     * @return self for chaining.
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> addVelocity(double x, double y, double z) {
        return addVelocity(new Vec3(x, y, z));
    }

    /**
     * @since 1.8.4
     */
    private ClientPlayerEntityHelper<T> setPos(Vec3 pos, boolean await) throws InterruptedException {
        boolean joinedMain = JsMacrosClient.clientCore.profile.checkJoinedThreadStack();
        if (joinedMain) {
            base.setPos(pos);
        } else {
            Semaphore wait = new Semaphore(await ? 0 : 1);
            mc.execute(() -> {
                base.setPos(pos);
                wait.release();
            });
            wait.acquire();
        }
        return this;
    }

    /**
     * Sets the player's position. Off the client thread, the change is queued and this
     * call returns without waiting for it to run.
     *
     * @param pos the position to move the player to
     * @return self for chaining.
     * @throws InterruptedException if the thread is interrupted
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> setPos(Pos3D pos) throws InterruptedException {
        return setPos(pos, false);
    }

    /**
     * Sets the player's position, optionally waiting for the change to run.
     * <p>
     * Off the client thread, the position change is queued and {@code await=true}
     * blocks until that task runs. On the client thread, the position is set immediately
     * regardless of {@code await}.
     *
     * @param pos   the position to move the player to
     * @param await whether to wait for the move to be applied
     * @return self for chaining.
     * @throws InterruptedException if the thread is interrupted while waiting for the
     * move to be applied
     * @since 1.9.0
     */
    public ClientPlayerEntityHelper<T> setPos(Pos3D pos, boolean await) throws InterruptedException {
        return setPos(new Vec3(pos.x, pos.y, pos.z), await);
    }

    /**
     * Sets the player's position to the given coordinates. Off the client thread, the
     * change is queued and this call returns without waiting for it to run.
     *
     * @param x the x coordinate to move the player to
     * @param y the y coordinate to move the player to
     * @param z the z coordinate to move the player to
     * @return self for chaining.
     * @throws InterruptedException if the thread is interrupted
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> setPos(double x, double y, double z) throws InterruptedException {
        return setPos(x, y, z, false);
    }

    /**
     * Sets the player's position to the given coordinates, optionally waiting for the
     * change to run.
     * <p>
     * When called from the client thread there is nothing to hand off and nothing to
     * wait for, so the {@code await} flag makes no difference there. Off the client
     * thread the move is queued onto it, and the flag decides whether this call blocks
     * until its task has run.
     *
     * @param x     the x coordinate to move the player to
     * @param y     the y coordinate to move the player to
     * @param z     the z coordinate to move the player to
     * @param await whether to wait for the move to be applied
     * @return self for chaining.
     * @throws InterruptedException if the thread is interrupted while waiting for the
     * move to be applied
     * @since 1.9.0
     */
    public ClientPlayerEntityHelper<T> setPos(double x, double y, double z, boolean await) throws InterruptedException {
        return setPos(new Vec3(x, y, z), await);
    }

    /**
     * Adds the offset to the player's position and queues the resulting position change
     * when called off the client thread. This overload does not wait for that change.
     *
     * @param pos the offset to move the player by
     * @return self for chaining.
     * @throws InterruptedException if the thread is interrupted
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> addPos(Pos3D pos) throws InterruptedException {
        return addPos(pos, false);
    }

    /**
     * moves the player by an offset rather than to a position, with a choice about
     * whether to wait for it.
     * <p>
     * When called from the client thread there is nothing to hand off and nothing to
     * wait for, so the {@code await} flag makes no difference there. Off the client
     * thread the move is queued onto it, and the flag decides whether this call blocks
     * until its own task has run.
     * <p>
     * The offset is applied to the position read when this method is called. Concurrent
     * queued calls can therefore compute from the same starting position; waiting for a
     * call to finish before the next ensures the next reads the updated position.
     *
     * @param pos   the offset to move the player by
     * @param await whether to wait for the move to be applied
     * @return self for chaining.
     * @throws InterruptedException if the thread is interrupted while waiting for the
     * move to be applied
     * @since 1.9.0
     */
    public ClientPlayerEntityHelper<T> addPos(Pos3D pos, boolean await) throws InterruptedException {
        return setPos(getPos().add(pos), await);
    }

    /**
     * Adds the given offset to the player's position. Off the client thread, the change
     * is queued and this call returns without waiting for it to run.
     *
     * @param x the offset on the x axis
     * @param y the offset on the y axis
     * @param z the offset on the z axis
     * @return self for chaining.
     * @throws InterruptedException if the thread is interrupted
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> addPos(double x, double y, double z) throws InterruptedException {
        return addPos(x, y, z, false);
    }

    /**
     * Adds the given offset to the player's position, optionally waiting for the change
     * to run.
     * <p>
     * When called from the client thread there is nothing to hand off and nothing to
     * wait for, so the {@code await} flag makes no difference there. Off the client
     * thread the move is queued onto it, and the flag decides whether this call blocks
     * until its task has run.
     *
     * @param x     the offset on the x axis
     * @param y     the offset on the y axis
     * @param z     the offset on the z axis
     * @param await whether to wait for the move to be applied
     * @return self for chaining.
     * @throws InterruptedException if the thread is interrupted while waiting for the
     * move to be applied
     * @since 1.9.0
     */
    public ClientPlayerEntityHelper<T> addPos(double x, double y, double z, boolean await) throws InterruptedException {
        return setPos(getPos().add(x, y, z), await);
    }

    /**
     * Sets the player rotation along the given axis and keeps the other axis the same.
     * <p>
     * Horizontal directions set yaw and preserve pitch; vertical directions set pitch
     * to straight up or down and preserve yaw. The name is case-insensitive. Other
     * names cause a {@link NullPointerException}.
     *
     * @param direction possible values are "up", "down", "north", "south", "east", "west"
     * @return self for chaining.
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> lookAt(String direction) {
        Direction dir = Direction.byName(direction.toLowerCase(Locale.ROOT));
        double yaw = getYaw();
        double pitch = getPitch();
        if (dir.getAxis().isHorizontal()) {
            yaw = dir.toYRot();
        } else {
            pitch = dir == Direction.UP ? -90 : 90;
        }
        return lookAt(yaw, pitch);
    }

    /**
     * Wraps the target angle to the equivalent value nearest the previous angle.
     *
     * @param lastAngle the previous rotation value
     * @param targetAngle the desired logical angle
     * @return an angle value safe to send this tick
     */
    private static float safeWrapDegrees(float lastAngle, float targetAngle) {
        float delta = Mth.wrapDegrees(targetAngle - lastAngle);
        return lastAngle + delta;
    }

    /**
     * Sets the player's yaw and pitch. Yaw is wrapped to an equivalent angle nearest
     * the current yaw; pitch is clamped to the range {@code -90} to {@code 90} degrees.
     * If mounted, the vehicle is notified of the passenger's turn.
     *
     * @param yaw   (was pitch prior to 1.2.6)
     * @param pitch (was yaw prior to 1.2.6)
     * @return self for chaining.
     * @since 1.0.3
     */
    public ClientPlayerEntityHelper<T> lookAt(double yaw, double pitch) {
        base.xRotO = base.getXRot();
        base.yRotO = base.getYRot();

        float safeYaw = safeWrapDegrees(base.yRotO, (float) yaw);
        float safePitch = Mth.clamp((float) pitch, -90.0F, 90.0F);

        base.setXRot(safePitch);
        base.setYRot(safeYaw);
        if (base.getVehicle() != null) {
            base.getVehicle().onPassengerTurned(base);
        }
        return this;
    }

    /**
     * Turns the player toward the given world coordinates, calculating the angles
     * from the player's eye position.
     *
     * @param x the x coordinate to look at
     * @param y the y coordinate to look at
     * @param z the z coordinate to look at
     * @return this helper for chaining.
     * @since 1.2.8
     */
    public ClientPlayerEntityHelper<T> lookAt(double x, double y, double z) {
        Vec3D vec = new Vec3D(base.getX(), base.getY() + base.getEyeHeight(base.getPose()), base.getZ(), x, y, z);
        lookAt(vec.getYaw(), vec.getPitch());
        return this;
    }

    /**
     * Tries to turn toward the block at the given coordinates.
     * @param x the x coordinate of the block to look at
     * @param y the y coordinate of the block to look at
     * @param z the z coordinate of the block to look at
     * @return {@code true} if the player is targeting the specified block, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean tryLookAt(int x, int y, int z) {
        return tryLookAt(new BlockPosHelper(x, y, z));
    }

    /**
     * Searches for a rotation whose block-outline ray trace hits the specified block.
     * On success, updates the player's rotation; on failure, leaves it unchanged.
     * An empty block shape returns {@code false} without searching.
     *
     * @param pos the position of the block to look at
     * @return {@code true} if the player is targeting the specified block, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean tryLookAt(BlockPosHelper pos) {
        BlockState state = Minecraft.getInstance().level.getBlockState(pos.getRaw());
        VoxelShape shape = state.getShape(Minecraft.getInstance().level, pos.getRaw());
        if (shape.isEmpty()) {
            return false;
        }
        Pos3D eyePos = getEyePos();
        double distance = base.getEyePosition().distanceTo(new Vec3(pos.getX(), pos.getY(), pos.getZ()));

        List<AABB> bounds = shape.toAabbs().stream().map(b -> b.move(pos.getRaw())).collect(Collectors.toList());
        // Scale offset with distance to the target. Closer targets should have more rays to find a possible angle
        double offset = Math.min(0.25, 0.01 * Math.max(distance, 0.5));
        for (AABB bound : bounds) {
            Vec3 center = bound.getCenter();
            double xDiff = (bound.maxX - bound.minX) / 2;
            double yDiff = (bound.maxY - bound.minY) / 2;
            double zDiff = (bound.maxZ - bound.minZ) / 2;
            // Round the offsets down so they perfectly fit the bounds
            double xOffset = xDiff / Math.ceil(xDiff / offset);
            double yOffset = yDiff / Math.ceil(yDiff / offset);
            double zOffset = zDiff / Math.ceil(zDiff / offset);
            // Iterate alternating around the center to iterate outwards which will give more pleasing results
            for (int yc = 0; yc < (int) (yDiff / yOffset) * 2 + 2; yc++) {
                for (int xc = 0; xc < (int) (xDiff / xOffset) * 2 + 2; xc++) {
                    for (int zc = 0; zc < (int) (zDiff / zOffset) * 2 + 2; zc++) {
                        // Don't remove the integer division, because we want to round down the value
                        // The 0.999 helps with edge cases, literally
                        double x = center.x + ((xc & 1) == 0 ? 1 : -1) * xOffset * (xc / 2) * 0.999;
                        double y = center.y + ((yc & 1) == 0 ? 1 : -1) * yOffset * (yc / 2) * 0.999;
                        double z = center.z + ((zc & 1) == 0 ? 1 : -1) * zOffset * (zc / 2) * 0.999;
                        BlockHitResult result = base.level().clip(new ClipContext(base.getEyePosition(), new Vec3(x, y, z), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, base));
                        if (result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos.getRaw())) {
                            Vec3D vec = new Vec3D(eyePos, new Pos3D(x, y, z));
                            lookAt(vec.getYaw(), vec.getPitch());
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Turns the player 90 degrees left while preserving pitch.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> turnLeft() {
        return lookAt(getYaw() - 90, getPitch());
    }

    /**
     * Turns the player 90 degrees right while preserving pitch.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> turnRight() {
        return lookAt(getYaw() + 90, getPitch());
    }

    /**
     * Turns the player 180 degrees while preserving pitch.
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> turnBack() {
        return lookAt(getYaw() + 180, getPitch());
    }

    /**
     * Attacks the entity and swings the main hand.
     * @param entity entity to attack
     * @since 1.5.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletIgnore
    public ClientPlayerEntityHelper<T> attack(EntityHelper<?> entity) throws InterruptedException {
        return attack(entity, false);
    }

    /**
     * Attacks the entity and swings the main hand. Off-thread calls are scheduled on
     * the client thread; {@code await} controls whether this call waits for that action.
     * @param entity entity to attack; attacking the player itself throws an assertion error
     * @param await whether to wait for the scheduled action when called off-thread
     * @since 1.6.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletIgnore
    public ClientPlayerEntityHelper<T> attack(EntityHelper<?> entity, boolean await) throws InterruptedException {
        boolean joinedMain = JsMacrosClient.clientCore.profile.checkJoinedThreadStack();
        assert mc.gameMode != null;
        if (entity.getRaw() == mc.player) {
            throw new AssertionError("Can't interact with self!");
        }
        if (joinedMain) {
            mc.gameMode.attack(mc.player, entity.getRaw());
            assert mc.player != null;
            mc.player.swing(InteractionHand.MAIN_HAND);
        } else {
            Semaphore wait = new Semaphore(await ? 0 : 1);
            mc.execute(() -> {
                mc.gameMode.attack(mc.player, entity.getRaw());
                assert mc.player != null;
                mc.player.swing(InteractionHand.MAIN_HAND);
                wait.release();
            });
            wait.acquire();
        }
        return this;
    }

    /**
     * Starts destroying the block at the given coordinates and swings the main hand.
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @param direction face name: {@code up}, {@code down}, {@code north}, {@code south}, {@code east}, or {@code west}
     * @return this helper for chaining
     * @since 1.8.4
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletIgnore
    @DocletReplaceParams("x: int, y: int, z: int, direction: Direction")
    public ClientPlayerEntityHelper<T> attack(int x, int y, int z, String direction) throws InterruptedException {
        return attack(x, y, z, direction, false);
    }

    /**
     * Starts destroying the block at the given coordinates and swings the main hand.
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @param direction face index in {@code DOWN, UP, NORTH, SOUTH, WEST, EAST} order; indices outside {@code 0-5} wrap
     * @since 1.5.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletIgnore
    @DocletReplaceParams("x: int, y: int, z: int, direction: Hexit")
    public ClientPlayerEntityHelper<T> attack(int x, int y, int z, int direction) throws InterruptedException {
        return attack(x, y, z, direction, false);
    }

    /**
     * Starts destroying the block at the given coordinates and swings the main hand.
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @param direction face name: {@code up}, {@code down}, {@code north}, {@code south}, {@code east}, or {@code west}
     * @param await whether to wait for the scheduled client-thread action when called off-thread
     * @return this helper
     * @since 1.8.4
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletIgnore
    @DocletReplaceParams("x: int, y: int, z: int, direction: Direction, await: boolean")
    public ClientPlayerEntityHelper<T> attack(int x, int y, int z, String direction, boolean await) throws InterruptedException {
        return attack(x, y, z, Direction.byName(direction.toLowerCase(Locale.ROOT)), await);
    }

    /**
     * Starts destroying the block at the given coordinates and swings the main hand.
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @param direction face index in {@code DOWN, UP, NORTH, SOUTH, WEST, EAST} order; indices outside {@code 0-5} wrap
     * @param await whether to wait for the scheduled client-thread action when called off-thread
     * @since 1.6.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletIgnore
    @DocletReplaceParams("x: int, y: int, z: int, direction: Hexit, await: boolean")
    public ClientPlayerEntityHelper<T> attack(int x, int y, int z, int direction, boolean await) throws InterruptedException {
        return attack(x, y, z, Direction.from3DDataValue(direction), await);
    }

    private ClientPlayerEntityHelper<T> attack(int x, int y, int z, Direction direction, boolean await) throws InterruptedException {
        assert mc.gameMode != null;
        boolean joinedMain = JsMacrosClient.clientCore.profile.checkJoinedThreadStack();
        if (joinedMain) {
            mc.gameMode.startDestroyBlock(new BlockPos(x, y, z), direction);
            assert mc.player != null;
            mc.player.swing(InteractionHand.MAIN_HAND);
        } else {
            Semaphore wait = new Semaphore(await ? 0 : 1);
            mc.execute(() -> {
                mc.gameMode.startDestroyBlock(new BlockPos(x, y, z), direction);
                assert mc.player != null;
                mc.player.swing(InteractionHand.MAIN_HAND);
                wait.release();
            });
            wait.acquire();
        }
        return this;
    }

    /**
     * Interacts with an entity using the selected hand.
     * @param entity entity to interact with; interacting with the player itself throws an assertion error
     * @param offHand whether to use the off hand instead of the main hand
     * @since 1.5.0, renamed from {@code interact} in 1.6.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletIgnore
    public ClientPlayerEntityHelper<T> interactEntity(EntityHelper<?> entity, boolean offHand) throws InterruptedException {
        return interactEntity(entity, offHand, false);
    }

    /**
     * Interacts with an entity using the selected hand and swings it when the action consumes input.
     * @param entity entity to interact with; interacting with the player itself throws an assertion error
     * @param offHand whether to use the off hand instead of the main hand
     * @param await whether to wait for the scheduled client-thread action when called off-thread
     * @since 1.6.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletIgnore
    public ClientPlayerEntityHelper<T> interactEntity(EntityHelper<?> entity, boolean offHand, boolean await) throws InterruptedException {
        assert mc.gameMode != null;
        if (entity.getRaw() == mc.player) {
            throw new AssertionError("Can't interact with self!");
        }
        InteractionHand hand = offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        boolean joinedMain = JsMacrosClient.clientCore.profile.checkJoinedThreadStack();
        if (joinedMain) {
            InteractionResult result = InteractionCompat.interact(mc.gameMode, mc.player, entity.getRaw(), hand);
            assert mc.player != null;
            if (result.consumesAction()) {
                mc.player.swing(hand);
            }
        } else {
            Semaphore wait = new Semaphore(await ? 0 : 1);
            mc.execute(() -> {
                InteractionResult result = InteractionCompat.interact(mc.gameMode, mc.player, entity.getRaw(), hand);
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
     * Uses an item from the selected hand and swings it when the action consumes input.
     * @param offHand whether to use the off hand instead of the main hand
     * @since 1.5.0, renamed from {@code interact} in 1.6.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletIgnore
    public ClientPlayerEntityHelper<T> interactItem(boolean offHand) throws InterruptedException {
        return interactItem(offHand, false);
    }

    /**
     * Uses an item from the selected hand and swings it when the action consumes input.
     * @param offHand whether to use the off hand instead of the main hand
     * @param await whether to wait for the scheduled client-thread action when called off-thread
     * @since 1.6.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletIgnore
    public ClientPlayerEntityHelper<T> interactItem(boolean offHand, boolean await) throws InterruptedException {
        assert mc.gameMode != null;
        InteractionHand hand = offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        boolean joinedMain = JsMacrosClient.clientCore.profile.checkJoinedThreadStack();
        if (joinedMain) {
            InteractionResult result = mc.gameMode.useItem(mc.player, hand);
            assert mc.player != null;
            if (result.consumesAction()) {
                mc.player.swing(hand);
            }
        } else {
            Semaphore wait = new Semaphore(await ? 0 : 1);
            mc.execute(() -> {
                InteractionResult result = mc.gameMode.useItem(mc.player, hand);
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
     * Interacts with the block at the given coordinates using the selected face and hand.
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @param direction face name: {@code up}, {@code down}, {@code north}, {@code south}, {@code east}, or {@code west}
     * @return this helper
     * @since 1.8.4
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletIgnore
    @DocletReplaceParams("x: int, y: int, z: int, direction: Direction, offHand: boolean")
    public ClientPlayerEntityHelper<T> interactBlock(int x, int y, int z, String direction, boolean offHand) throws InterruptedException {
        return interactBlock(x, y, z, direction, offHand, false);
    }

    /**
     * Interacts with the block at the given coordinates using the selected face and hand.
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @param direction face index in {@code DOWN, UP, NORTH, SOUTH, WEST, EAST} order; indices outside {@code 0-5} wrap
     * @param offHand whether to use the off hand instead of the main hand
     * @since 1.5.0, renamed from {@code interact} in 1.6.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletIgnore
    @DocletReplaceParams("x: int, y: int, z: int, direction: Hexit, offHand: boolean")
    public ClientPlayerEntityHelper<T> interactBlock(int x, int y, int z, int direction, boolean offHand) throws InterruptedException {
        return interactBlock(x, y, z, direction, offHand, false);
    }

    /**
     * Interacts with the block at the given coordinates using the selected face and hand.
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @param direction face name: {@code up}, {@code down}, {@code north}, {@code south}, {@code east}, or {@code west}
     * @param await whether to wait for the scheduled client-thread action when called off-thread
     * @return this helper
     * @since 1.8.4
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletIgnore
    @DocletReplaceParams("x: int, y: int, z: int, direction: Direction, offHand: boolean, await: boolean")
    public ClientPlayerEntityHelper<T> interactBlock(int x, int y, int z, String direction, boolean offHand, boolean await) throws InterruptedException {
        return interactBlock(x, y, z, Direction.byName(direction.toLowerCase(Locale.ROOT)), offHand, await);
    }

    /**
     * Interacts with the block at the given coordinates using the selected face and hand.
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @param direction face index in {@code DOWN, UP, NORTH, SOUTH, WEST, EAST} order; indices outside {@code 0-5} wrap
     * @param offHand whether to use the off hand instead of the main hand
     * @param await whether to wait for the scheduled client-thread action when called off-thread
     * @since 1.5.0, renamed from {@code interact} in 1.6.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletIgnore
    @DocletReplaceParams("x: int, y: int, z: int, direction: Hexit, offHand: boolean, await: boolean")
    public ClientPlayerEntityHelper<T> interactBlock(int x, int y, int z, int direction, boolean offHand, boolean await) throws InterruptedException {
        return interactBlock(x, y, z, Direction.from3DDataValue(direction), offHand, await);
    }

    /** Attempts block interaction using the active client player and interaction manager. Off the client thread, schedules the action; {@code await} waits for that action to run, not for server confirmation or success. On the client thread or a joined thread, it runs inline regardless of {@code await}. Interrupting the wait propagates {@code InterruptedException} without cancelling the queued action, even when {@code await} is false. The supplied coordinates are both the block coordinates and hit-vector coordinates, without a center offset. Swings the selected hand when the interaction result consumes an action.
     * @param x block x coordinate and hit-vector x coordinate
     * @param y block y coordinate and hit-vector y coordinate
     * @param z block z coordinate and hit-vector z coordinate
     * @param direction the block face to interact with
     * @param offHand whether to use the off hand instead of the main hand
     * @param await whether to wait for the scheduled action when not running on the client or a joined thread
     * @return self for chaining
     */
    public ClientPlayerEntityHelper<T> interactBlock(int x, int y, int z, Direction direction, boolean offHand, boolean await) throws InterruptedException {
        assert mc.gameMode != null;
        InteractionHand hand = offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        boolean joinedMain = JsMacrosClient.clientCore.profile.checkJoinedThreadStack();
        if (joinedMain) {
            InteractionResult result = mc.gameMode.useItemOn(mc.player, hand,
                    new BlockHitResult(new Vec3(x, y, z), direction, new BlockPos(x, y, z), false));
            assert mc.player != null;
            if (result.consumesAction()) {
                mc.player.swing(hand);
            }
        } else {
            Semaphore wait = new Semaphore(await ? 0 : 1);
            mc.execute(() -> {
                InteractionResult result = mc.gameMode.useItemOn(mc.player, hand,
                        new BlockHitResult(new Vec3(x, y, z), direction, new BlockPos(x, y, z), false));
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
     * Starts using the currently selected item.
     * @since 1.5.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletIgnore
    public ClientPlayerEntityHelper<T> interact() throws InterruptedException {
        return interact(false);
    }

    /**
     * Starts using the currently selected item. Off the main thread, {@code await} controls whether this call waits for the scheduled input task to run.
     * @param await whether to wait for the scheduled task when called off the main thread
     * @since 1.6.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletIgnore
    public ClientPlayerEntityHelper<T> interact(boolean await) throws InterruptedException {
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
     * Starts the attack action.
     * @since 1.5.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletIgnore
    public ClientPlayerEntityHelper<T> attack() throws InterruptedException {
        return attack(false);
    }

    /**
     * Starts the attack action. Off the main thread, {@code await} controls whether this call waits for the scheduled input task to run.
     * @param await whether to wait for the scheduled task when called off the main thread
     * @since 1.6.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletIgnore
    public ClientPlayerEntityHelper<T> attack(boolean await) throws InterruptedException {
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
     * Sets or clears the attack key's pressed state.
     * @param stop whether to clear the pressed state; otherwise, clicks the attack key
     * @return this helper for chaining
     * @since 1.6.3
     * @deprecated use {@code Player.getInteractionManager().breakBlock()} instead
     */
    @Deprecated
    @DocletIgnore
    public ClientPlayerEntityHelper<T> setLongAttack(boolean stop) {
        if (!stop) {
            KeyMapping.click(InputConstants.getKey(mc.options.keyAttack.saveString()));
        } else {
            KeyMapping.set(InputConstants.getKey(mc.options.keyAttack.saveString()), false);
        }
        return this;
    }

    /**
     * Sets or clears the use key's pressed state.
     * @param stop whether to clear the pressed state; otherwise, clicks the use key
     * @return this helper for chaining
     * @since 1.6.3
     * @deprecated use {@code Player.getInteractionManager().holdInteract()} instead
     */
    @Deprecated
    @DocletIgnore
    public ClientPlayerEntityHelper<T> setLongInteract(boolean stop) {
        if (!stop) {
            KeyMapping.click(InputConstants.getKey(mc.options.keyUse.saveString()));
        } else {
            KeyMapping.set(InputConstants.getKey(mc.options.keyUse.saveString()), false);
        }
        return this;
    }

    /**
     * Returns the active item cooldowns and their remaining duration in ticks. Items without an active cooldown are absent; the returned map is a new snapshot on each call.
     *
     * @return a map from item id to the ticks left on its cooldown
     * @since 1.6.5
     */
    @DocletReplaceReturn("JavaMap<ItemId, int>")
    public Map<String, Integer> getItemCooldownsRemainingTicks() {
        int tick = ((IItemCooldownManager) base.getCooldowns()).jsmacros_getManagerTicks();
        Map<Item, IItemCooldownEntry> map = ((IItemCooldownManager) base.getCooldowns()).jsmacros_getCooldownItems();
        return map.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getName(e.getKey().getDefaultInstance()).getString(), e -> e.getValue().jsmacros_getEndTick() - tick));
    }

    /**
     * Returns the named item's remaining cooldown in ticks, or {@code -1} if no cooldown entry exists. The item id may omit the namespace, which defaults to {@code minecraft}.
     *
     * @param item the id of the item to look up
     * @return the ticks left on the cooldown, or {@code -1} if the item is not on one
     * @since 1.6.5
     */
    @DocletReplaceParams("item: CanOmitNamespace<ItemId>")
    public int getItemCooldownRemainingTicks(String item) {
        int tick = ((IItemCooldownManager) base.getCooldowns()).jsmacros_getManagerTicks();
        Map<Item, IItemCooldownEntry> map = ((IItemCooldownManager) base.getCooldowns()).jsmacros_getCooldownItems();
        IItemCooldownEntry entry = map.get(BuiltInRegistries.ITEM.getValue(RegistryHelper.parseIdentifier(item)));
        if (entry == null) {
            return -1;
        }
        return entry.jsmacros_getEndTick() - tick;
    }

    /**
     * Returns active item cooldowns mapped to start tick minus current manager tick. Values start at zero and become more negative as the cooldown runs; items without an active cooldown are absent. The returned map is a new snapshot on each call.
     *
     * @return a map from item id to start tick minus current manager tick
     * @since 1.6.5
     */
    @DocletReplaceReturn("JavaMap<ItemId, int>")
    public Map<String, Integer> getTicksSinceCooldownsStart() {
        int tick = ((IItemCooldownManager) base.getCooldowns()).jsmacros_getManagerTicks();
        Map<Item, IItemCooldownEntry> map = ((IItemCooldownManager) base.getCooldowns()).jsmacros_getCooldownItems();
        return map.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getName(e.getKey().getDefaultInstance()).getString(), e -> e.getValue().jsmacros_getStartTick() - tick));
    }

    /**
     * Returns the named item's start tick minus the current manager tick. The value starts at zero and becomes more negative as the cooldown runs; {@code -1} also indicates no cooldown entry, so it is ambiguous. The item id may omit the namespace, which defaults to {@code minecraft}.
     *
     * @param item the id of the item to look up
     * @return the start tick minus current manager tick, or {@code -1} if inactive
     * @since 1.6.5
     */
    @DocletReplaceParams("item: CanOmitNamespace<ItemId>")
    public int getTicksSinceCooldownStart(String item) {
        int tick = ((IItemCooldownManager) base.getCooldowns()).jsmacros_getManagerTicks();
        Map<Item, IItemCooldownEntry> map = ((IItemCooldownManager) base.getCooldowns()).jsmacros_getCooldownItems();
        IItemCooldownEntry entry = map.get(BuiltInRegistries.ITEM.getValue(RegistryHelper.parseIdentifier(item)));
        if (entry == null) {
            return -1;
        }
        return entry.jsmacros_getStartTick() - tick;
    }

    /**
     * Returns the player's food level in hunger points, from {@code 0} to {@code 20}.
     *
     * @return the player's food level, from zero to twenty
     * @since 1.1.2
     */
    public int getFoodLevel() {
        return base.getFoodData().getFoodLevel();
    }

    /**
     * Returns the player's saturation level, a floating-point food buffer consumed before food level.
     *
     * @return the saturation level.
     * @since 1.8.4
     */
    public float getSaturation() {
        return base.getFoodData().getSaturationLevel();
    }

    /**
     * Drops the held item, either one item or the whole stack according to {@code dropStack}.
     *
     * @param dropStack whether to drop the whole stack rather than one of it
     * @return this helper for chaining
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<?> dropHeldItem(boolean dropStack) {
        base.drop(dropStack);
        return this;
    }

    /**
     * Returns a helper for the advancement tree received by this client's connection.
     *
     * @return an advancement manager to work with advancements.
     * @since 1.8.4
     */
    public AdvancementManagerHelper getAdvancementManager() {
        return new AdvancementManagerHelper(base.connection.getAdvancements().getTree());
    }

    /**
     * Estimates mining duration using the player's main-hand item and current player state.
     * The result is in ticks; {@code 0} means instant mining and {@code -1} means the item
     * cannot mine the block.
     *
     * @param block the block to mine
     * @return the time in ticks that it will approximately take the player with the currently held
     * item to mine said block.
     */
    public int calculateMiningSpeed(BlockStateHelper block) {
        return calculateMiningSpeed(getMainHand(), block);
    }

    /**
     * Estimates mining duration in ticks with {@code usedItem} and the current player's state.
     * Use an air item for the bare hand. {@code 0} means instant mining; {@code -1} means the
     * item cannot mine the block. Player effects, game mode, and position affect the estimate.
     *
     * @param usedItem   the item to mine with
     * @param blockState the block to mine
     * @return the time in ticks that it will approximately take the player with the specified item
     * to mine said block.
     * @since 1.8.4
     */
    public int calculateMiningSpeed(ItemStackHelper usedItem, BlockStateHelper blockState) {
        var enchRegistry = mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Player player = mc.player;
        BlockState state = blockState.getRaw();
        ItemStack item = usedItem.getRaw();

        if (!item.canDestroyBlock(state, mc.level, player.blockPosition(), player)) {
            return -1;
        } else if (player.isCreative()) {
            return 0;
        }
        float hardness = state.getDestroySpeed(mc.level, null);
        if (hardness == -1) {
            return -1;
        }
        float speedMultiplier = item.getDestroySpeed(state);
        if (speedMultiplier > 1.0F) {
            int efficiency = enchRegistry.get(Enchantments.EFFICIENCY).map(e -> EnchantmentHelper.getItemEnchantmentLevel(e, item)).orElse(0);
            if (efficiency > 0 && !item.isEmpty()) {
                speedMultiplier += (efficiency * efficiency + 1F);
            }
        }
        if (MobEffectUtil.hasDigSpeed(player)) {
            speedMultiplier *= 1.0F + (MobEffectUtil.getDigSpeedAmplification(player) + 1F) * 0.2F;
        }
        if (player.hasEffect(MobEffects.MINING_FATIGUE)) {
            switch (player.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
                case 0:
                    speedMultiplier *= 0.3;
                    break;
                case 1:
                    speedMultiplier *= 0.09;
                    break;
                case 2:
                    speedMultiplier *= 0.0027;
                    break;
                default:
                    speedMultiplier *= 0.00081;
                    break;
            }
        }
        if (player.isEyeInFluid(FluidTags.WATER) && enchRegistry.get(Enchantments.AQUA_AFFINITY).map(e -> EnchantmentHelper.getItemEnchantmentLevel(e, item)).orElse(0) == 0) {
            speedMultiplier /= 5;
        }
        if (!player.onGround()) {
            speedMultiplier /= 5;
        }
        float damage = speedMultiplier / hardness;
        damage /= (!state.requiresCorrectToolForDrops() || item.isCorrectToolForDrops(state)) ? 30 : 100;
        if (damage >= 1) {
            return 0;
        }
        return (int) Math.ceil(1 / damage);
    }

}
