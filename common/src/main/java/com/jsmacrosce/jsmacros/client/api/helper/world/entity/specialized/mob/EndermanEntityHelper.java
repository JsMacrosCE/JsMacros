package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.EnderMan;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class EndermanEntityHelper extends MobEntityHelper<EnderMan> {

    public EndermanEntityHelper(EnderMan base) {
        super(base);
    }

    /**
     * @return {@code true} if this enderman is screaming, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isScreaming() {
        return base.isCreepy();
    }

    /**
     * @return {@code true} if this enderman was provoked by a player, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isProvoked() {
        return base.hasBeenStaredAt();
    }

    /**
     * @return {@code true} if this enderman is holding a block, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isHoldingBlock() {
        return base.getCarriedBlock() != null;
    }

    /**
     * @return the held block of this enderman, or {@code null} if it is not holding a block.
     * @since 1.8.4
     */
    @Nullable
    public BlockStateHelper getHeldBlock() {
        return isHoldingBlock() ? new BlockStateHelper(base.getCarriedBlock()) : null;
    }

}
