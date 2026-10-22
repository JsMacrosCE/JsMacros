package com.jsmacrosce.jsmacros.client.api.helper.world;

import com.jsmacrosce.doclet.DocletCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A kind of block rather than a block in the world: stone is one of these, and so is every
 * individual block of stone that has ever been placed. It is what the registry hands out and what
 * {@link BlockStateHelper#getBlock()} gives back, and it knows the block's own properties without
 * needing a position or a world.<br>
 * A block on its own is only half a thing in a modern game, since most blocks come in variants: the
 * block is the kind, and a {@link BlockStateHelper} is one particular variant of it. The state
 * answers everything this class does and more, so reach for this one when what is wanted is the
 * kind itself, such as the item that places it or the list of every variant.
 * example:
 * <pre>
 * const block = Client.getRegistryManager().getBlock("stone");
 * Chat.log(`${block.getId()} is called ${block.getName()}`);
 *
 * // the block on its own, and the state it comes in by default
 * Chat.log(`default state has ${block.getDefaultState().getId()}`);
 * Chat.log(`and ${block.getStates().size()} states in total`);
 *
 * // the item that places it
 * const stack = block.getDefaultItemStack();
 * Chat.log(`${stack.getItemId()} holds up to ${stack.getMaxCount()}`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.6.5
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class BlockHelper extends BaseHelper<Block> {

    public BlockHelper(Block base) {
        super(base);
    }

    /**
     * The block's default state, which is the variant the game treats as the plain form of this
     * block. For a block whose variants are all equal it is the only one, and for a block that
     * comes in shapes or facings it is whichever the game picked as the plain one.
     * example:
     * <pre>
     * // the plain form of a block that comes in variants
     * const stairs = Client.getRegistryManager().getBlock("oak_stairs");
     * const plain = stairs.getDefaultState();
     * for (const [name, value] of plain.toMap()) {
     *   Chat.log(`${name} is ${value}`);
     * }
     *
     * // and the same block, a named variant instead
     * const turned = Client.getRegistryManager().getBlockState("oak_stairs", "[facing=east]");
     * Chat.log(`${plain.toMap().facing} versus ${turned.toMap().facing}`);
     * </pre>
     *
     * @return the default state of the block.
     * @since 1.6.5
     */
    public BlockStateHelper getDefaultState() {
        return new BlockStateHelper(base.defaultBlockState());
    }

    /**
     * A stack of one of the item that places this block, with nothing on it. For a block that has no
     * item form, such as water or a piston that is extended, the item the game substitutes comes
     * back instead, so this is not a way of finding out whether a block is placeable.
     * example:
     * <pre>
     * // the item that places a block, as a stack of one
     * const block = Client.getRegistryManager().getBlock("oak_stairs");
     * const stack = block.getDefaultItemStack();
     * Chat.log(`${stack.getItemId()} x ${stack.getCount()}`);
     *
     * // a stack of sixty-four of it, ready to place
     * const many = stack.copy().getCreative().setCount(64);
     * Chat.log(`${many.getCount()} of ${many.getItemId()}`);
     * </pre>
     *
     * @return the default item stack of the block.
     * @since 1.6.5
     */
    public ItemStackHelper getDefaultItemStack() {
        return new ItemStackHelper(base.asItem().getDefaultInstance());
    }

    /**
     * Whether a mob is allowed to spawn inside this block. This is asked of the block's default
     * state and comes down to two things about it: it is not solid, and it is not a liquid. A block
     * with a shape that leaves a gap answers true, and a solid one does not.<br>
     * It is the default state that is asked about, so for a block that comes in variants this is the
     * answer for whichever one the game treats as the plain form.
     * example:
     * <pre>
     * // which of a handful of blocks leave room for a mob inside them
     * for (const id of ["minecraft:stone", "minecraft:oak_slab", "minecraft:cake", "minecraft:water"]) {
     *   const block = Client.getRegistryManager().getBlock(id);
     *   Chat.log(`${id}: ${block.canMobSpawnInside()}`);
     * }
     * </pre>
     *
     * @return {@code true} if a mob may spawn inside this block's default state.
     * @since 1.6.5
     */
    public boolean canMobSpawnInside() {
        return base.isPossibleToRespawnInThis(base.defaultBlockState());
    }

    /**
     * Whether the block's outline depends on something other than the block itself, such as what it
     * is joined to. This is a property of the kind of block, so every state of it answers the same.
     * example:
     * <pre>
     * // a fence changes shape depending on its neighbours
     * const fence = Client.getRegistryManager().getBlock("oak_fence");
     * Chat.log(`a fence has dynamic bounds: ${fence.hasDynamicBounds()}`);
     *
     * const stone = Client.getRegistryManager().getBlock("stone");
     * Chat.log(`stone does not: ${stone.hasDynamicBounds()}`);
     * </pre>
     *
     * @return {@code true} if the block has dynamic bounds.
     * @since 1.6.5
     */
    public boolean hasDynamicBounds() {
        return base.hasDynamicShape();
    }

    /**
     * How well the block stands up to an explosion, as the resistance value the game uses. A
     * larger number is harder to blow up.
     * example:
     * <pre>
     * // compare what is hard and what is not to blow up
     * for (const id of ["minecraft:obsidian", "minecraft:dirt", "minecraft:bedrock"]) {
     *   const block = Client.getRegistryManager().getBlock(id);
     *   Chat.log(`${id}: ${block.getBlastResistance()}`);
     * }
     * </pre>
     *
     * @return the blast resistance.
     * @since 1.6.5
     */
    public float getBlastResistance() {
        return base.getExplosionResistance();
    }

    /**
     * How much faster the player jumps while springing off this block, as a multiplier. A value of
     * one is an ordinary jump.
     * example:
     * <pre>
     * // the spring blocks
     * for (const id of ["minecraft:slime_block", "minecraft:honey_block", "minecraft:stone"]) {
     *   const block = Client.getRegistryManager().getBlock(id);
     *   Chat.log(`${id} multiplies jump speed by ${block.getJumpVelocityMultiplier()}`);
     * }
     * </pre>
     *
     * @return the jump velocity multiplier.
     * @since 1.6.5
     */
    public float getJumpVelocityMultiplier() {
        return base.getJumpFactor();
    }

    /**
     * How slippery the block's top is, as the friction value the game uses for it. A larger number
     * is more slippery, and a value of one is an ordinary surface to walk on.
     * example:
     * <pre>
     * // ice and blue ice against ordinary stone
     * for (const id of ["minecraft:stone", "minecraft:ice", "minecraft:blue_ice", "minecraft:slime_block"]) {
     *   const block = Client.getRegistryManager().getBlock(id);
     *   Chat.log(`${id} has friction ${block.getSlipperiness()}`);
     * }
     * </pre>
     *
     * @return the slipperiness.
     * @since 1.6.5
     */
    public float getSlipperiness() {
        return base.getFriction();
    }

    /**
     * How long the block takes to break, as the game's own value for it. A block that cannot be
     * broken at all reports {@code -1}, which is the value the game itself reads as unbreakable
     * rather than as a hardness of any size.<br>
     * This is the block's number and takes no account of the block state, so a block whose states
     * differ still gives one answer. {@link BlockStateHelper#getHardness()} is the per-state form.
     * example:
     * <pre>
     * // the block's own number, including the unbreakable case
     * for (const id of ["minecraft:dirt", "minecraft:stone", "minecraft:obsidian", "minecraft:bedrock"]) {
     *   const block = Client.getRegistryManager().getBlock(id);
     *   Chat.log(`${id} has hardness ${block.getHardness()}`);
     * }
     *
     * // and whether a tool is needed at all, which the state answers
     * const stone = Client.getRegistryManager().getBlock("stone");
     * Chat.log(`stone needs a tool: ${stone.getDefaultState().isToolRequired()}`);
     * </pre>
     *
     * @return the hardness.
     * @since 1.6.5
     */
    public float getHardness() {
        return base.defaultDestroyTime();
    }

    /**
     * How much faster entities move over this block's surface, as a multiplier. A value of one means
     * no change, and the game's ice uses a value well above that.
     * example:
     * <pre>
     * // how much a surface speeds things up
     * for (const id of ["minecraft:stone", "minecraft:ice", "minecraft:packed_mud"]) {
     *   const block = Client.getRegistryManager().getBlock(id);
     *   Chat.log(`${id} multiplies speed by ${block.getVelocityMultiplier()}`);
     * }
     * </pre>
     *
     * @return the velocity multiplier.
     * @since 1.6.5
     */
    public float getVelocityMultiplier() {
        return base.getSpeedFactor();
    }

    /**
     * Every block tag the block is in, as the tag ids themselves in full. The tags are how the game
     * groups blocks for recipes, for what tools mine them, and for the creative tabs, so this is
     * the way to ask what a block is a member of rather than what it is.
     * example:
     * <pre>
     * // everything a block is a member of
     * const block = Client.getRegistryManager().getBlock("oak_planks");
     * const tags = block.getTags();
     * Chat.log(`${block.getId()} is in ${tags.size()} tags`);
     * for (let i = 0; i !== tags.size(); i += 1) {
     *   Chat.log(`  ${tags.get(i)}`);
     * }
     *
     * // a block that is in no tags at all
     * Chat.log(Client.getRegistryManager().getBlock("air").getTags().size());
     * </pre>
     *
     * @return all tags of the block as an {@link java.util.ArrayList ArrayList}.
     * @since 1.6.5
     */
    @DocletReplaceReturn("JavaList<BlockTag>")
    public List<String> getTags() {
        return base.builtInRegistryHolder().tags().map(t -> t.location().toString()).collect(Collectors.toList());
    }

    /**
     * Every state this block has, all of them at once. A block that comes in variants has a good
     * many of these, so the list is worth filtering rather than reading through: the number of
     * states is the product of the sizes of its properties.
     * example:
     * <pre>
     * // how many variants a block has, and a few of them
     * const block = Client.getRegistryManager().getBlock("oak_stairs");
     * const states = block.getStates();
     * Chat.log(`${block.getId()} has ${states.size()} states`);
     * for (let i = 0; i !== states.size(); i += 1) {
     *   if (i === 3) {
     *     break;
     *   }
     *   const props = states.get(i).toMap();
     *   Chat.log(`  one of them: ${props.facing} ${props.half} ${props.shape}`);
     * }
     *
     * // the same list, filtered to the solid ones
     * let solid = 0;
     * for (let i = 0; i !== states.size(); i += 1) {
     *   if (states.get(i).isSolid()) {
     *     solid += 1;
     *   }
     * }
     * Chat.log(`${solid} of them are solid`);
     * </pre>
     *
     * @return all possible block states of the block.
     * @since 1.6.5
     */
    public List<BlockStateHelper> getStates() {
        return base.getStateDefinition().getPossibleStates().stream().map(BlockStateHelper::new).collect(Collectors.toList());
    }

    /**
     * The block's registry id, in full, so {@code minecraft:stone}. This is the same string
     * {@link com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper#getBlock(String)} takes, so it
     * can be handed straight back to look the block up again.
     * example:
     * <pre>
     * const reg = Client.getRegistryManager();
     * const block = reg.getBlock("stone");
     * // the id comes back in full even when it was asked for short
     * Chat.log(block.getId());
     * Chat.log(reg.getBlock(block.getId()).getId());
     * </pre>
     *
     * @return the identifier of the block.
     * @since 1.6.5
     */
    @DocletReplaceReturn("BlockId")
    public String getId() {
        return BuiltInRegistries.BLOCK.getKey(base).toString();
    }

    /**
     * The block's translated name, which is the word the game shows for it in the language the
     * player has picked. It is a text rather than a string, so it can carry formatting.
     * example:
     * <pre>
     * const block = Client.getRegistryManager().getBlock("oak_stairs");
     * Chat.log(block.getName().getString());
     * </pre>
     *
     * @return the name of the block.
     * @since 1.8.4
     */
    public TextHelper getName() {
        return TextHelper.wrap(base.getName());
    }

    @Override
    public String toString() {
        return String.format("BlockHelper:{\"id\": \"%s\"}", getId());
    }

}
