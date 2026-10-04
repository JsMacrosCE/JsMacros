package com.jsmacrosce.jsmacros.client.api.helper.world;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A block state with a named method for every block state property the game has, each one reading
 * that property and giving the value back already typed.<br>
 * This is the class to reach for when a script knows which property it is after.
 * {@link StateHelper#toMap()} gives every property on a state as a name, which is the right tool
 * for reading a property whose name is a variable, and this gives one method per property, which is
 * the right tool for reading a property whose name is written down in the script. The difference
 * shows in what comes back: a name for a property read through the map, and the value itself here,
 * so a facing is a direction rather than a word, and an age is a number rather than digits in a
 * string.
 * <p>
 * Every method here reads one fixed property, and it is an error to ask a state that has no such
 * property rather than something that comes back false or null. That is the trade for the speed:
 * these are reads rather than lookups, and a script that wants to know what a state has should ask
 * {@link StateHelper#toMap()} first. The four number families are the exception worth knowing
 * about, since they try a list of properties in turn and raise if the state has none of them:
 * {@link #getLevel()} and its min and max, {@link #getDistance()} and its min and max, and
 * {@link #getAge()} with {@link #getMaxAge()}.
 * <p>
 * A script gets one from {@link BlockStateHelper#getUniversal()}.
 * example:
 * <pre>
 * // the typed view of a state, against the map view of the same state
 * const reg = Client.getRegistryManager();
 * const state = reg.getBlockState("minecraft:oak_stairs", "[facing=east,half=bottom]");
 * const universal = state.getUniversal();
 *
 * Chat.log(universal.getHorizontalFacing().getName());   // a direction
 * Chat.log(universal.getBlockHalf());                    // a word
 * Chat.log(universal.isWaterlogged());                    // a boolean
 *
 * for (const [name, value] of state.toMap()) {
 *   Chat.log(`${name} is ${value}`);                      // every property, as names
 * }
 *
 * // a property the state does not have is an error, not a false
 * try {
 *   universal.getHoneyLevel();
 * } catch (e) {
 *   Chat.log("a stair has no honey level");
 * }
 *
 * // and the number families try several properties before giving up
 * const wheat = reg.getBlockState("minecraft:wheat").getUniversal();
 * Chat.log(`age ${wheat.getAge()} of ${wheat.getMaxAge()}`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class UniversalBlockStateHelper extends BlockStateHelper {

    public UniversalBlockStateHelper(BlockState base) {
        super(base);
    }

    /**
     * @return the {@code BELL_ATTACHMENT} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getAttachment() {
        return base.getValue(BlockStateProperties.BELL_ATTACHMENT).getSerializedName();
    }

    /**
     * @return the {@code EAST_WALL} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getEastWallShape() {
        return base.getValue(BlockStateProperties.EAST_WALL).getSerializedName();
    }

    /**
     * @return the {@code NORTH_WALL} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getNorthWallShape() {
        return base.getValue(BlockStateProperties.NORTH_WALL).getSerializedName();
    }

    /**
     * @return the {@code SOUTH_WALL} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getSouthWallShape() {
        return base.getValue(BlockStateProperties.SOUTH_WALL).getSerializedName();
    }

    /**
     * @return the {@code WEST_WALL} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getWestWallShape() {
        return base.getValue(BlockStateProperties.WEST_WALL).getSerializedName();
    }

    /**
     * @return the {@code EAST_REDSTONE} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getEastWireConnection() {
        return base.getValue(BlockStateProperties.EAST_REDSTONE).getSerializedName();
    }

    /**
     * @return the {@code NORTH_REDSTONE} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getNorthWireConnection() {
        return base.getValue(BlockStateProperties.NORTH_REDSTONE).getSerializedName();
    }

    /**
     * @return the {@code SOUTH_REDSTONE} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getSouthWireConnection() {
        return base.getValue(BlockStateProperties.SOUTH_REDSTONE).getSerializedName();
    }

    /**
     * @return the {@code WEST_REDSTONE} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getWestWireConnection() {
        return base.getValue(BlockStateProperties.WEST_REDSTONE).getSerializedName();
    }

    /**
     * @return the {@code HALF} property's value, as the name the game gives it.
     * example:
     * <pre>
     * // which half of a two block shape this is
     * const bed = Client.getRegistryManager().getBlockState("minecraft:red_bed", "[part=head]").getUniversal();
     * Chat.log(`bed part ${bed.getBedPart()}, half ${bed.getBlockHalf()}`);
     *
     * // slabs and stairs, which use a different pair of properties for the same idea
     * const slab = Client.getRegistryManager().getBlockState("minecraft:oak_slab", "[type=top]").getUniversal();
     * Chat.log(`slab type ${slab.getSlabType()}`);
     * const pillar = Client.getRegistryManager()
     *   .getBlockState("minecraft:oak_fence", "[half=upper]").getUniversal();
     * Chat.log(`fence half ${pillar.getBlockHalf()}`);
     * </pre>
     * @since 1.8.4
     */
    public String getBlockHalf() {
        return base.getValue(BlockStateProperties.HALF).getSerializedName();
    }

    /**
     * @return the {@code DOUBLE_BLOCK_HALF} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getDoubleBlockHalf() {
        return base.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF).getSerializedName();
    }

    /**
     * @return the {@code RAIL_SHAPE} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getRailShape() {
        return base.getValue(BlockStateProperties.RAIL_SHAPE).getSerializedName();
    }

    /**
     * @return the {@code RAIL_SHAPE_STRAIGHT} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getStraightRailShape() {
        return base.getValue(BlockStateProperties.RAIL_SHAPE_STRAIGHT).getSerializedName();
    }

    /**
     * @return the {@code ORIENTATION} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getOrientation() {
        return base.getValue(BlockStateProperties.ORIENTATION).getSerializedName();
    }

    /**
     * @return the {@code HORIZONTAL_AXIS} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getHorizontalAxis() {
        return base.getValue(BlockStateProperties.HORIZONTAL_AXIS).getSerializedName();
    }

    /**
     * @return the {@code AXIS} property's value, as the name the game gives it.
     * example:
     * <pre>
     * // a log's axis, which is the one property a log is mostly about
     * const reg = Client.getRegistryManager();
     * const state = reg.getBlockState("minecraft:oak_log", "[axis=x]");
     * Chat.log(`${state.getUniversal().getAxis()} and ${state.toMap().axis}`);
     *
     * // and the one place a pillar can point
     * for (const axis of ["x", "y", "z"]) {
     *   const turned = Client.getRegistryManager().getBlockState("minecraft:oak_log", `[axis=${axis}]`)
     *     .getUniversal();
     *   Chat.log(`${turned.getAxis()}`);
     * }
     * </pre>
     * @since 1.8.4
     */
    public String getAxis() {
        return base.getValue(BlockStateProperties.AXIS).getName();
    }

    /**
     * @return the {@code HORIZONTAL_FACING} property, as a direction.
     * example:
     * <pre>
     * // a facing, as a direction rather than as a word
     * const reg = Client.getRegistryManager();
     * const stairs = reg.getBlockState("minecraft:oak_stairs", "[facing=east]");
     * const facing = stairs.getUniversal().getHorizontalFacing();
     * Chat.log(`${facing.getName()} is on the ${facing.getAxis()} axis, yaw ${facing.getYaw()}`);
     *
     * // and the same word, as the property map spells it
     * Chat.log(stairs.toMap().facing);
     * </pre>
     * @since 1.8.4
     */
    public DirectionHelper getHorizontalFacing() {
        return new DirectionHelper(base.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }

    /**
     * @return the {@code FACING_HOPPER} property, as a direction.
     * @since 1.8.4
     */
    public DirectionHelper getHopperFacing() {
        return new DirectionHelper(base.getValue(BlockStateProperties.FACING_HOPPER));
    }

    /**
     * @return the {@code FACING} property, as a direction.
     * example:
     * <pre>
     * // the six directions a furnace can face
     * for (const name of ["north", "south", "east", "west", "up", "down"]) {
     *   const block = Client.getRegistryManager()
     *     .getBlockState("minecraft:furnace", `[facing=${name}]`).getUniversal();
     *   const facing = block.getFacing();
     *   Chat.log(`${facing.getName()}, pitch ${facing.getPitch()}`);
     * }
     * </pre>
     * @since 1.8.4
     */
    public DirectionHelper getFacing() {
        return new DirectionHelper(base.getValue(BlockStateProperties.FACING));
    }

    /**
     * @return the {@code UP} property's value.
     * @since 1.8.4
     */
    public boolean isUp() {
        return base.getValue(BlockStateProperties.UP);
    }

    /**
     * @return the {@code DOWN} property's value.
     * @since 1.8.4
     */
    public boolean isDown() {
        return base.getValue(BlockStateProperties.DOWN);
    }

    /**
     * @return the {@code NORTH} property's value.
     * @since 1.8.4
     */
    public boolean isNorth() {
        return base.getValue(BlockStateProperties.NORTH);
    }

    /**
     * @return the {@code SOUTH} property's value.
     * @since 1.8.4
     */
    public boolean isSouth() {
        return base.getValue(BlockStateProperties.SOUTH);
    }

    /**
     * @return the {@code EAST} property's value.
     * @since 1.8.4
     */
    public boolean isEast() {
        return base.getValue(BlockStateProperties.EAST);
    }

    /**
     * @return the {@code WEST} property's value.
     * @since 1.8.4
     */
    public boolean isWest() {
        return base.getValue(BlockStateProperties.WEST);
    }

    /**
     * Used on beehives.
     *
     * @return the {@code LEVEL_HONEY} property's value.
     * example:
     * <pre>
     * // how full a beehive or a bottle is, from zero to five
     * const reg = Client.getRegistryManager();
     * for (let n = 0; n !== 6; n += 1) {
     *   const hive = reg.getBlockState("minecraft:beehive", `[honey_level=${n}]`).getUniversal();
     *   Chat.log(`honey level ${hive.getHoneyLevel()}`);
     * }
     *
     * // and the state a filled bottle is in
     * const bottle = reg.getBlockState("minecraft:honey_bottle").getUniversal();
     * Chat.log(`a honey bottle is level ${bottle.getHoneyLevel()}`);
     * </pre>
     * @since 1.8.4
     */
    public int getHoneyLevel() {
        return base.getValue(BlockStateProperties.LEVEL_HONEY);
    }

    /**
     * Used on scaffolding.
     *
     * @return the {@code BOTTOM} property's value.
     * @since 1.8.4
     */
    public boolean isBottom() {
        return base.getValue(BlockStateProperties.BOTTOM);
    }

    /**
     * Used on bubble columns.
     *
     * @return the {@code DRAG} property's value.
     * @since 1.8.4
     */
    @Ignore({"hasDrag"})
    public boolean isBubbleColumnDown() {
        return base.getValue(BlockStateProperties.DRAG);
    }

    /**
     * Used on bubble columns.
     *
     * @return the {@code DRAG} property's value, negated.
     * @since 1.8.4
     */
    @Ignore
    public boolean isBubbleColumnUp() {
        return !base.getValue(BlockStateProperties.DRAG);
    }

    /**
     * Used on trip wire hooks.
     *
     * @return the {@code ATTACHED} property's value.
     * @since 1.8.4
     */
    public boolean isAttached() {
        return base.getValue(BlockStateProperties.ATTACHED);
    }

    /**
     * Used on trip wires.
     *
     * @return the {@code DISARMED} property's value.
     * @since 1.8.4
     */
    public boolean isDisarmed() {
        return base.getValue(BlockStateProperties.DISARMED);
    }

    /**
     * Used on command blocks.
     *
     * @return the {@code CONDITIONAL} property's value.
     * @since 1.8.4
     */
    public boolean isConditional() {
        return base.getValue(BlockStateProperties.CONDITIONAL);
    }

    /**
     * Used on hoppers.
     *
     * @return the {@code ENABLED} property's value.
     * @since 1.8.4
     */
    public boolean isEnabled() {
        return base.getValue(BlockStateProperties.ENABLED);
    }

    /**
     * Used on pistons.
     *
     * @return the {@code EXTENDED} property's value.
     * @since 1.8.4
     */
    public boolean isExtended() {
        return base.getValue(BlockStateProperties.EXTENDED);
    }

    /**
     * Used on piston heads.
     *
     * @return the {@code SHORT} property's value.
     * @since 1.8.4
     */
    public boolean isShort() {
        return base.getValue(BlockStateProperties.SHORT);
    }

    /**
     * Used on end portal frames.
     *
     * @return the {@code EYE} property's value.
     * @since 1.8.4
     */
    public boolean hasEye() {
        return base.getValue(BlockStateProperties.EYE);
    }

    /**
     * Used on fluids.
     *
     * @return the {@code FALLING} property's value.
     * @since 1.8.4
     */
    public boolean isFalling() {
        return base.getValue(BlockStateProperties.FALLING);
    }

    // don't make static, causes crash in main function below
    private final IntegerProperty[] levels = {
            BlockStateProperties.LEVEL_FLOWING,
            BlockStateProperties.LEVEL_CAULDRON,
            BlockStateProperties.LEVEL_COMPOSTER,
            BlockStateProperties.LEVEL
    };

    /**
     * The level of this state, read from the first of the game's level properties the state turns
     * out to have. The list is tried in order and the first match wins, so a state that has more
     * than one of them answers with the earliest rather than with the most meaningful.<br>
     * A state with none of them is an error rather than a zero, so this is only worth asking of a
     * block that is one of the level-carrying kinds.
     * example:
     * <pre>
     * // the level of a fluid, and the same number through the property map
     * const reg = Client.getRegistryManager();
     * const water = reg.getFluidState("minecraft:water").getBlockState().getUniversal();
     * Chat.log(`level ${water.getLevel()}, between ${water.getMinLevel()} and ${water.getMaxLevel()}`);
     *
     * // and a block with no level property at all
     * try {
     *   reg.getBlockState("minecraft:stone").getUniversal().getLevel();
     * } catch (e) {
     *   Chat.log("stone has no level");
     * }
     * </pre>
     *
     * Used on fluids and stuff
     *
     * @return the level of this state, from the first of the game's level properties it has.
     * @throws IllegalStateException if the state has none of the level properties.
     * @since 1.8.4
     */
    @Ignore({"getLevel1_8", "getLevel3", "getLevel8", "getLevel15"})
    public int getLevel() {
        for (IntegerProperty level : levels) {
            if (base.hasProperty(level)) {
                return base.getValue(level);
            }
        }
        throw new IllegalStateException("No level property found");
    }

    /**
     * The largest value the level property this state has could hold, worked out from the property
     * itself rather than from any particular value. The same list of properties is tried as
     * {@link #getLevel()}, and the first one the state has is the one measured.
     * example:
     * <pre>
     * // the range a level can take here
     * const lava = Client.getRegistryManager().getFluidState("minecraft:lava")
     *   .getBlockState().getUniversal();
     * Chat.log(`${lava.getMinLevel()} to ${lava.getMaxLevel()}`);
     * </pre>
     *
     * @return the largest value the level property this state has can hold.
     * @throws IllegalStateException if the state has none of the level properties.
     * @since 1.8.4
     */
    @Ignore
    public int getMaxLevel() {
        for (IntegerProperty level : levels) {
            if (base.hasProperty(level)) {
                return level.getPossibleValues().stream().max(Integer::compare).orElse(-1);
            }
        }
        throw new IllegalStateException("No level property found");
    }

    /**
     * The smallest value the level property this state has could hold, worked out from the property
     * itself. The same list of properties is tried as {@link #getLevel()}, and the first one the
     * state has is the one measured.
     * example:
     * <pre>
     * // the range a level can take here
     * const water = Client.getRegistryManager().getFluidState("minecraft:water")
     *   .getBlockState().getUniversal();
     * Chat.log(`${water.getMinLevel()} to ${water.getMaxLevel()}`);
     * </pre>
     *
     * @return the smallest value the level property this state has can hold.
     * @throws IllegalStateException if the state has none of the level properties.
     * @since 1.8.4
     */
    @Ignore
    public int getMinLevel() {
        for (IntegerProperty level : levels) {
            if (base.hasProperty(level)) {
                return level.getPossibleValues().stream().min(Integer::compare).orElse(-1);
            }
        }
        throw new IllegalStateException("No level property found");
    }

    /**
     * Used on lanterns.
     *
     * @return the {@code HANGING} property's value.
     * @since 1.8.4
     */
    public boolean isHanging() {
        return base.getValue(BlockStateProperties.HANGING);
    }

    /**
     * Used on brewing stands.
     *
     * @return the {@code HAS_BOTTLE_0} property's value.
     * @since 1.8.4
     */
    public boolean hasBottle0() {
        return base.getValue(BlockStateProperties.HAS_BOTTLE_0);
    }

    /**
     * Used on brewing stands.
     *
     * @return the {@code HAS_BOTTLE_1} property's value.
     * @since 1.8.4
     */
    public boolean hasBottle1() {
        return base.getValue(BlockStateProperties.HAS_BOTTLE_1);
    }

    /**
     * Used on brewing stands.
     *
     * @return the {@code HAS_BOTTLE_2} property's value.
     * @since 1.8.4
     */
    public boolean hasBottle2() {
        return base.getValue(BlockStateProperties.HAS_BOTTLE_2);
    }

    /**
     * Used on jukeboxes.
     *
     * @return the {@code HAS_RECORD} property's value.
     * @since 1.8.4
     */
    public boolean hasRecord() {
        return base.getValue(BlockStateProperties.HAS_RECORD);
    }

    /**
     * Used on lecterns.
     *
     * @return the {@code HAS_BOOK} property's value.
     * @since 1.8.4
     */
    public boolean hasBook() {
        return base.getValue(BlockStateProperties.HAS_BOOK);
    }

    /**
     * Used on daylight sensors.
     *
     * @return the {@code INVERTED} property's value.
     * @since 1.8.4
     */
    public boolean isInverted() {
        return base.getValue(BlockStateProperties.INVERTED);
    }

    /**
     * Used on fence gates.
     *
     * @return the {@code IN_WALL} property's value.
     * @since 1.8.4
     */
    public boolean isInWall() {
        return base.getValue(BlockStateProperties.IN_WALL);
    }

    /**
     * Used on fence gates, barrels, trap doors and doors.
     *
     * @return the {@code OPEN} property's value.
     * example:
     * <pre>
     * // everything in one place that opens, and whether it is open
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:oak_door", "minecraft:oak_trapdoor", "minecraft:iron_door",
     *                   "minecraft:oak_gate", "minecraft:barrel", "minecraft:chest"]) {
     *   try {
     *     const state = reg.getBlockState(id, "[open=true]").getUniversal();
     *     Chat.log(`${id}: ${state.isOpen()}`);
     *   } catch (e) {
     *     Chat.log(`${id} has no open property`);
     *   }
     * }
     * </pre>
     * @since 1.8.4
     */
    public boolean isOpen() {
        return base.getValue(BlockStateProperties.OPEN);
    }

    /**
     * Used on candles, all types of furnaces, campfires and redstone torches.
     *
     * @return the {@code LIT} property's value.
     * example:
     * <pre>
     * // which of the burning things are alight
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:furnace", "minecraft:blast_furnace", "minecraft:campfire",
     *                   "minecraft:torch", "minecraft:candle"]) {
     *   const state = reg.getBlockState(id, "[lit=true]").getUniversal();
     *   Chat.log(`${id} is lit: ${state.isLit()}`);
     * }
     * </pre>
     * @since 1.8.4
     */
    public boolean isLit() {
        return base.getValue(BlockStateProperties.LIT);
    }

    /**
     * Used on repeaters.
     *
     * @return the {@code LOCKED} property's value.
     * @since 1.8.4
     */
    public boolean isLocked() {
        return base.getValue(BlockStateProperties.LOCKED);
    }

    /**
     * Used on repeaters.
     *
     * @return the {@code DELAY} property's value.
     * example:
     * <pre>
     * // the four repeater settings
     * for (let d = 1; d !== 5; d += 1) {
     *   const rep = Client.getRegistryManager()
     *     .getBlockState("minecraft:repeater", `[delay=${d}]`).getUniversal();
     *   Chat.log(`delay ${rep.getDelay()}, locked ${rep.isLocked()}`);
     * }
     * </pre>
     * @since 1.8.4
     */
    public int getDelay() {
        return base.getValue(BlockStateProperties.DELAY);
    }

    /**
     * Used on beds.
     *
     * @return the {@code OCCUPIED} property's value.
     * @since 1.8.4
     */
    public boolean isOccupied() {
        return base.getValue(BlockStateProperties.OCCUPIED);
    }

    /**
     * Used on leaves.
     *
     * @return the {@code PERSISTENT} property's value.
     * @since 1.8.4
     */
    public boolean isPersistent() {
        return base.getValue(BlockStateProperties.PERSISTENT);
    }

    private final IntegerProperty distance[] = {
            BlockStateProperties.STABILITY_DISTANCE,
            BlockStateProperties.DISTANCE
    };

    /**
     * The distance property of this state, read from the first of the game's distance properties
     * the state turns out to have. The list is tried in order and the first match wins, so a state
     * that has both answers with the earlier one.
     * <p>
     * A state with none of them is an error rather than a zero.
     * example:
     * <pre>
     * // how far a leaf block is from its trunk
     * const leaves = Client.getRegistryManager()
     *   .getBlockState("minecraft:oak_leaves", "[distance=5,persistent=true]").getUniversal();
     * Chat.log(`distance ${leaves.getDistance()} of ${leaves.getMaxDistance()}`);
     * Chat.log(`and it is persistent: ${leaves.isPersistent()}`);
     *
     * // a state with no distance property at all
     * try {
     *   Client.getRegistryManager().getBlockState("minecraft:stone").getUniversal().getDistance();
     * } catch (e) {
     *   Chat.log("stone has no distance");
     * }
     * </pre>
     *
     * Used on leaves and scaffold.
     *
     * @return the distance of this state, from the first of the game's distance properties it has.
     * @throws IllegalStateException if the state has none of the distance properties.
     * @since 1.8.4
     */
    @Ignore({"getDistance0_7", "getDistance1_7"})
    public int getDistance() {
        for (IntegerProperty d : distance) {
            if (base.hasProperty(d)) {
                return base.getValue(d);
            }
        }
        throw new IllegalStateException("No distance property found");
    }

    /**
     * The largest value the distance property this state has could hold, worked out from the
     * property itself. The same list of properties is tried as {@link #getDistance()}.
     * example:
     * <pre>
     * // how far a leaf block can decay
     * const leaves = Client.getRegistryManager().getBlockState("minecraft:oak_leaves").getUniversal();
     * Chat.log(`${leaves.getMinDistance()} to ${leaves.getMaxDistance()}`);
     * </pre>
     *
     * Used on leaves and scaffold.
     *
     * @return the largest value the distance property this state has can hold.
     * @throws IllegalStateException if the state has none of the distance properties.
     * @since 1.8.4
     */
    @Ignore
    public int getMaxDistance() {
        for (IntegerProperty d : distance) {
            if (base.hasProperty(d)) {
                return d.getPossibleValues().stream().max(Integer::compare).orElse(-1);
            }
        }
        throw new IllegalStateException("No distance property found");
    }

    /**
     * The smallest value the distance property this state has could hold, worked out from the
     * property itself. The same list of properties is tried as {@link #getDistance()}.
     * example:
     * <pre>
     * // how far a leaf block counts from its trunk
     * const leaves = Client.getRegistryManager().getBlockState("minecraft:oak_leaves").getUniversal();
     * Chat.log(`${leaves.getMinDistance()} to ${leaves.getMaxDistance()}`);
     * </pre>
     *
     * Used on leaves and scaffold.
     *
     * @return the smallest value the distance property this state has can hold.
     * @throws IllegalStateException if the state has none of the distance properties.
     * @since 1.8.4
     */
    @Ignore
    public int getMinDistance() {
        for (IntegerProperty d : distance) {
            if (base.hasProperty(d)) {
                return d.getPossibleValues().stream().min(Integer::compare).orElse(-1);
            }
        }
        throw new IllegalStateException("No distance property found");
    }

    /**
     * Used on bells, buttons, detector rails, diodes, doors, fence gates, lecterns, levers,
     * lightning rods, note blocks, observers, powered rails, pressure plates, trap doors, trip wire
     * hooks and trip wires.
     *
     * @return the {@code POWERED} property's value.
     * example:
     * <pre>
     * // the long list of blocks the game marks as powered
     * const reg = Client.getRegistryManager();
     * let count = 0;
     * for (const id of ["minecraft:redstone_torch", "minecraft:lever", "minecraft:redstone_block",
     *                   "minecraft:oak_pressure_plate", "minecraft:stone", "minecraft:observer"]) {
     *   const state = reg.getBlockState(id);
     *   if (state.getUniversal().isPowered()) {
     *     count += 1;
     *     Chat.log(`${id} is powered`);
     *   }
     * }
     * Chat.log(`${count} of those six`);
     * </pre>
     * @since 1.8.4
     */
    public boolean isPowered() {
        return base.getValue(BlockStateProperties.POWERED);
    }

    /**
     * Used on campfires.
     *
     * @return the {@code SIGNAL_FIRE} property's value.
     * @since 1.8.4
     */
    public boolean isSignalFire() {
        return base.getValue(BlockStateProperties.SIGNAL_FIRE);
    }

    /**
     * Used on snowy dirt blocks.
     *
     * @return the {@code SNOWY} property's value.
     * @since 1.8.4
     */
    public boolean isSnowy() {
        return base.getValue(BlockStateProperties.SNOWY);
    }

    /**
     * Used on dispensers.
     *
     * @return the {@code TRIGGERED} property's value.
     * @since 1.8.4
     */
    public boolean isTriggered() {
        return base.getValue(BlockStateProperties.TRIGGERED);
    }

    /**
     * Used on tnt.
     *
     * @return the {@code UNSTABLE} property's value.
     * @since 1.8.4
     */
    public boolean isUnstable() {
        return base.getValue(BlockStateProperties.UNSTABLE);
    }

    /**
     * Used on amethysts, corals, rails, dripleaves, dripleaf stems, campfires, candles, chains,
     * chests, conduits, fences, double plants, ender chests, iron bars, glass panes, glow lichen,
     * hanging roots, ladders, lanterns, light blocks, lightning rods, pointed dripstone,
     * scaffolding , sculk sensors, sea pickles, signs, stairs, slabs, trap doors and walls
     *
     * @return the {@code WATERLOGGED} property's value.
     * example:
     * <pre>
     * // is there water in this block rather than around it
     * const reg = Client.getRegistryManager();
     * const dry = reg.getBlockState("minecraft:oak_fence", "[waterlogged=false]").getUniversal();
     * const wet = reg.getBlockState("minecraft:oak_fence", "[waterlogged=true]").getUniversal();
     * Chat.log(`dry ${dry.isWaterlogged()}, wet ${wet.isWaterlogged()}`);
     *
     * // and what the wet one actually holds
     * const state = reg.getBlockState("minecraft:oak_fence", "[waterlogged=true]");
     * Chat.log(state.getFluidState().getId());
     * </pre>
     * @since 1.8.4
     */
    public boolean isWaterlogged() {
        return base.getValue(BlockStateProperties.WATERLOGGED);
    }

    /**
     * @return the {@code BED_PART} property's value, as the name the game gives it.
     * example:
     * <pre>
     * // the two halves of a bed, read apart
     * const reg = Client.getRegistryManager();
     * for (const part of ["head", "foot"]) {
     *   const half = reg.getBlockState("minecraft:red_bed", `[part=${part}]`).getUniversal();
     *   Chat.log(`${part}: ${half.getBedPart()}`);
     * }
     * </pre>
     * @since 1.8.4
     */
    public String getBedPart() {
        return base.getValue(BlockStateProperties.BED_PART).getSerializedName();
    }

    /**
     * @return the {@code DOOR_HINGE} property's value, as the name the game gives it.
     * example:
     * <pre>
     * // which side a door is hinged on
     * const door = Client.getRegistryManager()
     *   .getBlockState("minecraft:oak_door", "[hinge=left,open=true]").getUniversal();
     * Chat.log(`hinge ${door.getDoorHinge()}, open ${door.isOpen()}`);
     *
     * // all four combinations the game allows
     * for (const hinge of ["left", "right"]) {
     *   for (const open of ["true", "false"]) {
     *     const d = Client.getRegistryManager()
     *       .getBlockState("minecraft:oak_door", `[hinge=${hinge},open=${open}]`).getUniversal();
     *     Chat.log(`${d.getDoorHinge()} ${d.isOpen()}`);
     *   }
     * }
     * </pre>
     * @since 1.8.4
     */
    public String getDoorHinge() {
        return base.getValue(BlockStateProperties.DOOR_HINGE).getSerializedName();
    }

    /**
     * @return the {@code NOTEBLOCK_INSTRUMENT} property's value, as the name the game gives it.
     * example:
     * <pre>
     * // what a note block is set to
     * const reg = Client.getRegistryManager();
     * for (const name of ["harp", "basedrum", "snare", "hat", "guitar", "bell", "chime"]) {
     *   const note = reg.getBlockState("minecraft:note_block", `[instrument=${name}]`).getUniversal();
     *   Chat.log(`${note.getInstrument()} is note ${note.getNote()}`);
     * }
     * </pre>
     * @since 1.8.4
     */
    public String getInstrument() {
        return base.getValue(BlockStateProperties.NOTEBLOCK_INSTRUMENT).getSerializedName();
    }

    /**
     * @return the {@code PISTON_TYPE} property's value, as the name the game gives it.
     * example:
     * <pre>
     * // sticky and not
     * for (const type of ["normal", "sticky"]) {
     *   const piston = Client.getRegistryManager()
     *     .getBlockState("minecraft:piston", `[type=${type},facing=up]`).getUniversal();
     *   Chat.log(`${piston.getPistonType()}, facing ${piston.getFacing().getName()}, extended ${piston.isExtended()}`);
     * }
     * </pre>
     * @since 1.8.4
     */
    public String getPistonType() {
        return base.getValue(BlockStateProperties.PISTON_TYPE).getSerializedName();
    }

    /**
     * @return the {@code SLAB_TYPE} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getSlabType() {
        return base.getValue(BlockStateProperties.SLAB_TYPE).getSerializedName();
    }

    /**
     * @return the {@code STAIRS_SHAPE} property's value, as the name the game gives it.
     * example:
     * <pre>
     * // the four shapes a stair can be
     * for (const shape of ["straight", "inner_left", "inner_right", "outer_left", "outer_right"]) {
     *   const stair = Client.getRegistryManager()
     *     .getBlockState("minecraft:oak_stairs", `[shape=${shape}]`).getUniversal();
     *   Chat.log(`${stair.getStairShape()}, half ${stair.getBlockHalf()}`);
     * }
     * </pre>
     * @since 1.8.4
     */
    public String getStairShape() {
        return base.getValue(BlockStateProperties.STAIRS_SHAPE).getSerializedName();
    }

    /**
     * @return the {@code STRUCTUREBLOCK_MODE} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getStructureBlockMode() {
        return base.getValue(BlockStateProperties.STRUCTUREBLOCK_MODE).getSerializedName();
    }

    /**
     * @return the {@code BAMBOO_LEAVES} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getBambooLeaves() {
        return base.getValue(BlockStateProperties.BAMBOO_LEAVES).getSerializedName();
    }

    /**
     * @return the {@code TILT} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getTilt() {
        return base.getValue(BlockStateProperties.TILT).getSerializedName();
    }

    /**
     * @return the {@code VERTICAL_DIRECTION} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getVerticalDirection() {
        return base.getValue(BlockStateProperties.VERTICAL_DIRECTION).getSerializedName();
    }

    /**
     * @return the {@code DRIPSTONE_THICKNESS} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getThickness() {
        return base.getValue(BlockStateProperties.DRIPSTONE_THICKNESS).getSerializedName();
    }

    /**
     * @return the {@code CHEST_TYPE} property's value, as the name the game gives it.
     * example:
     * <pre>
     * // the two kinds of chest the game has
     * for (const type of ["single", "double"]) {
     *   const chest = Client.getRegistryManager()
     *     .getBlockState("minecraft:chest", `[type=${type}]`).getUniversal();
     *   Chat.log(`${chest.getChestType()}`);
     * }
     *
     * // and a block that has no such property
     * try {
     *   Client.getRegistryManager().getBlockState("minecraft:stone").getUniversal().getChestType();
     * } catch (e) {
     *   Chat.log("stone has no chest type");
     * }
     * </pre>
     * @since 1.8.4
     */
    public String getChestType() {
        return base.getValue(BlockStateProperties.CHEST_TYPE).getSerializedName();
    }

    /**
     * @return the {@code MODE_COMPARATOR} property's value, as the name the game gives it.
     * @since 1.8.4
     */
    public String getComparatorMode() {
        return base.getValue(BlockStateProperties.MODE_COMPARATOR).getSerializedName();
    }

    /**
     * Used on cave vine roots.
     *
     * @return the {@code BERRIES} property's value.
     * @since 1.8.4
     */
    public boolean hasBerries() {
        return base.getValue(BlockStateProperties.BERRIES);
    }

    // don't make static, causes crash in main function below
    private final IntegerProperty[] ages = {
            BlockStateProperties.AGE_1,
            BlockStateProperties.AGE_2,
            BlockStateProperties.AGE_3,
            BlockStateProperties.AGE_4,
            BlockStateProperties.AGE_5,
            BlockStateProperties.AGE_7,
            BlockStateProperties.AGE_15,
            BlockStateProperties.AGE_25
    };

    /**
     * The age of this state, read from the first of the game's age properties the state turns out
     * to have. The list is tried in order and the first match wins, so a block that grows through
     * several ages is read off whichever of those properties it actually uses rather than off a
     * single one scaled to fit.
     * <p>
     * A state with none of them is an error rather than a zero.
     * example:
     * <pre>
     * // how grown a crop is
     * const wheat = Client.getRegistryManager().getBlockState("minecraft:wheat").getUniversal();
     * Chat.log(`age ${wheat.getAge()} of ${wheat.getMaxAge()}`);
     *
     * // and a block that is not one of the growing kinds
     * try {
     *   Client.getRegistryManager().getBlockState("minecraft:stone").getUniversal().getAge();
     * } catch (e) {
     *   Chat.log("stone has no age");
     * }
     * </pre>
     *
     * crop age and such
     *
     * @return the age of this state, from the first of the game's age properties it has.
     * @throws IllegalStateException if the state has none of the age properties.
     * @author Wagyourtail
     * @since 1.8.4
     */
    @Ignore({"getAge1", "getAge2", "getAge3", "getAge4", "getAge5", "getAge7", "getAge15", "getAge25"})
    public int getAge() {
        for (IntegerProperty property : ages) {
            if (base.hasProperty(property)) {
                return base.getValue(property);
            }
        }
        throw new IllegalStateException("No age property found");
    }

    /**
     * The largest value the age property this state has could hold, which is how grown a block of
     * this kind can get. The same list of properties is tried as {@link #getAge()}.
     * example:
     * <pre>
     * // the age range of a few things that grow
     * const reg = Client.getRegistryManager();
     * for (const id of ["minecraft:wheat", "minecraft:oak_sapling", "minecraft:sugar_cane"]) {
     *   const grown = reg.getBlockState(id).getUniversal();
     *   Chat.log(`${id}: up to ${grown.getMaxAge()}`);
     * }
     * </pre>
     *
     * @return the largest value the age property this state has can hold.
     * @throws IllegalStateException if the state has none of the age properties.
     * @since 1.8.4
     */
    @Ignore
    public int getMaxAge() {
        for (IntegerProperty property : ages) {
            if (base.hasProperty(property)) {
                return property.getPossibleValues().stream().max(Integer::compareTo).orElse(-1);
            }
        }
        throw new IllegalStateException("No age property found");
    }

    /**
     * Used on cakes.
     *
     * @return the {@code BITES} property's value.
     * @since 1.8.4
     */
    public int getBites() {
        return base.getValue(BlockStateProperties.BITES);
    }

    /**
     * Used on candles.
     *
     * @return the {@code CANDLES} property's value.
     * @since 1.8.4
     */
    public int getCandles() {
        return base.getValue(BlockStateProperties.CANDLES);
    }

    /**
     * Used on turtle eggs.
     *
     * @return the {@code EGGS} property's value.
     * @since 1.8.4
     */
    public int getEggs() {
        return base.getValue(BlockStateProperties.EGGS);
    }

    /**
     * Used on turtle eggs.
     *
     * @return the {@code HATCH} property's value.
     * @since 1.8.4
     */
    @Ignore("getHatch")
    public int getHatched() {
        return base.getValue(BlockStateProperties.HATCH);
    }

    /**
     * Used on snow layers.
     *
     * @return the {@code LAYERS} property's value.
     * @since 1.8.4
     */
    public int getLayers() {
        return base.getValue(BlockStateProperties.LAYERS);
    }

    /**
     * Used on farmland.
     *
     * @return the {@code MOISTURE} property's value.
     * @since 1.8.4
     */
    public int getMoisture() {
        return base.getValue(BlockStateProperties.MOISTURE);
    }

    /**
     * Used on note blocks.
     *
     * @return the {@code NOTE} property's value.
     * @since 1.8.4
     */
    public int getNote() {
        return base.getValue(BlockStateProperties.NOTE);
    }

    /**
     * Used on sea pickles.
     *
     * @return the {@code PICKLES} property's value.
     * @since 1.8.4
     */
    public int getPickles() {
        return base.getValue(BlockStateProperties.PICKLES);
    }

    /**
     * Used on daylight sensors, redstone wires, sculk sensors, target blocks, weighted pressure
     * plates.
     *
     * @return the {@code POWER} property's value.
     * example:
     * <pre>
     * // the power a redstone wire is carrying
     * const reg = Client.getRegistryManager();
     * for (let p = 0; p !== 16; p += 5) {
     *   const wire = reg.getBlockState("minecraft:redstone_wire", `[power=${p}]`).getUniversal();
     *   Chat.log(`power ${wire.getPower()}`);
     * }
     *
     * // and a daylight sensor, which shares the property
     * const sensor = reg.getBlockState("minecraft:daylight_detector", "[power=15]").getUniversal();
     * Chat.log(`sensor reads ${sensor.getPower()}, inverted ${sensor.isInverted()}`);
     * </pre>
     * @since 1.8.4
     */
    public int getPower() {
        return base.getValue(BlockStateProperties.POWER);
    }

    /**
     * Used on bamboo, saplings.
     *
     * @return the {@code STAGE} property's value.
     * @since 1.8.4
     */
    public int getStage() {
        return base.getValue(BlockStateProperties.STAGE);
    }

    /**
     * Used on respawn anchors.
     *
     * @return the {@code RESPAWN_ANCHOR_CHARGES} property's value.
     * @since 1.8.4
     */
    public int getCharges() {
        return base.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES);
    }

    /**
     * Used on sculk sensors.
     *
     * @return the {@code SHRIEKING} property's value.
     * @since 1.8.4
     */
    public boolean isShrieking() {
        return base.getValue(BlockStateProperties.SHRIEKING);
    }

    /**
     * Used on sculk sensors.
     *
     * @return the {@code CAN_SUMMON} property's value.
     * @since 1.8.4
     */
    public boolean canSummon() {
        return base.getValue(BlockStateProperties.CAN_SUMMON);
    }

    /**
     * Used on sculk sensors.
     *
     * @return the {@code SCULK_SENSOR_PHASE} property's value, as the name the game gives it.
     * example:
     * <pre>
     * // the sculk sensor's own phase property, beside the two flags it works from
     * const sensor = Client.getRegistryManager().getBlockState("minecraft:sculk_sensor").getUniversal();
     * Chat.log(`phase ${sensor.getSculkSensorPhase()}`);
     * Chat.log(`shrieking ${sensor.isShrieking()}, can summon ${sensor.canSummon()}`);
     * </pre>
     * @since 1.8.4
     */
    public String getSculkSensorPhase() {
        return base.getValue(BlockStateProperties.SCULK_SENSOR_PHASE).getSerializedName();
    }

    /**
     * @return the {@code BLOOM} property's value.
     * @since 1.8.4
     */
    public boolean isBloom() {
        return base.getValue(BlockStateProperties.BLOOM);
    }

    /**
     * @return the {@code ROTATION_16} property's value.
     * @since 1.8.4
     */
    public int getRotation() {
        return base.getValue(BlockStateProperties.ROTATION_16);
    }

    /**
     * @return the {@code SLOT_0_OCCUPIED} property's value.
     * example:
     * <pre>
     * // the six slots of a chiseled bookshelf
     * for (let i = 0; i !== 6; i += 1) {
     *   let occupied = false;
     *   if (i === 0) {
     *     occupied = Client.getRegistryManager()
     *       .getBlockState("minecraft:chiseled_bookshelf", "[slot0_occupied=true]").getUniversal().isSlot0Occupied();
     *   } else if (i === 1) {
     *     occupied = Client.getRegistryManager()
     *       .getBlockState("minecraft:chiseled_bookshelf", "[slot1_occupied=true]").getUniversal().isSlot1Occupied();
     *   } else if (i === 2) {
     *     occupied = Client.getRegistryManager()
     *       .getBlockState("minecraft:chiseled_bookshelf", "[slot2_occupied=true]").getUniversal().isSlot2Occupied();
     *   } else if (i === 3) {
     *     occupied = Client.getRegistryManager()
     *       .getBlockState("minecraft:chiseled_bookshelf", "[slot3_occupied=true]").getUniversal().isSlot3Occupied();
     *   } else if (i === 4) {
     *     occupied = Client.getRegistryManager()
     *       .getBlockState("minecraft:chiseled_bookshelf", "[slot4_occupied=true]").getUniversal().isSlot4Occupied();
     *   } else {
     *     occupied = Client.getRegistryManager()
     *       .getBlockState("minecraft:chiseled_bookshelf", "[slot5_occupied=true]").getUniversal().isSlot5Occupied();
     *   }
     *   Chat.log(`slot ${i}: ${occupied}`);
     * }
     * </pre>
     * @since 1.8.4
     */
    public boolean isSlot0Occupied() {
        //? if >1.21.8 {
        /*return base.getValue(BlockStateProperties.SLOT_0_OCCUPIED);
        *///?} else {
        return base.getValue(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_0_OCCUPIED);
        //?}
    }

    /**
     * @return the {@code SLOT_1_OCCUPIED} property's value.
     * @since 1.8.4
     */
    public boolean isSlot1Occupied() {
        //? if >1.21.8 {
        /*return base.getValue(BlockStateProperties.SLOT_1_OCCUPIED);
        *///?} else {
        return base.getValue(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_1_OCCUPIED);
        //?}
    }

    /**
     * @return the {@code SLOT_2_OCCUPIED} property's value.
     * @since 1.8.4
     */
    public boolean isSlot2Occupied() {
        //? if >1.21.8 {
        /*return base.getValue(BlockStateProperties.SLOT_2_OCCUPIED);
        *///?} else {
        return base.getValue(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_2_OCCUPIED);
        //?}
    }

    /**
     * @return the {@code SLOT_3_OCCUPIED} property's value.
     * @since 1.8.4
     */
    public boolean isSlot3Occupied() {
        //? if >1.21.8 {
        /*return base.getValue(BlockStateProperties.SLOT_3_OCCUPIED);
        *///?} else {
        return base.getValue(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_3_OCCUPIED);
        //?}
    }

    /**
     * @return the {@code SLOT_4_OCCUPIED} property's value.
     * @since 1.8.4
     */
    public boolean isSlot4Occupied() {
        //? if >1.21.8 {
        /*return base.getValue(BlockStateProperties.SLOT_4_OCCUPIED);
        *///?} else {
        return base.getValue(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_4_OCCUPIED);
        //?}
    }

    /**
     * @return the {@code SLOT_5_OCCUPIED} property's value.
     * @since 1.8.4
     */
    public boolean isSlot5Occupied() {
        //? if >1.21.8 {
        /*return base.getValue(BlockStateProperties.SLOT_5_OCCUPIED);
        *///?} else {
        return base.getValue(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_5_OCCUPIED);
        //?}
    }

    /**
     * @since 1.9.0
     * @return the {@code FLOWER_AMOUNT} property's value.
     */
    public int getFlowerAmount() {
        return base.getValue(BlockStateProperties.FLOWER_AMOUNT);
    }

    /**
     * @since 1.9.0
     * @return the {@code ATTACH_FACE} property's value, as the name the game gives it.
     */
    public String getBlockFace() {
        return base.getValue(BlockStateProperties.ATTACH_FACE).getSerializedName();
    }

    /**
     * @since 1.9.0
     * @return the {@code DUSTED} property's value.
     */
    public int getDusted() {
        return base.getValue(BlockStateProperties.DUSTED);
    }

    /**
     * @since 1.9.0
     * @return the {@code CRACKED} property's value.
     */
    public boolean isCracked() {
        return base.getValue(BlockStateProperties.CRACKED);
    }

    /**
     * @since 2.0.0
     * @return the {@code CRAFTING} property's value.
     */
    public boolean isCrafting() {
        return base.getValue(BlockStateProperties.CRAFTING);
    }

    /**
     * @since 2.0.0
     * @return the {@code TRIAL_SPAWNER_STATE} property's value, as the name the game gives it.
     */
    public String getTrialSpawnerState() {
        return base.getValue(BlockStateProperties.TRIAL_SPAWNER_STATE).getSerializedName();
    }

    /**
     * @since 2.0.0
     * @return the {@code VAULT_STATE} property's value, as the name the game gives it.
     */
    public String getVaultState() {
        return base.getValue(BlockStateProperties.VAULT_STATE).getSerializedName();
    }

    /**
     * @since 2.0.0
     * @return the {@code OMINOUS} property's value.
     */
    public boolean isOminous() {
        return base.getValue(BlockStateProperties.OMINOUS);
    }

    @Ignore
    private static String SCREAMING_SNAKE_CASE_TO_PascalCase(String input) {
        StringBuilder result = new StringBuilder();
        String[] allWords = input.split("_");
        for (String word : allWords) {
            if (word.equalsIgnoreCase("has") || word.equalsIgnoreCase("is") || word.equalsIgnoreCase("can")) {
                continue;
            }
            //test if previous ended with a number and current starts with a number
            if (!result.isEmpty() && Character.isDigit(result.charAt(result.length() - 1))) {
                if (Character.isDigit(word.charAt(0))) {
                    result.append("_");
                }
            }
            result.append(Character.toUpperCase(word.charAt(0)));
            result.append(word.substring(1).toLowerCase());
        }
        return result.toString();
    }

    @Ignore
    public static void main(String[] args) {
        Set<String> properties = new HashSet<>();
        for (Method declaredMethod : UniversalBlockStateHelper.class.getDeclaredMethods()) {
            if (declaredMethod.isAnnotationPresent(Ignore.class)) {
                properties.addAll(Arrays.asList(declaredMethod.getAnnotation(Ignore.class).value()));
            } else {
                properties.add(declaredMethod.getName());
            }
        }

        System.out.println("Properties to add:");

        StringBuilder builder = new StringBuilder();

        for (Field field : BlockStateProperties.class.getDeclaredFields()) {
            if (Property.class.isAssignableFrom(field.getType())) {
                if (BooleanProperty.class.isAssignableFrom(field.getType())) {
                    if (!properties.remove("is" + SCREAMING_SNAKE_CASE_TO_PascalCase(field.getName())) &&
                            !properties.remove("has" + SCREAMING_SNAKE_CASE_TO_PascalCase(field.getName())) &&
                            !properties.remove("can" + SCREAMING_SNAKE_CASE_TO_PascalCase(field.getName()))) {
                        System.out.println("public boolean is" + SCREAMING_SNAKE_CASE_TO_PascalCase(field.getName()) + "() {");
                        System.out.println("    return base.get(Properties." + field.getName() + ");");
                        System.out.println("}");
                        System.out.println();
                    }
                } else {
                    if (!properties.remove("get" + SCREAMING_SNAKE_CASE_TO_PascalCase(field.getName()))) {
                        // get type of property
                        String type = field.getType().getSimpleName();
                        type = type.substring(0, type.length() - "Property".length());
                        // lowercase first letter
                        type = Character.toLowerCase(type.charAt(0)) + type.substring(1);
                        if (type.equals("enum")) {
                            type = "String";
                        }
                        System.out.println("public " + type + " get" + SCREAMING_SNAKE_CASE_TO_PascalCase(field.getName()) + "() {");
                        System.out.print("    return base.get(Properties." + field.getName() + ")");
                        if (type.equals("String")) {
                            System.out.print(".asString()");
                        }
                        System.out.println(";");
                        System.out.println("}");
                        System.out.println();
                    }
                }
            }
        }

        System.out.println("Properties not found:");

        System.out.println(builder);

        for (String property : properties) {
            System.out.println(property);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    private @interface Ignore {
        String[] value() default {};

    }

}
