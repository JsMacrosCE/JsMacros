package com.jsmacrosce.jsmacros.client.api.helper.world;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Etheradon
 * @since 1.6.5
 */
@SuppressWarnings("unused")
public class BlockHelper extends BaseHelper<Block> {

    public BlockHelper(Block base) {
        super(base);
    }

    /**
     * @return the default state of the block.
     * @since 1.6.5
     */
    public BlockStateHelper getDefaultState() {
        return new BlockStateHelper(base.defaultBlockState());
    }

    /**
     * @return the default item stack of the block.
     * @since 1.6.5
     */
    public ItemStackHelper getDefaultItemStack() {
        return new ItemStackHelper(base.asItem().getDefaultInstance());
    }

    public boolean canMobSpawnInside() {
        return base.isPossibleToRespawnInThis(base.defaultBlockState());
    }

    /**
     * @return {@code true} if the block has dynamic bounds.
     * @since 1.6.5
     */
    public boolean hasDynamicBounds() {
        return base.hasDynamicShape();
    }

    /**
     * @return the blast resistance.
     * @since 1.6.5
     */
    public float getBlastResistance() {
        return base.getExplosionResistance();
    }

    /**
     * @return the jump velocity multiplier.
     * @since 1.6.5
     */
    public float getJumpVelocityMultiplier() {
        return base.getJumpFactor();
    }

    /**
     * @return the slipperiness.
     * @since 1.6.5
     */
    public float getSlipperiness() {
        return base.getFriction();
    }

    /**
     * @return the hardness.
     * @since 1.6.5
     */
    public float getHardness() {
        return base.defaultDestroyTime();
    }

    /**
     * @return the velocity multiplier.
     * @since 1.6.5
     */
    public float getVelocityMultiplier() {
        return base.getSpeedFactor();
    }

    /**
     * @return all tags of the block as an {@link java.util.ArrayList ArrayList}.
     * @since 1.6.5
     */
    @DocletReplaceReturn("JavaList<BlockTag>")
    public List<String> getTags() {
        return base.builtInRegistryHolder().tags().map(t -> t.location().toString()).collect(Collectors.toList());
    }

    /**
     * @return all possible block states of the block.
     * @since 1.6.5
     */
    public List<BlockStateHelper> getStates() {
        return base.getStateDefinition().getPossibleStates().stream().map(BlockStateHelper::new).collect(Collectors.toList());
    }

    /**
     * @return the identifier of the block.
     * @since 1.6.5
     */
    @DocletReplaceReturn("BlockId")
    public String getId() {
        return BuiltInRegistries.BLOCK.getKey(base).toString();
    }

    /**
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
