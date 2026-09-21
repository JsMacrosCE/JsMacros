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
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.doclet.DocletReplaceTypeParams;
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
import com.jsmacrosce.jsmacros.util.ChunkPosUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

//? if >=1.21.11 {
/*import net.minecraft.world.entity.vehicle.boat.Boat;
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
import net.minecraft.world.entity.vehicle.Boat;
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
 * Wraps a Minecraft {@link Entity} and exposes convenience methods for reading
 * entity state, querying world-related information, and accessing specialized
 * helper types.
 *
 * @param <T> the wrapped entity type
 * @author Wagyourtail
 */
@SuppressWarnings("unused")
public class EntityHelper<T extends Entity> extends BaseHelper<T> {

    protected EntityHelper(T e) {
        super(e);
    }

    /**
     * Returns the current position of the entity.
     *
     * @return a new {@link Pos3D} representing the entity's current world position
     */
    public Pos3D getPos() {
        return new Pos3D(base.getX(), base.getY(), base.getZ());
    }

    /**
     * Returns the interpolated position of the entity for the given render tick.
     *
     * @param partialTicks the fractional tick value used for interpolation between
     *                     the previous and current tick (0–1)
     * @return a new {@link Pos3D} representing the interpolated entity position
     */
    public Pos3D getPos(float partialTicks) {
        return new Pos3D(base.getPosition(partialTicks));
    }

    /**
     * Returns the entity's current block position.
     *
     * @return a helper representing the entity's current block coordinates
     * @since 1.6.5
     */
    public BlockPosHelper getBlockPos() {
        return new BlockPosHelper(base.blockPosition());
    }

    /**
     * Returns the entity's current eye position.
     *
     * @return a new {@link Pos3D} representing the entity's eye position
     * @since 1.8.4
     */
    public Pos3D getEyePos() {
        return new Pos3D(base.getEyePosition().x, base.getEyePosition().y, base.getEyePosition().z);
    }

    /**
     * Returns the chunk coordinates containing this entity.
     *
     * @return a new {@link Pos2D} containing the chunk x and z coordinates, where
     *         the z coordinate is stored in the {@code y} field
     * @since 1.6.5
     */
    public Pos2D getChunkPos() {
        return new Pos2D(ChunkPosUtil.x(base.chunkPosition()), ChunkPosUtil.z(base.chunkPosition()));
    }

    /**
     * Returns the entity's current x-coordinate.
     *
     * @return the {@code x} coordinate of the entity
     * @since 1.0.8
     */
    public double getX() {
        return base.getX();
    }

    /**
     * Returns the entity's current y-coordinate.
     *
     * @return the {@code y} coordinate of the entity
     * @since 1.0.8
     */
    public double getY() {
        return base.getY();
    }

    /**
     * Returns the entity's current z-coordinate.
     *
     * @return the {@code z} coordinate of the entity
     * @since 1.0.8
     */
    public double getZ() {
        return base.getZ();
    }

    /**
     * Returns the entity's current eye height offset for its active pose.
     *
     * @return the vertical offset from the entity's position to its eye position
     * @since 1.2.8
     */
    public double getEyeHeight() {
        return base.getEyeHeight(base.getPose());
    }

    /**
     * Returns the entity's pitch rotation.
     *
     * @return the {@code pitch} value of the entity in degrees
     * @since 1.0.8
     */
    public float getPitch() {
        return base.getXRot();
    }

    /**
     * Returns the entity's yaw rotation.
     *
     * @return the wrapped {@code yaw} value of the entity in degrees
     * @since 1.0.8
     */
    public float getYaw() {
        return Mth.wrapDegrees(base.getYRot());
    }

    /**
     * Returns the display name of the entity.
     *
     * @return the entity's name as a {@link TextHelper}
     * @since 1.0.8 [citation needed], returned string until 1.6.4
     */
    public TextHelper getName() {
        return TextHelper.wrap(base.getName());
    }

    /**
     * Returns the registry ID of the entity type.
     *
     * @return the namespaced ID of the entity type
     */
    @DocletReplaceReturn("EntityId")
    public String getType() {
        return EntityType.getKey(base.getType()).toString();
    }

