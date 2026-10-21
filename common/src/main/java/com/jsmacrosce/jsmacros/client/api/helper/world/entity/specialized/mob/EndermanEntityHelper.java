package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.EnderMan;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * Provides access to an enderman's target-related flags and carried block state.
 * <p>
 * {@link #isScreaming()} reflects whether the enderman has a target; that flag selects its
 * screaming ambient sound. {@link #isProvoked()} separately reflects whether its stare goal
 * has started after identifying a player as a target. An enderman may therefore be provoked
 * before it has a target. When an enderman picks up a block, the game stores that block's
 * default state, not necessarily the state of the block at its original location.
 * <pre>
 * const EndermanEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.EndermanEntityHelper");
 * const endermen = World.getEntities(32, "enderman");
 * if (endermen !== null) {
 *   for (const entity of endermen) {
 *     const enderman = EndermanEntityHelper.class.cast(entity);
 *     if (enderman.isHoldingBlock()) {
 *       // The carried block is exposed as a block state.
 *       Chat.log(`an enderman at ${enderman.getPos()} is carrying ${enderman.getHeldBlock().getId()}`);
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Entity Helpers")
@SuppressWarnings("unused")
public class EndermanEntityHelper extends MobEntityHelper<EnderMan> {

    /**
     * Wraps the supplied enderman and retains its reference for this helper.
     *
     * @param base enderman entity to wrap
     */
    public EndermanEntityHelper(EnderMan base) {
        super(base);
    }

    /**
     * Whether the enderman has a target. This flag also selects the screaming ambient sound
     * and is distinct from {@link #isProvoked()}, which can become {@code true} before a
     * target is assigned.
     *
     * @return {@code true} if this enderman is screaming, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isScreaming() {
        return base.isCreepy();
    }

    /**
     * Whether the enderman's player-targeting goal has identified a player and started
     * pursuing it. This flag is set when that goal starts, before it assigns the target,
     * and is cleared when the target is set to {@code null}.
     *
     * @return {@code true} if this enderman was provoked by a player, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isProvoked() {
        return base.hasBeenStaredAt();
    }

    /**
     * Whether the enderman has a carried block; equivalent to checking whether
     * {@link #getHeldBlock()} returns a non-null value.
     *
     * @return {@code true} if this enderman is holding a block, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isHoldingBlock() {
        return base.getCarriedBlock() != null;
    }

    /**
     * The carried block's default state, or {@code null} if the enderman is carrying no
     * block. When a block is picked up, its original state properties are not retained.
     *
     * @return the held block of this enderman, or {@code null} if it is not holding a
     * block.
     * @since 1.8.4
     */
    @Nullable
    public BlockStateHelper getHeldBlock() {
        return isHoldingBlock() ? new BlockStateHelper(base.getCarriedBlock()) : null;
    }

}
