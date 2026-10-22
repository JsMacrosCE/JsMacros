package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.EnderMan;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

/**
 * the enderman, and the two moods it has.
 * <p>
 * An enderman starts out standing still and takes a block, and it turns aggressive when a
 * player looks straight at it. The game tracks that as two flags rather than as one
 * state, and they are not the same question:
 * {@link #isScreaming() isScreaming} is the flag the enderman's ambient sound is chosen
 * from, and it is raised as soon as the enderman is given a target, while
 * {@link #isProvoked() isProvoked} is the separate flag the stare goal raises to record
 * that a player has actually been looking at it.
 * <p>
 * What it is carrying is the other half of an enderman's day, and
 * {@link #isHoldingBlock() isHoldingBlock} and {@link #getHeldBlock() getHeldBlock} are the
 * check and the answer to the same question.
 * example:
 * <pre>
 * const EndermanEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.EndermanEntityHelper");
 * const endermen = World.getEntities(32, "enderman");
 * if (endermen !== null) {
 *   for (const entity of endermen) {
 *     const enderman = EndermanEntityHelper.class.cast(entity);
 *     if (enderman.isHoldingBlock()) {
 *       // the held block is a block state, and it is the block it will put down or drop
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

    public EndermanEntityHelper(EnderMan base) {
        super(base);
    }

    /**
     * Whether the enderman is in its aggressive mood, which is the flag the game picks the
     * enderman's ambient sound from: a screaming enderman makes the screaming sound rather
     * than the idle one. The game sets it as soon as the enderman is given a target and
     * clears it when that target is dropped, so it is really "is it after something".
     * <p>
     * This is a separate flag from {@link #isProvoked() isProvoked} and the wider of the
     * two. The two are not simply one behind the other: the stare goal raises the provoked
     * flag as it starts and the enderman is only given its target a few ticks after that,
     * so a provoked enderman can read {@code false} here for those ticks.
     * example:
     * <pre>
     * const EndermanEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.EndermanEntityHelper");
     * const endermen = World.getEntities(32, "enderman");
     * if (endermen !== null) {
     *   for (const entity of endermen) {
     *     const enderman = EndermanEntityHelper.class.cast(entity);
     *     if (enderman.isScreaming()) {
     *       Chat.log(`a screaming enderman at ${enderman.getPos()}, provoked: ${enderman.isProvoked()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this enderman is screaming, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isScreaming() {
        return base.isCreepy();
    }

    /**
     * Whether the enderman has been provoked, which is the game's separate record that a
     * player looked at it. The stare goal raises the flag as it starts, and the game only
     * lowers it again when the enderman is given no target at all.
     * <p>
     * It is set a few ticks before the enderman actually has a target, so a provoked
     * enderman can still read {@code false} on {@link #isScreaming() isScreaming} for a
     * moment. Once the target is there the two stay together, because the game clears both
     * flags in the same place.
     * example:
     * <pre>
     * const EndermanEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.EndermanEntityHelper");
     * const endermen = World.getEntities(32, "enderman");
     * if (endermen !== null) {
     *   for (const entity of endermen) {
     *     const enderman = EndermanEntityHelper.class.cast(entity);
     *     if (enderman.isProvoked()) {
     *       // a provoked enderman is the one that teleports out of the way
     *       Chat.log(`a provoked enderman at ${enderman.getPos()}, health ${enderman.getHealth()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this enderman was provoked by a player, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isProvoked() {
        return base.hasBeenStaredAt();
    }

    /**
     * Whether the enderman is carrying a block, which is the same check
     * {@link #getHeldBlock() getHeldBlock} makes before handing anything back, so a script
     * can ask this first and only unwrap the result when it is {@code true}.
     * example:
     * <pre>
     * const EndermanEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.EndermanEntityHelper");
     * const endermen = World.getEntities(32, "enderman");
     * if (endermen !== null) {
     *   for (const entity of endermen) {
     *     const enderman = EndermanEntityHelper.class.cast(entity);
     *     if (enderman.isHoldingBlock()) {
     *       // asking first keeps the null check off the unwrapping
     *       Chat.log(`carrying something at ${enderman.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this enderman is holding a block, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isHoldingBlock() {
        return base.getCarriedBlock() != null;
    }

    /**
     * The block state the enderman is carrying, or {@code null} when it has nothing. What
     * comes back is the block as a state rather than as an item, so a block that has
     * several states is the state the enderman picked up rather than the bare block id.
     * example:
     * <pre>
     * const EndermanEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.EndermanEntityHelper");
     * const endermen = World.getEntities(32, "enderman");
     * if (endermen !== null) {
     *   for (const entity of endermen) {
     *     const enderman = EndermanEntityHelper.class.cast(entity);
     *     const held = enderman.getHeldBlock();
     *     if (held !== null) {
     *       Chat.log(`an enderman at ${enderman.getPos()} is carrying ${held.getId()}`);
     *     }
     *   }
     * }
     * </pre>
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