    /**
     * Checks whether this entity's type matches any of the given type IDs.
     *
     * @param types one or more entity type IDs to compare against
     * @return {@code true} if this entity matches at least one provided type,
     *         {@code false} otherwise
     * @since 1.9.0
     */
    @DocletReplaceTypeParams("E extends CanOmitNamespace<EntityId>")
    @DocletReplaceParams("...anyOf: JavaVarArgs<E>")
    @DocletReplaceReturn("this is EntityTypeFromId<E>")
    public boolean is(String ...types) {
        return Arrays.stream(types).map(RegistryHelper::parseNameSpace).anyMatch(getType()::equals);
    }

    /**
     * Checks whether the entity is currently glowing.
     *
     * @return {@code true} if the entity has the glowing effect, {@code false}
     *         otherwise
     * @since 1.1.9
     */
    public boolean isGlowing() {
        return base.isCurrentlyGlowing();
    }

    /**
     * Checks whether the entity is currently in lava.
     *
     * @return {@code true} if the entity is in lava, {@code false} otherwise
     * @since 1.1.9
     */
    public boolean isInLava() {
        return base.isInLava();
    }

    /**
     * Checks whether the entity is currently on fire.
     *
     * @return {@code true} if the entity is on fire, {@code false} otherwise
     * @since 1.1.9
     */
    public boolean isOnFire() {
        return base.isOnFire();
    }

    /**
     * Checks whether the entity is sneaking.
     *
     * @return {@code true} if the entity is sneaking, {@code false} otherwise
     * @since 1.8.4
     */
    public boolean isSneaking() {
        return base.isShiftKeyDown();
    }

    /**
     * Checks whether the entity is sprinting.
     *
     * @return {@code true} if the entity is sprinting, {@code false} otherwise
     * @since 1.8.4
     */
    public boolean isSprinting() {
        return base.isSprinting();
    }

    /**
     * Returns the vehicle this entity is riding.
     *
     * @return a helper for the entity's vehicle, or {@code null} if the entity is
     *         not riding anything
     * @since 1.1.8 [citation needed]
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
     * Ray traces from the entity's viewpoint and returns the first block hit.
     *
     * @param distance the maximum trace distance in blocks
     * @param fluid whether fluids should be considered hittable
     * @return a helper for the hit block, or {@code null} if no block was hit
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
     * Ray traces from the entity's viewpoint and returns the first entity hit.
     *
     * @param distance the maximum trace distance in blocks
     * @return a helper for the targeted entity, or {@code null} if no entity was hit
     * @since 1.9.0
     */
    @Nullable
    public EntityHelper<?> rayTraceEntity(int distance) {
        return DebugRenderer.getTargetedEntity(base, distance).map(EntityHelper::create).orElse(null);
    }

    /**
     * Returns the entities riding this entity.
     *
     * @return a list of passenger helpers, or {@code null} if the entity has no
     *         passengers
     * @since 1.1.8 [citation needed]
     */
    @Nullable
    public List<EntityHelper<?>> getPassengers() {
        List<EntityHelper<?>> entities = base.getPassengers().stream().map(EntityHelper::create).collect(Collectors.toList());
        return entities.size() == 0 ? null : entities;

    }

