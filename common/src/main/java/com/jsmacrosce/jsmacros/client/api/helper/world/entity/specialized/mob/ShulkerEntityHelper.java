package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import net.minecraft.world.entity.monster.Shulker;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.client.api.helper.DyeColorHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.DirectionHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinShulkerEntity;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class ShulkerEntityHelper extends MobEntityHelper<Shulker> {

    public ShulkerEntityHelper(Shulker base) {
        super(base);
    }

    public boolean isClosed() {
        return ((MixinShulkerEntity) base).invokeIsClosed();
    }

    public DirectionHelper getAttachedSide() {
        return new DirectionHelper(base.getAttachFace());
    }

    @Nullable
    public DyeColorHelper getColor() {
        return base.getColor() == null ? null : new DyeColorHelper(base.getColor());
    }

}
