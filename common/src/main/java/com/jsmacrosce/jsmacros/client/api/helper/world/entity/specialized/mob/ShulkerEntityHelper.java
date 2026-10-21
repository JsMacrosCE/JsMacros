package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Shulker;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.DyeColorHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.DirectionHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinShulkerEntity;

/**
 * Provides access to a shulker's shell state, attachment face, and optional dye color.
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

    /**
     * Wraps the supplied shulker and retains its reference for this helper.
     *
     * @param base shulker entity to wrap
     */
    public ShulkerEntityHelper(Shulker base) {
        super(base);
    }

    /**
     * Returns whether the shulker's raw peek amount is zero.
     *
     * @return {@code true} if this shulker is fully retracted into its shell, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isClosed() {
        return ((MixinShulkerEntity) base).invokeIsClosed();
    }

    /**
     * Returns the direction of the face to which the shulker is attached.
     *
     * @return the face of the block this shulker is attached to.
     * @since 1.8.4
     */
    public DirectionHelper getAttachedSide() {
        return new DirectionHelper(base.getAttachFace());
    }

    /**
     * Returns the shulker's dye color, or {@code null} when it has no color.
     *
     * @return the dye color, or {@code null} if the shulker has no color.
     * @since 1.8.4
     */
    @Nullable
    public DyeColorHelper getColor() {
        return base.getColor() == null ? null : new DyeColorHelper(base.getColor());
    }

}
