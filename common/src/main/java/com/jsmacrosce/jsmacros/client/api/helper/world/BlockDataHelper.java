package com.jsmacrosce.jsmacros.client.api.helper.world;

import net.minecraft.Util;
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

/**
 * @author Wagyourtail
 */
@SuppressWarnings("unused")
public class BlockDataHelper extends BaseHelper<BlockState> {
    private static final Minecraft mc = Minecraft.getInstance();

    private final Block b;
    private final BlockPos bp;
    private final BlockEntity e;

    public BlockDataHelper(BlockState b, BlockEntity e, BlockPos bp) {
        super(b);
        this.b = b.getBlock();
        this.bp = bp;
        this.e = e;
    }

    /**
     * @return the {@code x} value of the block.
     * @since 1.1.7
     */
    public int getX() {
        return bp.getX();
    }

    /**
     * @return the {@code y} value of the block.
     * @since 1.1.7
     */
    public int getY() {
        return bp.getY();
    }

    /**
     * @return the {@code z} value of the block.
     * @since 1.1.7
     */
    public int getZ() {
        return bp.getZ();
    }

    /**
     * @return the item ID of the block.
     */
    @DocletReplaceReturn("BlockId")
    public String getId() {
        return BuiltInRegistries.BLOCK.getKey(b).toString();
    }

    /**
     * @return the translated name of the block. (was string before 1.6.5)
     */
    public TextHelper getName() {
        return TextHelper.wrap(b.getName());
    }

    /**
     * @return
     * @since 1.5.1, used to be a {@link Map}&lt;{@link String}, {@link String}&gt;
     */
    @Nullable
    public NBTElementHelper.NBTCompoundHelper getNBT() {
        return e == null ? null : NBTElementHelper.wrapCompound(e.saveWithoutMetadata(RegistryHelper.getWrapperLookup()));
    }

    /**
     * @return
     * @since 1.6.5
     */
    public BlockStateHelper getBlockStateHelper() {
        return new BlockStateHelper(base);
    }

    /**
     * @return
     * @since 1.6.5
     * @deprecated use {@link #getBlock()} instead.
     */
    @Deprecated
    public BlockHelper getBlockHelper() {
        return getBlock();
    }

    /**
     * @return the block
     * @since 1.6.5
     */
    public BlockHelper getBlock() {
        return new BlockHelper(base.getBlock());
    }

    /**
     * @return block state data as a {@link Map}.
     * @since 1.1.7
     */
    public Map<String, String> getBlockState() {
        Map<String, String> map = new HashMap<>();
        for (Entry<Property<?>, Comparable<?>> e : base.getValues().entrySet()) {
            map.put(e.getKey().getName(), Util.getPropertyName(e.getKey(), e.getValue()));
        }
        return map;
    }

    /**
     * @return the block pos.
     * @since 1.2.7
     */
    public BlockPosHelper getBlockPos() {
        return new BlockPosHelper(bp);
    }

    public Block getRawBlock() {
        return b;
    }

    public BlockState getRawBlockState() {
        return base;
    }

    public BlockEntity getRawBlockEntity() {
        return e;
    }

    @Override
    public String toString() {
        return String.format("BlockDataHelper:{\"x\": %d, \"y\": %d, \"z\": %d, \"id\": \"%s\"}", bp.getX(), bp.getY(), bp.getZ(), this.getId());
    }

}
