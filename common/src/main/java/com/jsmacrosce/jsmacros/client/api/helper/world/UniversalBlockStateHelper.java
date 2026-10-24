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
 * Provides typed accessors for common block-state properties. Unlike
 * {@link StateHelper#toMap()}, which returns property values as names, these accessors return
 * values such as directions and numbers in their typed form. Accessing a property absent from the
 * state throws. The level, distance, and age accessors try supported properties in order and throw
 * if none is present. Obtain this helper from {@link BlockStateHelper#getUniversal()}.
 * <pre>
 * // Compare a typed direction with the map's property-name value.
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
 * // Accessing an absent property throws.
 * try {
 *   universal.getHoneyLevel();
 * } catch (e) {
 *   Chat.log("a stair has no honey level");
 * }
 *
 * // Numeric families try their supported properties in order.
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

    /**
     * Wraps the supplied block-state reference.
     *
     * @param base non-null block state to wrap
     */
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
     * @return the serialized value of the {@code HALF} property.
     * @since 1.8.4
     */
    public String getBlockHalf() {
        return base.getValue(BlockStateProperties.HALF).getSerializedName();
    }

    /**
     * @return the serialized value of the {@code DOUBLE_BLOCK_HALF} property.
     * @since 1.8.4
     */
    public String getDoubleBlockHalf() {
        return base.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF).getSerializedName();
    }

    /**
     * @return the serialized value of the {@code RAIL_SHAPE} property.
     * @since 1.8.4
     */
    public String getRailShape() {
        return base.getValue(BlockStateProperties.RAIL_SHAPE).getSerializedName();
    }

    /**
     * @return the serialized value of the {@code RAIL_SHAPE_STRAIGHT} property.
     * @since 1.8.4
     */
    public String getStraightRailShape() {
        return base.getValue(BlockStateProperties.RAIL_SHAPE_STRAIGHT).getSerializedName();
    }

    /**
     * @return the serialized value of the {@code ORIENTATION} property.
     * @since 1.8.4
     */
    public String getOrientation() {
        return base.getValue(BlockStateProperties.ORIENTATION).getSerializedName();
    }

    /**
     * @return the serialized value of the {@code HORIZONTAL_AXIS} property.
     * @since 1.8.4
     */
    public String getHorizontalAxis() {
        return base.getValue(BlockStateProperties.HORIZONTAL_AXIS).getSerializedName();
    }

    /**
     * @return the name of the {@code AXIS} property's value.
     * @since 1.8.4
     */
    public String getAxis() {
        return base.getValue(BlockStateProperties.AXIS).getName();
    }

    /**
     * @return the value of the {@code HORIZONTAL_FACING} property as a direction.
     * @since 1.8.4
     */
    public DirectionHelper getHorizontalFacing() {
        return new DirectionHelper(base.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }

    /**
     * @return the value of the {@code FACING_HOPPER} property as a direction.
     * @since 1.8.4
     */
    public DirectionHelper getHopperFacing() {
        return new DirectionHelper(base.getValue(BlockStateProperties.FACING_HOPPER));
    }

    /**
     * @return the value of the {@code FACING} property as a direction.
     * @since 1.8.4
     */
    public DirectionHelper getFacing() {
        return new DirectionHelper(base.getValue(BlockStateProperties.FACING));
    }

    /**
     * @return whether the state's {@code UP} property is true.
     * @since 1.8.4
     */
    public boolean isUp() {
        return base.getValue(BlockStateProperties.UP);
    }

    /**
     * @return whether the state's {@code DOWN} property is true.
     * @since 1.8.4
     */
    public boolean isDown() {
        return base.getValue(BlockStateProperties.DOWN);
    }

    /**
     * @return whether the state's {@code NORTH} property is true.
     * @since 1.8.4
     */
    public boolean isNorth() {
        return base.getValue(BlockStateProperties.NORTH);
    }

    /**
     * @return whether the state's {@code SOUTH} property is true.
     * @since 1.8.4
     */
    public boolean isSouth() {
        return base.getValue(BlockStateProperties.SOUTH);
    }

    /**
     * @return whether the state's {@code EAST} property is true.
     * @since 1.8.4
     */
    public boolean isEast() {
        return base.getValue(BlockStateProperties.EAST);
    }

    /**
     * @return whether the state's {@code WEST} property is true.
     * @since 1.8.4
     */
    public boolean isWest() {
        return base.getValue(BlockStateProperties.WEST);
    }

    /**
     * @return the value of the {@code LEVEL_HONEY} property (0–5).
     * @since 1.8.4
     */
    public int getHoneyLevel() {
        return base.getValue(BlockStateProperties.LEVEL_HONEY);
    }

    /**
     * @return whether the state's {@code BOTTOM} property is true.
     * @since 1.8.4
     */
    public boolean isBottom() {
        return base.getValue(BlockStateProperties.BOTTOM);
    }

    /**
     * @return the {@code DRAG} property value; {@code true} indicates downward bubble-column motion.
     * @since 1.8.4
     */
    @Ignore({"hasDrag"})
    public boolean isBubbleColumnDown() {
        return base.getValue(BlockStateProperties.DRAG);
    }

    /**
     * @return the negation of the {@code DRAG} property; {@code true} indicates upward bubble-column motion.
     * @since 1.8.4
     */
    @Ignore
    public boolean isBubbleColumnUp() {
        return !base.getValue(BlockStateProperties.DRAG);
    }

    /**
     * @return the {@code ATTACHED} property's value; present on tripwire hooks.
     * @since 1.8.4
     */
    public boolean isAttached() {
        return base.getValue(BlockStateProperties.ATTACHED);
    }

    /**
     * @return the {@code DISARMED} property's value; present on tripwire.
     * @since 1.8.4
     */
    public boolean isDisarmed() {
        return base.getValue(BlockStateProperties.DISARMED);
    }

    /**
     * @return the {@code CONDITIONAL} property's value; present on command blocks.
     * @since 1.8.4
     */
    public boolean isConditional() {
        return base.getValue(BlockStateProperties.CONDITIONAL);
    }

    /**
     * @return the {@code ENABLED} property's value; present on hoppers.
     * @since 1.8.4
     */
    public boolean isEnabled() {
        return base.getValue(BlockStateProperties.ENABLED);
    }

    /**
     * @return the {@code EXTENDED} property's value; present on pistons.
     * @since 1.8.4
     */
    public boolean isExtended() {
        return base.getValue(BlockStateProperties.EXTENDED);
    }

    /**
     * @return the {@code SHORT} property's value; present on piston heads.
     * @since 1.8.4
     */
    public boolean isShort() {
        return base.getValue(BlockStateProperties.SHORT);
    }

    /**
     * @return the {@code EYE} property's value; present on end portal frames.
     * @since 1.8.4
     */
    public boolean hasEye() {
        return base.getValue(BlockStateProperties.EYE);
    }

    /**
     * @return the {@code FALLING} property's value; present on fluids.
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
     * Returns the value of the first present level property, checking flowing-fluid, cauldron,
     * composter, then generic level.
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
     * @return the value of the first present level property.
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
     * Returns the greatest allowed value of the first present level property, checking the same
     * properties in the same order as {@link #getLevel()}.
     * <pre>
     * // the range a level can take here
     * const lava = Client.getRegistryManager().getFluidState("minecraft:lava")
     *   .getBlockState().getUniversal();
     * Chat.log(`${lava.getMinLevel()} to ${lava.getMaxLevel()}`);
     * </pre>
     *
     * @return the greatest allowed value of the first present level property.
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
     * Returns the smallest allowed value of the first present level property, using the same
     * property order as {@link #getLevel()}.
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
     * Returns the state's {@code HANGING} property.
     * @return whether the state's {@code HANGING} property is true
     * @since 1.8.4
     */
    public boolean isHanging() {
        return base.getValue(BlockStateProperties.HANGING);
    }

    /**
     * Returns the state's {@code HAS_BOTTLE_0} property.
     * @return whether the state's {@code HAS_BOTTLE_0} property is true
     * @since 1.8.4
     */
    public boolean hasBottle0() {
        return base.getValue(BlockStateProperties.HAS_BOTTLE_0);
    }

    /**
     * Returns the state's {@code HAS_BOTTLE_1} property.
     * @return whether the state's {@code HAS_BOTTLE_1} property is true
     * @since 1.8.4
     */
    public boolean hasBottle1() {
        return base.getValue(BlockStateProperties.HAS_BOTTLE_1);
    }

    /**
     * Returns the state's {@code HAS_BOTTLE_2} property.
     * @return whether the state's {@code HAS_BOTTLE_2} property is true
     * @since 1.8.4
     */
    public boolean hasBottle2() {
        return base.getValue(BlockStateProperties.HAS_BOTTLE_2);
    }

    /**
     * Returns the state's {@code HAS_RECORD} property.
     * @return whether the state's {@code HAS_RECORD} property is true
     * @since 1.8.4
     */
    public boolean hasRecord() {
        return base.getValue(BlockStateProperties.HAS_RECORD);
    }

    /**
     * Returns the state's {@code HAS_BOOK} property.
     * @return whether the state's {@code HAS_BOOK} property is true
     * @since 1.8.4
     */
    public boolean hasBook() {
        return base.getValue(BlockStateProperties.HAS_BOOK);
    }

    /**
     * Returns the state's {@code INVERTED} property.
     * @return whether the state's {@code INVERTED} property is true
     * @since 1.8.4
     */
    public boolean isInverted() {
        return base.getValue(BlockStateProperties.INVERTED);
    }

    /**
     * Returns the state's {@code IN_WALL} property.
     * @return whether the state's {@code IN_WALL} property is true
     * @since 1.8.4
     */
    public boolean isInWall() {
        return base.getValue(BlockStateProperties.IN_WALL);
    }

    /**
     * Returns the state's {@code OPEN} property.
     * @return whether the state's {@code OPEN} property is true
     * @since 1.8.4
     */
    public boolean isOpen() {
        return base.getValue(BlockStateProperties.OPEN);
    }

    /**
     * Returns the state's {@code LIT} property.
     * @return the {@code LIT} property's value.
     * @since 1.8.4
     */
    public boolean isLit() {
        return base.getValue(BlockStateProperties.LIT);
    }

    /**
     * Returns the state's {@code LOCKED} property.
     * @return the {@code LOCKED} property's value.
     * @since 1.8.4
     */
    public boolean isLocked() {
        return base.getValue(BlockStateProperties.LOCKED);
    }

    /**
     * Returns the state's {@code DELAY} property.
     * @return the {@code DELAY} property's value.
     * @since 1.8.4
     */
    public int getDelay() {
        return base.getValue(BlockStateProperties.DELAY);
    }

    /**
     * Returns the state's {@code OCCUPIED} property.
     * @return the {@code OCCUPIED} property's value.
     * @since 1.8.4
     */
    public boolean isOccupied() {
        return base.getValue(BlockStateProperties.OCCUPIED);
    }

    /**
     * Returns the state's {@code PERSISTENT} property.
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
     * Returns the first present property in order: {@code STABILITY_DISTANCE}, then
     * {@code DISTANCE}. Throws if neither property is present.
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
     * Returns the maximum possible value of the first present distance property, in the order
     * described by {@link #getDistance()}.
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
     * Returns the minimum possible value of the first present distance property, in the order
     * described by {@link #getDistance()}.
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
     * Returns the state's {@code POWERED} property.
     * @return the {@code POWERED} property's value.
     * @since 1.8.4
     */
    public boolean isPowered() {
        return base.getValue(BlockStateProperties.POWERED);
    }

    /**
     * Returns the state's {@code SIGNAL_FIRE} property.
     * @return the {@code SIGNAL_FIRE} property's value.
     * @since 1.8.4
     */
    public boolean isSignalFire() {
        return base.getValue(BlockStateProperties.SIGNAL_FIRE);
    }

    /**
     * Returns the state's {@code SNOWY} property.
     *
     * @return the {@code SNOWY} property's value.
     * @since 1.8.4
     */
    public boolean isSnowy() {
        return base.getValue(BlockStateProperties.SNOWY);
    }

    /**
     * Returns the state's {@code TRIGGERED} property.
     *
     * @return the {@code TRIGGERED} property's value.
     * @since 1.8.4
     */
    public boolean isTriggered() {
        return base.getValue(BlockStateProperties.TRIGGERED);
    }

    /**
     * Returns the state's {@code UNSTABLE} property.
     *
     * @return the {@code UNSTABLE} property's value.
     * @since 1.8.4
     */
    public boolean isUnstable() {
        return base.getValue(BlockStateProperties.UNSTABLE);
    }

    /**
     * Returns whether this state's {@code WATERLOGGED} property is true.
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
     *
     * @return the {@code WATERLOGGED} property's value
     * @since 1.8.4
     */
    public boolean isWaterlogged() {
        return base.getValue(BlockStateProperties.WATERLOGGED);
    }

    /**
     * @return the serialized name of the {@code BED_PART} property's value.
     * @since 1.8.4
     */
    public String getBedPart() {
        return base.getValue(BlockStateProperties.BED_PART).getSerializedName();
    }

    /**
     * Returns the door hinge's serialized name: {@code left} or {@code right}.
     * <pre>
     * // which side a door is hinged on
     * const door = Client.getRegistryManager()
     *   .getBlockState("minecraft:oak_door", "[hinge=left,open=true]").getUniversal();
     * Chat.log(`hinge ${door.getDoorHinge()}, open ${door.isOpen()}`);
     * </pre>
     *
     * @return the serialized name of the {@code DOOR_HINGE} property's value
     * @since 1.8.4
     */
    public String getDoorHinge() {
        return base.getValue(BlockStateProperties.DOOR_HINGE).getSerializedName();
    }

    /**
     * Returns the note-block instrument's serialized name, such as {@code harp} or {@code bell}.
     * <pre>
     * // what a note block is set to
     * const reg = Client.getRegistryManager();
     * for (const name of ["harp", "basedrum", "snare", "hat", "guitar", "bell", "chime"]) {
     *   const note = reg.getBlockState("minecraft:note_block", `[instrument=${name}]`).getUniversal();
     *   Chat.log(`${note.getInstrument()} is note ${note.getNote()}`);
     * }
     * </pre>
     *
     * @return the serialized name of the {@code NOTEBLOCK_INSTRUMENT} property's value
     * @since 1.8.4
     */
    public String getInstrument() {
        return base.getValue(BlockStateProperties.NOTEBLOCK_INSTRUMENT).getSerializedName();
    }

    /**
     * Returns the piston-head type's serialized name: {@code normal} or {@code sticky}.
     * <pre>
     * // sticky and not
     * for (const type of ["normal", "sticky"]) {
     *   const piston = Client.getRegistryManager()
     *     .getBlockState("minecraft:piston_head", `[type=${type},facing=up]`).getUniversal();
     *   Chat.log(`${piston.getPistonType()}, facing ${piston.getFacing().getName()}`);
     * }
     * </pre>
     *
     * @return the serialized name of the {@code PISTON_TYPE} property's value
     * @since 1.8.4
     */
    public String getPistonType() {
        return base.getValue(BlockStateProperties.PISTON_TYPE).getSerializedName();
    }

    /**
     * @return the serialized name of the {@code SLAB_TYPE} property's value.
     * @since 1.8.4
     */
    public String getSlabType() {
        return base.getValue(BlockStateProperties.SLAB_TYPE).getSerializedName();
    }

    /**
     * Returns the stair shape's serialized name: {@code straight}, {@code inner_left},
     * {@code inner_right}, {@code outer_left}, or {@code outer_right}.
     * <pre>
     * // the five shapes a stair can be
     * for (const shape of ["straight", "inner_left", "inner_right", "outer_left", "outer_right"]) {
     *   const stair = Client.getRegistryManager()
     *     .getBlockState("minecraft:oak_stairs", `[shape=${shape}]`).getUniversal();
     *   Chat.log(`${stair.getStairShape()}, half ${stair.getBlockHalf()}`);
     * }
     * </pre>
     *
     * @return the serialized name of the {@code STAIRS_SHAPE} property's value
     * @since 1.8.4
     */
    public String getStairShape() {
        return base.getValue(BlockStateProperties.STAIRS_SHAPE).getSerializedName();
    }

    /**
     * @return the serialized name of the {@code STRUCTUREBLOCK_MODE} property.
     * @since 1.8.4
     */
    public String getStructureBlockMode() {
        return base.getValue(BlockStateProperties.STRUCTUREBLOCK_MODE).getSerializedName();
    }

    /**
     * @return the serialized name of the {@code BAMBOO_LEAVES} property.
     * @since 1.8.4
     */
    public String getBambooLeaves() {
        return base.getValue(BlockStateProperties.BAMBOO_LEAVES).getSerializedName();
    }

    /**
     * @return the serialized name of the {@code TILT} property.
     * @since 1.8.4
     */
    public String getTilt() {
        return base.getValue(BlockStateProperties.TILT).getSerializedName();
    }

    /**
     * @return the serialized name of the {@code VERTICAL_DIRECTION} property.
     * @since 1.8.4
     */
    public String getVerticalDirection() {
        return base.getValue(BlockStateProperties.VERTICAL_DIRECTION).getSerializedName();
    }

    /**
     * @return the serialized name of the {@code DRIPSTONE_THICKNESS}/{@code SPELEOTHEM_THICKNESS} property.
     * @since 1.8.4
     */
    public String getThickness() {
        //? if >=26.2 {
        /*return base.getValue(BlockStateProperties.SPELEOTHEM_THICKNESS).getSerializedName();
        *///?} else {
        return base.getValue(BlockStateProperties.DRIPSTONE_THICKNESS).getSerializedName();
        //?}
    }

    /**
     * @return the serialized name of the {@code CHEST_TYPE} property.
     * @since 1.8.4
     */
    public String getChestType() {
        return base.getValue(BlockStateProperties.CHEST_TYPE).getSerializedName();
    }

    /**
     * @return the serialized name of the {@code MODE_COMPARATOR} property.
     * @since 1.8.4
     */
    public String getComparatorMode() {
        return base.getValue(BlockStateProperties.MODE_COMPARATOR).getSerializedName();
    }

    /**
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
     * Returns the value of the first present property in the helper's configured order: {@code AGE_1},
     * {@code AGE_2}, {@code AGE_3}, {@code AGE_4}, {@code AGE_5}, {@code AGE_7}, {@code AGE_15},
     * then {@code AGE_25}. No scaling is applied between properties.
     *
     * @return the value of the first present age property.
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
     * Returns the maximum possible value of the first present age property, using the same property
     * order as {@link #getAge()}.
     *
     * @return the maximum possible value of the first present age property.
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
     * @return the {@code BITES} property's value.
     * @since 1.8.4
     */
    public int getBites() {
        return base.getValue(BlockStateProperties.BITES);
    }

    /**
     * @return the {@code CANDLES} property's value.
     * @since 1.8.4
     */
    public int getCandles() {
        return base.getValue(BlockStateProperties.CANDLES);
    }

    /**
     * @return the {@code EGGS} property's value.
     * @since 1.8.4
     */
    public int getEggs() {
        return base.getValue(BlockStateProperties.EGGS);
    }

    /**
     * @return the {@code HATCH} property's value.
     * @since 1.8.4
     */
    @Ignore("getHatch")
    public int getHatched() {
        return base.getValue(BlockStateProperties.HATCH);
    }

    /**
     * @return the {@code LAYERS} property's value.
     * @since 1.8.4
     */
    public int getLayers() {
        return base.getValue(BlockStateProperties.LAYERS);
    }

    /**
     * @return the {@code MOISTURE} property's value.
     * @since 1.8.4
     */
    public int getMoisture() {
        return base.getValue(BlockStateProperties.MOISTURE);
    }

    /**
     * @return the {@code NOTE} property's value.
     * @since 1.8.4
     */
    public int getNote() {
        return base.getValue(BlockStateProperties.NOTE);
    }

    /**
     * @return the {@code PICKLES} property's value.
     * @since 1.8.4
     */
    public int getPickles() {
        return base.getValue(BlockStateProperties.PICKLES);
    }

    /**
     * @return the {@code POWER} property's value.
     * @since 1.8.4
     */
    public int getPower() {
        return base.getValue(BlockStateProperties.POWER);
    }

    /**
     * @return the {@code STAGE} property's value.
     * @since 1.8.4
     */
    public int getStage() {
        return base.getValue(BlockStateProperties.STAGE);
    }

    /**
     * @return the respawn anchor's charge count.
     * @since 1.8.4
     */
    public int getCharges() {
        return base.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES);
    }

    /**
     * @return whether this sculk sensor is shrieking.
     * @since 1.8.4
     */
    public boolean isShrieking() {
        return base.getValue(BlockStateProperties.SHRIEKING);
    }

    /**
     * @return whether this sculk sensor can summon.
     * @since 1.8.4
     */
    public boolean canSummon() {
        return base.getValue(BlockStateProperties.CAN_SUMMON);
    }

    /**
     * @return the sculk sensor phase's serialized name.
     * @since 1.8.4
     */
    public String getSculkSensorPhase() {
        return base.getValue(BlockStateProperties.SCULK_SENSOR_PHASE).getSerializedName();
    }

    /**
     * @return whether this block state has bloom enabled.
     * @since 1.8.4
     */
    public boolean isBloom() {
        return base.getValue(BlockStateProperties.BLOOM);
    }

    /**
     * @return the block's rotation in one of 16 steps.
     * @since 1.8.4
     */
    public int getRotation() {
        return base.getValue(BlockStateProperties.ROTATION_16);
    }

    /**
     * @return whether chiseled bookshelf slot 0 is occupied.
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
     * @return whether chiseled bookshelf slot 4 is occupied.
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
     * @return whether chiseled bookshelf slot 5 is occupied.
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
     * @return the flower bed's flower count.
     */
    public int getFlowerAmount() {
        return base.getValue(BlockStateProperties.FLOWER_AMOUNT);
    }

    /**
     * @since 1.9.0
     * @return the attachment face's serialized name.
     */
    public String getBlockFace() {
        return base.getValue(BlockStateProperties.ATTACH_FACE).getSerializedName();
    }

    /**
     * @since 1.9.0
     * @return the copper bulb's oxidation level.
     */
    public int getDusted() {
        return base.getValue(BlockStateProperties.DUSTED);
    }

    /**
     * @since 1.9.0
     * @return whether the decorated pot is cracked.
     */
    public boolean isCracked() {
        return base.getValue(BlockStateProperties.CRACKED);
    }

    /**
     * @since 2.0.0
     * @return whether the crafter is crafting.
     */
    public boolean isCrafting() {
        return base.getValue(BlockStateProperties.CRAFTING);
    }

    /**
     * @since 2.0.0
     * @return the trial spawner state's serialized name.
     */
    public String getTrialSpawnerState() {
        return base.getValue(BlockStateProperties.TRIAL_SPAWNER_STATE).getSerializedName();
    }

    /**
     * @since 2.0.0
     * @return the vault state's serialized name.
     */
    public String getVaultState() {
        return base.getValue(BlockStateProperties.VAULT_STATE).getSerializedName();
    }

    /**
     * @since 2.0.0
     * @return whether the vault is ominous.
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

    /**
     * Prints candidate accessor snippets for declared block-state properties without a matching
     * accessor, followed by method names left unmatched by the diagnostic. The printed snippets
     * are suggestions, not guaranteed compilable source.
     *
     * @param args unused command-line arguments
     */
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
