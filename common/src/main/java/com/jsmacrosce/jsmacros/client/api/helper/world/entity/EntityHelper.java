package com.jsmacrosce.jsmacros.client.api.helper.world.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.doclet.DocletReplaceTypeParams;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.api.math.Pos2D;
import com.jsmacrosce.jsmacros.api.math.Pos3D;
import com.jsmacrosce.jsmacros.client.access.IMixinEntity;
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;
import com.jsmacrosce.jsmacros.client.api.helper.NBTElementHelper;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockDataHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.ChunkHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.DirectionHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.boss.EnderDragonEntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.boss.WitherEntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.ArmorStandEntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.EndCrystalEntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.ItemFrameEntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.decoration.PaintingEntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.BlockDisplayEntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.DisplayEntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.ItemDisplayEntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.display.TextDisplayEntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.*;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.other.InteractionEntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive.*;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.ArrowEntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.FishingBobberEntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.TridentEntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile.WitherSkullEntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle.BoatEntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle.FurnaceMinecartEntityHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.vehicle.TntMinecartEntityHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;
import com.jsmacrosce.jsmacros.util.ChunkPosCompat;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
*///? } else {
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.Drowned;
//? }

//? if >1.21.5 {
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueOutput;
//?}

