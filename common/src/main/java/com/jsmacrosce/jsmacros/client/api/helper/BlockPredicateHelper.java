package com.jsmacrosce.jsmacros.client.api.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;

import org.jetbrains.annotations.Nullable;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.List;

import net.minecraft.advancements.critereon.BlockPredicate;

/**
 * A condition that tests whether a block at a position matches its block, state, and NBT criteria.
 * <p>
 * Each criterion may be absent; the getters return {@code null} when absent. {@link #test(BlockPosHelper)}
 * checks the block and state criteria and, when present, its block-entity NBT predicate. It does
 * not apply the underlying predicate's data-component matcher.
 * <pre>
 * const player = Player.getPlayer();
 * if (player === null) { throw new Error("not in a world"); }
 *
 * // an elytra restricts where it may be used, and those are block conditions
 * const elytra = player.getMainHand();
 * for (const condition of elytra.getDestroyRestrictions()) {
 *   const blocks = condition.getBlocks();
 *   // an absent list means "any block", which is null rather than empty
 *   if (blocks === null) {
 *     Chat.log("this one restricts nothing on its own");
 *   } else {
 *     for (const block of blocks) {
 *       Chat.log(`allowed block ${block.getName().getString()}`);
 *     }
 *   }
 *
 *   // the actual test, against a position in the world
 *   const under = player.getBlockPos().down();
 *   Chat.log(`the block below passes: ${condition.test(under)}`);
 * }
 * </pre>
 *
 * @since 1.9.1
 */
@DocletCategory("Misc Helpers")
public class BlockPredicateHelper extends BaseHelper<BlockPredicate> {
    private static final Minecraft mc = Minecraft.getInstance();

    /** Wraps the supplied block predicate reference without copying or validating it.
     * @param base raw block predicate to wrap
     */
    public BlockPredicateHelper(BlockPredicate base) {
        super(base);
    }

    /**
     * Returns the blocks this condition accepts, or {@code null} if it names none.
     * <p>
     * An absent list means the condition does not restrict the block type. The returned list is
     * newly built on each call.
     *
     * @return the accepted blocks, or {@code null} when the condition does not restrict block type
     * @since 1.9.1
     */
    @Nullable
    public List<BlockHelper> getBlocks() {
        if (base.blocks().isEmpty()) return null;
        return base.blocks().get().stream().map(Holder::value).map(BlockHelper::new).toList();
    }

    /**
     * Returns the exact block states this condition accepts, or {@code null} if it names none.
     * <p>
     * When present, this criterion further restricts the block states accepted by
     * {@link #getBlocks()}.
     *
     * @return the state criterion, or {@code null} when absent
     * @since 1.9.1
     */
    @Nullable
    public StatePredicateHelper getStatePredicate() {
        if (base.properties().isEmpty()) return null;
        return new StatePredicateHelper(base.properties().get());
    }

    /**
     * Returns the extra block data this condition requires, or {@code null} if it names none.
     * <p>
     * When present, this criterion is tested against block-entity data at the position.
     *
     * @return the NBT criterion, or {@code null} when absent
     * @since 1.9.1
     */
    @Nullable
    public NbtPredicateHelper getNbtPredicate() {
        if (base.nbt().isEmpty()) return null;
        return new NbtPredicateHelper(base.nbt().get());
    }

    /**
     * Tests the block at the position against this condition using the current client level.
     * The test uses the block and state criteria and optional block-entity NBT predicate; it
     * does not check the data-component matcher. A world must be available.
     *
     * @param state position to test in the current client world
     * @return whether the block matches the criteria checked by this method
     * @since 1.9.1
     */
    public boolean test(BlockPosHelper state) {
        return base.matches(new BlockInWorld(mc.level, state.getRaw(), true));
    }

}
