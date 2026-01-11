package com.jsmacrosce.jsmacros.client.api.helper.world;

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
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class UniversalBlockStateHelper extends BlockStateHelper {

    public UniversalBlockStateHelper(BlockState base) {
        super(base);
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getAttachment() {
        return base.getValue(BlockStateProperties.BELL_ATTACHMENT).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getEastWallShape() {
        return base.getValue(BlockStateProperties.EAST_WALL).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getNorthWallShape() {
        return base.getValue(BlockStateProperties.NORTH_WALL).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getSouthWallShape() {
        return base.getValue(BlockStateProperties.SOUTH_WALL).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getWestWallShape() {
        return base.getValue(BlockStateProperties.WEST_WALL).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getEastWireConnection() {
        return base.getValue(BlockStateProperties.EAST_REDSTONE).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getNorthWireConnection() {
        return base.getValue(BlockStateProperties.NORTH_REDSTONE).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getSouthWireConnection() {
        return base.getValue(BlockStateProperties.SOUTH_REDSTONE).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getWestWireConnection() {
        return base.getValue(BlockStateProperties.WEST_REDSTONE).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getBlockHalf() {
        return base.getValue(BlockStateProperties.HALF).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getDoubleBlockHalf() {
        return base.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getRailShape() {
        return base.getValue(BlockStateProperties.RAIL_SHAPE).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getStraightRailShape() {
        return base.getValue(BlockStateProperties.RAIL_SHAPE_STRAIGHT).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getOrientation() {
        return base.getValue(BlockStateProperties.ORIENTATION).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getHorizontalAxis() {
        return base.getValue(BlockStateProperties.HORIZONTAL_AXIS).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getAxis() {
        return base.getValue(BlockStateProperties.AXIS).getName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public DirectionHelper getHorizontalFacing() {
        return new DirectionHelper(base.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }

    /**
     * @return
     * @since 1.8.4
     */
    public DirectionHelper getHopperFacing() {
        return new DirectionHelper(base.getValue(BlockStateProperties.FACING_HOPPER));
    }

    /**
     * @return
     * @since 1.8.4
     */
    public DirectionHelper getFacing() {
        return new DirectionHelper(base.getValue(BlockStateProperties.FACING));
    }

    /**
     * @return
     * @since 1.8.4
     */
    public boolean isUp() {
        return base.getValue(BlockStateProperties.UP);
    }

    /**
     * @return
     * @since 1.8.4
     */
    public boolean isDown() {
        return base.getValue(BlockStateProperties.DOWN);
    }

    /**
     * @return
     * @since 1.8.4
     */
    public boolean isNorth() {
        return base.getValue(BlockStateProperties.NORTH);
    }

    /**
     * @return
     * @since 1.8.4
     */
    public boolean isSouth() {
        return base.getValue(BlockStateProperties.SOUTH);
    }

    /**
     * @return
     * @since 1.8.4
     */
    public boolean isEast() {
        return base.getValue(BlockStateProperties.EAST);
    }

    /**
     * @return
     * @since 1.8.4
     */
    public boolean isWest() {
        return base.getValue(BlockStateProperties.WEST);
    }

    /**
     * Used on beehives.
     *
     * @return
     * @since 1.8.4
     */
    public int getHoneyLevel() {
        return base.getValue(BlockStateProperties.LEVEL_HONEY);
    }

    /**
     * Used on scaffolding.
     *
     * @return
     * @since 1.8.4
     */
    public boolean isBottom() {
        return base.getValue(BlockStateProperties.BOTTOM);
    }

    /**
     * Used on bubble columns.
     *
     * @return
     * @since 1.8.4
     */
    @Ignore({"hasDrag"})
    public boolean isBubbleColumnDown() {
        return base.getValue(BlockStateProperties.DRAG);
    }

    /**
     * Used on bubble columns.
     *
     * @return
     * @since 1.8.4
     */
    @Ignore
    public boolean isBubbleColumnUp() {
        return !base.getValue(BlockStateProperties.DRAG);
    }

    /**
     * Used on trip wire hooks.
     *
     * @return
     * @since 1.8.4
     */
    public boolean isAttached() {
        return base.getValue(BlockStateProperties.ATTACHED);
    }

    /**
     * Used on trip wires.
     *
     * @return
     * @since 1.8.4
     */
    public boolean isDisarmed() {
        return base.getValue(BlockStateProperties.DISARMED);
    }

    /**
     * Used on command blocks.
     *
     * @return
     * @since 1.8.4
     */
    public boolean isConditional() {
        return base.getValue(BlockStateProperties.CONDITIONAL);
    }

    /**
     * Used on hoppers.
     *
     * @return
     * @since 1.8.4
     */
    public boolean isEnabled() {
        return base.getValue(BlockStateProperties.ENABLED);
    }

    /**
     * Used on pistons.
     *
     * @return
     * @since 1.8.4
     */
    public boolean isExtended() {
        return base.getValue(BlockStateProperties.EXTENDED);
    }

    /**
     * Used on piston heads.
     *
     * @return
     * @since 1.8.4
     */
    public boolean isShort() {
        return base.getValue(BlockStateProperties.SHORT);
    }

    /**
     * Used on end portal frames.
     *
     * @return
     * @since 1.8.4
     */
    public boolean hasEye() {
        return base.getValue(BlockStateProperties.EYE);
    }

    /**
     * Used on fluids.
     *
     * @return
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
     * Used on fluids and stuff
     *
     * @return
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
     * @return
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
     * @return
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
     * @return
     * @since 1.8.4
     */
    public boolean isHanging() {
        return base.getValue(BlockStateProperties.HANGING);
    }

    /**
     * Used on brewing stands.
     *
     * @return
     * @since 1.8.4
     */
    public boolean hasBottle0() {
        return base.getValue(BlockStateProperties.HAS_BOTTLE_0);
    }

    /**
     * Used on brewing stands.
     *
     * @return
     * @since 1.8.4
     */
    public boolean hasBottle1() {
        return base.getValue(BlockStateProperties.HAS_BOTTLE_1);
    }

    /**
     * Used on brewing stands.
     *
     * @return
     * @since 1.8.4
     */
    public boolean hasBottle2() {
        return base.getValue(BlockStateProperties.HAS_BOTTLE_2);
    }

    /**
     * Used on jukeboxes.
     *
     * @return
     * @since 1.8.4
     */
    public boolean hasRecord() {
        return base.getValue(BlockStateProperties.HAS_RECORD);
    }

    /**
     * Used on lecterns.
     *
     * @return
     * @since 1.8.4
     */
    public boolean hasBook() {
        return base.getValue(BlockStateProperties.HAS_BOOK);
    }

    /**
     * Used on daylight sensors.
     *
     * @return
     * @since 1.8.4
     */
    public boolean isInverted() {
        return base.getValue(BlockStateProperties.INVERTED);
    }

    /**
     * Used on fence gates.
     *
     * @return
     * @since 1.8.4
     */
    public boolean isInWall() {
        return base.getValue(BlockStateProperties.IN_WALL);
    }

    /**
     * Used on fence gates, barrels, trap doors and doors.
     *
     * @return
     * @since 1.8.4
     */
    public boolean isOpen() {
        return base.getValue(BlockStateProperties.OPEN);
    }

    /**
     * Used on candles, all types of furnaces, campfires and redstone torches.
     *
     * @return
     * @since 1.8.4
     */
    public boolean isLit() {
        return base.getValue(BlockStateProperties.LIT);
    }

    /**
     * Used on repeaters.
     *
     * @return
     * @since 1.8.4
     */
    public boolean isLocked() {
        return base.getValue(BlockStateProperties.LOCKED);
    }

    /**
     * Used on repeaters.
     *
     * @return
     * @since 1.8.4
     */
    public int getDelay() {
        return base.getValue(BlockStateProperties.DELAY);
    }

    /**
     * Used on beds.
     *
     * @return
     * @since 1.8.4
     */
    public boolean isOccupied() {
        return base.getValue(BlockStateProperties.OCCUPIED);
    }

    /**
     * Used on leaves.
     *
     * @return
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
     * Used on leaves and scaffold.
     *
     * @return
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
     * Used on leaves and scaffold.
     *
     * @return
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
     * Used on leaves and scaffold.
     *
     * @return
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
     * @return
     * @since 1.8.4
     */
    public boolean isPowered() {
        return base.getValue(BlockStateProperties.POWERED);
    }

    /**
     * Used on campfires.
     *
     * @return
     * @since 1.8.4
     */
    public boolean isSignalFire() {
        return base.getValue(BlockStateProperties.SIGNAL_FIRE);
    }

    /**
     * Used on snowy dirt blocks.
     *
     * @return
     * @since 1.8.4
     */
    public boolean isSnowy() {
        return base.getValue(BlockStateProperties.SNOWY);
    }

    /**
     * Used on dispensers.
     *
     * @return
     * @since 1.8.4
     */
    public boolean isTriggered() {
        return base.getValue(BlockStateProperties.TRIGGERED);
    }

    /**
     * Used on tnt.
     *
     * @return
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
     * @return
     * @since 1.8.4
     */
    public boolean isWaterlogged() {
        return base.getValue(BlockStateProperties.WATERLOGGED);
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getBedPart() {
        return base.getValue(BlockStateProperties.BED_PART).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getDoorHinge() {
        return base.getValue(BlockStateProperties.DOOR_HINGE).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getInstrument() {
        return base.getValue(BlockStateProperties.NOTEBLOCK_INSTRUMENT).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getPistonType() {
        return base.getValue(BlockStateProperties.PISTON_TYPE).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getSlabType() {
        return base.getValue(BlockStateProperties.SLAB_TYPE).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getStairShape() {
        return base.getValue(BlockStateProperties.STAIRS_SHAPE).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getStructureBlockMode() {
        return base.getValue(BlockStateProperties.STRUCTUREBLOCK_MODE).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getBambooLeaves() {
        return base.getValue(BlockStateProperties.BAMBOO_LEAVES).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getTilt() {
        return base.getValue(BlockStateProperties.TILT).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getVerticalDirection() {
        return base.getValue(BlockStateProperties.VERTICAL_DIRECTION).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getThickness() {
        return base.getValue(BlockStateProperties.DRIPSTONE_THICKNESS).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getChestType() {
        return base.getValue(BlockStateProperties.CHEST_TYPE).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public String getComparatorMode() {
        return base.getValue(BlockStateProperties.MODE_COMPARATOR).getSerializedName();
    }

    /**
     * Used on cave vine roots.
     *
     * @return
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
     * crop age and such
     *
     * @return
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
     * @return
     * @since 1.8.4
     */
    public int getBites() {
        return base.getValue(BlockStateProperties.BITES);
    }

    /**
     * Used on candles.
     *
     * @return
     * @since 1.8.4
     */
    public int getCandles() {
        return base.getValue(BlockStateProperties.CANDLES);
    }

    /**
     * Used on turtle eggs.
     *
     * @return
     * @since 1.8.4
     */
    public int getEggs() {
        return base.getValue(BlockStateProperties.EGGS);
    }

    /**
     * Used on turtle eggs.
     *
     * @return
     * @since 1.8.4
     */
    @Ignore("getHatch")
    public int getHatched() {
        return base.getValue(BlockStateProperties.HATCH);
    }

    /**
     * Used on snow layers.
     *
     * @return
     * @since 1.8.4
     */
    public int getLayers() {
        return base.getValue(BlockStateProperties.LAYERS);
    }

    /**
     * Used on farmland.
     *
     * @return
     * @since 1.8.4
     */
    public int getMoisture() {
        return base.getValue(BlockStateProperties.MOISTURE);
    }

    /**
     * Used on note blocks.
     *
     * @return
     * @since 1.8.4
     */
    public int getNote() {
        return base.getValue(BlockStateProperties.NOTE);
    }

    /**
     * Used on sea pickles.
     *
     * @return
     * @since 1.8.4
     */
    public int getPickles() {
        return base.getValue(BlockStateProperties.PICKLES);
    }

    /**
     * Used on daylight sensors, redstone wires, sculk sensors, target blocks, weighted pressure
     * plates.
     *
     * @return
     * @since 1.8.4
     */
    public int getPower() {
        return base.getValue(BlockStateProperties.POWER);
    }

    /**
     * Used on bamboo, saplings.
     *
     * @return
     * @since 1.8.4
     */
    public int getStage() {
        return base.getValue(BlockStateProperties.STAGE);
    }

    /**
     * Used on respawn anchors.
     *
     * @return
     * @since 1.8.4
     */
    public int getCharges() {
        return base.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES);
    }

    /**
     * Used on sculk sensors.
     *
     * @return
     * @since 1.8.4
     */
    public boolean isShrieking() {
        return base.getValue(BlockStateProperties.SHRIEKING);
    }

    /**
     * Used on sculk sensors.
     *
     * @return
     * @since 1.8.4
     */
    public boolean canSummon() {
        return base.getValue(BlockStateProperties.CAN_SUMMON);
    }

    /**
     * Used on sculk sensors.
     *
     * @return
     * @since 1.8.4
     */
    public String getSculkSensorPhase() {
        return base.getValue(BlockStateProperties.SCULK_SENSOR_PHASE).getSerializedName();
    }

    /**
     * @return
     * @since 1.8.4
     */
    public boolean isBloom() {
        return base.getValue(BlockStateProperties.BLOOM);
    }

    /**
     * @return
     * @since 1.8.4
     */
    public int getRotation() {
        return base.getValue(BlockStateProperties.ROTATION_16);
    }

    /**
     * @return
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
     * @return
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
     * @return
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
     * @return
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
     * @return
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
     * @return
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
     * @return
     */
    public int getFlowerAmount() {
        return base.getValue(BlockStateProperties.FLOWER_AMOUNT);
    }

    /**
     * @since 1.9.0
     * @return
     */
    public String getBlockFace() {
        return base.getValue(BlockStateProperties.ATTACH_FACE).getSerializedName();
    }

    /**
     * @since 1.9.0
     * @return
     */
    public int getDusted() {
        return base.getValue(BlockStateProperties.DUSTED);
    }

    /**
     * @since 1.9.0
     * @return
     */
    public boolean isCracked() {
        return base.getValue(BlockStateProperties.CRACKED);
    }

    /**
     * @since 2.0.0
     */
    public boolean isCrafting() {
        return base.getValue(BlockStateProperties.CRAFTING);
    }

    /**
     * @since 2.0.0
     */
    public String getTrialSpawnerState() {
        return base.getValue(BlockStateProperties.TRIAL_SPAWNER_STATE).getSerializedName();
    }

    /**
     * @since 2.0.0
     */
    public String getVaultState() {
        return base.getValue(BlockStateProperties.VAULT_STATE).getSerializedName();
    }

    /**
     * @since 2.0.0
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