/**
 * Wraps a Minecraft entity for script access. Factories such as
 * {@code World.getEntities()} return the most specific supported helper subtype;
 * helpers wrap the underlying entity rather than copying it.
 * <pre>
 * const nearby = World.getEntities(10);
 * if (nearby !== null) {
 *   for (const entity of nearby) {
 *     Chat.log(`${entity.getType()} at ${entity.getPos()}`);
 *   }
 * }
 * </pre>
 *
 * @param <T> the Minecraft entity wrapped by this helper
 * @author Wagyourtail
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class EntityHelper<T extends Entity> extends BaseHelper<T> {

    protected EntityHelper(T e) {
        super(e);
    }

    /**
     * Returns a copy of the entity's current position in world coordinates.
     * @return the entity's position.
     */
    public Pos3D getPos() {
        return new Pos3D(base.getX(), base.getY(), base.getZ());
    }

    /**
     * Returns the entity's position interpolated between its previous and current
     * positions by the supplied partial-tick fraction.
     * @param partialTicks the partial-tick interpolation fraction.
     * @return the interpolated entity position.
     */
    public Pos3D getPos(float partialTicks) {
        return new Pos3D(base.getPosition(partialTicks));
    }

    /**
     * Interpolated entity position, used for render-time placement.
     *
     * @since 2.0.0
     */
    @DocletIgnore
    public Pos3D getInterpolatedPos(float partialTicks) {
        return getPos(partialTicks);
    }

    /**
     * Returns the block position containing the entity's position (coordinates are
     * rounded down, not to the nearest block).
     * @return the entity's block position.
     * @since 1.6.5
     */
    public BlockPosHelper getBlockPos() {
        return new BlockPosHelper(base.blockPosition());
    }

    /**
     * Returns the position of the entity's eyes, which depends on its current pose.
     * @return the entity's eye position.
     * @since 1.8.4
     */
    public Pos3D getEyePos() {
        return new Pos3D(base.getEyePosition().x, base.getEyePosition().y, base.getEyePosition().z);
    }

    /**
     * Returns the entity's chunk coordinates as a {@link Pos2D}: x is chunk x and y
     * is world chunk z.
     * @return the entity's chunk coordinates.
     * @since 1.6.5
     */
    public Pos2D getChunkPos() {
        ChunkPos pos = base.chunkPosition();
        return new Pos2D(ChunkPosCompat.x(pos), ChunkPosCompat.z(pos));
    }

    /**
     * Returns the entity's current x coordinate in world coordinates.
     * @return the x coordinate.
     * @since 1.0.8
     */
    public double getX() {
        return base.getX();
    }

    /**
     * Returns the entity's current y coordinate in world coordinates.
     * @return the y coordinate.
     * @since 1.0.8
     */
    public double getY() {
        return base.getY();
    }

    /**
     * Returns the entity's current z coordinate in world coordinates.
     * @return the z coordinate.
     * @since 1.0.8
     */
    public double getZ() {
        return base.getZ();
    }

    /**
     * Returns the entity's eye-height offset for its current pose. Adding it to
     * {@link #getY()} gives the eye position's y coordinate.
     *
     * @return the current eye height offset for the entity.
     * @since 1.2.8
     */
    public double getEyeHeight() {
        return base.getEyeHeight(base.getPose());
    }

    /**
     * Returns the entity's pitch in degrees, without angle wrapping.
     *
     * @return the {@code pitch} value of the entity.
     * @since 1.0.8
     */
    public float getPitch() {
        return base.getXRot();
    }

    /**
     * Returns the entity's yaw in degrees, wrapped to the range {@code [-180, 180)}.
     *
     * @return the {@code yaw} value of the entity.
     * @since 1.0.8
     */
    public float getYaw() {
        return Mth.wrapDegrees(base.getYRot());
    }

    /**
     * Returns the entity's displayed name as text, including its styling or custom name.
     * Before 1.6.4, this returned a {@code String}.
     *
     * @return the name of the entity.
     * @since 1.0.2
     */
    public TextHelper getName() {
        return TextHelper.wrap(base.getName());
    }

    /**
     * Returns the entity type's registry id, including its namespace; this identifies
     * the type, not an individual entity.
     *
     * @return the type of the entity.
     */
    @DocletReplaceReturn("EntityId")
    public String getType() {
        return EntityType.getKey(base.getType()).toString();
    }

    /**
     * Returns whether this entity's type matches any specified entity id.
     * <p>
     * This is a check on {@link #getType()}, so what is being matched is the kind of
     * thing rather than anything about this individual. Several types can be given at
     * once, in which case the answer is whether the entity is any of them rather than
     * all of them. Each is an entity id and the namespace may be left off, in which case
     * it is read as {@code minecraft}.
     * <p>
     * Giving no ids answers {@code false}, since there is nothing to match.
     *
     * @param types the entity ids to match against
     * @return {@code true} if this entity is any of the given types, {@code false}
     * otherwise.
     * @since 1.9.0
     */
    @DocletReplaceTypeParams("E extends CanOmitNamespace<EntityId>")
    @DocletReplaceParams("...anyOf: JavaVarArgs<E>")
    @DocletReplaceReturn("this is EntityTypeFromId<E>")
    public boolean is(String ...types) {
        return Arrays.stream(types).map(RegistryHelper::parseNameSpace).anyMatch(getType()::equals);
    }

    /**
     * Returns whether the entity is currently glowing. This does not identify the
     * cause; the outline color is read separately with {@link #getGlowingColor()}.
     *
     * @return if the entity has the glowing effect.
     * @since 1.1.9
     */
    public boolean isGlowing() {
        return base.isCurrentlyGlowing();
    }

    /**
     * Returns whether the entity is in lava; this is distinct from {@link #isOnFire()}.
     *
     * @return if the entity is in lava.
     * @since 1.1.9
     */
    public boolean isInLava() {
        return base.isInLava();
    }

    /**
     * Returns whether the entity is on fire.
     *
     * @return if the entity is on fire.
     * @since 1.1.9
     */
    public boolean isOnFire() {
        return base.isOnFire();
    }

    /**
     * Returns whether the entity's shift-key-down state is set; this is not a pose check.
     *
     * @return {@code true} if the entity is sneaking, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSneaking() {
        return base.isShiftKeyDown();
    }

    /**
     * Returns whether the entity's sprinting state is set.
     *
     * @return {@code true} if the entity is sprinting, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSprinting() {
        return base.isSprinting();
    }

    /**
     * Returns the vehicle carrying this entity, or {@code null} if it has none.
     *
     * @return the vehicle of the entity.
     * @since 1.2.5
     */
    @Nullable
    public EntityHelper<?> getVehicle() {
        Entity parent = base.getVehicle();
        if (parent != null) {
            return EntityHelper.create(parent);
        }
        return null;
    }

    /**
     * Traces from the entity to find a block within the given distance, or returns
     * {@code null} if the trace misses or hits void air. Distance is in blocks; the
     * {@code fluid} argument controls fluid handling.
     *
     * @param distance how far to reach in blocks
     * @param fluid    whether fluids should be traced through and against
     * @return the block the entity is looking at, or {@code null} if there is none
     * @since 1.9.0
     */
    @Nullable
    public BlockDataHelper rayTraceBlock(double distance, boolean fluid) {
        BlockHitResult h = (BlockHitResult) base.pick(distance, 0, fluid);
        if (h.getType() == HitResult.Type.MISS) {
            return null;
        }
        BlockState b = base.level().getBlockState(h.getBlockPos());
        BlockEntity t = base.level().getBlockEntity(h.getBlockPos());
        if (b.getBlock().equals(Blocks.VOID_AIR)) {
            return null;
        }
        return new BlockDataHelper(b, t, h.getBlockPos());
    }


    /**
     * Returns the pickable entity hit by the entity's targeting trace, or {@code null}
     * if no entity is hit within the given distance. Distance is in blocks.
     *
     * @param distance how far to reach in blocks
     * @return the hit entity, or {@code null} if there is no hit
     * @since 1.9.0
     */
    @Nullable
    public EntityHelper<?> rayTraceEntity(int distance) {
        return DebugRenderer.getTargetedEntity(base, distance).map(EntityHelper::create).orElse(null);
    }

    /**
     * Returns the entity's passengers, or {@code null} if it has none.
     *
     * @return the entity passengers, or {@code null} if there are none
     * @since 1.2.5
     */
    @Nullable
    public List<EntityHelper<?>> getPassengers() {
        List<EntityHelper<?>> entities = base.getPassengers().stream().map(EntityHelper::create).collect(Collectors.toList());
        return entities.size() == 0 ? null : entities;

    }

    /**
     * Returns a newly serialized NBT compound for the entity, excluding its ID.
     *
     * @return the entity as a piece of NBT
     * @since 1.2.8, was a {@link String} until 1.5.0
     */
    public NBTElementHelper.NBTCompoundHelper getNBT() {
        //? if >1.21.5 {
        ValueOutput view = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                Objects.requireNonNull(Minecraft.getInstance().getConnection()).registryAccess()
        );
        base.saveWithoutId(view);
        CompoundTag nbt = ((TagValueOutput) view).buildResult();
        //?} else {
        /*CompoundTag nbt = new CompoundTag();
        base.saveWithoutId(nbt);
        *///?}

        return NBTElementHelper.wrapCompound(nbt);
    }

    /**
     * Sets the entity's custom name; passing {@code null} clears it.
     *
     * @param name the name to give the entity, or {@code null} to take it off
     * @return self for chaining.
     * @since 1.6.4
     */
    public EntityHelper<T> setCustomName(@Nullable TextHelper name) {
        if (name == null) {
            base.setCustomName(null);
        } else {
            base.setCustomName(name.getRaw());
        }
        return this;
    }

    /**
     * Sets whether the entity's custom name is always visible.
     *
     * @param b whether the name should always be shown
     * @return self for chaining.
     * @since 1.8.0
     */
    public EntityHelper<T> setCustomNameVisible(boolean b) {
        base.setCustomNameVisible(b);
        return this;
    }

    /**
     * Sets the entity's client-side glow color using a packed color value. This can
     * override the color used for drawing without changing its team color.
     *
     * @param color the packed rgb colour to glow in
     * @return self for chaining.
     */
    public EntityHelper<T> setGlowingColor(int color) {
        ((IMixinEntity) base).jsmacros_setGlowingColor(color);
        return this;
    }

    /**
     * Restores the entity's team-derived glow colour after {@link #setGlowingColor(int)}.
     * This does not change whether the entity glows; use {@link #resetGlowing()} for that.
     *
     * @return self for chaining.
     */
    public EntityHelper<T> resetGlowingColor() {
        ((IMixinEntity) base).jsmacros_resetColor();
        return this;
    }

    /**
     * Returns the entity's team colour, or {@code 0} if it has no team colour.
     * A colour set by {@link #setGlowingColor(int)} can override the colour used for drawing
     * without changing this value.
     *
     * @return glow color
     * @since 1.8.2
     */
    public int getGlowingColor() {
        return base.getTeamColor();
    }

    /**
     * Forces the entity's client-side glow outline on or off, overriding the game's
     * normal decision. {@link #resetGlowing()} restores the game's decision.
     *
     * @param val whether the entity should be glowing
     * @return self for chaining.
     * @since 1.1.9
     */
    public EntityHelper<T> setGlowing(boolean val) {
        ((IMixinEntity) base).jsmacros_setForceGlowing(val ? 2 : 0);
        return this;
    }

    /**
     * Restores the game's decision about whether the entity glows. This does not reset
     * its glow colour; use {@link #resetGlowingColor()} for that.
     *
     * @return self for chaining.
     * @since 1.6.3
     */
    public EntityHelper<T> resetGlowing() {
        ((IMixinEntity) base).jsmacros_setForceGlowing(1);
        return this;
    }

    /**
     * Returns whether the entity is alive.
     *
     * @return {@code true} if the entity has not been killed, {@code false} otherwise
     * @since 1.2.8
     */
    public boolean isAlive() {
        return base.isAlive();
    }

    /**
     * @return the entity's UUID as a string.
     *
     * @since 1.6.5
     */
    public String getUUID() {
        return base.getUUID().toString();
    }

    /**
     * Returns the maximum air supply, in ticks.
     *
     * @return the maximum amount of air this entity can have.
     * @since 1.8.4
     */
    public int getMaxAir() {
        return base.getMaxAirSupply();
    }

    /**
     * Returns the current air supply, in ticks.
     *
     * @return the amount of air this entity has.
     * @since 1.8.4
     */
    public int getAir() {
        return base.getAirSupply();
    }

    /**
     * Returns horizontal movement since the previous tick, scaled to blocks per second.
     *
     * @return this entity's current speed in blocks per second.
     * @since 1.8.4
     */
    public double getSpeed() {
        double dx = Math.abs(base.getX() - base.xo);
        double dz = Math.abs(base.getZ() - base.zo);
        return Math.sqrt(dx * dx + dz * dz) * 20;
    }

    /**
     * Returns the horizontal cardinal direction corresponding to the entity's yaw.
     *
     * @return the direction the entity is facing, rounded to the nearest 90 degrees.
     * @since 1.8.4
     */
    public DirectionHelper getFacingDirection() {
        return new DirectionHelper(base.getDirection());
    }

    /**
     * Returns the three-dimensional distance between the entities' positions (not their eyes).
     *
     * @param entity the entity to measure to
     * @return the distance in blocks.
     * @since 1.8.4
     */
    public float distanceTo(EntityHelper<?> entity) {
        return base.distanceTo(entity.getRaw());
    }

    /**
     * Returns the distance from this entity's position to the center of the block.
     *
     * @param pos the position to measure to
     * @return the distance in blocks.
     * @since 1.8.4
     */
    public double distanceTo(BlockPosHelper pos) {
        return Math.sqrt(pos.getRaw().distToCenterSqr(base.position()));
    }

    /**
     * Returns the three-dimensional distance from this entity's position to the given position.
     *
     * @param pos the position to measure to
     * @return the distance in blocks.
     * @since 1.8.4
     */
    public double distanceTo(Pos3D pos) {
        return Math.sqrt(base.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()));
    }

    /**
     * Returns the three-dimensional distance from this entity's position to the given coordinates.
     *
     * @param x the x coordinate to measure to
     * @param y the y coordinate to measure to
     * @param z the z coordinate to measure to
     * @return the distance in blocks.
     * @since 1.8.4
     */
    public double distanceTo(double x, double y, double z) {
        return Math.sqrt(base.distanceToSqr(x, y, z));
    }

    /**
     * Returns a copy of the entity's current movement vector, in blocks per tick. Changing
     * the returned position does not change the entity's movement.
     *
     * @return the velocity vector.
     * @since 1.8.4
     */
    public Pos3D getVelocity() {
        return new Pos3D(base.getDeltaMovement().x, base.getDeltaMovement().y, base.getDeltaMovement().z);
    }

    /**
     * Returns the chunk containing this entity's block position.
     *
     * @return the chunk helper for the chunk this entity is in.
     * @since 1.8.4
     */
    public ChunkHelper getChunk() {
        return new ChunkHelper(base.level().getChunk(base.blockPosition()));
    }

    /**
     * Returns the registry ID of the biome at this entity's block position in the client's current world.
     *
     * @return the biome registry ID, including its namespace.
     * @since 1.8.4
     */
    @DocletReplaceReturn("Biome")
    public String getBiome() {
        return Minecraft.getInstance().level.registryAccess().lookupOrThrow(Registries.BIOME).getKey(Minecraft.getInstance().level.getBiome(base.blockPosition()).value()).toString();
    }

    /**
     * Returns a diagnostic string containing this helper's runtime class name, the
     * representation of its {@link #getName()} text helper, and its entity type ID.
     * @return a diagnostic representation of the entity
     */
    @Override
    public String toString() {
        return String.format("%s:{\"name\": \"%s\", \"type\": \"%s\"}", getClass().getSimpleName(), this.getName(), this.getType());
    }

    /**
     * Wraps a Minecraft entity in the most specific supported helper subtype. Throws
     * {@link NullPointerException} if {@code e} is {@code null}.
     *
     * @param e the Minecraft entity to wrap
     * @return the helper subtype matching the entity, or a plain {@code EntityHelper} if no more specific helper applies
     */
    public static EntityHelper<?> create(@NotNull Entity e) {
        Objects.requireNonNull(e, "Entity cannot be null.");

        // Players
        if (e instanceof LocalPlayer) {
            return new ClientPlayerEntityHelper<>((LocalPlayer) e);
        }
        if (e instanceof Player) {
            return new PlayerEntityHelper<>((Player) e);
        }

        if (e instanceof Mob) {
            // Merchants
            if (e instanceof Villager) {
                return new VillagerEntityHelper((Villager) e);
            }
            if (e instanceof AbstractVillager) {
                return new MerchantEntityHelper<>((AbstractVillager) e);
            }

            // Bosses
            if (e instanceof EnderDragon) {
                return new EnderDragonEntityHelper(((EnderDragon) e));
            } else if (e instanceof WitherBoss) {
                return new WitherEntityHelper(((WitherBoss) e));
            }

            // Hostile mobs
            if (e instanceof AbstractPiglin) {
                if (e instanceof Piglin) {
                    return new PiglinEntityHelper(((Piglin) e));
                } else {
                    return new AbstractPiglinEntityHelper<>(((AbstractPiglin) e));
                }
            } else if (e instanceof Creeper) {
                return new CreeperEntityHelper(((Creeper) e));
            } else if (e instanceof Zombie) {
                if (e instanceof Drowned) {
                    return new DrownedEntityHelper(((Drowned) e));
                } else if (e instanceof ZombieVillager) {
                    return new ZombieVillagerEntityHelper(((ZombieVillager) e));
                } else {
                    return new ZombieEntityHelper<>(((Zombie) e));
                }
            } else if (e instanceof EnderMan) {
                return new EndermanEntityHelper(((EnderMan) e));
            } else if (e instanceof Ghast) {
                return new GhastEntityHelper(((Ghast) e));
            } else if (e instanceof Blaze) {
                return new BlazeEntityHelper(((Blaze) e));
            } else if (e instanceof Guardian) {
                return new GuardianEntityHelper(((Guardian) e));
            } else if (e instanceof Phantom) {
                return new PhantomEntityHelper(((Phantom) e));
            } else if (e instanceof AbstractIllager) {
                if (e instanceof Vindicator) {
                    return new VindicatorEntityHelper(((Vindicator) e));
                } else if (e instanceof Pillager) {
                    return new PillagerEntityHelper(((Pillager) e));
                } else if (e instanceof SpellcasterIllager) {
                    return new SpellcastingIllagerEntityHelper<>(((SpellcasterIllager) e));
                } else {
                    return new IllagerEntityHelper<>(((AbstractIllager) e));
                }
            } else if (e instanceof Shulker) {
                return new ShulkerEntityHelper(((Shulker) e));
            } else if (e instanceof Slime) {
                return new SlimeEntityHelper(((Slime) e));
            } else if (e instanceof Spider) {
                return new SpiderEntityHelper(((Spider) e));
            } else if (e instanceof Vex) {
                return new VexEntityHelper(((Vex) e));
            } else if (e instanceof Warden) {
                return new WardenEntityHelper(((Warden) e));
            } else if (e instanceof Witch) {
                return new WitchEntityHelper(((Witch) e));
            }

            // Animals
            if (e instanceof Animal) {
                if (e instanceof AbstractHorse) {
                    if (e instanceof Horse) {
                        return new HorseEntityHelper(((Horse) e));
                    } else if (e instanceof AbstractChestedHorse) {
                        if (e instanceof Llama) {
                            return new LlamaEntityHelper<>(((Llama) e));
                        } else {
                            return new DonkeyEntityHelper<>(((AbstractChestedHorse) e));
                        }
                    } else {
                        return new AbstractHorseEntityHelper<>(((AbstractHorse) e));
                    }
                } else if (e instanceof Axolotl) {
                    return new AxolotlEntityHelper(((Axolotl) e));
                } else if (e instanceof Bee) {
                    return new BeeEntityHelper(((Bee) e));
                } else if (e instanceof Fox) {
                    return new FoxEntityHelper(((Fox) e));
                } else if (e instanceof Frog) {
                    return new FrogEntityHelper(((Frog) e));
                } else if (e instanceof Goat) {
                    return new GoatEntityHelper(((Goat) e));
                } else if (e instanceof MushroomCow) {
                    return new MooshroomEntityHelper(((MushroomCow) e));
                } else if (e instanceof Ocelot) {
                    return new OcelotEntityHelper(((Ocelot) e));
                } else if (e instanceof Panda) {
                    return new PandaEntityHelper(((Panda) e));
                } else if (e instanceof Pig) {
                    return new PigEntityHelper(((Pig) e));
                } else if (e instanceof PolarBear) {
                    return new PolarBearEntityHelper(((PolarBear) e));
                } else if (e instanceof Rabbit) {
                    return new RabbitEntityHelper(((Rabbit) e));
                } else if (e instanceof Sheep) {
                    return new SheepEntityHelper(((Sheep) e));
                } else if (e instanceof Strider) {
                    return new StriderEntityHelper(((Strider) e));
                } else if (e instanceof TamableAnimal) {
                    if (e instanceof Cat) {
                        return new CatEntityHelper(((Cat) e));
                    } else if (e instanceof Wolf) {
                        return new WolfEntityHelper(((Wolf) e));
                    } else if (e instanceof Parrot) {
                        return new ParrotEntityHelper(((Parrot) e));
                    } else {
                        return new TameableEntityHelper<>(((TamableAnimal) e));
                    }
                } else {
                    return new AnimalEntityHelper<>(((Animal) e));
                }
            }

            // Neutral mobs
            if (e instanceof Allay) {
                return new AllayEntityHelper(((Allay) e));
            } else if (e instanceof Bat) {
                return new BatEntityHelper(((Bat) e));
            } else if (e instanceof Dolphin) {
                return new DolphinEntityHelper(((Dolphin) e));
            } else if (e instanceof IronGolem) {
                return new IronGolemEntityHelper(((IronGolem) e));
            } else if (e instanceof SnowGolem) {
                return new SnowGolemEntityHelper(((SnowGolem) e));
            } else if (e instanceof AbstractFish) {
                if (e instanceof Pufferfish) {
                    return new PufferfishEntityHelper(((Pufferfish) e));
                } else if (e instanceof TropicalFish) {
                    return new TropicalFishEntityHelper(((TropicalFish) e));
                } else {
                    return new FishEntityHelper<>(((AbstractFish) e));
                }
            }
        }

        // Projectiles
        if (e instanceof Projectile) {
            if (e instanceof Arrow) {
                return new ArrowEntityHelper(((Arrow) e));
            } else if (e instanceof FishingHook) {
                return new FishingBobberEntityHelper(((FishingHook) e));
            } else if (e instanceof ThrownTrident) {
                return new TridentEntityHelper(((ThrownTrident) e));
            } else if (e instanceof WitherSkull) {
                return new WitherSkullEntityHelper(((WitherSkull) e));
            }
        }

        // Decorations
        if (e instanceof ArmorStand) {
            return new ArmorStandEntityHelper(((ArmorStand) e));
        } else if (e instanceof EndCrystal) {
            return new EndCrystalEntityHelper(((EndCrystal) e));
        } else if (e instanceof ItemFrame) {
            return new ItemFrameEntityHelper(((ItemFrame) e));
        } else if (e instanceof Painting) {
            return new PaintingEntityHelper(((Painting) e));
        }

        // Vehicles
        if (e instanceof AbstractBoat) {
            return new BoatEntityHelper(((AbstractBoat) e));
        } else if (e instanceof MinecartFurnace) {
            return new FurnaceMinecartEntityHelper(((MinecartFurnace) e));
        } else if (e instanceof MinecartTNT) {
            return new TntMinecartEntityHelper(((MinecartTNT) e));
        }

        if (e instanceof LivingEntity) {
            return new LivingEntityHelper<>((LivingEntity) e);
        }
        if (e instanceof ItemEntity) {
            return new ItemEntityHelper((ItemEntity) e);
        }
        if (e instanceof Display) {
            if (e instanceof Display.ItemDisplay) {
                return new ItemDisplayEntityHelper((Display.ItemDisplay) e);
            } else if (e instanceof Display.TextDisplay) {
                return new TextDisplayEntityHelper((Display.TextDisplay) e);
            } else if (e instanceof Display.BlockDisplay) {
                return new BlockDisplayEntityHelper((Display.BlockDisplay) e);
            }
            return new DisplayEntityHelper<>((Display) e);
        }
        if (e instanceof Interaction) {
            return new InteractionEntityHelper((Interaction) e);
        }
        return new EntityHelper<>(e);
    }

    /**
     * Casts this helper to a local-player helper. This is a Java cast, not a type check;
     * it throws {@code ClassCastException} unless this helper wraps the local player.
     *
     * @return this helper as a local-player helper
     * @since 1.6.3
     */
    public ClientPlayerEntityHelper<?> asClientPlayer() {
        return (ClientPlayerEntityHelper<?>) this;
    }

    /**
     * Casts this helper to a player helper. This is a Java cast, not a type check;
     * it throws {@code ClassCastException} unless this helper wraps a player.
     *
     * @return this helper as a player helper
     * @since 1.6.3
     */
    public PlayerEntityHelper<?> asPlayer() {
        return (PlayerEntityHelper<?>) this;
    }

    /**
     * Casts this helper to a villager helper. This cast is valid only for villagers;
     * other merchant entities, such as wandering traders, are not villagers.
     *
     * @return this helper as a villager helper
     * @since 1.6.3
     */
    public VillagerEntityHelper asVillager() {
        return (VillagerEntityHelper) this;
    }

    /**
     * Casts this helper to a merchant helper. Villagers and other merchant entities,
     * including wandering traders, can be cast; the helper exposes merchant state.
     *
     * @return this helper as a merchant helper
     * @since 1.6.3
     */
    public MerchantEntityHelper<?> asMerchant() {
        return (MerchantEntityHelper<?>) this;
    }

    /**
     * Casts this helper to a living-entity helper, exposing living-entity state.
     *
     * @return this helper as a living-entity helper
     * @since 1.6.3
     */
    public LivingEntityHelper<?> asLiving() {
        return (LivingEntityHelper<?>) this;
    }

    /**
     * Casts this helper to an animal-entity helper. The entity must be an animal;
     * being a living entity alone is not sufficient.
     *
     * @return this helper as an animal-entity helper
     * @since 1.8.4
     */
    public LivingEntityHelper<?> asAnimal() {
        return (AnimalEntityHelper<?>) this;
    }

    /**
     * Casts this helper to an item-entity helper for a dropped item entity.
     *
     * @return this helper as an item-entity helper
     * @since 1.6.3
     */
    public ItemEntityHelper asItem() {
        return (ItemEntityHelper) this;
    }

    /**
     * Looks up the same UUID in the integrated server's level corresponding to this
     * entity's dimension. The lookup runs on the server thread; the returned helper
     * wraps a server-side entity and must only be accessed there. Returns {@code null}
     * if there is no integrated server, level, or matching loaded entity.
     *
     * @return the server-side helper, or {@code null} if the lookup finds no entity
     * @since 1.8.4
     */
    @Nullable
    public EntityHelper<?> asServerEntity() {
        Minecraft client = Minecraft.getInstance();
        if (!client.hasSingleplayerServer()) {
            return null;
        }
        var server = client.getSingleplayerServer();
        var dimension = base.level().dimension();
        var uuid = base.getUUID();
        java.util.function.Supplier<EntityHelper<?>> lookup = () -> {
            var level = server.getLevel(dimension);
            if (level == null) return null;
            Entity entity = level.getEntity(uuid);
            return entity == null ? null : create(entity);
        };
        try {
            return server.isSameThread() ? lookup.get() : server.submit(lookup).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for integrated server", e);
        } catch (Exception e) {
            throw new IllegalStateException("Could not look up entity on integrated server", e);
        }
    }

}
