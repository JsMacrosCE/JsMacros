package com.jsmacrosce.jsmacros.client.api.helper.world;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;

/**
 * One particular state of a block: not just stone, but this stair facing that way with that shape.
 * A state is what the world actually holds, what a block position gives back, and what a placement
 * screen expects.<br>
 * It is a {@link StateHelper}, so it answers {@link StateHelper#toMap()} and
 * {@link StateHelper#with(String, String)} as well as everything here, and the one thing those two
 * cannot do is read a property as the right type. That is what {@link #getUniversal()} is for: it
 * gives back a helper with a named method for every property the game knows about, which is what a
 * script wants when it already knows which property it is after.<br>
 * A script gets a state from {@link BlockDataHelper#getBlockStateHelper()}, from
 * {@link BlockHelper#getDefaultState()}, or from the registry with
 * {@link com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper#getBlockState(String)}.
 * example:
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

    public BlockStateHelper(BlockState base) {
        super(base);
    }

    /**
     * The kind of block this state belongs to, with the variant left off. A stair state and a plain
     * stair block both give the same answer here.
     * example:
     * <pre>
     * // the block behind a state, and the states that block has
     * const state = Client.getRegistryManager().getBlockState("minecraft:oak_stairs", "[facing=east]");
     * const block = state.getBlock();
     * Chat.log(`${state.getId()} is the block ${block.getId()}`);
     * Chat.log(`which has ${block.getStates().size()} states`);
     * </pre>
     *
     * @return the block the state belongs to.
     * @since 1.6.5
     */
    public BlockHelper getBlock() {
        return new BlockHelper(base.getBlock());
    }

    /**
     * The registry id of the block this state belongs to, in full. This is the block's id and not
     * the state's, so two different states of the same block give the same string.
     * example:
     * <pre>
     * // the id names the block, not the variant
     * const reg = Client.getRegistryManager();
     * const a = reg.getBlockState("minecraft:oak_stairs", "[facing=north]");
     * const b = reg.getBlockState("minecraft:oak_stairs", "[facing=east]");
     * Chat.log(a.getId());
     * Chat.log(b.getId());
     * </pre>
     *
     * @return the block's id.
     * @since 1.8.4
     */
    @DocletReplaceReturn("BlockId")
    public String getId() {
        return BuiltInRegistries.BLOCK.getKey(base.getBlock()).toString();
    }

    /**
     * The fluid this state holds, which is a state of its own and not a block. Every state answers
     * this, and one that holds no fluid answers with the empty fluid state rather than
     * {@code null}, so {@link FluidStateHelper#isEmpty()} is the test for whether there is one.
     * example:
     * <pre>
     * // is the block a player is standing in made of a fluid
     * const block = World.getBlock(Player.getPlayer().getBlockPos());
     * if (block !== null) {
     *   const fluid = block.getBlockStateHelper().getFluidState();
     *   if (fluid.isEmpty()) {
     *     Chat.log("not a fluid block");
     *   } else {
     *     Chat.log(`${fluid.getId()} at level ${fluid.getLevel()}`);
     *   }
     * }
     * </pre>
     *
     * @return the fluid state of this block state.
     * @since 1.8.4
     */
    public FluidStateHelper getFluidState() {
        return new FluidStateHelper(base.getFluidState());
    }

    /**
     * How long this state takes to break, as the game's own number. This is the same value
     * {@link BlockHelper#getHardness()} gives, since the number belongs to the block rather than to
     * the variant, and a block that cannot be broken at all reports {@code -1}.
     * <p>
     * Nothing about the world is consulted, so this is the raw hardness rather than a break time:
     * how long it actually takes also depends on the tool and on the player's speed.
     * example:
     * <pre>
     * // the raw number, with no tool or player taken into account
     * const state = Client.getRegistryManager().getBlockState("minecraft:obsidian");
     * Chat.log(`obsidian has hardness ${state.getHardness()}`);
     * Chat.log(`and the block agrees: ${state.getBlock().getHardness()}`);
     *
     * // a tool being required is a separate question from how hard it is
     * Chat.log(`and it needs the right tool: ${state.isToolRequired()}`);
     * </pre>
     *
     * @return the hardness.
     * @since 1.6.5
     */
    public float getHardness() {
        return base.getDestroySpeed(null, null);
    }

    /**
     * How much light this state gives off, on the game's scale of zero to fifteen. Zero is a block
     * that emits nothing at all, and a torch answers well above that.
     * example:
     * <pre>
     * // which blocks light a room
     * for (const id of ["minecraft:stone", "minecraft:torch", "minecraft:sea_lantern", "minecraft:glowstone"]) {
     *   const state = Client.getRegistryManager().getBlockState(id);
     *   Chat.log(`${id} emits ${state.getLuminance()}`);
     * }
     *
     * // a lit block in the world, rather than a hypothetical one
     * const near = World.getEntities(8);
     * if (near !== null) {
     *   Chat.log(`${near.size()} entities within eight blocks`);
     * }
     * </pre>
     *
     * @return the luminance.
     * @since 1.6.5
     */
    public int getLuminance() {
        return base.getLightEmission();
    }

    /**
     * Whether this state gives off redstone power on its own, which is what the game checks when it
     * is deciding whether a neighbour is powered. A block answers this by saying so for itself, and
     * a block that only carries a signal along rather than making one answers false.
     * example:
     * <pre>
     * // which of these are power sources in their own right
     * for (const id of ["minecraft:redstone_block", "minecraft:redstone_wire", "minecraft:stone"]) {
     *   const state = Client.getRegistryManager().getBlockState(id);
     *   Chat.log(`${id} emits power: ${state.emitsRedstonePower()}`);
     * }
     * </pre>
     *
     * @return {@code true} if the state emits redstone power.
     * @since 1.6.5
     */
    public boolean emitsRedstonePower() {
        return base.isSignalSource();
    }

    /**
     * Whether this state's outline is bigger than a single block's, which is the test the game uses
     * for whether the block reaches into its neighbours. A block whose outline depends on what it is
     * joined to has no fixed outline to measure and always answers true.
     * example:
     * <pre>
     * // a wall reaches out, a plain block does not
     * for (const id of ["minecraft:stone", "minecraft:cobblestone_wall", "minecraft:oak_fence"]) {
     *   const state = Client.getRegistryManager().getBlockState(id);
     *   Chat.log(`${id} exceeds a cube: ${state.exceedsCube()}`);
     * }
     * </pre>
     *
     * @return {@code true} if the shape of the state is a cube.
     * @since 1.6.5
     */
    public boolean exceedsCube() {
        return base.hasLargeCollisionShape();
    }

    /**
     * Whether this state is air. This is a property of the block rather than a guess from the
     * outline, so it is true for the air block and for the void, and false for anything with a
     * shape however empty that shape looks.
     * example:
     * <pre>
     * // air, and the things that are not air despite looking like it
     * for (const id of ["minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:glass"]) {
     *   const state = Client.getRegistryManager().getBlockState(id);
     *   Chat.log(`${id} is air: ${state.isAir()}`);
     * }
     * </pre>
     *
     * @return {@code true} if the state is air.
     * @since 1.6.5
     */
    public boolean isAir() {
        return base.isAir();
    }

    /**
     * Whether this state blocks light and sight through it. Glass answers false even though it has
     * a full block's outline, so this is not the same question as being solid.
     * example:
     * <pre>
     * // what you can see through
     * for (const id of ["minecraft:stone", "minecraft:glass", "minecraft:oak_slab", "minecraft:water"]) {
     *   const state = Client.getRegistryManager().getBlockState(id);
     *   Chat.log(`${id} is opaque: ${state.isOpaque()}`);
     * }
     * </pre>
     *
     * @return {@code true} if the state is opaque.
     * @since 1.6.5
     */
    public boolean isOpaque() {
        return base.canOcclude();
    }

    /**
     * Whether the correct tool has to be used for this state to drop anything, which is the flag
     * the game checks before deciding whether a block breaks into its items. A block with the flag
     * set still breaks with the wrong tool; it just drops nothing.
     * example:
     * <pre>
     * // which blocks insist on the right tool
     * for (const id of ["minecraft:stone", "minecraft:dirt", "minecraft:diamond_ore", "minecraft:wooden_pickaxe"]) {
     *   const state = Client.getRegistryManager().getBlockState(id);
     *   Chat.log(`${id} needs a tool: ${state.isToolRequired()}`);
     * }
     * </pre>
     *
     * @return {@code true} if a tool is required to mine the block.
     * @since 1.6.5
     */
    public boolean isToolRequired() {
        return base.requiresCorrectToolForDrops();
    }

    /**
     * Whether this state carries a block entity, which is what decides whether the game keeps extra
     * data for the block: a chest's contents, a sign's text, a spawner's mob.
     * example:
     * <pre>
     * // which of these keep data of their own
     * for (const id of ["minecraft:chest", "minecraft:stone", "minecraft:oak_sign", "minecraft:furnace"]) {
     *   const state = Client.getRegistryManager().getBlockState(id);
     *   Chat.log(`${id} has a block entity: ${state.hasBlockEntity()}`);
     * }
     *
     * // and what that data actually is, read off a placed block
     * const placed = World.getBlock(0, 64, 0);
     * if (placed !== null) {
     *   const nbt = placed.getNBT();
     *   if (nbt !== null) {
     *     Chat.log(nbt.asString());
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if the state has a block entity.
     * @since 1.6.5
     */
    public boolean hasBlockEntity() {
        return base.hasBlockEntity();
    }

    /**
     * Whether the game ticks this state on its own from time to time. Grass spreading, a crop
     * growing and leaves decaying are all things that come out of this flag, and it is set on the
     * block rather than worked out.
     * example:
     * <pre>
     * // which blocks change on their own over time
     * for (const id of ["minecraft:grass_block", "minecraft:wheat", "minecraft:stone", "minecraft:sapling"]) {
     *   const state = Client.getRegistryManager().getBlockState(id);
     *   Chat.log(`${id} ticks on its own: ${state.hasRandomTicks()}`);
     * }
     * </pre>
     *
     * @return {@code true} if the state can be random ticked.
     * @since 1.6.5
     */
    public boolean hasRandomTicks() {
        return base.isRandomlyTicking();
    }

    /**
     * Whether this state has a comparator output to read. A container answers true and its reading
     * is what fills the comparator, and a block with nothing to report answers false.
     * example:
     * <pre>
     * // which blocks a comparator can read
     * for (const id of ["minecraft:chest", "minecraft:barrel", "minecraft:hopper", "minecraft:stone"]) {
     *   const state = Client.getRegistryManager().getBlockState(id);
     *   Chat.log(`${id} has a comparator output: ${state.hasComparatorOutput()}`);
     * }
     * </pre>
     *
     * @return {@code true} if the state has a comparator output.
     * @since 1.6.5
     */
    public boolean hasComparatorOutput() {
        return base.hasAnalogOutputSignal();
    }

    /**
     * What a piston does when it is asked to move this state: {@code NORMAL}, {@code BLOCK},
     * {@code PUSH_ONLY}, {@code DESTROY} or {@code IGNORE}. A piston will not push a chest, will
     * pull one but not push it, and will simply break one it is pointed at, which is what these
     * five spell out.
     * example:
     * <pre>
     * // what a piston will and will not move
     * for (const id of ["minecraft:stone", "minecraft:chest", "minecraft:water", "minecraft:obsidian"]) {
     *   const state = Client.getRegistryManager().getBlockState(id);
     *   Chat.log(`${id}: ${state.getPistonBehaviour()}`);
     * }
     * </pre>
     *
     * @return the piston behaviour of the state.
     * @since 1.6.5
     */
    @DocletReplaceReturn("PistonBehaviour")
    public String getPistonBehaviour() {
        switch (base.getPistonPushReaction()) {
            case NORMAL:
                return "NORMAL";
            case BLOCK:
                return "BLOCK";
            case PUSH_ONLY:
                return "PUSH_ONLY";
            case DESTROY:
                return "DESTROY";
            case IGNORE:
                return "IGNORE";
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Whether this state stops anything from moving through it. A block with a gap in it answers
     * false even though it is in the way of part of the space, and water is not in the way of
     * anything that swims.
     * example:
     * <pre>
     * // which of these are in the way
     * for (const id of ["minecraft:stone", "minecraft:oak_slab", "minecraft:water", "minecraft:oak_fence"]) {
     *   const state = Client.getRegistryManager().getBlockState(id);
     *   Chat.log(`${id} blocks movement: ${state.blocksMovement()}`);
     * }
     * </pre>
     *
     * @return {@code true} if the state blocks the movement of entities.
     * @since 1.6.5
     */
    public boolean blocksMovement() {
        return base.blocksMotion();
    }

    /**
     * Whether this state can be set alight by lava, which is the flag the game checks before it
     * turns a neighbour into fire.
     * example:
     * <pre>
     * // what lava next to this would set on fire
     * for (const id of ["minecraft:oak_planks", "minecraft:stone", "minecraft:oak_log", "minecraft:dirt"]) {
     *   const state = Client.getRegistryManager().getBlockState(id);
     *   Chat.log(`${id} is burnable: ${state.isBurnable()}`);
     * }
     * </pre>
     *
     * @return {@code true} if the state is burnable.
     * @since 1.6.5
     */
    public boolean isBurnable() {
        return base.ignitedByLava();
    }

    /**
     * Whether this state is a liquid, which is a property of the block rather than a look at the
     * block that holds the fluid: water and lava are liquid, and a waterlogged fence is not, though
     * it does hold water.
     * example:
     * <pre>
     * // liquid blocks, and a block that merely holds a fluid
     * for (const id of ["minecraft:water", "minecraft:lava", "minecraft:oak_fence"]) {
     *   const state = Client.getRegistryManager().getBlockState(id, "[waterlogged=true]");
     *   Chat.log(`${id}: liquid ${state.isLiquid()}, holds ${state.getFluidState().getId()}`);
     * }
     * </pre>
     *
     * @return {@code true} if the state is a liquid.
     * @since 1.6.5
     */
    public boolean isLiquid() {
        return base.liquid();
    }

    /**
     * Whether this state is solid, which is the flag the game uses for the old full-cube notion of
     * solid: a block answers true when the game has decided it fills its space, and false both for
     * something with a gap in it and for something whose outline depends on its neighbours, since
     * there is nothing fixed to decide about.
     * example:
     * <pre>
     * // solid is not the same as opaque, and not the same as full
     * for (const id of ["minecraft:stone", "minecraft:glass", "minecraft:oak_slab", "minecraft:oak_fence"]) {
     *   const state = Client.getRegistryManager().getBlockState(id);
     *   Chat.log(`${id}: solid ${state.isSolid()}, opaque ${state.isOpaque()}`);
     * }
     * </pre>
     *
     * @return {@code true} if the state is solid.
     * @since 1.6.5
     */
    public boolean isSolid() {
        return base.isSolid();
    }

    /**
     * This will return true for blocks like air and grass, that can be replaced without breaking
     * them first. The flag is set on the block when it is registered rather than worked out from
     * the outline, so a block with a gap in it is not replaceable unless it was registered that way.
     * example:
     * <pre>
     * // what a block can be built straight over
     * for (const id of ["minecraft:air", "minecraft:short_grass", "minecraft:water", "minecraft:stone"]) {
     *   const state = Client.getRegistryManager().getBlockState(id);
     *   Chat.log(`${id} is replaceable: ${state.isReplaceable()}`);
     * }
     *
     * // the same question about the block standing at a position
     * const under = World.getBlock(Player.getPlayer().getBlockPos().down());
     * if (under !== null) {
     *   Chat.log(`the ground here is replaceable: ${under.getBlockStateHelper().isReplaceable()}`);
     * }
     * </pre>
     *
     * @return {@code true} if the state can be replaced.
     * @since 1.6.5
     */
    public boolean isReplaceable() {
        return base.canBeReplaced();
    }

    /**
     * Whether a mob of the given type may spawn on this state at the given position, in the world
     * that is currently loaded. This is a question about the world rather than about the block, so
     * the answer depends on what else is there: a block may be a fine place for a sheep and a poor
     * one for a squid, and the same block at the same coordinates gives different answers in
     * different surroundings. With no world joined there is nothing to ask about and the call
     * cannot be made.
     * example:
     * <pre>
     * // what could spawn on the block under the player
     * const pos = Player.getPlayer().getBlockPos().down();
     * const state = World.getBlock(pos);
     * if (state !== null) {
     *   const block = state.getBlockStateHelper();
     *   for (const mob of ["minecraft:sheep", "minecraft:pig", "minecraft:spider"]) {
     *     Chat.log(`${mob} on ${block.getId()}: ${block.allowsSpawning(pos, mob)}`);
     *   }
     * }
     * </pre>
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
     * Whether a mob inside this state at the given position would suffocate, which is what the game
     * checks when it decides whether a mob is stuck. Like {@link #allowsSpawning(BlockPosHelper, String)}
     * this is asked of the world that is loaded rather than of the block on its own.
     * example:
     * <pre>
     * // could a mob suffocate in the block at the player's feet
     * const pos = Player.getPlayer().getBlockPos();
     * const state = World.getBlock(pos);
     * if (state !== null) {
     *   Chat.log(`a mob inside that would suffocate: ${state.getBlockStateHelper().shouldSuffocate(pos)}`);
     * }
     * </pre>
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
     * A view of this state with a named method for every block state property the game knows about,
     * each one returning the value already typed rather than as a name. This is the form to use when
     * the property is already known by name: {@code getUniversal().isWaterlogged()} rather than
     * digging a string out of {@link StateHelper#toMap()}.<br>
     * The methods read the property straight out of the state, so asking one a state does not have
     * is an error rather than a false.
     * example:
     * <pre>
     * const state = Client.getRegistryManager().getBlockState("minecraft:oak_stairs", "[facing=east]");
     * const universal = state.getUniversal();
     *
     * // typed reads of the properties
     * Chat.log(`facing ${universal.getHorizontalFacing().getName()}`);
     * Chat.log(`half ${universal.getBlockHalf()}`);
     * Chat.log(`waterlogged ${universal.isWaterlogged()}`);
     *
     * // the one number-valued family, which also throws when the block has none
     * try {
     *   Chat.log(`age ${universal.getAge()}`);
     * } catch (e) {
     *   Chat.log("stairs have no age");
     * }
     * </pre>
     *
     * @return an {@link UniversalBlockStateHelper} to access all properties of this block state.
     * @since 1.8.4
     */
    public UniversalBlockStateHelper getUniversal() {
        return new UniversalBlockStateHelper(base);
    }

    @Override
    protected StateHelper<BlockState> create(BlockState base) {
        return new BlockStateHelper(base);
    }

    @Override
    public String toString() {
        return String.format("BlockStateHelper:{\"id\": \"%s\", \"properties\": %s}", getId(), toMap());
    }

}
