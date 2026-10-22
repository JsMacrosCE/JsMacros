package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Shulker;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.DyeColorHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.DirectionHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinShulkerEntity;

/**
 * the shulker, which is a mob that spends its life attached to a ceiling.
 * <p>
 * All three calls here are about the shulker's shell rather than about a fight.
 * {@link #getAttachedSide() getAttachedSide} is the face of the block it is stuck to, which
 * is also the direction it came from: the shulker looks and shoots away from the block
 * rather than into it. {@link #isClosed() isClosed} is whether it is pulled all the way in,
 * and {@link #getColor() getColor} is the dye on its shell, which a shulker spawned
 * without one does not have.
 * example:
 * <pre>
 * const ShulkerEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.ShulkerEntityHelper");
 * const shulkers = World.getEntities(32, "shulker");
 * if (shulkers !== null) {
 *   for (const entity of shulkers) {
 *     const shulker = ShulkerEntityHelper.class.cast(entity);
 *     if (shulker.isClosed()) {
 *       // fully retracted, so it is not shooting out of its shell
 *       Chat.log(`a closed shulker at ${shulker.getPos()}, on the ${shulker.getAttachedSide().getName()}`);
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
public class ShulkerEntityHelper extends MobEntityHelper<Shulker> {

    public ShulkerEntityHelper(Shulker base) {
        super(base);
    }

    /**
     * Whether the shulker is closed, which is the game checking the peek amount against
     * zero: a shulker with nothing showing is closed and one that is peeking out is not.
     * <p>
     * The game sounds different for a closed shulker — it picks a separate hurt sound and
     * skips the ambient one entirely — so this is the check for "is it shut".
     * example:
     * <pre>
     * const ShulkerEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.ShulkerEntityHelper");
     * const shulkers = World.getEntities(32, "shulker");
     * if (shulkers !== null) {
     *   for (const entity of shulkers) {
     *     const shulker = ShulkerEntityHelper.class.cast(entity);
     *     if (shulker.isClosed()) {
     *       // the peek is a whole number of ticks, so a closed shulker is at zero
     *       Chat.log(`a closed shulker at ${shulker.getPos()}, health ${shulker.getHealth()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return {@code true} if this shulker is fully retracted into its shell, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isClosed() {
        return ((MixinShulkerEntity) base).invokeIsClosed();
    }

    /**
     * The face of the block the shulker is attached to. This is not a direction the shulker
     * is facing: it is the side of the world it is stuck to, and the shulker's body and its
     * projectiles come out on the opposite side of it, so a shulker hanging from a ceiling
     * has {@code UP} here and comes out of the bottom of its shell.
     * <p>
     * The game re-picks this whenever the block it was on stops holding it, and moves the
     * shulker elsewhere if there is nowhere to go.
     * example:
     * <pre>
     * const ShulkerEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.ShulkerEntityHelper");
     * const shulkers = World.getEntities(32, "shulker");
     * if (shulkers !== null) {
     *   for (const entity of shulkers) {
     *     const shulker = ShulkerEntityHelper.class.cast(entity);
     *     // the attached face, not the facing: these are opposites of each other
     *     const side = shulker.getAttachedSide();
     *     if (side.isVertical()) {
     *       Chat.log(`a shulker at ${shulker.getPos()} is on the ${side.getName()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the face of the block this shulker is attached to.
     * @since 1.8.4
     */
    public DirectionHelper getAttachedSide() {
        return new DirectionHelper(base.getAttachFace());
    }

    /**
     * The dye colour on the shulker's shell, or {@code null} when it has none. The game
     * stores a marker for "no colour" separately from the sixteen colours, and that marker
     * is what comes back as {@code null} here, so a plain shulker answers {@code null}
     * rather than a default colour.
     * <p>
     * The colour is the shulker's own variant rather than anything about the block it is
     * stuck to, and it is set as a data component on the shell as well, so it survives in
     * an item as well as on the mob.
     * example:
     * <pre>
     * const ShulkerEntityHelper = Java.type("com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob.ShulkerEntityHelper");
     * const shulkers = World.getEntities(32, "shulker");
     * if (shulkers !== null) {
     *   for (const entity of shulkers) {
     *     const shulker = ShulkerEntityHelper.class.cast(entity);
     *     // null is a real answer here: it means the shell has no dye on it
     *     const color = shulker.getColor();
     *     if (color === null) {
     *       Chat.log(`an undyed shulker at ${shulker.getPos()}`);
     *     } else {
     *       Chat.log(`a ${color.getName()} shulker at ${shulker.getPos()}`);
     *     }
     *   }
     * }
     * </pre>
     *
     * @return the dye colour of this shulker's shell, or {@code null} if it has none.
     * @since 1.8.4
     */
    @Nullable
    public DyeColorHelper getColor() {
        return base.getColor() == null ? null : new DyeColorHelper(base.getColor());
    }

}
