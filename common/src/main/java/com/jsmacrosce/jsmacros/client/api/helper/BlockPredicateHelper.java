package com.jsmacrosce.jsmacros.client.api.helper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.List;

//? if >=1.21.11 {
/*import net.minecraft.advancements.criterion.BlockPredicate;
*///? } else {
import net.minecraft.advancements.critereon.BlockPredicate;
//? }

/**
 * @since 1.9.1
 */
public class BlockPredicateHelper extends BaseHelper<BlockPredicate> {
    private static final Minecraft mc = Minecraft.getInstance();

    public BlockPredicateHelper(BlockPredicate base) {
        super(base);
    }

    /**
     * @since 1.9.1
     */
    @Nullable
    public List<BlockHelper> getBlocks() {
        if (base.blocks().isEmpty()) return null;
        return base.blocks().get().stream().map(Holder::value).map(BlockHelper::new).toList();
    }

    /**
     * @since 1.9.1
     */
    @Nullable
    public StatePredicateHelper getStatePredicate() {
        if (base.properties().isEmpty()) return null;
        return new StatePredicateHelper(base.properties().get());
    }

    /**
     * @since 1.9.1
     */
    @Nullable
    public NbtPredicateHelper getNbtPredicate() {
        if (base.nbt().isEmpty()) return null;
        return new NbtPredicateHelper(base.nbt().get());
    }

    /**
     * @since 1.9.1
     *
     * @param state
     * @return
     */
    public boolean test(BlockPosHelper state) {
        return base.matches(new BlockInWorld(mc.level, state.getRaw(), true));
    }

}
