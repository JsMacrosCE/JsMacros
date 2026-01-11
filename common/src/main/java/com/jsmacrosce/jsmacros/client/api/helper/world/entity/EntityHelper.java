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
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >1.21.5 {
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueOutput;
//?}
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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Wagyourtail
 */
@SuppressWarnings("unused")
public class EntityHelper<T extends Entity> extends BaseHelper<T> {

    protected EntityHelper(T e) {
        super(e);
    }

    /**
     * @return entity position.
     */
    public Pos3D getPos() {
        return new Pos3D(base.getX(), base.getY(), base.getZ());
    }

    /**
     * @return entity block position.
     * @since 1.6.5
     */
    public BlockPosHelper getBlockPos() {
        return new BlockPosHelper(base.blockPosition());
    }

    /**
     * @return the entity's eye position.
     * @since 1.8.4
     */
    public Pos3D getEyePos() {
        return new Pos3D(base.getEyePosition().x, base.getEyePosition().y, base.getEyePosition().z);
    }

    /**
     * @return entity chunk coordinates. Since Pos2D only has x and y fields, z coord is y.
     * @since 1.6.5
     */
    public Pos2D getChunkPos() {
        return new Pos2D(base.chunkPosition().x, base.chunkPosition().z);
    }

    /**
     * @return the {@code x} value of the entity.
     * @since 1.0.8
     */
    public double getX() {
        return base.getX();
    }

    /**
     * @return the {@code y} value of the entity.
     * @since 1.0.8
     */
    public double getY() {
        return base.getY();
    }

    /**
     * @return the {@code z} value of the entity.
     * @since 1.0.8
     */
    public double getZ() {
        return base.getZ();
    }

    /**
     * @return the current eye height offset for the entity.
     * @since 1.2.8
     */
    public double getEyeHeight() {
        return base.getEyeHeight(base.getPose());
    }

    /**
     * @return the {@code pitch} value of the entity.
     * @since 1.0.8
     */
    public float getPitch() {
        return base.getXRot();
    }

    /**
     * @return the {@code yaw} value of the entity.
     * @since 1.0.8
     */
    public float getYaw() {
        return Mth.wrapDegrees(base.getYRot());
    }

    /**
     * @return the name of the entity.
     * @since 1.0.8 [citation needed], returned string until 1.6.4
     */
    public TextHelper getName() {
        return TextHelper.wrap(base.getName());
    }

    /**
     * @return the type of the entity.
     */
    @DocletReplaceReturn("EntityId")
    public String getType() {
        return EntityType.getKey(base.getType()).toString();
    }

    /**
     * checks if this entity type equals to any of the specified types<br>
     * @since 1.9.0
     */
    @DocletReplaceTypeParams("E extends CanOmitNamespace<EntityId>")
    @DocletReplaceParams("...anyOf: JavaVarArgs<E>")
    @DocletReplaceReturn("this is EntityTypeFromId<E>")
    public boolean is(String ...types) {
        return Arrays.stream(types).map(RegistryHelper::parseNameSpace).anyMatch(getType()::equals);
    }

    /**
     * @return if the entity has the glowing effect.
     * @since 1.1.9
     */
    public boolean isGlowing() {
        return base.isCurrentlyGlowing();
    }

    /**
     * @return if the entity is in lava.
     * @since 1.1.9
     */
    public boolean isInLava() {
        return base.isInLava();
    }

    /**
     * @return if the entity is on fire.
     * @since 1.1.9
     */
    public boolean isOnFire() {
        return base.isOnFire();
    }

    /**
     * @return {@code true} if the entity is sneaking, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSneaking() {
        return base.isShiftKeyDown();
    }

    /**
     * @return {@code true} if the entity is sprinting, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSprinting() {
        return base.isSprinting();
    }

    /**
     * @return the vehicle of the entity.
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
     * @since 1.9.0
     * @param distance
     * @return
     */
    @Nullable
    public EntityHelper<?> rayTraceEntity(int distance) {
        return DebugRenderer.getTargetedEntity(base, distance).map(EntityHelper::create).orElse(null);
    }

    /**
     * @return the entity passengers.
     * @since 1.1.8 [citation needed]
     */
    @Nullable
    public List<EntityHelper<?>> getPassengers() {
        List<EntityHelper<?>> entities = base.getPassengers().stream().map(EntityHelper::create).collect(Collectors.toList());
        return entities.size() == 0 ? null : entities;

    }

    /**
     * @return
     * @since 1.2.8, was a {@link String} until 1.5.0
     */
    public NBTElementHelper.NBTCompoundHelper getNBT() {
        CompoundTag nbt = new CompoundTag();
        //? if >1.21.5 {
        ValueOutput view = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                Objects.requireNonNull(Minecraft.getInstance().getConnection()).registryAccess()
        );
        base.saveWithoutId(view);
        CompoundTag result = ((TagValueOutput) view).buildResult();
        //?} else {
        /*base.saveWithoutId(nbt);
        *///?}

        return NBTElementHelper.wrapCompound(nbt);
    }

    /**
     * @param name
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
     * sets the name to always display
     *
     * @param b
     * @since 1.8.0
     */
    public EntityHelper<T> setCustomNameVisible(boolean b) {
        base.setCustomNameVisible(b);
        return this;
    }

    /**
     * @param color
     */
    public EntityHelper<T> setGlowingColor(int color) {
        ((IMixinEntity) base).jsmacros_setGlowingColor(color);
        return this;
    }

    /**
     *
     */
    public EntityHelper<T> resetGlowingColor() {
        ((IMixinEntity) base).jsmacros_resetColor();
        return this;
    }

    /**
     * warning: affected by setGlowingColor
     *
     * @return glow color
     * @since 1.8.2
     */
    public int getGlowingColor() {
        return base.getTeamColor();
    }

    /**
     * Sets whether the entity is glowing.
     *
     * @param val
     * @return
     * @since 1.1.9
     */
    public EntityHelper<T> setGlowing(boolean val) {
        ((IMixinEntity) base).jsmacros_setForceGlowing(val ? 2 : 0);
        return this;
    }

    /**
     * reset the glowing effect to proper value.
     *
     * @return
     * @since 1.6.3
     */
    public EntityHelper<T> resetGlowing() {
        ((IMixinEntity) base).jsmacros_setForceGlowing(1);
        return this;
    }

    /**
     * Checks if the entity is still alive.
     *
     * @return
     * @since 1.2.8
     */
    public boolean isAlive() {
        return base.isAlive();
    }

    /**
     * @return UUID of the entity, random* if not a player, otherwise the player's uuid.
     * @since 1.6.5
     */
    public String getUUID() {
        return base.getUUID().toString();
    }

    /**
     * @return the maximum amount of air this entity can have.
     * @since 1.8.4
     */
    public int getMaxAir() {
        return base.getMaxAirSupply();
    }

    /**
     * @return the amount of air this entity has.
     * @since 1.8.4
     */
    public int getAir() {
        return base.getAirSupply();
    }

    /**
     * @return this entity's current speed in blocks per second.
     * @since 1.8.4
     */
    public double getSpeed() {
        double dx = Math.abs(base.getX() - base.xo);
        double dz = Math.abs(base.getZ() - base.zo);
        return Math.sqrt(dx * dx + dz * dz) * 20;
    }

    /**
     * @return the direction the entity is facing, rounded to the nearest 45 degrees.
     * @since 1.8.4
     */
    public DirectionHelper getFacingDirection() {
        return new DirectionHelper(base.getDirection());
    }

    /**
     * @return the distance between this entity and the specified one.
     * @since 1.8.4
     */
    public float distanceTo(EntityHelper<?> entity) {
        return base.distanceTo(entity.getRaw());
    }

    /**
     * @return the distance between this entity and the specified position.
     * @since 1.8.4
     */
    public double distanceTo(BlockPosHelper pos) {
        return Math.sqrt(pos.getRaw().distToCenterSqr(base.position()));
    }

    /**
     * @return the distance between this entity and the specified position.
     * @since 1.8.4
     */
    public double distanceTo(Pos3D pos) {
        return Math.sqrt(base.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()));
    }

    /**
     * @return the distance between this entity and the specified position.
     * @since 1.8.4
     */
    public double distanceTo(double x, double y, double z) {
        return Math.sqrt(base.distanceToSqr(x, y, z));
    }

    /**
     * @return the velocity vector.
     * @since 1.8.4
     */
    public Pos3D getVelocity() {
        return new Pos3D(base.getDeltaMovement().x, base.getDeltaMovement().y, base.getDeltaMovement().z);
    }

    /**
     * @return the chunk helper for the chunk this entity is in.
     * @since 1.8.4
     */
    public ChunkHelper getChunk() {
        return new ChunkHelper(base.level().getChunk(base.blockPosition()));
    }

    /**
     * @return the name of the biome this entity is in.
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
     * mostly for internal use.
     *
     * @param e mc entity.
     * @return correct subclass of this.
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
     * @return cast of this entity helper (mainly for typescript)
     * @since 1.6.3
     */
    public ClientPlayerEntityHelper<?> asClientPlayer() {
        return (ClientPlayerEntityHelper<?>) this;
    }

    /**
     * @return cast of this entity helper (mainly for typescript)
     * @since 1.6.3
     */
    public PlayerEntityHelper<?> asPlayer() {
        return (PlayerEntityHelper<?>) this;
    }

    /**
     * @return cast of this entity helper (mainly for typescript)
     * @since 1.6.3
     */
    public VillagerEntityHelper asVillager() {
        return (VillagerEntityHelper) this;
    }

    /**
     * @return cast of this entity helper (mainly for typescript)
     * @since 1.6.3
     */
    public MerchantEntityHelper<?> asMerchant() {
        return (MerchantEntityHelper<?>) this;
    }

    /**
     * @return cast of this entity helper (mainly for typescript)
     * @since 1.6.3
     */
    public LivingEntityHelper<?> asLiving() {
        return (LivingEntityHelper<?>) this;
    }

    /**
     * @return this helper as an animal entity helper (mainly for typescript).
     * @since 1.8.4
     */
    public LivingEntityHelper<?> asAnimal() {
        return (AnimalEntityHelper<?>) this;
    }

    /**
     * @return cast of this entity helper (mainly for typescript)
     * @since 1.6.3
     */
    public ItemEntityHelper asItem() {
        return (ItemEntityHelper) this;
    }

    /**
     * @return the entity as a server entity if an integrated server is running and {@code null} otherwise.
     * @since 1.8.4
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
