package com.jsmacrosce.jsmacros.client.api.helper.world;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.jsmacros.client.api.classes.RegistryHelper;
import com.jsmacrosce.jsmacros.client.api.helper.NBTElementHelper;
import com.jsmacrosce.jsmacros.client.api.helper.TextHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.Util;

/**
 * A block state, its position, and its optional block entity, returned by world block lookups.
 * {@link #getNBT()} returns {@code null} when no block entity was captured.
 * <pre>
 * const block = World.getBlock(Player.getPlayer().getBlockPos().down());
 * if (block !== null) {
 *   Chat.log(`${block.getId()} at ${block.getBlockPos()}`);
 *   Chat.log(`it is ${block.getName()} and solid is ${block.getBlockStateHelper().isSolid()}`);
 *
 *   // the extra data, for the blocks that keep some
 *   const nbt = block.getNBT();
 *   if (nbt === null) {
 *     Chat.log("no block entity here");
 *   } else {
 *     Chat.log(nbt.asString());
 *   }
 * }
 * </pre>
 *
 * @author Wagyourtail
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class BlockDataHelper extends BaseHelper<BlockState> {
    private static final Minecraft mc = Minecraft.getInstance();

    private final Block b;
    private final BlockPos bp;
    private final BlockEntity e;

    /**
     * Stores the supplied block state, its block type, optional block entity, and position.
     * The state must be non-null; the position is retained as supplied.
     *
     * @param b block state to wrap; must be non-null
     * @param e block entity, or {@code null} when absent
     * @param bp block position to store
     */
    public BlockDataHelper(BlockState b, BlockEntity e, BlockPos bp) {
        super(b);
        this.b = b.getBlock();
        this.bp = bp;
        this.e = e;
    }

    /**
      * The block's {@code x} coordinate.
     *
     * @return the {@code x} value of the block.
     * @since 1.1.7
     */
    public int getX() {
        return bp.getX();
    }

    /**
      * The block's {@code y} coordinate.
     *
     * @return the {@code y} value of the block.
     * @since 1.1.7
     */
    public int getY() {
        return bp.getY();
    }

    /**
      * The block's {@code z} coordinate.
     *
     * @return the {@code z} value of the block.
     * @since 1.1.7
     */
    public int getZ() {
        return bp.getZ();
    }

    /**
     * The block's registry id, independent of its state properties.
     *
     * @return the block's registry id.
     */
    @DocletReplaceReturn("BlockId")
    public String getId() {
        return BuiltInRegistries.BLOCK.getKey(b).toString();
    }

    /**
     * The block's translated name, independent of its state.
     *
     * @return the translated name of the block. (was string before 1.6.5)
     */
    public TextHelper getName() {
        return TextHelper.wrap(b.getName());
    }

    /**
     * Serializes the captured block entity to a compound on each call, excluding its position and id.
     *
     * @return the block entity's data, or {@code null} if this block has no block entity.
     * @since 1.5.1, used to be a {@link Map} of {@link String} to {@link String}
     */
    @Nullable
    public NBTElementHelper.NBTCompoundHelper getNBT() {
        return e == null ? null : NBTElementHelper.wrapCompound(e.saveWithoutMetadata(RegistryHelper.getWrapperLookup()));
    }

    /**
     * Returns a helper for this block's state and its properties.
     *
     * @return a helper over the state this block is in.
     * @since 1.6.5
     */
    public BlockStateHelper getBlockStateHelper() {
        return new BlockStateHelper(base);
    }

    /**
     * The kind of block this is, with the state left off.
     *
     * @return a helper over the kind of block.
     * @since 1.6.5
     * @deprecated use {@link #getBlock()} instead.
     */
    @DocletIgnore
    @Deprecated
    public BlockHelper getBlockHelper() {
        return getBlock();
    }

    /**
     * Returns a helper for the block kind, without its state properties.
     *
     * @return the block
     * @since 1.6.5
     */
    public BlockHelper getBlock() {
        return new BlockHelper(base.getBlock());
    }

    /**
     * Returns every state property keyed by its name, with the current game-defined value name.
     * This matches {@link StateHelper#toMap()} on {@link #getBlockStateHelper()}.
     *
     * @return the property names and their current values.
     * @since 1.1.7
     */
    public Map<String, String> getBlockState() {
        Map<String, String> map = new HashMap<>();
        //? if >=26.1 {
        /*base.getValues().forEach(e -> map.put(e.property().getName(), Util.getPropertyName(e.property(), e.value())));
        *///?} else {
        base.getValues().forEach((key, value) -> map.put(key.getName(), Util.getPropertyName(key, value)));
        //?}
        return map;
    }

    /**
     * Returns the position stored when this block helper was created.
     *
     * @return the block pos.
     * @since 1.2.7
     */
    public BlockPosHelper getBlockPos() {
        return new BlockPosHelper(bp);
    }

    /**
     * Returns the underlying Minecraft block type, without its state properties.
     *
     * @return the raw block object this helper wraps.
     */
    public Block getRawBlock() {
        return b;
    }

    /**
     * Returns the underlying Minecraft block state wrapped by this helper.
     *
     * @return the raw block state this helper wraps.
     */
    public BlockState getRawBlockState() {
        return base;
    }

    /**
     * Returns the captured block entity, or {@code null} if none was captured. {@link #getNBT()}
     * serializes this same entity when present.
     *
     * @return the raw block entity, or {@code null} if this block has none.
     */
    public BlockEntity getRawBlockEntity() {
        return e;
    }

    /**
     * Returns a diagnostic representation containing the stored position coordinates and block registry id.
     *
     * @return diagnostic representation of this block data
     */
    @Override
    public String toString() {
        return String.format("BlockDataHelper:{\"x\": %d, \"y\": %d, \"z\": %d, \"id\": \"%s\"}", bp.getX(), bp.getY(), bp.getZ(), this.getId());
    }

}
