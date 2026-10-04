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
 * anything in the world: a mob, a player, a dropped item, an arrow, a piece of furniture.
 * It is the base every other entity helper in this package extends, and it is what a
 * plain list of entities comes back as.
 * <p>
 * Most of what is here is a read on the entity as the client currently believes it to
 * be. The world belongs to the server, so a position or a rotation read through one of
 * these is the last the client was told rather than a live reading, and a second read a
 * tick later can differ. The exceptions are the handful of setters, which write to the
 * client's own copy of the entity.
 * <p>
 * An entity is wrapped as the most particular helper that fits it, so a list from
 * {@code World.getEntities()} holds the right type for each rather than plain ones.
 * Narrowing a helper to something more particular is a cast: {@code asLiving()},
 * {@code asPlayer()}, {@code asItem()} and the rest answer with the right type or
 * throw, and a script that wants to try them needs a type check first.
 * <p>
 * A helper is a wrapper rather than a copy, so two helpers on the same entity are two
 * views of one entity and a change through either is a change to both.
 * example:
     * <pre>
 * // the nearest few things in the world, with their type and position
 * const nearby = World.getEntities(10);
 * if (nearby !== null) {
 *   for (const entity of nearby) {
 *     const pos = entity.getPos();
 *     Chat.log(`${entity.getType()} at ${pos.x.toFixed(1)}, `
 *       + `${pos.y.toFixed(1)}, ${pos.z.toFixed(1)}`);
 *   }
 * }
 *
 * // a type check is a cast that can fail, so it is done in a try
 * for (const entity of nearby) {
 *   try {
 *     const living = entity.asLiving();
 *     Chat.log(`alive on ${living.getHealth()}`);
 *   } catch (e) {
 *     // not a living entity, which is most of what is in the world
 *   }
 * }
 * </pre>
 *
 * @author Wagyourtail
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class EntityHelper<T extends Entity> extends BaseHelper<T> {

    protected EntityHelper(T e) {
        super(e);
    }

    /**
     * where the entity is right now, as block coordinates rather than as a chunk
     * position, and read at the moment of the call. The position belongs to the server,
     * so this is the last position the client was sent rather than a live one.
     * <p>
     * A position is a value rather than a handle: writing to the result does not move
     * the entity, and there is a setter for that.
     * example:
     * <pre>
     * const mobs = World.getEntities(10);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     Chat.log(`a ${entity.getType()} at ${entity.getPos()}`);
     *   }
     * }
     * </pre>
     *
     * @return entity position.
     */
    public Pos3D getPos() {
        return new Pos3D(base.getX(), base.getY(), base.getZ());
    }

    /**
     * where the entity is drawn rather than where it is, worked out by interpolating
     * between the position it had last tick and the one it has now.
     * <p>
     * The argument is how far through the current tick to sit, from nothing at all up to
     * but not including the whole tick. A reading is taken twenty times a second while
     * the game is drawing, and this is the one to use for anything that has to match
     * what the player sees: a drawn line, a name tag, an overlay. The position of the
     * world itself is {@link #getPos()} with no argument.
     * <p>
     * A partial tick of zero gives the position the entity had at the start of the tick
     * and one gives the position it has now, so the two sit at the ends of the range
     * rather than a tick apart being an error.
     * example:
     * <pre>
     * const draw = Hud.createDraw2D();
     * draw.register();
     * const mobs = World.getEntities(10);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     // half way through the tick, which is where the game is drawing it
     *     const drawn = entity.getPos(0.5);
     *     Chat.log(`drawn at ${drawn}, actually at ${entity.getPos()}`);
     *   }
     * }
     * </pre>
     *
     * @param partialTicks the fraction of the current tick to interpolate.
     * @return interpolated entity position.
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
     * the entity's position rounded down to whole blocks, which is the block the entity
     * is standing in rather than the point inside it. The y is the one to watch: a
     * player standing on a block has the block below as their block position, so this is
     * the floor rather than the feet.
     * <p>
     * It is rounded down rather than to the nearest, so a position just inside a block
     * and a position just inside the next one give different answers either side of the
     * boundary.
     * example:
     * <pre>
     * const mobs = World.getEntities(10);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     // the block rather than the point, so a standing player gives the floor
     *     const block = entity.getBlockPos();
     *     Chat.log(`${entity.getType()} is in the block at ${block.getX()}, `
     *       + `${block.getY()}, ${block.getZ()}`);
     *   }
     * }
     * </pre>
     *
     * @return entity block position.
     * @since 1.6.5
     */
    public BlockPosHelper getBlockPos() {
        return new BlockPosHelper(base.blockPosition());
    }

    /**
     * where the entity is looking from, which is its position raised by its own eye
     * height. The height is not a constant: it depends on the entity and on the pose it
     * is in, so a crouching entity's eyes are lower and a baby mob's are lower still.
     * <p>
     * This is the point the game's own line of sight and picking work from, so a script
     * that wants to match what the player can see wants this rather than
     * {@link #getPos()}.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // the eyes, and how far they sit above the feet
     *   Chat.log(`eyes at ${player.getEyePos()}, `
     *     + `${player.getEyeHeight()} above the feet`);
     * }
     * </pre>
     *
     * @return the entity's eye position.
     * @since 1.8.4
     */
    public Pos3D getEyePos() {
        return new Pos3D(base.getEyePosition().x, base.getEyePosition().y, base.getEyePosition().z);
    }

    /**
     * entity chunk coordinates, which is the block position divided by sixteen. Since
     * Pos2D only has x and y fields, z coord is y, so the world z is read from the
     * {@code y} field of the result.
     * <p>
     * This is the chunk the entity is in rather than the one it is standing on: an
     * entity at the boundary belongs to the chunk above, so the answer changes sixteen
     * blocks at a time rather than every block.
     * example:
     * <pre>
     * const mobs = World.getEntities(20);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     const chunk = entity.getChunkPos();
     *     // the world z is the y field, because a two dimensional position has no z
     *     Chat.log(`chunk ${chunk.getX()}, ${chunk.getY()}`);
     *   }
     * }
     * </pre>
     *
     * @return entity chunk coordinates. Since Pos2D only has x and y fields, z coord is y.
     * @since 1.6.5
     */
    public Pos2D getChunkPos() {
        ChunkPos pos = base.chunkPosition();
        return new Pos2D(ChunkPosCompat.x(pos), ChunkPosCompat.z(pos));
    }

    /**
     * the {@code x} value of the entity, read at the moment of the call. It is a plain
     * number rather than a whole block, so an entity between two blocks gives a
     * fraction; {@link #getBlockPos()} is the rounded-down form.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   Chat.log(`x is ${player.getX()}, in block ${player.getBlockPos().getX()}`);
     * }
     * </pre>
     *
     * @return the {@code x} value of the entity.
     * @since 1.0.8
     */
    public double getX() {
        return base.getX();
    }

    /**
     * the {@code y} value of the entity, which is its feet rather than its eyes, so it
     * is the height of the bottom of the entity and not of the point it is looking from.
     * {@link #getEyePos()} is the other one.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   Chat.log(`feet at y ${player.getY()}, eyes at y ${player.getEyePos().y}`);
     * }
     * </pre>
     *
     * @return the {@code y} value of the entity.
     * @since 1.0.8
     */
    public double getY() {
        return base.getY();
    }

    /**
     * the {@code z} value of the entity, read at the moment of the call and not rounded
     * to a block.
     * example:
     * <pre>
     * const mobs = World.getEntities(10);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     Chat.log(`${entity.getType()} at z ${entity.getZ()}`);
     *   }
     * }
     * </pre>
     *
     * @return the {@code z} value of the entity.
     * @since 1.0.8
     */
    public double getZ() {
        return base.getZ();
    }

    /**
     * how far above its feet the entity's eyes are, which is an offset rather than a
     * position. It is worked out for the pose the entity is in right now, so a crouching
     * or sleeping entity gives a smaller number than a standing one and the value
     * changes as the pose does.
     * <p>
     * Adding this to {@link #getY()} gives {@link #getEyePos()}.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const eyes = player.getEyePos();
     *   const feet = player.getPos();
     *   // the two differ by exactly the eye height
     *   Chat.log(`eyes are ${eyes.y - feet.y} above the feet`);
     * }
     * </pre>
     *
     * @return the current eye height offset for the entity.
     * @since 1.2.8
     */
    public double getEyeHeight() {
        return base.getEyeHeight(base.getPose());
    }

    /**
     * the {@code pitch} value of the entity, which is how far it is looking up or down.
     * It is read straight off the entity rather than wrapped, so it runs from minus
     * ninety at straight up to ninety at straight down, and a value in between is the
     * angle off the horizontal.
     * <p>
     * It is not corrected for wrapping the way {@link #getYaw()} is, so a value outside
     * that range is possible here.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // ninety is straight down, minus ninety is straight up
     *   Chat.log(`pitch ${player.getPitch()}, yaw ${player.getYaw()}`);
     * }
     * </pre>
     *
     * @return the {@code pitch} value of the entity.
     * @since 1.0.8
     */
    public float getPitch() {
        return base.getXRot();
    }

    /**
     * the {@code yaw} value of the entity, which is the compass direction it faces, and
     * it is put through the game's own wrapping first. That means the number is always
     * in the same range whatever the entity's own figure is, so turning a long way round
     * gives a small change rather than a large one.
     * <p>
     * The game measures yaw from due south rather than from due north, so zero faces
     * south, ninety faces west, one hundred and eighty faces north and two hundred and
     * seventy faces east. Those are the headings the game itself works in rather than
     * the figures that come back out of the wrapping: the range runs from
     * {@code -180} up to but not including {@code 180}, and the far end is moved rather
     * than the near one, so a heading of one hundred and eighty is read back as minus
     * one hundred and eighty and a heading of two hundred and seventy as minus ninety.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // the compass direction rather than the raw figure
     *   Chat.log(`facing ${player.getFacingDirection().getName()}`);
     * }
     * </pre>
     *
     * @return the {@code yaw} value of the entity.
     * @since 1.0.8
     */
    public float getYaw() {
        return Mth.wrapDegrees(base.getYRot());
    }

    /**
     * the name of the entity as it is shown, which is a text wrapper rather than a plain
     * string, so any styling and any custom name the entity has been given come with it.
     * Returned a {@code String} before 1.6.4.
     * <p>
     * This is the name in the world, which is not the same as the account name of a
     * player: an entity can be renamed, and a mob is named after what it is rather than
     * after an account. A player entity has its own reader for the account name.
     * example:
     * <pre>
     * const mobs = World.getEntities(10);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     // getString() gives the text on its own
     *     Chat.log(`a ${entity.getType()} called ${entity.getName().getString()}`);
     *   }
     * }
     * </pre>
     *
     * @return the name of the entity.
     * @since 1.0.2
     */
    public TextHelper getName() {
        return TextHelper.wrap(base.getName());
    }

    /**
     * the type of the entity, which is the registry id of the kind of thing it is rather
     * than anything about this individual: every zombie gives the same one, and it is
     * {@code minecraft:zombie} with the namespace on the front.
     * <p>
     * This is the string {@link #is(String...)} matches against and the one a script
     * compares, rather than a name that changes with a rename or a breeding.
     * example:
     * <pre>
     * const mobs = World.getEntities(20);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     // the kind, which is the same for every one of them
     *     Chat.log(`${entity.getName().getString()} is a ${entity.getType()}`);
     *   }
     * }
     * </pre>
     *
     * @return the type of the entity.
     */
    @DocletReplaceReturn("EntityId")
    public String getType() {
        return EntityType.getKey(base.getType()).toString();
    }

    /**
     * checks if this entity type equals to any of the specified types
     * <p>
     * This is a check on {@link #getType()}, so what is being matched is the kind of
     * thing rather than anything about this individual. Several types can be given at
     * once, in which case the answer is whether the entity is any of them rather than
     * all of them. Each is an entity id and the namespace may be left off, in which case
     * it is read as {@code minecraft}.
     * <p>
     * Giving none of them answers {@code false}, since there is nothing to match.
     * example:
     * <pre>
     * const mobs = World.getEntities(20);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     // any of the three, rather than all of them
     *     if (entity.is("zombie", "skeleton", "creeper")) {
     *       Chat.log(`a hostile at ${entity.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
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
     * whether the entity is drawn with the glowing outline, which is what the game uses
     * to make something stand out. The entity can be glowing for more than one reason:
     * a team can set it, an effect can, a command can, or a script can through
     * {@link #setGlowing(boolean)}, and this does not say which.
     * <p>
     * The colour of the outline is a separate reading, on
     * {@link #getGlowingColor()}.
     * example:
     * <pre>
     * const mobs = World.getEntities(20);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     if (entity.isGlowing()) {
     *       Chat.log(`glowing at ${entity.getPos()}, `
     *         + `colour 0x${entity.getGlowingColor().toString(16)}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return if the entity has the glowing effect.
     * @since 1.1.9
     */
    public boolean isGlowing() {
        return base.isCurrentlyGlowing();
    }

    /**
     * whether the entity is inside a lava block, which is a different thing from being
     * on fire: an entity can be burning for a while after it has left the lava, and
     * {@link #isOnFire()} is what says that.
     * example:
     * <pre>
     * const mobs = World.getEntities(20, "pig", "cow", "sheep");
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     if (entity.isInLava()) {
     *       Chat.log(`a ${entity.getType()} is standing in lava at ${entity.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return if the entity is in lava.
     * @since 1.1.9
     */
    public boolean isInLava() {
        return base.isInLava();
    }

    /**
     * whether the entity is burning, which covers both its own fire and a fire that is
     * set on it. It stays {@code true} for a while after whatever set it burning is gone,
     * so an entity that has walked out of lava still answers this for a few seconds
     * afterwards.
     * example:
     * <pre>
     * const mobs = World.getEntities(20);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     if (entity.isOnFire()) {
     *       Chat.log(`burning at ${entity.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return if the entity is on fire.
     * @since 1.1.9
     */
    public boolean isOnFire() {
        return base.isOnFire();
    }

    /**
     * {@code true} if the entity is sneaking, {@code false} otherwise.
     * <p>
     * This is whether the shift key is held rather than a check of the pose, so an
     * entity that is crouched for some other reason does not answer {@code true} and one
     * that is holding the key while lying down still does.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   if (player.isSneaking()) {
     *     // the game treats a sneaking player as being on a different row
     *     Chat.log(`sneaking, so at y ${player.getY()}`);
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the entity is sneaking, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSneaking() {
        return base.isShiftKeyDown();
    }

    /**
     * {@code true} if the entity is sprinting, {@code false} otherwise.
     * <p>
     * This is the flag rather than a measurement of speed, so an entity that is moving
     * at a sprint because something pushed it does not answer {@code true}.
     * example:
     * <pre>
     * const mobs = World.getEntities(20, "pig", "cow");
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     if (entity.isSprinting()) {
     *       // and how fast that actually is
     *       Chat.log(`a ${entity.getType()} sprinting at ${entity.getSpeed()} blocks a second`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the entity is sprinting, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSprinting() {
        return base.isSprinting();
    }

    /**
     * the vehicle the entity is in, or {@code null} when it is not in one. A boat, a
     * minecart or a horse all count, and a player riding one is in it however the
     * player got there.
     * <p>
     * This is the other direction from {@link #getPassengers()}: that is what is in the
     * entity, this is what the entity is in.
     * example:
     * <pre>
     * const mobs = World.getEntities(20);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     const vehicle = entity.getVehicle();
     *     if (vehicle !== null) {
     *       Chat.log(`a ${entity.getType()} is riding a ${vehicle.getType()}`);
     *     }
     *   }
     * }
     * </pre>
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
     * the block the entity is looking at, or {@code null} when there is nothing there.
     * <p>
     * The trace starts at the entity's own eyes and goes along the direction it is
     * facing, and the distance is how far it reaches in blocks. It stops at the first
     * block outline it meets rather than at the first solid one, so a block the entity
     * can see through is not the answer.
     * <p>
     * There are three ways this gives {@code null} rather than a block. A trace that
     * reaches its distance without meeting anything gives {@code null}. So does a trace
     * that meets a block the game treats as air. A third case is a trace that meets
     * nothing but the game still reports, which is not something a caller can tell from
     * the other two.
     * <p>
     * The {@code fluid} argument is what decides whether fluids are traced through and
     * against. With it off, a block of water is not the answer; with it on, it is.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // five blocks ahead, ignoring fluids
     *   const block = player.rayTraceBlock(5, false);
     *   if (block === null) {
     *     Chat.log("nothing in the way");
     *   } else {
     *     Chat.log(`looking at ${block.getId()} at ${block.getBlockPos()}`);
     *   }
     * }
     * </pre>
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
     * the entity the entity is looking at, or {@code null} when it is looking at
     * nothing that can be picked.
     * <p>
     * The trace goes from the entity's eyes along the direction it is facing, and the
     * distance is how far it reaches in blocks. It stops at the first thing the game
     * will let be picked, which is not every entity: a block or a display entity is not
     * pickable and a trace that meets one passes through it rather than stopping.
     * <p>
     * A trace that reaches its distance without meeting a pickable entity gives
     * {@code null}, and so does one that meets something too far away to count even
     * though it was met inside the distance.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // three blocks ahead, at the first pickable thing
     *   const target = player.rayTraceEntity(3);
     *   if (target !== null) {
     *     Chat.log(`looking at a ${target.getType()} at ${target.getPos()}`);
     *   } else {
     *     Chat.log("looking at nothing pickable");
     *   }
     * }
     * </pre>
     *
     * @param distance how far to reach in blocks
     * @return the entity the entity is looking at, or {@code null} if there is none
     * @since 1.9.0
     */
    @Nullable
    public EntityHelper<?> rayTraceEntity(int distance) {
        return DebugRenderer.getTargetedEntity(base, distance).map(EntityHelper::create).orElse(null);
    }

    /**
     * the entity passengers, or {@code null} when there are none. A boat or a minecart
     * can carry several and a horse one, so this is a list rather than a single entity.
     * <p>
     * An empty result is {@code null} rather than an empty list, so a caller has to
     * check before walking it. This is the other direction from {@link #getVehicle()}.
     * example:
     * <pre>
     * const mobs = World.getEntities(20, "boat", "minecart", "horse", "pig");
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     const riders = entity.getPassengers();
     *     if (riders !== null) {
     *       Chat.log(`a ${entity.getType()} with ${riders.size()} on it`);
     *     }
     *   }
     * }
     * </pre>
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
     * the whole entity written out as a piece of NBT, which is the form the game saves
     * and sends it in. It is a compound rather than a single value, and it is built
     * fresh on each call, so it is a reading rather than a view of the entity.
     * <p>
     * What is in it is whatever the game puts in the entity's own record, so it moves
     * with the version and is not a fixed set of keys. A name, a position, a type and
     * the entity's health are in there, and so is whatever else that kind of entity
     * keeps.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // the entity as the game would write it out
     *   Chat.log(player.getNBT().toString());
     * }
     * </pre>
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
     * gives the entity a name of its own, which is the name it is shown under rather
     * than the one its kind gives it. The name is a text wrapper, so it can be styled,
     * and it comes back out through {@link #getName()}.
     * <p>
     * Passing {@code null} takes the name off again rather than setting it to nothing,
     * and a name that is set here stays only as long as the server keeps it: the client
     * is not the one that decides what an entity is called.
     * example:
     * <pre>
     * const mobs = World.getEntities(20, "armor_stand");
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     entity.setCustomName(Chat.createTextHelperFromString("a stand"));
     *     // and off again
     *     entity.setCustomName(null);
     *   }
     * }
     * </pre>
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
     * sets whether the name is shown all the time rather than only when the pointer is
     * over the entity. A name that is not visible is still on the entity, so
     * {@link #getName()} still gives it back; this only decides whether it is drawn.
     * <p>
     * Whether the server accepts it is another matter, since a client is not the one that
     * decides what an entity is called.
     * example:
     * <pre>
     * const mobs = World.getEntities(20, "armor_stand");
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     entity.setCustomName(Chat.createTextHelperFromString("a stand"));
     *     // and shown without the pointer being on it
     *     entity.setCustomNameVisible(true);
     *     Chat.log(`now called ${entity.getName().getString()}`);
     *   }
     * }
     * </pre>
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
     * sets the colour of the glow the entity is drawn with, as a packed rgb number.
     * This is the colour {@link #getGlowingColor()} then reports, and it is worth
     * knowing that the game's own reading of that colour comes from the entity's team
     * rather than from the number set here, so the two can disagree.
     * <p>
     * A colour set here is on the client, so it is a change to how the entity is drawn
     * rather than to the entity itself.
     * example:
     * <pre>
     * const mobs = World.getEntities(20);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     // bright red, and glowing so that the colour is used at all
     *     entity.setGlowingColor(0xFFFF0000);
     *     entity.setGlowing(true);
     *   }
     * }
     * </pre>
     *
     * @param color the packed rgb colour to glow in
     * @return self for chaining.
     */
    public EntityHelper<T> setGlowingColor(int color) {
        ((IMixinEntity) base).jsmacros_setGlowingColor(color);
        return this;
    }

    /**
     * drops the glow colour set by {@link #setGlowingColor(int)}, so the entity goes
     * back to whatever colour the game works out for it on its own. This is separate
     * from {@link #resetGlowing()}, which is about whether the entity glows at all
     * rather than what colour it does.
     * example:
     * <pre>
     * const mobs = World.getEntities(20);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     entity.setGlowingColor(0xFFFF0000);
     *     // back to the game's own choice of colour
     *     entity.resetGlowingColor();
     *   }
     * }
     * </pre>
     *
     * @return self for chaining.
     */
    public EntityHelper<T> resetGlowingColor() {
        ((IMixinEntity) base).jsmacros_resetColor();
        return this;
    }

    /**
     * the colour of the glow the entity is drawn with, as a packed rgb number.
     * warning: affected by setGlowingColor
     * <p>
     * The figure comes from the entity's team rather than from a colour set on the
     * entity, so it is zero on an entity that is not glowing and a value the team chose
     * on one that is. A colour put on with {@link #setGlowingColor(int)} is what the
     * entity is drawn in, which is not the same number.
     * example:
     * <pre>
     * const mobs = World.getEntities(20);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     if (entity.isGlowing()) {
     *       Chat.log(`glow colour 0x${entity.getGlowingColor().toString(16)}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return glow color
     * @since 1.8.2
     */
    public int getGlowingColor() {
        return base.getTeamColor();
    }

    /**
     * Sets whether the entity is glowing.
     * <p>
     * This forces the outline on or off rather than writing a flag the game reads, so
     * it overrides whatever would otherwise decide: a team colour, an effect, or the
     * fact that the entity simply is not glowing. {@link #resetGlowing()} hands the
     * decision back to the game.
     * <p>
     * The change is on the client, so it is about how the entity is drawn rather than
     * about the entity itself.
     * example:
     * <pre>
     * const mobs = World.getEntities(20);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     // forced on, whatever the game would have decided
     *     entity.setGlowing(true);
     *   }
     * }
     * </pre>
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
     * reset the glowing effect to proper value.
     * <p>
     * This gives the decision back to the game, so whatever would make the entity glow
     * does so again and whatever would not stops it. It undoes a
     * {@link #setGlowing(boolean)} of either kind rather than only the one that turned
     * it on.
     * <p>
     * This is about whether the entity glows; the colour is
     * {@link #resetGlowingColor()}.
     * example:
     * <pre>
     * const mobs = World.getEntities(20);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     entity.setGlowing(true);
     *     // back to whatever the game decides on its own
     *     entity.resetGlowing();
     *   }
     * }
     * </pre>
     *
     * @return self for chaining.
     * @since 1.6.3
     */
    public EntityHelper<T> resetGlowing() {
        ((IMixinEntity) base).jsmacros_setForceGlowing(1);
        return this;
    }

    /**
     * Checks if the entity is still alive, which is the game being told the entity has
     * not been killed. An entity that has been killed answers {@code false} from the
     * moment it dies rather than being taken out of the world straight away, so a
     * corpse is still something a script can see.
     * example:
     * <pre>
     * const mobs = World.getEntities(20);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     if (!entity.isAlive()) {
     *       Chat.log(`a dead ${entity.getType()} at ${entity.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the entity has not been killed, {@code false} otherwise
     * @since 1.2.8
     */
    public boolean isAlive() {
        return base.isAlive();
    }

    /**
     * @return UUID of the entity, random* if not a player, otherwise the player's uuid.
     * <p>
     * The figure is a string rather than a uuid object. A player entity answers with the
     * uuid of the account behind it, so two entities that are the same player give the
     * same one, and a mob that is not a player answers with something generated for it
     * rather than with an account.
     * <p>
     * An entity can be removed from the world and a later one put in its place with a
     * different one, so this is the way to tell whether two things are the same entity
     * rather than the position being enough.
     * example:
     * <pre>
     * const mobs = World.getEntities(20);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     Chat.log(`${entity.getType()} is ${entity.getUUID()}`);
     *   }
     * }
     * </pre>
     *
     * @since 1.6.5
     */
    public String getUUID() {
        return base.getUUID().toString();
    }

    /**
     * the maximum amount of air this entity can have, which is the number
     * {@link #getAir()} is measured against and the one the air supply is refilled to.
     * <p>
     * It is the same figure whatever the entity is, since the game keeps one air supply
     * for all of them, and an entity that does not breathe air still answers with it.
     * example:
     * <pre>
     * const mobs = World.getEntities(20, "dolphin", "cod", "player");
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     Chat.log(`${entity.getType()}: ${entity.getAir()} of ${entity.getMaxAir()}`);
     *   }
     * }
     * </pre>
     *
     * @return the maximum amount of air this entity can have.
     * @since 1.8.4
     */
    public int getMaxAir() {
        return base.getMaxAirSupply();
    }

    /**
     * the amount of air this entity has, which counts down as it goes without air and is
     * refilled to {@link #getMaxAir()} when it can breathe. The number of ticks rather
     * than a fraction, so an entity about to run out is one with a small number here.
     * <p>
     * The number moves as the entity breathes, so a second read of the same entity a
     * moment later gives a different one.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const air = player.getAir();
     *   const max = player.getMaxAir();
     *   // the smaller the remaining figure, the closer to running out
     *   if (max > air * 2) {
     *     Chat.log(`less than half the air left, ${air} of ${max} ticks`);
     *   }
     * }
     * </pre>
     *
     * @return the amount of air this entity has.
     * @since 1.8.4
     */
    public int getAir() {
        return base.getAirSupply();
    }

    /**
     * this entity's current speed in blocks per second.
     * <p>
     * It is worked out from how far the entity has moved since the start of the tick,
     * on the horizontal plane only, so a jump reads as no movement at all and a number
     * here is a horizontal pace rather than a speed through the air. The figure moves as
     * the entity does, so a second read a moment later gives a different one.
     * <p>
     * The same figure is a rough measure of a mob's pace: faster than the player walking
     * is a mob that is sprinting, and faster still is one that is running away.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // the player's own pace, which is the yardstick for a mob's
     *   Chat.log(`the player is moving at ${player.getSpeed().toFixed(2)} blocks a second`);
     * }
     * </pre>
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
     * the direction the entity is facing, worked out from its yaw.
     * <p>
     * It is a horizontal direction rather than a full one: the figure is the yaw cut
     * into quarters, so the answer is one of north, east, south or west and never one
     * of the eight the game has in total. There is no up or down here; the entity's
     * {@link #getPitch()} is the separate reading for that.
     * <p>
     * A quarter turn is ninety degrees rather than forty-five, so an entity facing
     * between two of them reads as whichever is nearer.
     * example:
     * <pre>
     * const mobs = World.getEntities(10, "pig", "cow", "sheep");
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     Chat.log(`a ${entity.getType()} faces ${entity.getFacingDirection().getName()}`);
     *   }
     * }
     * </pre>
     *
     * @return the direction the entity is facing, rounded to the nearest 90 degrees.
     * @since 1.8.4
     */
    public DirectionHelper getFacingDirection() {
        return new DirectionHelper(base.getDirection());
    }

    /**
     * the distance between this entity and the specified one, measured to its feet
     * rather than to its eyes, and as a whole number rather than as a fraction.
     * <p>
     * It is the three dimensional distance, so an entity directly overhead is as far
     * away as one on the ground. The value is a float rather than a double, so it
     * loses a little precision on a long distance.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * const mobs = World.getEntities(30) ?? [];
     * if (player !== null) {
     *   for (const entity of mobs) {
     *     Chat.log(`${entity.getType()} is ${entity.distanceTo(player)} blocks away`);
     *   }
     * }
     * </pre>
     *
     * @param entity the entity to measure to
     * @return the distance between this entity and the specified one.
     * @since 1.8.4
     */
    public float distanceTo(EntityHelper<?> entity) {
        return base.distanceTo(entity.getRaw());
    }

    /**
     * the distance between this entity and the specified position, which is the
     * straight line to the middle of the block rather than to any corner of it. The
     * position is a block, so the half a block either way across is already in the
     * answer before the entity's own position is taken into account.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const block = player.getBlockPos();
     *   // to the middle of the block the player is standing in
     *   Chat.log(`half a block and change, ${player.distanceTo(block)}`);
     * }
     * </pre>
     *
     * @param pos the position to measure to
     * @return the distance between this entity and the specified position.
     * @since 1.8.4
     */
    public double distanceTo(BlockPosHelper pos) {
        return Math.sqrt(pos.getRaw().distToCenterSqr(base.position()));
    }

    /**
     * the distance between this entity and the specified position, as a straight line
     * in three dimensions rather than on the ground, so an entity directly overhead is
     * as far away as one on the ground.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // to a point ten blocks up in the air above where the player is standing
     *   const above = player.getPos().add(0, 10, 0);
     *   Chat.log(`${player.distanceTo(above)} blocks straight up`);
     * }
     * </pre>
     *
     * @param pos the position to measure to
     * @return the distance between this entity and the specified position.
     * @since 1.8.4
     */
    public double distanceTo(Pos3D pos) {
        return Math.sqrt(base.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()));
    }

    /**
     * the distance between this entity and the point at the three given coordinates,
     * as a straight line in three dimensions. It is the form to use when the point is
     * worked out rather than held, since it saves building a position for it.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // to the player's own position, offset by a block in each direction
     *   const pos = player.getPos();
     *   Chat.log(`${player.distanceTo(pos.x + 1, pos.y, pos.z + 1)} blocks diagonally`);
     * }
     * </pre>
     *
     * @param x the x coordinate to measure to
     * @param y the y coordinate to measure to
     * @param z the z coordinate to measure to
     * @return the distance between this entity and the specified position.
     * @since 1.8.4
     */
    public double distanceTo(double x, double y, double z) {
        return Math.sqrt(base.distanceToSqr(x, y, z));
    }

    /**
     * the velocity vector, as a position rather than as a direction, which means the
     * numbers are the movement per tick rather than a heading. It is the same figure the
     * game moves the entity by, so a number of one is a block a tick.
     * <p>
     * It is a reading rather than a handle, and it changes as the entity does: friction
     * and drag are applied to it over the tick rather than the entity being moved by a
     * fixed amount, so a second read gives a smaller figure. Writing to the result does
     * not change the entity's movement; a local player has setters for that.
     * example:
     * <pre>
     * const mobs = World.getEntities(10);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     const velocity = entity.getVelocity();
     *     if (velocity.y > 0) {
     *       Chat.log(`${entity.getType()} is moving up at ${velocity.y} a tick`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the velocity vector.
     * @since 1.8.4
     */
    public Pos3D getVelocity() {
        return new Pos3D(base.getDeltaMovement().x, base.getDeltaMovement().y, base.getDeltaMovement().z);
    }

    /**
     * the chunk helper for the chunk this entity is in, which is the one its
     * {@link #getBlockPos()} falls in rather than the one its position rounds towards.
     * <p>
     * A chunk is sixteen blocks across on each axis, so the figure is the block position
     * divided by sixteen.
     * example:
     * <pre>
     * const mobs = World.getEntities(20);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     const chunk = entity.getChunk();
     *     Chat.log(`a ${entity.getType()} is in chunk `
     *       + `${chunk.getChunkX()}, ${chunk.getChunkZ()}`);
     *   }
     * }
     * </pre>
     *
     * @return the chunk helper for the chunk this entity is in.
     * @since 1.8.4
     */
    public ChunkHelper getChunk() {
        return new ChunkHelper(base.level().getChunk(base.blockPosition()));
    }

    /**
     * the name of the biome this entity is in, as a registry id with the namespace on
     * the front.
     * <p>
     * It is read from the client's own world rather than from the entity, and the
     * position it uses is the entity's own block position, so an entity standing on the
     * line between two biomes gives whichever one the game has put at that block. The
     * two are not the same thing: a biome can change with height inside one column.
     * example:
     * <pre>
     * const mobs = World.getEntities(20);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     Chat.log(`a ${entity.getType()} is standing in ${entity.getBiome()}`);
     *   }
     * }
     * </pre>
     *
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
     * <p>
     * This is what wraps an entity as the most particular helper that fits it, checking
     * a long list of kinds in order and returning the first that matches. A villager
     * comes back as a villager helper rather than as a merchant or a mob one, an arrow
     * as an arrow helper, and a boat as a boat helper. Something the list does not cover
     * comes back as a plain helper.
     * <p>
     * A script rarely needs this, because {@code World.getEntities()} already hands back
     * entities wrapped this way. It is worth reaching for when a script has a raw
     * entity from somewhere else and wants the same treatment.
     * <p>
     * Passing {@code null} throws rather than giving back {@code null}, so a caller that
     * might not have an entity has to check first.
     * example:
     * <pre>
     * const EntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper");
     * const mobs = World.getEntities(20);
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     // already wrapped, so this gives the same kind of helper back
     *     const wrapped = EntityHelper.create(entity.getRaw());
     *     Chat.log(`${entity.getType()} is a ${wrapped.getClass().getSimpleName()}`);
     *   }
     * }
     * </pre>
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
     * cast of this entity helper (mainly for typescript)
     * <p>
     * These narrowing calls are casts rather than checks. Each one answers with the more
     * particular type or throws a cast error, and none of them answers with {@code null}.
     * The type a script sees changes, which is what these are for; the object does not,
     * so a cast to the wrong kind fails rather than quietly giving back something else.
     * <p>
     * This one is for the local player, which is the player the client is controlling.
     * An entity that is a player but not the local one is not this, and casting it gives
     * an error rather than a helper of the wrong kind.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   // the local player, which is what a script means by "the player"
     *   const local = player.asClientPlayer();
     *   Chat.log(`looking at ${local.getFacingDirection().getName()}`);
     * }
     * </pre>
     *
     * @return cast of this entity helper (mainly for typescript)
     * @since 1.6.3
     */
    public ClientPlayerEntityHelper<?> asClientPlayer() {
        return (ClientPlayerEntityHelper<?>) this;
    }

    /**
     * cast of this entity helper (mainly for typescript)
     * <p>
     * This is for any player, the local one included, so a cast that fails here means
     * the entity is not a player at all rather than that it is the wrong player.
     * example:
     * <pre>
     * const players = World.getEntities(30, "player");
     * if (players !== null) {
     *   for (const entity of players) {
     *     const player = entity.asPlayer();
     *     Chat.log(`${player.getPlayerName()} on ${player.getXPLevel()}`);
     *   }
     * }
     * </pre>
     *
     * @return cast of this entity helper (mainly for typescript)
     * @since 1.6.3
     */
    public PlayerEntityHelper<?> asPlayer() {
        return (PlayerEntityHelper<?>) this;
    }

    /**
     * cast of this entity helper (mainly for typescript)
     * <p>
     * This is for a villager specifically rather than for anything that trades, so a
     * wandering trader is not this and casting one gives an error.
     * example:
     * <pre>
     * const villagers = World.getEntities(30, "villager");
     * if (villagers !== null) {
     *   for (const entity of villagers) {
     *     const villager = entity.asVillager();
     *     Chat.log(`level ${villager.getLevel()} ${villager.getProfession()}`);
     *   }
     * }
     * </pre>
     *
     * @return cast of this entity helper (mainly for typescript)
     * @since 1.6.3
     */
    public VillagerEntityHelper asVillager() {
        return (VillagerEntityHelper) this;
    }

    /**
     * cast of this entity helper (mainly for typescript)
     * <p>
     * This is for anything that trades, so a villager is this as well as being
     * {@link #asVillager()}. What it adds over a plain living entity is the trading
     * state, and the trade list on it is not readable from the client.
     * example:
     * <pre>
     * const villagers = World.getEntities(30, "villager", "wandering_trader");
     * if (villagers !== null) {
     *   for (const entity of villagers) {
     *     const merchant = entity.asMerchant();
     *     // the trading state, which is readable, unlike the trade list
     *     Chat.log(`busy: ${merchant.hasCustomer()}, `
     *       + `experience: ${merchant.getExperience()}`);
     *   }
     * }
     * </pre>
     *
     * @return cast of this entity helper (mainly for typescript)
     * @since 1.6.3
     */
    public MerchantEntityHelper<?> asMerchant() {
        return (MerchantEntityHelper<?>) this;
    }

    /**
     * cast of this entity helper (mainly for typescript)
     * <p>
     * This is for anything alive, so a mob, a player and a villager are all this. What
     * it adds over a plain entity is health, equipment and the effects.
     * <p>
     * A list from {@code World.getEntities()} holds dropped items and projectiles as
     * well as living things, so a script walking one has to have filtered by type
     * before this can be called on everything in it.
     * example:
     * <pre>
     * // filtered by type first, so the cast is safe on everything in the list
     * const mobs = World.getEntities(20, "zombie", "skeleton", "cow");
     * if (mobs !== null) {
     *   for (const entity of mobs) {
     *     const living = entity.asLiving();
     *     Chat.log(`${living.getType()} on ${living.getHealth()} health`);
     *   }
     * }
     * </pre>
     *
     * @return cast of this entity helper (mainly for typescript)
     * @since 1.6.3
     */
    public LivingEntityHelper<?> asLiving() {
        return (LivingEntityHelper<?>) this;
    }

    /**
     * this helper as an animal entity helper (mainly for typescript).
     * <p>
     * This is for anything of the animal kind rather than for anything alive, so a
     * zombie is not this. What a script gains over {@link #asLiving()} is whatever the
     * animal helpers add, and the mob-only methods here are not part of it.
     * example:
     * <pre>
     * const animals = World.getEntities(30, "pig", "cow", "sheep", "chicken");
     * if (animals !== null) {
     *   for (const entity of animals) {
     *     const animal = entity.asAnimal();
     *     Chat.log(`an animal at ${animal.getPos()}`);
     *   }
     * }
     * </pre>
     *
     * @return this helper as an animal entity helper (mainly for typescript).
     * @since 1.8.4
     */
    public LivingEntityHelper<?> asAnimal() {
        return (AnimalEntityHelper<?>) this;
    }

    /**
     * cast of this entity helper (mainly for typescript)
     * <p>
     * This is for a dropped stack, which is a single item lying in the world rather
     * than a container. What it adds is the one reading of what the stack is.
     * example:
     * <pre>
     * const dropped = World.getEntities(20, "item");
     * if (dropped !== null) {
     *   for (const entity of dropped) {
     *     const stack = entity.asItem().getContainedItemStack();
     *     if (!stack.isEmpty()) {
     *       Chat.log(`${stack.getCount()} ${stack.getItemId()} at ${entity.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return cast of this entity helper (mainly for typescript)
     * @since 1.6.3
     */
    public ItemEntityHelper asItem() {
        return (ItemEntityHelper) this;
    }

    /**
     * Looks up the entity on the integrated server thread. The returned helper wraps a server
     * entity; callers must only access it on that thread. Returns {@code null} in multiplayer or
     * when the entity is no longer loaded on the integrated server.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const server = player.asServerEntity();
     *   // do not read server entity state from this client-side callback
     *   Chat.log(server === null ? "server entity unavailable" : "server entity found");
     * }
     * </pre>
     *
     * @return the server entity helper, or {@code null} when unavailable.
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
