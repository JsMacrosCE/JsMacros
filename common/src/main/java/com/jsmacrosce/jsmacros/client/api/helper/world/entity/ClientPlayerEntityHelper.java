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
 * the local player, which is the one the client is controlling. It is the only player a
 * client has any business with, and it is the one a script means by "the player".
 * <p>
 * What it adds over an ordinary player is everything that goes through the client rather
 * than through the server: turning, walking, the ability to be moved, and the mining
 * speed figure the client works out for itself. These reach the server as ordinary
 * player actions, so they are how a script drives the player around.
 * <p>
 * The movement setters come in pairs. The form without a flag hands the work to the
 * client thread and hands the control back as soon as it is queued; the form with a
 * {@code true} waits until it has run. Waiting also waits out whatever else was queued
 * ahead of it. Off the client thread that is the whole difference, because on the client
 * thread there is nothing to hand off and nothing to wait for.
 * example:
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
 *
 * @author Wagyourtail
 * @since 1.0.3
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class ClientPlayerEntityHelper<T extends LocalPlayer> extends PlayerEntityHelper<T> {
    protected final Minecraft mc = Minecraft.getInstance();

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
     * sets how fast the player is moving, as a position rather than as a direction,
     * which means the numbers are the movement per tick rather than a heading. A number
     * of one is a block a tick, which is the game's own walking pace.
     * <p>
     * This replaces the movement rather than adding to it, so it is the way to stop a
     * player dead: setting all three to zero leaves the player where it is with nothing
     * carrying it. The player's own friction and drag are applied on top of this over
     * the following ticks, so the movement does not stay at exactly the figure given.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // straight up at half a block a tick
     *   player.setVelocity(0, 0.5, 0);
     * }
     * </pre>
     *
     * @param velocity the movement to give the player, per tick
     * @return self for chaining.
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> setVelocity(Pos3D velocity) {
        return setVelocity(new Vec3(velocity.x, velocity.y, velocity.z));
    }

    /**
     * sets how fast the player is moving, as three numbers rather than as a position.
     * This is the form to use when the movement is worked out rather than held, since it
     * saves building a position for it.
     * <p>
     * All three at zero stops the player where it is with nothing carrying it.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // nothing carrying it at all
     *   player.setVelocity(0, 0, 0);
     *   Chat.log(`stopped at ${player.getPos()}`);
     * }
     * </pre>
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
     * adds to how fast the player is moving rather than replacing it, so it is the way
     * to give a player a shove on top of what it is already doing. The numbers are the
     * movement per tick, as they are on the setter this is the counterpart to.
     * <p>
     * The result is still passed through the player's own friction over the following
     * ticks, so a shove is a change of momentum rather than a speed that sticks.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // a shove straight up, on top of whatever the player is already doing
     *   player.addVelocity(0, 1, 0);
     * }
     * </pre>
     *
     * @param velocity the movement to add, per tick
     * @return self for chaining.
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> addVelocity(Pos3D velocity) {
        return addVelocity(new Vec3(velocity.x, velocity.y, velocity.z));
    }

    /**
     * adds to how fast the player is moving rather than replacing it, as three numbers
     * rather than as a position. This is the form to use when the shove is worked out
     * rather than held.
     * <p>
     * One straight up is a jump, and a shove towards a target is how a script sends a
     * player towards something without turning them first.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // a shove along whichever way the player is already facing
     *   const facing = player.getFacingDirection();
     *   if (facing.getAxis() === "x") {
     *     player.addVelocity(0.2, 0, 0);
     *   } else {
     *     player.addVelocity(0, 0, 0.2);
     *   }
     * }
     * </pre>
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
     * moves the player to a position, handing the control back as soon as the move is
     * queued rather than waiting for it. This is the form to use from a script running
     * off the client thread, where blocking the calling thread on a game action is
     * rarely what a script wants.
     * <p>
     * The move is put on the client's own thread, so it happens on the client's terms
     * rather than the script's: the player is not there until the game next runs, and
     * the server is told about it in the ordinary way. There is a form that waits if a
     * script needs the player to have arrived.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // queued rather than waited for
     *   player.setPos(player.getPos().add(0, 5, 0));
     *   Chat.log("the move has been queued");
     * }
     * </pre>
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
     * moves the player to a position, with a choice about whether to wait for it.
     * <p>
     * When called from the client thread there is nothing to hand off and nothing to
     * wait for, so the {@code await} flag makes no difference there. Off the client
     * thread the move is queued onto it, and the flag decides whether this call blocks
     * until its own task has run. Blocking also waits out whatever else was queued ahead
     * of it, which is usually harmless and occasionally is a long wait.
     * <p>
     * The move is the player's position being set rather than the player being walked
     * there, so nothing has to be able to get in the way and no path is looked for.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // and wait for the player to actually be there
     *   player.setPos(player.getPos().add(0, 5, 0), true);
     *   Chat.log(`arrived at ${player.getPos()}`);
     * }
     * </pre>
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
     * moves the player to three coordinates, handing the control back as soon as the
     * move is queued. This is the form to use when the target is worked out rather than
     * held, since it saves building a position for it.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // straight up five blocks, queued rather than waited for
     *   const pos = player.getPos();
     *   player.setPos(pos.x, pos.y + 5, pos.z);
     * }
     * </pre>
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
     * moves the player to three coordinates, with a choice about whether to wait for it.
     * <p>
     * When called from the client thread there is nothing to hand off and nothing to
     * wait for, so the {@code await} flag makes no difference there. Off the client
     * thread the move is queued onto it, and the flag decides whether this call blocks
     * until its own task has run.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const pos = player.getPos();
     *   // and wait for the player to actually be there
     *   player.setPos(pos.x, pos.y + 5, pos.z, true);
     *   Chat.log(`arrived at ${player.getPos()}`);
     * }
     * </pre>
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
     * moves the player by an offset rather than to a position, handing the control back
     * as soon as the move is queued. The offset is added to the player's position as it
     * stands at the moment the move runs, not as it stood when this was called, so a
     * script that has queued several of these has them all read from wherever the
     * player has got to.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // up one block, from wherever the player has got to
     *   player.addPos(0, 1, 0);
     *   Chat.log("the step has been queued");
     * }
     * </pre>
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
     * The offset is read from the player's position at the moment the move runs, so
     * waiting is what makes a chain of these add up the way a script would expect.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // three steps up, each waiting, so the third is a full three blocks higher
     *   for (let i = 0; i !== 3; i += 1) {
     *     player.addPos(0, 1, 0, true);
     *   }
     *   Chat.log(`three blocks up, at ${player.getPos()}`);
     * }
     * </pre>
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
     * moves the player by an offset rather than to a position, as three numbers rather
     * than as a position, handing the control back as soon as the move is queued.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // queued rather than waited for
     *   player.addPos(0, 1, 0);
     *   Chat.log("the step has been queued");
     * }
     * </pre>
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
     * moves the player by an offset rather than to a position, as three numbers rather
     * than as a position, with a choice about whether to wait for it.
     * <p>
     * When called from the client thread there is nothing to hand off and nothing to
     * wait for, so the {@code await} flag makes no difference there. Off the client
     * thread the move is queued onto it, and the flag decides whether this call blocks
     * until its own task has run.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // and wait for the player to actually be there
     *   player.addPos(0, 1, 0, true);
     *   Chat.log(`now at ${player.getPos()}`);
     * }
     * </pre>
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
     * A horizontal direction turns the player to a compass point and leaves the pitch
     * alone; a vertical one sets the pitch to straight up or straight down and leaves
     * the yaw alone. The name is read in lower case, so any casing works, and a name
     * that is none of the six gives a direction of {@code null} rather than an error
     * from here, which then fails on the axis being asked for.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // face due north, with the pitch left as it was
     *   player.lookAt("north");
     *   Chat.log(`yaw ${player.getYaw()}, pitch ${player.getPitch()}`);
     *   // and now straight up, with the yaw left as it was
     *   player.lookAt("up");
     * }
     * </pre>
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
     * Wraps {@code targetAngle} into an equivalent angle that is
     * closest to {@code lastAngle}, preventing large packet deltas.
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
     * turns the player to a yaw and a pitch, which is the pair of angles rather than a
     * point to look at. The two are not the same as {@link #getYaw()} and
     * {@link #getPitch()}: the yaw is put through the game's own wrapping first, so
     * turning a long way round is a small change rather than a large one, and the pitch
     * is clamped to the range from straight up to straight down rather than refused.
     * <p>
     * This is the form every other turning call goes through, so it is the one to reach
     * for when a script has two angles rather than a point. Nothing here waits for the
     * turn, and the change goes to the server as an ordinary rotation.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // due north, level
     *   player.lookAt(180, 0);
     *   Chat.log(`yaw ${player.getYaw()}, pitch ${player.getPitch()}`);
     *   // and straight down, which the pitch clamp allows
     *   player.lookAt(180, 90);
     * }
     * </pre>
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
     * look at the specified coordinates, which is the form to use when a script has a
     * point rather than a pair of angles. The angles are worked out from the player's
     * own eyes rather than from its feet, so what it looks at is the point given rather
     * than a point at the height of its feet.
     * <p>
     * Nothing here waits for the turn, so the turn and whatever a script does next
     * happen in the same tick rather than one after the other.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // straight ahead, which is where the player is already looking
     *   const ahead = player.getEyePos();
     *   player.lookAt(ahead.x * 2, ahead.y, ahead.z * 2);
     *   Chat.log(`now facing ${player.getFacingDirection().getName()}`);
     * }
     * </pre>
     *
     * @param x the x coordinate to look at
     * @param y the y coordinate to look at
     * @param z the z coordinate to look at
     * @return self for chaining.
     * @since 1.2.8
     */
    public ClientPlayerEntityHelper<T> lookAt(double x, double y, double z) {
        Vec3D vec = new Vec3D(base.getX(), base.getY() + base.getEyeHeight(base.getPose()), base.getZ(), x, y, z);
        lookAt(vec.getYaw(), vec.getPitch());
        return this;
    }

    /**
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
     * Will try many rotations to find one that will make the player target the specified block. If
     * successful, the player will be turned towards the block and {@code true} will be returned. If
     * {@code false} is returned, the player will keep its current rotation.
     * <p>
     * This is a search rather than a single turn. It walks the shape of the block
     * outwards from its centre in a grid of points, aiming at each in turn, and the
     * first point the game's own block trace says is on that block is the one it stops
     * on. The grid gets finer the closer the block is, up to a quarter of a block
     * apart, and a block with a shape that fills more than one box is searched box by
     * box, so a stair or a fence can be aimed at rather than only at its centre.
     * <p>
     * A block with nothing in its shape gives {@code false} straight away rather than
     * searching, which is what air does.
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
     * turns the player a quarter turn to the left, keeping the pitch where it was. This
     * is the same as taking ninety degrees <b>off</b> the yaw rather than putting them
     * on, which is how a left turn works out in the game's convention, and because the
     * yaw is put through the game's own wrapping, a player already near the end of the
     * range comes out the other side rather than at a strange angle.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   player.turnLeft();
     *   Chat.log(`now facing ${player.getFacingDirection().getName()}`);
     *   // three of these come to the same place as one turnRight, not as a half turn
     *   player.turnLeft().turnLeft().turnLeft();
     *   Chat.log(`and now ${player.getFacingDirection().getName()}`);
     * }
     * </pre>
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> turnLeft() {
        return lookAt(getYaw() - 90, getPitch());
    }

    /**
     * turns the player a quarter turn to the right, keeping the pitch where it was. This
     * is the same as adding ninety degrees to the yaw, where {@link #turnLeft()} takes
     * them off, and it wraps the same way that one does.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   player.turnRight();
     *   Chat.log(`now facing ${player.getFacingDirection().getName()}`);
     * }
     * </pre>
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> turnRight() {
        return lookAt(getYaw() + 90, getPitch());
    }

    /**
     * turns the player a half turn, so it faces where it came from, keeping the pitch
     * where it was. It is the same as {@link #turnLeft()} twice, and the yaw is put
     * through the game's own wrapping on the way.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const before = player.getFacingDirection().getName();
     *   player.turnBack();
     *   Chat.log(`${before}, then ${player.getFacingDirection().getName()}`);
     * }
     * </pre>
     *
     * @return self for chaining.
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> turnBack() {
        return lookAt(getYaw() + 180, getPitch());
    }

    /**
     * @param entity
     * @since 1.5.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    public ClientPlayerEntityHelper<T> attack(EntityHelper<?> entity) throws InterruptedException {
        return attack(entity, false);
    }

    /**
     * @param await
     * @param entity
     * @since 1.6.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
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
     * @param x         the x coordinate to attack
     * @param y         the y coordinate to attack
     * @param z         the z coordinate to attack
     * @param direction possible values are "up", "down", "north", "south", "east", "west"
     * @return self for chaining.
     * @since 1.8.4
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletReplaceParams("x: int, y: int, z: int, direction: Direction")
    public ClientPlayerEntityHelper<T> attack(int x, int y, int z, String direction) throws InterruptedException {
        return attack(x, y, z, direction, false);
    }

    /**
     * @param x
     * @param y
     * @param z
     * @param direction 0-5 in order: [DOWN, UP, NORTH, SOUTH, WEST, EAST];
     * @since 1.5.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletReplaceParams("x: int, y: int, z: int, direction: Hexit")
    public ClientPlayerEntityHelper<T> attack(int x, int y, int z, int direction) throws InterruptedException {
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
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletReplaceParams("x: int, y: int, z: int, direction: Direction, await: boolean")
    public ClientPlayerEntityHelper<T> attack(int x, int y, int z, String direction, boolean await) throws InterruptedException {
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
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
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
     * @param entity
     * @param offHand
     * @since 1.5.0, renamed from {@code interact} in 1.6.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    public ClientPlayerEntityHelper<T> interactEntity(EntityHelper<?> entity, boolean offHand) throws InterruptedException {
        return interactEntity(entity, offHand, false);
    }

    /**
     * @param entity
     * @param offHand
     * @param await
     * @throws InterruptedException
     * @since 1.6.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
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
     * @param offHand
     * @since 1.5.0, renamed from {@code interact} in 1.6.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    public ClientPlayerEntityHelper<T> interactItem(boolean offHand) throws InterruptedException {
        return interactItem(offHand, false);
    }

    /**
     * @param offHand
     * @param await
     * @since 1.6.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
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
     * @param x         the x coordinate to interact
     * @param y         the y coordinate to interact
     * @param z         the z coordinate to interact
     * @param direction possible values are "up", "down", "north", "south", "east", "west"
     * @return self for chaining.
     * @since 1.8.4
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletReplaceParams("x: int, y: int, z: int, direction: Direction, offHand: boolean")
    public ClientPlayerEntityHelper<T> interactBlock(int x, int y, int z, String direction, boolean offHand) throws InterruptedException {
        return interactBlock(x, y, z, direction, offHand, false);
    }

    /**
     * @param x
     * @param y
     * @param z
     * @param direction 0-5 in order: [DOWN, UP, NORTH, SOUTH, WEST, EAST];
     * @param offHand
     * @since 1.5.0, renamed from {@code interact} in 1.6.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletReplaceParams("x: int, y: int, z: int, direction: Hexit, offHand: boolean")
    public ClientPlayerEntityHelper<T> interactBlock(int x, int y, int z, int direction, boolean offHand) throws InterruptedException {
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
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletReplaceParams("x: int, y: int, z: int, direction: Direction, offHand: boolean, await: boolean")
    public ClientPlayerEntityHelper<T> interactBlock(int x, int y, int z, String direction, boolean offHand, boolean await) throws InterruptedException {
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
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    @DocletReplaceParams("x: int, y: int, z: int, direction: Hexit, offHand: boolean, await: boolean")
    public ClientPlayerEntityHelper<T> interactBlock(int x, int y, int z, int direction, boolean offHand, boolean await) throws InterruptedException {
        return interactBlock(x, y, z, Direction.from3DDataValue(direction), offHand, await);
    }

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
     * @since 1.5.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    public ClientPlayerEntityHelper<T> interact() throws InterruptedException {
        return interact(false);
    }

    /**
     * @param await
     * @since 1.6.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
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
     * @since 1.5.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
    public ClientPlayerEntityHelper<T> attack() throws InterruptedException {
        return attack(false);
    }

    /**
     * @param await
     * @since 1.6.0
     * @deprecated moved to {@code Player.getInteractionManager()}
     */
    @Deprecated
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
     * @param stop
     * @return
     * @since 1.6.3
     * @deprecated use {@code Player.getInteractionManager().breakBlock()} instead
     */
    @Deprecated
    public ClientPlayerEntityHelper<T> setLongAttack(boolean stop) {
        if (!stop) {
            KeyMapping.click(InputConstants.getKey(mc.options.keyAttack.saveString()));
        } else {
            KeyMapping.set(InputConstants.getKey(mc.options.keyAttack.saveString()), false);
        }
        return this;
    }

    /**
     * @param stop
     * @return
     * @since 1.6.3
     * @deprecated use {@code Player.getInteractionManager().holdInteract()} instead
     */
    @Deprecated
    public ClientPlayerEntityHelper<T> setLongInteract(boolean stop) {
        if (!stop) {
            KeyMapping.click(InputConstants.getKey(mc.options.keyUse.saveString()));
        } else {
            KeyMapping.set(InputConstants.getKey(mc.options.keyUse.saveString()), false);
        }
        return this;
    }

    /**
     * every item that is on a cooldown and how long it has left, as a map from the
     * item's id to the number of ticks remaining. An item that is not on a cooldown is
     * not in the map at all, so a lookup that finds nothing is a figure of its own and
     * not a leftover from a previous use.
     * <p>
     * The remaining figure counts down as the cooldown runs, so a second call gives
     * smaller numbers. The map is built fresh each call, so writing to it changes
     * nothing.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   for (const entry of player.getItemCooldownsRemainingTicks().entrySet()) {
     *     // a hundred ticks is five seconds
     *     Chat.log(`${entry.getKey()}: ${entry.getValue()} tick(s) left`);
     *   }
     * }
     * </pre>
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
     * how long the named item has left on its cooldown, in ticks. An item that is not
     * on a cooldown gives {@code -1} rather than zero, so a caller can tell "not
     * cooling down" from "just became available", which are different things to a
     * script that is waiting for one.
     * <p>
     * The name is an item id and the namespace may be left off, in which case it is read
     * as {@code minecraft}. A name that is not a registered item is a name of nothing,
     * so it gives the same {@code -1}.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const left = player.getItemCooldownRemainingTicks("ender_pearl");
     *   if (0 > left) {
     *     Chat.log("an ender pearl is ready to throw");
     *   } else {
     *     Chat.log(`an ender pearl is not ready for ${left} more tick(s)`);
     *   }
     * }
     * </pre>
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
     * every item that is on a cooldown and how long it has been on one, as a map from
     * the item's id to the number of ticks since the cooldown started. It is the other
     * half of {@link #getItemCooldownsRemainingTicks()}, counting up from the start of
     * the cooldown rather than down to the end of it, and an item that is not on a
     * cooldown is not in the map.
     * <p>
     * Neither figure is a total: how long a cooldown lasts is the sum of the two, which
     * a script can work out for itself. The map is built fresh each call.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   for (const entry of player.getTicksSinceCooldownsStart().entrySet()) {
     *     const started = entry.getValue();
     *     const left = player.getItemCooldownRemainingTicks(entry.getKey());
     *     Chat.log(`${entry.getKey()}: ${started} in, ${left} to go`);
     *   }
     * }
     * </pre>
     *
     * @return a map from item id to the ticks since its cooldown started
     * @since 1.6.5
     */
    @DocletReplaceReturn("JavaMap<ItemId, int>")
    public Map<String, Integer> getTicksSinceCooldownsStart() {
        int tick = ((IItemCooldownManager) base.getCooldowns()).jsmacros_getManagerTicks();
        Map<Item, IItemCooldownEntry> map = ((IItemCooldownManager) base.getCooldowns()).jsmacros_getCooldownItems();
        return map.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getName(e.getKey().getDefaultInstance()).getString(), e -> e.getValue().jsmacros_getStartTick() - tick));
    }

    /**
     * how long the named item has been on its cooldown, in ticks, counting up from when
     * the cooldown started. An item that is not on a cooldown gives {@code -1} rather
     * than zero, the same as the remaining-ticks form.
     * <p>
     * The name is an item id and the namespace may be left off, in which case it is read
     * as {@code minecraft}.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const started = player.getTicksSinceCooldownStart("ender_pearl");
     *   if (started > 0) {
     *     const left = player.getItemCooldownRemainingTicks("ender_pearl");
     *     Chat.log(`an ender pearl cooldown is ${started + left} ticks long in all`);
     *   }
     * }
     * </pre>
     *
     * @param item the id of the item to look up
     * @return the ticks since the cooldown started, or {@code -1} if the item is not on
     * one
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
     * how full the player is, as a whole number from zero to twenty where twenty is a
     * full bar. It is the number of hunger points rather than the shank icons, so a
     * player with a full bar reads twenty here and ten half-hunger icons on screen.
     * <p>
     * The number goes down as the player does things and is refilled by eating, and it
     * is the exhaustion the player has built up that decides when it starts going down
     * at all, so a player who has not moved has not spent anything.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const food = player.getFoodLevel();
     *   if (food > 15) {
     *     Chat.log(`not hungry, ${food} of 20`);
     *   } else {
     *     Chat.log(`getting hungry, ${food} of 20`);
     *   }
     * }
     * </pre>
     *
     * @return the player's food level, from zero to twenty
     * @since 1.1.2
     */
    public int getFoodLevel() {
        return base.getFoodData().getFoodLevel();
    }

    /**
     * This will return the invisible hunger decade that you may have seen in mods as a yellow overlay.
     * <p>
     * Saturation is what stops the food level going down once it has been spent, so a
     * player who has just eaten is full and saturated and stays that way for a while
     * without losing anything. It is a fraction rather than a whole number, and it
     * drains as the player uses it up.
     * <p>
     * It is separate from the exhaustion the player has built up by moving, which is
     * what finally brings the food level down. A player with a full bar and no
     * saturation has spent their buffer and is already on the way down.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   Chat.log(`food ${player.getFoodLevel()}, saturation ${player.getSaturation()}`);
     *   // a high saturation is the buffer a well fed player has
     *   if (player.getSaturation() > 5) {
     *     Chat.log("and it will be a while before that runs out");
     *   }
     * }
     * </pre>
     *
     * @return the saturation level.
     * @since 1.8.4
     */
    public float getSaturation() {
        return base.getFoodData().getSaturationLevel();
    }

    /**
     * drops whatever the player is holding, which is the same action as pressing the drop
     * key rather than anything about the item itself.
     * <p>
     * The argument is what happens to a whole stack. With it on, everything in the
     * hand goes; with it off, one item does. An empty hand drops nothing either way, and
     * a stack of one is the same thing under both.
     * <p>
     * The item lands as a dropped stack in the world like any other, and whether it is
     * picked up is up to the player. Nothing here waits for the drop to happen.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const held = player.getMainHand();
     *   if (!held.isEmpty()) {
     *     // the whole stack rather than one of it
     *     player.dropHeldItem(true);
     *     Chat.log(`dropped ${held.getCount()} ${held.getItemId()}`);
     *   }
     * }
     * </pre>
     *
     * @param dropStack whether to drop the whole stack rather than one of it
     * @return self for chaining.
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<?> dropHeldItem(boolean dropStack) {
        base.drop(dropStack);
        return this;
    }

    /**
     * an advancement manager to work with advancements, which is the player's own
     * advancements rather than the ones of everybody in the game. It is a wrapper on
     * the tree the client has been sent, so what it holds is what the server knows and
     * not what has been earned since.
     * <p>
     * It needs a player that has joined a world: a client with nothing loaded has no
     * connection to read the tree from, so this is worth guarding rather than calling
     * at startup.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const manager = player.getAdvancementManager();
     *   Chat.log(`${manager.getAdvancements().size()} advancement(s) tracked`);
     *   for (const root of manager.getRootAdvancements()) {
     *     Chat.log(`root: ${root.getId()}`);
     *   }
     * }
     * </pre>
     *
     * @return an advancement manager to work with advancements.
     * @since 1.8.4
     */
    public AdvancementManagerHelper getAdvancementManager() {
        return new AdvancementManagerHelper(base.connection.getAdvancements().getTree());
    }

    /**
     * The returned time is an approximation and will likely be off by a few ticks, although it
     * should always be less than the actual time.
     * <p>
     * The item is the one in the player's main hand, so this is the same as passing that
     * in by hand. Three figures come out of it rather than a time, and none of them is a
     * number of ticks. A time of zero means the block comes away at once, either
     * because the player is in creative or because the block is soft enough for the item
     * in hand; a time of {@code -1} means the block cannot be mined with that item at
     * all, or is one that has no mining speed to speak of.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const block = player.rayTraceBlock(5, false);
     *   if (block !== null) {
     *     // the calculation wants the block state, which is one step further on
     *     const ticks = player.calculateMiningSpeed(block.getBlockStateHelper());
     *     if (0 > ticks) {
     *       Chat.log("the held item cannot mine that");
     *     } else {
     *       if (ticks === 0) {
     *         Chat.log("at once");
     *       } else {
     *         Chat.log(`about ${ticks} tick(s), ${(ticks / 20).toFixed(1)} seconds`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @param block the block to mine
     * @return the time in ticks that it will approximately take the player with the currently held
     * item to mine said block.
     */
    public int calculateMiningSpeed(BlockStateHelper block) {
        return calculateMiningSpeed(getMainHand(), block);
    }

    /**
    /**
     * Calculate mining speed for a given block mined with a specified item in ticks. Use air to
     * calculate the mining speed for the hand. The returned time is an approximation and will
     * likely be off by a few ticks, although it should always be less than the actual time.
     * <p>
     * This is the form to use when a script wants to work out what a tool would be worth
     * rather than what the player is holding, and air is what gives the bare hand. The
     * player is what the calculation is done for, so their effects, their gamemode and
     * whether they are standing in water or in the air all come into it even though the
     * item is the one being varied.
     * <p>
     * The three figures are the same as the one-item form's: a time in ticks, zero for a
     * block that comes away at once, and {@code -1} for one the item cannot mine.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const block = player.rayTraceBlock(5, false);
     *   if (block !== null) {
     *     // a diamond pickaxe on the block in front, rather than whatever is held
     *     const ItemStackHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper");
     *     const pick = new ItemStackHelper("minecraft:diamond_pickaxe", 1);
     *     const state = block.getBlockStateHelper();
     *     Chat.log(`${player.calculateMiningSpeed(pick, state)} tick(s) with a pickaxe`);
     *   }
     * }
     * </pre>
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
