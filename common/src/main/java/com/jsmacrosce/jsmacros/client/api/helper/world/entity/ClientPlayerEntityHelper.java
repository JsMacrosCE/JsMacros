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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * @author Wagyourtail
 * @see PlayerEntityHelper
 * @since 1.0.3
 */
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
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> setVelocity(Pos3D velocity) {
        return setVelocity(new Vec3(velocity.x, velocity.y, velocity.z));
    }

    /**
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
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> addVelocity(Pos3D velocity) {
        return addVelocity(new Vec3(velocity.x, velocity.y, velocity.z));
    }

    /**
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
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> setPos(Pos3D pos) throws InterruptedException {
        return setPos(pos, false);
    }

    /**
     * @since 1.9.0
     */
    public ClientPlayerEntityHelper<T> setPos(Pos3D pos, boolean await) throws InterruptedException {
        return setPos(new Vec3(pos.x, pos.y, pos.z), await);
    }

    /**
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> setPos(double x, double y, double z) throws InterruptedException {
        return setPos(x, y, z, false);
    }

    /**
     * @since 1.9.0
     */
    public ClientPlayerEntityHelper<T> setPos(double x, double y, double z, boolean await) throws InterruptedException {
        return setPos(new Vec3(x, y, z), await);
    }

    /**
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> addPos(Pos3D pos) throws InterruptedException {
        return addPos(pos, false);
    }

    /**
     * @since 1.9.0
     */
    public ClientPlayerEntityHelper<T> addPos(Pos3D pos, boolean await) throws InterruptedException {
        return setPos(getPos().add(pos), await);
    }

    /**
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> addPos(double x, double y, double z) throws InterruptedException {
        return addPos(x, y, z, false);
    }

    /**
     * @since 1.9.0
     */
    public ClientPlayerEntityHelper<T> addPos(double x, double y, double z, boolean await) throws InterruptedException {
        return setPos(getPos().add(x, y, z), await);
    }

    /**
     * Sets the player rotation along the given axis and keeps the other axis the same.
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
     * @param yaw   (was pitch prior to 1.2.6)
     * @param pitch (was yaw prior to 1.2.6)
     * @return
     * @since 1.0.3
     */
    public ClientPlayerEntityHelper<T> lookAt(double yaw, double pitch) {
        pitch = Mth.clamp(pitch, -90.0F, 90.0F);
        base.xRotO = base.getXRot();
        base.yRotO = base.getYRot();
        base.setXRot((float) pitch);
        base.setYRot(Mth.wrapDegrees((float) yaw));
        if (base.getVehicle() != null) {
            base.getVehicle().onPassengerTurned(base);
        }
        return this;
    }

    /**
     * look at the specified coordinates.
     *
     * @param x
     * @param y
     * @param z
     * @return
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
     * @return self for chaining.
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> turnLeft() {
        return lookAt(getYaw() - 90, getPitch());
    }

    /**
     * @return self for chaining.
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<T> turnRight() {
        return lookAt(getYaw() + 90, getPitch());
    }

    /**
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
            InteractionResult result = mc.gameMode.interact(mc.player, entity.getRaw(), hand);
            assert mc.player != null;
            if (result.consumesAction()) {
                mc.player.swing(hand);
            }
        } else {
            Semaphore wait = new Semaphore(await ? 0 : 1);
            mc.execute(() -> {
                InteractionResult result = mc.gameMode.interact(mc.player, entity.getRaw(), hand);
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
     * @return
     * @since 1.6.5
     */
    @DocletReplaceReturn("JavaMap<ItemId, int>")
    public Map<String, Integer> getItemCooldownsRemainingTicks() {
        int tick = ((IItemCooldownManager) base.getCooldowns()).jsmacros_getManagerTicks();
        Map<Item, IItemCooldownEntry> map = ((IItemCooldownManager) base.getCooldowns()).jsmacros_getCooldownItems();
        return map.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getName().getString(), e -> e.getValue().jsmacros_getEndTick() - tick));
    }

    /**
     * @param item
     * @return
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
     * @return
     * @since 1.6.5
     */
    @DocletReplaceReturn("JavaMap<ItemId, int>")
    public Map<String, Integer> getTicksSinceCooldownsStart() {
        int tick = ((IItemCooldownManager) base.getCooldowns()).jsmacros_getManagerTicks();
        Map<Item, IItemCooldownEntry> map = ((IItemCooldownManager) base.getCooldowns()).jsmacros_getCooldownItems();
        return map.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getName().getString(), e -> e.getValue().jsmacros_getStartTick() - tick));
    }

    /**
     * @param item
     * @return
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
     * @return
     * @since 1.1.2
     */
    public int getFoodLevel() {
        return base.getFoodData().getFoodLevel();
    }

    /**
     * This will return the invisible hunger decade that you may have seen in mods as a yellow overlay.
     *
     * @return the saturation level.
     * @since 1.8.4
     */
    public float getSaturation() {
        return base.getFoodData().getSaturationLevel();
    }

    /**
     * @return self for chaining.
     * @since 1.8.4
     */
    public ClientPlayerEntityHelper<?> dropHeldItem(boolean dropStack) {
        base.drop(dropStack);
        return this;
    }

    /**
     * @return an advancement manager to work with advancements.
     * @since 1.8.4
     */
    public AdvancementManagerHelper getAdvancementManager() {
        return new AdvancementManagerHelper(base.connection.getAdvancements().getTree());
    }

    /**
     * The returned time is an approximation and will likely be off by a few ticks, although it
     * should always be less than the actual time.
     *
     * @param block the block to mine
     * @return the time in ticks that it will approximately take the player with the currently held
     * item to mine said block.
     */
    public int calculateMiningSpeed(BlockStateHelper block) {
        return calculateMiningSpeed(getMainHand(), block);
    }

    /**
     * Calculate mining speed for a given block mined with a specified item in ticks. Use air to
     * calculate the mining speed for the hand. The returned time is an approximation and will
     * likely be off by a few ticks, although it should always be less than the actual time.
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
