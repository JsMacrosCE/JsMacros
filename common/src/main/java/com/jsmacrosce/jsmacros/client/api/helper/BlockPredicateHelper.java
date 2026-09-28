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
 * a block condition from an item or a recipe, such as "any oak log".
 * <p>
 * This is a <i>condition</i> rather than a block: it is a test that a position either passes or
 * does not, and {@link #test(BlockPosHelper)} is the only way to ask. It is the shape the game
 * uses for the restrictions items carry — where a compass or an elytra may be used, and what an
 * item is allowed to break or place — and the same shape a recipe's ingredient list uses.
 * <p>
 * A condition is made of up to three parts, and any of them may be absent: the blocks it accepts
 * ({@link #getBlocks()}), the exact block states it accepts ({@link #getStatePredicate()}), and
 * the extra block data it requires ({@link #getNbtPredicate()}). A condition with none of them
 * is "any block", and a part that is absent gives {@code null} rather than an empty list, so a
 * script has to treat {@code null} and empty as the same thing.
 * <br>
 * All three narrow rather than widen: a condition passes only if every part it names passes, so
 * a condition naming both a block list and a state is a stricter test than either alone. The
 * condition does carry a fourth part in the game — a data-component matcher on the block entity
 * — and the check the game runs against a server world does apply it, but the overload
 * {@link #test(BlockPosHelper)} reaches is the one that reads only the three parts above, so a
 * data-component matcher cannot turn down a position through this class. A script cannot see
 * that fourth part either, since this wrapper exposes no getter for it.
 * example:
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

    public BlockPredicateHelper(BlockPredicate base) {
        super(base);
    }

    /**
     * the blocks this condition accepts, or {@code null} if it names none.
     * <p>
     * An absent list means the condition does not restrict which block it is, so
     * {@code null} and an empty result both mean "any block" as far as {@link #test} is
     * concerned. What is returned is a snapshot list built on each call.
     * <p>
     * A block is a type rather than a state, so a condition naming stone accepts every state
     * of stone unless {@link #getStatePredicate()} narrows it further.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const elytra = player.getMainHand();
     * for (const condition of elytra.getDestroyRestrictions()) {
     *   const blocks = condition.getBlocks();
     *   if (blocks !== null) {
     *     for (const block of blocks) {
     *       Chat.log(`allowed block ${block.getName().getString()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @since 1.9.1
     */
    @Nullable
    public List<BlockHelper> getBlocks() {
        if (base.blocks().isEmpty()) return null;
        return base.blocks().get().stream().map(Holder::value).map(BlockHelper::new).toList();
    }

    /**
     * the exact block states this condition accepts, or {@code null} if it names none.
     * <p>
     * This is the narrower half of a block condition: {@link #getBlocks()} picks which blocks
     * are acceptable and this picks which of their states are. A condition that has one but not
     * the other is more permissive than one that has both.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const elytra = player.getMainHand();
     * for (const condition of elytra.getDestroyRestrictions()) {
     *   const states = condition.getStatePredicate();
     *   if (states !== null) {
     *     // the state test needs the actual state, so ask the world for it
     *     const below = World.getBlock(player.getBlockPos().down());
     *     if (below !== null) {
     *       Chat.log(`the block below matches the state: ${states.test(below.getBlockStateHelper())}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @since 1.9.1
     */
    @Nullable
    public StatePredicateHelper getStatePredicate() {
        if (base.properties().isEmpty()) return null;
        return new StatePredicateHelper(base.properties().get());
    }

    /**
     * the extra block data this condition requires, or {@code null} if it names none.
     * <p>
     * This is the third and last part, and it is the one that checks data rather than the block
     * itself — a chest's contents, a sign's text. It is also the only part that can be applied
     * to something that is not a block in the world, which is what makes it usable against an
     * item or an entity.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player === null) { throw new Error("not in a world"); }
     * const elytra = player.getMainHand();
     * for (const condition of elytra.getDestroyRestrictions()) {
     *   const nbt = condition.getNbtPredicate();
     *   if (nbt !== null) {
     *     const below = World.getBlock(player.getBlockPos().down());
     *     if (below !== null) {
     *       const data = below.getNBT();
     *       if (data !== null) {
     *         Chat.log(`the block below matches the data: ${nbt.test(data)}`);
     *       }
     *     }
     *   }
     * }
     * </pre>
     *
     * @since 1.9.1
     */
    @Nullable
    public NbtPredicateHelper getNbtPredicate() {
        if (base.nbt().isEmpty()) return null;
        return new NbtPredicateHelper(base.nbt().get());
    }

    /**
     * whether a block in the world satisfies this condition.
     * <p>
     * This is the test the whole class is for. It reads the block at the given position out of
     * the current level and applies every part of the condition to it, so all of
     * {@link #getBlocks()}, {@link #getStatePredicate()} and {@link #getNbtPredicate()} have to
     * pass for the answer to be {@code true} — and a condition with none of them set accepts
     * anything.
     * <br>
     * The position is read from the level this client has, so a position that is not loaded
     * gives whatever the unloaded chunk looks like rather than a failure. This needs a world.
     * example:
     * <pre>
     * const player = Player.getPlayer();
     * if (player !== null) {
     *   const elytra = player.getMainHand();
     *   const target = player.getBlockPos();
     *   for (const condition of elytra.getDestroyRestrictions()) {
     *     if (condition.test(target)) {
     *       Chat.log(`the elytra may be used at ${target.getX()}, ${target.getY()}, ${target.getZ()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @param state
     * @return
     * @since 1.9.1
     */
    public boolean test(BlockPosHelper state) {
        return base.matches(new BlockInWorld(mc.level, state.getRaw(), true));
    }

}