    /**
     * Serializes the entity into NBT without its entity ID.
     *
     * @return the entity's NBT data as a compound helper
     * @since 1.2.8, was a {@link String} until 1.5.0
     */
    public NBTElementHelper.NBTCompoundHelper getNBT() {
        //? if >1.21.5 {
        ValueOutput view = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                player.registryAccess()
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
     * Sets the entity's custom name.
     *
     * @param name the name to set, or {@code null} to clear the custom name
     * @return this helper instance
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
     * Sets whether the entity's custom name should always be visible.
     *
     * @param b {@code true} to always show the custom name, {@code false} to use
     *          normal visibility rules
     * @return this helper instance
     * @since 1.8.0
     */
    public EntityHelper<T> setCustomNameVisible(boolean b) {
        base.setCustomNameVisible(b);
        return this;
    }

    /**
     * Overrides the entity's glowing outline color.
     *
     * @param color the ARGB or packed color value to apply
     * @return this helper instance
     */
    public EntityHelper<T> setGlowingColor(int color) {
        ((IMixinEntity) base).jsmacros_setGlowingColor(color);
        return this;
    }

    /**
     * Clears any glowing color override previously applied with
     * {@link #setGlowingColor(int)}.
     *
     * @return this helper instance
     */
    public EntityHelper<T> resetGlowingColor() {
        ((IMixinEntity) base).jsmacros_resetColor();
        return this;
    }

    /**
     * Returns the entity's current glowing color.
     *
     * <p>This value may be affected by {@link #setGlowingColor(int)}.</p>
     *
     * @return the current glow color value
     * @since 1.8.2
     */
    public int getGlowingColor() {
        return base.getTeamColor();
    }

    /**
     * Sets whether the entity should be forced into the glowing state.
     *
     * @param val {@code true} to force glowing, {@code false} to disable the forced
     *            glowing state
     * @return this helper instance
     * @since 1.1.9
     */
    public EntityHelper<T> setGlowing(boolean val) {
        ((IMixinEntity) base).jsmacros_setForceGlowing(val ? 2 : 0);
        return this;
    }

    /**
     * Resets the entity's glowing state to the normal game-controlled value.
     *
     * @return this helper instance
     * @since 1.6.3
     */
    public EntityHelper<T> resetGlowing() {
        ((IMixinEntity) base).jsmacros_setForceGlowing(1);
        return this;
    }

    /**
     * Checks whether the entity is still alive.
     *
     * @return {@code true} if the entity is alive, {@code false} otherwise
     * @since 1.2.8
     */
    public boolean isAlive() {
        return base.isAlive();
    }

    /**
     * Returns the UUID of the entity.
     *
     * @return the entity UUID as a string
     * @since 1.6.5
     */
    public String getUUID() {
        return base.getUUID().toString();
    }

    /**
     * Returns the maximum amount of air the entity can store.
     *
     * @return the entity's maximum air supply
     * @since 1.8.4
     */
    public int getMaxAir() {
        return base.getMaxAirSupply();
    }

    /**
     * Returns the entity's current air supply.
     *
     * @return the amount of air currently available to the entity
     * @since 1.8.4
     */
    public int getAir() {
        return base.getAirSupply();
    }

    /**
     * Returns the entity's approximate horizontal speed in blocks per second.
     *
     * @return the current horizontal speed based on the previous tick position
     * @since 1.8.4
     */
    public double getSpeed() {
        double dx = Math.abs(base.getX() - base.xo);
        double dz = Math.abs(base.getZ() - base.zo);
        return Math.sqrt(dx * dx + dz * dz) * 20;
    }

    /**
     * Returns the direction the entity is facing.
     *
     * @return a helper for the entity's facing direction, rounded to the nearest
     *         cardinal/intercardinal direction
     * @since 1.8.4
     */
    public DirectionHelper getFacingDirection() {
        return new DirectionHelper(base.getDirection());
    }

    /**
     * Returns the distance from this entity to another entity.
     *
     * @param entity the entity to measure distance to
     * @return the distance between the two entities in blocks
     * @since 1.8.4
     */
    public float distanceTo(EntityHelper<?> entity) {
        return base.distanceTo(entity.getRaw());
    }

    /**
     * Returns the distance from this entity to the given block position.
     *
     * @param pos the position to measure distance to
     * @return the distance between this entity and the center of the given block
     * @since 1.8.4
     */
    public double distanceTo(BlockPosHelper pos) {
        return Math.sqrt(pos.getRaw().distToCenterSqr(base.position()));
    }

    /**
     * Returns the distance from this entity to the given world position.
     *
     * @param pos the position to measure distance to
     * @return the distance between this entity and the given position in blocks
     * @since 1.8.4
     */
    public double distanceTo(Pos3D pos) {
        return Math.sqrt(base.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()));
    }

    /**
     * Returns the distance from this entity to the given coordinates.
     *
     * @param x the target x-coordinate
     * @param y the target y-coordinate
     * @param z the target z-coordinate
     * @return the distance between this entity and the given coordinates in blocks
     * @since 1.8.4
     */
    public double distanceTo(double x, double y, double z) {
        return Math.sqrt(base.distanceToSqr(x, y, z));
    }

    /**
     * Returns the entity's current velocity vector.
     *
     * @return a new {@link Pos3D} representing the entity's delta movement
     * @since 1.8.4
     */
    public Pos3D getVelocity() {
        return new Pos3D(base.getDeltaMovement().x, base.getDeltaMovement().y, base.getDeltaMovement().z);
    }

    /**
     * Returns the chunk currently containing this entity.
     *
     * @return a helper for the entity's current chunk
     * @since 1.8.4
     */
    public ChunkHelper getChunk() {
        return new ChunkHelper(base.level().getChunk(base.blockPosition()));
    }

