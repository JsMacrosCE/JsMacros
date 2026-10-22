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
 * Describes a block type independently of any world position; {@link BlockStateHelper} represents
 * one of that block's variants.
 * <pre>
 * const block = Client.getRegistryManager().getBlock("stone");
 * Chat.log(`${block.getId()} is called ${block.getName()}`);
 *
 * // inspect its default state and all defined variants
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

    /** Wraps the supplied block type.
     * @param base raw block type to wrap
     */
    public BlockHelper(Block base) {
        super(base);
    }

    /**
     * Returns the block's registered default state.
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
     * Returns the default stack for the item's form of this block. Blocks without a dedicated item
     * use the item returned by Minecraft's block-to-item mapping.
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
     * Tests whether the block's default state is non-solid and non-liquid.
     * @return {@code true} if its default state is non-solid and non-liquid.
     * @since 1.6.5
     */
    public boolean canMobSpawnInside() {
        return base.isPossibleToRespawnInThis(base.defaultBlockState());
    }

    /**
     * Returns whether this block's shape is marked dynamic.
     * @return {@code true} if its shape is dynamic.
     * @since 1.6.5
     */
    public boolean hasDynamicBounds() {
        return base.hasDynamicShape();
    }

    /**
     * Returns this block's explosion resistance.
     * @return the explosion resistance value.
     * @since 1.6.5
     */
    public float getBlastResistance() {
        return base.getExplosionResistance();
    }

    /**
     * Returns this block's jump factor.
     * @return the jump factor multiplier.
     * @since 1.6.5
     */
    public float getJumpVelocityMultiplier() {
        return base.getJumpFactor();
    }

    /**
     * Returns this block's friction value.
     * @return the friction value.
     * @since 1.6.5
     */
    public float getSlipperiness() {
        return base.getFriction();
    }

    /**
     * Returns the block's default destroy-time value, independent of block state. An unbreakable
     * block uses {@code -1}; {@link BlockStateHelper#getHardness()} reports state-specific hardness.
     * @return the default destroy-time value.
     * @since 1.6.5
     */
    public float getHardness() {
        return base.defaultDestroyTime();
    }

    /**
     * Returns this block's entity movement speed factor.
     * @return the speed factor multiplier.
     * @since 1.6.5
     */
    public float getVelocityMultiplier() {
        return base.getSpeedFactor();
    }

    /**
     * Returns the identifiers of the tags assigned to this block, as full ids.
     *
     * @return the full identifiers of the block's tags.
     * @since 1.6.5
     */
    @DocletReplaceReturn("JavaList<BlockTag>")
    public List<String> getTags() {
        return base.builtInRegistryHolder().tags().map(t -> t.location().toString()).collect(Collectors.toList());
    }

    /**
     * Returns every possible state defined for this block.
     *
     * @return all possible block states of the block.
     * @since 1.6.5
     */
    public List<BlockStateHelper> getStates() {
        return base.getStateDefinition().getPossibleStates().stream().map(BlockStateHelper::new).collect(Collectors.toList());
    }

    /**
     * Returns the block's full registry identifier, such as {@code minecraft:stone}.
     *
     * @return the identifier of the block.
     * @since 1.6.5
     */
    @DocletReplaceReturn("BlockId")
    public String getId() {
        return BuiltInRegistries.BLOCK.getKey(base).toString();
    }

    /**
     * Returns the block's display name as text.
     *
     * @return the name of the block.
     * @since 1.8.4
     */
    public TextHelper getName() {
        return TextHelper.wrap(base.getName());
    }

    /** Returns a diagnostic representation containing this block type's identifier.
     * @return diagnostic representation of the wrapped block type
     */
    @Override
    public String toString() {
        return String.format("BlockHelper:{\"id\": \"%s\"}", getId());
    }

}
