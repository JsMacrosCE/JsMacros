package com.jsmacrosce.jsmacros.client.api.helper.world;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;

/**
 * Represents one particular state of a block, including its property values. It extends
 * {@link StateHelper} with block-specific queries; {@link #getUniversal()} exposes properties as
 * typed values. States can be obtained from {@link BlockDataHelper#getBlockStateHelper()},
 * {@link BlockHelper#getDefaultState()}, or from the registry with
 * {@link com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper#getBlockState(String)}.
 * <pre>
 * const state = Client.getRegistryManager().getBlockState("minecraft:oak_stairs", "[facing=east]");
 * Chat.log(`${state.getId()}: ${state.toMap().facing} ${state.toMap().half}`);
 *
 * // the properties as typed values rather than as names
 * const universal = state.getUniversal();
 * Chat.log(`facing is ${universal.getHorizontalFacing().getName()}`);
 * Chat.log(`waterlogged is ${universal.isWaterlogged()}`);
 *
 * // and the world's own opinion of the block
 * Chat.log(`solid ${state.isSolid()}, opaque ${state.isOpaque()}, light ${state.getLuminance()}`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.6.5
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class BlockStateHelper extends StateHelper<BlockState> {

    /**
     * Wraps the supplied raw block state without copying or changing it.
     *
     * @param base raw block state to wrap
     */
    public BlockStateHelper(BlockState base) {
        super(base);
    }

    /**
     * Returns the block that owns this state.
     *
     * @return the block the state belongs to.
     * @since 1.6.5
     */
    public BlockHelper getBlock() {
        return new BlockHelper(base.getBlock());
    }

    /**
     * Returns whether this state's block belongs to the supplied block tag. Tag membership is
     * per block, not per state, and is data-driven, so it can differ between loaded datapacks.
     * Unknown tags and tags from other registries return {@code false}.
     *
     * @param tag the block tag id, optionally prefixed with {@code #} or missing its namespace
     * @return {@code true} if this state's block is in the tag
     * @since 2.0.0
     */
    @DocletReplaceParams("tag: CanOmitNamespace<BlockTag>")
    public boolean hasTag(String tag) {
        String id = tag.startsWith("#") ? tag.substring(1) : tag;
        return base.is(TagKey.create(Registries.BLOCK, RegistryHelper.parseIdentifier(id)));
    }

    /**
     * Returns the registry id of the block, independent of this state's properties.
     *
     * @return the block's id.
     * @since 1.8.4
     */
    @DocletReplaceReturn("BlockId")
    public String getId() {
        return BuiltInRegistries.BLOCK.getKey(base.getBlock()).toString();
    }

    /**
     * Returns the fluid state held by this block state. A state with no fluid returns an empty
     * state, not {@code null}; use {@link FluidStateHelper#isEmpty()} to test it.
     *
     * @return the fluid state of this block state.
     * @since 1.8.4
     */
    public FluidStateHelper getFluidState() {
        return new FluidStateHelper(base.getFluidState());
    }

    /**
     * Returns this state's destroy-speed value without a world, position, player, or tool. It is
     * block hardness, not an estimate of breaking time; unbreakable blocks report {@code -1}.
     *
     * @return the hardness.
     * @since 1.6.5
     */
    public float getHardness() {
        return base.getDestroySpeed(null, null);
    }

    /**
     * Returns this state's light emission value.
     *
     * @return the luminance.
     * @since 1.6.5
     */
    public int getLuminance() {
        return base.getLightEmission();
    }

    /**
     * Returns whether this state's block is a redstone signal source.
     *
     * @return {@code true} if the state emits redstone power.
     * @since 1.6.5
     */
    public boolean emitsRedstonePower() {
        return base.isSignalSource();
    }

    /**
     * Returns the game's large-collision-shape flag for this state.
     *
     * @return {@code true} if the state has a large collision shape.
     * @since 1.6.5
     */
    public boolean exceedsCube() {
        return base.hasLargeCollisionShape();
    }

    /**
     * Returns whether this state is marked as air.
     *
     * @return {@code true} if the state is air.
     * @since 1.6.5
     */
    public boolean isAir() {
        return base.isAir();
    }

    /**
     * Returns whether this state can occlude neighboring blocks.
     *
     * @return {@code true} if the state is opaque.
     * @since 1.6.5
     */
    public boolean isOpaque() {
        return base.canOcclude();
    }

    /**
     * Returns whether correct-tool status is required for this state's drops.
     *
     * @return {@code true} if drops require a correct tool.
     * @since 1.6.5
     */
    public boolean isToolRequired() {
        return base.requiresCorrectToolForDrops();
    }

    /**
     * Returns whether this state has a block entity.
     *
     * @return {@code true} if the state has a block entity.
     * @since 1.6.5
     */
    public boolean hasBlockEntity() {
        return base.hasBlockEntity();
    }

    /**
     * Returns whether this state is marked for random ticking.
     *
     * @return {@code true} if the state can be random ticked.
     * @since 1.6.5
     */
    public boolean hasRandomTicks() {
        return base.isRandomlyTicking();
    }

    /**
     * Returns whether this state supports an analog output signal.
     *
     * @return {@code true} if the state has a comparator output.
     * @since 1.6.5
     */
    public boolean hasComparatorOutput() {
        return base.hasAnalogOutputSignal();
    }

    // TODO(26.3): Decide how to handle this deprecation
    /**
     * Returns the piston push-reaction name for this state: {@code NORMAL}, {@code BLOCK},
     * {@code PUSH_ONLY}, {@code DESTROY}, or {@code IGNORE}.
     *
     * @return the piston behaviour of the state.
     * @since 1.6.5
     */
    @DocletReplaceReturn("PistonBehaviour")
    public String getPistonBehaviour() {
        //? if >=26.3 {
        /*return switch (base.getPistonPushReaction()) {
            case PUSH_PULL -> "NORMAL";
            case IMMOVEABLE -> "BLOCK";
            case PUSH -> "PUSH_ONLY";
            case POPPED -> "DESTROY";
            case IGNORE_ENTITY -> "IGNORE";
        };
        *///?} else {
        return switch (base.getPistonPushReaction()) {
            case NORMAL -> "NORMAL";
            case BLOCK -> "BLOCK";
            case PUSH_ONLY -> "PUSH_ONLY";
            case DESTROY -> "DESTROY";
            case IGNORE -> "IGNORE";
        };
        //?}
    }

    /**
     * Returns the legacy movement-blocking flag, excluding cobwebs and bamboo saplings.
     *
     * @return {@code true} if the state blocks the movement of entities.
     * @since 1.6.5
     */
    public boolean blocksMovement() {
        // inlined, blocksMotion() is deprecated on older targets and removed in 26.3
        return !base.is(Blocks.COBWEB)
                && !base.is(Blocks.BAMBOO_SAPLING)
                && base.isSolid();
    }

    /**
     * Returns whether this state is marked as ignitable by lava.
     *
     * @return {@code true} if the state is burnable.
     * @since 1.6.5
     */
    public boolean isBurnable() {
        return base.ignitedByLava();
    }

    /**
     * Returns the state's liquid flag; this does not test whether it contains fluid.
     *
     * @return {@code true} if the state is a liquid.
     * @since 1.6.5
     */
    public boolean isLiquid() {
        return base.liquid();
    }

    /**
     * Returns the state's legacy solid flag; this is distinct from its opacity.
     *
     * @return {@code true} if the state is solid.
     * @since 1.6.5
     */
    public boolean isSolid() {
        return base.isSolid();
    }

    /**
     * Returns whether this state can be replaced in the default placement context.
     *
     * @return {@code true} if the state can be replaced.
     * @since 1.6.5
     */
    public boolean isReplaceable() {
        return base.canBeReplaced();
    }

    /**
     * Returns the game's spawn-validity result for this state, position, and entity type. The
     * result can depend on the supplied position and world, not just on the state.
     *
     * @param pos    the position of the block to check
     * @param entity the entity type to check
     * @return {@code true} if the entity can spawn on this block state at the given position in the
     * current world.
     * @since 1.6.5
     */
    @DocletReplaceParams("pos: BlockPosHelper, entity: CanOmitNamespace<EntityId>")
    public boolean allowsSpawning(BlockPosHelper pos, String entity) {
        return base.isValidSpawn(Minecraft.getInstance().level, pos.getRaw(), BuiltInRegistries.ENTITY_TYPE.getValue(RegistryHelper.parseIdentifier(entity)));
    }

    /**
     * Returns the game's suffocation result for this state at the given position in the currently
     * loaded world.
     *
     * @param pos the position of the block to check
     * @return {@code true} if an entity can suffocate in this block state at the given position in
     * the current world.
     * @since 1.6.5
     */
    public boolean shouldSuffocate(BlockPosHelper pos) {
        return base.isSuffocating(Minecraft.getInstance().level, pos.getRaw());
    }

    /**
     * Returns a helper with typed accessors for the block-state properties it exposes. Unlike
     * {@link StateHelper#toMap()}, these accessors return property values in their typed form; an
     * accessor for a property absent from this state throws rather than returning {@code false}.
     *
     * @return a typed-property view of this state.
     * @since 1.8.4
     */
    public UniversalBlockStateHelper getUniversal() {
        return new UniversalBlockStateHelper(base);
    }

    @Override
    protected StateHelper<BlockState> create(BlockState base) {
        return new BlockStateHelper(base);
    }

    /**
     * Returns a diagnostic representation containing the wrapped block's identifier and property map.
     *
     * @return a diagnostic representation of the wrapped block state.
     */
    @Override
    public String toString() {
        return String.format("BlockStateHelper:{\"id\": \"%s\", \"properties\": %s}", getId(), toMap());
    }

}