    /**
     * Returns the biome registry ID at the entity's current position.
     *
     * @return the namespaced biome ID for the biome containing this entity
     * @since 1.8.4
     */
    @DocletReplaceReturn("Biome")
    public String getBiome() {
        return Minecraft.getInstance().level.registryAccess().lookupOrThrow(Registries.BIOME).getKey(Minecraft.getInstance().level.getBiome(base.blockPosition()).value()).toString();
    }

    @Override
    public String toString() {
        return String.format("%s:{\"name\": \"%s\", \"type\": \"%s\"}", getClass().getSimpleName(), this.getName(), this.getType());
    }

    /**
     * Creates the most specific helper wrapper available for the given entity.
     *
     * <p>This is primarily intended for internal use when converting raw Minecraft
     * entities into their corresponding helper types.</p>
     *
     * @param e the Minecraft entity to wrap
     * @return the most specific helper implementation for the given entity
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
        if (e instanceof Boat) {
            return new BoatEntityHelper(((Boat) e));
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
     * Casts this helper to a client player helper.
     *
     * <p>This is primarily intended for TypeScript-facing APIs.</p>
     *
     * @return this helper cast to {@link ClientPlayerEntityHelper}
     * @since 1.6.3
     */
    public ClientPlayerEntityHelper<?> asClientPlayer() {
        return (ClientPlayerEntityHelper<?>) this;
    }

    /**
     * Casts this helper to a player helper.
     *
     * <p>This is primarily intended for TypeScript-facing APIs.</p>
     *
     * @return this helper cast to {@link PlayerEntityHelper}
     * @since 1.6.3
     */
    public PlayerEntityHelper<?> asPlayer() {
        return (PlayerEntityHelper<?>) this;
    }

    /**
     * Casts this helper to a villager helper.
     *
     * <p>This is primarily intended for TypeScript-facing APIs.</p>
     *
     * @return this helper cast to {@link VillagerEntityHelper}
     * @since 1.6.3
     */
    public VillagerEntityHelper asVillager() {
        return (VillagerEntityHelper) this;
    }

    /**
     * Casts this helper to a merchant helper.
     *
     * <p>This is primarily intended for TypeScript-facing APIs.</p>
     *
     * @return this helper cast to {@link MerchantEntityHelper}
     * @since 1.6.3
     */
    public MerchantEntityHelper<?> asMerchant() {
        return (MerchantEntityHelper<?>) this;
    }

    /**
     * Casts this helper to a living entity helper.
     *
     * <p>This is primarily intended for TypeScript-facing APIs.</p>
     *
     * @return this helper cast to {@link LivingEntityHelper}
     * @since 1.6.3
     */
    public LivingEntityHelper<?> asLiving() {
        return (LivingEntityHelper<?>) this;
    }

    /**
     * Casts this helper to an animal entity helper.
     *
     * <p>This is primarily intended for TypeScript-facing APIs.</p>
     *
     * @return this helper cast to {@link AnimalEntityHelper}
     * @since 1.8.4
     */
    public LivingEntityHelper<?> asAnimal() {
        return (AnimalEntityHelper<?>) this;
    }

    /**
     * Casts this helper to an item entity helper.
     *
     * <p>This is primarily intended for TypeScript-facing APIs.</p>
     *
     * @return this helper cast to {@link ItemEntityHelper}
     * @since 1.6.3
     */
    public ItemEntityHelper asItem() {
        return (ItemEntityHelper) this;
    }

    /**
     * Returns this entity as its server-side counterpart when available.
     *
     * @return the matching server-side entity helper if an integrated server is
     *         running, or {@code null} otherwise
     * @since 1.8.4
     * @throws UnsupportedOperationException always, because this is not currently
     *                                       supported in the client environment
     */
    @Nullable
    public EntityHelper<?> asServerEntity() {
        // TODO: Implement server entity retrieval on integrated server from client
        throw new UnsupportedOperationException("asServerEntity is not supported in client environment.");
        /*Minecraft client = Minecraft.getInstance();
        if (!client.hasSingleplayerServer()) {
            return null;
        }
        Entity entity = client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID()).serverLevel().getEntity(base.getUUID());
        if (entity == null) {
            return null;
        } else {
            return create(entity);
        }*/
    }

}
